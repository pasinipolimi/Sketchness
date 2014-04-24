package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import play.mvc.WebSocket;

public class Painter extends Player implements Comparable<Painter> {

    public String name;
    //ROLE TYPES: UNDEFINED, SKETCHER, GUESSER
    public String role;
    public Boolean guessed;
    public Boolean hasBeenSketcher;
    public final WebSocket.Out<JsonNode> channel;

    public Painter(WebSocket.Out<JsonNode> channel) {
        this.channel = channel;
        this.role = "UNDEFINED";
        hasBeenSketcher = false;
    }

    public Painter(String name, String color, String role, int size, WebSocket.Out<JsonNode> channel) {
        this.name = name;
        this.role = role;
        this.channel = channel;
    }

    public Painter(String name, Boolean hasBeenSketcher) {
        this.name = name;
        this.hasBeenSketcher = hasBeenSketcher;
        channel = null;
    }

    public void updateFromJson(JsonNode json) {
        if (json.has("name")) {
            this.name = json.get("name").textValue();
        }
        if (json.has("role")) {
            this.role = json.get("role").textValue();
        }
        if (json.has("guessed")) {
            this.guessed = Boolean.getBoolean(json.get("guessed").textValue());
        }
    }

    public JsonNode toJson() {
        ObjectNode json = Json.newObject();
        json.put("name", this.name);
        json.put("role", this.role);
        json.put("guessed", this.guessed);
        return json;
    }

    @Override
    public String toString() {
        return "Painter{"
                + "name='" + name + '\''
                + ", role='" + role + '\''
                + ", guessed='" + guessed + '\''
                + '}';
    }

    public void setCorrectGuess() {
        guessed = true;
    }

    @Override
    public int compareTo(Painter t) {
        //If we have more points we are in lower positions
        if (this.getPoints() > t.getPoints()) {
            return -1;
        } else if (this.getPoints() < t.getPoints()) {
            return 1;
        }
        return 0;
    }
}

enum Roles {

    UNDEFINED, SKETCHER, GUESSER
}