package controllers

import play.api._
import libs.json.{JsValue, JsObject}
import libs.ws.WS
import play.api.mvc._
import java.nio.charset.StandardCharsets
import play.api.libs.concurrent.Execution.Implicits._
import concurrent.Future

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.main())
  }

  def getLocation = Action { request =>

    request.body.asJson.map { json =>

      val ip = (json \ "ip").as[String]
      val coordinates = (json \ "coordinates").as[String].split(",")

      Logger info ("IP " + ip)
      Logger info ("Coords " + coordinates(0)  + " " + coordinates(1))

      val latitude = coordinates(0).trim
      val longitude = coordinates(1).trim

      val request: Future[String] = serviceArrgregator(latitude, longitude)

      Async {
        request.map { response =>
          Ok(response)
        }
      }

    }.getOrElse {
      
      BadRequest("Expecting Json data")
    
    }
  }

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

  private def serviceArrgregator(latitude: String, longitude: String): Future[String] = {
    buildGoogleRequest(latitude, longitude) fallbackTo buildBingRequest(latitude, longitude)
  }
}