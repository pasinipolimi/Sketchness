package models.game;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import models.Painter;
import models.factory.GameRoom;
import play.Logger;
import play.Play;
import play.db.DB;
import play.i18n.Messages;
import play.libs.Akka;
import scala.concurrent.duration.Duration;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.levenshteinDistance;
import utils.CMS.CMS;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.Join;
import utils.gamebus.GameMessages.LogLevel;
import utils.gamebus.GameMessages.Room;
import utils.gamemanager.GameManager;
import akka.actor.Cancellable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.logging.Level;
import utils.gamebus.GameEventType;
import utils.gamemanager.GameManagerInterface;
import views.html.leaderboard;

/**
 * A chat room is an Actor.
 */
public class Game extends GameRoom {

	private final Integer remainingTimeOnGuess = Integer.parseInt(Play
			.application().configuration().getString("remainingTimeOnGuess")); // Once
																				// a
																				// player
																				// has
																				// guessed,
																				// change
																				// the
																				// time
																				// for
																				// everyone
																				// to
																				// 20
																				// seconds
	private final Integer remainingTimeOnAllGuess = Integer
			.parseInt(Play.application().configuration()
					.getString("remainingTimeOnAllGuess")); // If all have
															// guessed, reduce
															// the timer to 3
															// seconds
	private final Integer maxSketcherPointsRemaining = Integer.parseInt(Play
			.application().configuration()
			.getString("maxSketcherPointsRemaining"));
	private final Integer minSketcherPointsRemaining = Integer.parseInt(Play
			.application().configuration()
			.getString("minSketcherPointsRemaining"));
	private final Integer maxGuesserPointsRemaining = Integer.parseInt(Play
			.application().configuration()
			.getString("maxGuesserPointsRemaining"));
	private final Integer minGuesserPointsRemaining = Integer.parseInt(Play
			.application().configuration()
			.getString("minGuesserPointsRemaining"));
	private Integer maxRound = Integer.parseInt(Play.application()
			.configuration().getString("maxRounds")); // Maximum number of
														// rounds
	private Integer requiredPlayers = Integer.parseInt(Play.application()
			.configuration().getString("requiredPlayers"));
	private final Boolean fixGroundTruth = Boolean.parseBoolean(Play
			.application().configuration().getString("fixGroundTruth"));
	private final Integer groundTruthId = Integer.parseInt(Play.application()
			.configuration().getString("groundTruthId"));
	// [TODO] Minimum tags that an image should have to avoid asking to the
	// users for new tags
	private final Integer minimumTags = Integer.parseInt(Play.application()
			.configuration().getString("minimumTags"));
	// Variables used to manage the rounds
	private Boolean guessedWord = false; // Has the word been guessed for the
											// current round?
	private Boolean gameStarted = false;
	private Boolean areWeAsking = false;
	private Integer roundNumber = 0; // Starts with round number 1
	private Integer sketcherPointsRemaining = 0; // The number of points
													// available to the
													// sketcher: for the first
													// reply he gets 5, for the
													// second 4 and so on
	private Integer guesserPointsRemaining = maxGuesserPointsRemaining; // The
																		// number
																		// of
																		// points
																		// available
																		// to
																		// the
																		// guessers:the
																		// first
																		// get
																		// 10,
																		// the
																		// second
																		// 9 and
																		// so on
	private Integer numberGuessed = 0; // Number of players that have guessed
										// for a specific round
	private Painter sketcherPainter; // The current sketcher
	// System variables
	private Room roomChannel; // Name of the room
	private Boolean taskAcquired = false;
	// Members of this room.
	private CopyOnWriteArrayList<Painter> playersVect = new CopyOnWriteArrayList<>();
	// Control Variables
	private ObjectNode guessObject;
	private Integer missingPlayers = requiredPlayers;
	private Integer disconnectedPlayers = 0;
	private Boolean shownImages = false;
	private HashSet<ObjectNode> taskHashSet = new HashSet<>();
	private final HashSet<ObjectNode> priorityTaskHashSet = new HashSet<>();
	// We should not assign the same uTask to the same match, keep a list of the
	// uTasks that has been already used
	private final HashSet<Integer> usedUTasks = new HashSet<>();
	private ObjectNode taskImage;
	private Integer sessionId;
	private Integer uTaskID;
	private final static Integer maxSinglePlayer = Play.application()
			.configuration().getInt("maxSinglePlayer");

