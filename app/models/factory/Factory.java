package models.factory;

import utils.gamebus.GameBus;
import play.libs.*;
import play.libs.F.*;
import akka.actor.*;
import utils.gamebus.GameMessages.Room;
import utils.levenshteinDistance.*;

public abstract class Factory {
    /**
     * Join the default room.
     */
    @SuppressWarnings("rawtypes")
    protected static ActorRef create(final String room, Class module) throws Exception {
        ActorRef newRoom;                
        String roomID=room+"_"+module.getSimpleName();
        //Try to see if we have already registered this actor in the system, if it is the case return the reference
        newRoom=Akka.system().actorFor("akka://application/user/"+roomID);
        if(newRoom instanceof EmptyLocalActorRef)
        {
            @SuppressWarnings("unchecked")
            Props properties= new Props(module);
            //Otherwise create a new actor to the room and subscribe it to the message channel
            newRoom =  Akka.system().actorOf(properties,roomID);
            newRoom.tell(new Room(room),newRoom);
            GameBus.getInstance().subscribe(newRoom, room);
        }
        return newRoom;
    }

}
