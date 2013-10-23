package utils.gamebus;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
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

        private GameEvent(String message, Room channel) {
            this.channel = channel;
            this.type = GameEventType.unknown;
            this.message = message;
        }
        
        public GameEvent(JsonNode message, Room channel) {
            this.channel = channel;
            this.type = GameEventType.unknown;
            this.json = message;
        }

        public GameEvent(Room channel, GameEventType type) {
            this.channel = channel;
            this.type = type;
        }

        public GameEvent(GameEventType type) {
            this.type = type;
        }

        public GameEvent(String message, Room channel, GameEventType type) {
            this.channel = channel;
            this.type = type;
            this.message = message;
        }

        public GameEvent(String message, String username, GameEventType type) {
            this.message = message;
            this.username = username;
            this.type = type;
        }

        public GameEvent(String message, String username, Room channel, GameEventType type) {
            this(message, channel);
            this.username = username;
            this.type = type;
        }

        public GameEventType getType() {
            return type;
        }

        public void setObject(ObjectNode object) {
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

    public static ObjectNode composeJoin(String username) {
        ObjectNode event = Json.newObject();
        ObjectNode structure = Json.newObject();
        structure.put("type", "join");
        ObjectNode content = Json.newObject();
        content.put("user", username);
        content.put("name", username);
        content.put("img", "images/UI/femaleAvatar.png");
        structure.put("content",content);
        event.put("message", structure);
        return event;
    }
    
    public static ObjectNode composeChatMessage(String user, String text) {
            ObjectNode content = Json.newObject();
            content.put("user", user);
            content.put("message", text);
            ObjectNode event = composeJsonMessage("chat",content);
            return event;
    }
    
    public static ObjectNode composeLogMessage(String level, String message) {
            ObjectNode content = Json.newObject();
            content.put("level", level);
            content.put("message", message);
            ObjectNode event = composeJsonMessage("log",content);
            return event;
    }
    
    
    public static ObjectNode composeJsonMessage(String kind, ObjectNode content) {
        ObjectNode event = Json.newObject();
        ObjectNode structure = Json.newObject();
        structure.put("type", kind);
        structure.put("content",content);
        event.put("message", structure);
        return event;
    }
    
    public static ObjectNode composeLoading() {
        ObjectNode event = composeJsonMessage("loading",Json.newObject());
        return event;
    }
    
    public static ObjectNode composeRoundBegin(String sketcher) {
          ObjectNode content = Json.newObject();
          content.put("sketcher", sketcher);
          ObjectNode event = composeJsonMessage("roundBegin",content);
          return event;
    }
    
    public static ObjectNode composeRoundEnd(String word) {
          ObjectNode content = Json.newObject();
          content.put("word", word);
          ObjectNode event = composeJsonMessage("roundEnd",content);
          return event;
    }
    
    public static ObjectNode composeImageInfo(String id,String url,Integer width, Integer height) {
          ObjectNode content = Json.newObject();
          content.put("id", id);
          content.put("url", url);
          content.put("width", width);
          content.put("height", height);
          ObjectNode event = composeJsonMessage("image",content);
          return event;
    }
    
    public static ObjectNode composeTag() {
         ObjectNode event = composeJsonMessage("tag",Json.newObject());
         return event;
    }
    
    public static ObjectNode composeTask(String word) {
        ObjectNode content = Json.newObject();
        content.put("word", word);
        ObjectNode event = composeJsonMessage("task",content);
        return event;
    }
    
    public static ObjectNode composeScore(String user, Integer score) {
        ObjectNode content = Json.newObject();
        content.put("user", user);
        content.put("score", score);
        ObjectNode event = composeJsonMessage("score",content);
        return event;
    }
    
    
     public static ObjectNode composeTimer(Integer time) {
        ObjectNode content = Json.newObject();
        content.put("time", time);
        ObjectNode event = composeJsonMessage("timer",content);
        return event;
    }
     
     public static ObjectNode composeGuess(String user, String word, String affinity) {
        ObjectNode content = Json.newObject();
        content.put("user", user);
        content.put("word", word);
        content.put("affinity", affinity);
        ObjectNode event = composeJsonMessage("guess",content);
        return event;
    }
     
    public static ObjectNode composeGuessed(String word) {
        ObjectNode content = Json.newObject();
        content.put("word", word);
        ObjectNode event = composeJsonMessage("guessed",content);
        return event;
    }
    
    public static JsonNode composeQuit(final String username) {
        ObjectNode content = Json.newObject();
        content.put("user", username);
        ObjectNode event = composeJsonMessage("leave",content);
        return event;
    }
     
    public static class SystemMessage extends GameEvent {

        public SystemMessage(String message, Room channel) {
            super(message, channel);
        }
    }
    
    

    public static class Join {

        final String username;
        final WebSocket.Out<JsonNode> channel;

        public Join(String username, WebSocket.Out<JsonNode> channel) {
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

        public Room(String room, int requiredPlayers) {
            this.room = room;
            this.requiredPlayers = requiredPlayers;
        }

        public Room(String room) {
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

        public GameInfo(String roomName, Integer nPlayers, Integer maxPlayers) {
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
}