	String currentGuess;
	Boolean askTag = false;
	String askTagSketcher;
	Boolean loadingAlreadySent = false;

	public Game() {
		super(Game.class);
	}

	/*
	 * Handles all the messages sent to this actor
	 * 
	 * @param message An Object representing a generic message sent to our Actor
	 */
	@Override
	public void onReceive(final Object message) {
		try {
			if (message instanceof Room) {
				this.roomChannel = ((Room) message);
				requiredPlayers = ((Room) message).getRequiredPlayers();
				// In the initial idea of single player, give 50 images to the
				// player that is segmenting
				if (requiredPlayers == 1)
					maxRound = maxSinglePlayer;
				missingPlayers = requiredPlayers;
				newGameSetup();
				Logger.info("[GAME] " + roomChannel.getRoom() + " created.");
			}
			if (message instanceof Join) {
				playerJoin(((Join) message).getUsername());
			} else if (message instanceof GameEvent) {
				JsonNode event = ((GameEvent) message).getJson();
				if (event != null) {
					event = event.get("message");
					final String type = event.get("type").asText();
					switch (type) {
					case "taskAcquired":
						taskAcquired();
						break;
					case "leave":
						handleQuitter(event);
						break;
					case "matchInfo":
						publishLobbyEvent();
						break; // publishLobbyEvent(GameEventType.matchEnd);
					// break point between tested and not Tested
					case "skip":
						skipTask();
						break;
					case "guessed":
						guessed(event.get("content").get("user").asText());
						break;
					case "guessAttempt":
						handleTalk(event);
						break;
					case "tag":
						tagReceived(event.get("content").get("word").asText());
						break;
					case "finalTraces":
						CMS.segmentation((ObjectNode) event.get("content"),
								sketcherPainter.name, sessionId);
						break;
					case "endSegmentation":
						endSegmentation(event.get("content").get("user")
								.asText());
						break;
					// break point between working and doing
					case "timer":
						playerTimeExpired(event.get("content").get("user")
								.asText());
						break;
					}
				}
			} else if (message instanceof ObjectNode) {
				JsonNode event = ((JsonNode) message);
				GameBus.getInstance().publish(
						new GameMessages.GameEvent(event, roomChannel));
				event = event.get("message");
				final String type = event.get("type").asText();
				switch (type) {
				case "leave":
					handleQuitter(event);
					break;
				}
			}
		} catch (final Exception e) {
			LoggerUtils.error("[GAME]:", e);
                        
		}
	}

	/*
	 * Retrieves one of the images that has been stored to be segmented at
	 * random being careful not to retrieve the same image two times for the
	 * same match. <p>
	 * 
	 * @return The object related to the task: image + tag
	 */
	private ObjectNode retrieveTaskImage() throws Exception {
		guessObject = null;
		uTaskID = null;
		// If we have task prioritized, then use them first
		while (priorityTaskHashSet.size() > 0 && guessObject == null) {
			final Iterator<ObjectNode> it2 = priorityTaskHashSet.iterator();
			while (it2.hasNext()) {
				final ObjectNode obj = it2.next();
				guessObject = obj;
				final Integer task = guessObject.get("taskid").asInt();
				if (usedUTasks.contains(task)) {
					priorityTaskHashSet.remove(guessObject);
					guessObject = null;
				} else {
					uTaskID = guessObject.get("utaskid").asInt();
					usedUTasks.add(task);
				}
				break;
			}
			if (guessObject != null) {
				priorityTaskHashSet.remove(guessObject);
			}
		}
		 if (guessObject == null) {
                    Iterator<ObjectNode> it = taskHashSet.iterator();
                    while (it.hasNext()) {
                        ObjectNode obj = it.next();
                        guessObject = obj;
                    }
                    taskHashSet.remove(guessObject);
                }
		return guessObject;
	}

