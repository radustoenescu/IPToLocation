package controllers

import play.api._
import libs.json.{JsValue, JsObject}
import libs.ws.WS
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play._
import com.maxmind.geoip2.DatabaseReader
import scala.concurrent._
import java.net.InetAddress

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.main())
  }

  def getLocation = Action { request =>

    request.body.asJson.map { json =>

      val ip = (json \ "ip").as[String]
      val coordinates = (json \ "coordinates").as[String].split(",")

      val parsedCoords = if (coordinates.length == 2) Some((coordinates(0).trim, coordinates(1).trim)) else None

      val request: Future[String] = serviceArrgregator(parsedCoords, ip)

      Async {
        request.map { response =>
          Ok(response)
        }
      }

    }.getOrElse {
      
      BadRequest("Expecting Json data")
    
    }
  }

  /**
   * Ask Google.
   * @param latitude
   * @param longitude
   * @return
   */
  private def buildGoogleRequest(latitude: String, longitude: String): Future[String] = {
    val url = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
      latitude + "," + longitude + "&sensor=false"

    WS.url(url).get().
      map { response => response.json
    }.filter { response => (response \ "status").asOpt[String] match {
      //            make sure status is ok
        case Some(value) => value equals "OK"
        case None => false
      }
    }.map { response =>
      (response \ "results").asOpt[List[JsObject]] match {
        case Some(results) =>
          Logger info (results.mkString("\n"))
          //            get those results of type locality
          results.filter { result =>

            (result \ "types").asOpt[List[JsValue]] match {
              case Some(types) =>
                Logger info types.mkString("\n")

                types.exists { resultType =>
                  resultType.toString() == "\"locality\""
                }

              case None => false
            }

          } match {
            case locality :: _ => (locality \ "address_components")(0).\("long_name").toString
            case _ => throw new NoSuchElementException
          }

        case None => throw new NoSuchElementException
      }
    }
  }

  /**
   * Ask Bing.
   * @param latitude
   * @param longitude
   * @return
   */
  private def buildBingRequest(latitude: String, longitude: String): Future[String] = {
    val url = "http://dev.virtualearth.net/REST/v1/Locations/" + latitude +
    "," + longitude + "?o=json&key=AgC3EDAI9VgAoukryRvih86t9M9WUEOhkC7xu7VV0AF6W5aVNPvz6thRnoeRMTRg"

    WS.url(url).get().
      map { response => response.json
    }.filter { response => (response \ "statusDescription").asOpt[String] match {
      //            make sure status is ok
        case Some(value) => value equals "OK"
        case None => false
      }
    }.map { response =>
      (response \ "resourceSets").asOpt[List[JsObject]] match {

        case Some(results) if results.length > 0 =>
          Logger info (results.mkString("\n"))
          ((results(0) \ "resources")(0) \ "address" \ "locality").toString

        case None => throw new NoSuchElementException
      }
    }
  }

  private lazy val ipBasedLocationService = new DatabaseReader(getExistingFile("/app/resources/db.mmdb").get)

  /**
   * Ask the GeoIP database.
   * @param ip
   * @return
   */
  private def buildMaxMindIPRequest(ip: String): Future[String] = {
    future {
      val city = ipBasedLocationService.city(InetAddress.getByName(ip))
      city.toString
    }
  }

  /**
   * When nothing works return a kind error message.
   * @return
   */
  private def defaultCityName: Future[String] = future("Could not find city")

  type LatLng = Option[(String,String)]

  /**
   * Method building the layered fallback path
   * @param coords
   * @param ip
   * @return
   */
  private def serviceArrgregator(coords: LatLng, ip: String = "127.0.0.1"): Future[String] = {
    //    build fallback layer
    val geoIpOrError = buildMaxMindIPRequest(ip) fallbackTo defaultCityName
    //    build web service request layer if params given
    val googleOrBing = coords match {
      case Some((lat, lng)) => Some(buildGoogleRequest(lat, lng) fallbackTo buildBingRequest(lat, lng))
      case _ => None
    }
    //    combine the two layers
    googleOrBing match {
      case Some(googleOrBingRequest) => googleOrBingRequest fallbackTo geoIpOrError
      case None => geoIpOrError
    }
  }
}