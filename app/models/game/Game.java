package models.game;


import play.libs.F.*;


import akka.actor.*;


import java.util.*;
import models.chat.ChatRoomFactory;

import models.Messages.*;
import models.Painter;
import models.gamebus.GameBus;
import models.gamebus.GameMessages;
import models.gamebus.GameMessages.GameEvent;
import models.gamebus.GameMessages.PlayerJoin;
import models.gamebus.GameMessages.PlayerQuit;
import models.gamebus.GameMessages.SystemMessage;


import play.i18n.Messages;
import models.levenshteinDistance.*;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;




import play.Logger;
import play.libs.Json;

/**
 * A chat room is an Actor.
 */
public class Game extends UntypedActor {

    //[TODO] Should be declared as config variables
    //Variables to manage the point system in the game
    private Boolean guessedWord=false; //Has the word been guessed for the current round?
    private Integer remainingTimeOnGuess=20;  //Once a player has guessed, change the time for everyone to 20 seconds
    private Integer remainingTimeOnAllGuess=3; //If all have guessed, reduce the timer to 3 seconds
    private int guesserPointsRemaining=10;  //The number of points available to the guessers:the first get 10, the second 9 and so on
    private int sketcherPointsRemaining=0;  //The number of points available to the sketcher: for the first reply he gets 5, for the second 4 and so on
    
    
    //Variables used to manage the rounds
    private boolean gameStarted=false;
    private int roundNumber=1;  //Starts with round number 1
    private static int maxRound=6;  //Maximum number of rounds
    private int numberGuessed=0;   //Number of players that have guessed for a specific round
    private Painter sketcherPainter;  //The current sketcher
    
    //System variables
    private int modules = 2; //Number of modules available in the game (chat/paint)
    String  roomChannel;  //Name of the room
    
    //Control Variables
    private ObjectNode guessObject;
    private static final int requiredPlayers=3;
    private static int missingPlayers=requiredPlayers;
    private int disconnectedPlayers=0;
    
    private static Boolean shownImages=false;
        
    private HashSet<ObjectNode> taskHashSet = new HashSet<>();
    
    
    
