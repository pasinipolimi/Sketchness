import sbt._
import sbt.Keys._
import play.Project._

object ApplicationBuild extends Build {

	val appName         = "Sketchness"
	val appVersion      = "1.0-SNAPSHOT"

	
	val appDependencies = Seq(
		// Add your project dependencies here,
		javaCore,
		javaJdbc,
		javaEbean,
		"org.json" % "json" % "20090211",
		"gov.nih.imagej" % "imagej" % "1.47",
		"net.sf.json-lib" % "json-lib" % "2.4" classifier "jdk15",
		"be.objectify"  %%  "deadbolt-java"     % "2.2.1-RC2",
		"com.feth"      %%  "play-authenticate" % "0.5.3-SNAPSHOT"
	)

	/** Defines a new setting key that contains the resources list */
	lazy val sketchnessResources = SettingKey[Seq[Resource]](
		"sketchness-resources",
		"The remote resources handled outside del VCS.")

	val sketchnessResourcesSetting = sketchnessResources := Seq(
		// Add here the required resources
		// Resource(name: String, path :String, url :String = null, zipped :Boolean = true)
		Resource("Audio Folder", "public/sounds"),
		Resource("RequireJS", "public/javascripts/lib/require.js", "http://requirejs.org/docs/release/2.1.8/minified/require.js", false),
		Resource("PaperJS", "public/javascripts/lib/paper.js", "https://raw.github.com/paperjs/paper.js/v0.9.9/dist/paper-full.js", false),
		Resource("JS State Machine", "public/javascripts/lib/state-machine.js", "https://raw.github.com/jakesgordon/javascript-state-machine/master/state-machine.js", false),
		Resource("jQuery", "public/javascripts/lib/jquery.js", "http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js", false),
		Resource("jQuery map", "public/javascripts/lib/jquery.min.map", "http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.map", false),
		Resource("jQuery i18n", "public/javascripts/lib/jquery.i18n.properties.js", "http://jquery-i18n-properties.googlecode.com/files/jquery.i18n.properties-min-1.0.9.js", false),
		Resource("jScrollPane", "public/javascripts/lib/jquery.jscrollpane.js", "http://jscrollpane.kelvinluck.com/script/jquery.jscrollpane.min.js", false),
		Resource("jScrollPane MouseWheel", "public/javascripts/lib/jquery.mousewheel.js", "http://jscrollpane.kelvinluck.com/script/jquery.mousewheel.js", false),
		Resource("jScrollPane MouseWheelIntent", "public/javascripts/lib/mwheelIntent.js", "http://jscrollpane.kelvinluck.com/script/mwheelIntent.js", false),
		Resource("popUpjs","public/javascripts/lib/popUp.js","https://raw.github.com/Toddish/Popup/master/assets/js/jquery.popup.min.js",false),
		Resource("popUpCSS","public/stylesheets/lib/popUp.css","https://raw.github.com/Toddish/Popup/master/assets/css/popup.css",false),
		Resource("jScrollPane CSS", "public/stylesheets/lib/jquery.jscrollpane.css", "http://jscrollpane.kelvinluck.com/style/jquery.jscrollpane.css", false),
		Resource("Bootstrap CSS", "public/stylesheets/lib/bootstrap.css", "https://raw.github.com/twbs/bootstrap/master/dist/css/bootstrap.css", false),
		Resource("Bootstrap CSS Map", "public/stylesheets/lib/bootstrap.css.map", "https://raw.github.com/twbs/bootstrap/master/dist/css/bootstrap.css.map", false),
		Resource("Bootstrap Js", "public/javascripts/lib/bootstrap.min.js","https://raw.github.com/twbs/bootstrap/master/dist/js/bootstrap.min.js",false),
		Resource("Howler Js", "public/javascripts/lib/howler.js","https://raw.github.com/goldfire/howler.js/master/howler.min.js",false),
		Resource("jQuery UI", "public/javascripts/lib/jquery-ui.js", "http://code.jquery.com/ui/1.11.1/jquery-ui.min.js", false),
		Resource("noUISlider", "public/javascripts/lib/nouislider.js","https://raw.githubusercontent.com/leongersen/noUiSlider/6.2.0/jquery.nouislider.js",false),
		Resource("noUISlider Link", "public/javascripts/lib/Link.js","https://raw.githubusercontent.com/leongersen/noUiSlider/6.2.0/Link.js",false),
		Resource("noUISlider CSS", "public/stylesheets/lib/nouislider.css", "https://raw.github.com/leongersen/noUiSlider/6.2.0/jquery.nouislider.css", false),
		Resource("Spectrum", "public/javascripts/lib/spectrum.js", "https://raw.github.com/bgrins/spectrum/master/spectrum.js", false),
		Resource("Spectrum JS", "public/stylesheets/lib/spectrum.css", "https://raw.github.com/bgrins/spectrum/master/spectrum.css", false),
		Resource("Modernizr", "public/javascripts/lib/modernizr.custom.js", "https://raw.githubusercontent.com/codrops/Blueprint-ResponsiveFullWidthGrid/master/js/modernizr.custom.js", false),
		//Zipped files with assets: images and sounds
		Resource("Libraries", "/", "http://54.75.252.197/storage/libs.zip", true),
		Resource("Website Images", "public/images", "http://54.75.252.197/storage/spritesheets.zip",true),
		Resource("Website Sound Effects", "public/sounds/effects", "http://54.75.252.197/storage/effects.zip",true)
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
		sketchnessCleanResourcesTask,
		resolvers += "MavenHub repository" at "http://repo1.maven.org/maven2",
		
		resolvers += Resolver.url("Objectify Play Repository (release)", url("http://schaloner.github.com/releases/"))(Resolver.ivyStylePatterns),
        resolvers += Resolver.url("Objectify Play Repository (snapshot)", url("http://schaloner.github.com/snapshots/"))(Resolver.ivyStylePatterns),

        resolvers += Resolver.url("play-easymail (release)", url("http://joscha.github.com/play-easymail/repo/releases/"))(Resolver.ivyStylePatterns),
        resolvers += Resolver.url("play-easymail (snapshot)", url("http://joscha.github.com/play-easymail/repo/snapshots/"))(Resolver.ivyStylePatterns),

        resolvers += Resolver.url("play-authenticate (release)", url("http://joscha.github.com/play-authenticate/repo/releases/"))(Resolver.ivyStylePatterns),
        resolvers += Resolver.url("play-authenticate (snapshot)", url("http://joscha.github.com/play-authenticate/repo/snapshots/"))(Resolver.ivyStylePatterns)
    )

}
