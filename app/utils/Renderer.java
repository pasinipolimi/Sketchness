package utils;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import play.libs.F.Promise;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.CMS.CMS;
import utils.CMS.CMSException;
import utils.CMS.CMSJsonReader;
import utils.CMS.CMSUtilities;
import utils.CMS.SortObject;
import utils.CMS.models.Action;
import utils.CMS.models.Collection;
import utils.CMS.models.Mask;
import utils.CMS.models.MicroTask;
import utils.CMS.models.Point;
import utils.CMS.models.Task;
import utils.CMS.models.User;
import utils.aggregator.ContourAggregator;
import utils.gamebus.GameMessages.Join;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import play.libs.Json;
import utils.CMS.SortObject;
import utils.CMS.SortObjectAction;
import utils.CMS.models.Pose;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

import compgeom.RLineSegment2D;
import compgeom.RPoint2D;
import compgeom.algorithms.BentleyOttmann;


public class Renderer extends UntypedActor {

	private final static String rootUrl = Play.application().configuration().getString("cmsUrl");
	//private final static String rootUrl = "http://localhost:3000";
	private final static Integer timeout = Play.application().configuration().getInt("cmsTimeout");
	private WebSocket.Out<JsonNode> channel;
	String imageId;

	public static synchronized void createRenderer(final String imageID,
			final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out)
					throws Exception {

		final Props properties = new Props(Renderer.class);
		final ActorRef finalRoom = Akka.system().actorOf(properties);
		final Future<Object> future = Patterns.ask(finalRoom, new Join(imageID,
				out), 1000000000);
		// Send the Join message to the room
		final String result = (String) Await.result(future,
				Duration.create(10, SECONDS));

		if ("OK".equals(result)) {
			// in: handle messages from the painter
			in.onMessage(new F.Callback<JsonNode>() {
				@Override
				public void invoke(final JsonNode json) throws Throwable {
					if (!json.get("type").asText().equals("task")) {
						finalRoom.tell(json, finalRoom);
					}
				}
			});

			// User has disconnected.
			in.onClose(new F.Callback0() {
				@Override
				public void invoke() throws Throwable {
				}
			});
		}
	}

	// Manage the messages
	@Override
	public void onReceive(final Object message) throws Exception {
		if (message instanceof Join) {
			start((Join) message);
		} else if (message instanceof JsonNode) {
			aggregate(true, ((JsonNode) message).get("tag").asText(), false);
		} else if (message instanceof MaskParameters) {
			createMask(((MaskParameters) message).getId(),
					((MaskParameters) message).getTag());
		}
	}

	private void start(final Join message) throws IOException, Exception {

		final Join join = message;
		imageId = join.getUsername();
		channel = join.getChannel();
		getSender().tell("OK", this.getSelf());
	}

	private Image aggregate(final boolean networkOn, final String requiredTag,
			final boolean transparentMask) throws IOException, Exception {
		// final JavascriptColor[] colors = JavascriptColor.values();
		//final JsonReader jsonReader = new JsonReader();
		//
		final utils.CMS.models.Image image = CMS.getImage(Integer.valueOf(imageId));
		final Integer imWidth = image.getWidth();
		final Integer imHeight = image.getHeight();
		final Integer tagId = CMS.getTagId(requiredTag);
		//final List<Action> actions = CMS.getSegmentationsByImageAndTag(Integer.valueOf(imageId), tagId);
		final List<Action> actions = CMS.getBestSegmentation(Integer.valueOf(imageId), tagId);
		Image toReturn = null;
		Boolean found = false;
		if (actions != null && actions.size() > 0) {
		 final int numberTraces = actions.size();
		 final ArrayList<String> toAggregate = new ArrayList<>();
		 for (int i = 0; i < numberTraces; i++) {
		// final JavascriptColor c = colors[i % colors.length];
			 final Action action = actions.get(i);
			 Integer action_id = action.getId();
			 Action current = CMS.getAction(action_id);
			 final Integer tag = current.getTag();
			 found = true;
		 	 List<Point> p = current.getSegmentation().getPoints();
			 /*final ObjectMapper mapper = new ObjectMapper();
			 final JsonFactory factory = mapper.getJsonFactory();
			 final String toParse = p.toString().replace("\\", "");
			 final JsonParser jp = factory.createJsonParser(toParse);
			 final JsonNode actualObj = mapper.readTree(jp);*/
		 	 JSONObject element = null;
		 	final JSONArray actualObj = new JSONArray();
		 	for(int j=0;j<p.size();j++){
		 		element = new JSONObject();
		 		element.put("x", p.get(j).getX());
		 		element.put("y", p.get(j).getY());
		 		actualObj.put(element);
		 	}
			 toAggregate.add(actualObj.toString());
		}
		if (found && !networkOn) {
			final String[] toAggregateString = new String[toAggregate.size()];
			for (int i = 0; i < toAggregate.size(); i++) {
				toAggregateString[i] = toAggregate.get(i);
			}
			toReturn = ContourAggregator.simpleAggregator(toAggregateString, imWidth, imHeight);
			if (transparentMask) {
				final BufferedImage transparent = imageToBufferedImage(toReturn);
				toReturn = makeColorTransparent(transparent, Color.WHITE);
			}
		} else if (!networkOn) {
			LoggerUtils.error("[AGGREGATOR] Cannot retrieve mask for image "+ imageId + " with tag " + requiredTag,"NOT FOUND");
			throw new Exception("[AGGREGATOR] Tag " + requiredTag+ " not found for image " + imageId);
		}
	}
		return toReturn;
		//return null;
	}

