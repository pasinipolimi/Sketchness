package utils.CMS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import play.Logger;
import play.Play;
import utils.CMS.models.Action;
import utils.CMS.models.Collection;
import utils.CMS.models.Image;
import utils.CMS.models.Mask;
import utils.CMS.models.MicroTask;
import utils.CMS.models.Tag;
import utils.CMS.models.Task;

import com.fasterxml.jackson.databind.JsonNode;

public class CMSUtilities {

	private final static String rootUrl = Play.application().configuration().getString("cmsUrl");
	//private final static String rootUrl = "http://localhost:3000";

	public static JSONArray buildImageId(
			final List<utils.CMS.models.Image> images) throws JSONException {
		final JSONArray imageIds = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		JSONObject element;
		int num = 0;

		for (int j = 0; j < images.size(); j++) {
			final utils.CMS.models.Image i = images.get(j);
			sorting = new SortObject() {

			};
			sorting.setId(String.valueOf(i.getId()));
			sorting.setMedia(rootUrl + i.getMediaLocator());

			try {
				num = CMS.getSegmentationCount();
			} catch (final CMSException e) {
				Logger.error("Unable to read segmentations from CMS", e);
				throw new JSONException("Unable to read segmentations from CMS");
			}

			sorting.setNum(num);
			tempList.add(j, sorting);
			num = 0;

		}



		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getNum() > o2.getNum()) {
					return -1;
				} else if (o1.getNum() < o2.getNum()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		while (it.hasNext()) {
			element = new JSONObject();
			final SortObject obj = it.next();
			element.put("id", obj.getId());
			element.put("media", obj.getMedia());
			element.put("numAnnotations", obj.getNum());
			imageIds.put(element);
		}

		return imageIds;
	}

	public static JSONArray buildTaskIds(final List<Task> tasks)
			throws JSONException {
		final JSONArray taskIds = new JSONArray();
		JSONObject element;

		for (final Task t : tasks) {
			element = new JSONObject();
			element.put("id", t.getId());

			// TODO non c'è il tipo
			// element.put("taskType", t.getType());
			String status = "true";
			if (t.getCompleted_at() != null) {
				status = "false";
			}
			element.put("status", status);
			taskIds.put(element);
		}
		return taskIds;
	}

	public static String retriveImgInfo(final utils.CMS.models.Image image)
			throws JSONException {
		final JSONArray info = new JSONArray();

		String media;
		Integer width;
		Integer height;

		media = rootUrl + image.getMediaLocator();
		width = image.getWidth();
		height = image.getHeight();

		List<utils.CMS.models.Tag> tags;
		try {
			tags = CMS.getTagsByImage(image.getId());
		} catch (final CMSException e) {
			Logger.error("Unable to read tags from CMS", e);
			throw new JSONException("Unable to read tags from CMS");
		}
		final JSONArray tagsJ = retrieveTagsInfo(tags);
		final int numSegment = retrieveNumSegments(tags);

		JSONObject element;


		element = new JSONObject();
		element.put("tags", tagsJ);
		element.put("medialocator", media);
		element.put("height", height);
		element.put("width", width);
		element.put("annotations", numSegment);
		info.put(element);
		final String result = info.toString();
		return result;
	}

	private static int retrieveNumSegments(final List<utils.CMS.models.Tag> tags) {
		// TODO Auto-generated method stub
		//return 0;
		return tags.size();
	}

	private static JSONArray retrieveTagsInfo(
			final List<utils.CMS.models.Tag> tagsList)
					throws JSONException {

		final JsonNode valid = null, lang = null, annotationId = null;
		JSONObject element;
		final JSONArray tags = new JSONArray();
		final int count = 0;

		for (final utils.CMS.models.Tag t : tagsList) {

			element = new JSONObject();
			element.put("tagId", t.getId());
			element.put("tag", t.getName());
			// TODO count
			element.put("numAnnotations", count);

			// TOFIX valid
			element.put("valid", valid);

			// TOFIX mettere sempre inglese
			element.put("lang", "eng");

			tags.put(element);

		}

		return tags;
	}

	/**
	 * Retrive info for a specific mask
	 * 
	 * @param jsonMask
	 *            The specific mask which I'm evalueting
	 * @return It's medialocator
	 * @throws JSONException
	 */
	public static String retriveMaskInfo(final Mask mask) throws JSONException {

		final JSONArray info = new JSONArray();

		JSONObject element;

		String media;
		media = rootUrl + mask.getMediaLocator();
		String quality;
		quality = String.valueOf(mask.getQuality());

		element = new JSONObject();
		element.put("medialocator", media);
		element.put("quality", quality);

		info.put(element);
		final String result = info.toString();
		return result;

	}