	private void endSegmentation(final String user) throws Exception {
		final GameEvent eventGuesser = new GameEvent(GameMessages.composeScore(
				user, guesserPointsRemaining), roomChannel);
		GameBus.getInstance().publish(eventGuesser);
		GameBus.getInstance().publish(
				new GameEvent(GameMessages.composeSaveTraces(), roomChannel));
		GameBus.getInstance().publish(
				new GameEvent(GameMessages.composeEndSegmentation(),
						roomChannel));
		nextRound();
	}

	/*
	 * Retrieves the name of the sketcher for the next round. It assigns the
	 * sketcher role to the players that have never played as such first <p>
	 * 
	 * @return The object related to the task: image + tag [TESTED]
	 */
	private String nextSketcher() {
		sketcherPainter = null;
		final int currentPlayers = requiredPlayers - disconnectedPlayers;
		int count = 0;
		// Publish system messages to inform that a new round is starting and
		// the roles are being chosen
		GameBus.getInstance().publish(
				new GameEvent(GameMessages.composeLogMessage(LogLevel.info,
						Messages.get(LanguagePicker.retrieveLocale(),
								"newround")), roomChannel));
		GameBus.getInstance().publish(
				new GameEvent(GameMessages.composeLogMessage(LogLevel.info,
						Messages.get(LanguagePicker.retrieveLocale(),
								"choosingroles")), roomChannel));

		// Set all the players as GUESSERS at the beginning
		for (int i = 0; i < currentPlayers; i++) {
			playersVect.get(i).role = "GUESSER";
		}

		// Keep searching for a new sketcher
		while (sketcherPainter == null) {
			// If all the players have been sketcher at least one time, reset
			// their status
			if (count == currentPlayers) {
				for (int i = 0; i < currentPlayers; i++) {
					playersVect.get(i).hasBeenSketcher = false;
				}
				count = 0;
			} // Find a sketcher at random among the ones that have never played
				// such a role
			else {
				final int index = (int) (Math.random() * currentPlayers);
				if (!playersVect.get(index).hasBeenSketcher) {
					sketcherPainter = playersVect.get(index);
					sketcherPainter.role = "SKETCHER";
					sketcherPainter.hasBeenSketcher = true;
				}
				count++;
			}
		}
		// Publish a system message to inform the other players on who is the
		// sketcher
		GameBus.getInstance().publish(
				new GameEvent(GameMessages.composeLogMessage(
						LogLevel.info,
						Messages.get(LanguagePicker.retrieveLocale(),
								"thesketcheris") + " " + sketcherPainter.name),
						roomChannel));
		GameBus.getInstance().publish(
				new GameEvent(GameMessages
						.composeRoundBegin(sketcherPainter.name), roomChannel));
		return sketcherPainter.name;
	}

	/*
	 * Check if there are enough players connected and that all the modules have
	 * received the login information regarding all these players. If not enough
	 * players are connected, inform the players with a message and wait for new
	 * connections or for all the modules to receive the login of the respective
	 * players [TESTED]
	 */
	private void checkStart() throws Exception {
		if (!triggerStart()) // Send a message to inform about the missing
								// players
		{
			final int nPlayers = playersVect.size();
			if (requiredPlayers - nPlayers > 1) {
				GameBus.getInstance()
						.publish(
								new GameEvent(
										GameMessages
												.composeLogMessage(
														LogLevel.info,
														Messages.get(
																LanguagePicker
																		.retrieveLocale(),
																"waitingfor")
																+ " "
																+ (requiredPlayers - nPlayers)
																+ " "
																+ Messages.get(
																		LanguagePicker
																				.retrieveLocale(),
																		"playerstostart")),
										roomChannel));
			} else if (requiredPlayers - nPlayers == 1) {
				GameBus.getInstance()
						.publish(
								new GameEvent(
										GameMessages
												.composeLogMessage(
														LogLevel.info,
														Messages.get(
																LanguagePicker
																		.retrieveLocale(),
																"waitingfor")
																+ " "
																+ (requiredPlayers - nPlayers)
																+ " "
																+ Messages.get(
																		LanguagePicker
																				.retrieveLocale(),
																		"playertostart")),
										roomChannel));
			}
		}
	}

