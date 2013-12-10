package models.paint;

import java.awt.image.BufferedImage;
import java.net.URL;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;

import models.Painter;
import models.Point;
import models.Segment;
import models.factory.GameRoom;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages.GameEvent;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.json.JSONException;
import utils.LanguagePicker;
import utils.gamebus.GameEventType;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.Join;
import utils.gamebus.GameMessages.Room;

public class Paint extends GameRoom {

    Boolean gameStarted = false;
    Room roomChannel;
    BufferedImage taskImage;
    String taskUrl;
    int taskWidth;
    int taskHeight;
    String sketcher;
    String guessWord;
    private Segment currentSegment = new Segment("rgba(255,255,255,1.0)");
    // The list of all connected painters (identified by ids)
    private ConcurrentHashMap<String, Painter> painters = new ConcurrentHashMap<>();
    //Traces 
    JsonNodeFactory factory = JsonNodeFactory.instance;
    private ObjectNode traces = new ObjectNode(factory);

    public Paint() {
        super(Paint.class);
    }

    //Manage the messages
    @Override
    public void onReceive(Object message) throws Exception {
        //We are initializing the room 
        if (message instanceof Room) {
            this.roomChannel = ((Room) message);
            Logger.info("[PAINT] " + roomChannel.getRoom() + " created.");
        }
        if (message instanceof Join) {
            handleJoin((Join) message);
        }
        if (message instanceof JsonNode) {
            JsonNode event=((JsonNode)message);
            GameBus.getInstance().publish(new GameMessages.GameEvent(event, roomChannel));  //TODO ***
            event = event.get("message");
            String type = event.get("type").asText();
            switch(type) {
                case "change":
                    Painter painter = painters.get(event.get("content").get("name").getTextValue());
                    if (painter != null) {
                        painter.updateFromJson(event.get("content"));
                    }
                    break;
                case "roundEnd":
        //            GameBus.getInstance().publish(new GameEvent(json.get("player").getTextValue(), roomChannel, GameEventType.timeExpired));
                    notifyAll(event.get("content"));
                    break;
                case "trace":
                    addTrace(event.get("content"));
                    break;
                case "endsegmentation":
           //         GameBus.getInstance().publish(new GameEvent(json.get("player").getTextValue(), roomChannel, GameEventType.guessed));
                    saveTraces();
                    break;
                case "changeTool":
                    notifyAll(event.get("content"));
                    break;
            }
            notifyAll(event.get("content"));
        }
        else if (message instanceof GameEvent) {
        //    GameEvent event = (GameEvent) message;
            JsonNode event = ((GameEvent) message).getJson();
            event = event.get("message");
            String type = event.get("type").asText();
      //      switch (event.getType()) {
            switch (type) {
                case "matchEnd":
                    killActor();
                    gameStarted = false;
                    break;
                case "loading":
                    gameLoading();
                    break;
                case "roundBegin":
                    gameStarted = true;
                    roundBegin(event.get("content"));
                    break;
                case "showImages":
                    notifyAll(event.get("content"));
                    break;
                case "saveTraces":
                    saveTraces();
                    break;
                case "nextRound": //DONE
                    //              nextRound(event.getMessage());
                    nextRound(event.get("content").get("user").asText());
                    break;
                case "task":
       //             sendTask(event.getMessage(), event.getObject());
                    sendTask(event.get("content"));
                    break;
                case "tagS":
                    //            sendTag(event.getMessage(), event.getObject());
                    sendTag(event.get("content"));
                    break;
                case "points":
                    //            notifySingle(event.getMessage(), event.getObject());
                    notifySingle(event.get("content"));
                    break;
                case "guessed":
                    //             notifySingle(event.getMessage(), event.getObject());
                    notifyAll(event.get("content"));
                    break;
                case "timer":
                    //             notifyAll(event.getObject());
                    notifyAll(event.get("content"));
                    break;
                case "leaderboard":
                    //            notifyAll(event.getObject());
                    notifyAll(event.get("content"));
                    break;
                case "leave":
                    //           handleQuitter(event.getMessage());
                    handleQuitter(event.get("content").get("user").asText());
                case "changeTool":
                    notifyAll(event.get("content"));
                    break;
                case "beginPath":
                    notifyAll(event.get("content"));
                    break;
                case "point":
                    notifyAll(event.get("content"));
                    break;
                case "endPath":
                    sendEnd(event.get("content"));
                    break;
                case "roundEndS":
                    //            GameBus.getInstance().publish(new GameEvent(json.get("player").getTextValue(), roomChannel, GameEventType.timeExpired));
                    roundEnd(event.get("content"));
                    break;
            }
        }
    }

