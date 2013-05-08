package controllers;

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
