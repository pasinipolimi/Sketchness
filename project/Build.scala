import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "Sketchness"
    val appVersion      = "0.1"

    val appDependencies = Seq(
	  javaCore
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
