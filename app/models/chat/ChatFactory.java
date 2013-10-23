package models.chat;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import static java.util.concurrent.TimeUnit.SECONDS;
import models.factory.Factory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages.Join;
import utils.gamemanager.GameManager;

public class ChatFactory extends Factory {

    public static synchronized void createChat(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception {
        final ActorRef obtained = create(room, Chat.class);
        Future<Object> future = Patterns.ask(obtained, new Join(username, out), 1000);
        // Send the Join message to the room
        String result = (String) Await.result(future, Duration.create(10, SECONDS));

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
