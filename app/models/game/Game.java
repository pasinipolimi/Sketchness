package models.game;


import play.libs.F.*;


import akka.actor.*;


import java.util.*;
import models.chat.ChatRoomFactory;

import models.Messages.*;
import models.paint.PaintRoom;
import models.Painter;
import models.levenshteinDistance;

import play.i18n.Messages;

import play.mvc.*;

import org.codehaus.jackson.*;

import models.levenshteinDistance.*;




import play.Logger;

/**
 * A chat room is an Actor.
 */
public class Game extends UntypedActor {
    
    
        //Reference to the drawing logic
        static PaintRoom paintLogic;
    
	//Control Variables
	
		private static final int requiredPlayers=3;
        private static int missingPlayers=requiredPlayers;
        private int disconnectedPlayers=0;
		private boolean gameStarted=false;
		private String currentSketcher;
        private String currentGuess;
       public ChatRoomFactory current=null;
        
        private int roundNumber=1;
        private static int maxRound=6;
		
		private static Boolean shownImages=false;
        
	
    
    // Default room.
    static Map<String,ActorRef> rooms = new HashMap<String, ActorRef>();
    
    
    
    // Members of this room.
    ArrayList<Painter> playersVect = new ArrayList<Painter>();
    
    public void tryStartMatch(Join join)
    {
                if(playersVect.size()>=requiredPlayers)
                {
                        disconnectedPlayers=0;
                	gameStarted=true;
                	nextSketcher();
                        paintLogic.matchStarted(currentSketcher);
                        currentGuess=paintLogic.getCurrentGuess();
                }
                else
                {
                    //if(requiredPlayers-playersMap.size()>1)
                        //notifyAll("system", "Sketchness", Messages.get("waitingfor")+(requiredPlayers-playersMap.size())+Messages.get("playerstostart"));
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
         gameStarted=false;
         playersVect = new ArrayList<>();
     }

    public String getCurrentGuess() {
        return currentGuess;
    }
    
    @Override
    public void onReceive(Object message) throws Exception {  
        if(message instanceof Join) 
        {
            // Received a Join message
            Join join = (Join)message;
            tryStartMatch(join);
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
					//case 1: notifyAll("talkNear", talk.username, talk.text);
					        //break;
					//case 2: notifyAll("talkWarning", talk.username, talk.text);
					        ///break;
					//default: notifyAll("talkError", talk.username, talk.text);
			                 //break;
				}
            }
            else{}
                //The players are just chatting, not playing
                //notifyAll("talk", talk.username, talk.text);
            
        } else if(message instanceof Quit)  {
            
            // Received a Quit message
            Quit quit = (Quit)message;
            
            //playersMap.remove(quit.username);
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
            
            //notifyAll("quit", quit.username, Messages.get("quit"));
            disconnectedPlayers++;
            //End the game if there's just one player or less
            if(((requiredPlayers-disconnectedPlayers)<=1)&&gameStarted)
            {
                //Restart the game
                paintLogic.gameEnded();
               // current.newGameSetup();
            }
        } else {
            unhandled(message);
        } 
    }

}
