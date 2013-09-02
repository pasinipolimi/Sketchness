package utils.CMS;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
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
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class CMS{
    
    //Url of the CMS system
    private final static String rootUrl=Play.application().configuration().getString("cmsUrl");
    private final static String oauthConsumerKey=Play.application().configuration().getString("oauthConsumerKey");
    
    
    public static void closeUTask(Integer uTaskID, Integer actionId){
        if(uTaskID!=null) {
            String request = rootUrl+"/wsmc/utask/"+uTaskID+"/"+actionId+"/close";
            WS.url(request).setContentType("application/x-www-form-urlencoded").put("");
            Logger.debug("[CMS] Closing uTask "+uTaskID);
        }
    }
    
    public static Integer segmentation(ObjectNode finalTraces, String username,Integer session) throws MalformedURLException, IOException, JSONException {
        String id = finalTraces.get("id").getTextValue();
        String label = finalTraces.get("label").getTextValue();
        String traces = finalTraces.get("traces").toString();
        String history = finalTraces.get("history").toString();   
        
        String urlParameters = "ta_name=tag&ta_val="+label+"&content_type=segmentation&&user_id="+username+"&language="+LanguagePicker.retrieveIsoCode()+"&session_id="+session+"&polyline_r="+traces+"&polyline_h="+history+"&oauth_consumer_key="+oauthConsumerKey;
        String request = rootUrl+"/wsmc/image/"+id+"/segmentation.json";
        F.Promise<WS.Response> returned =WS.url(request).setContentType("application/x-www-form-urlencoded").post(urlParameters);
        JSONObject actionInfo = new JSONObject(returned.get().getBody());
        Integer actionId=Integer.parseInt(actionInfo.get("vid").toString());
        Logger.debug("[CMS] Storing segmentation with action "+actionId+" for image with id "+id+" and tag "+label);
        return actionId;
    }
    
    
    public static Integer openSession() throws Error {
        String request = rootUrl+"/wsmc/session.json";
        Logger.debug("[CMS] Opening a new session...");
        F.Promise<WS.Response> returned = WS.url(request).setContentType("application/x-www-form-urlencoded").post("oauth_consumer_key="+oauthConsumerKey);
        
	String sessionId=returned.get().getBody();
        sessionId=sessionId.replace("[\"","");
        sessionId=sessionId.replace("\"]", "");
        Logger.debug("[CMS] Retrieved session "+sessionId);
        return Integer.valueOf(sessionId);
    }
    
    public static void closeSession(Integer sessionId) throws Error {
        String request = rootUrl+"/wsmc/session/"+sessionId;
        WS.url(request).setContentType("application/x-www-form-urlencoded").put("state=0&oauth_consumer_key="+oauthConsumerKey);
        Logger.debug("[CMS] Closing session "+sessionId);
    }
    
    public static void postAction(Integer sessionId, String actionType, String username, String log) throws Error {
        String request = rootUrl+"/wsmc/action";
        if(log.equals(""))
            log="{}";
        String parameters="session_id="+sessionId+"&action_type="+actionType+"&user_id="+username+"&oauth_consumer_key="+oauthConsumerKey+"&action_log="+log;  
        WS.url(request).setContentType("application/x-www-form-urlencoded").post(parameters);
        Logger.debug("[CMS] Action "+actionType+" for session "+sessionId+": "+log);
    }
    
    
    
    
    /**
     * Retrieving data from the CMS [TODO] Right now we are not retrieving based on the requirements of our tasks
     * such as completing tasks that have not been already faced and so on. We will add this feature in the future.
    **/
    public static void taskSetInitialization(HashSet<ObjectNode> priorityTaskHashSet,HashSet<ObjectNode> taskHashSet, Room roomChannel) throws Error {
        
       JsonReader jsonReader= new JsonReader();
       JsonNode retrievedTasks=null;
       JsonNode retrievedImages=null;
       //[TODO] Fail safe in case of not being able to retrieve the instances
       try{
            retrievedTasks = jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/task.json");
            retrievedImages= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image.json");
       }
       catch(IllegalArgumentException e)
       {
           throw new RuntimeException("[CMS] The request to the CMS is malformed");
       }
       
       
       //Fill the set of task to be performed with the task that has been 
       //explicitly declared
       
       if(retrievedTasks!=null)
       {
            retrievedTasks=retrievedTasks.get("task");
            for (JsonNode item : retrievedTasks) {
                if(item.getElements().hasNext()) {
                    //If the task is still open
                    if(item.get("status").asInt()==1)
                    {
                        item=jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/task/"+item.get("id").getTextValue()+".json");
                        item=item.get("task");
                        Logger.debug("[CMS] Retrieved open task "+item.toString());
                        JsonNode uTasks=item.get("uTask");
                        String id=item.get("image").getElements().next().getElements().next().asText();
                        JsonNode image = jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image/"+id+".json");
                        //Retrieve all the uTask for the current task and assign them
                        for (JsonNode utask : uTasks) {
                            if(utask.getElements().hasNext()) {
                                utask=jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/utask/"+utask.get("id").getTextValue()+".json");
                                utask=utask.get("uTask");
                                if(utask.get("status").asInt()==1) {
                                    ObjectNode guessWord = Json.newObject();
                                    guessWord.put("type", "task");
                                    guessWord.put("id", id);
                                    //Change the task to assign based on the kind of task that has to be performed
                                    //for now just tagging and segmentation are supported for the images.
                                    switch (utask.get("taskType").asText()) {
                                        case "tagging":
                                            guessWord.put("tag",chooseTag(null));
                                            guessWord.put("lang",LanguagePicker.retrieveIsoCode());
                                            guessWord.put("image",rootUrl+image.get("mediaLocator").asText());
                                            guessWord.put("width",image.get("width").asInt());
                                            guessWord.put("height",image.get("height").asInt());
                                            guessWord.put("utaskid", utask.get("id").asInt());
                                            priorityTaskHashSet.add(guessWord);
                                            sendTaskAcquired(roomChannel);
                                            break;
                                        case "segmentation":
                                            ArrayList<String> tags;
                                            //Get all the segments that have been stored for the image
                                            JsonNode imageSegments= image.get("descriptions");
                                            if(imageSegments!=null) {
                                                tags=retrieveTags(imageSegments);
                                                guessWord.put("tag",chooseTag(tags));
                                                guessWord.put("lang",LanguagePicker.retrieveIsoCode());
                                                guessWord.put("image",rootUrl+image.get("mediaLocator").asText());
                                                guessWord.put("width",image.get("width").asInt());
                                                guessWord.put("height",image.get("height").asInt());
                                                guessWord.put("utaskid", utask.get("id").asInt());
                                                priorityTaskHashSet.add(guessWord);
                                                sendTaskAcquired(roomChannel);
                                            }
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
       }
       
       
       
       if(retrievedImages!=null)
       {
        //For each image
        for (JsonNode item : retrievedImages) {
            if(item.getElements().hasNext())
            {
                //Save information related to the image
                String id=item.get("id").asText();
                String url=rootUrl+item.get("mediaLocator").asText();
                Integer width = item.get("width").asInt();
                Integer height = item.get("height").asInt();
                
                //Get all the segments that have been stored for the image
                JsonNode imageSegments= item.get("descriptions").get("segmentation");
                ArrayList<String> tags = new ArrayList<>();
                ObjectNode guessWord = Json.newObject();
                guessWord.put("type", "task");
                guessWord.put("id", id);
                
                //Find the valid tags for this task.
                if(imageSegments!=null) {
                   retrieveTags(imageSegments);
                }
                //Add one tag among the ones that have been retrieved following a particular policy
                guessWord.put("tag",chooseTag(tags));
                guessWord.put("lang",LanguagePicker.retrieveIsoCode());
                guessWord.put("image",url);
                guessWord.put("width",width);
                guessWord.put("height",height);
                taskHashSet.add(guessWord);
                sendTaskAcquired(roomChannel);
            }          
        }  
    }
    else
           throw new Error("[GAME]: Cannot retrieve the tasks from the CMS.");
  }
    

private static void sendTaskAcquired(Room roomChannel)
{
    GameMessages.GameEvent taskAcquired = new GameMessages.GameEvent(roomChannel,GameEventType.taskAcquired);
    GameBus.getInstance().publish(taskAcquired); 
}
    
private static ArrayList<String> retrieveTags(JsonNode imageSegments)
{
    ArrayList<String> tags = new ArrayList<>();
    for(JsonNode segment:imageSegments)
    {
        if(segment.getElements().hasNext())
        {
            JsonNode textAnnotations = segment.getElements().next().get("itemAnnotations");
            if(null!=textAnnotations) {
                for(JsonNode annotation:textAnnotations)
                {
                    //For each annotation stored for the image
                    if(annotation.getElements().hasNext())
                    {
                        //If the annotation is a tag and is in the same language as the one defined in the system, add the tag to the list of possible tags
                        if((annotation.get("name").asText().equals("tag"))&&annotation.get("language").asText().equals(LanguagePicker.retrieveIsoCode())) 
                            tags.add(annotation.get("value").asText());
                    }
                }
            }
        }
    }
    return tags;
}
    
    
    
/*
 * Returns a tag based on a particular choice policy
 * @return String retrieved tag following a policy
 */
private static String chooseTag(ArrayList<String> tags)
{
    if(tags!=null&&tags.size()>0)
        return tags.get(new Random().nextInt(tags.size()));
    else
        return "";
}
}
