package models;

import java.util.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

public class PaintRoom {
	
    public String name;
    // The list of all connected painters (identified by ids)
    public Map<Integer, Painter> painters = new ConcurrentHashMap<Integer, Painter>();
    public AtomicInteger counter = new AtomicInteger(0);
    public AtomicInteger connections = new AtomicInteger(0);
    
    private HashSet<ObjectNode> taskHashSet = new HashSet<ObjectNode>();

    
    private String currentGuess;
    private Boolean guessedWord;
    
    private ChatRoom chatRoom=null;
    
    private int remainingTimeOnGuess=20;
    private int guesserPointsRemaining=10;
    private int sketcherPointsRemaining=0;
    private int roundNumber=1;
    private static int maxRound=6;
    private int numberGuessed=0;
   
    private Painter sketcherPainter;
    
    private ObjectNode guessObject;
    

    public PaintRoom(String name) {
		this.name = name;
                currentGuess="";
                guessedWord=false;
                taskSetInitialization();
    }
    
    
    //Stub function to save the task objects in the system
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
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","maschera");
       guessWord.put("image","/assets/taskImages/gasMask.jpg");
       taskHashSet.add(guessWord); 
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
       guessWord = Json.newObject();
       guessWord.put("type", "task");
       guessWord.put("word","tankini");
       guessWord.put("image","/assets/taskImages/tankini.jpg");
       taskHashSet.add(guessWord);
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
    }

    public void createPainter(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
        counter.incrementAndGet();
        connections.incrementAndGet();
        final int pid = counter.intValue(); // the painter id

        // in: handle messages from the painter
        in.onMessage(new F.Callback<JsonNode>() {
            @Override
            public void invoke(JsonNode json) throws Throwable {
                String type = json.get("type").getTextValue();
                // The painter wants to change some of his property
                if(type.equalsIgnoreCase("change")) {
                        Boolean found=false;
                        Painter painter = painters.get(pid);
                        if(painter == null) {
                            for(Map.Entry<Integer, Painter> entry : painters.entrySet())
                            {
                                String name=json.get("name").getTextValue();
                                String compareName=entry.getValue().getName();
                                if(compareName.equalsIgnoreCase(name))
                                {
                                    painters.get(entry.getKey()).setDisconnected(false);
                                    painters.get(entry.getKey()).getChannel().close();
                                    painters.get(entry.getKey()).setChannel(out);
                                    ObjectNode jsonTime = Json.newObject();
                                    jsonTime.put("type", "syncTime");
                                    jsonTime.put("pid", entry.getKey());
        
                                    // Inform the list of other painters
                                    for(Map.Entry<Integer, Painter> entryTime : painters.entrySet()) {
                                        if(!(entryTime.getValue().name.equalsIgnoreCase(name)))
                                            entryTime.getValue().getChannel().write(jsonTime);
                                    }

                                    found=true;
                                    break;
                                }
                            }
                            if(!found)
                            {
                                painter = new Painter(out);
                                painters.put(pid, painter);
                                 // Inform the painter who he is (which pid, he can then identify himself)
                                ObjectNode self = Json.newObject();
                                self.put("type", "youAre");
                                self.put("pid", pid);
                                self.put("role", painter.role);
                                painter.getChannel().write(self);

                                // Inform the list of other painters
                                for(Map.Entry<Integer, Painter> entry : painters.entrySet()) {
                                    ObjectNode other = (ObjectNode)entry.getValue().toJson();
                                    other.put("pid", entry.getKey());
                                    painter.getChannel().write(other);
                                }
                                }
                        }
                        painter.updateFromJson(json);
                }
                else if (type.equalsIgnoreCase("roundEnded")) {
                        chatRoom.playerTimeExpired(json.get("player").getTextValue());
                }
                else if(type.equalsIgnoreCase("syncTime"))
                {
                     Integer remaining = json.get("remaining").getIntValue();
                     Integer id=json.get("pid").getIntValue();
                     ObjectNode self = Json.newObject();
                                self.put("type", "createTimer");
                                self.put("amount", remaining);
                     painters.get(id).getChannel().write(self);
                }    
                ObjectNode node = ((ObjectNode)json);
                node.put("pid", pid);
                PaintRoom.this.notifyAll(node);
            }
        });

        // User has disconnected.
        in.onClose(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                painters.get(pid).setDisconnected(true);
                connections.decrementAndGet();

                ObjectNode json = Json.newObject();
                json.put("type", "disconnect");
                json.put("pid", pid);

                PaintRoom.this.notifyAll(json);

                Logger.debug("(pid:"+pid+") disconnected.");
                Logger.info(connections+" painter(s) currently connected.");
            }
        });

        Logger.debug("(pid:"+pid+") connected.");
        Logger.info(connections+" painter(s) currently connected.");
    }

    public void notifyAll(JsonNode json) {
        for(Painter painter : painters.values()) {
            painter.getChannel().write(json);
        }
    }
    
    public void matchStarted(String sketcher) 
    {
        roundNumber=1;
        guesserPointsRemaining=10;
        sketcherPointsRemaining=0;
        numberGuessed=0;
        guessedWord=false;
        Logger.debug("Starting match!\n");
        Logger.debug("Numero giocatori: "+painters.size());
        for(Map.Entry<Integer, Painter> entry : painters.entrySet()) {
            Logger.debug(entry.toString());
            if(entry.getValue().name.equals(sketcher))
            {
                ObjectNode roleMessage = Json.newObject();
                roleMessage.put("type", "role");
                roleMessage.put("role","SKETCHER");
                entry.getValue().getChannel().write(roleMessage);
                sketcherPainter=entry.getValue();
                
                
                //RETRIEVE THE IMAGE AND THE TAG TO SEND TO THE SKETCHER
                ObjectNode guessWord = retrieveTaskImage();
                entry.getValue().getChannel().write(guessWord);
                currentGuess=guessWord.get("word").asText();
            }
            else
            {
                ObjectNode self = Json.newObject();
                self.put("type", "role");
                self.put("role","GUESSER");
                entry.getValue().getChannel().write(self);
            }
        }
    }
    
    public String getCurrentGuess()
    {
        return currentGuess;
    }
	
	public void showImages()
	{
		for(Map.Entry<Integer, Painter> entry : painters.entrySet()) {
				ObjectNode guessWord = retrieveCurrentTaskImage();
				entry.getValue().getChannel().write(guessWord);
                ObjectNode show =  Json.newObject();
                show.put("type", "showImages");
                show.put("seconds",5);
				entry.getValue().getChannel().write(show); 
		}
	}
    
    public void guessedWord(String guesser)
    {
        for(Map.Entry<Integer, Painter> entry : painters.entrySet()) {
            if(entry.getValue().name.equals(guesser)&&entry.getValue().guessed==false&&!entry.getValue().role.equalsIgnoreCase("SKETCHER"))
            {
                numberGuessed++;
                Painter currentPlayer=entry.getValue();
                ObjectNode guesserJson =  Json.newObject();
                guesserJson.put("type", "guesser");
                guesserJson.put("name",guesser);
                guesserJson.put("guessWord",chatRoom.getCurrentGuess());
                guesserJson.put("points",guesserPointsRemaining);
                currentPlayer.setPoints(currentPlayer.getPoints()+guesserPointsRemaining);
                currentPlayer.getChannel().write(guesserJson);
                currentPlayer.setCorrectGuess();
                
                
                //Send the underlying image also to the guesser
                ObjectNode guessWord = retrieveCurrentTaskImage();
                entry.getValue().getChannel().write(guessWord);
                
                //Only if less than 5 players have already guessed assign the points to the sketcher
                if(guesserPointsRemaining>5)
                {
                    ObjectNode sketcherJson =  Json.newObject();
                    sketcherJson.put("type", "sketcher");
                    sketcherJson.put("name",sketcherPainter.name);
                    sketcherPointsRemaining= (guesserPointsRemaining==10) ? 10 : 1;
                    sketcherJson.put("points",sketcherPointsRemaining);
                    sketcherPainter.getChannel().write(sketcherJson);
                    sketcherPainter.setPoints(sketcherPainter.getPoints()+sketcherPointsRemaining);
                }
                if(guesserPointsRemaining>=5)
                    guesserPointsRemaining--;
            }
            if(!guessedWord)
            {
                entry.getValue().getChannel().write(timerChange(remainingTimeOnGuess));    
            }
        }
        //Every player has guessed, reduce the remaining time
        if(numberGuessed==(chatRoom.playersMap.size()-1))
        for(Map.Entry<Integer, Painter> entry : painters.entrySet())
                entry.getValue().getChannel().write(timerChange(3));
        guessedWord=true;
    }
    
    
    public void nextRound(String sketcher) 
    {
        roundNumber++;
        guesserPointsRemaining=10;
        sketcherPointsRemaining=0;
        numberGuessed=0;
        guessedWord=false;
        //We need to play another round
        if(roundNumber<=maxRound)
        {
            for(Map.Entry<Integer, Painter> entry : painters.entrySet()) {
                entry.getValue().guessed=false;
                if(entry.getValue().name.equals(sketcher))
                {
                    ObjectNode roleMessage = Json.newObject();
                    roleMessage.put("type", "role");
                    roleMessage.put("role","SKETCHER");
                    entry.getValue().getChannel().write(roleMessage);
                    sketcherPainter=entry.getValue();

                    //RETRIEVE THE IMAGE AND THE TAG TO SEND TO THE SKETCHER
                    ObjectNode guessWord =  retrieveTaskImage();
                    entry.getValue().getChannel().write(guessWord);
                    currentGuess=guessWord.get("word").asText();
                }
                else
                {
                    ObjectNode self = Json.newObject();
                    self.put("type", "role");
                    self.put("role","GUESSER");
                    entry.getValue().getChannel().write(self);
                }
            }
        }
    }
    
    public void gameEnded()
    {
            JsonNodeFactory factory = JsonNodeFactory.instance;
            ObjectNode leaderboard = new ObjectNode(factory);
            leaderboard.put("type", "leaderboard");
            leaderboard.put("playersNumber", painters.size());
            //[TODO] order the list
            List<Painter> list = new ArrayList<Painter>(painters.values());
            Collections.sort(list);
            ArrayNode playersOrder=new ArrayNode(factory);
            for (Painter painter : list) {
                ObjectNode row = new ObjectNode(factory);
                row.put("name", painter.name);
                row.put("points", painter.getPoints());
                playersOrder.add(row);
            }
            leaderboard.put("playerList",playersOrder);
            //Send the leaderboard information to all the players
            for(Map.Entry<Integer, Painter> entry : painters.entrySet()) {
                //Set the remaining time to 0
                entry.getValue().getChannel().write(timerChange(0));
                entry.getValue().getChannel().write(leaderboard);
            }
    }
	
    
    public ObjectNode retrieveTaskImage()
    {
         guessObject=null;
         int size = taskHashSet.size();
         int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
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

    
    public void setChatRoom(ChatRoom chatRoom)
    {
        this.chatRoom=chatRoom;
    }
    
    public ChatRoom getChatRoom()
    {
        return this.chatRoom;
    }
    
    public int paintersSize()
    {
        return painters.size();
    }
}
