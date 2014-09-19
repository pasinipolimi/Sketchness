package utils.gamebus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import models.Painter;
import play.libs.Json;
import play.mvc.WebSocket;

public class GameMessages {

	public static class GameEvent {

		protected Room channel;
		protected GameEventType type;
		protected String message;
		protected ObjectNode object;
		protected String username;
		protected JsonNode json;

		private GameEvent(final String message, final Room channel) {
			this.channel = channel;
			this.type = GameEventType.unknown;
			this.message = message;
		}

		public GameEvent(final JsonNode message, final Room channel) {
			this.channel = channel;
			this.type = GameEventType.unknown;
			this.json = message;
		}

		public GameEvent(final JsonNode message) {
			this.channel = null;
			this.type = GameEventType.unknown;
			this.json = message;
		}

		public GameEvent(final Room channel, final GameEventType type) {
			this.channel = channel;
			this.type = type;
		}

		public GameEvent(final GameEventType type) {
			this.type = type;
		}

		public GameEvent(final String message, final Room channel,
				final GameEventType type) {
			this.channel = channel;
			this.type = type;
			this.message = message;
		}

		public GameEvent(final String message, final String username,
				final GameEventType type) {
			this.message = message;
			this.username = username;
			this.type = type;
		}

		public GameEvent(final String message, final String username,
				final Room channel, final GameEventType type) {
			this(message, channel);
			this.username = username;
			this.type = type;
		}

		public GameEventType getType() {
			return type;
		}

		public void setObject(final ObjectNode object) {
			this.object = object;
		}

		public ObjectNode getObject() {
			return object;
		}

		public Room getChannel() {
			return channel;
		}

		public String getMessage() {
			return message;
		}

		public String getUsername() {
			return username;
		}

		public JsonNode getJson() {
			return json;
		}

	}

	public static ObjectNode composeJoin(ConcurrentHashMap<String, WebSocket.Out<JsonNode>> playersMap) {
		final ObjectNode event = Json.newObject();
		final ObjectNode structure = Json.newObject();
		structure.put("type", "join");
                final ArrayNode users = new ArrayNode(JsonNodeFactory.instance);
                for (Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap.entrySet()) {
                    String username = entry.getKey();
                    ObjectNode joined = Json.newObject();
                    joined.put("user", username);
                    joined.put("name", username);
                    joined.put("img", "images/UI/femaleAvatar.png");
                    users.add(joined);
                }
		structure.put("content", users);
		event.put("message", structure);
		return event;
	}

	public static ObjectNode composeTaskAcquired() {
		final ObjectNode content = Json.newObject();
		final ObjectNode event = composeJsonMessage("taskAcquired", content);
		return event;
	}

	public static ObjectNode composeMatchInfo() {
		final ObjectNode content = Json.newObject();
		final ObjectNode event = composeJsonMessage("matchInfo", content);
		return event;
	}

	public static ObjectNode composeChatMessage(final String user,
			final String text) {
		final ObjectNode content = Json.newObject();
		content.put("user", user);
		content.put("message", text);
		final ObjectNode event = composeJsonMessage("chat", content);
		return event;
	}

	public static ObjectNode composeMatchStart(final String id,
			final String roomName, final Integer currentPlayers,
			final Integer maxPlayers, final Boolean visible) {
		final ObjectNode content = Json.newObject();
		content.put("id", id);
		content.put("roomName", roomName);
		content.put("currentPlayers", currentPlayers);
		content.put("maxPlayers", maxPlayers);
		content.put("visible", visible);
		final ObjectNode event = composeJsonMessage("matchStart", content);
		return event;
	}

	public static ObjectNode composeMatchEnd(final String id,
			final String roomName, final Integer currentPlayers,
			final Integer maxPlayers, final Boolean visible) {
		final ObjectNode content = Json.newObject();
		content.put("id", id);
		content.put("roomName", roomName);
		content.put("currentPlayers", currentPlayers);
		content.put("maxPlayers", maxPlayers);
		content.put("visible", visible);
		final ObjectNode event = composeJsonMessage("matchEnd", content);
		return event;
	}

	public static ObjectNode composeMatchEnd() {
		final ObjectNode content = Json.newObject();
		final ObjectNode event = composeJsonMessage("matchEnd", content);
		return event;
	}

	public static ObjectNode composeNoTag() {
		final ObjectNode content = Json.newObject();
		final ObjectNode event = composeJsonMessage("noTag", content);
		return event;
	}

	public static ObjectNode composeMatchIfo(final String id,
			final String roomName, final Integer currentPlayers,
			final Integer maxPlayers, final Boolean visible) {
		final ObjectNode content = Json.newObject();
		content.put("id", id);
		content.put("roomName", roomName);
		content.put("currentPlayers", currentPlayers);
		content.put("maxPlayers", maxPlayers);
		content.put("visible", visible);
		final ObjectNode event = composeJsonMessage("matchInfo", content);
		return event;
	}

