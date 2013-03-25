package models;

import play.libs.*;
import play.libs.F.*;

import akka.util.*;
import akka.actor.*;
import akka.dispatch.*;
import static akka.pattern.Patterns.ask;
import scala.concurrent.Await;

import scala.concurrent.duration.Duration;

import akka.actor.ActorSystem;
import static akka.pattern.Patterns.ask;

import org.codehaus.jackson.node.*;

import java.util.*;

import models.Messages.*;

import play.i18n.Messages;

import play.mvc.*;

import org.codehaus.jackson.*;

import models.levenshteinDistance.*;



import static java.util.concurrent.TimeUnit.*;
import static models.ChatRoom.rooms;
import play.Logger;

/**
 * A chat room is an Actor.
 */
public class Chat extends UntypedActor {
    
    
        //Reference to the drawing logic
        static PaintRoom paintLogic;
    
	//Control Variables
	
		private static final int requiredPlayers=3;
        private static int missingPlayers=requiredPlayers;
        private int disconnectedPlayers=0;
		private boolean gameStarted=false;
		private String currentSketcher;
        private String currentGuess;
       public ChatRoom current=null;
        
        private int roundNumber=1;
        private static int maxRound=6;
		
		private static Boolean shownImages=false;
        
	
    
    // Default room.
    static Map<String,ActorRef> rooms = new HashMap<String, ActorRef>();
    
    
    
    // Members of this room.
    Map<String, WebSocket.Out<JsonNode>> playersMap = new HashMap<String, WebSocket.Out<JsonNode>>();
    ArrayList<Painter> playersVect = new ArrayList<Painter>();
    
    @Override
    public void onReceive(Object message) throws Exception {  
        if (message instanceof ChatRoom)
        {
            current=(ChatRoom)message;
            current.tryStartMatch();
        }
        if(message instanceof Join) 
        {
            // Received a Join message
            Join join = (Join)message;
            // Check if this username is free.
            if(playersMap.containsKey(join.username)) {
                getSender().tell(Messages.get("usernameused"));
            } 
            else if(!gameStarted) 
            {
                playersMap.put(join.username, join.channel);
                playersVect.add(new Painter(join.username,false));
                notifyAll("join", join.username, Messages.get("join"));
                getSender().tell("OK");
            }
            //[TODO]Disabling game started control for debug messages
            else
            {
            	getSender().tell(Messages.get("matchstarted"));
            }
            
        } else if(message instanceof Talk)  {
            
            // Received a Talk message
            Talk talk = (Talk)message;
            if(gameStarted)
			{
                 //Compare the message sent with the tag in order to establish if we have a right guess
				 levenshteinDistance distanza = new levenshteinDistance();
				 int lenLength = distanza.computeLevenshteinDistance(talk.text, currentGuess);
				 switch(distanza.computeLevenshteinDistance(talk.text, currentGuess)){
					case 0:	paintLogic.guessedWord(talk.username);
					        break;
					case 1: notifyAll("talkNear", talk.username, talk.text);
					        break;
					case 2: notifyAll("talkWarning", talk.username, talk.text);
					        break;
					default: notifyAll("talkError", talk.username, talk.text);
			                 break;
				}
            }
            else
                //The players are just chatting, not playing
                notifyAll("talk", talk.username, talk.text);
            
        } else if(message instanceof Quit)  {
            
            // Received a Quit message
            Quit quit = (Quit)message;
            
            playersMap.remove(quit.username);
            for (Painter painter : playersVect) {
                if(painter.name.equalsIgnoreCase(quit.username)){
                    playersVect.remove(painter);
                    break;
                }
            }
            for (int key : paintLogic.painters.keySet())
            {
                if(paintLogic.painters.get(key).name.equalsIgnoreCase(quit.username)){
                   paintLogic.painters.remove(key);
                   break;
                }
            }
            
            notifyAll("quit", quit.username, Messages.get("quit"));
            disconnectedPlayers++;
            //End the game if there's just one player or less
            if(((requiredPlayers-disconnectedPlayers)<=1)&&gameStarted)
            {
                //Restart the game
                paintLogic.gameEnded();
                current.newGameSetup();
            }
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

     
}