	private boolean triggerStart() throws Exception {
		// We need to wait for all the modules to receive the player list
		if (playersVect.size() >= requiredPlayers) {
			GameManager.getInstance().removeInstance(getSelf());
			// publishLobbyEvent();
			// //publishLobbyEvent(GameEventType.matchStart);
			if (taskAcquired) {
				if (!loadingAlreadySent) {
					GameBus.getInstance().publish(
							new GameEvent(GameMessages.composeLogMessage(
									LogLevel.info, Messages.get(
											LanguagePicker.retrieveLocale(),
											"acquiring")), roomChannel));
					GameBus.getInstance().publish(
							new GameEvent(GameMessages.composeLoading(),
									roomChannel));
					loadingAlreadySent = true;
				}

				// Create a new session in which to store the actions of the
				// game
				if (!fixGroundTruth)
					sessionId = CMS.openSession();
				else
					sessionId = groundTruthId;
				disconnectedPlayers = 0;
				roundNumber = 0;
				gameStarted = true;
				nextRound();
				// We start the game
				if (sketcherPainter != null) {
					// GameBus.getInstance().publish(new GameEvent(roomChannel,
					// GameEventType.matchStart));
					// GameBus.getInstance().publish(new
					// GameEvent(GameMessages.composeBegin(sketcherPainter.name),roomChannel));
				} else {
					throw new Error("[GAME] Cannot find a suitable Sketcher!");
				}
			} else {
				loadingAlreadySent = true;
				GameBus.getInstance().publish(
						new GameEvent(GameMessages.composeLogMessage(
								LogLevel.info, Messages.get(
										LanguagePicker.retrieveLocale(),
										"acquiring")), roomChannel));
				GameBus.getInstance().publish(
						new GameEvent(GameMessages.composeLoading(),
								roomChannel));

			}
			return true;
		}
		return false;
	}

	public Integer generateRandomItem(final int i, final int size) throws Exception {
		Integer item;
		byte trials = 0;
		do {
			try {
				item = new Random().nextInt(size);
			} catch (final IllegalArgumentException ex) {
				item = null;
				Logger.error("[GAME] Failed to retrieve Task Image, retrying.");
				trials++;
				if (trials >= 5) {
					gameError();
					throw new Error(
							"[GAME] Failed to retrieve Task Image, aborting");
				}
			}
		} while ((item == null) || (item == i));

		return item;
	}

	public Integer generateRandomItem(final int size) throws Exception {
		Integer item;
		byte trials = 0;
		do {
			try {
				item = new Random().nextInt(size);
			} catch (final IllegalArgumentException ex) {
				item = null;
				Logger.error("[GAME] Failed to retrieve Task Image, retrying.");
				trials++;
				if (trials >= 5) {
					gameError();
					throw new Error(
							"[GAME] Failed to retrieve Task Image, aborting");
				}
			}
		} while (item == null);

		return item;
	}

	/*
	 * Start a new round of the game
	 */
	private void nextRound() throws Exception {
		roundNumber++;
		if (roundNumber <= maxRound) {
			// Reset the points and status counters
			guesserPointsRemaining = maxGuesserPointsRemaining;
			sketcherPointsRemaining = 0;
			numberGuessed = 0;
			guessedWord = false;
			// Nobody has guessed for this round
			for (final Painter reset : playersVect) {
				reset.guessed = false;
			}
			nextSketcher();
			// Check if a tag for the current image as already been provided;if
			// not, ask for a new one
			try {
				taskImage = retrieveTaskImage();
			} // We cannot recover the task to be done, recover the error by
				// closing
				// the game
			catch (final Exception e) {
				Logger.error("[GAME] Failed to retrieve Task Image, aborting.");
				gameEnded();
				throw new Error(
						"[GAME] Failed to retrieve Task Image, aborting");
			}
			if (taskImage != null) {
				final String label = taskImage.get("tag").asText();
				if (label.equals("")) // We need to ask for a new tag
				{
					sendTask(true);
				} else // We have already a tag that has been provided, use that
						// one
				{
					currentGuess = label;
					sendTask(false);
				}
			} // We have no more things to do
			else {
				Logger.info("[GAME] Nothing more to do.");
				gameEnded();
			}
		} // We have played all the rounds for the game, inform the users and
			// the modules
			// that the match has ended
		else {
			Logger.info("[GAME] Round ended, closing the game.");
			gameEnded();
		}

	}

