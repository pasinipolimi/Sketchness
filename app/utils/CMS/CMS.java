package utils.CMS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import play.Logger;
import play.Play;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import utils.JsonReader;
import utils.LanguagePicker;
import utils.gamebus.GameBus;
import utils.gamebus.GameEventType;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.Room;

/**
 * Wrapper for the CMS API
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class CMS {

    //Url of the CMS system
    private final static String rootUrl = Play.application().configuration().getString("cmsUrl");
    private final static String oauthConsumerKey = Play.application().configuration().getString("oauthConsumerKey");

    public static void closeUTask(Integer uTaskID, Integer actionId) {
        if (uTaskID != null) {
            String request = rootUrl + "/wsmc/utask/" + uTaskID + "/" + actionId + "/close";
            WS.url(request).setContentType("application/x-www-form-urlencoded").put("");
            Logger.debug("[CMS] Closing uTask " + uTaskID);
        }
    }

    public static void closeTask(Integer taskID) {
        if (taskID != null) {
            String request = rootUrl + "/wsmc/task/" + taskID + "/close";
            WS.url(request).setContentType("application/x-www-form-urlencoded").put("");
            Logger.debug("[CMS] Closing Task " + taskID);
        }
    }

    public static Integer segmentation(ObjectNode finalTraces, String username, Integer session) throws MalformedURLException, IOException, JSONException {
        String id = finalTraces.get("id").getTextValue();
        String label = finalTraces.get("label").getTextValue();
        textAnnotation(finalTraces, username, session);
        String traces = finalTraces.get("traces").toString();
        String history = finalTraces.get("history").toString();

        String urlParameters = "ta_name=tag&ta_val=" + label + "&content_type=segmentation&&user_id=" + username + "&language=" + LanguagePicker.retrieveIsoCode() + "&session_id=" + session + "&polyline_r=" + traces + "&polyline_h=" + history + "&oauth_consumer_key=" + oauthConsumerKey;
        String request = rootUrl + "/wsmc/image/" + id + "/segmentation.json";
        F.Promise<WS.Response> returned = WS.url(request).setContentType("application/x-www-form-urlencoded").post(urlParameters);
        JSONObject actionInfo = new JSONObject(returned.get().getBody());
        Integer actionId = Integer.parseInt(actionInfo.get("vid").toString());
        Logger.debug("[CMS] Storing segmentation with action " + actionId + " for image with id " + id + " and tag " + label);
        return actionId;
    }

    public static Integer textAnnotation(ObjectNode finalTraces, String username, Integer session) throws MalformedURLException, IOException, JSONException {
        JsonReader jsonReader = new JsonReader();
        String label = finalTraces.get("label").getTextValue();
        String id = finalTraces.get("id").getTextValue();
        JsonNode image = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image/" + id + ".json");
        JsonNode imageSegments = image.get("descriptions");
        HashSet<String> available = retrieveTags(imageSegments);
        //If the tag is not present in the list of the available tags, add it to 
        //the list
        if (!available.contains(label)) {
            //Just the list of the single, current tags is saved under a content descriptor called availableTags
            String urlParameters = "ta_name=tag&ta_val=" + label + "&content_type=availableTags&&user_id=" + username + "&language=" + LanguagePicker.retrieveIsoCode() + "&session_id=" + session + "&oauth_consumer_key=" + oauthConsumerKey;
            String request = rootUrl + "/wsmc/image/" + id + "/textAnnotation.json";
            WS.url(request).setContentType("application/x-www-form-urlencoded").post(urlParameters);
            Logger.debug("[CMS] Adding new tag: " + label + " for image with id " + id);
        }
        //In any case, record that the player has tagged the image with this tag
        String urlParameters = "ta_name=tag&ta_val=" + label + "&content_type=tagging&&user_id=" + username + "&language=" + LanguagePicker.retrieveIsoCode() + "&session_id=" + session + "&oauth_consumer_key=" + oauthConsumerKey;
        String request = rootUrl + "/wsmc/image/" + id + "/textAnnotation.json";
        F.Promise<WS.Response> returned = WS.url(request).setContentType("application/x-www-form-urlencoded").post(urlParameters);
        JSONObject actionInfo = new JSONObject(returned.get().getBody());
        Integer actionId = Integer.parseInt(actionInfo.get("vid").toString());
        Logger.debug("[CMS] Storing textAnnotation with action " + actionId + " for image with id " + id + " and tag " + label);
        return actionId;
    }

    public static Integer openSession() throws Error {
        String request = rootUrl + "/wsmc/session.json";
        Logger.debug("[CMS] Opening a new session...");
        F.Promise<WS.Response> returned = WS.url(request).setContentType("application/x-www-form-urlencoded").post("oauth_consumer_key=" + oauthConsumerKey);

        String sessionId = returned.get().getBody();
        sessionId = sessionId.replace("[\"", "");
        sessionId = sessionId.replace("\"]", "");
        Logger.debug("[CMS] Retrieved session " + sessionId);
        return Integer.valueOf(sessionId);
    }

    public static void closeSession(Integer sessionId) throws Error {
        String request = rootUrl + "/wsmc/session/" + sessionId;
        WS.url(request).setContentType("application/x-www-form-urlencoded").put("state=0&oauth_consumer_key=" + oauthConsumerKey);
        Logger.debug("[CMS] Closing session " + sessionId);
    }

    public static void postAction(Integer sessionId, String actionType, String username, String log) throws Error {
        String request = rootUrl + "/wsmc/action";
        if (log.equals("")) {
            log = "{}";
        }
        String parameters = "session_id=" + sessionId + "&action_type=" + actionType + "&user_id=" + username + "&oauth_consumer_key=" + oauthConsumerKey + "&action_log=" + log;
        WS.url(request).setContentType("application/x-www-form-urlencoded").post(parameters);
        Logger.debug("[CMS] Action " + actionType + " for session " + sessionId + ": " + log);
    }
    
    public static void fixGroundTruth(Integer sessionId, HashSet<ObjectNode> priorityTaskHashSet, HashSet<ObjectNode> taskHashSet, Room roomChannel) {
        JsonReader jsonReader = new JsonReader();
        JsonNode retrievedImages;
        HashMap<String,ObjectNode> temporary = new HashMap<>();
        retrievedImages = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image.json");
         if (retrievedImages != null) {
            //For each image
            for (JsonNode item : retrievedImages) {
                if (item.getElements().hasNext()) {
                    //Save information related to the image
                    String id = item.get("id").asText();
                    String url = rootUrl + item.get("mediaLocator").asText();
                    Integer width = item.get("width").asInt();
                    Integer height = item.get("height").asInt();

                    //Get all the segments that have been stored for the image
                    JsonNode imageSegments = item.get("descriptions");
                    HashSet<String> tags = new HashSet<>();
                    ObjectNode guessWord = Json.newObject();
                    guessWord.put("type", "task");
                    guessWord.put("id", id);

                    //Find the valid tags for this task.
                    if (imageSegments != null) {
                        tags = retrieveTags(imageSegments);
                    }
                    //Add one tag among the ones that have been retrieved following a particular policy
                    guessWord.put("tag", chooseTag(tags));
                    guessWord.put("lang", LanguagePicker.retrieveIsoCode());
                    guessWord.put("image", url);
                    guessWord.put("width", width);
                    guessWord.put("height", height);
                    temporary.put(id, guessWord);
                }
            }
            JsonNode processedSession = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/session/"+sessionId+".json");
            if(processedSession!=null) {
                processedSession = processedSession.get("actions");
                for (JsonNode item : processedSession) {
                    if(item.get("type").asText().equals("segmentation")) {
                        JsonNode segmentation = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/action/"+item.get("id").asText()+".json");
                        if(temporary.containsKey(segmentation.get("image").asText()))
                            temporary.remove(segmentation.get("image").asText());
                    }
                }
            }
            for (Map.Entry pairs : temporary.entrySet()) {
                taskHashSet.add((ObjectNode)pairs.getValue());
            }
            sendTaskAcquired(roomChannel);
         }
    }

    /**
     * Retrieving data from the CMS [TODO] Right now we are not retrieving based
     * on the requirements of our tasks such as completing tasks that have not
     * been already faced and so on. We will add this feature in the future.
    *
     */
    public static void taskSetInitialization(HashSet<ObjectNode> priorityTaskHashSet, HashSet<ObjectNode> taskHashSet, Room roomChannel) throws Error {

        JsonReader jsonReader = new JsonReader();
        JsonNode retrievedTasks = null;
        JsonNode retrievedImages = null;
        //[TODO] Fail safe in case of not being able to retrieve the instances
        try {
            retrievedTasks = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/task.json");
            retrievedImages = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image.json");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("[CMS] The request to the CMS is malformed");
        }


        //Fill the set of task to be performed with the task that has been 
        //explicitly declared

        if (retrievedTasks != null) {
            retrievedTasks = retrievedTasks.get("task");
            for (JsonNode item : retrievedTasks) {
                if (item.getElements().hasNext()) {
                    //If the task is still open
                    if (item.get("status").asInt() == 1) {
                        String taskId = item.get("id").getTextValue();
                        JsonNode uTasks = item.get("utask");
                        item = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/task/" + item.get("id").getTextValue() + ".json");
                        item = item.get("task");
                        Logger.debug("[CMS] Retrieved open task " + item.toString());
                        String id = item.get("image").getElements().next().getElements().next().asText();
                        if (uTasks != null) {
                            //Retrieve the first uTask for the current task and assign it
                            for (JsonNode utask : uTasks) {
                                if (utask.getElements().hasNext()) {
                                    if (utask.get("status").asInt() == 1) {
                                        utask = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/utask/" + utask.get("id").getTextValue() + ".json");
                                        JsonNode image = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image/" + id + ".json");
                                        utask = utask.get("uTask");
                                        ObjectNode guessWord = Json.newObject();
                                        guessWord.put("type", "task");
                                        guessWord.put("id", id);
                                        //Change the task to assign based on the kind of task that has to be performed
                                        //for now just tagging and segmentation are supported for the images.
                                        switch (utask.get("taskType").asText()) {
                                            case "tagging":
                                                guessWord.put("tag", chooseTag(null));
                                                guessWord.put("lang", LanguagePicker.retrieveIsoCode());
                                                guessWord.put("image", rootUrl + image.get("mediaLocator").asText());
                                                guessWord.put("width", image.get("width").asInt());
                                                guessWord.put("height", image.get("height").asInt());
                                                guessWord.put("utaskid", utask.get("id").asInt());
                                                guessWord.put("taskid", taskId);
                                                priorityTaskHashSet.add(guessWord);
                                                sendTaskAcquired(roomChannel);
                                                break;
                                            case "segmentation":
                                                HashSet<String> tags;
                                                //Get all the segments that have been stored for the image
                                                JsonNode imageSegments = image.get("descriptions");
                                                if (imageSegments != null) {
                                                    tags = retrieveTags(imageSegments);
                                                    guessWord.put("tag", chooseTag(tags));
                                                    guessWord.put("lang", LanguagePicker.retrieveIsoCode());
                                                    guessWord.put("image", rootUrl + image.get("mediaLocator").asText());
                                                    guessWord.put("width", image.get("width").asInt());
                                                    guessWord.put("height", image.get("height").asInt());
                                                    guessWord.put("utaskid", utask.get("id").asInt());
                                                    guessWord.put("taskid", taskId);
                                                    priorityTaskHashSet.add(guessWord);
                                                    sendTaskAcquired(roomChannel);
                                                }
                                                break;
                                        }
                                        break;
                                    }
                                }
                            }
                        } //There are no more uTasks left, close the task
                        else {
                            closeTask(Integer.parseInt(taskId));
                        }
                    }
                }
            }
        }



        if (retrievedImages != null) {
            //For each image
            for (JsonNode item : retrievedImages) {
                if (item.getElements().hasNext()) {
                    //Save information related to the image
                    String id = item.get("id").asText();
                    String url = rootUrl + item.get("mediaLocator").asText();
                    Integer width = item.get("width").asInt();
                    Integer height = item.get("height").asInt();

                    //Get all the segments that have been stored for the image
                    JsonNode imageSegments = item.get("descriptions");
                    HashSet<String> tags = new HashSet<>();
                    ObjectNode guessWord = Json.newObject();
                    guessWord.put("type", "task");
                    guessWord.put("id", id);

                    //Find the valid tags for this task.
                    if (imageSegments != null) {
                        tags = retrieveTags(imageSegments);
                    }
                    //Add one tag among the ones that have been retrieved following a particular policy
                    guessWord.put("tag", chooseTag(tags));
                    guessWord.put("lang", LanguagePicker.retrieveIsoCode());
                    guessWord.put("image", url);
                    guessWord.put("width", width);
                    guessWord.put("height", height);
                    taskHashSet.add(guessWord);
                    sendTaskAcquired(roomChannel);
                }
            }
        } else {
            throw new Error("[GAME]: Cannot retrieve the tasks from the CMS.");
        }
    }

    /*
     * Inform the game that at least one task is ready and we can start the game
     */
    private static void sendTaskAcquired(Room roomChannel) {
        GameMessages.GameEvent taskAcquired = new GameMessages.GameEvent(roomChannel, GameEventType.taskAcquired);
        GameBus.getInstance().publish(taskAcquired);
    }

    public static HashSet<String> retrieveTags(JsonNode imageSegments) {
        JsonReader jsonReader = new JsonReader();
        HashSet<String> tags = new HashSet<>();
        imageSegments = imageSegments.get("availableTags");
        if (imageSegments != null) {
            if (imageSegments.getElements().hasNext()) {
                for (JsonNode segment : imageSegments) {
                    //Retrieve the content descriptor
                    if (null != segment) {
                        JsonNode retrieved = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/content/" + segment.get("id").getTextValue() + ".json");
                        retrieved = retrieved.get("itemAnnotations").get(0);
                        //If the annotation is a tag and is in the same language as the one defined in the system, add the tag to the list of possible tags
                        if ((retrieved.get("name").asText().equals("tag")) && (retrieved.get("language").asText().equals(LanguagePicker.retrieveIsoCode()) || LanguagePicker.retrieveIsoCode().equals(""))) {
                            tags.add(retrieved.get("value").asText());
                        }
                    }
                }
            }
        }
        return tags;
    }

    /**
     * Retrive all the images' Ids that are stored in the system
     *
     * @param jsonImages    JsonNode off all images
     * @return              JSONArray with all the ids sorted depending on number of annotations
     * @throws JSONException
     */
    public static JSONArray retriveImageId(JsonNode jsonImages) throws JSONException{

        JSONArray imageIds= new JSONArray();
        SortObject sorting;
        ArrayList<SortObject> tempList = new ArrayList<>();
        JsonNode object;
        JSONObject element;
        int num = 0;

        int i=0;
        while(i<jsonImages.size()){
            sorting = new SortObject() {};
            object = jsonImages.get(i);
            sorting.setId(object.get("id").asText());
            sorting.setMedia(rootUrl + object.get("mediaLocator").asText());
            if(object.has("descriptions")){
                if(object.get("descriptions").has("segmentation")){
                    num = object.get("descriptions").get("segmentation").size();
                }
                else{
                    num = 0;
                }
            }
            sorting.setNum(num);
            tempList.add(i, sorting);
            num = 0;
            i++;
        }

        Collections.sort(tempList, new Comparator<SortObject>() {
            @Override public int compare(SortObject o1, SortObject o2) {
                if (o1.getNum() > o2.getNum()) {
                    return -1;
                } else if (o1.getNum() < o2.getNum()) {
                    return 1;
                }
                return 0;
            }

        });

        Iterator<SortObject> it = tempList.iterator();
        while(it.hasNext())
        {
            element = new JSONObject();
            SortObject obj = it.next();
            element.put("id", obj.getId());
            element.put("media", obj.getMedia());
            imageIds.put(element);
        }

        return imageIds;
    }



    /**
     * Retrive all the tasks' ids that are stored in the system
     *
     * @param jsonTask  JsonNode off all tasks
     * @return          JSONArray with all the ids
     * @throws JSONException
     */
    public static JSONArray retriveTaskId(JsonNode jsonTask) throws JSONException{

        JSONArray taskIds= new JSONArray();
        JsonNode object;
        JSONObject  element;
        int i=0;

        while(i<jsonTask.get("task").size()){
            element = new JSONObject();
            object = jsonTask.get("task").get(i);
            element.put("id", object.get("id"));
            element.put("taskType", object.get("taskType"));
            taskIds.put(element);
            i++;
        }
        return taskIds;
    }

    /**
     * Retrive the stats of the system
     *
     * @param jsonImages    JsonNode off all images
     * @return              JSONArray with number of tags and number of segmentation
     * @throws JSONException
     */
    public static JSONArray retriveStats(JsonNode jsonImages) throws JSONException{

        JSONArray values= new JSONArray();
        JsonNode object;
        JsonNode descObj;
        JsonNode tmpArr;
        JSONObject element = new JSONObject();
        int numTag=0;
        int numSegment=0;
        int i=0;

        while(i<jsonImages.size()){
            object = jsonImages.get(i);
            if(object.has("descriptions")){
                descObj=  object.get("descriptions");
                if(descObj.has("availableTags")){
                    tmpArr = descObj.get("availableTags");
                    numTag= numTag + tmpArr.size();
                }//if se descObject ha dei availableTags
                if(descObj.has("segmentation")){
                    tmpArr = descObj.get("segmentation");
                    numSegment = numSegment + tmpArr.size();
                }
            }//if se c'è il campo description
            i++;
        }//fine while
        element.append("numTag", numTag);
        element.append("numSegment", numSegment);
        values.put(element);
        return values;
    }

    /**
     * Retrive info for a specific image
     *
     * @param jsonImages    The specific image which I'm evalueting
     * @return              It's tags, medialocator and number of annotation
     * @throws JSONException
     */
    public static String retriveImgInfo(JsonNode jsonImages)throws JSONException{

        JSONArray info= new JSONArray();
        JsonReader jsonReader = new JsonReader();
        JsonNode itemTag;
        JsonNode segmentArr,object2, tagId;
        JsonNode descObj;
        JsonNode tagArr;
        JSONObject element;
        JSONArray tags = new JSONArray();
        int numSegment=0;
        int j=0;
        String tmpTag;
        JsonNode media;

        media = jsonImages.get("mediaLocator");
        if(jsonImages.has("descriptions")){
            descObj=  jsonImages.get("descriptions");
            if(descObj.has("availableTags")){
                tagArr = descObj.get("availableTags");
                while(j<tagArr.size()){
                    tagId = tagArr.get(j);
                    tmpTag = tagId.get("id").toString();
                    tmpTag = tmpTag.substring(1, tmpTag.length() - 1);
                    itemTag = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/content/" + tmpTag + ".json");
                    object2 = itemTag.get("itemAnnotations").get(0).get("value");
                    element= new JSONObject();
                    element.put("tag", object2);
                    tags.put(element);
                    j++;
                }//fine while
            }//if se descObject ha dei availableTags
            if(descObj.has("segmentation")){
                segmentArr = descObj.get("segmentation");
                numSegment = segmentArr.size();
            }
        }//if se c'è il campo description
        element= new JSONObject();
        element.put("tags", tags);
        element.put("medialocator", media);
        element.put("annotations", numSegment);
        info.put(element);
        String result = info.toString();
        return result;
    }

    /**
     * Retrive the microTask of a particular task
     * @param jsonTasks     JsonNode of all the Tasks
     * @param selected      id of the task that I want
     * @return              the status of the task (open or closed) and the information of its microtask (id, type, status)
     * @throws JSONException
     */
    public static String retriveTaskInfo(JsonNode jsonTasks, String selected) throws JSONException{

        JSONArray info= new JSONArray();
        JsonNode object,object2;
        JsonNode taskObj;
        JSONObject element;
        JSONArray uTasks = new JSONArray();
        int i=0;
        int j=0;
        String tmpId;
        JsonNode status = null;

        object = jsonTasks.get("task");
        while(i<object.size()){
            object2 = object.get(i);
            tmpId = object2.get("id").asText();
            if(tmpId.equals(selected)){
                status = object2.get("status");
                if(object2.has("utask")){
                    element= new JSONObject();
                    element.put("utask", "full");
                    taskObj=  object2.get("utask");
                    while(j<taskObj.size()){
                        element= new JSONObject();
                        element.put("id", taskObj.get(j).get("id"));
                        element.put("taskType", taskObj.get(j).get("taskType"));
                        element.put("status", taskObj.get(j).get("status"));
                        uTasks.put(element);
                        j++;
                    }
                    break;
                }//if se c'è il campo uTask
                else{
                    element= new JSONObject();
                    element.put("utask", "empty");
                    uTasks.put(element);
                }
            }
            i++;
        }
        element= new JSONObject();
        element.put("status", status);
        element.put("uTasks", uTasks);
        info.put(element);
        String result = info.toString();
        return result;
    }

    /**
     * Close a particulr task
     *
     * @param taskID        id of the task that I want to close
     * @throws IOException
     */
    public static void closeTask2(String taskID) throws IOException{
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(rootUrl + "/wsmc/task/" + taskID +"/close");
        CloseableHttpResponse response1 = httpclient.execute(httpPut);

        HttpEntity entity1 = response1.getEntity();
        EntityUtils.consume(entity1);
        response1.close();
    }

    /**
     * Open a new task
     *
     * @param taskType      The type (segmentation or tagging) of the new task that i want to open
     * @param selectedImg   The id of the image which will be associated to the new task
     * @return              The id of the new task
     * @throws IOException
     * @throws JSONException
     */
    public static String addTask(String taskType, String selectedImg) throws IOException, JSONException{

        String newId;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(rootUrl + "/wsmc/task.json");
        List <NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("taskType", taskType));
        nvps.add(new BasicNameValuePair("image", selectedImg));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        CloseableHttpResponse response1 = httpclient.execute(httpPost);
        HttpEntity entity1 = response1.getEntity();
        BufferedReader in = new BufferedReader(new InputStreamReader(entity1.getContent()));
        String inputLine = in.readLine();
        JSONObject obj = new JSONObject(inputLine);
        newId = obj.getString("nid");
        EntityUtils.consume(entity1);
        response1.close();
        return newId;
    }

    /**
     * Open a new microTask
     *
     * @param taskType          The type (segmentation or tagging) of the new microTask that i want to open
     * @param selectionTask     The id of the task which will be associated to the new microTask
     * @return                  The id of the new microTask
     * @throws IOException
     * @throws JSONException
     */
    public static String addUTask(String taskType, String selectionTask) throws IOException, JSONException{

        String newId;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(rootUrl + "/wsmc/utask.json");
        List <NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("taskType", taskType));
        nvps.add(new BasicNameValuePair("task", selectionTask));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        CloseableHttpResponse response1 = httpclient.execute(httpPost);
        HttpEntity entity1 = response1.getEntity();
        BufferedReader in = new BufferedReader(new InputStreamReader(entity1.getContent()));
        String inputLine = in.readLine();
        JSONObject obj = new JSONObject(inputLine);
        newId = obj.getString("nid");
        EntityUtils.consume(entity1);
        response1.close();
        return newId;
    }

    public static JSONArray loadFirst(JsonNode jsonImages) throws JSONException{
        JSONArray graph= new JSONArray();
        SortObject sorting;
        ArrayList<SortObject> tempList = new ArrayList<>();
        JsonNode object;
        JSONObject element;
        int num = 0;

        int i=0;
        while(i<jsonImages.size()){
            sorting = new SortObject() {};
            object = jsonImages.get(i);
            sorting.setId(object.get("id").asText());
            if(object.has("descriptions")){
                if(object.get("descriptions").has("segmentation")){
                    num = object.get("descriptions").get("segmentation").size();
                }
                else{
                    num = 0;
                }
            }
            sorting.setNum(num);
            tempList.add(i, sorting);
            num = 0;
            i++;
        }

        Collections.sort(tempList, new Comparator<SortObject>() {
            @Override public int compare(SortObject o1, SortObject o2) {
                if (o1.getNum() < o2.getNum()) {
                    return -1;
                } else if (o1.getNum() > o2.getNum()) {
                    return 1;
                }
                return 0;
            }

        });

        Iterator<SortObject> it = tempList.iterator();
        int tmp = tempList.get(0).getNum();
        int count = 0;

        while(it.hasNext()){
            SortObject obj = it.next();

            if(obj.getNum() == tmp){
                count ++;
            }
            else if(obj.getNum() != tmp){
                element = new JSONObject();
                element.put("occurence", count);
                element.put("annotations", tmp);
                graph.put(element);
                do{
                    tmp++;
                    count = 0;
                    if(obj.getNum() != tmp){
                        element = new JSONObject();
                        element.put("occurence", count);
                        element.put("annotations", tmp);
                        graph.put(element);
                    }
                }while(obj.getNum() != tmp);
                count = 1;
            }
        }
        element = new JSONObject();
        element.put("occurence", count);
        element.put("annotations", tmp);
        graph.put(element);

        return graph;
    }

    public static JSONArray loadSecond(JsonNode actions) throws JSONException{
        JSONArray graph= new JSONArray();
        SortObject sorting;
        ArrayList<SortObject> tempList = new ArrayList<>();
        ArrayList<SortObject> tempList2 = new ArrayList<>();
        JsonNode object;
        JSONObject element;

        int i = 0;
        int j = 0;
        while(i<actions.size()){
            object = actions.get(i);
            if(object.get("type").asText().equals("segmentation")){
                if(object.has("user")){
                    if(object.get("user").has("cubrik_userid")){
                        sorting = new SortObject() {};
                        sorting.setIdU(object.get("user").get("cubrik_userid").asInt());
                        tempList.add(j, sorting);
                        j++;
                    }
                }
            }

            i++;
        }

        Collections.sort(tempList, new Comparator<SortObject>() {
            @Override public int compare(SortObject o1, SortObject o2) {
                if (o1.getIdU() < o2.getIdU()) {
                    return -1;
                } else if (o1.getIdU() > o2.getIdU()) {
                    return 1;
                }
                return 0;
            }

        });

        Iterator<SortObject> it = tempList.iterator();
        int tmp = tempList.get(0).getIdU();
        int count = 0;

        while(it.hasNext()){
            SortObject obj = it.next();

            if(obj.getIdU() == tmp){
                count ++;
            }
            else if(obj.getIdU() != tmp){
                sorting = new SortObject() {};
                sorting.setIdU(tmp);
                sorting.setNum(count);
                tempList2.add(sorting);
                do{
                    tmp++;

                }while(obj.getIdU() != tmp);
                count = 1;
            }
        }
        sorting = new SortObject() {};
        sorting.setIdU(tmp);
        sorting.setNum(count);
        tempList2.add(sorting);

        Collections.sort(tempList2, new Comparator<SortObject>() {
            @Override public int compare(SortObject o1, SortObject o2) {
                if (o1.getNum() < o2.getNum()) {
                    return -1;
                } else if (o1.getNum() > o2.getNum()) {
                    return 1;
                }
                return 0;
            }

        });

        Iterator<SortObject> it2 = tempList2.iterator();
        tmp = 0;
        count = 0;

        while(it2.hasNext()){
            SortObject obj = it2.next();

            if(obj.getNum() == tmp){
                count ++;
            }
            else if(obj.getNum() != tmp){
                element = new JSONObject();
                element.put("users", count);
                element.put("images", tmp);
                graph.put(element);
                do{
                    tmp++;
                    count = 0;
                    if(obj.getNum() != tmp){
                        element = new JSONObject();
                        element.put("users", count);
                        element.put("images", tmp);
                        graph.put(element);
                    }
                }while(obj.getNum() != tmp);
                count = 1;
            }
        }
        element = new JSONObject();
        element.put("users", count);
        element.put("images", tmp);
        graph.put(element);

        return graph;
    }

    public static JSONArray download1(JsonNode actions) throws JSONException{
        JSONArray down1= new JSONArray();
        SortObject sorting;
        ArrayList<SortObject> tempList = new ArrayList<>();
        ArrayList<DownObject> tempList2 = new ArrayList<>();
        JsonNode object;

        int i = 0;
        int j = 0;
        while(i<actions.size()){
            object = actions.get(i);
            if(object.get("type").asText().equals("segmentation")){
                if(object.has("user")){
                    if(object.get("user").has("cubrik_userid")){
                        sorting = new SortObject() {};
                        sorting.setIdU(object.get("user").get("cubrik_userid").asInt());
                        sorting.setIdTmp(object.get("id").asInt());
                        sorting.setImgTmp(object.get("image").asInt());
                        tempList.add(j, sorting);
                        j++;
                    }
                }
            }

            i++;
        }

        Collections.sort(tempList, new Comparator<SortObject>() {
            @Override public int compare(SortObject o1, SortObject o2) {
                if (o1.getIdU() < o2.getIdU()) {
                    return -1;
                } else if (o1.getIdU() > o2.getIdU()) {
                    return 1;
                }
                return 0;
            }

        });

        Iterator<SortObject> it = tempList.iterator();
        int tmp = tempList.get(0).getIdU();
        DownObject user;
        StoredStatObj stat;
        ArrayList<StoredStatObj> elements = new ArrayList<>();

        while(it.hasNext()){
            SortObject obj = it.next();

            if(obj.getIdU() == tmp){

                stat= new StoredStatObj(obj.getIdTmp(), obj.getImgTmp());
                elements.add(stat);
            }
            else if(obj.getIdU() != tmp){
                user = new DownObject() {};
                user.setId(tmp);
                user.setElement(elements);
                tempList2.add(user);
                do{
                    tmp++;

                }while(obj.getIdU() != tmp);
                elements = new ArrayList<>();
                stat= new StoredStatObj(obj.getIdTmp(), obj.getImgTmp());
                elements.add(stat);
            }
        }
        user = new DownObject() {};
        user.setId(tmp);
        user.setElement(elements);
        tempList2.add(user);



        Iterator<DownObject> it2 = tempList2.iterator();

        JSONObject son;
        JSONArray body;
        JSONObject content;

        while(it2.hasNext()){
            son = new JSONObject();
            body = new JSONArray();
            DownObject obj = it2.next();
            son.put("user", obj.getId());

            Iterator<StoredStatObj> it3 = obj.getElement().iterator();

            while(it3.hasNext()){
                StoredStatObj obj2 = it3.next();
                content = new JSONObject();
                content.put("segment",obj2.getId1());
                content.put("image",obj2.getId2());
                body.put(content);
            }
            son.put("sketch",body);
            down1.put(son);
        }


        return down1;
    }

    public static JSONArray download2(JsonNode actions) throws JSONException{
        JSONArray down2= new JSONArray();
        SortObject sorting;
        ArrayList<SortObject> tempList = new ArrayList<>();
        ArrayList<DownObject> tempList2 = new ArrayList<>();
        JsonNode object;

        int i = 0;
        int j = 0;
        while(i<actions.size()){
            object = actions.get(i);
            if(object.get("type").asText().equals("segmentation")){
                if(object.has("user")){
                    if(object.get("user").has("cubrik_userid")){
                        sorting = new SortObject() {};
                        sorting.setIdU(object.get("user").get("cubrik_userid").asInt());
                        sorting.setIdTmp(object.get("id").asInt());
                        sorting.setImgTmp(object.get("image").asInt());
                        tempList.add(j, sorting);
                        j++;
                    }
                }
            }

            i++;
        }

        Collections.sort(tempList, new Comparator<SortObject>() {
            @Override public int compare(SortObject o1, SortObject o2) {
                if (o1.getImgTmp() < o2.getImgTmp()) {
                    return -1;
                } else if (o1.getImgTmp() > o2.getImgTmp()) {
                    return 1;
                }
                return 0;
            }

        });

        Iterator<SortObject> it = tempList.iterator();
        int tmp = tempList.get(0).getImgTmp();
        DownObject image;
        StoredStatObj stat;
        ArrayList<StoredStatObj> elements = new ArrayList<>();

        while(it.hasNext()){
            SortObject obj = it.next();

            if(obj.getImgTmp() == tmp){

                stat= new StoredStatObj(obj.getIdTmp(), obj.getIdU());
                elements.add(stat);
            }
            else if(obj.getImgTmp() != tmp){
                image = new DownObject() {};
                image.setId(tmp);
                image.setElement(elements);
                tempList2.add(image);
                do{
                    tmp++;

                }while(obj.getImgTmp() != tmp);
                elements = new ArrayList<>();
                stat= new StoredStatObj(obj.getIdTmp(), obj.getIdU());
                elements.add(stat);
            }
        }
        image = new DownObject() {};
        image.setId(tmp);
        image.setElement(elements);
        tempList2.add(image);



        Iterator<DownObject> it2 = tempList2.iterator();

        JSONObject son;
        JSONArray body;
        JSONObject content;

        while(it2.hasNext()){
            son = new JSONObject();
            body = new JSONArray();
            DownObject obj = it2.next();
            son.put("image", obj.getId());

            Iterator<StoredStatObj> it3 = obj.getElement().iterator();

            while(it3.hasNext()){
                StoredStatObj obj2 = it3.next();
                content = new JSONObject();
                content.put("segment",obj2.getId1());
                content.put("user",obj2.getId2());
                body.put(content);
            }
            son.put("sketch",body);
            down2.put(son);
        }


        return down2;
    }


    /*
     * Returns a tag based on a particular choice policy
     * @return String retrieved tag following a policy
     */
    private static String chooseTag(HashSet<String> tags) {
        if (tags != null && tags.size() > 0) {
            Object[] stringTags = tags.toArray();
            String toReturn = (String) stringTags[(new Random().nextInt(tags.size()))];
            return toReturn;
        } else {
            return "";
        }
    }




}
