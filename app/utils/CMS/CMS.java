package utils.CMS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;
import scala.concurrent.duration.Duration;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.CMS.models.Action;
import utils.CMS.models.CMSObject;
import utils.CMS.models.ChooseImage;
import utils.CMS.models.ChooseImageTag;
import utils.CMS.models.Collection;
import utils.CMS.models.History;
import utils.CMS.models.Image;
import utils.CMS.models.Mask;
import utils.CMS.models.MicroTask;
import utils.CMS.models.Point;
import utils.CMS.models.SegmentToClose;
import utils.CMS.models.Segmentation;
import utils.CMS.models.Session;
import utils.CMS.models.Tag;
import utils.CMS.models.Task;
import utils.CMS.models.User;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.Room;
import akka.actor.Cancellable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CMS {

	private final static String rootUrl = Play.application().configuration()
			.getString("cmsUrl");
	private final static Integer timeoutPostCMS = Play.application()
			.configuration().getInt("cmsTimeoutPost");
	private final static Integer timeout = Play.application().configuration()
			.getInt("cmsTimeout");

	private final static Integer collection = Play.application()
			.configuration().getInt("collection");

	private final static String useImageWithNoTags = Play.application()
			.configuration().getString("useImageWithNoTags");

	private final static String policy = Play.application().configuration()
			.getString("policy");
	private static final long MAX_OPEN_ACTION = 1800000;

	private static HashMap<String, Cancellable> runningThreads = new HashMap<String, Cancellable>();

	// Minimum tags that an image should have to avoid asking to the
	// users for new tags
	private static Integer minimumTags = Integer.parseInt(Play.application()
			.configuration().getString("minimumTags"));

	public static List<Image> getImages() throws CMSException {
		return getObjs(Image.class, "image", "images");
	}

	public static List<Action> getActions() throws CMSException {
		return getObjs(Action.class, "action", "actions");
	}

	// public static List<Image> getImages() throws CMSException {
	// return getObjs(Image.class, "image", "images");
	// }

	public static Image getImage(final Integer id) throws CMSException {
		return getObj(utils.CMS.models.Image.class, "image", id, "image");
	}

	public void postImage(final Image i) throws CMSException {
		postObj(i, "image", Image.class);
	}

	private static <T extends Object> T postObj(final T obj,
			final String service, final Class<T> claz) throws CMSException {
		try {

			final F.Promise<WS.Response> returned;
			final WSRequestHolder prov = WS.url(rootUrl + "/" + service)
					.setHeader("Accept", "application/json")
					.setTimeout(timeoutPostCMS);

			if (obj != null) {
				returned = prov.post(Json.toJson(obj));
			} else {
				returned = prov.post("");
			}

			final String respBody = returned.get().getBody();
			return Json.fromJson(Json.parse(respBody).get("id"), claz);

		} catch (final Exception e) {
			Logger.error("Unable to post: " + service, e);
			throw new CMSException("Unable to post: " + service);
		}

	}

	private static <T extends Object> Integer postObj2(final T obj,
			final String service) throws CMSException {
		try {

			final F.Promise<WS.Response> returned;
			final WSRequestHolder prov = WS.url(rootUrl + "/" + service)
					.setHeader("Accept", "application/json")
					.setTimeout(timeoutPostCMS);

			if (obj != null) {

				final JsonNode a = Json.toJson(obj);
				returned = prov.post(a);
			} else {
				returned = prov.post("");
			}

			final String respBody = returned.get().getBody();
			Logger.debug("Output for post on " + service + " : " + respBody);
			if (Json.parse(respBody).get("id") != null) {

				return Json.parse(respBody).get("id").asInt();
			}
			return 0;

		} catch (final Exception e) {
			Logger.error("Unable to post: " + service, e);
			return 0;
			// throw new CMSException("Unable to post: " + service);
		}

	}

	private static <T extends Object> Integer postObjString(final String obj,
			final String service) throws CMSException {
		try {

			final F.Promise<WS.Response> returned;
			final WSRequestHolder prov = WS.url(rootUrl + "/" + service)
					.setHeader("Accept", "application/json")
					.setHeader("Content-Type", "application/json")
					.setTimeout(timeoutPostCMS);

			if (obj != null) {

				returned = prov.post(obj);
			} else {
				returned = prov.post("");
			}

			final String respBody = returned.get().getBody();
			Logger.debug("Output for post on " + service + " : " + respBody);
			if (Json.parse(respBody).get("id") != null) {

				return Json.parse(respBody).get("id").asInt();
			}
			return 0;

		} catch (final Exception e) {
			Logger.error("Unable to post: " + service, e);
			return 0;
			// throw new CMSException("Unable to post: " + service);
		}

	}

	private static void postObj(final String service, final Integer id)
			throws CMSException {
		try {
			final F.Promise<WS.Response> returned = WS
					.url(rootUrl + "/" + service + "/" + id)
					.setTimeout(timeoutPostCMS).post("");

			returned.get();

		} catch (final Exception e) {
			throw new CMSException("Unable to post: " + service);
		}

	}

	private static <T extends Object> void updateObj(final T obj,
			final String service, final HashMap<String, String> params)
					throws CMSException {
		final F.Promise<WS.Response> returned;
		final WSRequestHolder ws = WS.url(rootUrl + "/" + service)
				.setHeader("Accept", "application/json")
				.setHeader("Content-Type", "application/json")
				.setTimeout(timeoutPostCMS);
		final Iterator<String> it = params.keySet().iterator();
		while (it.hasNext()) {
			final String parId = it.next();
			ws.setQueryParameter(parId, params.get(parId));
		}

		try {

			final JsonNode body = Json.toJson(obj);
			returned = ws.put(body);

			final String respBody = returned.get().getBody();
			Logger.debug("Output for put on " + service + " : " + respBody);

		} catch (final Exception e) {
			throw new CMSException("Unable to put: " + service);
		}

	}

	private <T extends Object> void deleteObj(final T obj,
			final String service, final String id) throws CMSException {
		try {
			WS.url(rootUrl + "/" + service + "/" + id)
			.setTimeout(timeoutPostCMS).delete();
		} catch (final Exception e) {
			throw new CMSException("Unable to post: " + service);
		}

	}

	private static <T extends Object> T getObj(final Class<T> claz,
			final String service, final Integer id,
			final HashMap<String, String> params, final String response)
					throws CMSException {
		final CMSJsonReader jsonReader = new CMSJsonReader();

		final JsonNode result = jsonReader.readJsonFromUrl(rootUrl, service,
				String.valueOf(id), params, response);
		return Json.fromJson(result, claz);
	}

	private static <T extends Object> T getObj(final Class<T> claz,
			final String service, final Integer id, final String response)
					throws CMSException {
		return getObj(claz, service, id, null, response);
	}

	public static <T extends CMSObject> List<T> getObjs(final Class<T> claz,
			final String service, final HashMap<String, String> params,
			final String response) throws CMSException {
		final CMSJsonReader jsonReader = new CMSJsonReader();

		// ListWrap<T> lista = new ListWrap<T>();
		// final JsonNode result = jsonReader.readJsonFromUrl(rootUrl, service,
		// params, response);
		final JsonNode result = jsonReader.readJsonFromUrl2(rootUrl, service,
				params, response);

		// lista = Json.fromJson(result, lista.getClass());

		final List<T> lista = new ArrayList<>();

		if (result != null) {
			for (final JsonNode jsonNodeInner : result) {
				final T mobj = Json.fromJson(jsonNodeInner, claz);
				lista.add(mobj);
			}

		}

		// return lista.getLista();
		return lista;
	}

	private static <T extends CMSObject> List<T> getObjs(final Class<T> claz,
			final String service, final Integer count, final Integer max_id,
			final Integer since_id, final Boolean populate,
			final String response) throws CMSException {

		return getObjs(claz, service,
				buildParams(count, max_id, since_id, populate), response);
	}

	private static HashMap<String, String> buildParams(final Integer count,
			final Integer max_id, final Integer since_id, final Boolean populate) {
		final HashMap<String, String> params = new HashMap<>();
		if (count != null) {
			params.put("count", String.valueOf(count));
		}
		if (max_id != null) {
			params.put("max_id", String.valueOf(max_id));
		}
		if (since_id != null) {
			params.put("since_id", String.valueOf(since_id));
		}
		if (populate != null) {
			params.put("populate", String.valueOf(populate));
		}
		return params;
	}

	private static <T extends CMSObject> List<T> getObjs(final Class<T> claz,
			final String service, final String response) throws CMSException {

		return getObjs(claz, service, null, response);
	}

	public static void closeUTask(final Integer uTaskID, final Integer actionId)
			throws CMSException {
		if (uTaskID != null) {
			postObj("microtask", uTaskID);
		}
	}

	public static void closeTask(final Integer taskID) throws CMSException {
		if (taskID != null) {
			postObj("task", taskID);
			LoggerUtils.debug("CMS", "Closing Task " + taskID);
		}
	}

	public static void invalidateTag(final String tagID, final String imageID)
			throws CMSException {

		final HashMap<String, String> body = new HashMap<>();
		body.put("validity", "true");
		final HashMap<String, String> params = new HashMap<>();
		params.put("image", imageID);
		params.put("tag", tagID);
		updateObj(body, "tag", params);

	}

	public static void postSegmentationOnAkka(final ObjectNode finalTraces,
			final String username, final Integer session,
			final HashMap<String, Integer> openActionsSeg,
			final HashMap<String, Integer> openActionsTag) throws Exception {
		Akka.system()
		.scheduler()
		.scheduleOnce(Duration.create(200, TimeUnit.MILLISECONDS),
				new Runnable() {
			@Override
			public void run() {
				postSegmentation(finalTraces, username,
						session, openActionsSeg, openActionsTag);
			}
		}, Akka.system().dispatcher());
	}

	public static void postSegmentation(final ObjectNode finalTraces,
			final String username, final Integer session,
			final HashMap<String, Integer> openActionsSeg,
			final HashMap<String, Integer> openActionsTag) {

		try {

			final Integer userId = postUser(username);

			final String image = finalTraces.get("id").textValue();
			final String label = finalTraces.get("label").textValue();
			final Integer tagId = saveTag(label);
			if (openActionsTag.containsKey(image)) {
				// era un azione di tag, il tag l ho gia salvato

				final ChooseImageTag stc = new ChooseImageTag(tagId);
				postObj2(stc, "action/" + openActionsTag.get(image));
			} else {

				postTagAction(userId, session, image, tagId);
			}

			final ArrayNode traces = (ArrayNode) finalTraces.get("traces");
			final JsonNode history = finalTraces.get("history");

			final List<utils.CMS.models.Point> points = readTraces(traces);

			final List<History> historyPoints = readHistory(history);

			// TODO fix quality
			final double quality = 0;

			final Segmentation segmentation = new Segmentation(points,
					historyPoints, quality);

			// final Segmentation segmentation = new Segmentation(points,
			// historyPoints);

			if (openActionsSeg.get(image + tagId) != null) {
				// esiste già l'azione devo solo chiuderla
				final SegmentToClose stc = new SegmentToClose(tagId, points,
						historyPoints);
				postObj2(stc, "action/" + openActionsSeg.get(image + tagId));

			} else {
				final Action action = Action.createSegmentationAction(
						Integer.valueOf(image), session, tagId, userId, true,
						points, historyPoints);
				postAction(action);
			}

		} catch (final Exception ex) {
			LoggerUtils.error("Unable to save segmentation, EXC2", ex);
		}

	}

	private static List<History> readHistory(final JsonNode history) {
		final List<History> hs = new ArrayList<>();

		final Iterator<JsonNode> histoPezzi = history.elements();
		// final int i = 0;
		String lastColor = "rgb(255, 0, 0)";
		while (histoPezzi.hasNext()) {
			final JsonNode histoPezzo = histoPezzi.next();
			final History h = new History();
			final ArrayNode jpoints = (ArrayNode) histoPezzo.get("points");
			final JsonNode first = jpoints.get(0);
			if (first != null) {

				String color = first.get("color").asText();
				if (color.equals("end")) {
					color = lastColor;
				} else {
					lastColor = color;
				}
				h.setColor(color);
				final List<Point> points = readHistoPoints(histoPezzo);
				h.setPoints(points);
				h.setSize(first.get("size").asInt());
				h.setTime(histoPezzo.get("time").asInt());
				// h.setTime(i++);
				hs.add(h);
			}

		}

		return hs;
	}

	private static List<Point> readHistoPoints(final JsonNode histoPezzo) {
		final List<Point> ps = new ArrayList<>();
		final ArrayNode jpoints = (ArrayNode) histoPezzo.get("points");
		for (final JsonNode jpoint : jpoints) {
			ps.add(new utils.CMS.models.Point(jpoint.get("x").asInt(), jpoint
					.get("y").asInt()));

		}
		return ps;
	}

	private static List<utils.CMS.models.Point> readTraces(
			final ArrayNode traces) {

		final List<utils.CMS.models.Point> points = new ArrayList<>();

		for (final JsonNode trace : traces) {
			final boolean end = trace.get("color").asText().equals("end");

			points.add(new utils.CMS.models.Point(trace.get("x").asInt(), trace
					.get("y").asInt(), end));
		}

		return points;

	}

	public static Integer saveTag(final String label) throws CMSException {

		return postObj2(new utils.CMS.models.Tag(label), "tag");
		// final utils.CMS.models.Tag tag = postObj(
		// new utils.CMS.models.Tag(label), "tag",
		// utils.CMS.models.Tag.class);
		// return tag.getId();
	}

	public static Integer postUser(final String username) throws CMSException {
		final utils.CMS.models.User user = new User(username);
		return postObj2(user, "user");
	}

	public static Integer postAction(final Action action) throws CMSException {
		return postObj2(action, "action");
	}

	public static void saveTagActionOnAkka(final ObjectNode finalTraces,
			final Integer userId, final Integer session, final String image,
			final Integer tagId) throws Exception {
		Akka.system()
		.scheduler()
		.scheduleOnce(
				Duration.create(timeoutPostCMS, TimeUnit.MILLISECONDS),
				new Runnable() {
					@Override
					public void run() {
						try {
							postTagAction(userId, session, image, tagId);
						} catch (final CMSException e) {
							Logger.error("Unable to save tag action.",
									e);
						}
					}
				}, Akka.system().dispatcher());
	}

	public static Integer postTagAction(final Integer userId,
			final Integer session, final String image, final Integer tagId)
					throws CMSException {

		final Action action = Action.createTagAction(Integer.valueOf(image),
				session, tagId, userId, true);
		return postAction(action);

	}

	public static Integer openSession() throws CMSException {
		// final Session session = postObj(null, "session", Session.class);
		// final Integer sessionId = session.getId();
		final Integer sessionId = postObj2(null, "session");
		LoggerUtils.debug("CMS", "Retrieved session " + sessionId);
		return sessionId;
	}

	public static void closeSession(final Integer sessionId)
			throws CMSException {
		postObj2(null, "session/" + sessionId);
		LoggerUtils.debug("CMS", "Closing session " + sessionId);
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

	public static void taskSetInitialization(
			final List<ObjectNode> priorityTaskHashSet,
			final List<ObjectNode> queueImages, final Room roomChannel,
			final Integer maxRound) throws Error, Exception {
		int uploadedTasks = 0;
		try {
			uploadedTasks = retrieveTasks(maxRound, priorityTaskHashSet,
					roomChannel);
		} catch (final Exception e) {
			LoggerUtils.error("CMS", "Unable to read tasks");
		}

		int tasksToAdd = maxRound - uploadedTasks;
		if (tasksToAdd > 0 && useImageWithNoTags.equals("true")) {
			uploadedTasks = retrieveImagesWithoutTag(tasksToAdd, queueImages,
					roomChannel, uploadedTasks > 0, uploadedTasks);

		}
		tasksToAdd = maxRound - uploadedTasks;
		if (tasksToAdd > 0) {
			retrieveImagesCERT(tasksToAdd, queueImages, roomChannel,
					uploadedTasks > 0);

		}

		LoggerUtils.debug("CMS", "Task init from CMS end");
	}

	private static int retrieveImagesWithoutTag(final Integer tasksToAdd,
			final List<ObjectNode> queueImages, final Room roomChannel,
			boolean taskSent, int uploadedTasks) {

		final List<ChooseImage> imgs;
		final List<ChooseImageTag> imgtgs = new ArrayList<>();
		try {
			LoggerUtils.debug("CMS", "Requested image list to CMS");
			imgs = CMS.getChooseImageOnly(collection, tasksToAdd.toString());
			if (imgs != null) {
				for (final ChooseImage imgtg : imgs) {
					if (imgtg.getCount() >= minimumTags) {
						break;
					}
					imgtgs.add(0, new ChooseImageTag(imgtg.getImage(), -1));
				}
			}
			LoggerUtils.debug("CMS", "Requested image list to CMS end");
		} catch (final Exception e) {
			throw new RuntimeException(
					"[CMS] The request to the CMS is malformed");
		}

		for (final ChooseImageTag imgtg : imgtgs) {
			// Save information related to the image
			final Integer id = imgtg.getImage();

			final ObjectNode guessWord = Json.newObject();
			guessWord.put("type", "task");
			guessWord.put("id", String.valueOf(id));
			// Find the valid tags for this task.

			try {
				buildGuessWordSegment(guessWord, imgtg.getTag(),
						CMS.getImage(id));
				uploadedTasks = uploadedTasks + 1;
			} catch (final CMSException e) {
				Logger.error("Unable to read image, ignoring...", e);
			}

			queueImages.add(guessWord);

			if (!taskSent) {
				taskSent = true;
				LoggerUtils.debug("CMS", "Send task aquired for image:" + id
						+ ", rooomChanel: " + roomChannel);
				sendTaskAcquired(roomChannel);
			}
		}
		return uploadedTasks;
	}

	public static void cleanOpenActions() throws CMSException {
		// final String[] invalid = { "20254" };
		// for (final String a : invalid) {
		// invalidateAction(a);
		// }

		final List<Action> actions = getAllOpenActions();
		for (final Action a : actions) {
			final String completed = a.getCompleted_at();
			if (completed != null) {
				continue;
			}

			final String started = a.getCreated_at();
			final Calendar date = javax.xml.bind.DatatypeConverter
					.parseDateTime(started);
			final Calendar now = Calendar.getInstance();
			final long diff = now.getTimeInMillis() - date.getTimeInMillis();
			if (diff > MAX_OPEN_ACTION) {
				System.out.println("Closing action");
				closeAction(a.getId());
			}
		}
	}

	private static List<Action> getAllOpenActions() throws CMSException {
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("completed", "false");
		params.put("type", "segmentation");
		final List<utils.CMS.models.Action> as = CMS.getAllObjs(Action.class,
				"action", params, "actions");
		return as;
	}

	private static void invalidateAction(final String a) throws CMSException {
		final HashMap<String, String> body = new HashMap<>();
		body.put("validity", "false");
		final HashMap<String, String> params = new HashMap<>();

		updateObj(body, "action/" + a, params);

	}

	private static void retrieveImages(final Integer tasksToAdd,
			final List<ObjectNode> queueImages, final Room roomChannel,
			boolean taskSent) throws Exception {

		List<ChooseImageTag> imgtgs;
		try {
			LoggerUtils.debug("CMS", "Requested image list to CMS");

			imgtgs = CMS.getChoose(collection, tasksToAdd.toString(), policy);

			LoggerUtils.debug("CMS", "Requested image list to CMS end");
		} catch (final Exception e) {
			throw new Exception("[CMS] The request to the CMS is malformed", e);
		}

		for (final ChooseImageTag imgtg : imgtgs) {
			// Save information related to the image
			final Integer id = imgtg.getImage();

			final ObjectNode guessWord = Json.newObject();
			guessWord.put("type", "task");
			guessWord.put("id", String.valueOf(id));
			// Find the valid tags for this task.

			try {
				buildGuessWordSegment(guessWord, imgtg.getTag(),
						CMS.getImage(id));
			} catch (final CMSException e) {
				Logger.error("Unable to read image, ignoring...", e);
			}

			queueImages.add(guessWord);

			if (!taskSent) {
				taskSent = true;
				LoggerUtils.debug("CMS", "Send task aquired for image:" + id
						+ ", rooomChanel: " + roomChannel);
				sendTaskAcquired(roomChannel);
			}
		}

	}

	private static void retrieveImagesCERT(final Integer tasksToAdd,
			final List<ObjectNode> queueImages, final Room roomChannel,
			final boolean taskSent) throws Exception {

		List<ChooseImageTag> imgtgs = new ArrayList<>();
		final List<ChooseImageTag> imgtgsDress;
		try {
			LoggerUtils.debug("CMS", "Requested image list to CMS");

			final int tot = tasksToAdd - 3;
			imgtgs = CMS.getChoose(1, "3", policy);
			imgtgsDress = CMS.getChoose(4, String.valueOf(tot), policy);
			imgtgs.addAll(imgtgsDress);

			LoggerUtils.debug("CMS", "Requested image list to CMS end");
		} catch (final Exception e) {
			throw new Exception("[CMS] The request to the CMS is malformed", e);
		}

		Collections.shuffle(imgtgs);

		for (final ChooseImageTag imgtg : imgtgs) {

			addImgToQueue(imgtg, queueImages, roomChannel, taskSent);

		}

	}

	private static void addImgToQueue(final ChooseImageTag imgtg,
			final List<ObjectNode> queueImages, final Room roomChannel,
			boolean taskSent) {
		final Integer id = imgtg.getImage();

		final ObjectNode guessWord = Json.newObject();
		guessWord.put("type", "task");
		guessWord.put("id", String.valueOf(id));
		// Find the valid tags for this task.

		try {
			buildGuessWordSegment(guessWord, imgtg.getTag(), CMS.getImage(id));
		} catch (final CMSException e) {
			Logger.error("Unable to read image, ignoring...", e);
		}

		queueImages.add(guessWord);

		if (!taskSent) {
			taskSent = true;
			LoggerUtils.debug("CMS", "Send task aquired for image:" + id
					+ ", rooomChanel: " + roomChannel);
			sendTaskAcquired(roomChannel);
		}

	}

	private static void openAction(final Integer image, final Integer session,
			final Integer tagId, final Integer userId) throws CMSException {
		final Action action = Action.createSegmentationAction(image, session,
				tagId, userId, true);
		postAction(action);
	}

	private static List<ChooseImageTag> getChoose(final Integer collection2,
			final String limit, final String policy) throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("limit", limit);
		params.put("collection", String.valueOf(collection2));
		return getObjs(ChooseImageTag.class, "choose/imageandtag/" + policy,
				params, "results");
	}

	private static List<ChooseImage> getChooseImageOnly(
			final Integer collection2, final String limit) throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("limit", limit);
		params.put("collection", String.valueOf(collection2));
		return getObjs(ChooseImage.class, "choose/image/" + policy, params,
				"results");
	}

	private static int retrieveTasks(final Integer maxRound,
			final List<ObjectNode> priorityTaskHashSet, final Room roomChannel) {
		boolean taskSent = false;

		int uploadedTasks = 0;

		final List<Task> tasklist;
		try {
			LoggerUtils.debug("CMS", "Requested task list to CMS "
					+ roomChannel);

			tasklist = getTaskCollection(collection);

			LoggerUtils.debug("CMS", "Requested task list to CMS end "
					+ roomChannel);

		} catch (final CMSException e) {
			throw new RuntimeException(
					"[CMS] Unable to download collection from CMS");
		}

		if (tasklist == null || tasklist.size() == 0) {
			return 0;
		}
		try {
			// for (final Task taskId : tasklist) {
			for (final Task t : tasklist) {
				final Integer taskId = t.getId();
				// final utils.CMS.models.Task t = getTask(taskId);

				final List<MicroTask> uTasks = CMS.getMicroTasks(String
						.valueOf(taskId));
				if (uTasks == null || uTasks.size() > 0) {
					continue;
				}

				final Integer imageId = t.getImage();
				final Image image = getImage(imageId);

				for (final MicroTask utask : uTasks) {

					final ObjectNode guessWord = Json.newObject();
					guessWord.put("type", "task");
					guessWord.put("id", imageId);

					final String type = utask.getType();
					switch (type) {
					case "tagging":
						buildGuessWordTagging(guessWord, image, utask, taskId);
						priorityTaskHashSet.add(guessWord);
						uploadedTasks++;
						if (!taskSent) {
							taskSent = true;
							sendTaskAcquired(roomChannel);
						}
						break;
					case "segmentation":
						final Integer tagid = t.getTag();

						buildGuessWordSegmentTask(guessWord, tagid, image,
								String.valueOf(taskId), utask);

						priorityTaskHashSet.add(guessWord);
						uploadedTasks++;
						if (!taskSent) {
							taskSent = true;
							sendTaskAcquired(roomChannel);
						}
						break;
					}
					break;

				}

			}
		} catch (final CMSException e) {
			throw new RuntimeException("[CMS] Unable to download task from CMS");
		}
		return uploadedTasks;
	}

	private static void buildGuessWordSegment(final ObjectNode guessWord,
			final Integer tagId, final Image image) throws CMSException {
		// Add one tag among the ones that have been retrieved following
		// a particular policy
		final Tag t = CMS.getTag(tagId);
		final String tag = t.getName();

		guessWord.put("tag", tag);
		guessWord.put("lang", LanguagePicker.retrieveIsoCode());
		guessWord.put("image", rootUrl + image.getMediaLocator());
		guessWord.put("width", image.getWidth());
		guessWord.put("height", image.getHeight());

	}

	private static Tag getTag(final Integer tagId) throws CMSException {
		if (tagId >= 0)
			return getObj(Tag.class, "tag", tagId, "tag");
		else
			// The image has no tag associated to it
			return new Tag("empty");
	}

	private static void buildGuessWordSegmentTask(final ObjectNode guessWord,
			final Integer tagId, final Image image, final String taskId,
			final MicroTask utask) throws CMSException {
		buildGuessWordSegment(guessWord, tagId, image);
		guessWord.put("utaskid", utask.getId());
		guessWord.put("taskid", taskId);

	}

	private static List<Task> getTaskCollection(final Integer collection2)
			throws CMSException {
		return getObjs(Task.class, "collection/" + collection2 + "/task",
				"tasks");
	}

	private static void buildGuessWordTagging(final ObjectNode guessWord,
			final Image image, final MicroTask utask, final Integer taskId) {
		// devono taggare, non aggiungo tag
		guessWord.put("tag", "");
		guessWord.put("lang", LanguagePicker.retrieveIsoCode());
		guessWord.put("image", rootUrl + image.getMediaLocator());
		guessWord.put("width", image.getWidth());
		guessWord.put("height", image.getHeight());
		guessWord.put("utaskid", utask.getId());
		guessWord.put("taskid", String.valueOf(taskId));

	}

	private static MicroTask getMicroTask(final Integer utaskid)
			throws CMSException {
		return getObj(MicroTask.class, "microtask", utaskid, "microtask");
	}

	private static utils.CMS.models.Task getTask(final Integer id)
			throws CMSException {
		return getObj(utils.CMS.models.Task.class, "task", id, "task");
	}

	public static Collection getCollection(final Integer collection2)
			throws CMSException {
		return getObj(Collection.class, "collection", collection2, "collection");
	}

	public static List<utils.CMS.models.Task> getTasks() throws CMSException {
		return getObjs(utils.CMS.models.Task.class, "task", "tasks");
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

	public static List<User> getAllUsers() throws CMSException, JSONException {
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("populate", "true");
		final List<utils.CMS.models.User> users = CMS.getAllObjs(User.class,
				"user", params, "users");
		return users;
	}

	private static <T extends CMSObject> List<T> getAllObjs(
			final Class<T> claz, final String service,
			final HashMap<String, String> params, final String response)
					throws CMSException {

		final CMSJsonReader jsonReader = new CMSJsonReader();
		final List<JsonNode> result = jsonReader.readJsonAllFromUrl(rootUrl,
				service, params, response);

		// lista = Json.fromJson(result, lista.getClass());

		final List<T> lista = new ArrayList<>();

		if (result != null) {
			for (final JsonNode json : result) {
				for (final JsonNode jsonNodeInner : json) {
					final T mobj = Json.fromJson(jsonNodeInner, claz);
					lista.add(mobj);
				}
			}

		}

		return lista;

	}

	public static List<User> getUsers() throws CMSException, JSONException {
		// return getObjs(User.class, "user?populate=true", "users");
		/*
		 * HashMap<String, String> params = new HashMap<String, String>();
		 * params.put("populate", "true"); return getObjs(User.class, "user",
		 * params, "users");
		 */
		// get ALL users
		Boolean end = false;
		final List<utils.CMS.models.User> users = new ArrayList<utils.CMS.models.User>();
		String max_id = "null";
		final String count = String.valueOf(100);

		while (!end) {
			final HashMap<String, String> params = new HashMap<String, String>();
			params.put("populate", "true");
			if (!max_id.equals("null")) {
				params.put("max_id", max_id);
				params.put("count", count);
			}

			try {
				final List<utils.CMS.models.User> nextUsers = CMS.getObjs(
						User.class, "user", params, "users");
				for (int k = 0; k < nextUsers.size(); k++) {
					users.add(nextUsers.get(k));
				}
			} catch (final CMSException e) {
				Logger.error("Unable to read users from cms", e);
				throw new JSONException("Unable to read users from cms");
			}

			// GET next result: max_id, count
			final String service = "user";
			final String response = "search_metadata";

			Promise<WS.Response> res;
			final WSRequestHolder wsurl = WS.url(rootUrl + "/" + service)
					.setHeader("Accept", "application/json")
					.setTimeout(timeout);
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

			if (res != null) {
				final Response result2 = res.get(1000000L);
				final JsonNode json = result2.asJson();
				JsonNode node = null;
				if (json.get("status").asText().equals("OK")) {
					node = json.get(response);
					if (node.has("next_results")) {
						final String nextResult = node.get("next_results")
								.asText();
						final String[] tokens = nextResult.split("=");
						max_id = tokens[1].split("&")[0];
						// count = tokens[2];
					} else {
						end = true;
					}
				} else {
					throw new CMSException(
							"Internal Server Error while invoking CMS: "
									+ json.get("error"));
				}
			} else {
				throw new IllegalStateException("CMS response timeout.");
			}
		}

		return users;

	}

	public static Integer getUserCount() throws CMSException {
		final Integer count = getCount("user");
		return count;
	}

	private static Integer getCount(final String service,
			final HashMap<String, String> params) throws CMSException {
		final CMSJsonReader jsonReader = new CMSJsonReader();

		final JsonNode result = jsonReader.readJsonFromUrl(rootUrl, service
				+ "/count", params, "count");
		return result.asInt();
	}

	private static Integer getCount(final String service) throws CMSException {

		return getCount(service, null);
	}

	public static Integer getImageCount() throws CMSException {
		return getCount("image");
	}

	public static Integer getTagActionCount() throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("type", "tagging");
		return getCount("action", params);
	}

	public static Integer getSegActionCount() throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("type", "segmentation");
		return getCount("action", params);
	}

	public static List<utils.CMS.models.Tag> getTagsByImage(
			final Integer imageId) throws CMSException {

		return getObjs(utils.CMS.models.Tag.class, "image/" + imageId + "/tag",
				"tags");
	}

	public static Mask getMask(final Integer imageId, final Integer tagId)
			throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("image", String.valueOf(imageId));
		params.put("tag", String.valueOf(tagId));
		final List<Mask> masks = getObjs(Mask.class, "mask", params, "masks");
		if (masks != null && masks.size() > 0) {
			return masks.get(0);
		}
		return null;
	}

	public static List<Collection> getCollections() throws CMSException {
		return getObjs(Collection.class, "collection", "collections");
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
	 * @throws CMSException
	 */
	public static Integer addTask(final String taskType,
			final String selectedImg) throws CMSException {

		Task task = new Task(Integer.valueOf(selectedImg));
		task = postObj(task, "task", Task.class);

		return task.getId();

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
	 * 
	 * @throws CMSException
	 */
	public static Integer addUTask(final String taskType,
			final String selectionTask) throws CMSException {

		MicroTask microtask = new MicroTask(taskType,
				Integer.valueOf(selectionTask), 0);
		microtask = postObj(microtask, "microtask", MicroTask.class);
		return microtask.getId();
	}

	public static List<Action> getSegmentationsByImage(final Integer id)
			throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("image", String.valueOf(id));
		params.put("type", "segmentation");
		final List<Action> segs = getObjs(Action.class, "action", params,
				"action");
		return segs;
	}

	public static List<MicroTask> getMicroTasks(final String taskId)
			throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("task", taskId);
		return getObjs(MicroTask.class, "microtask", params, "microtasks");
	}

	public static Integer getTagId(final String name) throws CMSException {
		final String service = "tag";
		try {
			final F.Promise<WS.Response> returned;
			final WSRequestHolder prov = WS.url(rootUrl + "/" + service)
					.setHeader("Accept", "application/json")
					.setTimeout(timeoutPostCMS);

			final ObjectNode node = JsonNodeFactory.instance.objectNode();
			node.put("name", name);
			returned = prov.post(node);
			final String respBody = returned.get().getBody();
			return Json.parse(respBody).get("id").asInt();

		} catch (final Exception e) {
			Logger.error("Unable to post: " + service, e);
			throw new CMSException("Unable to post: " + service);
		}

	}

	public static List<Action> getSegmentationsByImageAndTag(
			final Integer imageid, final Integer tagId) throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("image", String.valueOf(imageid));
		params.put("tag", String.valueOf(tagId));
		params.put("type", "segmentation");
		final List<Action> segs = getObjs(Action.class, "action", params,
				"action");
		return segs;
	}

	public static Action getAction(final Integer id) throws CMSException {
		return getObj(Action.class, "action", id, "action");
	}

	public static void test() throws CMSException {

		CMS.addImagesToCollection();

		// OK
		// final Tag ims = CMS.getTag(0);
		// System.out.println("ciao" + ims.getId());

		// final Image sd = CMS.getImage(0);
		// final Integer s = CMS.getImageCount();
		// final Integer ss = CMS.getUserCount();
		// final List<User> use = CMS.getUsers();
		// final List<Collection> sd = CMS.getCollections();
		// final Collection ss = CMS.getCollection(1);
		// final Integer use = CMS.openSession();
		// final int sd = CMS.getSegmentationCount();

		// TODO
		// final List<Task> t = CMS.getTasks();
		// final List<Task> s = CMS.getTaskCollection(1);
		// CMS.closeSession(8);

		// final Integer s = CMS.postUser("pippo");

		// System.out.println("ciao" + sd);
		// System.out.println("ciao" + s);
		// System.out.println("ciao" + ss);
		// System.out.println("ciao" + use);
		// System.out.println("ciao" + t);

		// getSegmentationsByImageAndTag
		// getTagId
		// getMicroTasks
		// getSegmentationsByImage
		// addUTask
		// addTask
		// getMask
		// getTagsByImage
		// getSegActionCount
		// getTagActionCount
		// getTask
		// getMicroTask
		// retrieveTasks
		// postAction
		// postTagAction
		// postUser
		// saveTag
		// postSegmentation
		// invalidateTag
		// closeTask
		// closeUTask

	}

	private static void addImagesToCollection() throws CMSException {
		final String[] imgFash = { "197", "198" };
		for (final String id : imgFash) {

			postObjString("{\"image\":" + id + "}", "collection/5/image");
		}

	}

	public static int getSegmentationCount() throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("type", "segmentation");
		return getCount("action", params);
	}

	public static List<Action> getBestSegmentation(final Integer imageId,
			final Integer tagId) throws CMSException {

		final HashMap<String, String> params = new HashMap();
		params.put("type", "segmentation");
		params.put("image", String.valueOf(imageId));
		params.put("tag", String.valueOf(tagId));
		params.put("completed", "true");
		params.put("validity", "true");

		// final Action a = getAction(37361);
		// final List<Action> ss = new ArrayList<>();
		// ss.add(a);
		// return ss;

		return getObjs(utils.CMS.models.Action.class, "action", params,
				"actions");

	}

	public static void closeAction(final Integer actionId) throws CMSException {
		postObj2(null, "action/" + actionId);
		LoggerUtils.debug("CMS", "Closing action " + actionId);

	}

	public static float getSegmentatioAvgDuration() throws CMSException {


		final List<Action> as = getAllSegmentations();

		Integer tot = 0;
		Double durations = (double) 0;
		Action a;
		for (final Action amin : as) {
			Logger.info("Downloading action: " + amin.getId());
			a = getAction(amin.getId());
			if (a.getSegmentation().getHistory() != null
					&& a.getSegmentation().getHistory().size() > 0) {
				final int start = a.getSegmentation().getHistory().get(0).getTime();
				final int end = a.getSegmentation().getHistory()
						.get(a.getSegmentation().getHistory().size() - 1).getTime();
				durations += (end - start);
				tot++;
			}
		}
		return (float) (durations / tot);

	}



	private static List<Action> getAllSegmentations() throws CMSException {
		Logger.debug("Downloading segs");
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("completed", "true");
		params.put("type", "segmentation");

		final List<utils.CMS.models.Action> as = CMS.getAllObjs(Action.class,
				"action", params, "actions");
		Logger.debug("Downloading segs end");
		return as;
	}

	public static Integer getSessionCount() throws CMSException {
		final Integer count = getCount("session");
		return count;
	}

	public static float getSessionAvgDuration() throws CMSException {

		Integer tot = 0;
		Double durations = (double) 0;
		final List<Session> actions = getAllClosedSessions();
		for (final Session a : actions) {
			final String completed = a.getCompleted_at();


			final String started = a.getCreated_at();
			final Calendar date = javax.xml.bind.DatatypeConverter
					.parseDateTime(started);
			final Calendar end = javax.xml.bind.DatatypeConverter
					.parseDateTime(completed);
			durations += end.getTimeInMillis() - date.getTimeInMillis();
			tot++;
		}
		return (float) (durations / tot);
	}

	private static List<Session> getAllClosedSessions() throws CMSException {
		Logger.debug("Downloading sessions");
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("completed", "true");

		final List<utils.CMS.models.Session> as = CMS.getAllObjs(Session.class,
				"session", params, "sessions");
		Logger.debug("Downloading sessions end");
		return as;
	}

	public static float getUserQualityAvg() throws CMSException, JSONException {
		final List<utils.CMS.models.User> as = getAllUsers();
		Integer tot = 0;
		Double durations = (double) 0;
		for (final User u : as) {
			if (u.getQuality() != null) {
				durations += u.getQuality();
				tot++;
			}
		}
		return (float) (durations / tot);
	}

}