	private void sendTask(final Boolean ask) throws Exception {
		final String id = guessObject.get("id").asText();
		final String medialocator = guessObject.get("image").asText();
		final int width = guessObject.get("width").asInt();
		final int height = guessObject.get("height").asInt();
		final String word = guessObject.get("tag").asText();
		if (ask) {
			areWeAsking = true;
			final GameEvent task = new GameEvent(GameMessages.composeTag(
					sketcherPainter.name, id, medialocator, width, height),
					roomChannel);
			GameBus.getInstance().publish(task);
		} else {
			areWeAsking = false;
			final GameEvent task = new GameEvent(
					GameMessages.composeTask(sketcherPainter.name, id,
							medialocator, word, width, height), roomChannel);
			GameBus.getInstance().publish(task);
		}
	}

	/*
	 * Check if the timer for all the players has expired, show the solution for
	 * the current round and start a new one
	 */
	private void playerTimeExpired(final String name) throws Exception {
		// If all the players have disconnected during a game, start a new one
		// if it's not a single player game
		if (((requiredPlayers - disconnectedPlayers) <= 1) && gameStarted
				&& requiredPlayers != 1) {
			// Restart the game
			Logger.info("[GAME] No more players playing, closing the game.");
			gameEnded();
		} // There are still players in game
		else {
			// We are still missing the end response from some players
			// [TODO] POSSIBLE EXPLOIT
			if (missingPlayers > 0) {
				for (final Iterator<Painter> it = playersVect.iterator(); it
						.hasNext();) {
					final Painter painter = it.next();
					if (painter.name.equals(name)) {
						missingPlayers--;
					}
				}
			}
			// If we have received a response from all the active players in the
			// game, end the round
			if ((missingPlayers - disconnectedPlayers) == 0) {
				// Before calling the new round, show the solution to all the
				// players and end the round
				if ((areWeAsking == false && shownImages == true)
						|| requiredPlayers == 1 || guessedWord) {
					// If at least one player has guessed, it means that the
					// drawn contour is a good one
					if (guessedWord) {
						// GameBus.getInstance().publish(new
						// GameEvent(roomChannel, GameEventType.saveTraces));
						GameBus.getInstance().publish(
								new GameEvent(GameMessages.composeSaveTraces(),
										roomChannel));
					}

					areWeAsking = false;
					shownImages = false;
					// Start a new round
					missingPlayers = requiredPlayers;
					nextRound();

				} // If the solution has been given or a tag has not been
					// chosen, start a new round
				else {
					if (areWeAsking) {
						GameBus.getInstance()
								.publish(
										new GameEvent(
												GameMessages
														.composeLogMessage(
																LogLevel.info,
																sketcherPainter.name
																		+ " "
																		+ Messages
																				.get(LanguagePicker
																						.retrieveLocale(),
																						"notag")),
												roomChannel));
						GameBus.getInstance().publish(
								new GameEvent(GameMessages.composeNoTag(),
										roomChannel));
						missingPlayers = requiredPlayers;
						areWeAsking = false;
						nextRound();
					} else if (!shownImages) {
						shownImages = true;
						final String id = taskImage.get("id").asText();
						final String medialocator = taskImage.get("image")
								.asText();
						final int width = taskImage.get("width").asInt();
						final int height = taskImage.get("height").asInt();
						GameBus.getInstance().publish(
								new GameEvent(GameMessages.composeRoundEnd(
										taskImage.get("tag").asText(), id,
										medialocator, width, height),
										roomChannel));
						missingPlayers = requiredPlayers;
					}
				}
			}
		}
	}

