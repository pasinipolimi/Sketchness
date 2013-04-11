package models;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.libs.Json;
import play.mvc.WebSocket;



public class Painter extends Player implements Comparable<Painter>{
    
	public String name;
	public String color="red";
	public int size=5;
        //ROLE TYPES: UNDEFINED, SKETCHER, GUESSER
        public String role;
        public boolean guessed;
        public boolean hasBeenSketcher;
        private int nModulesReceived;

    public final WebSocket.Out<JsonNode> channel;

    public Painter(WebSocket.Out<JsonNode> channel) {
        this.channel = channel;
        this.role = "UNDEFINED";
        hasBeenSketcher=false;
        nModulesReceived=0;
    }

    public Painter(String name, String color, String role, int size, WebSocket.Out<JsonNode> channel) {
		this.name = name;
		this.color = color;
		this.size = size;
                this.role = role;
                this.channel = channel;
                nModulesReceived=0;
	}
    
    public Painter(String name, Boolean hasBeenSketcher) {
		this.name = name;
		this.hasBeenSketcher = hasBeenSketcher;
                channel=null;
                nModulesReceived=0;
	}

    public void updateFromJson(JsonNode json) {
        if(json.has("name"))
            this.name = json.get("name").getTextValue();
        if(json.has("color"))
            this.color = json.get("color").getTextValue();
        if(json.has("size"))
            this.size = json.get("size").getIntValue();
        if(json.has("role"))
            this.role = json.get("role").getTextValue();
        if(json.has("guessed"))
            this.guessed = Boolean.getBoolean(json.get("guessed").getTextValue());
    }
    
    public JsonNode toJson() {
        ObjectNode json = Json.newObject();
        json.put("name", this.name);
        json.put("color", this.color);
        json.put("size", this.size);
        json.put("role", this.role);
        json.put("guessed", this.guessed);
        return  json;
    }

    @Override
    public String toString() {
        return "Painter{" +
                "name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", size=" + size  +
                ", role='" + role + '\'' +
                ", guessed='" + guessed + '\'' +
                '}';
    }
    
    public void setCorrectGuess()
    {
        guessed=true;
    }

    @Override
    public int compareTo(Painter t) {
        //If we have more points we are in lower positions
        if(this.getPoints()>t.getPoints())
            return -1;
        else if (this.getPoints()<t.getPoints())
            return 1;
        return 0;
    }

    public void setnModulesReceived(int nModulesReceived) {
        this.nModulesReceived = nModulesReceived;
    }

    public int getnModulesReceived() {
        return nModulesReceived;
    }
    
  
    
    
    
    
}