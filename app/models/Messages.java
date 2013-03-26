package models;

import org.codehaus.jackson.JsonNode;
import play.mvc.WebSocket;


public class Messages {
    
    public static class Join {
        
        final public String username;
        final public WebSocket.Out<JsonNode> channel;
        
        public Join(String username, WebSocket.Out<JsonNode> channel) {
            this.username = username;
            this.channel = channel;
        }
    }
    
    public static class Talk {
        
        final public String username;
        final public String text;
        
        public Talk(String username, String text) {
            this.username = username;
            this.text = text;
        }
        
    }
    
    public static class Quit {
        
        final public String username;
        
        public Quit(String username) {
            this.username = username;
        }
        
    }
    
    public static class Room{
        final public String room;

        public Room(String room) {
            this.room = room;
        }

        public String getRoom() {
            return room;
        }
    }
}