	public static JSONArray retriveCollectionInfo(final List<Collection> cs)
			throws JSONException {
		final JSONArray collectionIds = new JSONArray();
		JSONObject element;

		for (final Collection c : cs) {
			element = new JSONObject();
			element.put("id", c.getId());
			element.put("name", c.getName());
			collectionIds.put(element);
		}

		return collectionIds;
	}

	public static String retriveCollImages(final Collection c)
			throws JSONException, CMSException {
		final JSONArray imageIds = new JSONArray();
		JSONObject element, finalElement;
		final JSONArray info = new JSONArray();

		/*
		for (final Integer id : c.getImages()) {

			element = new JSONObject();
			element.put("id", id);
			imageIds.put(element);
		}
		*/
		for (final Integer id : c.getImages()) {
			Image im = CMS.getImage(id);
			element = new JSONObject();
			element.put("id", id);
			element.put("media", rootUrl + im.getMediaLocator());
			imageIds.put(element);
		}
		

		finalElement = new JSONObject();
		finalElement.put("images", imageIds);
		info.put(finalElement);
		final String result = info.toString();
		return result;
	}

	public static JSONArray loadFirstGraph() throws JSONException {

		List<Image> images;
		try {
			images = CMS.getImages();
		} catch (final CMSException e) {
			Logger.error("Unable to read images from CMS", e);
			throw new JSONException("");
		}

		final JSONArray graph = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		JSONObject element;
		int num = 0;

		for (int i = 0; i < images.size(); i++) {

			sorting = new SortObject() {
			};
			final Image image = images.get(i);
			sorting.setId(String.valueOf(image.getId()));

			List<Action> semgents;
			try {
				semgents = CMS.getSegmentationsByImage(image.getId());
			} catch (final CMSException e) {
				Logger.error("Unable to read segementations from CMS", e);
				throw new JSONException(
						"Unable to read segementations from CMS");
			}
			num = semgents.size();
			sorting.setNum(num);
			tempList.add(i, sorting);
			num = 0;
			//i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getNum() < o2.getNum()) {
					return -1;
				} else if (o1.getNum() > o2.getNum()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getNum();
		int count = 0;

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getNum() == tmp) {
				count++;
			} else if (obj.getNum() != tmp) {
				element = new JSONObject();
				element.put("occurence", count);
				element.put("annotations", tmp);
				graph.put(element);
				do {
					tmp++;
					count = 0;
					if (obj.getNum() != tmp) {
						element = new JSONObject();
						element.put("occurence", count);
						element.put("annotations", tmp);
						graph.put(element);
					}
				} while (obj.getNum() != tmp);
				count = 1;
			}
		}
		element = new JSONObject();
		element.put("occurence", count);
		element.put("annotations", tmp);
		graph.put(element);