	/*
	 * Initialization of the variables to start a new game
	 */
	public void newGameSetup() throws Exception {
		disconnectedPlayers = 0;
		roundNumber = 0;
		gameStarted = false;
		playersVect = new CopyOnWriteArrayList<>();
		final Cancellable init = Akka
				.system()
				.scheduler()
				.scheduleOnce(Duration.create(10, TimeUnit.MILLISECONDS),
						new Runnable() {
							@Override
							public void run() {
								Integer trials = 0;
								Boolean completed = false;
								while (trials < 5 && !completed) {
									try {
										trials++;
										if (!fixGroundTruth)
											CMS.taskSetInitialization(
													priorityTaskHashSet,
													taskHashSet, roomChannel,
													maxRound);
										else
											CMS.fixGroundTruth(groundTruthId,
													priorityTaskHashSet,
													taskHashSet, roomChannel);
										completed = true;
									} catch (final Exception ex) {
                                                                            try {
                                                                                gameError();
                                                                            } catch (Exception ex1) {
                                                                                LoggerUtils.error("GAME", ex1);
                                                                            }
										LoggerUtils.error("GAME", ex);
										return;
									}
								}
								if (trials >= 5) {
                                                                    try {
                                                                        gameEnded();
                                                                    } catch (Exception ex) {
                                                                        LoggerUtils.error("GAME", ex);
                                                                    }
									LoggerUtils
											.error("GAME",
													"[GAME] Impossible to retrieve the set of image relevant for this game, aborting");
									return;
								}
							}
						}, Akka.system().dispatcher());
		CMS.addInitializationThread(roomChannel.getRoom(), init);

		Logger.info("[GAME] New game started");
	}

	private void playerJoin(final String username) throws Exception {
		Logger.debug("[GAME] Player Joined");
		final Painter painter = new Painter(username, false);
		// Add the new entered player, it has never been a sketcher in this game
		// (false)
		playersVect.add(painter);
		getSender().tell("OK", this.getSelf());
		// publishLobbyEvent(GameEventType.join);
		publishLobbyEvent();
		Logger.info("[GAME] added player "
				+ playersVect.get(playersVect.size() - 1).name);
		Logger.debug("[GAME] Check Start");
		checkStart();
	}

	/**
	 * Function used to update the status of the lobby room
	 * 
	 * 
	 * destroyed, update status
	 * 
	 */
	// @param type the type of the event to publish: room created, room

	private void publishLobbyEvent() throws Exception {
                final ObjectNode status = new ObjectNode(JsonNodeFactory.instance);
		// Get the hashcode related to this actoref in order to make it unique
		status.put("id", this.getSelf().hashCode());
		status.put("roomName", roomChannel.getRoom());
		status.put("currentPlayers", playersVect.size());
		status.put("maxPlayers", requiredPlayers);
		status.put("visible", playersVect.size() < requiredPlayers);
                GameEvent join = null;
                try {
                   join = new GameEvent(GameMessages.composeGameListUpdate(status), GameManager.getInstance().getLobby());
                } catch(Exception e) {
                   LoggerUtils.error("[GAME]", e);
                }
		Logger.info("[GAME] room - " + roomChannel.getRoom()+ " current players - " + playersVect.size()+ " max players - " + requiredPlayers);
		if(join!=null)
                    GameBus.getInstance().publish(join);
                else {
                    GameBus.getInstance().publish(new GameEvent(GameEventType.error));
                }
	}

	private void handleQuitter(final JsonNode jquitter) throws Exception {
		final String quitter = jquitter.get("content").get("user").asText();
		for (final Painter painter : playersVect) {
			if (painter.name.equalsIgnoreCase(quitter)) {
				playersVect.remove(painter);
				disconnectedPlayers++;
				// GameBus.getInstance().publish(new GameEvent("quit",
				// GameManager.getInstance().getLobby(), GameEventType.quit));
				GameBus.getInstance().publish(
						new GameEvent(GameMessages.composeQuit(quitter),
								GameManager.getInstance().getLobby()));
				// End the game if there's just one player or less
				if (((requiredPlayers - disconnectedPlayers) == 1)
						&& gameStarted) {
					Logger.info("[GAME] Just one player left, closing the game.");
					gameEnded();
				} else if (((requiredPlayers - disconnectedPlayers) <= 0)
						&& gameStarted) {
					publishLobbyEvent(); // publishLobbyEvent(GameEventType.matchEnd);
				}

			}
		}
		if (playersVect.isEmpty()) {
			Logger.info("[GAME] No more players, closing the game.");
			gameEnded();
		}
	}

