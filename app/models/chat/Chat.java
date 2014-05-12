package models.chat;

import play.libs.F.*;
import play.i18n.Messages;
import play.mvc.*;
import play.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import models.factory.GameRoom;

import utils.gamebus.GameMessages.*;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;

public class Chat extends GameRoom {

    Room roomChannel;
    // Members of this room.
    ConcurrentHashMap<String, WebSocket.Out<JsonNode>> playersMap = new ConcurrentHashMap<>();

    public Chat() {
        super(Chat.class);
    }

    @Override
    public void onReceive(Object message) {
        try {
            if (message instanceof Room) {
                this.roomChannel = ((Room) message);
                LoggerUtils.info("CHAT", roomChannel.getRoom() + " created.");
            }
            if (message instanceof Join) {
                handleJoin((Join) message);
            } else if (message instanceof GameEvent) {
                JsonNode event = ((GameEvent) message).getJson();
                if(event!=null) {
                    event = event.get("message");
                    String type = event.get("type").asText();
                    switch(type) {
                          case "chat":notifyAll(event);break;
                          case "log":notifySystem(event);break;
                          case "leave":handleQuitter(event);break;
                          case "matchEnd": killActor();break;
                     }   
                }
            }
            else if (message instanceof ObjectNode) {
                JsonNode event=((JsonNode)message);
                GameBus.getInstance().publish(new GameMessages.GameEvent(event, roomChannel));
                event = event.get("message");
                String type = event.get("type").asText();
                switch(type) {
                      case "leave": handleQuitter(event);break;
                }
            }      

            else {
                unhandled(message);
            }
        }
        catch(Exception e) {
          LoggerUtils.error("[CHAT]", e);
        }
    }

   

    private void handleJoin(Join message) {
        // Check if this username is free.
        try {
            if (playersMap.containsKey(message.getUsername())) {
                getSender().tell(Messages.get(LanguagePicker.retrieveLocale(), "usernameused"), this.getSelf());
            } else {
                playersMap.put(message.getUsername(), message.getChannel());
                ObjectNode event = GameMessages.composeJoin(playersMap);
                getSender().tell("OK", this.getSelf());
                notifySystem(LogLevel.info,message.getUsername()+" "+Messages.get(LanguagePicker.retrieveLocale(), "join"));
                notifyMemberChange(event);
                LoggerUtils.info("CHAT","Added player " + message.getUsername());
            }
        }
        catch(Exception e) {
            LoggerUtils.error("Impossible to handle join",e);
        }
    }
    
    

    

    private void handleQuitter(JsonNode jquitter) throws InterruptedException {
        try {
            String quitter = jquitter.get("content").get("user").asText();
            for (Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(quitter)) {
                    //Close the websocket
                    entry.getValue().close();
                    playersMap.remove(quitter);
                    JsonNode event = GameMessages.composeQuit(quitter);
                    notifySystem(LogLevel.info,quitter+" "+Messages.get(LanguagePicker.retrieveLocale(), "quit"));
                    notifyMemberChange(event);
                    LoggerUtils.info("CHAT",quitter + " has disconnected.");
                }
            }
        }
        catch(Exception e) {
            LoggerUtils.error("Impossible to handle quitter",e);
        }
    }

    
    
    private void notifySystem(LogLevel level, String text) {
        for (WebSocket.Out<JsonNode> channel : playersMap.values()) {         
            ObjectNode event = GameMessages.composeLogMessage(level,text);
            channel.write(event);
        }
    }
    
    private void notifySystem(JsonNode event) {
        LogLevel level = LogLevel.valueOf(event.get("content").get("level").asText());
        String text = event.get("content").get("message").asText();
        for (WebSocket.Out<JsonNode> channel : playersMap.values()) {         
            event = GameMessages.composeLogMessage(level,text);
            channel.write(event);
        }
    }

    // Send a Json event to all members
    private void notifyAll(JsonNode event) {
        String user = event.get("content").get("user").asText();
        String text = event.get("content").get("message").asText();
        for (WebSocket.Out<JsonNode> channel : playersMap.values()) {         
            event = GameMessages.composeChatMessage(user,text);
            channel.write(event);
        }
    }

    // Send the updated list of members to the users
    private void notifyMemberChange(JsonNode event) {
        for (WebSocket.Out<JsonNode> channel : playersMap.values()) {
            channel.write(event);
        }
    }
}



