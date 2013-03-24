package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import play.Logger;
import play.mvc.*;

import play.i18n.Lang;



public class JavaScriptController extends Controller {
    public static Result i18n(){
        Lang l = request().acceptLanguages().get(0);
        String properties = "";
        // you can also use commons-io
        try(InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("messages." + l.language())){
            properties = new java.util.Scanner(in).useDelimiter("\\A").next();
        } catch (NoSuchElementException | IOException e) {
            Logger.error("Failed to read messages file");
        }
        return ok(properties).as("text/plain");
    }
}