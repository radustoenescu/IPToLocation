package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.main())
  }

  def getLocation = Action { request =>

    request.body.asJson.map { json =>

      val ip = (json \ "ip").as[String]
      val coordinates = (json \ "coordinates").as[String]

      println(ip + " " + coordinates)

      Ok("Feed added")

    }.getOrElse {
      
      BadRequest("Expecting Json data")
    
    }
  }
  
}