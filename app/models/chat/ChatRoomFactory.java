package models.chat;

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



public class ChatRoomFactory {

    // All the possible chatrooms
    static Map<String,ActorRef> rooms = new HashMap<>();
    

    // Members of this room.
    ArrayList<Painter> playersVect = new ArrayList<>();
    
    /**
     * Join the default room.
     */
    public static void create(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception{
        
        ActorRef newRoom;        
        if(rooms.containsKey(room))
            newRoom=rooms.get(room);
        else
        {
            newRoom= Akka.system().actorOf(new Props(Chat.class));
            rooms.put(room, newRoom);
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

}
