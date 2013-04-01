/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package models.paint;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import java.util.Map;
import static java.util.concurrent.TimeUnit.SECONDS;
import models.Messages;
import models.Painter;
import models.chat.Chat;
import models.factory.Factory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 *
 * @author Leyart
 */
public class PaintRoomFactory extends Factory{
    
    public static void createPaint(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception
    {

        final ActorRef finalRoom=create(room, Paint.class);
      Future<Object> future = (Future<Object>) Patterns.ask(finalRoom,new Messages.Join(username, out), 1000);
        // Send the Join message to the room
      String result = (String)Await.result(future, Duration.create(10, SECONDS));
        
        if("OK".equals(result)) 
        {
            // in: handle messages from the painter
            in.onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode json) throws Throwable {
                    finalRoom.tell(json);
                }
            });

            // User has disconnected.
            in.onClose(new F.Callback0() {
                @Override
                public void invoke() throws Throwable {
                    
                    finalRoom.tell(new Messages.Quit(username));
                }
            });
        }
    }
}