	public static ObjectNode composeGameListUpdate(final JsonNode game) {
		final ObjectNode event = composeJsonMessage("updateList",
				(ObjectNode) game);
		return event;
	}

	public static ObjectNode composeLogMessage(final LogLevel level,
			final String message) {
		final ObjectNode content = Json.newObject();
		content.put("level", level.name());
		content.put("message", message);
		final ObjectNode event = composeJsonMessage("log", content);
		return event;
	}

	public static ObjectNode composeJsonMessage(final String kind,
			final ObjectNode content) {
		final ObjectNode event = Json.newObject();
		final ObjectNode structure = Json.newObject();
		structure.put("type", kind);
		structure.put("content", content);
		event.put("message", structure);
		return event;
	}
        
        public static ObjectNode composeWaiting() {
		final ObjectNode event = composeJsonMessage("waiting", Json.newObject());
		return event;
	}

	public static ObjectNode composeLoading() {
		final ObjectNode event = composeJsonMessage("loading", Json.newObject());
		return event;
	}

	public static ObjectNode composeRoundBegin(final String sketcher) {
		final ObjectNode content = Json.newObject();
		content.put("sketcher", sketcher);
		final ObjectNode event = composeJsonMessage("roundBegin", content);
		return event;
	}

	public static ObjectNode composeRoundEnd(final String word) {
		final ObjectNode content = Json.newObject();
		content.put("word", word);
		final ObjectNode event = composeJsonMessage("roundEnd", content);
		return event;
	}

	public static ObjectNode composeRoundEnd(final String word,
			final String id, final String url, final Integer width,
			final Integer height) {
		final ObjectNode content = Json.newObject();
		content.put("word", word);
		content.put("id", id);
		content.put("url", url);
		content.put("width", width);
		content.put("height", height);
		final ObjectNode event = composeJsonMessage("roundEnd", content);
		return event;
	}

	public static ObjectNode composeImageInfo(final String id,
			final String url, final Integer width, final Integer height) {
		final ObjectNode content = Json.newObject();
		content.put("id", id);
		content.put("url", url);
		content.put("width", width);
		content.put("height", height);
		final ObjectNode event = composeJsonMessage("image", content);
		return event;
	}

	public static ObjectNode composeImage(final String id, final String url,
			final Integer width, final Integer height) {
		final ObjectNode content = Json.newObject();
		content.put("id", id);
		content.put("url", url);
		content.put("width", width);
		content.put("height", height);
		final ObjectNode event = composeJsonMessage("image", content);
		return event;
	}

	public static ObjectNode composeImage(final String user, final String id,
			final String url, final Integer width, final Integer height) {
		final ObjectNode content = Json.newObject();
		content.put("user", user);
		content.put("id", id);
		content.put("url", url);
		content.put("width", width);
		content.put("height", height);
		final ObjectNode event = composeJsonMessage("image", content);
		return event;
	}

	public static ObjectNode composeImage(final String id, final String url,
			final Integer width, final Integer height, final Integer time) {
		final ObjectNode content = Json.newObject();
		content.put("id", id);
		content.put("url", url);
		content.put("width", width);
		content.put("height", height);
		content.put("time", time);
		final ObjectNode event = composeJsonMessage("image", content);
		return event;
	}

	public static ObjectNode composeTag(final String user, final String id,
			final String url, final Integer width, final Integer height) {
		final ObjectNode content = Json.newObject();
		content.put("sketcher", user);
		content.put("id", id);
		content.put("url", url);
		content.put("width", width);
		content.put("height", height);
		final ObjectNode event = composeJsonMessage("tagS", content);
		return event;
	}

	public static ObjectNode composeTag() {
		final ObjectNode event = composeJsonMessage("tag", Json.newObject());
		return event;
	}

	public static ObjectNode composeSkip() {
		final ObjectNode event = composeJsonMessage("skipTask",
				Json.newObject());
		return event;
	}

	public static ObjectNode composeTimeExpired(final String player) {
		final ObjectNode content = Json.newObject();
		content.put("player", player);
		final ObjectNode event = composeJsonMessage("timeExpired",
				Json.newObject());
		return event;
	}

	public static ObjectNode composeSaveTraces() {
		final ObjectNode event = composeJsonMessage("saveTraces",
				Json.newObject());
		return event;
	}

	public static ObjectNode composeEndSegmentation() {
		final ObjectNode event = composeJsonMessage("endSegmentationC",
				Json.newObject());
		return event;
	}

	public static ObjectNode composeTask(final String word, final String id, final Integer width, final Integer height, final String url) {
		final ObjectNode content = Json.newObject();
		content.put("word", word);
		content.put("id", id);
		content.put("word", word);
		content.put("url", url);
		content.put("width", width);
		content.put("height", height);
		final ObjectNode event = composeJsonMessage("task", content);
		return event;
	}

