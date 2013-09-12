package utils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
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
import play.Logger;
import play.Play;

import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.aggregator.ContourAggregator;
import utils.gamebus.GameMessages.Join;


public class Renderer extends UntypedActor{
    
    private final String rootUrl=Play.application().configuration().getString("cmsUrl");
    private WebSocket.Out<JsonNode> channel;
    
    String imageId;
    
    public static synchronized void createRenderer(final String imageID, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception
    {
      @SuppressWarnings("unchecked")
      Props properties= new Props(Renderer.class);
      final ActorRef finalRoom =  Akka.system().actorOf(properties);
      Future<Object> future = Patterns.ask(finalRoom,new Join(imageID, out), 1000000000);
        // Send the Join message to the room
      String result = (String)Await.result(future, Duration.create(10, SECONDS));
        
        if("OK".equals(result)) 
        {
            // in: handle messages from the painter
            in.onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode json) throws Throwable {
                    if(!json.get("type").asText().equals("task"))
                        finalRoom.tell(json,finalRoom);
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
        if(message instanceof Join )
        {
            start((Join) message);
        }
        else if(message instanceof JsonNode)
        {
                aggregate(true,((JsonNode)message).get("tag").asText());
        }
        else if (message instanceof MaskParameters)
        {
            createMask(((MaskParameters)message).getId(),((MaskParameters)message).getTag());
        }
    }
    
    private void start(Join message) throws IOException
    {
            Join join = message;
            imageId=join.getUsername();
            channel=join.getChannel();
            getSender().tell("OK",this.getSelf());
            JsonReader jsonReader= new JsonReader();
            JsonNode item= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image/"+imageId+".json");
            String url=rootUrl+item.get("mediaLocator").asText();
            Integer width = item.get("width").asInt();
            Integer height = item.get("height").asInt();
            String label="";
            ObjectNode guessWord = Json.newObject();
            guessWord.put("type", "task");
            guessWord.put("id", imageId);
            guessWord.put("word",label);
            guessWord.put("lang",LanguagePicker.retrieveIsoCode());
            guessWord.put("image",url);
            guessWord.put("width",width);
            guessWord.put("height",height);
            channel.write(guessWord);
            
            JsonNode imageSegments= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image/"+imageId+".json");
            imageSegments= imageSegments.get("descriptions").get("segmentation");
            HashSet<String> toAggregate = new HashSet<>();
            if(null!=imageSegments)
            {
                    int numberTraces=imageSegments.size();
                    for(int i=0;i<numberTraces;i++)
                    {
                        JsonNode current=imageSegments.get(i);
                        JsonNode annotation=current.get("itemAnnotations");
                        for(int j=0;j<annotation.size();j++)
                        {
                            JsonNode retrieved=annotation.get(j);
                            toAggregate.add(retrieved.get("value").asText());
                        }
                    }
            }
            
            ObjectNode availableTags = Json.newObject();
            ArrayNode tagArray = new ArrayNode(JsonNodeFactory.instance);
            availableTags.put("type","tags");
            for(String retrieved:toAggregate)
            {
                tagArray.add(retrieved);
            }
            availableTags.put("tags", tagArray);
            channel.write(availableTags);
    }
    
    private Image aggregate(boolean networkOn, String requiredTag) throws IOException, Exception
    {
        JavascriptColor[] colors = JavascriptColor.values();
        JsonReader jsonReader= new JsonReader();
        JsonNode imageSegments= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image/"+imageId+".json");
        Integer imWidth = imageSegments.get("width").asInt();
        Integer imHeight = imageSegments.get("height").asInt();
        imageSegments= imageSegments.get("descriptions").get("segmentation");
        Image toReturn=null;
        Boolean found=false;
        if(null!=imageSegments)
        {
                int numberTraces=imageSegments.size();
                ArrayList<String> toAggregate = new ArrayList<>();
                for(int i=0;i<numberTraces;i++)
                {
                    JavascriptColor c = colors[i%colors.length];
                    JsonNode current=imageSegments.get(i);
                    JsonNode annotation=current.get("itemAnnotations");
                    for(int j=0;j<annotation.size();j++) {
                        JsonNode retrieved=annotation.get(j);
                        if(retrieved.get("value").asText().equals(requiredTag)) {
                            found=true;
                            current=current.get("mediaSegment");
                            for(JsonNode result:current)
                            {
                                if(result.get("name").asText().equals("result"))
                                {
                                    result=result.get("polyline");
                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
                                    String toParse=result.asText().replace("\\", "");
                                    JsonParser jp = factory.createJsonParser(toParse);
                                    JsonNode actualObj = mapper.readTree(jp);
                                    if(networkOn) {
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
                if(found&&!networkOn)
                {
                    String[] toAggregateString = new String[toAggregate.size()];
                    for(int i=0;i<toAggregate.size();i++)
                        toAggregateString[i]=toAggregate.get(i);
                    toReturn=ContourAggregator.simpleAggregator(toAggregateString,imWidth,imHeight);
                }
                else if(!networkOn) {
                   Logger.error("[AGGREGATOR] Cannot retrieve mask for image "+imageId+" with tag "+requiredTag);
                   throw new Exception("[AGGREGATOR] Tag "+requiredTag+" not found for image "+imageId);
                }
         }
        return toReturn;
      }
    
      public static synchronized File retrieveMask(String ImageID, String tag) throws Exception {
        @SuppressWarnings("unchecked")
        Props properties= new Props(Renderer.class);
        final ActorRef finalRoom =  Akka.system().actorOf(properties);
        Future<Object> future = Patterns.ask(finalRoom, new MaskParameters(ImageID,tag), 1000000000);
          // Send the Join message to the room
        File result = (File)Await.result(future, Duration.create(180, SECONDS));
        if(result instanceof File) 
        {
              Logger.info("[AGGREGATOR] Retrieved mask for image "+ImageID);
              return result;
        }
        Logger.error("[AGGREGATOR] Retrieved mask for image "+ImageID);
        return null;
      }
      
      
      private void createMask(String id, String tag) throws IOException, Exception
      {
          Logger.info("[AGGREGATOR] Retrieving aggregated mask for image "+id);
          try {
            imageId=id;
            Image retrieved=aggregate(false,tag);
            if(null!=retrieved) {
              BufferedImage bufIm = (BufferedImage)retrieved;
              ImageIO.write((RenderedImage)bufIm,"png",new File(imageId+".png"));
              File toReturn=new File(imageId+".png");
              getSender().tell(toReturn, this.getSelf());
            }
            else
              getSender().tell(null,this.getSelf());
          }
          catch(Exception e) {
             getSender().tell(null,this.getSelf()); 
          }
      }
}


class MaskParameters
{

    String id, tag;
    public MaskParameters(String id, String tag) {
        this.id=id;
        this.tag=tag;
    }

    public String getId() {
        return id;
    }

    public String getTag() {
        return tag;
    }
    
    
}
enum JavascriptColor{
    aqua, blue, fuchsia, green, lime, maroon, navy, olive, orange, purple, red, teal, white, yellow
}
