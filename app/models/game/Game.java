package models.game;


import play.libs.F.*;


import akka.actor.*;
import java.io.IOException;
import java.net.MalformedURLException;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import utils.Messages.*;
import models.Painter;
import org.codehaus.jackson.JsonNode;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.PlayerJoin;
import utils.gamebus.GameMessages.SystemMessage;


import play.i18n.Messages;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONException;


import play.Logger;
import play.libs.Akka;
import play.libs.Json;
import play.libs.WS;
import scala.concurrent.duration.Duration;
import utils.JsonReader;
import utils.LanguagePicker;

/**
 * A chat room is an Actor.
 */
public class Game extends UntypedActor {
    
    private final String rootUrl="http://webservices.comoconnection.com/";
    //Minimum tags that an image should have to avoid asking to the users for new tags
    private final int minimumTags=3;

    //[TODO] Should be declared as config variables
    //Variables to manage the point system in the game
    private Boolean guessedWord=false; //Has the word been guessed for the current round?
    private Integer remainingTimeOnGuess=20;  //Once a player has guessed, change the time for everyone to 20 seconds
    private Integer remainingTimeOnAllGuess=3; //If all have guessed, reduce the timer to 3 seconds
    private int guesserPointsRemaining=10;  //The number of points available to the guessers:the first get 10, the second 9 and so on
    private int sketcherPointsRemaining=0;  //The number of points available to the sketcher: for the first reply he gets 5, for the second 4 and so on
    
    
    //Variables used to manage the rounds
    private boolean gameStarted=false;
    private int roundNumber=0;  //Starts with round number 1
    private static int maxRound=6;  //Maximum number of rounds
    private int numberGuessed=0;   //Number of players that have guessed for a specific round
    private Painter sketcherPainter;  //The current sketcher
    
    //System variables
    private int modules = 2; //Number of modules available in the game (chat/paint)
    String  roomChannel;  //Name of the room
    
    boolean taskAcquired=false;
    
    //Control Variables
    private ObjectNode guessObject;
    private static final int requiredPlayers=3;
    private static int missingPlayers=requiredPlayers;
    private int disconnectedPlayers=0;
    
    private static Boolean shownImages=false;
        
    private HashSet<ObjectNode> taskHashSet = new HashSet<>();
    
    private ObjectNode taskImage;
    