	private static BufferedImage imageToBufferedImage(final Image image) {

		final BufferedImage bufferedImage = new BufferedImage(
				image.getWidth(null), image.getHeight(null),
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = bufferedImage.createGraphics();
		g2.drawImage(image, 0, 0, null);
		g2.dispose();

		return bufferedImage;

	}

	public static Image makeColorTransparent(final BufferedImage im,
			final Color color) {
		final ImageFilter filter = new RGBImageFilter() {

			// the color we are looking for... Alpha bits are set to opaque
			public int markerRGB = color.getRGB() | 0xFF000000;

			@Override
			public final int filterRGB(final int x, final int y, final int rgb) {
				if ((rgb | 0xFF000000) == markerRGB) {
					// Mark the alpha bits as zero - transparent
					return 0x00FFFFFF & rgb;
				} else {
					// nothing to do
					return 0xAAFFFFFF & rgb;
				}
			}
		};

		final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}

	public static synchronized File retrieveMask(final String ImageID,
			final String tag) throws Exception {
		final Props properties = new Props(Renderer.class);
		final ActorRef finalRoom = Akka.system().actorOf(properties);
		final Future<Object> future = Patterns.ask(finalRoom,
				new MaskParameters(ImageID, tag), 1000000000);
		// Send the Join message to the room
		final File result = (File) Await.result(future,
				Duration.create(180, SECONDS));
		if (result instanceof File) {
			LoggerUtils.info("AGGREGATOR", "Retrieved mask for image "
					+ ImageID);
			return result;
		}
		LoggerUtils.debug("AGGREGATOR", "Retrieved mask for image " + ImageID);
		return null;
	}

	public static BufferedImage retrieveMaskImage(final String media) throws Exception {

		BufferedImage img = null;
		Logger.info(media);
		try {
			final URL url = new URL(media);
			img = ImageIO.read(url);
		} catch (final IOException e) {
			Logger.error("[AGGREGATOR] " + e);
		}

		if (img instanceof Image) {
			Logger.info("[AGGREGATOR1] Retrieved mask");
			return img;
		}
		Logger.error("[AGGREGATOR2] Retrieved mask");
		return null;
	}

	// non la usa nessuno, ignoro
	public static synchronized JsonNode retrieveImages() throws Exception {

		final JsonReader jsonReader = new JsonReader();
		return jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image.json");
	}

	public static synchronized JsonNode retrieveTags(final String imageID)
			throws Exception {

		final ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
		final HashSet<String> tags = CMSUtilities.getTags(imageID);

		for (final String tag : tags) {
			result.add(tag);
		}
		return result;
	}

	private void createMask(final String id, final String tag)
			throws IOException, Exception {
		LoggerUtils.info("AGGREGATOR", "Retrieving aggregated mask for image "
				+ id);
		try {
			imageId = id;
			final Image retrieved = aggregate(false, tag, true);
			if (null != retrieved) {
				final BufferedImage bufIm = imageToBufferedImage(retrieved);
				ImageIO.write(bufIm, "png", new File(imageId + ".png"));
				final File toReturn = new File(imageId + ".png");
				getSender().tell(toReturn, this.getSelf());
			} else {
				//getSender().tell(null, this.getSelf());
			}
		} catch (final Exception e) {
			//getSender().tell(null, this.getSelf());
		}
	}

	/**
	 * Return the list of images' ids and task' ids in the system
	 * 
	 * @return the list of images' ids and task' ids in the system
	 * @throws JSONException
	 * @throws CMSException 
	 */
	public static String webToolAjax(final String max_id, final String count) throws JSONException, CMSException {

		List<utils.CMS.models.Image> images;
		final List<utils.CMS.models.Task> tasks;
		final HashMap<String, String> params = new HashMap<String,String>();

		if(!max_id.equals("null")){
			params.put("max_id", max_id);
			params.put("count", count);
		}
	
		try {
			images = CMS.getObjs(utils.CMS.models.Image.class, "image", params, "images");
			tasks = CMS.getTasks();
		} catch (final CMSException e) {
			Logger.error("Unable to read images and tasks from cms", e);
			throw new JSONException("Unable to read images and tasks from cms");
		}

		JSONArray imagesJ = new JSONArray();

		JSONArray tasksJ = new JSONArray();
		JSONObject element;
		final JSONObject result = new JSONObject();
		boolean check = false;

		if (images != null && images.size() > 0) {
			imagesJ = CMSUtilities.buildImageId(images);
		} else {
			element = new JSONObject();
			element.append("id", "No photos in the system");
			imagesJ.put(element);
		}
		if (tasks != null && tasks.size() > 0) {
			tasksJ = CMSUtilities.buildTaskIds(tasks);
			check = true;
		} else {
			element = new JSONObject();
			element.append("id", "No Tasks in the system");
			element.append("taskType", "");
			tasksJ.put(element);
		}
		result.append("image", imagesJ);
		result.append("task", tasksJ);
		result.append("check", check);
		
		//GET next result: max_id, count
		final String service = "image";
		final String response = "search_metadata";
		
		Promise<WS.Response> res;
		final WSRequestHolder wsurl = WS.url(rootUrl + "/" + service).setHeader("Accept", "application/json").setTimeout(timeout);
		if (params != null) {
				final Iterator<Entry<String, String>> it = params.entrySet()
						.iterator();
				while (it.hasNext()) {
					final Map.Entry<java.lang.String, java.lang.String> param = it
							.next();
					wsurl.setQueryParameter(param.getKey(), param.getValue());
				}
		}

		res = wsurl.get();
		String new_max_id = "";
		String new_count = "";
		if (res != null) {
			final Response result2 = res.get(1000000L);
			final JsonNode json = result2.asJson();
			// with a system out i can see that the json is parsed correctly
			JsonNode node = null;
			if (json.get("status").asText().equals("OK")) {
				node = json.get(response);
				if(node.has("next_results")){
					String nextResult = node.get("next_results").asText();
					String[] tokens = nextResult.split("=");
					new_max_id = tokens[1].split("&")[0];
					new_count = tokens[2];
				}
			} else {
				throw new CMSException(
						"Internal Server Error while invoking CMS: "
								+ json.get("error"));
			}
		} else {
			throw new IllegalStateException("CMS response timeout.");
		}
		result.append("max_id", new_max_id);
		result.append("count", new_count);

		final String options = result.toString();
		return options;
	}

	/**
	 * Return the list of task' ids in the system (called only after a new task
	 * is added in order to refresh the list of tasks without refreshing the
	 * list of images
	 * 
	 * @return the list of task' ids in the system
	 * @throws JSONException
	 */
	public static String taskSelection() throws JSONException {
		List<Task> tasks = null;
		try {
			tasks = CMS.getTasks();
		} catch (final CMSException e) {
			Logger.error("Unable to read tasks from CMS", e);
			throw new JSONException("");
		}

		boolean check = false;
		JSONArray tasksJ = new JSONArray();
		JSONObject element;
		final JSONObject result = new JSONObject();

		if (tasks != null && tasks.size() > 0) {
			tasksJ = CMSUtilities.retriveTaskId(tasks);
			check = true;
		} else {
			element = new JSONObject();
			element.append("id", "No Tasks in the system");
			element.append("taskType", "");
			tasksJ.put(element);
		}

		result.append("task", tasks);
		result.append("check", check);
		final String options = result.toString();
		return options;
	}

	/**
	 * Load the stats of the system
	 * 
	 * @return Number of images, avg of tags per image, number of segments, avg
	 *         of segments for image
	 * @throws JSONException
	 */
	public static String loadStats() throws JSONException {

		Integer numImages;
		final Integer userCount;
		try {
			numImages = CMS.getImageCount();
			userCount = CMS.getUserCount();
		} catch (final CMSException e) {
			Logger.error("Unable to read stats", e);
			throw new net.sf.json.JSONException();
		}
		

		final JSONObject result = new JSONObject();

		Integer numTags;
		Integer numSegs;
		try {
			numTags = CMS.getTagActionCount();
			numSegs = CMS.getSegActionCount();
		} catch (final CMSException e) {
			Logger.error("Unable to read actions from CMS", e);
			throw new JSONException("Unable to read actions from CMS");
		}

		final float mediaTagImg = (float) numTags / numImages;
		final float mediaSegImg = (float) numSegs / numImages;

		result.append("totImg", String.valueOf(numImages));
		result.append("mediaTag", Float.toString(mediaTagImg));
		result.append("numSegment", Integer.toString(numSegs));
		result.append("mediaSegImg", Float.toString(mediaSegImg));
		result.append("numberUsers", String.valueOf(userCount));
		final String sendStats = result.toString();
		return sendStats;
	}

	/**
	 * Load the stats of the users
	 * 
	 * @return name, number of annotations, number of plays, quality
	 * @throws JSONException
	 */
	public static String loadUsersStats() throws JSONException {
		final List<User> users;
		try {
			users = CMS.getUsers();
		} catch (final CMSException e) {
			Logger.error("Unable to read users from CMS", e);
			throw new JSONException("Unable to read users from CMS");
		}

		final JSONObject result = new JSONObject();

		final JSONArray usersInfo = new JSONArray();
		JSONObject element;
		final JsonNode object;

		final int i = 0;

		for (final User u : users) {
			element = new JSONObject();
			// TODO
			// element.put("cubrik_userid",
			// object.get("cubrik_userid").toString());
			element.put("app_id", u.getApp_id());
			element.put("app_user_id", u.getApp_user_id());
			//element.put("quality", u.getQuality());
			element.put("quality", 0);
			element.put("num_actions", u.getStatistics().getActions());
			element.put("num_sessions", u.getStatistics().getSessions());
			// TODO
			// element.put("number_of_plays", CMS);
			// element.put("number_of_annotations",
			// object.get("number_of_annotations"));
			usersInfo.put(element);

		}

		result.append("usersInfo", usersInfo);
		final String sendStats = result.toString();
		return sendStats;

	}

	/**
	 * Load more details of a particular image such as the medialocator, the
	 * tags and the number of anotations
	 * 
	 * @param selection
	 *            the image that I want to analyse
	 * @return the info that i retrived
	 * @throws JSONException
	 */
	public static String webInfoAjax(final String selection)
			throws JSONException {


		final utils.CMS.models.Image image;
		try {
			image = CMS.getImage(Integer.parseInt(selection));
		} catch (final CMSException e) {
			Logger.error("Unable to read images from CMS", e);
			throw new net.sf.json.JSONException();
		}

		final String info = CMSUtilities.retriveImgInfo(image);

		return info;
	}

	/**
	 * Load more details of a particular mask such as the medialocator
	 * 
	 * @param imageId
	 *            the image that I want to analyse
	 * @param tagId
	 *            the tag that I want to analyse
	 * @return the info that i retrived
	 * @throws JSONException
	 */
	public static String maskAjaxCall(final String imageId, final String tagId)
			throws JSONException {

		Mask mask;
		try {
			mask = CMS.getMask(Integer.valueOf(imageId), Integer.valueOf(tagId));
		} catch (final Exception e) {
			Logger.error("Unable to read mask from CMS", e);
			throw new JSONException("Unable to read mask from CMS");
		}

		final String info = CMSUtilities.retriveMaskInfo(mask);
		return info;
	}

	/**
	 * Retrieve the file name of the fashionista mask, given imageId and tagId
	 * 
	 * @param imageId
	 *            the image that I want to analyse
	 * @param tagId
	 *            the tag that I want to analyse
	 * @return the image url
	 * @throws JSONException
	 */
	public static String maskFashionistaAjaxCall(final String imageId,
			final String tagName) throws JSONException {

		final JSONObject result = new JSONObject();
		final JSONArray info = new JSONArray();

		String url = "";
		String img = "";
		String tag = "";
		String qual = "";
		String temp = "";
		String quality = "";

		final java.io.File folder = play.Play.application().getFile(
				"public/images/fashionista");

		final File[] files = folder.listFiles();

		if (files != null) {
			for (final File file : files) {
				temp = file.getName();
				final StringTokenizer st = new StringTokenizer(temp, "_");
				final String mask = (String) st.nextElement();
				img = (String) st.nextElement();
				tag = (String) st.nextElement();
				qual = (String) st.nextElement();

				if (img.equals(imageId) && tag.equals(tagName)) {
					url = file.getName();
					final StringTokenizer st2 = new StringTokenizer(qual,
							".png");
					quality = (String) st2.nextElement();
				}
			}
		}

		result.append("url", url);
		result.append("quality", quality);
		info.put(result);
		return info.toString();

	}

	/**
	 * Load the list of microTask of a particular task
	 * 
	 * @param selection
	 *            the task that I want to analyse
	 * @return the info that i retrived
	 * @throws JSONException
	 */
	public static String webInfoTask(final String selection)
			throws JSONException {

		final String info = CMSUtilities.retriveTaskInfo(selection);
		return info;
	}

	/**
	 * Close a particular task
	 * 
	 * @param selection
	 *            the task that i want to close
	 * @throws IOException
	 */
	public static void closeTask(final String selection) throws CMSException {
		CMS.closeTask(Integer.valueOf(selection));
	}

	/**
	 * Invalidate tag
	 */
	public static void invalidateTag(final String tagid, final String imageId)
			throws CMSException {
		CMS.invalidateTag(tagid, imageId);
	}

	/**
	 * Add a new task to a specific image
	 * 
	 * @param taskType
	 *            the type of the task that I want to add (segmentation or
	 *            tagging)
	 * @param selectedImg
	 *            the image connected to the new task
	 * @return the id of the new task
	 * @throws IOException
	 * @throws JSONException
	 * @throws CMSException
	 */
	public static String addTask(final String taskType, final String selectedImg)
			throws IOException, JSONException, CMSException {

		final Integer info = CMS.addTask(taskType, selectedImg);
		return info.toString();
	}

	/**
	 * Add a new microTask to a specific task
	 * 
	 * @param taskType
	 *            the type of the microTask that I want to add (segmentation or
	 *            tagging)
	 * @param selectionTask
	 *            the task connected to the new microTask
	 * @return the id of the new microTask
	 * @throws IOException
	 * @throws JSONException
	 * @throws CMSException
	 */
	public static String addUTask(final String taskType,
			final String selectionTask) throws IOException, JSONException,
			CMSException {

		final Integer info = CMS.addUTask(taskType, selectionTask);
		return info.toString();
	}

	/**
	 * Retrive stats needed to deploy first graph: Number of images with X
	 * annotations
	 * 
	 * @return the string with data
	 * @throws JSONException
	 */
	public static String loadFirstGraph() throws JSONException {

		final JSONArray info = CMSUtilities.loadFirstGraph();
		final String result = info.toString();
		Logger.info(result);
		return result;
	}

	/**
	 * Retrive stats needed to deploy first graph: Number of users that have
	 * annotated X images
	 * 
	 * @return the string with data
	 * @throws JSONException
	 */
	public static String loadSecondGraph() throws JSONException {

		final JSONArray info = CMSUtilities.loadSecondGraph();
		final String result = info.toString();
		return result;
	}

	/**
	 * Generate a json file with User Data
	 * 
	 * @return the string with data
	 * @throws JSONException
	 */
	public static String downloadStats1() throws JSONException {

		final JSONArray info = CMSUtilities.downloadStats1();
		final String result = info.toString();
		return result;
	}

	/**
	 * Generate a json file with Images Data
	 * 
	 * @return the string with data
	 * @throws JSONException
	 */
	public static String downloadStats2() throws JSONException {

		final JSONArray info = CMSUtilities.downloadStats2();
		final String result = info.toString();
		return result;
	}

	/**
	 * Load the list of collections of images
	 * 
	 * @return the info that i retrived
	 * @throws JSONException
	 */
	public static String collectionAjaxCall() throws JSONException {

		List<Collection> cs;
		try {
			cs = CMS.getCollections();
		} catch (final CMSException e) {
			Logger.error("Unable to read collections from CMS", e);
			throw new net.sf.json.JSONException();
		}

		JSONArray collections = new JSONArray();
		collections = CMSUtilities.retriveCollectionInfo(cs);

		final JSONObject result = new JSONObject();
		result.append("collections", collections);
		final String options = result.toString();
		return options;

	}

	/**
	 * Load the images of a collection
	 * 
	 * @param collectionId
	 *            the id of the collection
	 * @return the info that i retrived
	 * @throws JSONException
	 * @throws CMSException 
	 */
	public static String collectionImagesAjaxCall(final String collectionId)
			throws JSONException, CMSException {

		final Collection c;
		try {
			c = CMS.getCollection(Integer.valueOf(collectionId));
		} catch (final CMSException e) {
			Logger.error("Unable to read collections from CMS", e);
			throw new net.sf.json.JSONException();
		}
		final String images = CMSUtilities.retriveCollImages(c);

		return images;
	}
	

	public static String initializeSlider()
			throws JSONException, ClientProtocolException, IOException, CMSException {

		final CMSJsonReader jsonReader = new CMSJsonReader();
		final JsonNode statistics = jsonReader.readJsonFromUrl2(rootUrl, "statistics", null, "statistics");
		JSONObject element = null;
		final JSONArray info = new JSONArray();
		
		if(statistics.has("actions")){
				element = new JSONObject();
				element.put("min", statistics.get("actions").get("segmentations_per_image").get("min"));
				element.put("max", statistics.get("actions").get("segmentations_per_image").get("max"));
			}

		info.put(element);
		return info.toString();
		

	}
	
	public static String annotationRange(final String min, final String max, String max_id, String count) throws JSONException, ClientProtocolException, IOException, CMSException {

		List<utils.CMS.models.Image> images;
		final HashMap<String, String> params = new HashMap<String,String>();

		if(!max_id.equals("null")){
			params.put("max_id", max_id);
			params.put("count", count);
		}
		params.put("max_segmentations", max);
		params.put("min_segmentations", min);
	
		try {
			images = CMS.getObjs(utils.CMS.models.Image.class, "image", params, "images");
		} catch (final CMSException e) {
			Logger.error("Unable to read images from cms", e);
			throw new JSONException("Unable to read images from cms");
		}

		JSONArray imagesJ = new JSONArray();
		JSONObject element;
		final JSONObject result = new JSONObject();

		if (images != null && images.size() > 0) {
			imagesJ = CMSUtilities.buildImageId(images);
		}
		
		result.append("image", imagesJ);
		
		//GET next result: max_id, count
		final String service = "image";
		final String response = "search_metadata";
		
		Promise<WS.Response> res;
		final WSRequestHolder wsurl = WS.url(rootUrl + "/" + service).setHeader("Accept", "application/json").setTimeout(timeout);
		if (params != null) {
				final Iterator<Entry<String, String>> it = params.entrySet()
						.iterator();
				while (it.hasNext()) {
					final Map.Entry<java.lang.String, java.lang.String> param = it
							.next();
					wsurl.setQueryParameter(param.getKey(), param.getValue());
				}
		}

		res = wsurl.get();
		String new_max_id = "";
		String new_count = "";
		if (res != null) {
			final Response result2 = res.get(1000000L);
			final JsonNode json = result2.asJson();
			// with a system out i can see that the json is parsed correctly
			JsonNode node = null;
			if (json.get("status").asText().equals("OK")) {
				node = json.get(response);
				if(node.has("next_results")){
					String nextResult = node.get("next_results").asText();
					String[] tokens = nextResult.split("=");
					new_max_id = tokens[1].split("&")[0];
					new_count = tokens[2];
				}
			} else {
				throw new CMSException(
						"Internal Server Error while invoking CMS: "
								+ json.get("error"));
			}
		} else {
			throw new IllegalStateException("CMS response timeout.");
		}
		result.append("max_id", new_max_id);
		result.append("count", new_count);

		final String options = result.toString();
		return options;
	}
	
	public static String getTraces(final String imageId, final String tagId)
			throws JSONException, CMSException {
		
		JSONArray result = new JSONArray();
		List<utils.CMS.models.Action> actions;
		try {
			actions = CMS.getBestSegmentation(Integer.valueOf(imageId),Integer.valueOf(tagId));
		} catch (final CMSException e) {
			Logger.error("Unable to read segmentation for image " + imageId, e);
			throw new JSONException("Unable to read segmentation from cms");
		}
		

		JSONObject element;
		JsonNode points = null;
		
		for (int j = 0; j < actions.size(); j++) {
			final utils.CMS.models.Action a = actions.get(j);
		    Integer action_id = a.getId();
		    utils.CMS.models.Action action = CMS.getAction(action_id);
			element = new JSONObject();
			points = Json.toJson(action.getSegmentation().getPoints());
			element.put("points", points);
			result.put(element);
		}

		return result.toString();
	
	}


/**
	 * Retrieve the best segmentation of an image
	 * 
	 * @param imageId
	 *            the id of the image
	 * @param tagId
	 *            the id of the tag
	 * @return the info that i retrieved
	 * @throws JSONException
	 * @throws CMSException 
	 */
	public static String segmentationImageCall(final String imageId, final String tagId)
			throws JSONException, CMSException {
		
		//choice of best segmentation among the "randomN" best ones
		int randomN = 3;
		
		JSONArray result = new JSONArray();
		List<utils.CMS.models.Action> actions;
		try {
			actions = CMS.getBestSegmentation(Integer.valueOf(imageId),Integer.valueOf(tagId));
		} catch (final CMSException e) {
			Logger.error("Unable to read segmentation for image " + imageId, e);
			throw new JSONException("Unable to read segmentation from cms");
		}
		
		JsonNode history = null;
		SortObjectAction sorting;
		final ArrayList<SortObjectAction> tempList = new ArrayList<>();

		if ((actions != null)&&(actions.size()>0)) {
			for (int j = 0; j < actions.size(); j++) {
				final utils.CMS.models.Action a = actions.get(j);
				if(a.getSegmentation().getQuality()!=null){
					sorting = new SortObjectAction() {};
					sorting.setQuality(a.getSegmentation().getQuality());
					sorting.setId(String.valueOf(a.getId()));
					tempList.add(sorting);
				}

			}

			Collections.sort(tempList, new Comparator<SortObjectAction>() {
				@Override
				public int compare(final SortObjectAction o1, final SortObjectAction o2) {
					if (o1.getQuality() > o2.getQuality()) {
						return -1;
					} else if (o1.getQuality() < o2.getQuality()) {
						return 1;
					}
					return 0;
				}

			});
			
			if(tempList.size()-1<randomN){
				randomN = tempList.size()-1;
			}
			
		    int randomNum = (int)(Math.random() * (randomN + 1));
			Logger.info("randomNum" + randomNum);
			Logger.info("actions" + tempList.size());
		    Integer action_id = Integer.valueOf(tempList.get(randomNum).getId());
		    utils.CMS.models.Action action = CMS.getAction(action_id);
			history = Json.toJson(action.getSegmentation().getHistory());
			JSONObject obj = new JSONObject();
			obj.put("history", history);
			utils.CMS.models.Image image = CMS.getImage(Integer.valueOf(imageId));
			obj.put("width", image.getWidth());
			obj.put("height", image.getHeight());
			result.put(obj);

		}
		
		return result.toString();
	}
	
	
	
	public static String retrievePoints(final JsonNode jsonPoints) throws JSONException {


		int i = 0;
		JsonNode object;
		Integer x,y;
		RPoint2D tempPoint;
		ArrayList<RPoint2D> vertices = new ArrayList<RPoint2D>();
		ArrayList<RPoint2D> finalVertices = new ArrayList<RPoint2D>();
		
		
		while (i < jsonPoints.size()) {
			object = jsonPoints.get(i);
			if (object.has("x")) {
				x = object.get("x").asInt();
				y = object.get("y").asInt();
				tempPoint = new RPoint2D(x,y);
				finalVertices.add(tempPoint);
				vertices.add(tempPoint);
			}
			i++;
		}// fine while
		
		RLineSegment2D seg;
		Set<RLineSegment2D> segments = new HashSet<RLineSegment2D>();
		
		for(int j=0;j<vertices.size()-1;j++){
			seg = new RLineSegment2D(vertices.get(j), vertices.get(j+1));
			segments.add(seg);
		}
		//add segment to close polygon
		seg = new RLineSegment2D(vertices.get(vertices.size()-1), vertices.get(0));
		segments.add(seg);

		
		Set<RLineSegment2D> segmentsInters;
		Map<RPoint2D, Set<RLineSegment2D>> map = BentleyOttmann.intersectionsMap(segments);
		
		String segment;
		RPoint2D point1,point2;
		
		final JSONObject result = new JSONObject();
		final JSONArray polygons = new JSONArray();
		JSONArray polygon = new JSONArray();
		JSONObject ver;
		//JSONObject first;
		RPoint2D vertex;
		String v;
		ArrayList<Integer> indices = new ArrayList<Integer>();

		for ( RPoint2D key : map.keySet() ) {
			segmentsInters = new HashSet<RLineSegment2D>();
			if(!vertices.contains(key)){
				segmentsInters = map.get(key);
				for(RLineSegment2D s : segmentsInters){
					segment = s.toString();
					//retrieve x,y
					String[] p = segment.split("\\(");
					String[] p1 = p[1].split(",");
					String x1 = p1[0];
					String[] p1y = p1[1].split("\\)");
					String y1 = p1y[0];
					String[] p2 = p[2].split(",");
					String x2 = p2[0];
					String[] p2y = p2[1].split("\\)");
					String y2 = p2y[0];
					point1 = new RPoint2D(Integer.parseInt(x1),Integer.parseInt(y1));
					point2 = new RPoint2D(Integer.parseInt(x2),Integer.parseInt(y2));
					for(int t=0;t<vertices.size()-1;t++){
						if((vertices.get(t).equals(point1))&&(vertices.get(t+1).equals(point2))){
							finalVertices.add(t+1,key);
							indices.add(t+1);
						}
					}

				}
			}
			
		}

		for(int j=0;j<indices.size();j++){
			if(j==0){
				polygon = new JSONArray();

				for(int k=0;k<=indices.get(j);k++){
					vertex = finalVertices.get(k);
					v = vertex.toString();
					String[] p = v.split("\\(");
					String[] p1 = p[1].split(",");
					String x1 = p1[0];
					String[] p1y = p1[1].split("\\)");
					String y1 = p1y[0];
					ver = new JSONObject();
					ver.put("x",x1);
					ver.put("y", y1);
					polygon.put(ver);
				
				}

				polygons.put(polygon);
				
			}
			else{
				polygon = new JSONArray();

				for(int k=indices.get(j-1);k<indices.get(j);k++){
					vertex = finalVertices.get(k);
					v = vertex.toString();
					String[] p = v.split("\\(");
					String[] p1 = p[1].split(",");
					String x1 = p1[0];
					String[] p1y = p1[1].split("\\)");
					String y1 = p1y[0];
					ver = new JSONObject();
					ver.put("x",x1);
					ver.put("y", y1);
					polygon.put(ver);
	
				}

				polygons.put(polygon);
			}
		}
		//add remaining points as last polygon
		polygon = new JSONArray();

		if(indices.size()!=0){

			for(int k=indices.get(indices.size()-1);k<finalVertices.size();k++){
				vertex = finalVertices.get(k);
				v = vertex.toString();
				String[] p = v.split("\\(");
				String[] p1 = p[1].split(",");
				String x1 = p1[0];
				String[] p1y = p1[1].split("\\)");
				String y1 = p1y[0];
				ver = new JSONObject();
				ver.put("x",x1);
				ver.put("y", y1);
				polygon.put(ver);

			}

			polygons.put(polygon);
		}
		else{
			for(int k=0; k<finalVertices.size();k++){
				vertex = finalVertices.get(k);
				v = vertex.toString();
				String[] p = v.split("\\(");
				String[] p1 = p[1].split(",");
				String x1 = p1[0];
				String[] p1y = p1[1].split("\\)");
				String y1 = p1y[0];
				ver = new JSONObject();
				ver.put("x",x1);
				ver.put("y", y1);
				polygon.put(ver);
			}
			polygons.put(polygon);
		}

		result.append("polygons", polygons);
		final String sendPoints = result.toString();
		return sendPoints;

	}
	
public static String poseClassifier(final String pose, final String ratio) throws JSONException{
	
	RandomTree mpClassifier = null;
	String result = "";
	try {
		String modelName = pose + "_random.model";
		//mpClassifier = (MultilayerPerceptron) SerializationHelper.read(new FileInputStream("C:/Users/Giorgia/multilayerPerceptronClassifier.model"));
		mpClassifier = (RandomTree) SerializationHelper.read(new FileInputStream(modelName));

	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();

	}

	
    // Declare a nominal attribute along with its values
	
	FastVector fvNominalVal = null;
    // Declare a nominal attribute along with its values
	if(pose.equals("head")){
		 fvNominalVal = new FastVector(6);

	     fvNominalVal.addElement("glasses");
	     fvNominalVal.addElement("hair");
	     fvNominalVal.addElement("necklace");
	     fvNominalVal.addElement("scarf");
	     fvNominalVal.addElement("hat");
	     fvNominalVal.addElement("earrings");
	}
	if(pose.equals("torso")){
		 fvNominalVal = new FastVector(8);

	     fvNominalVal.addElement("shirt");
	     fvNominalVal.addElement("bag");
	     fvNominalVal.addElement("dress");
	     fvNominalVal.addElement("accessories");
	     fvNominalVal.addElement("tie");
	     fvNominalVal.addElement("bodysuit");
	     fvNominalVal.addElement("wallet");
	     fvNominalVal.addElement("intimate");

	}
	if(pose.equals("arms")){
		 fvNominalVal = new FastVector(4);

	     fvNominalVal.addElement("skin");
		 fvNominalVal.addElement("bracelet");
	     fvNominalVal.addElement("watch");
	     fvNominalVal.addElement("ring");
	     fvNominalVal.addElement("gloves");

	}
	
	if(pose.equals("legs")){
		 fvNominalVal = new FastVector(5);

	     fvNominalVal.addElement("shorts");
	     fvNominalVal.addElement("skirt");
	     fvNominalVal.addElement("belt");
	     fvNominalVal.addElement("pants");
	     fvNominalVal.addElement("jeans");

	}
	
	if(pose.equals("feet")){
		 fvNominalVal = new FastVector(2);

	     fvNominalVal.addElement("socks");
	     fvNominalVal.addElement("shoes");

	}
    
    Attribute Attribute1 = new Attribute("label", fvNominalVal);
    
    
    // Declare numeric attribute
    Attribute Attribute2 = new Attribute("rapporto");
     
    // Declare the feature vector
    FastVector fvWekaAttributes = new FastVector(2);
    fvWekaAttributes.addElement(Attribute1);    
    fvWekaAttributes.addElement(Attribute2);    
  
    
    // Create an empty training set
    Instances instances = new Instances("Rel", fvWekaAttributes, 1);       
     
    // Set class index
    instances.setClassIndex(0);
    
    // Create the instance
    double ratioDouble = Double.parseDouble(ratio);
    Instance i = new Instance(2);
    i.setMissing(0);          
    i.setValue((Attribute)fvWekaAttributes.elementAt(1), ratioDouble);

    // add the instance
    instances.add(i);

    // Classification/prediction
    for (int k = 0; k < instances.numInstances(); k++) {
		double clsLabel = 0;
		try {
			clsLabel = mpClassifier.classifyInstance(instances.instance(k));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result = instances.classAttribute().value((int) clsLabel);
		
	}

    return result;

	
	}

	public static String getPose(final String image_id) throws JSONException, CMSException{
		
		final JsonReader jsonReader = new JsonReader();
		final utils.CMS.models.Image image = CMS.getImage(Integer.valueOf(image_id));
		List<Pose> pose = image.getPose();
		JSONArray poseJson = new JSONArray();
		JSONObject tempPose;

		for(int i=0;i<pose.size();i++){
			tempPose = new JSONObject();
			tempPose.put("location", pose.get(i).getLocation());
			tempPose.put("x0", pose.get(i).getX0());
			tempPose.put("x1", pose.get(i).getX1());
			tempPose.put("y0", pose.get(i).getY0());
			tempPose.put("y1", pose.get(i).getY1());
			poseJson.put(tempPose);
		}

		//JsonNode image = jsonReader.readJsonArrayFromUrl("http://localhost/cms/wsmc/image/"+image_id+".json");
		//String pose = image.get("pose").toString();
		//pose = pose.toString().replace("\\","");
		//pose = pose.substring(1, pose.length()-1);

		return poseJson.toString();
	}

}

class MaskParameters {

	String id, tag;

	public MaskParameters(final String id, final String tag) {
		this.id = id;
		this.tag = tag;
	}

	public String getId() {
		return id;
	}

	public String getTag() {
		return tag;
	}
}

enum JavascriptColor {

	aqua, blue, fuchsia, green, lime, maroon, navy, olive, orange, purple, red, teal, white, yellow
}
