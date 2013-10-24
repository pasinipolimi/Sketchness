package models.chat;

import play.libs.F.*;
import play.i18n.Messages;
import play.mvc.*;
import play.Logger;

import org.codehaus.jackson.node.*;
import org.codehaus.jackson.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import models.factory.GameRoom;

import utils.gamebus.GameMessages.*;
import utils.LanguagePicker;
import utils.gamebus.GameMessages;

public class Chat extends GameRoom {

    Room roomChannel;
    // Members of this room.
    ConcurrentHashMap<String, WebSocket.Out<JsonNode>> playersMap = new ConcurrentHashMap<>();

    public Chat() {
        super(Chat.class);
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof Room) {
            this.roomChannel = ((Room) message);
            Logger.info("[CHAT] " + roomChannel.getRoom() + " created.");
        }
        if (message instanceof SystemMessage) {
            //notifyAll(ChatKind.system, "Sketchness", ((SystemMessage) message).getMessage());
        }
        if (message instanceof Join) {
            handleJoin((Join) message);
        } else if (message instanceof GameEvent) {
            JsonNode event = ((GameEvent) message).getJson();
            if(event!=null) {
                event = event.get("message");
                String type = event.get("type").asText();
                switch(type) {
                      case "chat":notifyAll(event.get("content").get("user").asText(),event.get("content").get("message").asText());break;
                      case "leave":handleQuitter(event.get("content").get("user").asText());break;
                 }   
            }
        } 
        
        else {
            unhandled(message);
        }
    }

   

    private void handleJoin(Join message) {
        // Check if this username is free.
        if (playersMap.containsKey(message.getUsername())) {
            getSender().tell(Messages.get(LanguagePicker.retrieveLocale(), "usernameused"), this.getSelf());
        } else {
            playersMap.put(message.getUsername(), message.getChannel());
            ObjectNode event = GameMessages.composeJoin(message.getUsername());
            getSender().tell("OK", this.getSelf());
            notifySystem(LogLevel.info,message.getUsername()+" "+Messages.get(LanguagePicker.retrieveLocale(), "join"));
            notifyMemberChange(event);
            notifyMemberList(event);
            Logger.debug("[CHAT] added player " + message.getUsername());
        }
    }
    
    

    

    private void handleQuitter(String quitter) throws InterruptedException {
        for (Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(quitter)) {
                //Close the websocket
                entry.getValue().close();
                playersMap.remove(quitter);
                JsonNode event = GameMessages.composeQuit(quitter);
                notifySystem(LogLevel.info,quitter+" "+Messages.get(LanguagePicker.retrieveLocale(), "quit"));
                notifyMemberChange(event);
                Logger.debug("[CHAT] " + quitter + " has disconnected.");
            }
        }
    }

    
    
    private void notifySystem(LogLevel level, String text) {
        for (WebSocket.Out<JsonNode> channel : playersMap.values()) {         
            ObjectNode event = GameMessages.composeLogMessage(level,text);
            channel.write(event);
        }
    }

    // Send a Json event to all members
    private void notifyAll(String user, String text) {
        for (WebSocket.Out<JsonNode> channel : playersMap.values()) {         
            ObjectNode event = GameMessages.composeChatMessage(user,text);
            channel.write(event);
        }
    }

    // Send the updated list of members to the users
    private void notifyMemberChange(JsonNode event) {
        for (WebSocket.Out<JsonNode> channel : playersMap.values()) {
            channel.write(event);
        }
    }
    
    private void notifyMemberList(JsonNode event) {
         
         event = event.get("message");
         String username = event.get("content").get("user").asText();
         WebSocket.Out<JsonNode> channel = playersMap.get(username);
         for (Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap.entrySet()) {
            JsonNode user = GameMessages.composeJoin(entry.getKey());
            channel.write(user);
        }
    }
}