    private void roundEnd(JsonNode json){
        String word = json.get("word").asText();
        notifyAll(GameMessages.composeRoundEnd(word));
        String id = json.get("id").asText();
        String medialocator = json.get("url").asText();
        int width = json.get("width").asInt();
        int height = json.get("height").asInt();
        notifyAll(GameMessages.composeImage(id,medialocator,width,height));
    }

    private void roundBegin(JsonNode json){
        String sketcher = json.get("sketcher").asText();
        notifyAll(GameMessages.composeRoundBegin(sketcher));
    }

    private void saveTraces() {
    //    GameEvent tracesMessage = new GameEvent(roomChannel, GameEventType.finalTraces);
   //     ObjectNode finalTraces = new ObjectNode(factory);
        ArrayNode filtered = currentSegment.filter(taskWidth, taskHeight, 420, 350);
   //     finalTraces.put("id", taskUrl);
   //     finalTraces.put("label", guessWord);
   //     finalTraces.put("traces", filtered);
   //     finalTraces.put("history", traces);
  //      tracesMessage.setObject(finalTraces);
        GameEvent tracesMessage = new GameEvent(GameMessages.composeFinalTraces(taskUrl,guessWord,filtered,traces),roomChannel);
        GameBus.getInstance().publish(tracesMessage);
    }

    private void addTrace(JsonNode json) throws JSONException {
        Integer iKey = json.get("num").getIntValue();
        ObjectNode trace = new ObjectNode(factory);
        ArrayNode points = (ArrayNode) json.get("points");
        trace.put("points", points);
        trace.put("time", json.get("time"));
        traces.put(iKey.toString(), trace);
        int row = currentSegment.getRowSize();
        int column = 0;
        for (JsonNode object : points) {
            Point toBeAdded = new Point();
            toBeAdded.setX(object.get("x").asInt());
            toBeAdded.setY(object.get("y").asInt());
            toBeAdded.setColor(object.get("color").asText());
            toBeAdded.setSize(object.get("size").asInt());
            currentSegment.setPoint(row, column, toBeAdded);
            column++;
        }
    }

    private void handleQuitter(String quitter) throws InterruptedException {
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(quitter)) {
                entry.getValue().channel.close();
                painters.remove(quitter);
                Logger.debug("[PAINT] " + quitter + " has disconnected.");
      //          GameBus.getInstance().publish(new GameEvent(quitter, roomChannel, GameEventType.quit));
                GameBus.getInstance().publish(new GameEvent(GameMessages.composeQuit(quitter), roomChannel));
            }
        }
    }

    
