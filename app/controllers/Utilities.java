package controllers;

import java.io.File;
import org.codehaus.jackson.JsonNode;
import play.mvc.Result;
import static play.mvc.Results.ok;
import play.mvc.WebSocket;
import utils.Renderer;
import views.html.renderer;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class Utilities {
    
    public static Result retrieveMask(final String imageID) {
        try{
            File result=Renderer.retrieveMask(imageID);
            if(null!=result)
                return ok(result);
            else
                return ok("[SKETCHNESS] Cannot retrieve the mask for image "+imageID);
        }
        catch(Exception e)
        {
            return ok("[SKETCHNESS] "+e.toString());
        }
    }
    
    public static Result renderer(final String imageID) {
            return ok(renderer.render(imageID));
        }
        
    public static WebSocket<JsonNode> rendererStream(final String imageID) {  
        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
                try{
                    Renderer.createRenderer(imageID, in, out);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
