package controllers;

import java.io.File;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import models.User;
import org.codehaus.jackson.JsonNode;
import play.db.DB;
import play.mvc.Http;
import play.mvc.Result;
import static play.mvc.Results.ok;
import play.mvc.WebSocket;
import utils.LoggerUtils;
import utils.Renderer;
import views.html.renderer;
import views.html.newRender;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class Utilities {

    public static Result retrieveMask(final String imageID, final String tag) {
        try {
            File result = Renderer.retrieveMask(imageID, tag);
            if (null != result) {
                return ok(result);
            } else {
                return ok("[AGGREGATOR] Cannot retrieve the mask for image " + imageID + " and tag " + tag);
            }
        } catch (Exception e) {
            return ok("[AGGREGATOR] " + e.toString());
        }
    }

    public static Result retrieveTags(final String imageID) {
        try {
            JsonNode result = Renderer.retrieveTags(imageID);
            if (null != result) {
                return ok(result);
            } else {
                return ok("[AGGREGATOR] Cannot retrieve the tags for image " + imageID);
            }
        } catch (Exception e) {
            return ok("[AGGREGATOR] " + e.toString());
        }
    }

    public static Result retrieveImages() {
        try {
            JsonNode result = Renderer.retrieveImages();
            if (null != result) {
                return ok(result);
            } else {
                return ok("[AGGREGATOR] Cannot retrieve the images");
            }
        } catch (Exception e) {
            return ok("[AGGREGATOR] " + e.toString());
        }
    }

    public static Result renderer(final String imageID) {
        return ok(renderer.render(imageID));
    }

    public static Result newRenderer(){
        return ok();
    }


    public static WebSocket<JsonNode> rendererStream(final String imageID) {
        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
                try {
                    Renderer.createRenderer(imageID, in, out);
                } catch (Exception e) {
                    LoggerUtils.error("AGGREGATOR", e);
                }
            }
        };
    }
}
