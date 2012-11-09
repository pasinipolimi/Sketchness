package controllers;


import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import play.mvc.*;

import org.codehaus.jackson.*;

import views.html.*;

import models.*;
import play.mvc.Http.RequestBody;

public class Application extends Controller {
  
    static PaintRoom env = new PaintRoom("Public");
  
    /**
     * Display the home page.
     */
    public static Result index() {
    	String user = session("connected");
    	System.out.println(user);
        return ok(index.render(user));
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
     * Display the chat room.
     */
    public static Result chatRoomPost() {
    	Player newPlayer = new Player();
    	Map<String, String[]> dataUser = request().body().asFormUrlEncoded();	//POST Data converted in Map<String, String[]> format
    	boolean logged = newPlayer.sketchenessLogin(dataUser);
    	if(logged){
    		session("connected", dataUser.get("mail")[0]);
    	}
        
    	return redirect(routes.Application.index());
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
	public static Result register() {
		session("prova", "gabriele casati");
		return ok(register.render());
	}
	
	public static Result postRegister() {
		Map<String, String[]> dataUser = request().body().asFormUrlEncoded();	//POST Data converted in Map<String, String[]> format
		Player newPlayer = new Player();
		Map<String, String> result = newPlayer.userSave(dataUser);											//Save data user
		if(result.get("queryResult").equals("ko")){
			String ErrorMsg = "";
			for(String key :result.keySet()){
				String value = result.get(key);
				if(value == "error"){
					ErrorMsg = ErrorMsg + key;
				}
			}
			flash("error", "Error field: " + ErrorMsg);
			return redirect(routes.Application.index());
		}
		
		return ok(register.render());
		
    }
	
	public static Result logout() {
		session().clear();
		return ok(logout.render());
	}
	
}
