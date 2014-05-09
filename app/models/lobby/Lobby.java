package models.lobby;

import play.libs.F.*;
import play.i18n.Messages;
import play.mvc.*;
import akka.actor.*;
import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.node.ObjectNode;

import utils.gamebus.GameMessages.*;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamemanager.GameManager;

public class Lobby extends UntypedActor {

    Room roomChannel;
    String currentGuess;
    Boolean gameStarted = false;
    Boolean askTag = false;
    String askTagSketcher;
    // Members of this room.
    ConcurrentHashMap<String, WebSocket.Out<JsonNode>> playersMap = new ConcurrentHashMap<>();

    @Override
    public void onReceive(Object message) {
      try {
        if (message instanceof Room) {
            this.roomChannel = ((Room) message);
            LoggerUtils.info("LOBBY", roomChannel.getRoom() + " created.");
        }
        if (message instanceof Join) {
            handleJoin((Join) message);
        }
        if (message instanceof GameEvent) {
            JsonNode event = ((GameEvent) message).getJson();
            event = event.get("message");
            String type = event.get("type").asText();
            switch (type) {
                case "updateList":
                    updateList(event.get("content"));
                    break;
                case "leave":
                    handleQuitter(event);
                    break;
            }
        }
        if (message instanceof ObjectNode) {
            JsonNode event=((JsonNode)message);
            GameBus.getInstance().publish(new GameMessages.GameEvent(event, roomChannel));
            event = event.get("message");
            String type = event.get("type").asText();
            switch(type) {
                  case "leave":handleQuitter(event);break;
            }
        }      
      }
      catch(Exception e) {
          LoggerUtils.error("[LOBBY]", e);
      }
    }

    private void updateList(JsonNode object) {
        if (object != null) {
            object = GameMessages.composeGameListUpdate(object);
            for (WebSocket.Out<JsonNode> channel : playersMap.values()) {
                channel.write(object);
            }
        }
    }

    private void handleJoin(Join message) {
        try {
            // Check if this username is free.
            if (playersMap.containsKey(message.getUsername())) {
                getSender().tell(Messages.get(LanguagePicker.retrieveLocale(), "usernameused"), this.getSelf());
            } else {
                playersMap.put(message.getUsername(), message.getChannel());
                getSender().tell("OK", this.getSelf());
                LoggerUtils.info("LOBBY", "added player " + message.getUsername());
                GameManager.getInstance().getCurrentGames();
            }
        }
        catch(Exception e) {
          LoggerUtils.error("[LOBBY]", e);
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
                    LoggerUtils.debug("LOBBY", quitter + " has disconnected.");
                }
            }
        }
        catch(Exception e) {
          LoggerUtils.error("[LOBBY]", e);
        }
    }
}
