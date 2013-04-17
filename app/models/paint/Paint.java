package models.paint;

import akka.actor.UntypedActor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;

import play.libs.Json;


import utils.Messages;
import models.Painter;
import models.Point;
import models.Segment;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.GameEvent;
import net.coobird.thumbnailator.Thumbnails;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.json.JSONException;
import sun.misc.BASE64Decoder;


public class Paint extends UntypedActor{
	
    Boolean gameStarted=false;
    String  roomChannel;
    
    BufferedImage taskImage;
    String taskUrl;
    String sketcher;
    String guessWord;
    
    private Segment currentSegment=null;
    
   // The list of all connected painters (identified by ids)
   private Map<String, Painter> painters = new ConcurrentHashMap<>();
   
   //Traces 
   JsonNodeFactory factory = JsonNodeFactory.instance;
   private ObjectNode traces = new ObjectNode(factory);
    
                    
    //Manage the messages
    @Override
    public void onReceive(Object message) throws Exception {
        //We are initializing the room 
        if(message instanceof Messages.Room)
        {
            this.roomChannel=((Messages.Room)message).getRoom();
            Logger.info("PAINTROOM "+roomChannel+" created.");
        }
        if(message instanceof Messages.Join) 
        {
            Messages.Join join = (Messages.Join)message;
            String username=join.username;
            if(painters.containsKey(username))
                getSender().tell(play.i18n.Messages.get("usernameused"));
            else if(!gameStarted) 
            {
                Painter painter = new Painter(join.channel);
                    painter.name=username;
                    painters.put(username, painter);

                   // Inform the current painter of the whole list of users
                   for(Map.Entry<String, Painter> entry : painters.entrySet()) {
                        ObjectNode other = (ObjectNode)entry.getValue().toJson();
                        other.put("type", "change");
                        painter.channel.write(other);
                    }
                GameBus.getInstance().publish(new GameMessages.PlayerJoin(username, roomChannel));
                Logger.debug("PAINT ROOM ADDED "+username);
                getSender().tell("OK");
            }
            else
            {
            	getSender().tell(play.i18n.Messages.get("matchstarted"));
            }
        }
        if(message instanceof JsonNode )
        {
             JsonNode json = (JsonNode) message;
             String type = json.get("type").getTextValue().toLowerCase();
             switch(type)
             {
                 case "segment": writeSegment(json);break;
                 case "change": Painter painter = painters.get(json.get("name").getTextValue());
                                if(painter != null) {
                                    painter.updateFromJson(json);
                                };break;
                 case "roundended": GameBus.getInstance().publish(new GameEvent(json.get("player").getTextValue(), roomChannel,"timeExpired"));break;
                 case "trace":addTrace(json);break;
                     
             }
             notifyAll(json);
        }
        else
        if(message instanceof GameEvent)
        {
            GameEvent event= (GameEvent)message;
            switch(event.getType())
            {
                case "newGame":gameStarted=false;break;
                case "gameStart":gameStarted=true;break;
                case "showImages":notifyAll(event.getObject());break;
                case "nextRound":nextRound(event.getMessage());break;
                case "task":sendTask(event.getMessage(),event.getObject());break;
                case "sketcherPoints":notifySingle(event.getMessage(),event.getObject());break;
                case "guesserPoints":notifySingle(event.getMessage(),event.getObject());break;
                case "guessedObject":notifySingle(event.getMessage(),event.getObject());break;
                case "timerChange":notifyAll(event.getObject());break;
                case "leaderboard":notifyAll(event.getObject());break;
                case "quit":handleQuitter(event.getMessage());
            }
        }
    }
    
    
    private void saveTraces()
    {
        GameEvent tracesMessage = new GameEvent("",roomChannel,"finalTraces");
        traces.put("id", taskUrl);
        traces.put("label", guessWord);
        tracesMessage.setObject((ObjectNode)traces);
        GameBus.getInstance().publish(tracesMessage);
        currentSegment.filter();
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
        synchronized(painters)
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
                            GameBus.getInstance().publish(new GameEvent(quitter,roomChannel,"quit"));
                        }
                    }
        }
    }
   
    private void nextRound(String sketcher)
    {
         //It's the first match of the game, we have noting to store
         if(currentSegment!=null)
         {

             saveTraces();
         }
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
        taskUrl=task.get("id").getTextValue();
        this.sketcher=sketcher;
        guessWord=task.get("word").getTextValue();
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
    
    
    /*
     * Function used to store on the server the results of the segmentation of 
     * the players. It will be modified to use the CMS in the future
     */
    private void writeSegment(JsonNode json) throws Exception
    {
                String image=json.get("image").getTextValue();
                image=image.replace("data:image/png;base64,", "");
                BASE64Decoder decoder = new BASE64Decoder();
                byte[] decodedBytes = decoder.decodeBuffer(image);
                Logger.debug("Decoded upload data : " + decodedBytes.length);

                String data = new Date().toString();
                String urlSegment = "./results/"+taskUrl+"_segment.png";
                String urlTask = "./results/"+taskUrl+".png";

                BufferedImage imageB = ImageIO.read(new ByteArrayInputStream(decodedBytes));
                
                imageB=Thumbnails.of(imageB).forceSize(taskImage.getWidth(), taskImage.getHeight()).asBufferedImage();
                
                if (imageB == null) {
                      Logger.error("Buffered Image is null");
                  }
                File fTask = new File(urlTask);
                File fSegment = new File(urlSegment);

                // write the image and the segment
                ImageIO.write(taskImage, "png", fTask);
                ImageIO.write(imageB, "png", fSegment);
    }
}
