package models.paint;

import akka.actor.UntypedActor;

import java.util.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;

import play.libs.Json;


import models.Messages;
import models.Messages.Quit;
import models.Painter;
import models.gamebus.GameBus;
import models.gamebus.GameMessages;
import models.gamebus.GameMessages.GameEvent;


public class Paint extends UntypedActor{
	
    Boolean gameStarted=false;
    String  roomChannel;
    
    
   // The list of all connected painters (identified by ids)
   private Map<String, Painter> painters = new HashMap<>();
    
                    
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
             String type = json.get("type").getTextValue();
             
                    // The painter wants to change some of his property
                    if(type.equalsIgnoreCase("change")) {
                            Painter painter = painters.get(json.get("name").getTextValue());
                            if(painter != null) {
                                painter.updateFromJson(json);
                            }
                            
                    }
                    else if (type.equalsIgnoreCase("roundEnded")) 
                    {
                        GameBus.getInstance().publish(new GameEvent(json.get("player").getTextValue(), roomChannel,"timeExpired"));
                    }
                    notifyAll(json);
        }
        else
        if(message instanceof GameEvent)
        {
            GameEvent event= (GameEvent)message;
            switch(event.getType())
            {
                case "gameStart":gameStarted=true;break;
                case "showImages":notifyAll(event.getObject());break;
                case "nextRound":nextRound(event.getMessage());break;
                case "task":sendTask(event.getMessage(),event.getObject());break;
                case "sketcherPoints":notifySingle(event.getMessage(),event.getObject());break;
                case "guesserPoints":notifySingle(event.getMessage(),event.getObject());break;
                case "guessedObject":notifySingle(event.getMessage(),event.getObject());break;
                case "timerChange":notifyAll(event.getObject());break;
                case "leaderboard":notifyAll(event.getObject());break;
            }
        }
            if(message instanceof Quit)
            {
                /*//QUIT MESSAGE
                   / Quit received= (Quit)message;
                    Integer toRemove=null;
                    for (Map.Entry<Integer, Painter> entry : painters.entrySet()) {
                        Painter painter = entry.getValue();
                        if(painter.name.equalsIgnoreCase(received.username)){
                            toRemove=entry.getKey();
                        }
                    }
                    if(null!=toRemove)
                    {
                        painters.remove(toRemove);
                        connections.decrementAndGet();

                        ObjectNode json = Json.newObject();
                        json.put("type", "disconnect");
                        json.put("pid", toRemove);

                        notifyAll(json);

                        Logger.debug("(pid:"+toRemove+") disconnected.");
                        Logger.info(connections+" painter(s) currently connected.");
                        //GameBus.getInstance().publish(new GameMessages.PlayerQuit(quit.username,roomChannel));
                    }*/
            }
    }
   
    private void nextRound(String sketcher)
    {
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
    
    private void sendTask(String sketcher,ObjectNode task)
    {
        //Send to the users the information about their role
         for(Map.Entry<String, Painter> entry : painters.entrySet()) {
              if(entry.getValue().name.equals(sketcher))
            {
                entry.getValue().channel.write(task);
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
