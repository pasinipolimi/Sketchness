package controllers;

import java.io.InputStream;
import java.util.NoSuchElementException;
import play.Logger;
import play.Routes;
import play.mvc.*;

import play.i18n.Lang;
import static play.mvc.Controller.flash;
import static play.mvc.Results.ok;
import views.html.leaderboard;



public class JavascriptController extends Controller {
    public static Result i18n(){
        Lang l = request().acceptLanguages().get(0);
        String properties = "";
        try{
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("messages." + l.language());
            properties = new java.util.Scanner(in).useDelimiter("\\A").next();
        } catch (NoSuchElementException  e) {
            Logger.error("[SYSTEM] Failed to read messages file");
        }
        return ok(properties).as("text/plain");
    }

    public static Result javascriptRoutes() {
    response().setContentType("text/javascript");
    return ok(
      Routes.javascriptRouter("jsRoutes",
        // Routes
        controllers.routes.javascript.Sketchness.leaderboard()
      )
    );
  }
    
}