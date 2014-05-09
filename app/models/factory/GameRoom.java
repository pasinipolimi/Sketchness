package models.factory;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import play.Logger;
import utils.LoggerUtils;
import utils.gamebus.GameBus;

/**
 * 
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public abstract class GameRoom extends UntypedActor {

	Class roomType = GameRoom.class;

	public GameRoom(final Class roomType) {
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
            try {
		final ActorRef me = this.getContext().self();
		if (!me.path().toString().endsWith("lobby")) {
                        GameBus.getInstance().unsubscribe(me);
                        me.tell(PoisonPill.getInstance(), me);
			LoggerUtils.info("GAMEROOM", "Killed room "+ me.path());
		}
            }
            catch(Exception e){
                LoggerUtils.error("Error while sending poison pill",e);
            }
	}
}
