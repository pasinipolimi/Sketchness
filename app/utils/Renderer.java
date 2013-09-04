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
import static java.util.concurrent.TimeUnit.SECONDS;
import javax.imageio.ImageIO;
import org.codehaus.jackson.JsonFactory;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
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
            aggregate(true);
        }
        else if(message instanceof String)
        {
            createMask((String)message);
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
            String id=item.get("id").asText();
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
    }
    
    private Image aggregate(boolean networkOn) throws IOException, Exception
    {
        JavascriptColor[] colors = JavascriptColor.values();
        JsonReader jsonReader= new JsonReader();
        JsonNode imageSegments= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image/"+imageId+".json");
        Integer imWidth = imageSegments.get("width").asInt();
        Integer imHeight = imageSegments.get("height").asInt();
        imageSegments= imageSegments.get("descriptions").get("segmentation");
        Image toReturn=null;
        if(null!=imageSegments)
        {
                int numberTraces=imageSegments.size();
                String[] toAggregate = new String[numberTraces];
                for(int i=0;i<numberTraces;i++)
                {
                    JavascriptColor c = colors[i%colors.length];
                    JsonNode current=imageSegments.get(i);
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
                            toAggregate[i]=actualObj.toString();
                        }
                    }
                }
                toReturn=ContourAggregator.simpleAggregator(toAggregate,imWidth,imHeight);
         }
        return toReturn;
      }
    
      public static synchronized File retrieveMask(String ImageID) throws Exception {
        @SuppressWarnings("unchecked")
        Props properties= new Props(Renderer.class);
        final ActorRef finalRoom =  Akka.system().actorOf(properties);
        Future<Object> future = Patterns.ask(finalRoom, ImageID, 1000000000);
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
      
      
      private void createMask(String id) throws IOException, Exception
      {
          Logger.info("[AGGREGATOR] Retrieving aggregated mask for image "+id);
          imageId=id;
          Image retrieved=aggregate(false);
          BufferedImage bufIm = (BufferedImage)retrieved;
          ImageIO.write((RenderedImage)bufIm,"png",new File(imageId+".png"));
          File toReturn=new File(imageId+".png");
          getSender().tell(toReturn, this.getSelf());
      }
}

enum JavascriptColor{
    aqua, blue, fuchsia, green, lime, maroon, navy, olive, orange, purple, red, teal, white, yellow
}
