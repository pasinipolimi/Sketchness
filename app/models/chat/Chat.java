package models.chat;


import play.libs.*;
import play.libs.F.*;


import akka.actor.*;




import org.codehaus.jackson.node.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import utils.Messages.*;
import utils.levenshteinDistance;

import play.i18n.Messages;

import play.mvc.*;

import org.codehaus.jackson.*;

import utils.levenshteinDistance.*;

import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.PlayerQuit;
import play.Logger;

/**
 * A chat room is an Actor.
 */
public class Chat extends UntypedActor {

    String  roomChannel;
    String  currentGuess;
    Boolean gameStarted=false;

    
    
    
    
    // Members of this room.
    Map<String, WebSocket.Out<JsonNode>> playersMap = new ConcurrentHashMap<>();
    
    @Override
    public void onReceive(Object message) throws Exception {  
        
      
        if(message instanceof Room)
        {
            this.roomChannel=((Room)message).getRoom();
            Logger.info("CHATROOM "+roomChannel+" created.");
        }
        if(message instanceof GameMessages.SystemMessage)
        {
            notifyAll("system", "Sketchness", ((GameMessages.SystemMessage)message).getMessage());
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
                notifyAll("join", join.username, Messages.get("join"));
                GameBus.getInstance().publish(new GameMessages.PlayerJoin(join.username, roomChannel));
                getSender().tell("OK");
            }
            //[TODO]Disabling game started control for debug messages
            else
            {
            	getSender().tell(Messages.get("matchstarted"));
            }
        }
        else if(message instanceof Talk)  {
            
            // Received a Talk message
            Talk talk = (Talk)message;
            if(gameStarted)
			{
                 //Compare the message sent with the tag in order to establish if we have a right guess
				 levenshteinDistance distanza = new levenshteinDistance();
				 switch(distanza.computeLevenshteinDistance(talk.text, currentGuess)){
					case 0:	GameBus.getInstance().publish(new GameEvent(talk.username,roomChannel,"guessed"));
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
            
        } 
        else
        if(message instanceof GameMessages.GameEvent)
        {
            GameEvent event= (GameEvent)message;
            switch(event.getType())
            {
                case "gameStart":gameStarted=true;break;
                case "newGame":gameStarted=false;break;
                case "task":retrieveTask(event.getObject());break;
                case "quit":handleQuitter(event.getMessage());
            }
        }
        else if(message instanceof Quit)  {
            
            // Received a Quit message
            Quit quit = (Quit)message;
            playersMap.remove(quit.username);
            notifyAll("quit", quit.username, Messages.get("quit"));
            GameBus.getInstance().publish(new PlayerQuit(quit.username,roomChannel));
        } else {
            unhandled(message);
        } 
    }
    
    private  void handleQuitter(String quitter) throws InterruptedException
    {
        synchronized(playersMap)
        {
                    for (Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(quitter))
                        {
                            //Close the websocket
                            entry.getValue().close();
                            playersMap.remove(quitter);
                            notifyAll("quit", quitter, Messages.get("quit"));
                            Logger.debug("[CHAT] "+quitter+" has disconnected.");
                            GameBus.getInstance().publish(new GameEvent(quitter,roomChannel,"quit"));
                        } 
                    }
        }
    }

    
    private void retrieveTask(ObjectNode task)
    {
        currentGuess=task.get("word").asText();
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

     
}
