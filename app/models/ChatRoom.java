package models;

import play.libs.*;
import play.libs.F.*;

import akka.util.*;
import akka.actor.*;
import akka.dispatch.Futures;
import static akka.pattern.Patterns.ask;

import static akka.dispatch.Futures.future;
import scala.concurrent.Await;

import scala.concurrent.duration.Duration;

import akka.actor.ActorSystem;
import akka.dispatch.Future;
import akka.pattern.Patterns;

import org.codehaus.jackson.node.*;

import java.util.*;

import models.Messages.*;

import play.i18n.Messages;

import play.mvc.*;

import org.codehaus.jackson.*;

import models.levenshteinDistance.*;

import models.Chat;



import static java.util.concurrent.TimeUnit.*;
import play.Logger;

/**
 * A chat room is an Actor.
 */
public class ChatRoom {
    
    
        //Reference to the drawing logic
        static PaintRoom paintLogic;
    
	//Control Variables
	
		private static final int requiredPlayers=3;
        private static int missingPlayers=requiredPlayers;
        private int disconnectedPlayers=0;
		private boolean gameStarted=false;
		private String currentSketcher;
        private String currentGuess;
        private ChatRoom current=this;
        
        private int roundNumber=1;
        private static int maxRound=6;
		
		private static Boolean shownImages=false;
        
	
    
    // Default room.
    static Map<String,ActorRef> rooms = new HashMap<String, ActorRef>();
    

    // Members of this room.
    Map<String, WebSocket.Out<JsonNode>> playersMap = new HashMap<String, WebSocket.Out<JsonNode>>();
    ArrayList<Painter> playersVect = new ArrayList<Painter>();
    
    /**
     * Join the default room.
     */
    public static void join(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out, PaintRoom paintRoom) throws Exception{
        
        ActorRef newRoom;        
        if(rooms.containsKey(room))
            newRoom=rooms.get(room);
        else
        {
            newRoom= Akka.system().actorOf(new Props(Chat.class));
            rooms.put(room, newRoom);
        }

		final ActorRef finalRoom=newRoom;
        
            Future<ChatRoom> future = Patterns.ask(finalRoom,new Join(username, out), 1000);
        // Send the Join message to the room
        String result = (String)Await.result(future, Duration.create(10, SECONDS));
        
        if("OK".equals(result)) 
        {
            
            paintLogic=paintRoom;
            // For each event received on the socket,
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
            
        } else {
            
            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);
            
            // Send the error to the socket.
            out.write(error);
            
        }
        
    }
    
    
    public void tryStartMatch()
    {
                if(playersMap.size()>=requiredPlayers)
                {
                        paintLogic.setChatRoom(this);
                        //BAD BAD PRACTICE BUT WE HAVE TO REWORK EVERYTHING TO FIX THIS
                        while(paintLogic.paintersSize()!=playersMap.size())
                        {
                            try {
                                    Thread.sleep(100);
                                } catch (InterruptedException ex) {
                                    Logger.error("TruStartMatch", ex);
                                }
                        }
                        disconnectedPlayers=0;
                	gameStarted=true;
                	nextSketcher();
                        paintLogic.matchStarted(currentSketcher);
                        currentGuess=paintLogic.getCurrentGuess();
                }
                else
                {
                    //if(requiredPlayers-playersMap.size()>1)
                       // notifyAll("system", "Sketchness", Messages.get("waitingfor")+(requiredPlayers-playersMap.size())+Messages.get("playerstostart"));
                    //else
                        //notifyAll("system", "Sketchness", Messages.get("waitingfor")+(requiredPlayers-playersMap.size())+Messages.get("playertostart"));
                }
    }
    
    
    
    
     public void nextRound()
     {
         roundNumber++;
         if(roundNumber<=maxRound)
         {
            nextSketcher();
            paintLogic.nextRound(currentSketcher);
            currentGuess=paintLogic.getCurrentGuess();
         }
         else
         {
             //Manage round end
             //notifyAll("system", "Sketchness", Messages.get("end"));
             paintLogic.gameEnded();
             newGameSetup();
         } 
     }
	 
	 public void showImages()
	 {
		paintLogic.showImages();
	 }
     
     public String nextSketcher()
     {
         currentSketcher=null;
         int currentPlayers=requiredPlayers-disconnectedPlayers;
         int count=0;
         //notifyAll("system", "Sketchness", Messages.get("newround"));
         //notifyAll("system", "Sketchness", Messages.get("choosingroles"));
         while(currentSketcher==null)
         {
            if(count>currentPlayers)
            {
                for(int i=0;i<currentPlayers;i++)
                {
                    playersVect.get(i).hasBeenSketcher=false;
                }
                count=0;
            }
            else
            {
                int index = (int)(Math.random() * currentPlayers);
                if(!playersVect.get(index).hasBeenSketcher)
                {
                        currentSketcher=playersVect.get(index).name;
                        playersVect.get(index).hasBeenSketcher=true;
                }
                count++;
            }
         }
       //  notifyAll("system", "Sketchness", Messages.get("thesketcheris")+currentSketcher);
         return currentSketcher;
     }
     
     
     public void playerTimeExpired(String name)
     {
         if(((requiredPlayers-disconnectedPlayers)<=1)&&gameStarted)
         {
             //Restart the game
             paintLogic.gameEnded();
             newGameSetup();
         }
         else
         {
			if(missingPlayers>0)
			{
				for (Iterator<Painter> it = playersVect.iterator(); it.hasNext();) {
					Painter painter = it.next();
					if(painter.name.equals(name))
						missingPlayers--;
				}
			}
            if((missingPlayers-disconnectedPlayers)==0)
            {
				if(shownImages==false)
				{
					showImages();
					shownImages=true;
					missingPlayers=requiredPlayers;
				}
				else
				{
					nextRound();
					shownImages=false;
					missingPlayers=requiredPlayers;
				}
            }
         }
     }
     
     
     public void newGameSetup()
     {
         disconnectedPlayers=0;
         roundNumber=1;
         playersMap = new HashMap<String, WebSocket.Out<JsonNode>>();
         gameStarted=false;
         playersVect = new ArrayList<Painter>();
     }

    public String getCurrentGuess() {
        return currentGuess;
    }
     
     
}
