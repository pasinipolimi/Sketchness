/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package models.game;

import models.gamebus.GameBus;
import play.libs.*;
import play.libs.F.*;

import akka.actor.*;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;




import akka.pattern.Patterns;

import org.codehaus.jackson.node.*;

import java.util.*;

import models.Messages.*;
import models.paint.PaintRoom;
import models.Painter;


import play.mvc.*;

import org.codehaus.jackson.*;

import models.levenshteinDistance.*;




import static java.util.concurrent.TimeUnit.*;
import models.gamebus.GameMessages.GameStart;
import play.Logger;
import scala.concurrent.Future;

/**
 *
 * @author Leyart
 */
public class GameRoomFactory {
     
     
     // Default room.
    static Map<String,ActorRef> rooms = new HashMap<>();
    
    public static void join(final String room) throws Exception{
        ActorRef newRoom; 
        if(rooms.containsKey(room))
            newRoom=rooms.get(room);
        else
        {
            newRoom= Akka.system().actorOf(new Props(Game.class));
            rooms.put(room, newRoom);
        }
        
        final ActorRef finalRoom=newRoom;
            
        Future<Object> future = (Future<Object>) Patterns.ask(finalRoom,new GameStart("test"), 1000);
        // Send the Join message to the room
        String result = (String)Await.result(future, Duration.create(10, SECONDS));
        
        if("OK".equals(result)) 
        {
            GameBus.getInstance().subscribe(finalRoom, "gameEvents"+room);
        }
    }
}
