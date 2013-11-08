package utils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import static java.util.concurrent.TimeUnit.SECONDS;
import javax.imageio.ImageIO;
import org.codehaus.jackson.JsonFactory;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import play.Logger;
import play.Play;

import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.CMS.CMS;
import utils.aggregator.ContourAggregator;
import utils.gamebus.GameMessages.Join;

public class Renderer extends UntypedActor {

    private final static String rootUrl = Play.application().configuration().getString("cmsUrl");
    private WebSocket.Out<JsonNode> channel;
    String imageId;

    public static synchronized void createRenderer(final String imageID, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception {
        @SuppressWarnings("unchecked")
        Props properties = new Props(Renderer.class);
        final ActorRef finalRoom = Akka.system().actorOf(properties);
        Future<Object> future = Patterns.ask(finalRoom, new Join(imageID, out), 1000000000);
        // Send the Join message to the room
        String result = (String) Await.result(future, Duration.create(10, SECONDS));

        if ("OK".equals(result)) {
            // in: handle messages from the painter
            in.onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode json) throws Throwable {
                    if (!json.get("type").asText().equals("task")) {
                        finalRoom.tell(json, finalRoom);
                    }
                }
            });

            // User has disconnected.
            in.onClose(new F.Callback0() {
                @Override
                public void invoke() throws Throwable {
                }
            });
        }
    }

    //Manage the messages
    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Join) {
            start((Join) message);
        } else if (message instanceof JsonNode) {
            aggregate(true, ((JsonNode) message).get("tag").asText(),false);
        } else if (message instanceof MaskParameters) {
            createMask(((MaskParameters) message).getId(), ((MaskParameters) message).getTag());
        }
    }

    private void start(Join message) throws IOException, Exception {

        Join join = message;
        imageId = join.getUsername();
        channel = join.getChannel();
        getSender().tell("OK", this.getSelf());
        JsonReader jsonReader = new JsonReader();
        JsonNode item = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image/" + imageId + ".json");
        String url = rootUrl + item.get("mediaLocator").asText();
        Integer width = item.get("width").asInt();
        Integer height = item.get("height").asInt();
        String label = "";
        ObjectNode guessWord = Json.newObject();
        guessWord.put("type", "task");
        guessWord.put("id", imageId);
        guessWord.put("word", label);
        guessWord.put("lang", LanguagePicker.retrieveIsoCode());
        guessWord.put("image", url);
        guessWord.put("width", width);
        guessWord.put("height", height);
        channel.write(guessWord);
        JsonNode toAggregate = retrieveTags(imageId);
        ObjectNode availableTags = Json.newObject();
        availableTags.put("type", "tags");
        availableTags.put("tags", toAggregate);
        channel.write(availableTags);
    }

    private Image aggregate(boolean networkOn, String requiredTag, boolean transparentMask) throws IOException, Exception {
        JavascriptColor[] colors = JavascriptColor.values();
        JsonReader jsonReader = new JsonReader();
        JsonNode imageSegments = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image/" + imageId + ".json");
        Integer imWidth = imageSegments.get("width").asInt();
        Integer imHeight = imageSegments.get("height").asInt();
        imageSegments = imageSegments.get("descriptions").get("segmentation");
        Image toReturn = null;
        Boolean found = false;
        if (null != imageSegments) {
            int numberTraces = imageSegments.size();
            ArrayList<String> toAggregate = new ArrayList<>();
            for (int i = 0; i < numberTraces; i++) {
                JavascriptColor c = colors[i % colors.length];
                JsonNode current = imageSegments.get(i);
                JsonNode annotation = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/content/" + current.get("id").asText() + ".json");
                JsonNode tag = annotation.get("itemAnnotations");
                for (int j = 0; j < tag.size(); j++) {
                    JsonNode retrieved = tag.get(j);
                    if (retrieved.get("value").asText().equals(requiredTag)) {
                        found = true;
                        annotation = annotation.get("mediaSegment");
                        for (JsonNode result : annotation) {
                            if (result.get("name").asText().equals("result")) {
                                result = result.get("polyline");
                                ObjectMapper mapper = new ObjectMapper();
                                JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
                                String toParse = result.asText().replace("\\", "");
                                JsonParser jp = factory.createJsonParser(toParse);
                                JsonNode actualObj = mapper.readTree(jp);
                                if (networkOn) {
                                    ObjectNode points = new ObjectNode(JsonNodeFactory.instance);
                                    points.put("points", actualObj);
                                    points.put("type", "trace");
                                    points.put("color", c.name());
                                    channel.write(points);
                                }
                                toAggregate.add(actualObj.toString());
                            }
                        }
                    }
                }
            }
            if (found && !networkOn) {
                String[] toAggregateString = new String[toAggregate.size()];
                for (int i = 0; i < toAggregate.size(); i++) {
                    toAggregateString[i] = toAggregate.get(i);
                }
                toReturn = ContourAggregator.simpleAggregator(toAggregateString, imWidth, imHeight);
                if(transparentMask) {
                    BufferedImage transparent = imageToBufferedImage(toReturn);
                    toReturn = makeColorTransparent(transparent, Color.WHITE);
                }
            } else if (!networkOn) {
                Logger.error("[AGGREGATOR] Cannot retrieve mask for image " + imageId + " with tag " + requiredTag);
                throw new Exception("[AGGREGATOR] Tag " + requiredTag + " not found for image " + imageId);
            }
        }
        return toReturn;
    }
    
    private static BufferedImage imageToBufferedImage(Image image) {

    	BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    	Graphics2D g2 = bufferedImage.createGraphics();
    	g2.drawImage(image, 0, 0, null);
    	g2.dispose();

    	return bufferedImage;

    }
    
     public static Image makeColorTransparent(BufferedImage im, final Color color) {
    	ImageFilter filter = new RGBImageFilter() {

    		// the color we are looking for... Alpha bits are set to opaque
    		public int markerRGB = color.getRGB() | 0xFF000000;

    		public final int filterRGB(int x, int y, int rgb) {
    			if ((rgb | 0xFF000000) == markerRGB) {
    				// Mark the alpha bits as zero - transparent
    				return 0x00FFFFFF & rgb;
    			} else {
    				// nothing to do
    				return rgb;
    			}
    		}
    	};

    	ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
    	return Toolkit.getDefaultToolkit().createImage(ip);
    }

    public static synchronized File retrieveMask(String ImageID, String tag) throws Exception {
        @SuppressWarnings("unchecked")
        Props properties = new Props(Renderer.class);
        final ActorRef finalRoom = Akka.system().actorOf(properties);
        Future<Object> future = Patterns.ask(finalRoom, new MaskParameters(ImageID, tag), 1000000000);
        // Send the Join message to the room
        File result = (File) Await.result(future, Duration.create(180, SECONDS));
        if (result instanceof File) {
            Logger.info("[AGGREGATOR] Retrieved mask for image " + ImageID);
            return result;
        }
        Logger.error("[AGGREGATOR] Retrieved mask for image " + ImageID);
        return null;
    }

    public static synchronized JsonNode retrieveImages() throws Exception {
        @SuppressWarnings("unchecked")
        JsonReader jsonReader = new JsonReader();
        return jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image.json");
    }

    public static synchronized JsonNode retrieveTags(String ImageID) throws Exception {
        @SuppressWarnings("unchecked")
        JsonReader jsonReader = new JsonReader();
        JsonNode image = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image/" + ImageID + ".json");
        image = image.get("descriptions");
        ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
        HashSet<String> tags;
        if (image != null) {
            tags = CMS.retrieveTags(image);
        } else {
            tags = new HashSet<>();
        }
        for (String tag : tags) {
            result.add(tag);
        }
        return result;
    }

    private void createMask(String id, String tag) throws IOException, Exception {
        Logger.info("[AGGREGATOR] Retrieving aggregated mask for image " + id);
        try {
            imageId = id;
            Image retrieved = aggregate(false, tag, true);
            if (null != retrieved) {
                BufferedImage bufIm = imageToBufferedImage(retrieved);
                ImageIO.write((RenderedImage) bufIm, "png", new File(imageId + ".png"));
                File toReturn = new File(imageId + ".png");
                getSender().tell(toReturn, this.getSelf());
            } else {
                getSender().tell(null, this.getSelf());
            }
        } catch (Exception e) {
            getSender().tell(null, this.getSelf());
        }
    }

    public static String webToolAjax()throws JSONException {


        JsonReader jsonReader = new JsonReader();
        JsonNode itemImage = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image.json");
        JsonNode itemTask = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/task.json");
        JSONArray images = new JSONArray();
        boolean check= false;
        if(!itemImage.isNull()){
            images = CMS.retriveImageId(itemImage);
        }
        else{
            JSONObject element = new JSONObject();
            element.append("id", "No photos in the system");
            images.put(element);
        }
        JSONArray tasks = new JSONArray();
        if(itemTask != null){
            tasks = CMS.retriveTaskId(itemTask);
            check = true;
        }
        else{
            JSONObject element = new JSONObject();
            element.append("id", "No Tasks in the system");
            element.append("taskType", "");
            tasks.put(element);
        }

        JSONObject result = new JSONObject();

        result.append("image", images);
        result.append("task", tasks);
        result.append("check", check);

        String options = result.toString();


        return options;

    }

    public static String loadStats() throws JSONException{

        JsonReader jsonReader = new JsonReader();
        JsonNode itemImage = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image.json");
        int last;
        String totImg = Integer.toString(itemImage.size());

        JSONArray stats = CMS.retriveStats(itemImage);

        String tmp = stats.getJSONObject(0).getString("numTag");
        tmp = tmp.substring(1);
        last = tmp.length() -1;
        tmp = tmp.substring(0, last);

        int numTag= Integer.parseInt(tmp);
        tmp = stats.getJSONObject(0).getString("numSegment");
        tmp =tmp.substring(1);
        last = tmp.length() -1;
        tmp = tmp.substring(0, last);
        int numSegment= Integer.parseInt(tmp);

        float mediaTagImg = (float) numTag/itemImage.size();
        float mediaSegImg = (float) numSegment/itemImage.size();

        String mediaTag = Float.toString(mediaTagImg);
        String numeroSegmenti = Integer.toString(numSegment);
        String mediaSegPerImg = Float.toString(mediaSegImg);



        JSONObject result = new JSONObject();

        result.append("totImg", totImg);
        result.append("mediaTag", mediaTag);
        result.append("numSegment", numeroSegmenti);
        result.append("mediaSegImg", mediaSegPerImg);

        String sendStats = result.toString();

        return sendStats;
    }

    public static String webInfoAjax(String selection)throws JSONException{

        JsonReader jsonReader = new JsonReader();
        JsonNode item = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/image/" + selection + ".json");

        String info = CMS.retriveImgInfo(item);



        return info;
    }

    public static String webInfoTask(String selection)throws JSONException{

        JsonReader jsonReader = new JsonReader();
        JsonNode itemTask = jsonReader.readJsonArrayFromUrl(rootUrl + "/wsmc/task.json");

        String info = CMS.retriveTagInfo(itemTask,selection);



        return info;
    }

    public static void closeTask(String selection) throws IOException{
        CMS.closeTask2(selection);
    }

    public static String addTask(String taskType, String selectedImg)throws IOException, JSONException{



        String info = CMS.addTask(taskType, selectedImg);



        return info;
    }

    public static String addUTask(String taskType, String selectionTask)throws IOException, JSONException{



        String info = CMS.addUTask(taskType, selectionTask);



        return info;
    }


}

class MaskParameters {

    String id, tag;

    public MaskParameters(String id, String tag) {
        this.id = id;
        this.tag = tag;
    }

    public String getId() {
        return id;
    }

    public String getTag() {
        return tag;
    }
}

enum JavascriptColor {

    aqua, blue, fuchsia, green, lime, maroon, navy, olive, orange, purple, red, teal, white, yellow
}
