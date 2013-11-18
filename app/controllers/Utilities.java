package controllers;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import models.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONException;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import static play.mvc.Results.ok;
import play.mvc.WebSocket;
import utils.LoggerUtils;
import utils.Renderer;
import views.html.renderer;
import views.html.newRenderer;


import play.mvc.Http.Session;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class Utilities extends Controller {

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

    public static Result newRendererCall(){
        return ok(newRenderer.render());

    }

    public static Result webToolAjaxCall()throws JSONException {
        String result = Renderer.webToolAjax();
        return ok(result);
    }

    public static Result taskSelectionCall()throws JSONException{
        String result = Renderer.taskSelection();
        return ok(result);
    }

    public static Result loadStatsCall()throws JSONException {
        String result = Renderer.loadStats();
        return ok(result);
    }

    public static Result webInfoAjaxCall()throws JSONException {
        String selectedImg = request().getHeader("selected");
        String result = Renderer.webInfoAjax(selectedImg);
        return ok(result);
    }

    public static Result webTaskAjaxCall()throws JSONException {
        String selectedTask = request().getHeader("selected");
        String result = Renderer.webInfoTask(selectedTask);
        return ok(result);
    }

    public static Result closeTaskCall()throws IOException {
        String selectionTask = request().getHeader("selectionTask");
        Renderer.closeTask(selectionTask);
        String result="ok";
        return ok(result);
    }

    public static Result addTaskCall() throws IOException, JSONException {
        String newId;
        String taskType = request().getHeader("taskType");
        String selectionimg = request().getHeader("selectionimg");
        newId = Renderer.addTask(taskType,selectionimg);
        return ok(newId);
    }

    public static Result addUTaskCall() throws IOException, JSONException {
        String newId;
        String taskType = request().getHeader("taskType");
        String selectionTask = request().getHeader("selectionTask");
        newId = Renderer.addUTask(taskType,selectionTask);
        return ok(newId);
    }

    public static Result loadFirstGraphCall() throws JSONException{
        String result = Renderer.loadFirstGraph();
        return ok(result);
    }

    public static Result loadSecondGraphCall() throws JSONException{
        String result = Renderer.loadSecondGraph();
        return ok(result);
    }

    public static WebSocket<JsonNode> rendererStream(final String imageID)throws JSONException {

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
