import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

	val appName         = "Sketchness"
	val appVersion      = "1.0-SNAPSHOT"

	val appDependencies = Seq(
		// Add your project dependencies here,
		javaCore,
		javaJdbc,
		javaEbean,
		"org.json" % "json" % "20090211"
	)

    /** Defines a new setting key that contains the resources list */
    lazy val sketchnessResources = SettingKey[Seq[Resource]](
		"sketchness-resources",
		"The remote resources handled outside del VCS.")

    val sketchnessResourcesSetting = sketchnessResources := Seq(
		// Add here the required resources
		// Resource(name: String, path :String, url :String = null, zipped :Boolean = true)
		Resource("Lib Folder", "lib"),
		Resource("jQuery", "public/javascripts/lib/jquery.js", "http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js", false),
		Resource("jQuery i18n", "public/javascripts/lib/jquery.i18n.properties.js", "http://jquery-i18n-properties.googlecode.com/files/jquery.i18n.properties-min-1.0.9.js", false),
		Resource("jScrollPane", "public/javascripts/lib/jquery.jscrollpane.js", "http://jscrollpane.kelvinluck.com/script/jquery.jscrollpane.min.js", false),
		Resource("jScrollPane MouseWheel", "public/javascripts/lib/jquery.mousewheel.js", "http://jscrollpane.kelvinluck.com/script/jquery.mousewheel.js", false),
		Resource("jScrollPane MouseWheelIntent", "public/javascripts/lib/mwheelIntent.js", "http://jscrollpane.kelvinluck.com/script/mwheelIntent.js", false),
		Resource("jScrollPane CSS", "public/stylesheets/lib/jquery.jscrollpane.css", "http://jscrollpane.kelvinluck.com/style/jquery.jscrollpane.css", false),
		Resource("Bootstrap CSS", "public/stylesheets/lib/bootstrap.css", "https://raw.github.com/twitter/bootstrap/1c7c5f750fc221ee94e435e4e49bc2ba1a6be5e6/bootstrap.css", false),
		Resource("Website Images", "public/images", "https://dl.dropboxusercontent.com/u/1264722/images.zip?dl=1",true)
    )

    /** Defines a new task key for retrieving all the resources
	  *
      * (Callable from the play console in the terminal
      * with the command "sketchness-resources-get")
      */
    lazy val sketchnessGetResources = TaskKey[Unit](
		"sketchness-resources-get",
		"Task to retrieve the remote resources handled outside del VCS.")

    val sketchnessGetResourcesTask = sketchnessGetResources <<= (sketchnessResources, streams) map {
		(resources, streams) => resources foreach { _.get(streams.log) }
	}

    /** Defines a new task key for deleting all the resources
      *
      * (Callable from the play console in the terminal
      * with the command "sketchness-resources-clean")
      */
    lazy val sketchnessCleanResources = TaskKey[Unit](
		"sketchness-resources-clean",
		"Task to delete the local copy of the resources handled outside del VCS.")

    val sketchnessCleanResourcesTask = sketchnessCleanResources <<= (sketchnessResources, streams) map {
		(resources, streams) => resources foreach { _.clean(streams.log) }
	}

	val main = play.Project(appName, appVersion, appDependencies).settings(
		// Add your own project settings here
		sketchnessResourcesSetting,
		sketchnessGetResourcesTask,
		sketchnessCleanResourcesTask
	)

}