	private void guessed(final String guesser) throws Exception {
		String id;
		String medialocator;
		int width;
		int height;
		for (final Painter painter : playersVect) {
			// If the current painter is the guesser, has not guessed before and
			// it is not the sketcher, update his points
			if (painter.name.equals(guesser) && painter.guessed == false) {
				numberGuessed++;
				painter.setPoints(painter.getPoints() + guesserPointsRemaining);
				painter.setCorrectGuess();

				GameEvent eventGuesser = new GameEvent(
						GameMessages.composeScore(guesser,
								guesserPointsRemaining), roomChannel);
				GameBus.getInstance().publish(eventGuesser);

				id = guessObject.get("id").asText();
				medialocator = guessObject.get("image").asText();
				width = guessObject.get("width").asInt();
				height = guessObject.get("height").asInt();
				eventGuesser = new GameEvent(GameMessages.composeImage(guesser,
						id, medialocator, width, height), roomChannel);
				GameBus.getInstance().publish(eventGuesser);

				if (guesserPointsRemaining > minGuesserPointsRemaining) {
					// Assign the points to the sketcher. Has someone guessed
					// for this round? If not, assign maximum points, if so
					// assign the minimum for each guess
					sketcherPointsRemaining = (guesserPointsRemaining == maxGuesserPointsRemaining) ? maxSketcherPointsRemaining
							: minSketcherPointsRemaining;
					sketcherPainter.setPoints(sketcherPainter.getPoints()
							+ sketcherPointsRemaining);
					final GameEvent eventSketcher = new GameEvent(
							GameMessages.composeScore(sketcherPainter.name,
									sketcherPointsRemaining), roomChannel);
					GameBus.getInstance().publish(eventSketcher);
				}
				if (guesserPointsRemaining >= minGuesserPointsRemaining) {
					guesserPointsRemaining--;
				}
			}
			if (!guessedWord) {
				// Send the time change once just a player has guessed
				// GameEvent timeEvent = new GameEvent(roomChannel,
				// GameEventType.timerChange);
				GameEvent timeEvent;
				// If we are in single player mode, don't wait
				if (requiredPlayers == 1) {
					// timeEvent.setObject(timerChange(0,
					// CountdownTypes.round));
					timeEvent = new GameEvent(GameMessages.composeTimer(0),
							roomChannel);
				} else {
					// timeEvent.setObject(timerChange(remainingTimeOnGuess,
					// CountdownTypes.round));
					timeEvent = new GameEvent(
							GameMessages.composeTimer(remainingTimeOnGuess),
							roomChannel);
				}
				GameBus.getInstance().publish(timeEvent);
			}
		}
		if (numberGuessed == (playersVect.size() - 1)) {
			// Send the message to change the time for everyone to end the round
			// GameEvent timeEvent = new GameEvent(roomChannel,
			// GameEventType.timerChange);
			// timeEvent.setObject(timerChange(remainingTimeOnAllGuess,
			// CountdownTypes.round));
			final GameEvent timeEvent = new GameEvent(
					GameMessages.composeTimer(remainingTimeOnAllGuess),
					roomChannel);
			GameBus.getInstance().publish(timeEvent);

			id = taskImage.get("id").asText();
			medialocator = taskImage.get("image").asText();
			width = taskImage.get("width").asInt();
			height = taskImage.get("height").asInt();
			GameBus.getInstance().publish(
					new GameEvent(GameMessages.composeRoundEnd(
							taskImage.get("tag").asText(), id, medialocator,
							width, height), roomChannel));
		}
		guessedWord = true;
	}
        
        private void gameError() throws Exception {
            throw new Exception("NEEDS TO BE IMPLEMENTED");
        }