    // Members of this room.
    ArrayList<Painter> playersVect = new ArrayList<>();
    
    
    /*
     * Retrieves one of the images that has been stored to be segmented at random
     * being careful not to retrieve the same image two times for the same match.
     * <p>
     * @return The object related to the task: image + tag
     */
    public ObjectNode retrieveTaskImage()
    {
         guessObject=null;
         int size = taskHashSet.size();
         int item = new Random().nextInt(size);
         int i = 0;
         for(ObjectNode obj : taskHashSet)
         {
            if (i == item)
            {
                guessObject=obj;
                break;
            }
            i = i + 1;
         }
         taskHashSet.remove(guessObject);
         return guessObject;
    }
    
    
     /*
     * Retrieves the name of the sketcher for the next round. It assigns the sketcher
     * role to the players that have never played as such first
     * <p>
     * @return The object related to the task: image + tag
     */
     public String nextSketcher()
     {
         sketcherPainter=null;
         int currentPlayers=requiredPlayers-disconnectedPlayers;
         int count=0;
         //Publish system messages to inform that a new round is starting and the roles are being chosen
         GameBus.getInstance().publish(new SystemMessage(Messages.get("newround"), roomChannel));
         GameBus.getInstance().publish(new SystemMessage(Messages.get("choosingroles"), roomChannel));

         //Keep searching for a new sketcher
         while(sketcherPainter==null)
         {
            //If all the players have been sketcher at least one time, reset their status
            if(count>currentPlayers)
            {
                for(int i=0;i<currentPlayers;i++)
                {
                    playersVect.get(i).hasBeenSketcher=false;
                }
                count=0;
            }
            //Find a sketcher at random among the ones that have never played such a role
            else
            {
                int index = (int)(Math.random() * currentPlayers);
                if(!playersVect.get(index).hasBeenSketcher)
                {
                        sketcherPainter=playersVect.get(index);
                        sketcherPainter.hasBeenSketcher=true;
                }
                count++;
            }
         }
         //Publish a system message to inform the other players on who is the sketcher
         GameBus.getInstance().publish(new SystemMessage(Messages.get("thesketcheris")+" "+sketcherPainter.name, roomChannel));
         return sketcherPainter.name;
     }
     
     
     /*
     * Check if there are enough players connected and that all the modules have
     * received the login information regarding all these players.
     * If not enough players are connected, inform the players with a message and
     * wait for new connections or for all the modules to receive the login of the
     * respective players
     * <p>
     */
    public void checkStart() throws Exception
    {
                boolean canStart=true;
                for (Painter painter : playersVect) {
                   if(painter.getnModulesReceived()<modules)
                       canStart=false;
                }
                //We need to wait for all the modules to receive the player list
                if(canStart&&playersVect.size()>=requiredPlayers)
                {
                        disconnectedPlayers=0;
                        roundNumber=1;
                	gameStarted=true;
                        //We start the game
                        nextRound();
                        if(sketcherPainter!=null)
                            GameBus.getInstance().publish(new GameEvent(sketcherPainter.name, roomChannel,"gameStart"));
                        else
                            throw new Exception("[Error][Game]: Cannot find a suitable Sketcher!");
                }
                //Send a message to inform about the missing players
                else
                {
                    int nPlayers=playersVect.size();
                    if(requiredPlayers-nPlayers>1)
                        GameBus.getInstance().publish(new SystemMessage(Messages.get("waitingfor")+" "+(requiredPlayers-nPlayers)+" "+Messages.get("playerstostart"), roomChannel));
                    else if (requiredPlayers-nPlayers==1)
                        GameBus.getInstance().publish(new SystemMessage(Messages.get("waitingfor")+" "+(requiredPlayers-nPlayers)+" "+Messages.get("playertostart"), roomChannel));
                }
    }
    
    
    
     /*
     * Start a new round of the game
     */
     public void nextRound()
     {
         roundNumber++;
         if(roundNumber<=maxRound)
         {
           //Reset the points and status counters
           guesserPointsRemaining=10;
           sketcherPointsRemaining=0;
           numberGuessed=0;
           guessedWord=false;
           //Nobody has guessed for this round
           for (Painter reset : playersVect) {
                 reset.guessed=false;
           }
           //Find the new sketcher
           nextSketcher();
           //Inform the other modules that we are starting a new round
           GameBus.getInstance().publish(new GameEvent(sketcherPainter.name,roomChannel,"nextRound"));
           //We send the right task to the right sketcher
           GameEvent task = new GameEvent(sketcherPainter.name,roomChannel,"task");
           task.setObject(retrieveTaskImage());
           GameBus.getInstance().publish(task);
         }
         //We have played all the rounds for the game, inform the users and the modules
         //that the match has ended
         else
         {
             gameEnded();
             newGameSetup();
         } 
     }
	 
     

     
     /*
     * Check if the timer for all the players has expired, show the solution for the current
     * round and start a new one
     */
     public void playerTimeExpired(String name)
     {
         //If all the players have disconnected during a game, start a new one
         if(((requiredPlayers-disconnectedPlayers)<=1)&&gameStarted)
         {
             //Restart the game
             gameEnded();
             newGameSetup();
         }
         //There are still players in game
         else
         {
            //We are still missing the end response from some players
            //[TODO] POSSIBLE EXPLOIT
            if(missingPlayers>0)
	    {
	      for (Iterator<Painter> it = playersVect.iterator(); it.hasNext();) {
		Painter painter = it.next();
		if(painter.name.equals(name))
			missingPlayers--;
	      }
	    }
            //If we have received a response from all the active players in the game, end the round
            if((missingPlayers-disconnectedPlayers)==0)
            {
                //Before calling the new round, show the solution to all the players
		if(shownImages==false)
		{
		    showImages();
		    shownImages=true;
		    missingPlayers=requiredPlayers;
		}
                //If the solution has been given, start a new round
		else
		{
		  nextRound();
		  shownImages=false;
		  missingPlayers=requiredPlayers;
		}
            }
         }
     }
     
