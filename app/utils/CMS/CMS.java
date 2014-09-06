package utils.CMS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
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
import utils.CMS.models.Tag;
import utils.CMS.models.Task;
import utils.CMS.models.User;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.Room;
import akka.actor.Cancellable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CMS {

	// private final static String rootUrl = "http://80.240.141.191:80";

	private static String[] invalid = { "20254", "20252", "20225", "20166",
			"20096", "20064", "20015", "19666", "19664", "19648", "19647",
			"19588", "19587", "19538", "19537", "19530", "19529", "19506",
			"19505", "19396", "19395", "19380", "19379", "19368", "19367",
			"19292", "19291", "19254", "19253", "19242", "19241", "19216",
			"19215", "19210", "19209", "19136", "19135", "19096", "19095",
			"19016", "19015", "18998", "18997", "18954", "18953", "18872",
			"18871", "18774", "18773", "18756", "18755", "18592", "18591",
			"18558", "18557", "18544", "18543", "18448", "18447", "18410",
			"18409", "18392", "18391", "18370", "18369", "18360", "18359",
			"18296", "18295", "18260", "18259", "18198", "18197", "18082",
			"18081", "18026", "18025", "17958", "17957", "17928", "17927",
			"17916", "17915", "17908", "17907", "17890", "17889", "17796",
			"17795", "17784", "17783", "17776", "17775", "17756", "17755",
			"17744", "17743", "17732", "17731", "17664", "17663", "17654",
			"17653", "17648", "17647", "17634", "17633", "17622", "17621",
			"17556", "17555", "17528", "17527", "17476", "17475", "17462",
			"17461", "17456", "17455", "17446", "17445", "17432", "17431",
			"17424", "17423", "17404", "17403", "17370", "17369", "17352",
			"17351", "17178", "17177", "17138", "17137", "17102", "17101",
			"17028", "17027", "17006", "17005", "16984", "16983", "16952",
			"16951", "16928", "16927", "16914", "16913", "16880", "16879",
			"16748", "16747", "16726", "16725", "16714", "16713", "16700",
			"16699", "16644", "16643", "16568", "16567", "16428", "16427",
			"16326", "16325", "16254", "16253", "16228", "16227", "16204",
			"16203", "16190", "16189", "16128", "16127", "16118", "16117",
			"16106", "16105", "16098", "16097", "16048", "16047", "16038",
			"16037", "16018", "16017", "15994", "15993", "15988", "15987",
			"15952", "15951", "15902", "15901", "15874", "15873", "15846",
			"15845", "15832", "15831", "15788", "15787", "15746", "15745",
			"15738", "15737", "15728", "15727", "15708", "15707", "15662",
			"15661", "15654", "15653", "15644", "15643", "15602", "15601",
			"15580", "15579", "15542", "15541", "15520", "15519", "15480",
			"15479", "15352", "15351", "15326", "15325", "15302", "15301",
			"15272", "15271", "15262", "15261", "15238", "15237", "15222",
			"15221", "15062", "15061", "15048", "15047", "15010", "15009",
			"14880", "14879", "14846", "14845", "14832", "14831", "14804",
			"14803", "14780", "14779", "14682", "14681", "14658", "14657",
			"14650", "14649", "14592", "14591", "14556", "14555", "14502",
			"14501", "14492", "14491", "14448", "14447", "14438", "14437",
			"14426", "14425", "14414", "14413", "14392", "14391", "14332",
			"14331", "14320", "14319", "14308", "14307", "14296", "14295",
			"14288", "14287", "14278", "14277", "14264", "14263", "14248",
			"14247", "14236", "14235", "14166", "14165", "13892", "13891",
			"13830", "13829", "13768", "13767", "13730", "13729", "13720",
			"13719", "13712", "13711", "13704", "13703", "13668", "13667",
			"13660", "13659", "13636", "13635", "13628", "13627", "13618",
			"13617", "13608", "13607", "13584", "13583", "13548", "13547",
			"13516", "13515", "13430", "13429", "13416", "13415", "13376",
			"13375", "13364", "13363", "13352", "13351", "13316", "13315",
			"13304", "13303", "13248", "13247", "13234", "13233", "13222",
			"13221", "13200", "13199", "13180", "13179", "13152", "13151",
			"13072", "13071", "12884", "12883", "12872", "12871", "12814",
			"12813", "12776", "12775", "12760", "12759", "12446", "12428",
			"12369", "12362", "12360", "12290", "12139", "12050", "12039",
			"11745", "11633", "11627", "11465", "11409", "11235", "11179",
			"10977", "10945", "10825", "10769", "10639", "10491", "10421",
			"10305", "10305", "10223", "10131", "10023", "9927", "9867",
			"9723", "9645", "9557", "9431", "9405", "9217", "9163", "8967",
			"8959", "8831", "8681", "8587", "8501", "8321", "8283", "8201",
			"8061", "7953", "7893", "7813", "7751", "7633", "7473", "7469",
			"7401", "7211", "7191", "7075", "6919", "6917", "6747", "6643",
			"6529", "6491", "6297", "6275", "6111", "6049", "5965", "5963",
			"5731", "5667", "5591", "5469", "5389", "5257", "5159", "5135",
			"4995", "4831", "4801", "4611", "4583", "4447", "4431", "4359",
			"4217", "4193", "4089", "4049", "4025", "3922", "3882", "3726",
			"3604", "3578", "3486", "3292", "3214", "3096", "3058", "2971",
			"2931", "2921", "2917", "2907", "2839", "2745", "2649", "2569",
			"2473", "2391", "2237", "2235", "2193", "2187", "2091", "1951",
			"1929", "1789", "1699", "1685", "1515", "1483", "1483", "1465",
			"1091", "979", "897", "416", "57", "51", "49", "12662", "12614",
			"12560", "12558", "12327", "1547", "917", "67", "19695", "12658",
			"12263", "12211", "12119", "11922", "11659", "11363", "11115",
			"11013", "10809", "10545", "10341", "10253", "10067", "9751",
			"9499", "9279", "9133", "8881", "8753", "8641", "8251", "8127",
			"7907", "7809", "7443", "7307", "7167", "6853", "6667", "6451",
			"6359", "6179", "5859", "5797", "5577", "5485", "5229", "5105",
			"4967", "4799", "4591", "4397", "4153", "4015", "3864", "3664",
			"3416", "3236", "2990", "2573", "2505", "2279", "2195", "2189",
			"2057", "1259", "1149", "177", "11920", "11645", "11467", "11257",
			"10917", "10751", "10553", "10321", "10189", "9893", "9869",
			"9631", "9385", "9095", "9029", "8839", "8607", "8401", "8113",
			"7875", "7747", "7629", "7479", "7361", "7083", "6857", "6681",
			"6537", "6353", "6117", "5981", "5605", "5537", "5207", "5097",
			"4911", "4575", "4455", "4223", "3974", "3748", "3558", "3192",
			"3116", "2507", "2301", "2265", "2157", "2131", "1961", "1797",
			"1665", "1557", "1023", "315", "19931", "17400", "17399", "11825",
			"11789", "11782", "12600", "1901", "1751", "1671", "1413", "1005",
			"271", "20223", "20180", "20174", "20080", "19975", "19848",
			"19834", "19832", "19826", "19824", "19816", "19812", "19802",
			"19798", "19408", "19407", "19394", "19393", "19346", "19345",
			"19290", "19289", "19240", "19239", "19168", "19167", "19080",
			"19079", "18940", "18939", "18870", "18869", "18822", "18821",
			"18754", "18753", "18710", "18709", "18600", "18599", "18284",
			"18283", "18272", "18271", "18210", "18209", "18072", "18071",
			"18034", "18033", "18000", "17999", "17946", "17945", "17872",
			"17871", "17860", "17859", "17832", "17831", "17824", "17823",
			"17720", "17719", "17644", "17643", "17548", "17547", "17516",
			"17515", "17412", "17411", "17376", "17375", "17340", "17339",
			"17260", "17259", "17248", "17247", "17226", "17225", "17134",
			"17133", "17100", "17099", "17062", "17061", "17050", "17049",
			"16938", "16937", "16912", "16911", "16900", "16899", "16876",
			"16875", "16848", "16847", "16847", "16824", "16823", "16800",
			"16799", "16688", "16687", "16674", "16673", "16642", "16641",
			"16542", "16541", "16414", "16413", "16180", "16179", "16088",
			"16087", "15986", "15985", "15974", "15973", "15890", "15889",
			"15418", "15417", "15394", "15393", "15374", "15373", "15292",
			"15291", "15044", "15043", "15032", "15031", "15006", "15005",
			"14992", "14991", "14976", "14975", "14952", "14951", "14936",
			"14935", "14922", "14921", "14908", "14907", "14876", "14875",
			"14864", "14863", "14830", "14829", "14792", "14791", "14744",
			"14743", "14726", "14725", "14656", "14655", "14628", "14627",
			"14462", "14461", "14404", "14403", "14390", "14389", "14382",
			"14381", "14330", "14329", "14294", "14293", "14072", "14071",
			"14032", "14031", "13828", "13827", "13710", "13709", "13440",
			"13439", "13414", "13413", "13336", "13335", "13268", "13267",
			"13220", "13219", "13178", "13177", "13114", "13114", "13113",
			"13092", "13091" };

	private final static String rootUrl = Play.application().configuration()
			.getString("cmsUrl");
	private final static Integer timeoutPostCMS = Play.application()
			.configuration().getInt("cmsTimeoutPost");

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

	private static <T extends CMSObject> List<T> getObjs(final Class<T> claz,
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
			final HashMap<String, Integer> openActionsSeg, final HashMap<String, Integer> openActionsTag) throws Exception {
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
			points.add(new utils.CMS.models.Point(trace.get("x").asInt(), trace
					.get("y").asInt(), trace.get("color").asText()
					.equals("end")));

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
			final Integer maxRound)
					throws Error, Exception {
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
			retrieveImages(tasksToAdd, queueImages, roomChannel,
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

		for (final String a : invalid) {
			invalidateAction(a);
		}

		// final List<Action> actions = getActions();
		// for (final Action a : actions) {
		// final String completed = a.getCompleted_at();
		// if (completed != null) {
		// continue;
		// }
		//
		// final String started = a.getCreated_at();
		// final Calendar date = javax.xml.bind.DatatypeConverter
		// .parseDateTime(started);
		// final Calendar now = Calendar.getInstance();
		// final long diff = now.getTimeInMillis() - date.getTimeInMillis();
		// if (diff > MAX_OPEN_ACTION) {
		// System.out.println("Closing action");
		// closeAction(a.getId());
		// }
		// }
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

			imgtgs = CMS.getChoose(collection, tasksToAdd.toString());

			LoggerUtils.debug("CMS", "Requested image list to CMS end");
		} catch (final Exception e) {
			throw new Exception(
					"[CMS] The request to the CMS is malformed", e);
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

	private static void openAction(final Integer image, final Integer session,
			final Integer tagId, final Integer userId) throws CMSException {
		final Action action = Action.createSegmentationAction(image, session,
				tagId, userId, true);
		postAction(action);
	}

	private static List<ChooseImageTag> getChoose(final Integer collection2,
			final String limit) throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("limit", limit);
		params.put("collection", String.valueOf(collection2));
		return getObjs(ChooseImageTag.class, "choose/imageandtag/" + policy, params, "results");
	}

	private static List<ChooseImage> getChooseImageOnly(final Integer collection2,
			final String limit) throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("limit", limit);
		params.put("collection", String.valueOf(collection2));
		return getObjs(ChooseImage.class, "choose/image/" + policy, params, "results");
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
		if(tagId>=0)
			return getObj(Tag.class, "tag", tagId, "tag");
		else
			//The image has no tag associated to it
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

	public static List<User> getUsers() throws CMSException {
		return getObjs(User.class, "user", "user");
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
		params.put("type", "tag");
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
		return masks.get(0);
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
		final utils.CMS.models.Tag tag = postObj(
				new utils.CMS.models.Tag(name), "tag",
				utils.CMS.models.Tag.class);
		return tag.getId();
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

	public static void test() throws CMSException {

		// OK
		final Tag ims = CMS.getTag(0);
		System.out.println("ciao" + ims.getId());

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

		final Integer s = CMS.postUser("pippo");

		// System.out.println("ciao" + sd);
		System.out.println("ciao" + s);
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

	public static int getSegmentationCount() throws CMSException {
		final HashMap<String, String> params = new HashMap<>();
		params.put("type", "segmentation");
		return getCount("action", params);
	}

	public static void closeAction(final Integer actionId) throws CMSException {
		postObj2(null, "action/" + actionId);
		LoggerUtils.debug("CMS", "Closing action " + actionId);

	}

}
