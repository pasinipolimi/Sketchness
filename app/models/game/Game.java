package models.game;


import play.libs.*;
import play.libs.F.*;
import play.i18n.Messages;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import play.Logger;

import org.codehaus.jackson.node.*;
import org.codehaus.jackson.*;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import models.Painter;
import models.factory.GameRoom;
import play.Play;
import scala.concurrent.duration.Duration;

import utils.*;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages.*;
import utils.LanguagePicker;
import utils.gamebus.GameEventType;
import utils.gamemanager.GameManager;


/**
 * A chat room is an Actor.
 */
public class Game extends GameRoom {

    private final Integer remainingTimeOnGuess=Integer.parseInt(Play.application().configuration().getString("remainingTimeOnGuess"));  //Once a player has guessed, change the time for everyone to 20 seconds
    private final Integer remainingTimeOnAllGuess=Integer.parseInt(Play.application().configuration().getString("remainingTimeOnAllGuess")); //If all have guessed, reduce the timer to 3 seconds
    private final Integer maxSketcherPointsRemaining = Integer.parseInt(Play.application().configuration().getString("maxSketcherPointsRemaining"));
    private final Integer minSketcherPointsRemaining = Integer.parseInt(Play.application().configuration().getString("minSketcherPointsRemaining")); 
    private final Integer maxGuesserPointsRemaining = Integer.parseInt(Play.application().configuration().getString("maxGuesserPointsRemaining"));
    private final Integer minGuesserPointsRemaining = Integer.parseInt(Play.application().configuration().getString("minGuesserPointsRemaining"));
    private final Integer maxRound=Integer.parseInt(Play.application().configuration().getString("maxRounds"));  //Maximum number of rounds
    private final Integer requiredPlayers=Integer.parseInt(Play.application().configuration().getString("requiredPlayers"));
    private Integer maxPlayers=requiredPlayers;
    //Minimum tags that an image should have to avoid asking to the users for new tags
    private final Integer minimumTags=Integer.parseInt(Play.application().configuration().getString("minimumTags"));
    //Url of the CMS system
    private final String rootUrl=Play.application().configuration().getString("cmsUrl");
    
    //Variables used to manage the rounds
    private Boolean guessedWord=false; //Has the word been guessed for the current round?
    private Boolean gameStarted=false;
    private Boolean areWeAsking=false;
    private Integer roundNumber=0;  //Starts with round number 1
    private Integer sketcherPointsRemaining=0;  //The number of points available to the sketcher: for the first reply he gets 5, for the second 4 and so on
    private Integer guesserPointsRemaining=maxGuesserPointsRemaining;  //The number of points available to the guessers:the first get 10, the second 9 and so on
    private Integer numberGuessed=0;   //Number of players that have guessed for a specific round
    private Painter sketcherPainter;  //The current sketcher
    
    //System variables
    private Integer modules = 2; //Number of modules available in the game (chat/paint)
    private Room  roomChannel;  //Name of the room
    private Boolean taskAcquired=false;
    // Members of this room.
    private CopyOnWriteArrayList<Painter> playersVect = new CopyOnWriteArrayList<>();
    
    //Control Variables
    private ObjectNode guessObject;
    private Integer missingPlayers=requiredPlayers;
    private Integer disconnectedPlayers=0;
    private Boolean shownImages=false;
    private HashSet<ObjectNode> taskHashSet = new HashSet<>();
    private ObjectNode taskImage;

    public Game() {
        super(Game.class);
    }
    
    /*
     * Handles all the messages sent to this actor
     */
    @Override
    public void onReceive(Object message) throws Exception {
        
        if(message instanceof Room)
        {
            this.roomChannel=((Room)message);
            maxPlayers=requiredPlayers;
            newGameSetup();
            Logger.info("[GAME] "+roomChannel.getRoom()+" created.");
        }
        if(message instanceof GameEvent)
        {
            GameEvent event= (GameEvent)message;
            switch(event.getType())
            {
                case join:playerJoin(event.getMessage());publishLobbyEvent(GameEventType.gameInfo);break;
                case quit:handleQuitter(event.getMessage());publishLobbyEvent(GameEventType.gameInfo);break;
                case guessed:guessed(event.getMessage());break;
                case timeExpired: playerTimeExpired(event.getMessage());break;
                case finalTraces: sendFinalTraces(event.getObject());break;
                case tag: taskImage.remove("tag");taskImage.put("tag",event.getMessage());sendTask(false);break;
                case skipTask: skipTask(event.getMessage());break;
                case getGameInfo: publishLobbyEvent(GameEventType.gameInfo);break;
            }
        }
    }
    
    
    
