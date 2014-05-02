package models.game;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.TimeoutException;
import models.chat.ChatFactory;
import models.factory.Factory;
import models.paint.PaintFactory;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamemanager.GameManager;

public class GameFactory extends Factory {

    public static synchronized void createGame(final String username, final String room, final Integer maxPlayers, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception {
        int trial = 0;
        boolean retrieved = false;
        final ActorRef obtained = create(room, maxPlayers, Game.class);
        while(trial<=5 && !retrieved) {
           try {
            GameManager.getInstance().addInstance(maxPlayers, room, obtained);
            retrieved = true;
           } catch(Exception e) {
               trial++;
               Logger.error("GameManager failure, retrying...");
           }
        }
        trial = 0;
        if(!retrieved)
            throw new Exception("Game creation failed after 5 trials");
        //Subscribe to lobby messages
        GameBus.getInstance().subscribe(obtained, GameManager.getInstance().getLobby());
        
        Future<Object> future = Patterns.ask(obtained, new GameMessages.Join(username, out), 50000);
        // Send the Join message to the room
        String result = null;
        while(trial<=5 && result==null) {
            try {
                result = (String) Await.result(future, Duration.create(50, SECONDS));
            }
            catch (TimeoutException timeout) {
                result=null;
                trial++;
                future = Patterns.ask(obtained, new GameMessages.Join(username, out), 50000);
            }
        }
        if(result==null)
            throw new Exception("Game creation failed after 5 trials");
       
        if ("OK".equals(result)) {
            ChatFactory.createChat(username, room, in, out);
            PaintFactory.createPaint(username, room, in, out);
            //Define the actions to be performed on the websockets
            in.onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode event) {
                    obtained.tell(event, obtained);
                }
            });

            // When the socket is closed.
            in.onClose(new F.Callback0() {
                @Override
                public void invoke() {
                     obtained.tell(GameMessages.composeQuit(username), obtained);
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
}
