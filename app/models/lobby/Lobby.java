package models.lobby;

import play.libs.F.*;
import play.i18n.Messages;
import play.mvc.*;
import akka.actor.*;
import play.Logger;
import org.codehaus.jackson.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.codehaus.jackson.node.ObjectNode;

import utils.gamebus.GameMessages.*;
import utils.LanguagePicker;
import utils.gamebus.GameBus;
import static utils.gamebus.GameEventType.quit;
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
    public void onReceive(Object message) throws Exception {
        if (message instanceof Room) {
            this.roomChannel = ((Room) message);
            Logger.info("[LOBBY] " + roomChannel.getRoom() + " created.");
        }
        if (message instanceof Join) {
            handleJoin((Join) message);
        }
        if (message instanceof GameEvent) {
            GameEvent event = (GameEvent) message;
            switch (event.getType()) {
                case gameInfo:
                    updateList(event.getObject());
                    break;
                case gameStarted:
                    handleStartEnd();
                    break;
                case gameEnded:
                    handleStartEnd();
                    break;
                case quit:
                    handleQuitter(event.getMessage());
                    break;
            }
        }
        if (message instanceof ObjectNode) {
            JsonNode event=((JsonNode)message);
            GameBus.getInstance().publish(new GameMessages.GameEvent(event, roomChannel));
            event = event.get("message");
            String type = event.get("type").asText();
            switch(type) {
                  case "leave":handleQuitter(event.get("content").get("user").asText());break;
            }
        }            
    }

    private void updateList(ObjectNode object) {
        if (object != null) {
            Logger.debug(object.toString());
            object.put("type", "updateList");
            for (WebSocket.Out<JsonNode> channel : playersMap.values()) {
                channel.write(object);
            }
        }
    }

    private void handleJoin(Join message) {
        // Check if this username is free.
        if (playersMap.containsKey(message.getUsername())) {
            getSender().tell(Messages.get(LanguagePicker.retrieveLocale(), "usernameused"), this.getSelf());
        } else {
            playersMap.put(message.getUsername(), message.getChannel());
            getSender().tell("OK", this.getSelf());
            Logger.debug("[LOBBY] added player " + message.getUsername());
            GameManager.getInstance().getCurrentGames();
        }
    }

    private void handleStartEnd() {
        GameManager.getInstance().getCurrentGames();
    }

    private void handleQuitter(String quitter) throws InterruptedException {
        for (Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(quitter)) {
                //Close the websocket
                entry.getValue().close();
                playersMap.remove(quitter);
                Logger.debug("[LOBBY] " + quitter + " has disconnected.");
            }
        }
    }
}
