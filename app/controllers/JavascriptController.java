package controllers;

import java.io.InputStream;
import java.util.NoSuchElementException;
import play.Logger;
import play.mvc.*;

import play.i18n.Lang;



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
}