    /*
     * Retrieves one of the images that has been stored to be segmented at random
     * being careful not to retrieve the same image two times for the same match.
     * <p>
     * @return The object related to the task: image + tag
     */
    private ObjectNode retrieveTaskImage()
    {
         guessObject=null;
         int size = taskHashSet.size();
         Integer item=null;
         do {
           try {
              item = new Random().nextInt(size);
           }
           catch(IllegalArgumentException ex) {
              item=null;
              Logger.error("[GAME] Failed to retrieve Task Image, retrying.");
           }
         }while (item==null);
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
     private String nextSketcher()
     {
         sketcherPainter=null;
         int currentPlayers=requiredPlayers-disconnectedPlayers;
         int count=0;
         //Publish system messages to inform that a new round is starting and the roles are being chosen
         GameBus.getInstance().publish(new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),"newround"), roomChannel));
         GameBus.getInstance().publish(new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),"choosingroles"), roomChannel));

         //Set all the players as GUESSERS at the beginning
         for(int i=0;i<currentPlayers;i++)
         {
            playersVect.get(i).role="GUESSER";
         }
         
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
                        sketcherPainter.role="SKETCHER";
                        sketcherPainter.hasBeenSketcher=true;
                }
                count++;
            }
         }
         //Publish a system message to inform the other players on who is the sketcher
         GameBus.getInstance().publish(new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),"thesketcheris")+" "+sketcherPainter.name, roomChannel));
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
    private void checkStart() throws Exception
    {
                if(!triggerStart())
                //Send a message to inform about the missing players
                {
                    int nPlayers=playersVect.size();
                    if(requiredPlayers-nPlayers>1)
                        GameBus.getInstance().publish(new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),"waitingfor")+" "+(requiredPlayers-nPlayers)+" "+Messages.get(LanguagePicker.retrieveLocale(),"playerstostart"), roomChannel));
                    else if (requiredPlayers-nPlayers==1)
                        GameBus.getInstance().publish(new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),"waitingfor")+" "+(requiredPlayers-nPlayers)+" "+Messages.get(LanguagePicker.retrieveLocale(),"playertostart"), roomChannel));
                }
    }
    
    private boolean triggerStart() throws Error
    {
                boolean canStart=true;
                for (Painter painter : playersVect) {
                   if(painter.getnModulesReceived()<modules)
                       canStart=false;
                }
                //We need to wait for all the modules to receive the player list
                if(canStart&&playersVect.size()>=requiredPlayers)
                {
                    GameManager.getInstance().removeInstance(getSelf());
                    publishLobbyEvent(GameEventType.gameStarted);
                    if(taskAcquired)
                    {
                        disconnectedPlayers=0;
                        roundNumber=0;
                	gameStarted=true;
                        nextRound();
                        //We start the game
                        if(sketcherPainter!=null) {
                            GameBus.getInstance().publish(new GameEvent(roomChannel,GameEventType.gameStarted));
                            
                        }
                        else
                            throw new Error("[GAME]: Cannot find a suitable Sketcher!");
                    }
                    else
                    {
                         GameBus.getInstance().publish(new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),"acquiring"), roomChannel));
                    }
                    return true;
                }
                return false;
    }
    
    
    
     /*
     * Start a new round of the game
     */
     private void nextRound()
     {
         roundNumber++;
         if(roundNumber<=maxRound)
         {
           //Reset the points and status counters
           guesserPointsRemaining=maxGuesserPointsRemaining;
           sketcherPointsRemaining=0;
           numberGuessed=0;
           guessedWord=false;
           //Nobody has guessed for this round
           for (Painter reset : playersVect) {
                 reset.guessed=false;
           }
           nextSketcher();
           //Check if a tag for the current image as already been provided;if not, ask for a new one
           taskImage = retrieveTaskImage();
           String label=taskImage.get("tag").asText();
           if(label.equals(""))
               //We need to ask for a new tag
               sendTask(true);
           else
               //We have already a tag that has been provided, use that one
               sendTask(false);
         }
         //We have played all the rounds for the game, inform the users and the modules
         //that the match has ended
         else
         {
             gameEnded();
         } 
     }
     
     private void sendTask(Boolean ask) {
         if(ask) {
            areWeAsking=true;
            GameEvent task = new GameEvent(sketcherPainter.name,roomChannel,GameEventType.askTag);
            task.setObject(taskImage);
            GameBus.getInstance().publish(task);
         }
         else {
            areWeAsking=false;
            GameBus.getInstance().publish(new GameEvent(sketcherPainter.name,roomChannel,GameEventType.nextRound));
            GameEvent task = new GameEvent(sketcherPainter.name,roomChannel,GameEventType.task);
            task.setObject(taskImage);
            GameBus.getInstance().publish(task);
         }
     }
	 
     

     
     /*
     * Check if the timer for all the players has expired, show the solution for the current
     * round and start a new one
     */
     private void playerTimeExpired(String name) {
         //If all the players have disconnected during a game, start a new one
         if(((requiredPlayers-disconnectedPlayers)<=1)&&gameStarted)
         {
             //Restart the game
             gameEnded();
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
                //Before calling the new round, show the solution to all the players and end the round
		if(shownImages==false&&areWeAsking==false)
		{
		    showImages();
		    shownImages=true;
		    missingPlayers=requiredPlayers;
                    //If at least one player has guessed, it means that the drawn contour is a good one
                    if(guessedWord)
                        GameBus.getInstance().publish(new GameEvent(roomChannel, GameEventType.saveTraces));
		}
                //If the solution has been given or a tag has not been chosen, start a new round
		else
		{
                  if(areWeAsking)
                       GameBus.getInstance().publish(new SystemMessage(sketcherPainter.name+" "+Messages.get(LanguagePicker.retrieveLocale(),"notag"), roomChannel));
		  areWeAsking=false;
                  //Start a new round
                  nextRound();
		  shownImages=false;
		  missingPlayers=requiredPlayers;
		}
            }
         }
     }
     
     /*
      * Initialization of the variables to start a new game
     */
     public void newGameSetup() {
         disconnectedPlayers=0;
         roundNumber=0;
         gameStarted=false;
         playersVect =  new CopyOnWriteArrayList<>();
         Akka.system().scheduler().scheduleOnce(
            Duration.create(1, TimeUnit.MILLISECONDS),
            new Runnable() {
              @Override
              public void run() {
                  try {
                      taskSetInitialization();
                  } catch (Exception ex) {
                      ex.printStackTrace();
                  }
              }
            },
            Akka.system().dispatcher()
          ); 
         Logger.debug("[GAME] New game started");
     }
    
    
    
    
    private void playerJoin(String username) throws Exception {
        Logger.debug("[GAME]: Player Joined");
        int count=0;
        for (Painter painter : playersVect) {
            if(painter.name.equalsIgnoreCase(username)){
                painter.setnModulesReceived(painter.getnModulesReceived()+1);
                count=painter.getnModulesReceived();
                Logger.debug("[GAME]: player "+username+" counted "+count);
                break;
            }
       }
       if(count==0)
       {             
           Painter painter=new Painter(username, false);
           painter.setnModulesReceived(1);
            //Add the new entered player, it has never been a sketcher in this game (false)
            playersVect.add(painter);
            publishLobbyEvent(GameEventType.join);
            Logger.info("[GAME]: added player "+playersVect.get(playersVect.size()-1).name);
            //Check if we can start the game and, in such a case, start it
       }
       //Wait to see if all the modules have received the login information from the players
       else if (modules>1 ? count>(modules-1) : count>1)
       {
           Logger.info("[GAME]: Check Start");
           checkStart();
       }
    }
    
    private void publishLobbyEvent(GameEventType type)
    {
        GameEvent join = new GameEvent(type.toString(),GameManager.getInstance().getLobby(),type);
        ObjectNode status = new ObjectNode(JsonNodeFactory.instance);
        //Get the hashcode related to this actoref in order to make it unique
        status.put("id", this.getSelf().hashCode());
        status.put("roomName", roomChannel.getRoom());
        status.put("currentPlayers",playersVect.size());
        status.put("maxPlayers",maxPlayers);
        status.put("visible", playersVect.size()<maxPlayers);
        join.setObject(status);
        GameBus.getInstance().publish(join);
    }
    
    
    private void sendFinalTraces(ObjectNode finalTraces) throws MalformedURLException, IOException {
        String id = finalTraces.get("id").getTextValue();
        String label = finalTraces.get("label").getTextValue();
        String traces = finalTraces.get("traces").toString();
        String history = finalTraces.get("history").toString();
        
        String urlParameters = "label="+label+"&coordinates="+traces+"&history="+history+"&user_id="+sketcherPainter.name+"&language="+LanguagePicker.retrieveIsoCode()+"&session_id=2251";
        String request = rootUrl+"/wsmc/image/"+id+"/segment";
        
        WS.url(request).setContentType("application/x-www-form-urlencoded").post(urlParameters);
        
        urlParameters ="tag="+label+"&user_id="+sketcherPainter.name+"&language="+LanguagePicker.retrieveIsoCode()+"&session_id=2251";
        request = rootUrl+"/wsmc/image/"+id+"/tag";
        WS.url(request).setContentType("application/x-www-form-urlencoded").post(urlParameters);
    }
    
    private void handleQuitter(String quitter) {
            for (Painter painter : playersVect) {
                if(painter.name.equalsIgnoreCase(quitter)){
                    //Wait for all the modules to announce that the player has disconnected
                    painter.setnModulesReceived(painter.getnModulesReceived()-1);
                    if(painter.getnModulesReceived()==0)
                    {
                        playersVect.remove(painter);
                        disconnectedPlayers++;
                        GameBus.getInstance().publish(new GameEvent("quit",GameManager.getInstance().getLobby(),GameEventType.quit));
                        //End the game if there's just one player or less
                        if(((requiredPlayers-disconnectedPlayers)==1)&&gameStarted)
                            //Restart the game
                            gameEnded(); 
                        else if (((requiredPlayers-disconnectedPlayers)<=0)&&gameStarted)
                            publishLobbyEvent(GameEventType.gameEnded);
                    }
                }
            } 
    }
    
    private void guessed(String guesser) {
        for (Painter painter : playersVect) {
             //If the current painter is the guesser, has not guessed before and it is not the sketcher, update his points
             if(painter.name.equals(guesser)&&painter.guessed==false)
             {
                 numberGuessed++;
                 painter.setPoints(painter.getPoints()+guesserPointsRemaining);
                 painter.setCorrectGuess();
                 
                 //Send the updated information to the other modules
                 ObjectNode guesserJson =  Json.newObject();
                 guesserJson.put("type", "points");
                 guesserJson.put("name",guesser);
                 guesserJson.put("points",guesserPointsRemaining);
                 GameEvent eventGuesser = new GameEvent(guesser,roomChannel,GameEventType.points);
                 eventGuesser.setObject(guesserJson);
                 GameBus.getInstance().publish(eventGuesser);
                 
                 //Send also the image to be shown
                 eventGuesser = new GameEvent(guesser,roomChannel,GameEventType.guessedObject);
                 eventGuesser.setObject(guessObject);
                 GameBus.getInstance().publish(eventGuesser);

                if(guesserPointsRemaining>minGuesserPointsRemaining)
                {
                    //Assign the points to the sketcher. Has someone guessed for this round? If not, assign maximum points, if so assign the minimum for each guess
                    sketcherPointsRemaining = (guesserPointsRemaining==maxGuesserPointsRemaining) ? maxSketcherPointsRemaining : minSketcherPointsRemaining;
                    sketcherPainter.setPoints(sketcherPainter.getPoints()+sketcherPointsRemaining);
                    
                    //Send the updated information to the other modules
                    ObjectNode sketcherJson =  Json.newObject();
                    sketcherJson.put("type", "points");
                    sketcherJson.put("name",sketcherPainter.name);
                    sketcherJson.put("points",sketcherPointsRemaining);
                    GameEvent eventSketcher = new GameEvent(sketcherPainter.name,roomChannel,GameEventType.points);
                    eventSketcher.setObject(sketcherJson);
                    GameBus.getInstance().publish(eventSketcher);
                }
                if(guesserPointsRemaining>=minGuesserPointsRemaining)
                    guesserPointsRemaining--;
             }
            if(!guessedWord)
            {
                //Send the time change once just a player has guessed
                GameEvent timeEvent = new GameEvent(roomChannel,GameEventType.timerChange);
                timeEvent.setObject(timerChange(remainingTimeOnGuess, CountdownTypes.round));
                GameBus.getInstance().publish(timeEvent);   
            }
        }
        if(numberGuessed==(playersVect.size()-1))
        {
                //Send the message to change the time for everyone to end the round
                GameEvent timeEvent = new GameEvent(roomChannel,GameEventType.timerChange);
                timeEvent.setObject(timerChange(remainingTimeOnAllGuess, CountdownTypes.round));
                GameBus.getInstance().publish(timeEvent);
        }
        guessedWord=true;     
    }
    
    private void gameEnded() {
            GameEvent endEvent = new GameEvent(roomChannel, GameEventType.leaderboard);
            endEvent.setObject(compileLeaderboard());
            GameBus.getInstance().publish(endEvent);
            GameBus.getInstance().publish(new GameEvent(roomChannel, GameEventType.gameEnded));
            publishLobbyEvent(GameEventType.gameEnded);
            killActor(); 
   }
    
    
    //Prepares the leaderboard of the players based on their points
    private ObjectNode compileLeaderboard()
    {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode leaderboard = new ObjectNode(factory);
        leaderboard.put("type", "leaderboard");
        leaderboard.put("playersNumber", playersVect.size());
        Painter[] sorted = playersVect.toArray(new Painter[0]);
        Arrays.sort(sorted);
        ArrayNode playersOrder=new ArrayNode(factory);
        for (Painter painter : sorted) {
            ObjectNode row = new ObjectNode(factory);
            row.put("name", painter.name);
            row.put("points", painter.getPoints());
            playersOrder.add(row);
        }
        leaderboard.put("playerList",playersOrder);
        return leaderboard;
    }
	
    private ObjectNode timerChange(int remainingTime, CountdownTypes timeObject) {
        ObjectNode timeChange = Json.newObject();
        timeChange.put("type", "timeChange");
        timeChange.put("amount",remainingTime);
        timeChange.put("timeObject",timeObject.name());
        return timeChange;
    }
    
    private void skipTask(String kind)
    {
        GameBus.getInstance().publish(new SystemMessage(sketcherPainter.name+" "+Messages.get(LanguagePicker.retrieveLocale(),"skiptask"), roomChannel));
        GameEvent timeEvent = new GameEvent(roomChannel,GameEventType.timerChange);
        timeEvent.setObject(timerChange(0,CountdownTypes.valueOf(kind)));
        GameBus.getInstance().publish(timeEvent);   
    }
    
     
    private void showImages() {
       GameEvent showImages =  new GameEvent(roomChannel,GameEventType.showImages);
       ObjectNode show =  Json.newObject();
       show.put("type", "showImages");
       show.put("seconds",5);
       showImages.setObject(show);
       GameBus.getInstance().publish(showImages);
       //Send also the image to be shown
       for (Painter painter : playersVect) {
           if(!painter.role.equals("SKETCHER")&&painter.guessed==false) { 
             GameEvent eventGuesser = new GameEvent(painter.name,roomChannel,GameEventType.guessedObject);
             eventGuesser.setObject(guessObject);
             GameBus.getInstance().publish(eventGuesser);
           }
       }
       
    }
    
    /**
     * Retrieving data from the CMS [TODO] Right now we are not retrieving based on the requirements of our tasks
     * such as completing tasks that have not been already faced and so on. We will add this feature in the future.
    **/
    public void taskSetInitialization() throws Error{
        
       taskAcquired=false;
       JsonReader jsonReader= new JsonReader();
       JsonNode retrieved=null;
       //[TODO] Fail safe in case of not being able to retrieve the instances
       try{
            retrieved= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image.json");
       }
       catch(IllegalArgumentException e)
       {
           throw new RuntimeException("The request to the CMS is malformed");
       }
       if(retrieved!=null)
       {
        for (JsonNode item : retrieved) {
            if(item.getElements().hasNext())
            {
                item=item.get("image");
                String id=item.get("imgage_id").asText();
                String url=rootUrl+item.get("image_uri").asText();
                Integer width = item.get("imgage_width").asInt();
                Integer height = item.get("imgage_height").asInt();
                
                JsonNode imageSegments= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image/"+id+"/segment.json");

                String label="";
                ObjectNode guessWord = Json.newObject();
                guessWord.put("type", "task");
                guessWord.put("id", id);
                if(imageSegments.size()>0 && imageSegments.getElements().hasNext())
                {
                    JsonNode tags= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image/"+id+"/tag.json");
                    if(tags.size()>=minimumTags && tags.getElements().hasNext())
                    {
                        JsonNode retrievedTags=tags.get(new Random().nextInt(tags.size()));
                        retrievedTags=retrievedTags.get("image").get("tag");
                        JsonNode retrievedTag = retrievedTags.get(new Random().nextInt(retrievedTags.size())).get("tag");
                        if(retrievedTag.get("language").get("language_iso_code").asText().equals(LanguagePicker.retrieveIsoCode()))
                            label=retrievedTag.get("tag_name").asText();
                    }
                }
                guessWord.put("tag",label);
                guessWord.put("lang",LanguagePicker.retrieveIsoCode());
                guessWord.put("image",url);
                guessWord.put("width",width);
                guessWord.put("height",height);
                taskHashSet.add(guessWord);
                if(!taskAcquired)
                {
                    taskAcquired = true;
                    triggerStart();
                }
            }
        }  
    }
    else
           throw new Error("Cannot retrieve the tasks from the CMS.");
    }
    
    public Boolean isFull()
    {
        return playersVect.size()>=maxPlayers;
    }
}
  enum CountdownTypes
    {
        round,tag
    }


