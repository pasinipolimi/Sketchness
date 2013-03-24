import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "Sketchness"
    val appVersion      = "0.1"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    )

}
