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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import play.Logger;
import play.Play;
import play.db.DB;
import play.libs.Akka;
import play.libs.F;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.CMS.CMS;
import utils.aggregator.ContourAggregator;
import utils.gamebus.GameMessages.Join;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

import java.util.StringTokenizer;

public class Renderer extends UntypedActor {

	private final static String rootUrl = Play.application().configuration()
			.getString("cmsUrl");
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
		final JavascriptColor[] colors = JavascriptColor.values();
		final JsonReader jsonReader = new JsonReader();
		JsonNode imageSegments = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image/" + imageId + ".json");
		final Integer imWidth = imageSegments.get("width").asInt();
		final Integer imHeight = imageSegments.get("height").asInt();
		imageSegments = imageSegments.get("descriptions").get("segmentation");
		Image toReturn = null;
		Boolean found = false;
		if (null != imageSegments) {
			final int numberTraces = imageSegments.size();
			final ArrayList<String> toAggregate = new ArrayList<>();
			for (int i = 0; i < numberTraces; i++) {
				final JavascriptColor c = colors[i % colors.length];
				final JsonNode current = imageSegments.get(i);
				JsonNode annotation = jsonReader.readJsonArrayFromUrl(rootUrl
						+ "/wsmc/content/" + current.get("id").asText()
						+ ".json");
				final JsonNode tag = annotation.get("itemAnnotations");
				for (int j = 0; j < tag.size(); j++) {
					final JsonNode retrieved = tag.get(j);
					if (retrieved.get("value").asText().equals(requiredTag)) {
						found = true;
						annotation = annotation.get("mediaSegment");
						for (JsonNode result : annotation) {
							if (result.get("name").asText().equals("result")) {
								result = result.get("polyline");
								final ObjectMapper mapper = new ObjectMapper();
								final JsonFactory factory = mapper
										.getJsonFactory(); // since 2.1 use
															// mapper.getFactory()
															// instead
								final String toParse = result.asText().replace(
										"\\", "");
								final JsonParser jp = factory
										.createJsonParser(toParse);
								final JsonNode actualObj = mapper.readTree(jp);
								if (networkOn) {
									final ObjectNode points = new ObjectNode(
											JsonNodeFactory.instance);
									points.put("points", actualObj);
									points.put("type", "trace");
									points.put("color", c.name());
									points.put("imageId", imageId);
									channel.write(points);
								}
								toAggregate.add(actualObj.toString());
								jp.close();
							}
						}
					}
				}
			}
			if (found && !networkOn) {
				final String[] toAggregateString = new String[toAggregate
						.size()];
				for (int i = 0; i < toAggregate.size(); i++) {
					toAggregateString[i] = toAggregate.get(i);
				}
				toReturn = ContourAggregator.simpleAggregator(
						toAggregateString, imWidth, imHeight);
				if (transparentMask) {
					final BufferedImage transparent = imageToBufferedImage(toReturn);
					toReturn = makeColorTransparent(transparent, Color.WHITE);
				}
			} else if (!networkOn) {
				Logger.error("[AGGREGATOR] Cannot retrieve mask for image "
						+ imageId + " with tag " + requiredTag);
				throw new Exception("[AGGREGATOR] Tag " + requiredTag
						+ " not found for image " + imageId);
			}
		}
		return toReturn;
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
			Logger.info("[AGGREGATOR] Retrieved mask for image " + ImageID);
			return result;
		}
		Logger.error("[AGGREGATOR] Retrieved mask for image " + ImageID);
		return null;
	}
	

	public static synchronized JsonNode retrieveImages() throws Exception {

		final JsonReader jsonReader = new JsonReader();
		return jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image.json");
	}

	public static synchronized JsonNode retrieveTags(final String ImageID)
			throws Exception {

		final JsonReader jsonReader = new JsonReader();
		JsonNode image = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image/" + ImageID + ".json");
		image = image.get("descriptions");
		final ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
		HashSet<String> tags;
		if (image != null) {
			tags = CMS.retrieveTags(image);
		} else {
			tags = new HashSet<>();
		}
		for (final String tag : tags) {
			result.add(tag);
		}
		return result;
	}

	private void createMask(final String id, final String tag)
			throws IOException, Exception {
		Logger.info("[AGGREGATOR] Retrieving aggregated mask for image " + id);
		try {
			imageId = id;
			final Image retrieved = aggregate(false, tag, true);
			if (null != retrieved) {
				final BufferedImage bufIm = imageToBufferedImage(retrieved);
				ImageIO.write(bufIm, "png", new File(imageId + ".png"));
				final File toReturn = new File(imageId + ".png");
				getSender().tell(toReturn, this.getSelf());
			} else {
				getSender().tell(null, this.getSelf());
			}
		} catch (final Exception e) {
			getSender().tell(null, this.getSelf());
		}
	}

	/**
	 * Return the list of images' ids and task' ids in the system
	 * 
	 * @return the list of images' ids and task' ids in the system
	 * @throws JSONException
	 */
	public static String webToolAjax() throws JSONException {
		final JsonReader jsonReader = new JsonReader();
		final JsonNode itemImage = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image.json");
		final JsonNode itemTask = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/task.json");
		JSONArray images = new JSONArray();

		JSONArray tasks = new JSONArray();
		JSONObject element;
		final JSONObject result = new JSONObject();
		boolean check = false;

		if (!itemImage.isNull()) {
			images = CMS.retriveImageId(itemImage);
		} else {
			element = new JSONObject();
			element.append("id", "No photos in the system");
			images.put(element);
		}
		if (itemTask != null) {
			tasks = CMS.retriveTaskId(itemTask);
			check = true;
		} else {
			element = new JSONObject();
			element.append("id", "No Tasks in the system");
			element.append("taskType", "");
			tasks.put(element);
		}

		result.append("image", images);
		result.append("task", tasks);
		result.append("check", check);
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

		final JsonReader jsonReader = new JsonReader();
		final JsonNode itemTask = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/task.json");
		boolean check = false;
		JSONArray tasks = new JSONArray();
		JSONObject element;
		final JSONObject result = new JSONObject();

		if (itemTask != null) {
			tasks = CMS.retriveTaskId(itemTask);
			check = true;
		} else {
			element = new JSONObject();
			element.append("id", "No Tasks in the system");
			element.append("taskType", "");
			tasks.put(element);
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

		final JsonReader jsonReader = new JsonReader();
		final JsonNode itemImage = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image.json");
		final JsonNode users = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/cubrikuser/9.json");
		
		int last;
		final JSONObject result = new JSONObject();

		final String totImg = Integer.toString(itemImage.size());
		String numUsers = Integer.toString(users.size());
		final JSONArray stats = CMS.retriveStats(itemImage);
		
		String tmp = stats.getJSONObject(0).getString("numTag");
		tmp = tmp.substring(1);
		last = tmp.length() - 1;
		tmp = tmp.substring(0, last);

		final int numTag = Integer.parseInt(tmp);
		tmp = stats.getJSONObject(0).getString("numSegment");
		tmp = tmp.substring(1);
		last = tmp.length() - 1;
		tmp = tmp.substring(0, last);
		final int numSegment = Integer.parseInt(tmp);

		final float mediaTagImg = (float) numTag / itemImage.size();
		final float mediaSegImg = (float) numSegment / itemImage.size();

		final String mediaTag = Float.toString(mediaTagImg);
		final String numeroSegmenti = Integer.toString(numSegment);
		final String mediaSegPerImg = Float.toString(mediaSegImg);

		result.append("totImg", totImg);
		result.append("mediaTag", mediaTag);
		result.append("numSegment", numeroSegmenti);
		result.append("mediaSegImg", mediaSegPerImg);
		result.append("numberUsers", numUsers);
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
		
		final JsonReader jsonReader = new JsonReader();
		final JsonNode itemApp = jsonReader.readJsonArrayFromUrl(rootUrl
					+ "/wsmc/cubrikuser/9.json");

		final JSONObject result = new JSONObject();

		final JSONArray usersInfo = new JSONArray();
		JSONObject element; 
		JsonNode object;

		int i=0;
		
		while (i < itemApp.size()) {
			object = itemApp.get(i);
			element = new JSONObject();
			element.put("cubrik_userid", object.get("cubrik_userid").toString());
			element.put("app_id", object.get("app_id").toString());
			element.put("app_user_id", object.get("app_user_id").toString());
			element.put("number_of_plays", object.get("number_of_plays"));
			element.put("number_of_annotations", object.get("number_of_annotations"));
			
			usersInfo.put(element);

			i++;
			}// fine while
		
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

		final JsonReader jsonReader = new JsonReader();
		final JsonNode item = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image/" + selection + ".json");

		final String info = CMS.retriveImgInfo(item);

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

		final JsonReader jsonReader = new JsonReader();
		final JsonNode item = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/mask/" + imageId + "/" + tagId + ".json");

		final String info = CMS.retriveMaskInfo(item);
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
	public static String maskFashionistaAjaxCall(final String imageId, final String tagId)
			throws JSONException {
		
		
		    final JSONObject result = new JSONObject();
		    final JSONArray info = new JSONArray();

		    
			String url = "";
			String img = "";
			String tag = "";
			String qual = "";
			String temp = "";
			String quality = "";

		    java.io.File folder = play.Play.application().getFile("public/images/fashionista");

		    File[] files = folder.listFiles();
		    
		    if (files != null) {
			    for (File file : files) {
			    		temp = file.getName();
			    		StringTokenizer st = new StringTokenizer(temp, "_");
			    		String mask = (String) st.nextElement();
			    		img = (String) st.nextElement();
			    		tag = (String) st.nextElement();
			    		qual = (String) st.nextElement();
			    		
			    		if(img.equals(imageId)&&tag.equals(tagId))
			    		{
			    			url = file.getName();
			    			StringTokenizer st2 = new StringTokenizer(qual, ".png");
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

		final JsonReader jsonReader = new JsonReader();
		//final JsonNode itemTask = jsonReader.readJsonArrayFromUrl(rootUrl
		//		+ "/wsmc/task.json"); // TODO possible speed increment, directly
										// download need task and not all task +
										// loacal search
		
		final JsonNode itemTask = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/task/" + selection + ".json");
		
		//final String info = CMS.retriveTaskInfo(itemTask, selection);
		final String info = CMS.retriveTaskInfo(itemTask);

		return info;


	}

	/**
	 * Close a particular task
	 * 
	 * @param selection
	 *            the task that i want to close
	 * @throws IOException
	 */
	public static void closeTask(final String selection) throws IOException {
		CMS.closeTask2(selection);
	}
	
	/**
	 * Invalidate tag
	 */
	public static void invalidateTag(final String selection) throws IOException {
		CMS.invalidateTag(selection);
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
	 */
	public static String addTask(final String taskType, final String selectedImg)
			throws IOException, JSONException {

		final String info = CMS.addTask(taskType, selectedImg);
		return info;
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
	 */
	public static String addUTask(final String taskType,
			final String selectionTask) throws IOException, JSONException {

		final String info = CMS.addUTask(taskType, selectionTask);
		return info;
	}

    /**
     * Retrive stats needed to deploy first graph: Number of images with X annotations
     *
     * @return          the string with data
     * @throws JSONException
     */
	public static String loadFirstGraph() throws JSONException {

		final JsonReader jsonReader = new JsonReader();
		final JsonNode itemImage = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image.json");

		final JSONArray info = CMS.loadFirst(itemImage);
		final String result = info.toString();
		return result;
	}

    /**
     *  Retrive stats needed to deploy first graph: Number of users that have annotated X images
     *
     * @return          the string with data
     * @throws JSONException
     */
	public static String loadSecondGraph() throws JSONException {

		final JsonReader jsonReader = new JsonReader();
		final JsonNode itemAction = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/action.json");

		final JSONArray info = CMS.loadSecond(itemAction);
		final String result = info.toString();
		return result;
	}
	
	

    /**
     * Generate a json file with User Data
     *
     * @return      the string with data
     * @throws JSONException
     */
	public static String downloadStats1() throws JSONException {
		final JsonReader jsonReader = new JsonReader();
		
		final JsonNode itemAction = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/action.json");

		final JSONArray info = CMS.download1(itemAction);
		final String result = info.toString();
		return result;
	}

    /**
     * Generate a json file with Images Data
     *
     * @return      the string with data
     * @throws JSONException
     */
	public static String downloadStats2() throws JSONException {
		final JsonReader jsonReader = new JsonReader();
		final JsonNode itemAction = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/action.json");

		final JSONArray info = CMS.download2(itemAction);
		final String result = info.toString();
		return result;
	}
	
	
	/**
	 * Load the list of collections of images
	 * 
	 * @return the info that i retrived
	 * @throws JSONException
	 */
	public static String collectionAjaxCall()
			throws JSONException {

		final JsonReader jsonReader = new JsonReader();
		final JsonNode itemCollection = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/collection.json");
		
		JSONArray collections = new JSONArray();
		collections = CMS.retriveCollectionInfo(itemCollection);
		
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
	 */
	public static String collectionImagesAjaxCall(final String collectionId)
			throws JSONException {

		final JsonReader jsonReader = new JsonReader();
		final JsonNode item = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/collection/" + collectionId + ".json");

		//JSONArray images = new JSONArray();
		String images = CMS.retriveCollImages(item);
		/*
		final JSONObject result = new JSONObject();
		result.append("images", images);
		final String options = result.toString();
		return options;
		*/
		return images;
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