/*
//    private void sendTask(String sketcher, ObjectNode task) throws Exception {
    private void sendTask(JsonNode task) throws Exception {
//        task.remove("type");
//        task.remove("role");
//        task.put("type", "task");
        String sketcher = task.get("sketcher").asText();
        taskUrl = task.get("url").getTextValue();
        URL url = new URL(taskUrl);
        taskImage = ImageIO.read(url);
        taskWidth = taskImage.getWidth();
        taskHeight = taskImage.getHeight();
        taskUrl = task.get("id").getTextValue();
        this.sketcher = sketcher;
        guessWord = task.get("tag").getTextValue();
        //Send to the users the information about their role
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            if (entry.getValue().name.equals(sketcher)) {
                entry.getValue().channel.write(GameMessages.composeTask(guessWord));
                entry.getValue().channel.write(GameMessages.composeImageInfo(task.get("id").asText(), taskUrl, taskWidth, taskHeight));
            }
            else
                entry.getValue().channel.write(GameMessages.composeTask(""));
        }
    }
    */
    private void sendTask(JsonNode task) throws Exception {
        taskUrl = task.get("url").asText();
        taskWidth = task.get("width").asInt();
        taskHeight = task.get("height").asInt();
        guessWord = task.get("word").asText();
        //Send to the users the information about their role
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            entry.getValue().channel.write(GameMessages.composeTask(guessWord));
            entry.getValue().channel.write(GameMessages.composeImage(task.get("id").asText(), taskUrl, taskWidth, taskHeight));
        }
    }

    private void sendEnd(JsonNode task) throws Exception {
        String taskWord = task.get("url").asText();
        taskUrl = task.get("url").asText();
        taskWidth = task.get("width").asInt();
        taskHeight = task.get("height").asInt();
        guessWord = task.get("word").asText();
        //Send to the users the information about their role
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            entry.getValue().channel.write(GameMessages.composeRoundEnd(taskWord));
            entry.getValue().channel.write(GameMessages.composeImage(task.get("id").asText(), taskUrl, taskWidth, taskHeight));
        }
    }

    /*
  //  private void sendTag(String sketcher, ObjectNode task) throws Exception {
    private void sendTag(JsonNode task) throws Exception {
        String sketcher = task.get("sketcher").asText();
        taskUrl = task.get("url").getTextValue();
        URL url = new URL(taskUrl);
        taskImage = ImageIO.read(url);
        taskWidth = taskImage.getWidth();
        taskHeight = taskImage.getHeight();
        taskUrl = task.get("id").getTextValue();    
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            if (entry.getValue().name.equals(sketcher)) {
                entry.getValue().channel.write(GameMessages.composeTag());
                entry.getValue().channel.write(GameMessages.composeImageInfo(task.get("id").asText(), taskUrl, taskWidth, taskHeight));
            } else {
                entry.getValue().channel.write(GameMessages.composeTag());
            }
        }
    }
    */
    private void sendTag(JsonNode task) throws Exception {
        taskUrl = task.get("url").asText();
        taskWidth = task.get("width").asInt();
        taskHeight = task.get("height").asInt();
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            entry.getValue().channel.write(GameMessages.composeTag());
            entry.getValue().channel.write(GameMessages.composeImage(task.get("id").asText(), taskUrl, taskWidth, taskHeight));
        }
    }

    private void notifyAll(JsonNode json) {
        for (Painter painter : painters.values()) {
            painter.channel.write(json);
        }
    }

    private void notifySingle(String username, JsonNode json) {
        for (Painter painter : painters.values()) {
            if (painter.name.equalsIgnoreCase(username)) {
                painter.channel.write(json);
            }
        }
    }

    private void notifySingle(JsonNode json) {
        String username = json.get("user").asText();
        for (Painter painter : painters.values()) {
            if (painter.name.equalsIgnoreCase(username)) {
                painter.channel.write(json);
            }
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    /*
     * TESTED
     */
    private void handleJoin(Join message) {
        String username = message.getUsername();
        if (painters.containsKey(username)) {
            getSender().tell(play.i18n.Messages.get(LanguagePicker.retrieveLocale(), "usernameused"), this.getSelf());
        } else if (!gameStarted) {
            Painter painter = new Painter(message.getChannel());
            painter.name = username;
            painters.put(username, painter);
            Logger.debug("[PAINT] added player " + username);
            getSender().tell("OK", this.getSelf());
        } else {
            getSender().tell(play.i18n.Messages.get(LanguagePicker.retrieveLocale(), "matchstarted"), this.getSelf());
        }
    }

    
    /*
     * Send a message to all player to wait, since the match is starting
     * [TESTED]
     */
    private void gameLoading() {
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            entry.getValue().channel.write(GameMessages.composeLoading());
        }
    }

    /*
     * [TESTED]
     */
    private void nextRound(String sketcher) {
        //Reset the traces storage
        traces = new ObjectNode(factory);
        currentSegment = new Segment("rgba(255,255,255,1.0)");

        //Send to the users the information about their role
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
                entry.getValue().channel.write(GameMessages.composeRoundBegin(sketcher));
        }
    }
}


















enum JsonNodeType {
    segment, change, trace, roundended, move, skiptask, endsegmentation, changeTool, beginPath, point, endPath
}
