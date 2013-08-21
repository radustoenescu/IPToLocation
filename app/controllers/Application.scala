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

      val url = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
        latitude + "," + longitude + "&sensor=false"

      Logger info ("Asking Google for: " + url)

      val googleLocation = WS.url(url).get().
        map { response => response.json
        }.filter { response => (response \ "status").asOpt[String] match {
            case Some(value) => value equals "OK"
            case None => false
         }
        }. map { response =>
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

      val request: Future[String] = googleLocation

      Async {
        request.map { response =>
          Ok(response)
        }
      }

    }.getOrElse {
      
      BadRequest("Expecting Json data")
    
    }
  }
  
}