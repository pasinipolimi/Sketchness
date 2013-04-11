package models.factory;

import utils.gamebus.GameBus;
import play.libs.*;
import play.libs.F.*;
import akka.actor.*;
import utils.Messages.*;
import utils.levenshteinDistance.*;

public abstract class Factory {
    /**
     * Join the default room.
     */
    protected static ActorRef create(final String room, Class module) throws Exception{
        ActorRef newRoom;                
        String roomID=room+"_"+module.getSimpleName();
        //Try to see if we have already registered this actor in the system, if it is the case return the reference
        newRoom=Akka.system().actorFor("akka://application/user/"+roomID);
        if(newRoom instanceof EmptyLocalActorRef)
        {
            //Otherwise create a new actor to the room and subscribe it to the message channel
            newRoom= Akka.system().actorOf(new Props(module),roomID);
            newRoom.tell(new Room(room));
            GameBus.getInstance().subscribe(newRoom, room);
        }

        return newRoom;
    }

}