     /*
     * Initialization of the variables to start a new game
     * [TODO] Shouldn't we kill the current actor and all the associated ones?
     */
     public void newGameSetup()
     {
         disconnectedPlayers=0;
         roundNumber=1;
         gameStarted=false;
         playersVect = new ArrayList<>();
         sketcherPainter=null;
     }
    
    
    /*
     * Handles all the messages sent to this actor
     */
    @Override
    public void onReceive(Object message) throws Exception {
        
        if(message instanceof Room)
        {
            this.roomChannel=((Room)message).getRoom();
            Logger.info("GAMEROOM "+roomChannel+" created.");
            taskSetInitialization();
        }
        if(message instanceof PlayerJoin) 
        {
            int count=0;
            String username=((PlayerJoin)message).getUser();
            for (Painter painter : playersVect) {
                if(painter.name.equalsIgnoreCase(username)){
                    //playersVect.remove(painter);
                    painter.setnModulesReceived(painter.getnModulesReceived()+1);
                    count=painter.getnModulesReceived();
                    //playersVect.add(painter);
                    Logger.debug("GAMEROOM: player "+username+" counted "+count);
                    break;
                }
           }
           if(count==0)
           {             
               Painter painter=new Painter(username, false);
               painter.setnModulesReceived(1);
                //Add the new entered player, it has never been a sketcher in this game (false)
                playersVect.add(painter);
                Logger.info("GAMEROOM: added player "+playersVect.get(playersVect.size()-1).name);
                //Check if we can start the game and, in such a case, start it
           }
           //Wait to see if all the modules have received the login information from the players
           else if (modules>1 ? count>(modules-1) : count>1)
           {
               checkStart();
           }
        }
        if(message instanceof GameMessages.GameEvent)
        {
            GameEvent event= (GameEvent)message;
            switch(event.getType())
            {
                //[TODO] Get rid of the other messages and use just game event
                case "playerJoin":break;
                case "playerQuit":break;
                case "guessed":guessed(event.getMessage());break;
                case "timeExpired": playerTimeExpired(event.getMessage());
            }
        }
        if(message instanceof PlayerQuit) 
        {
            for (Painter painter : playersVect) {
                if(painter.name.equalsIgnoreCase(((PlayerQuit)message).getUser())){
                    playersVect.remove(painter);
                    break;
                }
            }
            disconnectedPlayers++;
            //End the game if there's just one player or less
            if(((requiredPlayers-disconnectedPlayers)<=1)&&gameStarted)
            {
                //Restart the game
                gameEnded();
                newGameSetup();
            }
        }
    }
    
