package controllers;

import play.mvc.*;

import org.codehaus.jackson.*;

import views.html.*;

import models.*;

public class Application extends Controller {
  
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
    public static Result chatRoom(String username) {
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Application.index());
        }
        return ok(chatRoom.render(username,env));
    }
    
    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chat(final String username) {
        return new WebSocket<JsonNode>() {
            
            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                
                // Join the chat room.
                try { 
                    ChatRoom.join(username, in, out, env);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }
	
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
