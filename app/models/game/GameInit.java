package models.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import play.Logger;
import play.Play;
import play.libs.Json;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.CMS.CMS;
import utils.CMS.CMSException;
import utils.CMS.models.ChooseImage;
import utils.CMS.models.ChooseImageTag;
import utils.CMS.models.Image;
import utils.CMS.models.MicroTask;
import utils.CMS.models.Tag;
import utils.CMS.models.Task;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.Room;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class GameInit {

	List<ObjectNode> queueImages = Collections
			.synchronizedList(new LinkedList<ObjectNode>());
	private final List<ObjectNode> priorityTaskQueue = Collections
			.synchronizedList(new LinkedList<ObjectNode>());
	List<ObjectNode> queueImagesBot = Collections
			.synchronizedList(new LinkedList<ObjectNode>());
	List<ObjectNode> queueImagesSpammer = Collections
			.synchronizedList(new LinkedList<ObjectNode>());

	private final static String rootUrl = Play.application().configuration()
			.getString("cmsUrl");

	private final static String useSupportingCollection = Play.application()
			.configuration().getString("useSupportingCollection");
	private final static String useSpammerCollection = Play.application()
			.configuration().getString("useSpammerCollection");
	private final static String useImageWithNoTags = Play.application()
			.configuration().getString("useImageWithNoTags");
	// Minimum tags that an image should have to avoid asking to the
	// users for new tags
	private static Integer minimumTags = Integer.parseInt(Play.application()
			.configuration().getString("minimumTags"));

	private final static Integer collection = Play.application()
			.configuration().getInt("user.collection");
	private final static Integer botCollection = Play.application()
			.configuration().getInt("bot.collection");
	private final static Integer spammerCollection = Play.application()
			.configuration().getInt("spammer.collection");
	private final static Integer supportingCollection = Play.application()
			.configuration().getInt("supporting.collection");
	private final static Integer supportingRatio = Play.application()
			.configuration().getInt("supporting.ratio");

	private final static String policy = Play.application().configuration()
			.getString("user.policy");
	private final static String botPolicy = Play.application().configuration()
			.getString("bot.policy");
	private final static String spammerPolicy = Play.application()
			.configuration().getString("spammer.policy");
	private final static String supportingPolicy = Play.application()
			.configuration().getString("supporting.policy");

	private final static Integer maxElements = Play.application()
			.configuration().getInt("user.maxElements");
	private final static Integer spammerMaxElements = Play.application()
			.configuration().getInt("spammer.maxElements");
	private final static Integer botMaxElements = Play.application()
			.configuration().getInt("bot.maxElements");
	private final static Integer supportingMaxElements = Play.application()
			.configuration().getInt("supporting.maxElements");

	public ObjectNode getPriorityTask() {
		if (priorityTaskQueue.size() > 0) {
			return priorityTaskQueue.remove(0);
		}
		return null;
	}

	public boolean hasImages(final String name) {
		// TODO fixa su tutte le code
		return queueImages.size() > 0;
	}

	public ObjectNode getImage(final String name) {
		// TODO Auto-generated method stub
		return queueImages.remove(0);
	}

	public boolean hasImagesOrTasks() {
		return (priorityTaskQueue.size() == 0
				&& queueImages.size() == 0);
	}

	public void addToPriorityTask(final ObjectNode guessWord) {
		priorityTaskQueue.add(guessWord);
	}



	public void taskSetInitialization(
			final Room roomChannel, final Integer maxRound,
			final boolean isSinglePlayer,
			final boolean hasBot)
					throws Error, Exception {
		int uploadedTasks = 0;
		try {
			uploadedTasks = retrieveTasks(maxRound, roomChannel);
		} catch (final Exception e) {
			LoggerUtils.error("CMS", "Unable to read tasks");
		}

		int tasksToAdd = maxRound - uploadedTasks;
		if (tasksToAdd > 0 && useImageWithNoTags.equals("true")) {
			uploadedTasks = retrieveImagesWithoutTag(tasksToAdd,
					roomChannel, uploadedTasks > 0, uploadedTasks);

		}
		tasksToAdd = maxRound - uploadedTasks;
		if (tasksToAdd > 0) {
			retrieveImages(tasksToAdd, roomChannel,
					uploadedTasks > 0, hasBot,
					isSinglePlayer);

		}

		LoggerUtils.debug("CMS", "Task init from CMS end");
	}

	// private void retrieveImagesCERT(final Integer tasksToAdd,
	// final Room roomChannel,
	// final boolean taskSent) throws Exception {
	//
	// List<ChooseImageTag> imgtgs = new ArrayList<>();
	// final List<ChooseImageTag> imgtgsDress;
	// try {
	// LoggerUtils.debug("CMS", "Requested image list to CMS");
	//
	// final int tot = tasksToAdd - 3;
	// imgtgs = CMS.getChoose(1, "3", policy);
	// imgtgsDress = CMS.getChoose(4, String.valueOf(tot), policy);
	// imgtgs.addAll(imgtgsDress);
	//
	// LoggerUtils.debug("CMS", "Requested image list to CMS end");
	// } catch (final Exception e) {
	// throw new Exception("[CMS] The request to the CMS is malformed", e);
	// }
	//
	// Collections.shuffle(imgtgs);
	//
	// for (final ChooseImageTag imgtg : imgtgs) {
	//
	// addImgToQueue(imgtg, roomChannel, taskSent);
	//
	// }
	//
	// }

	private void addImgToQueue(final ChooseImageTag imgtg,
			final Room roomChannel, boolean taskSent,
			final List<ObjectNode> queue) {
		final Integer id = imgtg.getImage();

		final ObjectNode guessWord = Json.newObject();
		guessWord.put("type", "task");
		guessWord.put("id", String.valueOf(id));
		// Find the valid tags for this task.

		try {
			buildGuessWordSegment(guessWord, imgtg.getTag(), CMS.getImage(id));
		} catch (final CMSException e) {
			Logger.error("Unable to read image, ignoring...", e);
		}

		queue.add(guessWord);

		if (!taskSent) {
			taskSent = true;
			LoggerUtils.debug("CMS", "Send task aquired for image:" + id
					+ ", rooomChanel: " + roomChannel);
			sendTaskAcquired(roomChannel);
		}

	}


	private void retrieveImages(final Integer tasksToAdd,
			final Room roomChannel, final boolean taskSent, final boolean hasBot, final boolean isSinglePlayer) throws Exception {

		List<ChooseImageTag> imgtgs;
		List<ChooseImageTag> imgtgsSupporting = null;
		List<ChooseImageTag> imgtgsSpammer = null;
		List<ChooseImageTag> imgtgsBot = null;
		try {
			LoggerUtils.debug("CMS", "Requested image list to CMS");

			imgtgs = CMS.getChoose(collection, tasksToAdd, policy);

			if (useSupportingCollection.equals("true") && !isSinglePlayer) {
				imgtgsSupporting = CMS.getChoose(supportingCollection,
						supportingMaxElements, supportingPolicy);
			}

			if (hasBot) {
				imgtgsBot = CMS.getChoose(botCollection, botMaxElements,
						botPolicy);
			}
			if (useSpammerCollection.equals("true")) {
				imgtgsSpammer = CMS.getChoose(spammerCollection,
						spammerMaxElements, spammerPolicy);
			}

			LoggerUtils.debug("CMS", "Requested image list to CMS end");
		} catch (final Exception e) {
			throw new Exception("[CMS] The request to the CMS is malformed", e);
		}

		final List<ObjectNode> queueImagesSupport = new ArrayList<>();
		final float ratiofrac = supportingRatio / 100;
		final int numSupport = (int) Math.ceil(tasksToAdd * ratiofrac);


		shuffleAndAdd(roomChannel, taskSent, queueImages, tasksToAdd + 1
				- numSupport, imgtgs);
		shuffleAndAdd(roomChannel, taskSent, queueImagesSupport, numSupport,
				imgtgsSupporting);
		queueImages.addAll(queueImagesSupport);
		Collections.shuffle(queueImages);

		shuffleAndAdd(roomChannel, taskSent, queueImagesBot, tasksToAdd,
				imgtgsSpammer);

		shuffleAndAdd(roomChannel, taskSent, queueImagesBot, tasksToAdd,
				imgtgsBot);

	}

	private void shuffleAndAdd(final Room roomChannel, final boolean taskSent,
			final List<ObjectNode> queue, final Integer tasksToAdd,
			final List<ChooseImageTag> imgtgs) {

		if (imgtgs == null) {
			return;
		}
		final Set<Integer> ids = new HashSet<>();
		for (int i = 0; i < tasksToAdd; i++) {

			int idx = new Random().nextInt(imgtgs.size());
			while (ids.contains(idx)) {
				idx = new Random().nextInt(imgtgs.size());
			}
			ids.add(idx);
			final ChooseImageTag random = (imgtgs.get(idx));
			addImgToQueue(random, roomChannel, taskSent, queue);
		}

	}

	private int retrieveImagesWithoutTag(final Integer tasksToAdd,
			final Room roomChannel, boolean taskSent,
			int uploadedTasks) {

		final List<ChooseImage> imgs;
		final List<ChooseImageTag> imgtgs = new ArrayList<>();
		try {
			LoggerUtils.debug("CMS", "Requested image list to CMS");
			imgs = CMS.getChooseImageOnly(collection, tasksToAdd.toString(),
					policy);
			if (imgs != null) {
				for (final ChooseImage imgtg : imgs) {
					if (imgtg.getCount() >= minimumTags) {
						break;
					}
					imgtgs.add(0, new ChooseImageTag(imgtg.getImage(), -1));
				}
			}
			LoggerUtils.debug("CMS", "Requested image list to CMS end");
		} catch (final Exception e) {
			throw new RuntimeException(
					"[CMS] The request to the CMS is malformed");
		}

		for (final ChooseImageTag imgtg : imgtgs) {
			// Save information related to the image
			final Integer id = imgtg.getImage();

			final ObjectNode guessWord = Json.newObject();
			guessWord.put("type", "task");
			guessWord.put("id", String.valueOf(id));
			// Find the valid tags for this task.

			try {
				buildGuessWordSegment(guessWord, imgtg.getTag(),
						CMS.getImage(id));
				uploadedTasks = uploadedTasks + 1;
			} catch (final CMSException e) {
				Logger.error("Unable to read image, ignoring...", e);
			}

			queueImages.add(guessWord);

			if (!taskSent) {
				taskSent = true;
				LoggerUtils.debug("CMS", "Send task aquired for image:" + id
						+ ", rooomChanel: " + roomChannel);
				sendTaskAcquired(roomChannel);
			}
		}
		return uploadedTasks;
	}

	private int retrieveTasks(final Integer maxRound,
			final Room roomChannel) {
		boolean taskSent = false;

		int uploadedTasks = 0;

		final List<Task> tasklist;
		try {
			LoggerUtils.debug("CMS", "Requested task list to CMS "
					+ roomChannel);

			tasklist = CMS.getTaskCollection(collection);

			LoggerUtils.debug("CMS", "Requested task list to CMS end "
					+ roomChannel);

		} catch (final CMSException e) {
			throw new RuntimeException(
					"[CMS] Unable to download collection from CMS");
		}

		if (tasklist == null || tasklist.size() == 0) {
			return 0;
		}
		try {
			// for (final Task taskId : tasklist) {
			for (final Task t : tasklist) {
				final Integer taskId = t.getId();
				// final utils.CMS.models.Task t = getTask(taskId);

				final List<MicroTask> uTasks = CMS.getMicroTasks(String
						.valueOf(taskId));
				if (uTasks == null || uTasks.size() > 0) {
					continue;
				}

				final Integer imageId = t.getImage();
				final Image image = CMS.getImage(imageId);

				for (final MicroTask utask : uTasks) {

					final ObjectNode guessWord = Json.newObject();
					guessWord.put("type", "task");
					guessWord.put("id", imageId);

					final String type = utask.getType();
					switch (type) {
					case "tagging":
						buildGuessWordTagging(guessWord, image, utask, taskId);
						addToPriorityTask(guessWord);
						uploadedTasks++;
						if (!taskSent) {
							taskSent = true;
							sendTaskAcquired(roomChannel);
						}
						break;
					case "segmentation":
						final Integer tagid = t.getTag();

						buildGuessWordSegmentTask(guessWord, tagid, image,
								String.valueOf(taskId), utask);

						addToPriorityTask(guessWord);
						uploadedTasks++;
						if (!taskSent) {
							taskSent = true;
							sendTaskAcquired(roomChannel);
						}
						break;
					}
					break;

				}

			}
		} catch (final CMSException e) {
			throw new RuntimeException("[CMS] Unable to download task from CMS");
		}
		return uploadedTasks;
	}

	private static void buildGuessWordSegment(final ObjectNode guessWord,
			final Integer tagId, final Image image) throws CMSException {
		// Add one tag among the ones that have been retrieved following
		// a particular policy
		final Tag t = CMS.getTag(tagId);
		final String tag = t.getName();

		guessWord.put("tag", tag);
		guessWord.put("lang", LanguagePicker.retrieveIsoCode());
		guessWord.put("image", rootUrl + image.getMediaLocator());
		guessWord.put("width", image.getWidth());
		guessWord.put("height", image.getHeight());

	}

	private static void buildGuessWordSegmentTask(final ObjectNode guessWord,
			final Integer tagId, final Image image, final String taskId,
			final MicroTask utask) throws CMSException {
		buildGuessWordSegment(guessWord, tagId, image);
		guessWord.put("utaskid", utask.getId());
		guessWord.put("taskid", taskId);

	}

	/*
	 * Inform the game that at least one task is ready and we can start the game
	 */
	private static void sendTaskAcquired(final Room roomChannel) {
		LoggerUtils.debug("CMS", "CMS sends task aquired... ");
		GameBus.getInstance().publish(
				new GameMessages.GameEvent(GameMessages.composeTaskAcquired(),
						roomChannel));
	}

	private static void buildGuessWordTagging(final ObjectNode guessWord,
			final Image image, final MicroTask utask, final Integer taskId) {
		// devono taggare, non aggiungo tag
		guessWord.put("tag", "");
		guessWord.put("lang", LanguagePicker.retrieveIsoCode());
		guessWord.put("image", rootUrl + image.getMediaLocator());
		guessWord.put("width", image.getWidth());
		guessWord.put("height", image.getHeight());
		guessWord.put("utaskid", utask.getId());
		guessWord.put("taskid", String.valueOf(taskId));

	}

}
