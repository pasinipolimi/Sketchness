package models.chat;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import models.factory.Factory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.TimeoutException;
import play.Logger;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.LoggerUtils;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages.Join;
import utils.gamemanager.GameManager;

public class ChatFactory extends Factory {

    public static void createChat(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception {
        int trial = 0;
        
        final ActorRef obtained = create(room, Chat.class);
        Future<Object> future = Patterns.ask(obtained, new Join(username, out), 5000);
        // Send the Join message to the room
        
        String result = null;
        try {
            result = (String) Await.result(future, Duration.create(5, SECONDS));
        }
        catch (TimeoutException timeout) {
            result=null;
            LoggerUtils.error("CHATFACTORY", timeout);
        }
        if(result==null) {
            throw new Exception("Chat creation failed");
        }
            

        if ("OK".equals(result)) {
                GameBus.getInstance().subscribe(obtained, GameManager.getInstance().getRoom(room));
        } else {
            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);
            // Send the error to the socket.
            out.write(error);
        }
    }
}
