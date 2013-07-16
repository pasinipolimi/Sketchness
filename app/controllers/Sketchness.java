package controllers;

import akka.actor.ActorSelection;
import java.util.ArrayList;
import models.chat.ChatRoomFactory;
import play.mvc.*;

import org.codehaus.jackson.JsonNode;

import views.html.index;
import views.html.chatRoom;
import views.html.lobby;

import models.game.GameRoomFactory;
import models.paint.Paint;
import models.paint.PaintRoomFactory;
import play.i18n.Lang;
import play.libs.Akka;
import static play.mvc.Controller.request;
import static play.mvc.Results.ok;
import utils.LanguagePicker;



public class Sketchness extends Controller {

    /**
     * Display the home page.
     */
    public static Result index() {
        if(LanguagePicker.retrieveIsoCode().equals(""))
        {
            LanguagePicker.setLanguage(Lang.preferred(request().acceptLanguages()));
        }
        return ok(index.render());
    }
    
    /**
     * Display the chat room.
     */
    public static Result chatRoom(final String username, final String roomName) throws Exception {
        if(LanguagePicker.retrieveIsoCode().equals(""))
        {
            LanguagePicker.setLanguage(Lang.preferred(request().acceptLanguages()));
        }
        /*Fix the errors with all the possible cases*/
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Sketchness.index());
        }
        GameRoomFactory.createGame(roomName);
        return ok(chatRoom.render(username,roomName));
    }
    
    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chatStream(final String username, final String roomName) {
        return new WebSocket<JsonNode>() {
            
            // Called when the Websocket Handshake is done.
            @Override
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                
                // Join the chat room.
                try { 
                    ChatRoomFactory.createChat(username, roomName, in, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }
	
    
    /**
     * 
     * Handle the paintroom websocket 
     */
    public static WebSocket<JsonNode> paintStream(final String username, final String roomName) {

    return new WebSocket<JsonNode>() {
        @Override
        public void onReady(In<JsonNode> in, Out<JsonNode> out) {
            try{
                PaintRoomFactory.createPaint(username, roomName, in, out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        };
    }
    
    
    /**
     * Display the lobby.
     */
    public static Result lobby(final String username) throws Exception {
        if(LanguagePicker.retrieveIsoCode().equals(""))
        {
            LanguagePicker.setLanguage(Lang.preferred(request().acceptLanguages()));
        }
        /*Fix the errors with all the possible cases*/
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Sketchness.index());
        }
        existingRooms(Paint.class);
        return ok(lobby.render(username));
    }
    
    
    private static ArrayList<String> existingRooms(Class roomClass)
    {
        ActorSelection existingRooms = Akka.system().actorSelection("akka://application/user/");
        String roomPrefix=roomClass.getSimpleName();
        return null;
    }
    
  
}