		return graph;
	}

	public static JSONArray downloadStats1() {

		final JSONArray down1 = new JSONArray();
		// SortObject sorting;
		// final ArrayList<SortObject> tempList = new ArrayList<>();
		// final ArrayList<DownObject> tempList2 = new ArrayList<>();
		// JsonNode object;
		//
		// int i = 0;
		// int j = 0;
		// while (i < actions.size()) {
		// object = actions.get(i);
		// if (object.get("type").asText().equals("segmentation")) {
		// if (object.has("user")) {
		// if (object.get("user").has("cubrik_userid")) {
		// sorting = new SortObject() {
		// };
		// sorting.setIdU(object.get("user").get("cubrik_userid")
		// .asInt());
		// sorting.setIdTmp(object.get("id").asInt());
		// sorting.setImgTmp(object.get("imageid").asInt());
		// tempList.add(j, sorting);
		// j++;
		// }
		// }
		// }
		//
		// i++;
		// }
		//
		// Collections.sort(tempList, new Comparator<SortObject>() {
		// @Override
		// public int compare(final SortObject o1, final SortObject o2) {
		// if (o1.getIdU() < o2.getIdU()) {
		// return -1;
		// } else if (o1.getIdU() > o2.getIdU()) {
		// return 1;
		// }
		// return 0;
		// }
		//
		// });
		//
		// final Iterator<SortObject> it = tempList.iterator();
		// int tmp = tempList.get(0).getIdU();
		// DownObject user;
		// StoredStatObj stat;
		// ArrayList<StoredStatObj> elements = new ArrayList<>();
		//
		// while (it.hasNext()) {
		// final SortObject obj = it.next();
		//
		// if (obj.getIdU() == tmp) {
		//
		// stat = new StoredStatObj(obj.getIdTmp(), obj.getImgTmp());
		// elements.add(stat);
		// } else if (obj.getIdU() != tmp) {
		// user = new DownObject() {
		// };
		// user.setId(tmp);
		// user.setElement(elements);
		// tempList2.add(user);
		// do {
		// tmp++;
		//
		// } while (obj.getIdU() != tmp);
		// elements = new ArrayList<>();
		// stat = new StoredStatObj(obj.getIdTmp(), obj.getImgTmp());
		// elements.add(stat);
		// }
		// }
		// user = new DownObject() {
		// };
		// user.setId(tmp);
		// user.setElement(elements);
		// tempList2.add(user);
		//
		// final Iterator<DownObject> it2 = tempList2.iterator();
		//
		// JSONObject son;
		// JSONArray body;
		// JSONObject content;
		//
		// while (it2.hasNext()) {
		// son = new JSONObject();
		// body = new JSONArray();
		// final DownObject obj = it2.next();
		// son.put("user", obj.getId());
		//
		// final Iterator<StoredStatObj> it3 = obj.getElement().iterator();
		//
		// while (it3.hasNext()) {
		// final StoredStatObj obj2 = it3.next();
		// content = new JSONObject();
		// content.put("segment", obj2.getId1());
		// content.put("image", obj2.getId2());
		// body.put(content);
		// }
		// son.put("sketch", body);
		// down1.put(son);
		// }

		return down1;
	}

	public static JSONArray loadSecondGraph() {
		final JSONArray graph = new JSONArray();
		// SortObject sorting;
		// final ArrayList<SortObject> tempList = new ArrayList<>();
		// final ArrayList<SortObject> tempList2 = new ArrayList<>();
		// JsonNode object;
		// JSONObject element;
		//
		// int i = 0;
		// int j = 0;
		// while (i < actions.size()) {
		// object = actions.get(i);
		// if (object.get("type").asText().equals("segmentation")) {
		// if (object.has("user")) {
		// if (object.get("user").has("cubrik_userid")) {
		// sorting = new SortObject() {
		// };
		// sorting.setIdU(object.get("user").get("cubrik_userid")
		// .asInt());
		// tempList.add(j, sorting);
		// j++;
		// }
		// }
		// }
		//
		// i++;
		// }
		//
		// Collections.sort(tempList, new Comparator<SortObject>() {
		// @Override
		// public int compare(final SortObject o1, final SortObject o2) {
		// if (o1.getIdU() < o2.getIdU()) {
		// return -1;
		// } else if (o1.getIdU() > o2.getIdU()) {
		// return 1;
		// }
		// return 0;
		// }
		//
		// });
		//
		// final Iterator<SortObject> it = tempList.iterator();
		// int tmp = tempList.get(0).getIdU();
		// int count = 0;
		//
		// while (it.hasNext()) {
		// final SortObject obj = it.next();
		//
		// if (obj.getIdU() == tmp) {
		// count++;
		// } else if (obj.getIdU() != tmp) {
		// sorting = new SortObject() {
		// };
		// sorting.setIdU(tmp);
		// sorting.setNum(count);
		// tempList2.add(sorting);
		// do {
		// tmp++;
		//
		// } while (obj.getIdU() != tmp);
		// count = 1;
		// }
		// }
		// sorting = new SortObject() {
		// };
		// sorting.setIdU(tmp);
		// sorting.setNum(count);
		// tempList2.add(sorting);
		//
		// Collections.sort(tempList2, new Comparator<SortObject>() {
		// @Override
		// public int compare(final SortObject o1, final SortObject o2) {
		// if (o1.getNum() < o2.getNum()) {
		// return -1;
		// } else if (o1.getNum() > o2.getNum()) {
		// return 1;
		// }
		// return 0;
		// }
		//
		// });
		//
		// final Iterator<SortObject> it2 = tempList2.iterator();
		// tmp = 0;
		// count = 0;
		//
		// while (it2.hasNext()) {
		// final SortObject obj = it2.next();
		//
		// if (obj.getNum() == tmp) {
		// count++;
		// } else if (obj.getNum() != tmp) {
		// element = new JSONObject();
		// element.put("users", count);
		// element.put("images", tmp);
		// graph.put(element);
		// do {
		// tmp++;
		// count = 0;
		// if (obj.getNum() != tmp) {
		// element = new JSONObject();
		// element.put("users", count);
		// element.put("images", tmp);
		// graph.put(element);
		// }
		// } while (obj.getNum() != tmp);
		// count = 1;
		// }
		// }
		// element = new JSONObject();
		// element.put("users", count);
		// element.put("images", tmp);
		// graph.put(element);

		return graph;
	}

	public static JSONArray downloadStats2() {
		final JSONArray down2 = new JSONArray();
		// SortObject sorting;
		// final ArrayList<SortObject> tempList = new ArrayList<>();
		// final ArrayList<DownObject> tempList2 = new ArrayList<>();
		// JsonNode object;
		//
		// int i = 0;
		// int j = 0;
		// while (i < actions.size()) {
		// object = actions.get(i);
		// if (object.get("type").asText().equals("segmentation")) {
		// if (object.has("user")) {
		// if (object.get("user").has("cubrik_userid")) {
		// sorting = new SortObject() {
		// };
		// sorting.setIdU(object.get("user").get("cubrik_userid")
		// .asInt());
		// sorting.setIdTmp(object.get("id").asInt());
		// sorting.setImgTmp(object.get("imageid").asInt());
		// tempList.add(j, sorting);
		// j++;
		// }
		// }
		// }
		//
		// i++;
		// }
		//
		// Collections.sort(tempList, new Comparator<SortObject>() {
		// @Override
		// public int compare(final SortObject o1, final SortObject o2) {
		// if (o1.getImgTmp() < o2.getImgTmp()) {
		// return -1;
		// } else if (o1.getImgTmp() > o2.getImgTmp()) {
		// return 1;
		// }
		// return 0;
		// }
		//
		// });
		//
		// final Iterator<SortObject> it = tempList.iterator();
		// int tmp = tempList.get(0).getImgTmp();
		// DownObject image;
		// StoredStatObj stat;
		// ArrayList<StoredStatObj> elements = new ArrayList<>();
		//
		// while (it.hasNext()) {
		// final SortObject obj = it.next();
		//
		// if (obj.getImgTmp() == tmp) {
		//
		// stat = new StoredStatObj(obj.getIdTmp(), obj.getIdU());
		// elements.add(stat);
		// } else if (obj.getImgTmp() != tmp) {
		// image = new DownObject() {
		// };
		// image.setId(tmp);
		// image.setElement(elements);
		// tempList2.add(image);
		// do {
		// tmp++;
		//
		// } while (obj.getImgTmp() != tmp);
		// elements = new ArrayList<>();
		// stat = new StoredStatObj(obj.getIdTmp(), obj.getIdU());
		// elements.add(stat);
		// }
		// }
		// image = new DownObject() {
		// };
		// image.setId(tmp);
		// image.setElement(elements);
		// tempList2.add(image);
		//
		// final Iterator<DownObject> it2 = tempList2.iterator();
		//
		// JSONObject son;
		// JSONArray body;
		// JSONObject content;
		//
		// while (it2.hasNext()) {
		// son = new JSONObject();
		// body = new JSONArray();
		// final DownObject obj = it2.next();
		// son.put("image", obj.getId());
		//
		// final Iterator<StoredStatObj> it3 = obj.getElement().iterator();
		//
		// while (it3.hasNext()) {
		// final StoredStatObj obj2 = it3.next();
		// content = new JSONObject();
		// content.put("segment", obj2.getId1());
		// content.put("user", obj2.getId2());
		// body.put(content);
		// }
		// son.put("sketch", body);
		// down2.put(son);
		// }

		return down2;
	}

	public static String retriveTaskInfo(final String taskId)
			throws JSONException {

		List<MicroTask> mts;
		try {
			mts = CMS.getMicroTasks(taskId);
		} catch (final CMSException e) {
			Logger.error("Unable to read microtask from CMS", e);
			throw new JSONException("Unable to read microtask from CMS");
		}

		final JSONArray info = new JSONArray();

		JSONObject element, finalElement;
		final JSONArray utasks = new JSONArray();


		for (final MicroTask utask : mts) {
			element = new JSONObject();
			element.put("id", String.valueOf(utask.getId()));
			element.put("status", utask.getStatus());
			element.put("utaskType", utask.getType());
			utasks.put(element);
		}

		finalElement = new JSONObject();
		finalElement.put("utasks", utasks);
		info.put(finalElement);
		final String result = info.toString();
		return result;
	}

	public static JSONArray retriveTaskId(final List<Task> tasks)
			throws JSONException {
		final JSONArray taskIds = new JSONArray();
		final JsonNode object;
		JSONObject element;
		int i = 0;

		for (final Task t : tasks) {
			element = new JSONObject();
			element.put("id", t.getId());
			// TODO togliere dalla ui il tasktype
			// element.put("taskType", object.get("taskType"));
			element.put("status", t.getStatus());
			taskIds.put(element);
			i++;
		}
		return taskIds;
	}

	public static HashSet<String> getTags(final String imageID)
			throws JSONException {
		final HashSet<String> res = new HashSet<>();
		List<Tag> tags;
		try {
			tags = CMS.getTagsByImage(Integer.valueOf(imageID));
		} catch (final Exception e) {
			Logger.error("Unable to read tags from CMS", e);
			throw new JSONException("Unable to read tags from CMS");
		}
		for (final Tag t : tags) {
			res.add(t.getName());
		}
		return res;

	}

}
