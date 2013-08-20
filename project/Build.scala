import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "locator"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.webjars" %% "webjars-play" % "2.1.0-2",    
    "org.webjars" % "angularjs" % "1.1.5-1",
    "com.twitter" % "finagle-core" % "6.5.2", 
    "com.twitter" % "finagle-http" % "6.5.2"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers ++= Seq("Twitter Repo" at "http://maven.twttr.com/")
  )


}