	private void gameEnded() throws Exception {
		// Close the gaming session
		CMS.cancelThread(roomChannel.getRoom());
		if (sessionId != null) {
			CMS.closeSession(sessionId);
		}

		final GameEvent endEvent = new GameEvent(
				GameMessages.composeLeaderboard(compileLeaderboard()),
				roomChannel);
		GameBus.getInstance().publish(endEvent);

		final Painter[] sorted = playersVect.toArray(new Painter[0]);
		Connection connection = null;
		PreparedStatement statement = null;
		PreparedStatement statement1 = null;
		ResultSet rs = null;

		try {
			connection = DB.getConnection();

			for (final Painter painter : sorted) {
				try {
					final String query = "SELECT * FROM USERS WHERE NAME=? ";
					final String query1 = "UPDATE USERS SET TOTAL_SCORE = ? WHERE NAME = ? ";

					statement = connection.prepareStatement(query);
					statement1 = connection.prepareStatement(query1);

					statement.setString(1, painter.name);
					rs = statement.executeQuery();

					rs.next();
					statement1.setInt(1,
							rs.getInt("TOTAL_SCORE") + painter.getPoints());
					statement1.setString(2, painter.name);
					statement1.executeUpdate();
				} catch (final SQLException ex) {
					play.Logger.error("Unable to update total score for user: "
							+ painter.name, ex);
				} finally {
					if (rs != null) {
						rs.close();
					}
					if (statement != null)
						statement.close();
					if (statement1 != null)
						statement1.close();
				}

			}
		} catch (final SQLException ex) {

			Logger.error("Unable to get a DB connection.");

		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (final SQLException e) {
				play.Logger.error("Unable to close a SQL connection.");
			}
		}
		final GameEvent endEvent2 = new GameEvent(
				GameMessages.composeMatchEnd(), roomChannel);
		GameBus.getInstance().publish(endEvent2);
		killActor();
	}

	// Prepares the leaderboard of the players based on their points
	private ObjectNode compileLeaderboard() throws Exception {
		final JsonNodeFactory factory = JsonNodeFactory.instance;
		final ObjectNode leaderboard = new ObjectNode(factory);
		leaderboard.put("type", "leaderboard");
		leaderboard.put("playersNumber", playersVect.size());
		final Painter[] sorted = playersVect.toArray(new Painter[0]);
		Arrays.sort(sorted);
		final ArrayNode playersOrder = new ArrayNode(factory);
		for (final Painter painter : sorted) {
			final ObjectNode row = new ObjectNode(factory);
			row.put("name", painter.name);
			row.put("points", painter.getPoints());
			playersOrder.add(row);
		}
		leaderboard.put("playerList", playersOrder);
		return leaderboard;
	}

	private void skipTask() throws Exception {
		CMS.postAction(sessionId, "skiptask", sketcherPainter.name, "");
		GameBus.getInstance().publish(
				new GameEvent(GameMessages.composeLogMessage(
						LogLevel.info,
						sketcherPainter.name
								+ " "
								+ Messages.get(LanguagePicker.retrieveLocale(),
										"skiptask")), roomChannel));
		nextRound();
	}

	private void tagReceived(final String word) throws Exception {
		taskImage.remove("tag");
		taskImage.put("tag", word);
		currentGuess = word;
		sendTask(false);
	}

	/*
	 * [TESTED]
	 */
	private void taskAcquired() throws Exception {
		if (!taskAcquired) {
			taskAcquired = true;
			triggerStart();
		}
	}

	/**
	 * [works][not tested]
	 * 
	 * @param jguess
	 */
	private void handleTalk(final JsonNode jguess) throws Exception {
		final String text = jguess.get("content").get("word").asText();
		final String username = jguess.get("content").get("user").asText();
		// Received a Talk message
		// If we are asking the sketcher for a tag, then save the tag
		if (askTag && username.equals(askTagSketcher)) {
			askTag = false;
			// GameBus.getInstance().publish(new GameEvent(text, username,
			// roomChannel, GameEventType.tag));
		} else if (gameStarted) {
			// Compare the message sent with the tag in order to establish if we
			// have a right guess
			final levenshteinDistance distance = new levenshteinDistance();
			if (text != null) {
				switch (distance.computeLevenshteinDistance(text, currentGuess)) {
				case 0:
					// GameBus.getInstance().publish(new GameEvent(username,
					// roomChannel, GameEventType.guessed));
					GameBus.getInstance().publish(
							new GameEvent(GameMessages.composeGuessed(username,
									text), roomChannel));
					break;
				case 1:
					GameBus.getInstance().publish(
							new GameMessages.GameEvent(GameMessages
									.composeGuess(username, text, "hot"),
									roomChannel));
					break;
				case 2:
					GameBus.getInstance().publish(
							new GameMessages.GameEvent(GameMessages
									.composeGuess(username, text, "warm"),
									roomChannel));
					break;
				default:
					GameBus.getInstance().publish(
							new GameMessages.GameEvent(GameMessages
									.composeGuess(username, text, "cold"),
									roomChannel));
					break;
				}
			}
		}
	}
}

enum CountdownTypes {
	round, tag
}