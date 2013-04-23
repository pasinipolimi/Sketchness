package models.paint;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import static java.util.concurrent.TimeUnit.SECONDS;
import utils.Messages;
import models.factory.Factory;
import org.codehaus.jackson.JsonNode;
import play.libs.F;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.gamebus.GameMessages;

/**
 *
 * @author Leyart
 */
public class PaintRoomFactory extends Factory{
    
    public static synchronized void createPaint(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception
    {

      final ActorRef finalRoom=create(room, Paint.class);
      Future<Object> future = Patterns.ask(finalRoom,new Messages.Join(username, out), 1000);
        // Send the Join message to the room
      String result = (String)Await.result(future, Duration.create(10, SECONDS));
        
        if("OK".equals(result)) 
        {
            // in: handle messages from the painter
            in.onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode json) throws Throwable {
                    finalRoom.tell(json,finalRoom);
                }
            });

            // User has disconnected.
            in.onClose(new F.Callback0() {
                @Override
                public void invoke() throws Throwable {
                    
                    finalRoom.tell(new GameMessages.GameEvent(username,room,"quit"),finalRoom);
                }
            });
        }
    }
}