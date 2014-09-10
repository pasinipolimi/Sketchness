package controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import utils.LoggerUtils;
import utils.Renderer;
import utils.CMS.CMS;
import utils.CMS.CMSException;
import views.html.admin_home;
import views.html.renderer;
import views.html.systemInfo;
import views.html.tasks;
import views.html.userInfo;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class Utilities extends Controller {

	public static Result retrieveMask(final String imageID, final String tag) {
		try {
			final File result = Renderer.retrieveMask(imageID, tag);
			if (null != result) {
				return ok(result);
			} else {
				return ok("[AGGREGATOR] Cannot retrieve the mask for image "
						+ imageID + " and tag: " + tag);
			}
		} catch (final Exception e) {
			return ok("[AGGREGATOR] " + e.toString());
		}
	}

	public static Result testCMS() {
		try {
			CMS.test();
		} catch (final CMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ok("ok");
	}

	public static Result cleanCMS() {
		try {
			CMS.cleanOpenActions();
		} catch (final CMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ok("ok");
	}

	public static Result retrieveMaskImage(final String media) throws CMSException {
		
		try {
			final BufferedImage img = Renderer.retrieveMaskImage(media);
			final File result = new File("Mask_.png");
			ImageIO.write(img, "png", result);
			if (null != result) {
				return ok(result);
			} else {
				return ok("[AGGREGATOR] Cannot retrieve the mask");
			}
		} catch (final Exception e) {
			return ok("[AGGREGATOR] " + e.toString());
		}
	}


	public static Result retrieveTags(final String imageID) {
		try {
			final JsonNode result = Renderer.retrieveTags(imageID);
			if (null != result) {
				return ok(result);
			} else {
				return ok("[AGGREGATOR] Cannot retrieve the tags for image "
						+ imageID);
			}
		} catch (final Exception e) {
			return ok("[AGGREGATOR] " + e.toString());
		}
	}

	public static Result retrieveImages() {
		try {
			final JsonNode result = Renderer.retrieveImages();
			if (null != result) {
				return ok(result);
			} else {
				return ok("[AGGREGATOR] Cannot retrieve the images");
			}
		} catch (final Exception e) {
			return ok("[AGGREGATOR] " + e.toString());
		}
	}

	public static Result renderer(final String imageID) {
		return ok(renderer.render(imageID));
	}

	public static Result newRendererCall() {
		//return ok(newRenderer.render());
		return ok(admin_home.render());

	}

	public static Result webToolAjaxCall() throws JSONException, CMSException {

		final String max_id = request().getHeader("max_id");
		final String count = request().getHeader("count");
		final String result = Renderer.webToolAjax(max_id, count);
		return ok(result);
	}

	public static Result taskSelectionCall() throws JSONException {

		final String result = Renderer.taskSelection();
		return ok(result);
	}

	public static Result loadStatsCall() throws JSONException {
		final String result = Renderer.loadStats();
		return ok(result);
	}

	public static Result usersStatsAjaxCall() throws JSONException {
		final String result = Renderer.loadUsersStats();
		return ok(result);
	}


	public static Result webInfoAjaxCall() throws JSONException {
		final String selectedImg = request().getHeader("selected");
		final String result = Renderer.webInfoAjax(selectedImg);
		return ok(result);
	}

	public static Result webTaskAjaxCall() throws JSONException {
		final String selectedTask = request().getHeader("selected");
		final String result = Renderer.webInfoTask(selectedTask);
		return ok(result);
	}

	public static Result closeTaskCall() throws IOException, CMSException {
		final String selectionTask = request().getHeader("selectionTask");
		Renderer.closeTask(selectionTask);
		final String result = "ok";
		return ok(result);
	}

	public static Result addTaskCall() throws IOException, JSONException,
	CMSException {
		String newId;
		final String taskType = request().getHeader("taskType");
		final String selectionimg = request().getHeader("selectionimg");
		newId = Renderer.addTask(taskType, selectionimg);
		return ok(newId);
	}

	public static Result addUTaskCall() throws IOException, JSONException,
	CMSException {
		String newId;
		final String taskType = request().getHeader("taskType");
		final String selectionTask = request().getHeader("selectionTask");
		newId = Renderer.addUTask(taskType, selectionTask);
		return ok(newId);
	}

	public static Result loadFirstGraphCall() throws JSONException {
		final String result = Renderer.loadFirstGraph();
		return ok(result);
	}

	public static Result loadSecondGraphCall() throws JSONException {
		final String result = Renderer.loadSecondGraph();
		return ok(result);
	}

	public static Result downloadStats1Call() throws JSONException {
		final String result = Renderer.downloadStats1();
		return ok(result);
	}

	public static Result downloadStats2Call() throws JSONException {
		final String result = Renderer.downloadStats2();
		return ok(result);
	}

	public static WebSocket<JsonNode> rendererStream(final String imageID)
			throws JSONException {

		return new WebSocket<JsonNode>() {
			@Override
			public void onReady(final WebSocket.In<JsonNode> in,
					final WebSocket.Out<JsonNode> out) {
				try {
					Renderer.createRenderer(imageID, in, out);
				} catch (final Exception e) {
					LoggerUtils.error("AGGREGATOR", e);
				}
			}
		};
	}

	public static Result invalidateTag() throws IOException, CMSException {
		final String tagId = request().getHeader("tagId");
		// TODO aff imageId in the UI!!
		final String imageId = request().getHeader("imageId");
		Renderer.invalidateTag(tagId, imageId);
		final String result = "ok";
		return ok(result);
	}

	public static Result tasksPageCall() {
		return ok(tasks.render());
	}

	public static Result systemInfoPageCall() {
		return ok(systemInfo.render());
	}

	public static Result usersInfoPageCall() {
		return ok(userInfo.render());
	}

	public static Result collectionAjaxCall() throws JSONException {
		final String result = Renderer.collectionAjaxCall();
		return ok(result);
	}

	public static Result collectionImagesAjaxCall() throws JSONException, CMSException {
		final String collectionId = request().getHeader("selected");
		final String result = Renderer.collectionImagesAjaxCall(collectionId);
		return ok(result);
	}

	public static Result maskAjaxCall() throws JSONException, CMSException {

		final String imageId = request().getHeader("idImage");
		final String tagName = request().getHeader("tag");
		final String tagId = String.valueOf(CMS.getTagId(tagName));
		final String result = Renderer.maskAjaxCall(imageId,tagId);
		return ok(result);

	}

	public static Result maskFashionistaAjaxCall() throws JSONException {
		final String imageId = request().getHeader("idImage");
		final String tagName = request().getHeader("tagName");
		final String result = Renderer.maskFashionistaAjaxCall(imageId,tagName);
		return ok(result);
	}
	
	public static Result initializeSlider() throws JSONException, ClientProtocolException, IOException, CMSException {
		
		final String result = Renderer.initializeSlider();
		return ok(result);
	}
	
	public static Result annotationRange() throws JSONException, ClientProtocolException, IOException, CMSException {
		
		final String max_id = request().getHeader("max_id");
		final String count = request().getHeader("count");
		final String max = request().getHeader("max");
		final String min = request().getHeader("min");
		final String result = Renderer.annotationRange(min,max,max_id,count);
		return ok(result);
	}
	

	public static Result getTraces() throws JSONException, CMSException {
		
		final String imageId = request().getHeader("idImage");
		final String name = request().getHeader("tagName");
		final String tagId = String.valueOf(CMS.getTagId(name));
		final String result = Renderer.getTraces(imageId,tagId);
		return ok(result);


	}		




}
