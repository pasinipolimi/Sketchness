package models.factory;

import models.chat.*;
import models.gamebus.GameBus;
import play.libs.*;
import play.libs.F.*;

import akka.actor.*;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;




import akka.pattern.Patterns;

import org.codehaus.jackson.node.*;

import java.util.*;

import models.Messages.*;
import models.Painter;


import play.mvc.*;

import org.codehaus.jackson.*;

import models.levenshteinDistance.*;




import static java.util.concurrent.TimeUnit.*;
import play.Logger;
import scala.concurrent.Future;



public abstract class Factory {
    

    // All the possible chatrooms
    static Map<String,ActorRef> rooms = new HashMap<>();
    

    // Members of this room.
    ArrayList<Painter> playersVect = new ArrayList<>();

    /**
     * Join the default room.
     */
    protected static void create(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out, Class module) throws Exception{
        ActorRef newRoom;     
        String roomID=room+"_"+module.getSimpleName();
        if(rooms.containsKey(roomID))
        {
            newRoom=rooms.get(roomID);
             Logger.debug("CHATFACTORY:EXISTING "+roomID);
        }
        else
        {
            Logger.debug("CHATFACTORY:CREATING "+roomID);
            newRoom= Akka.system().actorOf(new Props(module),roomID);
            rooms.put(roomID, newRoom);
            newRoom.tell(new Room(room));
            GameBus.getInstance().subscribe(newRoom, room);
        }
        
        final ActorRef finalRoom=newRoom;
        
            
        Future<Object> future = (Future<Object>) Patterns.ask(finalRoom,new Join(username, out), 1000);
        // Send the Join message to the room
        String result = (String)Await.result(future, Duration.create(10, SECONDS));
        
        if("OK".equals(result)) 
        {
            
            //Define the actions to be performed on the websockets
            in.onMessage(new Callback<JsonNode>() {
                @Override
               public void invoke(JsonNode event) {
                   
                   // Send a Talk message to the room.
                   finalRoom.tell(new Talk(username, event.get("text").asText()));
                   finalRoom.tell(this);
               } 
            });
            
            // When the socket is closed.
            in.onClose(new Callback0() {
                @Override
               public void invoke() {
                   
                   // Send a Quit message to the room.
                   finalRoom.tell(new Quit(username));
                   Logger.debug("L'UTENTE HA QUITTATO");
                   
               }
            });
            
        } 
        else 
        {
            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);
            
            // Send the error to the socket.
            out.write(error);
        }
        
    }
    
    
     protected static void create(final String room, Class module) throws Exception{
        ActorRef newRoom;     
        String roomID=room+"_"+module.getSimpleName();
        if(rooms.containsKey(roomID))
        {
            //newRoom=rooms.get(roomID);
            Logger.debug("GAMEFACTORY:EXISTING "+roomID);
        }
        else
        {
            Logger.debug("GAMEFACTORY:CREATING "+roomID);
            newRoom= (ActorRef)Akka.system().actorOf(new Props(module));
            rooms.put(roomID, newRoom);
            newRoom.tell(new Room(room));
            GameBus.getInstance().subscribe(newRoom, room);
        }
    }

}