    private void guessed(String guesser)
    {
        for (Painter painter : playersVect) {
             //If the current painter is the guesser, has not guessed before and it is not the sketcher, update his points
             if(painter.name.equals(guesser)&&painter.guessed==false)
             {
                 numberGuessed++;
                 painter.setPoints(painter.getPoints()+guesserPointsRemaining);
                 painter.setCorrectGuess();
                 
                 //Send the updated information to the other modules
                 ObjectNode guesserJson =  Json.newObject();
                 guesserJson.put("type", "guesser");
                 guesserJson.put("name",guesser);
                 guesserJson.put("points",guesserPointsRemaining);
                 GameEvent eventGuesser = new GameEvent(guesser,roomChannel,"guesserPoints");
                 eventGuesser.setObject(guesserJson);
                 GameBus.getInstance().publish(eventGuesser);
                 
                 //Send also the image to be shown
                 eventGuesser = new GameEvent(guesser,roomChannel,"guessedObject");
                 eventGuesser.setObject(guessObject);
                 GameBus.getInstance().publish(eventGuesser);
                 
                 
                if(guesserPointsRemaining>5)
                {
                    sketcherPointsRemaining= (guesserPointsRemaining==10) ? 10 : 1;
                    sketcherPainter.setPoints(sketcherPainter.getPoints()+sketcherPointsRemaining);
                    
                    //Send the updated information to the other modules
                    ObjectNode sketcherJson =  Json.newObject();
                    sketcherJson.put("type", "sketcher");
                    sketcherJson.put("name",sketcherPainter.name);
                    sketcherJson.put("points",sketcherPointsRemaining);
                    GameEvent eventSketcher = new GameEvent(sketcherPainter.name,roomChannel,"sketcherPoints");
                    eventSketcher.setObject(sketcherJson);
                    GameBus.getInstance().publish(eventSketcher);
                }
                if(guesserPointsRemaining>=5)
                    guesserPointsRemaining--;
             }
            if(!guessedWord)
            {
                //Send the time change once just a player has guessed
                GameEvent timeEvent = new GameEvent("",roomChannel,"timerChange");
                timeEvent.setObject(timerChange(remainingTimeOnGuess));
                GameBus.getInstance().publish(timeEvent);   
            }
        }
        if(numberGuessed==(playersVect.size()-1))
        {
        //Send the message to change the time for everyone to end the game
                GameEvent timeEvent = new GameEvent("",roomChannel,"timerChange");
                timeEvent.setObject(timerChange(remainingTimeOnAllGuess));
                GameBus.getInstance().publish(timeEvent);
        }
        guessedWord=true;     
    }
    
    public void gameEnded()
    {
            SystemMessage endRound = new SystemMessage(Messages.get("end"),roomChannel);
             //Manage round end:send the message to the chat
            GameBus.getInstance().publish(endRound);
            //Prepares the leaderboard of the players based on their points
            //and send it to all the interested modules
            JsonNodeFactory factory = JsonNodeFactory.instance;
            ObjectNode leaderboard = new ObjectNode(factory);
            leaderboard.put("type", "leaderboard");
            leaderboard.put("playersNumber", playersVect.size());
            //[TODO] order the list
            Collections.sort(playersVect);
            ArrayNode playersOrder=new ArrayNode(factory);
            for (Painter painter : playersVect) {
                ObjectNode row = new ObjectNode(factory);
                row.put("name", painter.name);
                row.put("points", painter.getPoints());
                playersOrder.add(row);
            }
            leaderboard.put("playerList",playersOrder);
            GameEvent endEvent = new GameEvent("", roomChannel, "leaderboard");
            endEvent.setObject(leaderboard);
            GameBus.getInstance().publish(endEvent);
    }
	
    
    
    
    public ObjectNode retrieveCurrentTaskImage()
    {
         return guessObject;
    }
    
    
    
    public ObjectNode timerChange(int remainingTime)
    {
        ObjectNode timeChange = Json.newObject();
        timeChange.put("type", "timeChange");
        timeChange.put("amount",remainingTime);
        return timeChange;
    }
    
     
     public void showImages()
	{
               GameEvent showImages = new GameEvent("",roomChannel,"showImages");
               showImages.setObject(guessObject);
               GameBus.getInstance().publish(showImages);
               
               showImages = new GameEvent("",roomChannel,"showImages");
               ObjectNode show =  Json.newObject();
               show.put("type", "showImages");
               show.put("seconds",5);
               showImages.setObject(show);
               GameBus.getInstance().publish(showImages);	
	}
    
    
     
