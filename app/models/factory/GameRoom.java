package models.factory;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import play.Logger;
import utils.gamebus.GameBus;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public abstract class GameRoom extends UntypedActor {

    Class roomType = GameRoom.class;

    public GameRoom(Class roomType) {
        this.roomType = roomType;
    }

    @Override
    /*
     * Each room must implement the onReceive method in order to exchange 
     * messages
     */
    public abstract void onReceive(Object message) throws Exception;

    /*
     * Function called by an actor in order to kill himself
     */
    protected void killActor() {
        ActorRef me = this.getContext().self();
        if (!me.path().toString().endsWith("lobby")) {
            me.tell(PoisonPill.getInstance(), null);
            GameBus.getInstance().unsubscribe(me);
            Logger.info("[" + roomType.getName() + "]: killed room " + me.path());
        }
    }
}
