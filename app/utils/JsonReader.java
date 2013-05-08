package utils;



import org.codehaus.jackson.JsonNode;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;


public class JsonReader {

  public JsonNode  readJsonArrayFromUrl(String url) {
    Promise<WS.Response> res = WS.url(url).get();

    Response result=res.get();
    JsonNode json=result.asJson();
    //with a system out i can see that the json is parsed correctly
    return json;
  }
}