	public static ObjectNode composeTask(final String user, final String id,
			final String url, final String word, final Integer width,
			final Integer height) {
		final ObjectNode content = Json.newObject();
		content.put("sketcher", user);
		content.put("id", id);
		content.put("url", url);
		content.put("width", width);
		content.put("height", height);
		content.put("word", word);
		final ObjectNode event = composeJsonMessage("task", content);
		return event;
	}

	public static ObjectNode composeScore(final String user, final Integer score) {
		final ObjectNode content = Json.newObject();
		content.put("user", user);
		content.put("score", score);
		final ObjectNode event = composeJsonMessage("score", content);
		return event;
	}

	public static ObjectNode composeTimer(final Integer time) {
		final ObjectNode content = Json.newObject();
		content.put("time", time);
		final ObjectNode event = composeJsonMessage("timerS", content);
		return event;
	}

	public static ObjectNode composeTimerForClient(final Integer time) {
		final ObjectNode content = Json.newObject();
		content.put("time", time);
		final ObjectNode event = composeJsonMessage("timer", content);
		return event;
	}

	public static ObjectNode composeGuess(final String user, final String word,
			final String affinity) {
		final ObjectNode content = Json.newObject();
		content.put("user", user);
		content.put("word", word);
		content.put("affinity", affinity);
		final ObjectNode event = composeJsonMessage("guess", content);
		return event;
	}

	public static ObjectNode composeGuessed(final String user, final String word) {
		final ObjectNode content = Json.newObject();
		content.put("user", user);
		content.put("word", word);
		final ObjectNode event = composeJsonMessage("guessed", content);
		return event;
	}

	public static ObjectNode composeChangeTool(final String tool,
			final Integer size, final String color) {
		final ObjectNode content = Json.newObject();
		content.put("tool", tool);
		content.put("size", size);
		content.put("color", color);
		final ObjectNode event = composeJsonMessage("changeTool", content);
		return event;
	}

	public static ObjectNode composePoint(final Integer x, final Integer y) {
		final ObjectNode content = Json.newObject();
		content.put("x", x);
		content.put("y", y);
		final ObjectNode event = composeJsonMessage("point", content);
		return event;
	}

	public static ObjectNode composeBeginPath() {
		final ObjectNode event = composeJsonMessage("beginPath",
				Json.newObject());
		return event;
	}

	public static ObjectNode composeEndPath() {
		final ObjectNode event = composeJsonMessage("endPath", Json.newObject());
		return event;
	}

	public static ObjectNode composeLeaderboard(final ObjectNode object) {
		final ObjectNode event = composeJsonMessage("leaderboard", object);
		return event;
	}
        
        public static ObjectNode composeHandleError(final ObjectNode object) {
		final ObjectNode event = composeJsonMessage("error", object);
		return event;
	}

	public static ObjectNode composeFinalTraces(final String url,
			final String label, final ArrayNode traces,
			final ObjectNode history, final String taskId) {
		final ObjectNode content = Json.newObject();
		content.put("id", taskId);
		content.put("url", url);
		content.put("label", label);
		content.put("traces", traces);
		content.put("history", history);
		final ObjectNode event = composeJsonMessage("finalTraces", content);
		return event;
	}

	public static ObjectNode composeQuit(final String username) {
		final ObjectNode content = Json.newObject();
		content.put("user", username);
		final ObjectNode event = composeJsonMessage("leave", content);
		return event;
	}

	public static class SystemMessage extends GameEvent {

		public SystemMessage(final String message, final Room channel) {
			super(message, channel);
		}
	}

	public static class Join {

		final String username;
		final WebSocket.Out<JsonNode> channel;

		public Join(final String username, final WebSocket.Out<JsonNode> channel) {
			this.username = username;
			this.channel = channel;
		}

		public WebSocket.Out<JsonNode> getChannel() {
			return channel;
		}

		public String getUsername() {
			return username;
		}
	}

	public static class Room {

		final String room;
		final int requiredPlayers;

		public Room(final String room, final int requiredPlayers) {
			this.room = room;
			this.requiredPlayers = requiredPlayers;
		}

		public Room(final String room) {
			this.room = room;
			requiredPlayers = -1;
		}

		public String getRoom() {
			return room;
		}

		public int getRequiredPlayers() {
			return requiredPlayers;
		}
	}

	public static class GameInfo {

		String roomName;
		Integer nPlayers;
		Integer maxPlayers;

		public GameInfo(final String roomName, final Integer nPlayers,
				final Integer maxPlayers) {
			this.roomName = roomName;
			this.nPlayers = nPlayers;
			this.maxPlayers = maxPlayers;
		}

		public Integer getMaxPlayers() {
			return maxPlayers;
		}

		public String getRoomName() {
			return roomName;
		}

		public Integer getnPlayers() {
			return nPlayers;
		}
	}

	public enum LogLevel {
		debug, info, warning, error
	}
}
