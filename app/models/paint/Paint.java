package models.paint;

import java.awt.image.BufferedImage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    public void onReceive(Object message) {
      try {
        //We are initializing the room 
        if (message instanceof Room) {
            this.roomChannel = ((Room) message);
            Logger.info("[PAINT] " + roomChannel.getRoom() + " created.");
        }
        if (message instanceof Join) {
            handleJoin((Join) message);
        }
        if (message instanceof GameEvent) {
            JsonNode event = ((GameEvent) message).getJson();
            if(event!=null) {
                event = event.get("message");
                if(event!=null) {
                    String type = event.get("type").asText();
                    if(type!=null) {
                        switch (type) {
                            case "matchEnd":
                                killActor();
                                gameStarted = false;
                                break;
                            case "loading":
                                gameLoading();
                                break;
                            case "skip":
                                skipTask();
                                break;
                            case "roundBegin":
                                gameStarted = true;
                                roundBegin(event.get("content"));
                                break;
                            case "saveTraces":
                                saveTraces();
                                break;
                            case "nextRound":
                                nextRound(event.get("content").get("user").asText());
                                break;
                            case "task":
                                sendTask(event.get("content"));
                                break;
                            case "tagS":
                                sendTag(event.get("content"));
                                break;
                            case "score":
                                notifyScore(event.get("content"));
                                break;
                            case "endSegmentationC":
                                notifyEndSegmentation(event.get("content"));
                                break;
                            case "guessed":
                                notifyGuessed(event.get("content"));
                                break;
                            case "timerS":
                                notifyTimer(event.get("content"));
                                break;
                            case "leaderboard":
                                notifyLeaderboard(event.get("content"));
                                break;
                            case "noTag":
                                noTag();
                                break;
                            case "guess":
                                notifyGuess(event.get("content"));
                                break;
                            case "leave":
                                handleQuitter(event.get("content").get("user").asText());
                            case "changeTool":
                                changeTool(event.get("content"));
                                break;
                            case "beginPath":
                                beginPath();
                                break;
                            case "point":
                                notifyPoint(event.get("content"));
                                break;
                            case "endPath":
                                notifyEndPath();
                                break;
                            case "roundEndS":
                                roundEnd(event.get("content"));
                                break;
                        }
                    }
                }
            }
            else
                Logger.error("Received null message");
        }
      }
      catch(Exception ex) {
          Logger.error(ex.toString());
      }
    }

    private void roundEnd(JsonNode json){
        try {
            String word = json.get("word").asText();
            notifyAll(GameMessages.composeRoundEnd(word));
            String id = json.get("id").asText();
            String medialocator = json.get("url").asText();
            int width = json.get("width").asInt();
            int height = json.get("height").asInt();
            notifyAll(GameMessages.composeImage(id,medialocator,width,height));
        }
        catch(Exception ex) {
          Logger.error(ex.toString());
        }
    }

    private void roundBegin(JsonNode json){
        try {
            String _sketcher = json.get("sketcher").asText();
            notifyAll(GameMessages.composeRoundBegin(_sketcher));
        }
        catch(Exception ex) {
          Logger.error(ex.toString());
        }
    }

    private void saveTraces() {
        ArrayNode filtered = currentSegment.filter(taskWidth, taskHeight, 420, 350);
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
        try {
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
        catch(Exception ex) {
          Logger.error(ex.toString());
        }
    }

    private void sendTask(JsonNode task) throws Exception {
        try {
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
        catch(Exception ex) {
          Logger.error(ex.toString());
        }
    }

    private void notifyEndPath() throws Exception {
        notifyAll(GameMessages.composeEndPath());
    }

    private void sendTag(JsonNode task) throws Exception {
        taskUrl = task.get("url").asText();
        taskWidth = task.get("width").asInt();
        taskHeight = task.get("height").asInt();
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            entry.getValue().channel.write(GameMessages.composeTag());
            entry.getValue().channel.write(GameMessages.composeImage(task.get("id").asText(), taskUrl, taskWidth, taskHeight));
        }
    }

    private void changeTool(JsonNode task) throws Exception {
        String tool = task.get("tool").asText();
        int size = task.get("size").asInt();
        String color = task.get("color").asText();

        notifyAll(GameMessages.composeChangeTool(tool, size, color));

    }

    private void notifyGuess(JsonNode guess)throws Exception{
        String usr = guess.get("user").asText();
        String word = guess.get("word").asText();
        String affinity = guess.get("affinity").asText();

        notifyAll(GameMessages.composeGuess(usr, word, affinity));
    }

    private void notifyGuessed(JsonNode guess)throws Exception{
        String usr = guess.get("user").asText();
        String word = guess.get("word").asText();

        notifyAll(GameMessages.composeGuessed(usr, word));
    }

    private void notifyPoint(JsonNode task) throws Exception {

        int x = task.get("x").asInt();
        int y = task.get("y").asInt();

        notifyAll(GameMessages.composePoint(x, y));

    }

    private void notifyTimer(JsonNode task) throws Exception {
        int time = task.get("time").asInt();
        notifyAll(GameMessages.composeTimerForClient(time));
    }

    private void notifyLeaderboard(JsonNode task) throws Exception {
        ObjectNode toSend = (ObjectNode) task;
        notifyAll(GameMessages.composeLeaderboard(toSend));
    }

    private void notifyScore(JsonNode task) throws Exception{
        String user = task.get("user").asText();
        int score = task.get("score").asInt();
        notifyAll(GameMessages.composeScore(user,score));
    }
    
    private void notifyEndSegmentation(JsonNode task) throws Exception{
        notifyAll(GameMessages.composeEndSegmentation());
    }

    private void beginPath() throws Exception {
        notifyAll(GameMessages.composeBeginPath());
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
    
    private void noTag() {
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            entry.getValue().channel.write(GameMessages.composeNoTag());
        }
    }
    
    private void skipTask() {
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            entry.getValue().channel.write(GameMessages.composeSkip());
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
        notifyAll(GameMessages.composeRoundBegin(sketcher));
    }
}


enum JsonNodeType {
    segment, change, trace, roundended, move, skiptask, endsegmentation, changeTool, beginPath, point, endPath
}
