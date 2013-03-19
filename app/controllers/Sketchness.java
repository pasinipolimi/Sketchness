package controllers;

import akka.actor.ActorRef;
import play.mvc.*;

import org.codehaus.jackson.*;

import views.html.*;

import models.*;



public class Sketchness extends Controller {
  
    static PaintRoom env = new PaintRoom("Public");
  
    /**
     * Display the home page.
     */
    public static Result index() {
        return ok(index.render());
    }
    
    /**
     * Display the chat room.
     */
    public static Result chatRoom(final String username, final String roomName) {
        /*Fix the errors with all the possible cases*/
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Sketchness.index());
        }
        return ok(chatRoom.render(username,roomName));
    }
    
    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chat(final String username, final String roomName) {
        return new WebSocket<JsonNode>() {
            
            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                
                // Join the chat room.
                try { 
                    ChatRoom.join(username, roomName, in, out, env);
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
	public static WebSocket<JsonNode> stream() {
        
        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(In<JsonNode> in, Out<JsonNode> out) {
                try{
                    env.createPainter(in, out);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    
    }
  
}
