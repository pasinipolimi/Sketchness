package controllers;

import models.paint.Paint;
import models.chat.ChatRoomFactory;
import akka.actor.ActorRef;
import play.mvc.*;

import org.codehaus.jackson.*;

import views.html.*;

import models.*;
import models.game.GameRoomFactory;
import models.paint.PaintRoomFactory;
import play.Logger;



public class Sketchness extends Controller {
  
    /**
     * Display the home page.
     */
    public static Result index() {
        return ok(index.render());
    }
    
    /**
     * Display the chat room.
     */
    public static Result chatRoom(final String username, final String roomName) throws Exception {
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
  
}
