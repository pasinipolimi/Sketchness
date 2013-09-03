package models.paint;

import java.awt.image.BufferedImage;
import java.net.URL;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;

import play.libs.Json;

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
import utils.gamebus.GameMessages.Join;
import utils.gamebus.GameMessages.Room;



public class Paint extends GameRoom{
	
    Boolean gameStarted=false;
    Room  roomChannel;
    
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
        if(message instanceof Room)
        {
            this.roomChannel=((Room)message);
            Logger.info("[PAINT] "+roomChannel.getRoom()+" created.");
        }
        if(message instanceof Join) 
        {
            handleJoin((Join)message);
        }
        if(message instanceof JsonNode )
        {
             JsonNode json = (JsonNode) message;
             JsonNodeType type = JsonNodeType.valueOf(json.get("type").getTextValue().toLowerCase());
             switch(type)
             {
                 case segment: break; //writeSegment(json);break;
                 case change: Painter painter = painters.get(json.get("name").getTextValue());
                                if(painter != null) {
                                    painter.updateFromJson(json);
                                };break;
                 case roundended: GameBus.getInstance().publish(new GameEvent(json.get("player").getTextValue(), roomChannel,GameEventType.timeExpired));break;
                 case trace: addTrace(json);break;
                 case skiptask: GameBus.getInstance().publish(new GameEvent(json.get("timerObject").getTextValue(), roomChannel,GameEventType.skipTask));break;
             }
             notifyAll(json);
        }
        else
        if(message instanceof GameEvent)
        {
            GameEvent event= (GameEvent)message;
            switch(event.getType())
            {
                case gameEnded:killActor();gameStarted=false;break;
                case gameStarted:gameStarted=true;break;
                case showImages:notifyAll(event.getObject());break;
                case saveTraces:saveTraces();break;
                case nextRound:nextRound(event.getMessage());break;
                case task:sendTask(event.getMessage(),event.getObject());break;
                case askTag:sendTag(event.getMessage(),event.getObject());break;
                case points:notifySingle(event.getMessage(),event.getObject());break;
                case guessedObject:notifySingle(event.getMessage(),event.getObject());break;
                case timerChange:notifyAll(event.getObject());break;
                case leaderboard:notifyAll(event.getObject());break;
                case quit:handleQuitter(event.getMessage());
            }
        }
    }
    
    private void handleJoin(Join message)
    {
        String username=message.getUsername();
        if(painters.containsKey(username))
            getSender().tell(play.i18n.Messages.get(LanguagePicker.retrieveLocale(),"usernameused"),this.getSelf());
        else if(!gameStarted) 
        {
           Painter painter = new Painter(message.getChannel());
           painter.name=username;
           painters.put(username, painter);

           // Inform the current painter of the whole list of users
           for(Map.Entry<String, Painter> entry : painters.entrySet()) {
                ObjectNode other = (ObjectNode)entry.getValue().toJson();
                other.put("type", "change");
                painter.channel.write(other);
            }
           GameBus.getInstance().publish(new GameEvent(username, roomChannel ,GameEventType.join));
           Logger.debug("[PAINT] added player "+username);
           getSender().tell("OK",this.getSelf());
        }
        else
        {
            getSender().tell(play.i18n.Messages.get(LanguagePicker.retrieveLocale(),"matchstarted"),this.getSelf());
        }
    }
    
    
    private void saveTraces()
    {
        GameEvent tracesMessage = new GameEvent(roomChannel,GameEventType.finalTraces);
        ObjectNode finalTraces = new ObjectNode(factory);
        ArrayNode filtered = currentSegment.filter(taskWidth,taskHeight,420,350);
        finalTraces.put("id", taskUrl);
        finalTraces.put("label", guessWord);
        finalTraces.put("traces", filtered);
        finalTraces.put("history", traces);
        tracesMessage.setObject(finalTraces);
        GameBus.getInstance().publish(tracesMessage);
    }
    
    
    private void addTrace(JsonNode json) throws JSONException
    {
        Integer iKey = json.get("num").getIntValue();
        ObjectNode trace = new ObjectNode(factory);
        ArrayNode points = (ArrayNode)json.get("points");
        trace.put("points", points);
        trace.put("time", json.get("time"));     
        traces.put(iKey.toString(),trace);
        int row = currentSegment.getRowSize();
        int column = 0;
        for (JsonNode object : points) {
            Point toBeAdded= new Point();
            toBeAdded.setX(object.get("x").asInt());
            toBeAdded.setY(object.get("y").asInt());
            toBeAdded.setColor(object.get("color").asText());
            toBeAdded.setSize(object.get("size").asInt());
            currentSegment.setPoint(row, column, toBeAdded);
            column++;
        }
    }
    
    private void handleQuitter(String quitter) throws InterruptedException
    {
        for (Map.Entry<String, Painter> entry : painters.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(quitter))
            {
                ObjectNode json = Json.newObject();
                json.put("type", "disconnect");
                json.put("username", quitter);
                notifyAll(json);

                entry.getValue().channel.close();
                painters.remove(quitter);
                Logger.debug("[PAINT] "+quitter+" has disconnected.");
                GameBus.getInstance().publish(new GameEvent(quitter,roomChannel,GameEventType.quit));
            }
        }    
    }
   
    private void nextRound(String sketcher)
    {
         //Reset the traces storage
         traces=new ObjectNode(factory);
         currentSegment = new Segment("rgba(255,255,255,1.0)");
         
         //Send to the users the information about their role
         for(Map.Entry<String, Painter> entry : painters.entrySet()) {
            if(entry.getValue().name.equals(sketcher))
            {
                entry.getValue().role="SKETCHER";
                ObjectNode roleMessage = Json.newObject();
                roleMessage.put("type", "role");
                roleMessage.put("name",entry.getValue().name);
                roleMessage.put("role","SKETCHER");
                entry.getValue().channel.write(roleMessage);
            }
            else
            {
                entry.getValue().role="GUESSER";
                ObjectNode self = Json.newObject();
                self.put("type", "role");
                self.put("name",entry.getValue().name);
                self.put("role","GUESSER");
                entry.getValue().channel.write(self);
            }
        }
   }
    
    private void sendTask(String sketcher,ObjectNode task) throws Exception
    {
        task.remove("type");
        task.remove("role");
        task.put("type", "task");
        //Send to the users the information about their role
         for(Map.Entry<String, Painter> entry : painters.entrySet()) {
            if(entry.getValue().name.equals(sketcher))
            {
                entry.getValue().channel.write(task);
            }
         }   
        
        taskUrl=task.get("image").getTextValue();
        URL url = new URL(taskUrl);
        taskImage = ImageIO.read(url);
        taskWidth = taskImage.getWidth();
        taskHeight = taskImage.getHeight();
        taskUrl=task.get("id").getTextValue();
        this.sketcher=sketcher;
        guessWord=task.get("tag").getTextValue();
    }
    
    private void sendTag(String sketcher, ObjectNode task) throws Exception
    {
        task.remove("type");
        task.put("type", "tag");
        ObjectNode cleanedTask = new ObjectNode(factory);
        cleanedTask.put("type","tag");
        for(Map.Entry<String, Painter> entry : painters.entrySet()) {
            if(entry.getValue().name.equals(sketcher))
            {
                task.put("role","SKETCHER");
                entry.getValue().channel.write(task);
            }
            else
            {
                cleanedTask.put("role","GUESSER");
                entry.getValue().channel.write(cleanedTask);
            }
         }
    }
    

    private void notifyAll(JsonNode json) {
        for(Painter painter : painters.values()) {
            painter.channel.write(json);
        }
    }
    
    private void notifySingle(String username, JsonNode json)
    {
        for(Painter painter : painters.values()) {
            if(painter.name.equalsIgnoreCase(username))
                 painter.channel.write(json);
        }
    }
}
enum JsonNodeType
{
    segment,change,trace,roundended, move, skiptask
}