    // Members of this room.
    CopyOnWriteArrayList<Painter> playersVect = new CopyOnWriteArrayList<>();

    
    
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
         GameBus.getInstance().publish(new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),"newround"), roomChannel));
         GameBus.getInstance().publish(new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),"choosingroles"), roomChannel));

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
    public void checkStart() throws Exception
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
    
    public boolean triggerStart() throws Exception
    {
                boolean canStart=true;
                for (Painter painter : playersVect) {
                   if(painter.getnModulesReceived()<modules)
                       canStart=false;
                }
                //We need to wait for all the modules to receive the player list
                if(canStart&&playersVect.size()>=requiredPlayers)
                {
                    if(taskAcquired)
                    {
                        disconnectedPlayers=0;
                        roundNumber=0;
                	gameStarted=true;
                        //We start the game
                        nextRound();
                        if(sketcherPainter!=null)
                            GameBus.getInstance().publish(new GameEvent(sketcherPainter.name, roomChannel,"gameStart"));
                        else
                            throw new Exception("[Error][Game]: Cannot find a suitable Sketcher!");
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
           //We send the right task to the right sketcher
           
           //Check if a tag for the current image as already been provided;if not, ask for a new one
           taskImage = retrieveTaskImage();
           String label=taskImage.get("word").asText();
           if(label.equals(""))
           {
               GameEvent task = new GameEvent(sketcherPainter.name,roomChannel,"askTag");
               task.setObject(taskImage);
               GameBus.getInstance().publish(task);
           }
           else
           {
                sendTask();
           }
         }
         //We have played all the rounds for the game, inform the users and the modules
         //that the match has ended
         else
         {
             gameEnded();
             newGameSetup();
         } 
     }
     
     public void sendTask()
     {
         GameBus.getInstance().publish(new GameEvent(sketcherPainter.name,roomChannel,"nextRound"));
         GameEvent task = new GameEvent(sketcherPainter.name,roomChannel,"task");
         task.setObject(taskImage);
         GameBus.getInstance().publish(task);
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
         roundNumber=0;
         gameStarted=false;
         playersVect =  new CopyOnWriteArrayList<>();
         GameBus.getInstance().publish(new GameEvent("",roomChannel,"newGame"));
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
    
    
    /*
     * Handles all the messages sent to this actor
     */
    @Override
    public void onReceive(Object message) throws Exception {
        
        if(message instanceof Room)
        {
            this.roomChannel=((Room)message).getRoom();
            newGameSetup();
            Logger.info("GAMEROOM "+roomChannel+" created.");
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
                case "quit":handleQuitter(event.getMessage());break;
                case "guessed":guessed(event.getMessage());break;
                case "timeExpired": playerTimeExpired(event.getMessage());break;
                case "finalTraces": sendFinalTraces(event.getObject());break;
                case "canStart": triggerStart();break;
                case "tag": taskImage.remove("word");taskImage.put("word",event.getMessage());sendTask();break;
            }
        }
    }
    
    
    private void sendFinalTraces(ObjectNode finalTraces) throws MalformedURLException, IOException
    {
        String id = finalTraces.get("id").getTextValue();
        String label = finalTraces.get("label").getTextValue();
        String traces = finalTraces.get("traces").toString();
        String history = finalTraces.get("history").toString();
        
        //[TODO] ADD THE LANGUAGE FOR THE TAG
        String urlParameters = "label="+label+"&coordinates="+traces+"&history="+history+"&user_id="+sketcherPainter.name+"&language="+LanguagePicker.retrieveIsoCode();
        String request = rootUrl+"/wsmc/image/"+id+"/segment";
        
        WS.url(request).setContentType("application/x-www-form-urlencoded").post(urlParameters);
    }
    
    private void handleQuitter(String quitter)
    {
            for (Painter painter : playersVect) {
                if(painter.name.equalsIgnoreCase(quitter)){
                    //Wait for all the modules to announce that the player has disconnected
                    painter.setnModulesReceived(painter.getnModulesReceived()-1);
                    if(painter.getnModulesReceived()==0)
                    {
                        playersVect.remove(painter);
                        disconnectedPlayers++;
                        //End the game if there's just one player or less
                        if(((requiredPlayers-disconnectedPlayers)==1)&&gameStarted)
                            //Restart the game
                            gameEnded();
                        else if (((requiredPlayers-disconnectedPlayers)<=0)&&gameStarted)
                            newGameSetup();
                    }
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
            GameBus.getInstance().publish(new GameEvent("", roomChannel, "gameEnded"));
            //Prepares the leaderboard of the players based on their points
            //and send it to all the interested modules
            JsonNodeFactory factory = JsonNodeFactory.instance;
            ObjectNode leaderboard = new ObjectNode(factory);
            leaderboard.put("type", "leaderboard");
            leaderboard.put("playersNumber", playersVect.size());
            //[TODO] order the list
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
    
    
     
    /**
     * Retrieving data from the CMS [TODO] Right now we are not retrieving based on the requirements of our tasks
     * such as completing tasks that have not been already faced and so on. We will add this feature in the future.
    **/
    public void taskSetInitialization() throws MalformedURLException, IOException, JSONException
    {
      
       
       taskAcquired=false;
       JsonReader jsonReader= new JsonReader();
       JsonNode retrieved= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image.json");
       
       //[TODO] NOT THE RIGHT POSITION, JUST FOR TESTING PURPOSES
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
                            label=retrievedTag.get("tag").get("tag_name").asText();
                    }
                }
                guessWord.put("word",label);
                guessWord.put("lang",LanguagePicker.retrieveIsoCode());
                guessWord.put("image",url);
                guessWord.put("width",width);
                guessWord.put("height",height);
                taskHashSet.add(guessWord);
                if(!taskAcquired)
                {
                    taskAcquired = true;
                    GameBus.getInstance().publish(new GameEvent("", roomChannel, "canStart"));
                }
            }
        }  
    }

}
