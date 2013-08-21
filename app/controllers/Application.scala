package controllers

import play.api._
import libs.ws.WS
import play.api.mvc._
import services.GoogleAPI
import java.nio.charset.StandardCharsets
import play.api.libs.concurrent.Execution.Implicits._

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

      Async {
        val url = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
          latitude + "," + longitude + "&sensor=false"

        Logger info ("Requesting: " + url)

        WS.url(url).get().map{response =>
          Logger info (response.body)

          Ok(response.json.toString())
        }
      }

    }.getOrElse {
      
      BadRequest("Expecting Json data")
    
    }
  }
  
}