package utils;

import org.codehaus.jackson.JsonNode;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;


public class JsonReader {

  public JsonNode  readJsonArrayFromUrl(String url) {
    Promise<WS.Response> res = null;  
    try{
        res = WS.url(url).get();
    }
    catch(Exception mf)
    {
        throw new IllegalArgumentException("The URL that has been provided is not valid");
    }
    if(res!=null)
    {
        Response result=res.get();
        JsonNode json=result.asJson();
        //with a system out i can see that the json is parsed correctly
        return json;
    }
    else
        throw new IllegalStateException("The response is not valid.");
  }
}
