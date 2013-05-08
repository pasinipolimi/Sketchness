package utils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import java.io.IOException;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.codehaus.jackson.JsonFactory;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.gamebus.GameMessages.Join;


public class Renderer extends UntypedActor{
    
    private final String rootUrl="http://webservices.comoconnection.com/";
    private WebSocket.Out<JsonNode> channel;
    
    String imageId;
    
    public static synchronized void createRenderer(final String imageID, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception
    {
      @SuppressWarnings("unchecked")
      Props properties= new Props(Renderer.class);
      final ActorRef finalRoom =  Akka.system().actorOf(properties);
      Future<Object> future = Patterns.ask(finalRoom,new Join(imageID, out), 1000);
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
            trace();
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
            item=item.get(0);
            item=item.get("image");
            String id=item.get("imgage_id").asText();
            String url=rootUrl+item.get("image_uri").asText();
            Integer width = item.get("imgage_width").asInt();
            Integer height = item.get("imgage_height").asInt();
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
    
    private void trace() throws IOException
    {
        JavascriptColor[] colors = JavascriptColor.values();
        JsonReader jsonReader= new JsonReader();
        JsonNode imageSegments= jsonReader.readJsonArrayFromUrl(rootUrl+"/wsmc/image/"+imageId+"/segment.json");
        imageSegments=imageSegments.get(0);
        imageSegments=imageSegments.get("image");
        imageSegments=imageSegments.get("polyline");
        int numberTraces=imageSegments.size();
        for(int i=0;i<numberTraces;i++)
        {
            JavascriptColor c = colors[i];
            JsonNode current=imageSegments.get(i);
            current=current.get("polyline_history");
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
            JsonParser jp = factory.createJsonParser(current.asText());
            JsonNode actualObj = mapper.readTree(jp);
            ObjectNode points = new ObjectNode(JsonNodeFactory.instance);
            points.put("points", actualObj);
            points.put("type", "trace");
            points.put("color", c.name());
            channel.write(points);
        }
    }
}

enum JavascriptColor{
    aqua, blue, fuchsia, green, lime, maroon, navy, olive, orange, purple, red, teal, white, yellow
}
