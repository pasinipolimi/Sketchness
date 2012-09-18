package models;

import play.mvc.*;
import play.libs.*;
import play.libs.F.*;

import akka.util.*;
import akka.actor.*;
import akka.dispatch.*;
import static akka.pattern.Patterns.ask;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;

import java.util.*;

import models.Messages.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * A chat room is an Actor.
 */
public class ChatRoom extends UntypedActor {
    
    
        //Reference to the drawing logic
        static PaintRoom paintLogic;
    
	//Control Variables
	
	private static int requiredPlayers=3;
        private static int missingPlayers=requiredPlayers;
	private boolean gameStarted=false;
	private String currentSketcher;
        private String currentGuess;
        
        
	
    
    // Default room.
    static ActorRef defaultRoom = Akka.system().actorOf(new Props(ChatRoom.class));

    
    /**
     * Join the default room.
     */
    public static void join(final String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out, PaintRoom paintRoom) throws Exception{
        
        // Send the Join message to the room
        String result = (String)Await.result(ask(defaultRoom,new Join(username, out), 1000), Duration.create(1, SECONDS));
        
        if("OK".equals(result)) 
        {
            
            paintLogic=paintRoom;
            // For each event received on the socket,
            in.onMessage(new Callback<JsonNode>() {
               public void invoke(JsonNode event) {
                   
                   // Send a Talk message to the room.
                   defaultRoom.tell(new Talk(username, event.get("text").asText()));
               } 
            });
            
            // When the socket is closed.
            in.onClose(new Callback0() {
               public void invoke() {
                   
                   // Send a Quit message to the room.
                   defaultRoom.tell(new Quit(username));
                   
               }
            });
            
        } else {
            
            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);
            
            // Send the error to the socket.
            out.write(error);
            
        }
        
    }
    
    // Members of this room.
    Map<String, WebSocket.Out<JsonNode>> playersMap = new HashMap<String, WebSocket.Out<JsonNode>>();
    ArrayList<Painter> playersVect = new ArrayList<>();
    
    public void onReceive(Object message) throws Exception {
        
        if(message instanceof Join) 
        {
            
            // Received a Join message
            Join join = (Join)message;
            
            // Check if this username is free.
            if(playersMap.containsKey(join.username)) {
                getSender().tell("This username is already used");
            } 
            else if(!gameStarted) 
            {
                playersMap.put(join.username, join.channel);
                playersVect.add(new Painter(join.username,false));
                notifyAll("join", join.username, "has entered the room");
                if(playersMap.size()>=requiredPlayers)
                {
                	gameStarted=true;
                	nextSketcher();
                        paintLogic.matchStarted(currentSketcher);
                        paintLogic.setChatRoom(this);
                        currentGuess=paintLogic.getCurrentGuess();
                }
                else
                {
                    if(requiredPlayers-playersMap.size()>1)
                        notifyAll("system", "Sketchness", "Waiting for "+(requiredPlayers-playersMap.size())+" players to start.");
                    else
                        notifyAll("system", "Sketchness", "Waiting for "+(requiredPlayers-playersMap.size())+" player to start.");
                }
                getSender().tell("OK");
            }
            //[TODO]Disabling game started control for debug messages
            /*else
            {
            	getSender().tell("The game has already started");
            }*/
            
        } else if(message instanceof Talk)  {
            
            // Received a Talk message
            Talk talk = (Talk)message;
            if(gameStarted){
                //Compare the message sent with the tag in order to establish if we have a right guess
                if(talk.text.equalsIgnoreCase(currentGuess))
                {
                    paintLogic.guessedWord(talk.username);
                }
                else
                    notifyAll("talk", talk.username, talk.text);
            }
            else
                //The players are just chatting, not playing
                notifyAll("talk", talk.username, talk.text);
            
        } else if(message instanceof Quit)  {
            
            // Received a Quit message
            Quit quit = (Quit)message;
            
            playersMap.remove(quit.username);
            
            notifyAll("quit", quit.username, "has leaved the room");
        
        } else {
            unhandled(message);
        }
        
    }
    
    // Send a Json event to all members
    public void notifyAll(String kind, String user, String text) {
        for(WebSocket.Out<JsonNode> channel: playersMap.values()) {
            
            ObjectNode event = Json.newObject();
            event.put("kind", kind);
            event.put("user", user);
            event.put("message", text);
            
            ArrayNode m = event.putArray("members");
            for(String u: playersMap.keySet()) {
                m.add(u);
            }
            
            channel.write(event);
        }
    }   
    
    // Send a Json event to all members
    public void notifyGuesser(String kind, String user, String text) {
        for(WebSocket.Out<JsonNode> channel: playersMap.values()) {
            
            ObjectNode event = Json.newObject();
            event.put("kind", kind);
            event.put("user", user);
            event.put("message", text);
            
            ArrayNode m = event.putArray("members");
            for(String u: playersMap.keySet()) {
                m.add(u);
            }
            
            channel.write(event);
        }
    } 
    
     public void nextRound()
     {
         nextSketcher();
         paintLogic.nextRound(currentSketcher);
         currentGuess=paintLogic.getCurrentGuess();
     }
     
     public String nextSketcher()
     {
         currentSketcher=null;
         notifyAll("system", "Sketchness", "The next round has started!");
         notifyAll("system", "Sketchness", "Randomly selecting roles...");
         while(currentSketcher==null)
         {
            int index = (int)(Math.random() * ((requiredPlayers - 1) + 1));
            if(!playersVect.get(index).hasBeenSketcher)
            {
                    currentSketcher=playersVect.get(index).name;
                    playersVect.get(index).hasBeenSketcher=true;
            }
         }
         notifyAll("system", "Sketchness", "The SKETCHER is "+currentSketcher);
         return currentSketcher;
     }
     
     
     public void playerTimeExpired(String name)
     {
         for (Iterator<Painter> it = playersVect.iterator(); it.hasNext();) {
             Painter painter = it.next();
             if(painter.name.equals(name))
                 missingPlayers--;
         }
         if(missingPlayers==0)
         {
             nextRound();
             missingPlayers=requiredPlayers;
         }
     }
}