    /**Stub function to save the task objects in the system
      WE ARE WAITING FOR THE CMS TO BE CREATED, THAT'S WHY FOR NOW THERE IS NO 
     NEED FOR COMPLEX FUNCTIONS
    **/
    public final void taskSetInitialization()
    {
       //First item in the task set
       ObjectNode guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","gonna");
       guessWord.put("image","/assets/taskImages/skirt.png");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","pantaloni");
       guessWord.put("image","/assets/taskImages/trousers.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","pantaloni");
       guessWord.put("image","/assets/taskImages/trousers2.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","poncho");
       guessWord.put("image","/assets/taskImages/poncho.jpg");
       taskHashSet.add(guessWord); 
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cintura");
       guessWord.put("image","/assets/taskImages/belt.jpg");
       taskHashSet.add(guessWord); 
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cintura");
       guessWord.put("image","/assets/taskImages/belt2.jpg");
       taskHashSet.add(guessWord); 
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","giacca");
       guessWord.put("image","/assets/taskImages/coat.jpg");
       taskHashSet.add(guessWord); 
	   
       /*
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","maschera");
       guessWord.put("image","/assets/taskImages/gasMask.jpg");
       taskHashSet.add(guessWord);*/
	   
	   
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","scarpe");
       guessWord.put("image","/assets/taskImages/shoes.jpg");
       taskHashSet.add(guessWord);  
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","scarpe");
       guessWord.put("image","/assets/taskImages/shoes2.jpg");
       taskHashSet.add(guessWord);  
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappello");
       guessWord.put("image","/assets/taskImages/hat.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","reggiseno");
       guessWord.put("image","/assets/taskImages/bra.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","sciarpa");
       guessWord.put("image","/assets/taskImages/scarf.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","stivali");
       guessWord.put("image","/assets/taskImages/boots.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","t-shirt");
       guessWord.put("image","/assets/taskImages/shirt.jpg");
       taskHashSet.add(guessWord);
	   
	   /*
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","tankini");
       guessWord.put("image","/assets/taskImages/tankini.jpg");
       taskHashSet.add(guessWord);*/
	   
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","pantaloncini");
       guessWord.put("image","/assets/taskImages/trunks.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","calze");
       guessWord.put("image","/assets/taskImages/socks.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","fazzoletto");
       guessWord.put("image","/assets/taskImages/handkerchief.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","unghie");
       guessWord.put("image","/assets/taskImages/nails.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","ombrello");
       guessWord.put("image","/assets/taskImages/umbrella.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cravatta");
       guessWord.put("image","/assets/taskImages/tie.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","occhiali");
       guessWord.put("image","/assets/taskImages/sunglasses.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","guanti");
       guessWord.put("image","/assets/taskImages/gloves.jpg");
	   taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","guanti");
       guessWord.put("image","/assets/taskImages/gloves2.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","guanti");
       guessWord.put("image","/assets/taskImages/gloves3.jpg");
       taskHashSet.add(guessWord);
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","zaino");
       guessWord.put("image","/assets/taskImages/backpack.jpg");
	   taskHashSet.add(guessWord);
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","sciarpa");
       guessWord.put("image","/assets/taskImages/scarf2.jpg");
	   taskHashSet.add(guessWord);
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","gonna");
       guessWord.put("image","/assets/taskImages/skirt.jpg");
	   taskHashSet.add(guessWord);
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","grembiule");
       guessWord.put("image","/assets/taskImages/grembiule.jpg");
	   taskHashSet.add(guessWord);
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","maschera");
       guessWord.put("image","/assets/taskImages/mask.jpg");
	   taskHashSet.add(guessWord);
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","maschera");
       guessWord.put("image","/assets/taskImages/mask2.jpg");
	   taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","borsa");
       guessWord.put("image","/assets/taskImages/handbag.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","orologio");
       guessWord.put("image","/assets/taskImages/clock.jpg");
       taskHashSet.add(guessWord);
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","pantaloncini");
       guessWord.put("image","/assets/taskImages/shorts.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappuccio");
       guessWord.put("image","/assets/taskImages/1.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","borsa");
       guessWord.put("image","/assets/taskImages/2.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","sciarpa");
       guessWord.put("image","/assets/taskImages/3.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","scarpe");
       guessWord.put("image","/assets/taskImages/4.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","minigonna");
       guessWord.put("image","/assets/taskImages/5.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cravatta");
       guessWord.put("image","/assets/taskImages/6.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappuccio");
       guessWord.put("image","/assets/taskImages/7.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","corona");
       guessWord.put("image","/assets/taskImages/8.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","stivaletti");
       guessWord.put("image","/assets/taskImages/9.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappello");
       guessWord.put("image","/assets/taskImages/11.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","reggiseno");
       guessWord.put("image","/assets/taskImages/12.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","reggiseno");
       guessWord.put("image","/assets/taskImages/13.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","fiocco");
       guessWord.put("image","/assets/taskImages/14.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappello");
       guessWord.put("image","/assets/taskImages/15.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","guanti");
       guessWord.put("image","/assets/taskImages/16.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","calze");
       guessWord.put("image","/assets/taskImages/17.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","slip");
       guessWord.put("image","/assets/taskImages/18.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","slip");
       guessWord.put("image","/assets/taskImages/19.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","fiocco");
       guessWord.put("image","/assets/taskImages/20.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","maschera");
       guessWord.put("image","/assets/taskImages/21.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cuffia");
       guessWord.put("image","/assets/taskImages/22.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","corno");
       guessWord.put("image","/assets/taskImages/23.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","maschera");
       guessWord.put("image","/assets/taskImages/25.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","poncho");
       guessWord.put("image","/assets/taskImages/26.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","ombrello");
       guessWord.put("image","/assets/taskImages/27.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","gonna");
       guessWord.put("image","/assets/taskImages/28.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","fiocco");
       guessWord.put("image","/assets/taskImages/29.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappello");
       guessWord.put("image","/assets/taskImages/30.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","minigonna");
       guessWord.put("image","/assets/taskImages/31.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappotto");
       guessWord.put("image","/assets/taskImages/32.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cintura");
       guessWord.put("image","/assets/taskImages/33.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappotto");
       guessWord.put("image","/assets/taskImages/34.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cerchietto");
       guessWord.put("image","/assets/taskImages/35.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","t-shirt");
       guessWord.put("image","/assets/taskImages/36.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappuccio");
       guessWord.put("image","/assets/taskImages/37.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappello");
       guessWord.put("image","/assets/taskImages/38.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","occhiali");
       guessWord.put("image","/assets/taskImages/40.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappello");
       guessWord.put("image","/assets/taskImages/41.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","fiocco");
       guessWord.put("image","/assets/taskImages/42.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","orologio");
       guessWord.put("image","/assets/taskImages/43.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cappello");
       guessWord.put("image","/assets/taskImages/44.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cravatta");
       guessWord.put("image","/assets/taskImages/45.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","sciarpa");
       guessWord.put("image","/assets/taskImages/46.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","anello");
       guessWord.put("image","/assets/taskImages/47.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","anello");
       guessWord.put("image","/assets/taskImages/48.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","orecchini");
       guessWord.put("image","/assets/taskImages/49.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","orecchini");
       guessWord.put("image","/assets/taskImages/50.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cravatta");
       guessWord.put("image","/assets/taskImages/51.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","cravatta");
       guessWord.put("image","/assets/taskImages/52.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","orologio");
       guessWord.put("image","/assets/taskImages/53.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","orologio");
       guessWord.put("image","/assets/taskImages/54.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","orologio");
       guessWord.put("image","/assets/taskImages/55.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","corona");
       guessWord.put("image","/assets/taskImages/56.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","corona");
       guessWord.put("image","/assets/taskImages/57.jpg");
       taskHashSet.add(guessWord);
	   
	   guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","borsa");
       guessWord.put("image","/assets/taskImages/58.jpg");
       taskHashSet.add(guessWord);
    }

}
