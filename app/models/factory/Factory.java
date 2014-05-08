package models.factory;

import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.EmptyLocalActorRef;
import akka.actor.Identify;
import akka.actor.Props;
import akka.pattern.AskableActorSelection;
import akka.util.Timeout;
import java.util.concurrent.TimeUnit;
import play.libs.Akka;
import scala.concurrent.Await;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages.Room;

public abstract class Factory {

	/**
	 * Join the evolutions.default room.
	 */
	protected static ActorRef create(final String room, final int maxMembers,
			final Class module) throws Exception {
		ActorRef newRoom;
		final String roomID = module.getSimpleName() + "_"
				+ room.replaceAll("[^\\w\\s]", "");

		// Try to see if we have already registered this actor in the system, if
		// it is the case return the reference
		final ActorSelection sel = Akka.system().actorSelection(
				"akka://application/user/" + roomID);

		final Timeout t = new Timeout(20, TimeUnit.SECONDS);
		final AskableActorSelection asker = new AskableActorSelection(sel);
		final scala.concurrent.Future<Object> fut = asker.ask(new Identify(1),
				t);
		final ActorIdentity ident = (ActorIdentity) Await.result(fut,
				t.duration());
		final ActorRef ref = ident.getRef();

		if (ref == null) {
			final Props properties = new Props(module);
			// Otherwise create a new actor to the room and subscribe it to the
			// message channel
			newRoom = Akka.system().actorOf(properties, roomID);
			newRoom.tell(new Room(room, maxMembers), newRoom);
			GameBus.getInstance().subscribe(newRoom, room);
		} else {
			newRoom = ref;
		}
		return newRoom;
	}

	protected static ActorRef create(final String room, final Class module)
			throws Exception {
		ActorRef newRoom;
		final String roomID = module.getSimpleName() + "_"
				+ room.replaceAll("[^\\w\\s]", "");

		// Try to see if we have already registered this actor in the system, if
		// it is the case return the reference
		final ActorSelection sel = Akka.system().actorSelection(
				"akka://application/user/" + roomID);

		final Timeout t = new Timeout(5, TimeUnit.SECONDS);
		final AskableActorSelection asker = new AskableActorSelection(sel);
		final scala.concurrent.Future<Object> fut = asker.ask(new Identify(1),
				t);
		final ActorIdentity ident = (ActorIdentity) Await.result(fut,
				t.duration());
		final ActorRef ref = ident.getRef();

		if (ref == null) {
			final Props properties = new Props(module);
			// Otherwise create a new actor to the room and subscribe it to the
			// message channel
			newRoom = Akka.system().actorOf(properties, roomID);
			newRoom.tell(new Room(room), newRoom);
			GameBus.getInstance().subscribe(newRoom, room);
		} else {
			newRoom = ref;
		}
		return newRoom;
	}
}
