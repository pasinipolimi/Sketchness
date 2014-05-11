package utils.gamemanager;

import akka.actor.ActorRef;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.util.Timeout;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import play.Logger;
import play.libs.Akka;
import utils.LoggerUtils;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.Room;

/**
 * Class used to manage the list of all the possible game instances that are
 * running. A game instance is identified by the name of the match and the
 * number of current players that have joined the match
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class GameManager implements GameManagerInterface, Serializable {

    private static GameManagerInterface instance = null;
    private static Long matchId = 1L;
    private static Room lobby = new GameMessages.Room("lobby");
    private HashMap<String, ActorRef> gameInstances = new HashMap<>();

    private GameManager() {
    }

    public static GameManagerInterface getInstance() throws Exception{
       if (instance == null) {
           synchronized (GameManager.class) {
                instance = TypedActor.get(Akka.system()).typedActorOf(new TypedProps<>(GameManagerInterface.class, GameManager.class).withTimeout(new Timeout(5000)));
           }
       }
       return instance;
    }

    @Override
    public void onReceive(Object message, ActorRef ref) {
         LoggerUtils.debug("GAMEMANAGER", (String)message);
    }

    @Override
    public String addInstance(String roomName, ActorRef current) throws Exception {
        try {
            Long id = matchId++;
            String instanceId = roomName + id;
            if (!gameInstances.containsValue(current)) {
                gameInstances.put(instanceId, current);
            }
            return instanceId;
        }
        catch (Exception e) {
             LoggerUtils.error("Error in adding instance",e);
            return null;
        }
    }

    @Override
    public void getCurrentGames() {
        for (Map.Entry pairs : gameInstances.entrySet()) {
            ((ActorRef) pairs.getValue()).tell(new GameEvent(GameMessages.composeMatchInfo()), (ActorRef) pairs.getValue());
        }
    }

    @Override
    public Room getRoom(String roomName) {
        return new GameMessages.Room(roomName);
    }
    
    

    @Override
    public Room getLobby() {
        return lobby;
    }

    @Override
    public void removeInstance(ActorRef toRemove) {
        gameInstances.values().remove(toRemove);
        getCurrentGames();
    }
}
