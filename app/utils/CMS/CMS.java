package utils.CMS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import scala.concurrent.duration.Duration;
import utils.JsonReader;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.Room;
import akka.actor.Cancellable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Wrapper for the CMS API
 * 
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class CMS {

	// Url of the CMS system
	private final static String rootUrl = Play.application().configuration()
			.getString("cmsUrl");
	private final static String oauthConsumerKey = Play.application()
			.configuration().getString("oauthConsumerKey");

	private final static String collection = Play.application().configuration()
			.getString("collection");
	private static final String POLICY = Play.application().configuration()
			.getString("policy");

	private static HashMap<String, Cancellable> runningThreads = new HashMap<String, Cancellable>();

	public static void closeUTask(final Integer uTaskID, final Integer actionId) {
		if (uTaskID != null) {
			final String request = rootUrl + "/wsmc/utask/" + uTaskID + "/"
					+ actionId + "/close";
			WS.url(request).setContentType("application/x-www-form-urlencoded")
			.put("");
			LoggerUtils.debug("CMS", "Closing uTask " + uTaskID);
		}
	}

	public static void closeTask(final Integer taskID) {
		if (taskID != null) {
			final String request = rootUrl + "/wsmc/task/" + taskID + "/close";
			WS.url(request).setContentType("application/x-www-form-urlencoded")
			.put("");
			LoggerUtils.debug("CMS", "Closing Task " + taskID);
		}
	}

	public static void invalidateTag(final String tagID) throws IOException {
		final CloseableHttpClient httpclient = HttpClients.createDefault();
		final HttpPut httpPut = new HttpPut(rootUrl + "/wsmc/content/" + tagID
				+ "/invalidate");
		final CloseableHttpResponse response1 = httpclient.execute(httpPut);

		final HttpEntity entity1 = response1.getEntity();
		EntityUtils.consume(entity1);
		response1.close();
		httpclient.close();
	}

	public static void segmentation(final ObjectNode finalTraces,
			final String username, final Integer session) throws Exception {
		Akka.system()
		.scheduler()
		.scheduleOnce(Duration.create(200, TimeUnit.MILLISECONDS),
				new Runnable() {
			@Override
			public void run() {
				try {
					final String id = finalTraces.get("id")
							.textValue();
					final String label = finalTraces.get(
							"label").textValue();
					textAnnotation(finalTraces, username,
							session);
					final String traces = finalTraces.get(
							"traces").toString();
					final String history = finalTraces.get(
							"history").toString();

					final String urlParameters = "ta_name=tag&ta_val="
							+ label
							+ "&content_type=segmentation&&user_id="
							+ username
							+ "&language="
							+ LanguagePicker.retrieveIsoCode()
							+ "&session_id="
							+ session
							+ "&polyline_r="
							+ traces
							+ "&polyline_h="
							+ history
							+ "&oauth_consumer_key="
							+ oauthConsumerKey;
					final String request = rootUrl
							+ "/wsmc/image/" + id
							+ "/segmentation.json";
					final JSONObject actionInfo;
					try {
						WS.url(request)
						.setContentType(
								"application/x-www-form-urlencoded")
								.setTimeout(10000)
								.post(urlParameters);
						LoggerUtils.debug("CMS",
								"Storing segmentation with action for image with id "
										+ id + " and tag "
										+ label);
					} catch (final Exception ex) {
						LoggerUtils
						.error("Unable to save segmentation, EXC1",
								ex);
					}
				} catch (final Exception ex) {
					LoggerUtils
					.error("Unable to save segmentation, EXC2",
							ex);
				}
			}
		}, Akka.system().dispatcher());
	}

	public static void textAnnotation(final ObjectNode finalTraces,
			final String username, final Integer session) throws Exception {
		Akka.system()
		.scheduler()
		.scheduleOnce(Duration.create(200, TimeUnit.MILLISECONDS),
				new Runnable() {
			@Override
			public void run() {
				final String label = finalTraces.get("label")
						.textValue();
				final String id = finalTraces.get("id")
						.textValue();

				final String urlParameters = "ta_name=tag&ta_val="
						+ label
						+ "&content_type=tagging&&user_id="
						+ username
						+ "&language="
						+ LanguagePicker.retrieveIsoCode()
						+ "&session_id="
						+ session
						+ "&oauth_consumer_key="
						+ oauthConsumerKey;
				final String request = rootUrl + "/wsmc/image/"
						+ id + "/textAnnotation.json";
				try {
					final F.Promise<WS.Response> returned = WS
							.url(request)
							.setContentType(
									"application/x-www-form-urlencoded")
									.setTimeout(10000)
									.post(urlParameters);
					LoggerUtils.debug("CMS",
							"Storing textAnnotation for image with id "
									+ id + " and tag " + label);
				} catch (final Exception e) {
					Logger.error("Unable to save annotation.",
							e);
				}
			}
		}, Akka.system().dispatcher());
	}

	public static Integer openSession() throws Error {
		final String request = rootUrl + "/wsmc/session.json";
		LoggerUtils.debug("CMS", "Opening a new session...");
		final F.Promise<WS.Response> returned = WS.url(request)
				.setContentType("application/x-www-form-urlencoded")
				.post("oauth_consumer_key=" + oauthConsumerKey);

		String sessionId = returned.get().getBody();
		sessionId = sessionId.replace("[\"", "");
		sessionId = sessionId.replace("\"]", "");
		LoggerUtils.debug("CMS", "Retrieved session " + sessionId);
		return Integer.valueOf(sessionId);
	}

	public static void closeSession(final Integer sessionId) throws Error {
		final String request = rootUrl + "/wsmc/session/" + sessionId;
		WS.url(request).setContentType("application/x-www-form-urlencoded")
		.put("state=0&oauth_consumer_key=" + oauthConsumerKey);
		LoggerUtils.debug("CMS", "Closing session " + sessionId);
	}

	public static void postAction(final Integer sessionId,
			final String actionType, final String username, String log)
					throws Error {
		final String request = rootUrl + "/wsmc/action";
		if (log.equals("")) {
			log = "{}";
		}
		final String parameters = "session_id=" + sessionId + "&action_type="
				+ actionType + "&user_id=" + username + "&oauth_consumer_key="
				+ oauthConsumerKey + "&action_log=" + log;
		WS.url(request).setContentType("application/x-www-form-urlencoded")
		.post(parameters);
		LoggerUtils.debug("CMS", "Action " + actionType + " for session "
				+ sessionId + ": " + log);
	}

	public static void fixGroundTruth(final Integer sessionId,
			final List<ObjectNode> priorityTaskHashSet,
			final List<ObjectNode> queueImages, final Room roomChannel) {
		final JsonReader jsonReader = new JsonReader();
		JsonNode retrievedImages;
		final HashMap<String, ObjectNode> temporary = new HashMap<>();
		retrievedImages = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image.json");
		boolean taskSent = false;
		if (retrievedImages != null) {
			// For each image
			for (final JsonNode item : retrievedImages) {
				if (item.elements().hasNext()) {
					// Save information related to the image
					final String id = item.get("id").asText();
					final String url = rootUrl
							+ item.get("mediaLocator").asText();
					final Integer width = item.get("width").asInt();
					final Integer height = item.get("height").asInt();

					// Get all the segments that have been stored for the image
					final JsonNode imageSegments = item.get("descriptions");
					HashSet<String> tags = new HashSet<>();
					final ObjectNode guessWord = Json.newObject();
					guessWord.put("type", "task");
					guessWord.put("id", id);

					// Find the valid tags for this task.
					if (imageSegments != null) {
						tags = retrieveTags(imageSegments);
					}
					// Add one tag among the ones that have been retrieved
					// following a particular policy
					guessWord.put("tag", chooseTag(tags));
					guessWord.put("lang", LanguagePicker.retrieveIsoCode());
					guessWord.put("image", url);
					guessWord.put("width", width);
					guessWord.put("height", height);
					temporary.put(id, guessWord);
				}
			}
			JsonNode processedSession = jsonReader.readJsonArrayFromUrl(rootUrl
					+ "/wsmc/session/" + sessionId + ".json");
			if (processedSession != null) {
				processedSession = processedSession.get("actions");
				for (final JsonNode item : processedSession) {
					if (item.get("type").asText().equals("segmentation")) {
						final JsonNode segmentation = jsonReader
								.readJsonArrayFromUrl(rootUrl + "/wsmc/action/"
										+ item.get("id").asText() + ".json");
						if (temporary.containsKey(segmentation.get("image")
								.asText()))
							temporary
							.remove(segmentation.get("image").asText());
					}
				}
			}
			for (final Map.Entry pairs : temporary.entrySet()) {
				queueImages.add((ObjectNode) pairs.getValue());
			}
			if (!taskSent) {
				taskSent = true;
				sendTaskAcquired(roomChannel);
			}
		}
	}

	public static void addInitializationThread(final String roomName,
			final Cancellable thread) throws Exception {
		runningThreads.put(roomName, thread);
	}

	public static boolean getThread(final String roomName) throws Exception {
		if (runningThreads.containsKey(roomName))
			return true;
		else
			return false;
	}

	public static void cancelThread(final String roomName) throws Exception {
		final Cancellable thread = runningThreads.get(roomName);
		if (thread != null) {
			thread.cancel();
			runningThreads.remove(roomName);
		}
	}

	/**
	 * Retrieving data from the CMS [TODO] Right now we are not retrieving based
	 * on the requirements of our tasks such as completing tasks that have not
	 * been already faced and so on. We will add this feature in the future.
	 * 
	 * @param maxRound
	 * 
	 */
	public static void taskSetInitialization(
			final List<ObjectNode> priorityTaskHashSet,
			final List<ObjectNode> queueImages, final Room roomChannel,
			final Integer maxRound) throws Error, JSONException {
		int uploadedTasks = 0;
		try {
			uploadedTasks = retrieveTasks(maxRound, priorityTaskHashSet,
					roomChannel);
		} catch (final Exception e) {
			LoggerUtils.error("CMS", "Unable to read tasks");
		}
		final int tasksToAdd = maxRound - uploadedTasks;
		if (tasksToAdd > 0) {
			retrieveImages(tasksToAdd, queueImages, roomChannel,
					uploadedTasks > 0);
		}
		LoggerUtils.debug("CMS","Task init from CMS end");
	}                                      



	private static void retrieveImages(final Integer tasksToAdd,
			final List<ObjectNode> queueImages, final Room roomChannel,
			boolean taskSent) {

		final JsonNode retrievedImagesOrdered;
		// final ArrayList<JsonNode> retrievedImages;

		// [TODO] Fail safe in case of not being able to retrieve the instances

		final JsonReader jsonReader = new JsonReader();
		try {
			LoggerUtils.debug("CMS", "Requested image list to CMS");
			final HashMap<String, String> params = new HashMap<>();
			params.put("collection", collection);
			params.put("limit", tasksToAdd.toString());
			params.put("nocache", String.valueOf(System.currentTimeMillis()));
			// params.put("select", "id");
			params.put("select", "all");
			params.put("policy", "fashionista");
			retrievedImagesOrdered = jsonReader.readJsonArrayFromUrl(rootUrl
					+ "/wsmc/image.json", params);
			LoggerUtils.debug("CMS", "Requested image list to CMS end");
		} catch (final IllegalArgumentException e) {
			throw new RuntimeException(
					"[CMS] The request to the CMS is malformed");
		}
		for (final JsonNode item : retrievedImagesOrdered) {
			if (item.elements().hasNext()) {
				// Save information related to the image
				final String id = item.get("id").asText();

				// Get all the segments that have been stored for the image
				final JsonNode imageSegments = item.get("descriptions");
				HashSet<String> tags = new HashSet<>();
				final ObjectNode guessWord = Json.newObject();
				guessWord.put("type", "task");
				guessWord.put("id", id);
				// Find the valid tags for this task.
				if (imageSegments != null) {
					tags = retrieveTagsLight(imageSegments);
				}

				buildGuessWordSegment(
						guessWord, tags, item);


				queueImages.add(guessWord);

				if (!taskSent) {
					taskSent = true;
					LoggerUtils.debug("CMS", "Send task aquired for image:"
							+ id + ", rooomChanel: " + roomChannel);
					sendTaskAcquired(roomChannel);
				}

			}
		}

	}

	private static HashMap<String, Integer> retrieveTagsCount(
			final HashSet<String> tagsList, JsonNode imageSegments) {
		final HashMap<String, Integer> tags = new HashMap<>();
		for (final String tag : tagsList) {
			tags.put(tag, 0);
		}

		imageSegments = imageSegments.get("segmentation");
		if (imageSegments != null) {
			if (imageSegments.elements().hasNext()) {
				for (final JsonNode segment : imageSegments) {
					// Retrieve the content descriptor
					if (null != segment) {

						final String tag = segment.get("annotation_value")
								.textValue();
						if (tags.get(tag) != null) {
							tags.put(tag, tags.get(tag) + 1);
						}

					}
				}
			}
		}
		return tags;
	}

	private static HashSet<String> retrieveTagsLight(JsonNode imageSegments) {
		final HashSet<String> tags = new HashSet<>();

		imageSegments = imageSegments.get("availableTags");
		tags.add(imageSegments.textValue());
		// if (imageSegments != null) {
		// if (imageSegments.elements().hasNext()) {
		// for (final JsonNode segment : imageSegments) {
		// // Retrieve the content descriptor
		// if (null != segment) {
		// // Logger.debug("send request to retrieve tags "
		// // + segment.get("id").textValue());
		//
		// if (segment.get("lang").textValue()
		// .equals(LanguagePicker.retrieveIsoCode())
		// || LanguagePicker.retrieveIsoCode().equals("")) {
		// tags.add(segment.get("value").textValue());
		// }
		//
		// }
		// }
		// }
		// }
		return tags;
	}

	private static int retrieveTasks(final Integer maxRound,
			final List<ObjectNode> priorityTaskHashSet, final Room roomChannel) {
		boolean taskSent = false;

		int uploadedTasks = 0;

		JsonNode retrievedTasks = null;
		final JsonReader jsonReader = new JsonReader();
		try {
			// TODO add id...
			LoggerUtils.debug("CMS", "Requested task list to CMS "
					+ roomChannel);
			final HashMap<String, String> params = new HashMap<>();
			params.put("collection", collection);
			params.put("limit", maxRound.toString());
			params.put("open", "true");
			final String url = rootUrl + "/wsmc/task.json";
			retrievedTasks = jsonReader.readJsonArrayFromUrl(url, params);
			LoggerUtils.debug("CMS", "Requested task list to CMS end"
					+ roomChannel);
		} catch (final IllegalArgumentException e) {
			throw new RuntimeException(
					"[CMS] The request to the CMS is malformed");
		}

		// Fill the set of task to be performed with the task that has been
		// explicitly declared
		try {
			if (retrievedTasks != null && retrievedTasks.size() != 0) {
				retrievedTasks = retrievedTasks.get("task");
				for (final JsonNode item : retrievedTasks) {
					if (item.elements().hasNext()) {

						final String taskId = item.get("id").textValue();
						final JsonNode uTasks = item.get("utask");

						final String imageId = item.get("image").elements()
								.next().elements().next().asText();
						final JsonNode image = jsonReader
								.readJsonArrayFromUrl(rootUrl + "/wsmc/image/"
										+ imageId + ".json");
						if (uTasks != null) {
							// Retrieve the first uTask for the current task and
							// assign it
							for (final JsonNode utask : uTasks) {
								if (utask.elements().hasNext()) {
									// FIXME non necessario, ho gia tutto
									// quello
									// che mi serve

									final ObjectNode guessWord = Json
											.newObject();
									guessWord.put("type", "task");
									guessWord.put("id", imageId);
									// Change the task to assign based on
									// the kind of task that has to be
									// performed
									// for now just tagging and segmentation
									// are supported for the images.
									switch (utask.get("utaskType").asText()) {
									case "tagging":
										buildGuessWordTagging(guessWord, image,
												utask, taskId);

										priorityTaskHashSet.add(guessWord);
										uploadedTasks++;
										if (!taskSent) {
											taskSent = true;
											sendTaskAcquired(roomChannel);
										}
										break;
									case "segmentation":
										HashSet<String> tags;
										// Get all the segments that have
										// been stored for the image
										final JsonNode imageSegments = image
												.get("descriptions");
										if (imageSegments != null) {
											tags = retrieveTags(imageSegments);
											buildGuessWordSegmentTask(
													guessWord, tags, image,
													taskId, utask);

											priorityTaskHashSet.add(guessWord);
											uploadedTasks++;
											if (!taskSent) {
												taskSent = true;
												sendTaskAcquired(roomChannel);
											}
										}
										break;
									}
									break;
								}
							}
						}

					}
				}
			}
		} catch (final Exception e) {
			throw new RuntimeException("[CMS] Data malformed");
		}
		return uploadedTasks;
	}

	private static void buildGuessWordSegment(final ObjectNode guessWord,
			final HashSet<String> tags, final JsonNode image) {
		// Add one tag among the ones that have been retrieved following
		// a particular policy
		final String tag = chooseTag(tags);

		guessWord.put("tag", tag);
		guessWord.put("lang", LanguagePicker.retrieveIsoCode());
		guessWord.put("image", rootUrl + image.get("mediaLocator").asText());
		guessWord.put("width", image.get("width").asInt());
		guessWord.put("height", image.get("height").asInt());

	}

	private static void buildGuessWordSegmentTask(final ObjectNode guessWord,
			final HashSet<String> tags, final JsonNode image,
			final String taskId, final JsonNode utask) {
		buildGuessWordSegment(guessWord, tags, image);
		guessWord.put("utaskid", utask.get("id").asInt());
		guessWord.put("taskid", taskId);

	}

	private static void buildGuessWordTagging(final ObjectNode guessWord,
			final JsonNode image, final JsonNode utask, final String taskId) {
		guessWord.put("tag", chooseTag(null));
		guessWord.put("lang", LanguagePicker.retrieveIsoCode());
		guessWord.put("image", rootUrl + image.get("mediaLocator").asText());
		guessWord.put("width", image.get("width").asInt());
		guessWord.put("height", image.get("height").asInt());
		guessWord.put("utaskid", utask.get("id").asInt());
		guessWord.put("taskid", taskId);

	}

	/*
	 * Inform the game that at least one task is ready and we can start the game
	 */
	private static void sendTaskAcquired(final Room roomChannel) {
		LoggerUtils.debug("CMS", "CMS sends task aquired... ");
		GameBus.getInstance().publish(
				new GameMessages.GameEvent(GameMessages.composeTaskAcquired(),
						roomChannel));
	}

	public static HashSet<String> retrieveTags(JsonNode imageSegments) {

		final JsonReader jsonReader = new JsonReader();
		final HashSet<String> tags = new HashSet<>();
		imageSegments = imageSegments.get("availableTags");
		if (imageSegments != null) {
			if (imageSegments.elements().hasNext()) {
				for (final JsonNode segment : imageSegments) {
					// Retrieve the content descriptor
					if (null != segment) {
						JsonNode retrieved = jsonReader
								.readJsonArrayFromUrl(rootUrl
										+ "/wsmc/content/"
										+ segment.get("id").textValue()
										+ ".json");
						retrieved = retrieved.get("itemAnnotations").get(0);
						// If the annotation is a tag and is in the same
						// language as the one defined in the system, add the
						// tag to the list of possible tags
						if ((retrieved.get("name").asText().equals("tag"))
								&& (retrieved
										.get("language")
										.asText()
										.equals(LanguagePicker
												.retrieveIsoCode()) || LanguagePicker
												.retrieveIsoCode().equals(""))) {
							tags.add(retrieved.get("value").asText());
						}
					}
				}
			}
		}
		return tags;
	}

	/**
	 * Returns all the items of a selected collection
	 * 
	 * @param collectonId
	 * @return
	 */
	public static HashSet<String> getCollection(final String collectonId) {

		final HashSet<String> photos = new HashSet<>();
		final JsonReader jsonReader = new JsonReader();
		final JsonNode retrieved = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/content/" + collectonId + ".json");
		// (retrieved.get("name").asText().equals("tag")
		return photos;
	}

	/**
	 * Retrive all the images' Ids that are stored in the system
	 * 
	 * @param jsonImages
	 *            JsonNode off all images
	 * @return JSONArray with all the ids sorted depending on number of
	 *         annotations
	 * @throws JSONException
	 */
	public static JSONArray retriveImageId(final JsonNode jsonImages)
			throws JSONException {

		final JSONArray imageIds = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		JsonNode object;
		JSONObject element;
		int num = 0;

		int i = 0;
		while (i < jsonImages.size()) {
			sorting = new SortObject() {
			};
			object = jsonImages.get(i);
			sorting.setId(object.get("id").asText());
			sorting.setMedia(rootUrl + object.get("mediaLocator").asText());
			if (object.has("descriptions")) {
				if (object.get("descriptions").has("segmentation")) {
					num = object.get("descriptions").get("segmentation").size();
				} else {
					num = 0;
				}
			}
			sorting.setNum(num);
			tempList.add(i, sorting);
			num = 0;
			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getNum() > o2.getNum()) {
					return -1;
				} else if (o1.getNum() < o2.getNum()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		while (it.hasNext()) {
			element = new JSONObject();
			final SortObject obj = it.next();
			element.put("id", obj.getId());
			element.put("media", obj.getMedia());
			element.put("numAnnotations", obj.getNum());
			imageIds.put(element);
		}

		return imageIds;
	}

	/**
	 * Retrive all the tasks' ids that are stored in the system
	 * 
	 * @param jsonTask
	 *            JsonNode off all tasks
	 * @return JSONArray with all the ids
	 * @throws JSONException
	 */
	public static JSONArray retriveTaskId(final JsonNode jsonTask)
			throws JSONException {

		final JSONArray taskIds = new JSONArray();
		JsonNode object;
		JSONObject element;
		int i = 0;
		if (jsonTask.get("task") != null) {

			while (i < jsonTask.get("task").size()) {
				element = new JSONObject();
				object = jsonTask.get("task").get(i);
				element.put("id", object.get("id"));
				element.put("taskType", object.get("taskType"));
				element.put("status", object.get("status"));
				taskIds.put(element);
				i++;
			}
		}
		return taskIds;
	}

	/**
	 * Retrive the stats of the system
	 * 
	 * @param jsonImages
	 *            JsonNode off all images
	 * @return JSONArray with number of tags and number of segmentation
	 * @throws JSONException
	 */
	public static JSONArray retriveStats(final JsonNode jsonImages)
			throws JSONException {

		final JSONArray values = new JSONArray();
		JsonNode object;
		JsonNode descObj;
		JsonNode tmpArr;
		final JSONObject element = new JSONObject();
		int numTag = 0;
		int numSegment = 0;
		int i = 0;

		while (i < jsonImages.size()) {
			object = jsonImages.get(i);
			if (object.has("descriptions")) {
				descObj = object.get("descriptions");
				if (descObj.has("availableTags")) {
					tmpArr = descObj.get("availableTags");
					numTag = numTag + tmpArr.size();
				}// if se descObject ha dei availableTags
				if (descObj.has("segmentation")) {
					tmpArr = descObj.get("segmentation");
					numSegment = numSegment + tmpArr.size();
				}
			}// if se c'è il campo description
			i++;
		}// fine while
		element.append("numTag", numTag);
		element.append("numSegment", numSegment);
		values.put(element);
		return values;
	}

	/**
	 * Retrive info for a specific image
	 * 
	 * @param jsonImages
	 *            The specific image which I'm evalueting
	 * @return It's tags, medialocator and number of annotation
	 * @throws JSONException
	 */
	public static String retriveImgInfo(final JsonNode jsonImages)
			throws JSONException {

		final JSONArray info = new JSONArray();
		final JsonReader jsonReader = new JsonReader();
		JsonNode itemTag;
		JsonNode segmentArr, object2, tagId, valid, lang, annotationId;
		JsonNode descObj;
		JsonNode tagArr;
		JsonNode annotArr;
		JSONObject element;
		final JSONArray tags = new JSONArray();
		int numSegment = 0;
		int j = 0;
		int count = 0;
		int k = 0;
		String tmpTag;
		JsonNode media, width, height;

		media = jsonImages.get("mediaLocator");
		width = jsonImages.get("width");
		height = jsonImages.get("height");
		if (jsonImages.has("descriptions")) {
			descObj = jsonImages.get("descriptions");
			if (descObj.has("availableTags")) {
				tagArr = descObj.get("availableTags");
				while (j < tagArr.size()) {
					tagId = tagArr.get(j);
					tmpTag = tagId.get("id").toString();
					tmpTag = tmpTag.substring(1, tmpTag.length() - 1);
					itemTag = jsonReader.readJsonArrayFromUrl(rootUrl
							+ "/wsmc/content/" + tmpTag + ".json");
					object2 = itemTag.get("itemAnnotations").get(0)
							.get("value");
					// count number of annotations for that given tag

					if (descObj.has("segmentation")) {
						annotArr = descObj.get("segmentation");

						count = 0;
						k = 0;

						while (k < annotArr.size()) {
							try{
								if (annotArr
										.get(k)
										.get("annotation_value")
										.asText()
										.equals(itemTag.get("itemAnnotations")
												.get(0).get("value").asText())) {
									count++;
								}
							}
							catch(final Exception e) {
								LoggerUtils.error("CMS","Could not retrieve annotation value for"+annotArr.get(k));
							}
							k++;
						}

					}

					lang = itemTag.get("itemAnnotations").get(0)
							.get("language");
					annotationId = itemTag.get("itemAnnotations").get(0)
							.get("id");
					valid = itemTag.get("valid");
					element = new JSONObject();
					element.put("tagId", tmpTag);
					element.put("tag", object2);
					element.put("numAnnotations", count);
					element.put("valid", valid);
					element.put("lang", lang);
					element.put("annotationId", annotationId);
					tags.put(element);
					j++;
				}// fine while
			}// if se descObject ha dei availableTags
			if (descObj.has("segmentation")) {
				segmentArr = descObj.get("segmentation");
				numSegment = segmentArr.size();
			}
		}// if se c'è il campo description
		element = new JSONObject();
		element.put("tags", tags);
		element.put("medialocator", media);
		element.put("height", height);
		element.put("width", width);
		element.put("annotations", numSegment);
		info.put(element);
		final String result = info.toString();
		return result;
	}

	/**
	 * Retrive info for a specific mask
	 * 
	 * @param jsonMask
	 *            The specific mask which I'm evalueting
	 * @return It's medialocator
	 * @throws JSONException
	 */
	public static String retriveMaskInfo(final JsonNode jsonMask)
			throws JSONException {

		final JSONArray info = new JSONArray();

		JSONObject element;

		String media;
		media = rootUrl + jsonMask.get("path").asText();
		String quality;
		quality = jsonMask.get("quality").asText();

		element = new JSONObject();
		element.put("medialocator", media);
		element.put("quality", quality);

		info.put(element);
		final String result = info.toString();
		return result;

	}

	/**
	 * Retrive the microTask of a particular task
	 * 
	 * @param jsonTasks
	 *            JsonNode of all the Tasks
	 * @param selected
	 *            id of the task that I want
	 * @return the status of the task (open or closed) and the information of
	 *         its microtask (id, type, status)
	 * @throws JSONException
	 */
	public static String retriveTaskInfo(final JsonNode jsonTasks)
			throws JSONException {

		final JSONArray info = new JSONArray();
		final JsonNode utaskId;

		JsonNode utaskArr;
		JSONObject element, finalElement;
		final JSONArray utasks = new JSONArray();
		final int j = 0;
		String tmpUtask, status, utaskType;

		if (jsonTasks.has("utasks")) {

			utaskArr = jsonTasks.get("utasks");

			for (final JsonNode utask : utaskArr) {
				tmpUtask = utask.get("id").textValue();
				status = utask.get("status").textValue();
				utaskType = utask.get("utaskType").textValue();

				element = new JSONObject();
				element.put("id", tmpUtask);
				element.put("status", status);
				element.put("utaskType", utaskType);

				utasks.put(element);
			}
		}// if se c'è campo utasks

		finalElement = new JSONObject();
		finalElement.put("utasks", utasks);
		info.put(finalElement);
		final String result = info.toString();
		return result;

		/*
		 * 
		 * 
		 * 
		 * final JSONArray info = new JSONArray(); JsonNode object, object2;
		 * JsonNode taskObj; JSONObject element; final JSONArray uTasks = new
		 * JSONArray(); int i = 0; int j = 0; String tmpId; JsonNode status =
		 * null;
		 * 
		 * object = jsonTasks.get("task"); while (i < object.size()) { object2 =
		 * object.get(i); tmpId = object2.get("id").asText(); if
		 * (tmpId.equals(selected)) { status = object2.get("status"); if
		 * (object2.has("utasks")) { element = new JSONObject();
		 * element.put("utask", "full"); taskObj = object2.get("utasks"); while
		 * (j < taskObj.size()) { element = new JSONObject(); element.put("id",
		 * taskObj.get(j).get("id")); element.put("taskType",
		 * taskObj.get(j).get("taskType")); element.put("status",
		 * taskObj.get(j).get("status")); uTasks.put(element); j++; } break; }//
		 * if se c'è il campo uTask else { element = new JSONObject();
		 * element.put("utask", "empty"); uTasks.put(element); } } i++; }
		 * element = new JSONObject(); element.put("status", status);
		 * element.put("uTasks", uTasks); info.put(element);
		 * 
		 * final String result = info.toString(); return result;
		 */
	}

	/**
	 * Retrive the list of collections id
	 * 
	 * @param jsonCollection
	 *            JsonNode of all the Collections
	 * @return the ids of the collections
	 * @throws JSONException
	 */

	public static JSONArray retriveCollectionInfo(final JsonNode jsonCollection)
			throws JSONException {

		final JSONArray collectionIds = new JSONArray();
		JsonNode object;
		JSONObject element;
		int i = 0;

		while (i < jsonCollection.get("collections").size()) {
			element = new JSONObject();
			object = jsonCollection.get("collections").get(i);
			element.put("id", object.get("id"));
			element.put("name", object.get("name"));
			collectionIds.put(element);
			i++;
		}
		return collectionIds;
	}

	/**
	 * Retrive the images of a collection
	 * 
	 * @param jsonCollection
	 *            JsonNode of all the Collections
	 * @return the images info
	 * @throws JSONException
	 */

	public static String retriveCollImages(final JsonNode jsonCollection)
			throws JSONException {

		final JSONArray imageIds = new JSONArray();
		final JsonNode object;
		JSONObject element, finalElement;
		String tmpImage;
		JsonNode imagesArr;
		final int i = 0;
		final JSONArray info = new JSONArray();

		if (jsonCollection.has("images")) {

			imagesArr = jsonCollection.get("images");

			for (final JsonNode image : imagesArr) {
				tmpImage = image.get("id").textValue();

				element = new JSONObject();
				element.put("id", tmpImage);

				imageIds.put(element);
			}
		}// if se c'è campo images
		/*
		 * while (i < jsonCollection.get("images").size()) { element = new
		 * JSONObject(); object = jsonCollection.get("images").get(i);
		 * element.put("id", object.get("id")); imageIds.put(element); i++; }
		 */
		finalElement = new JSONObject();
		finalElement.put("images", imageIds);
		info.put(finalElement);
		final String result = info.toString();
		return result;
		// return imageIds;
	}

	/**
	 * Close a particulr task
	 * 
	 * @param taskID
	 *            id of the task that I want to close
	 * @throws IOException
	 */
	public static void closeTask2(final String taskID) throws IOException {
		final CloseableHttpClient httpclient = HttpClients.createDefault();
		final HttpPut httpPut = new HttpPut(rootUrl + "/wsmc/task/" + taskID
				+ "/close");
		final CloseableHttpResponse response1 = httpclient.execute(httpPut);

		final HttpEntity entity1 = response1.getEntity();
		EntityUtils.consume(entity1);
		response1.close();
		httpclient.close();
	}

	/**
	 * Open a new task
	 * 
	 * @param taskType
	 *            The type (segmentation or tagging) of the new task that i want
	 *            to open
	 * @param selectedImg
	 *            The id of the image which will be associated to the new task
	 * @return The id of the new task
	 * @throws IOException
	 * @throws JSONException
	 */
	public static String addTask(final String taskType, final String selectedImg)
			throws IOException, JSONException {

		String newId;

		final CloseableHttpClient httpclient = HttpClients.createDefault();
		final HttpPost httpPost = new HttpPost(rootUrl + "/wsmc/task.json");
		final List<NameValuePair> nvps = new ArrayList<>();
		nvps.add(new BasicNameValuePair("taskType", taskType));
		nvps.add(new BasicNameValuePair("image", selectedImg));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));

		final CloseableHttpResponse response1 = httpclient.execute(httpPost);
		final HttpEntity entity1 = response1.getEntity();
		final BufferedReader in = new BufferedReader(new InputStreamReader(
				entity1.getContent()));
		final String inputLine = in.readLine();
		final JSONObject obj = new JSONObject(inputLine);
		newId = obj.getString("nid");
		EntityUtils.consume(entity1);
		response1.close();
		httpclient.close();
		return newId;
	}

	/**
	 * Open a new microTask
	 * 
	 * @param taskType
	 *            The type (segmentation or tagging) of the new microTask that i
	 *            want to open
	 * @param selectionTask
	 *            The id of the task which will be associated to the new
	 *            microTask
	 * @return The id of the new microTask
	 * @throws IOException
	 * @throws JSONException
	 */
	public static String addUTask(final String taskType,
			final String selectionTask) throws IOException, JSONException {

		String newId;

		final CloseableHttpClient httpclient = HttpClients.createDefault();
		final HttpPost httpPost = new HttpPost(rootUrl + "/wsmc/utask.json");
		final List<NameValuePair> nvps = new ArrayList<>();
		nvps.add(new BasicNameValuePair("taskType", taskType));
		nvps.add(new BasicNameValuePair("task", selectionTask));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));

		final CloseableHttpResponse response1 = httpclient.execute(httpPost);
		final HttpEntity entity1 = response1.getEntity();
		final BufferedReader in = new BufferedReader(new InputStreamReader(
				entity1.getContent()));
		final String inputLine = in.readLine();
		final JSONObject obj = new JSONObject(inputLine);
		newId = obj.getString("nid");
		EntityUtils.consume(entity1);
		response1.close();
		httpclient.close();
		return newId;
	}

	public static JSONArray loadFirst(final JsonNode jsonImages)
			throws JSONException {
		final JSONArray graph = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		JsonNode object;
		JSONObject element;
		int num = 0;

		int i = 0;
		while (i < jsonImages.size()) {
			sorting = new SortObject() {
			};
			object = jsonImages.get(i);
			sorting.setId(object.get("id").asText());
			if (object.has("descriptions")) {
				if (object.get("descriptions").has("segmentation")) {
					num = object.get("descriptions").get("segmentation").size();
				} else {
					num = 0;
				}
			}
			sorting.setNum(num);
			tempList.add(i, sorting);
			num = 0;
			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getNum() < o2.getNum()) {
					return -1;
				} else if (o1.getNum() > o2.getNum()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getNum();
		int count = 0;

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getNum() == tmp) {
				count++;
			} else if (obj.getNum() != tmp) {
				element = new JSONObject();
				element.put("occurence", count);
				element.put("annotations", tmp);
				graph.put(element);
				do {
					tmp++;
					count = 0;
					if (obj.getNum() != tmp) {
						element = new JSONObject();
						element.put("occurence", count);
						element.put("annotations", tmp);
						graph.put(element);
					}
				} while (obj.getNum() != tmp);
				count = 1;
			}
		}
		element = new JSONObject();
		element.put("occurence", count);
		element.put("annotations", tmp);
		graph.put(element);

		return graph;
	}

	public static JSONArray loadSecond(final JsonNode actions)
			throws JSONException {
		final JSONArray graph = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		final ArrayList<SortObject> tempList2 = new ArrayList<>();
		JsonNode object;
		JSONObject element;

		int i = 0;
		int j = 0;
		while (i < actions.size()) {
			object = actions.get(i);
			if (object.get("type").asText().equals("segmentation")) {
				if (object.has("user")) {
					if (object.get("user").has("cubrik_userid")) {
						sorting = new SortObject() {
						};
						sorting.setIdU(object.get("user").get("cubrik_userid")
								.asInt());
						tempList.add(j, sorting);
						j++;
					}
				}
			}

			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getIdU() < o2.getIdU()) {
					return -1;
				} else if (o1.getIdU() > o2.getIdU()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getIdU();
		int count = 0;

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getIdU() == tmp) {
				count++;
			} else if (obj.getIdU() != tmp) {
				sorting = new SortObject() {
				};
				sorting.setIdU(tmp);
				sorting.setNum(count);
				tempList2.add(sorting);
				do {
					tmp++;

				} while (obj.getIdU() != tmp);
				count = 1;
			}
		}
		sorting = new SortObject() {
		};
		sorting.setIdU(tmp);
		sorting.setNum(count);
		tempList2.add(sorting);

		Collections.sort(tempList2, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getNum() < o2.getNum()) {
					return -1;
				} else if (o1.getNum() > o2.getNum()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it2 = tempList2.iterator();
		tmp = 0;
		count = 0;

		while (it2.hasNext()) {
			final SortObject obj = it2.next();

			if (obj.getNum() == tmp) {
				count++;
			} else if (obj.getNum() != tmp) {
				element = new JSONObject();
				element.put("users", count);
				element.put("images", tmp);
				graph.put(element);
				do {
					tmp++;
					count = 0;
					if (obj.getNum() != tmp) {
						element = new JSONObject();
						element.put("users", count);
						element.put("images", tmp);
						graph.put(element);
					}
				} while (obj.getNum() != tmp);
				count = 1;
			}
		}
		element = new JSONObject();
		element.put("users", count);
		element.put("images", tmp);
		graph.put(element);

		return graph;
	}

	public static JSONArray download1(final JsonNode actions)
			throws JSONException {
		final JSONArray down1 = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		final ArrayList<DownObject> tempList2 = new ArrayList<>();
		JsonNode object;

		int i = 0;
		int j = 0;
		while (i < actions.size()) {
			object = actions.get(i);
			if (object.get("type").asText().equals("segmentation")) {
				if (object.has("user")) {
					if (object.get("user").has("cubrik_userid")) {
						sorting = new SortObject() {
						};
						sorting.setIdU(object.get("user").get("cubrik_userid")
								.asInt());
						sorting.setIdTmp(object.get("id").asInt());
						sorting.setImgTmp(object.get("imageid").asInt());
						tempList.add(j, sorting);
						j++;
					}
				}
			}

			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getIdU() < o2.getIdU()) {
					return -1;
				} else if (o1.getIdU() > o2.getIdU()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getIdU();
		DownObject user;
		StoredStatObj stat;
		ArrayList<StoredStatObj> elements = new ArrayList<>();

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getIdU() == tmp) {

				stat = new StoredStatObj(obj.getIdTmp(), obj.getImgTmp());
				elements.add(stat);
			} else if (obj.getIdU() != tmp) {
				user = new DownObject() {
				};
				user.setId(tmp);
				user.setElement(elements);
				tempList2.add(user);
				do {
					tmp++;

				} while (obj.getIdU() != tmp);
				elements = new ArrayList<>();
				stat = new StoredStatObj(obj.getIdTmp(), obj.getImgTmp());
				elements.add(stat);
			}
		}
		user = new DownObject() {
		};
		user.setId(tmp);
		user.setElement(elements);
		tempList2.add(user);

		final Iterator<DownObject> it2 = tempList2.iterator();

		JSONObject son;
		JSONArray body;
		JSONObject content;

		while (it2.hasNext()) {
			son = new JSONObject();
			body = new JSONArray();
			final DownObject obj = it2.next();
			son.put("user", obj.getId());

			final Iterator<StoredStatObj> it3 = obj.getElement().iterator();

			while (it3.hasNext()) {
				final StoredStatObj obj2 = it3.next();
				content = new JSONObject();
				content.put("segment", obj2.getId1());
				content.put("image", obj2.getId2());
				body.put(content);
			}
			son.put("sketch", body);
			down1.put(son);
		}

		return down1;
	}

	public static JSONArray download2(final JsonNode actions)
			throws JSONException {
		final JSONArray down2 = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		final ArrayList<DownObject> tempList2 = new ArrayList<>();
		JsonNode object;

		int i = 0;
		int j = 0;
		while (i < actions.size()) {
			object = actions.get(i);
			if (object.get("type").asText().equals("segmentation")) {
				if (object.has("user")) {
					if (object.get("user").has("cubrik_userid")) {
						sorting = new SortObject() {
						};
						sorting.setIdU(object.get("user").get("cubrik_userid")
								.asInt());
						sorting.setIdTmp(object.get("id").asInt());
						sorting.setImgTmp(object.get("imageid").asInt());
						tempList.add(j, sorting);
						j++;
					}
				}
			}

			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getImgTmp() < o2.getImgTmp()) {
					return -1;
				} else if (o1.getImgTmp() > o2.getImgTmp()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getImgTmp();
		DownObject image;
		StoredStatObj stat;
		ArrayList<StoredStatObj> elements = new ArrayList<>();

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getImgTmp() == tmp) {

				stat = new StoredStatObj(obj.getIdTmp(), obj.getIdU());
				elements.add(stat);
			} else if (obj.getImgTmp() != tmp) {
				image = new DownObject() {
				};
				image.setId(tmp);
				image.setElement(elements);
				tempList2.add(image);
				do {
					tmp++;

				} while (obj.getImgTmp() != tmp);
				elements = new ArrayList<>();
				stat = new StoredStatObj(obj.getIdTmp(), obj.getIdU());
				elements.add(stat);
			}
		}
		image = new DownObject() {
		};
		image.setId(tmp);
		image.setElement(elements);
		tempList2.add(image);

		final Iterator<DownObject> it2 = tempList2.iterator();

		JSONObject son;
		JSONArray body;
		JSONObject content;

		while (it2.hasNext()) {
			son = new JSONObject();
			body = new JSONArray();
			final DownObject obj = it2.next();
			son.put("image", obj.getId());

			final Iterator<StoredStatObj> it3 = obj.getElement().iterator();

			while (it3.hasNext()) {
				final StoredStatObj obj2 = it3.next();
				content = new JSONObject();
				content.put("segment", obj2.getId1());
				content.put("user", obj2.getId2());
				body.put(content);
			}
			son.put("sketch", body);
			down2.put(son);
		}

		return down2;
	}

	/*
	 * Returns a tag based on a particular choice policy
	 * 
	 * @return String retrieved tag following a policy
	 */
	private static String chooseTag(final HashSet<String> tags) {
		if (tags != null && tags.size() > 0) {

			final Object[] stringTags = tags.toArray();
			final String toReturn = (String) stringTags[(new Random()
			.nextInt(tags.size()))];
			return toReturn;

		} else {
			return "";
		}
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
			final Map<K, V> map) {
		final List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(final Map.Entry<K, V> o1,
					final Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		final Map<K, V> result = new LinkedHashMap<K, V>();
		for (final Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

}
