package models.chat;

import play.libs.*;
import play.libs.F.*;
import play.i18n.Messages;
import play.mvc.*;
import play.Logger;

import org.codehaus.jackson.node.*;
import org.codehaus.jackson.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import models.factory.GameRoom;

import utils.*;
import utils.gamebus.GameBus;
import utils.gamebus.GameMessages.*;
import utils.LanguagePicker;
import utils.gamebus.GameEventType;



public class Chat extends GameRoom {

    Room  roomChannel;
    String  currentGuess;
    Boolean gameStarted=false;
    Boolean askTag=false;
    String askTagSketcher;
    // Members of this room.
    ConcurrentHashMap<String, WebSocket.Out<JsonNode>> playersMap = new ConcurrentHashMap<>();

    public Chat() {
        super(Chat.class);
    }
    
    @Override
    public void onReceive(Object message) throws Exception {  
        
        if(message instanceof Room)
        {
            this.roomChannel=((Room)message);
            Logger.info("[CHAT] "+roomChannel.getRoom()+" created.");
        }
        if(message instanceof SystemMessage)
        {
            notifyAll(ChatKind.system, "Sketchness", ((SystemMessage)message).getMessage());
        }
        if(message instanceof Join) 
        {
            handleJoin((Join)message);
        }
        else
            if(message instanceof GameEvent) {
                GameEvent event= (GameEvent)message;
                //[TODO] E' una porcata, bisogna sistemarlo ma per ora è un fix veloce
                if(self().path().toString().endsWith("lobby"))
                {
                    switch(event.getType()) {
                        case talk:handleTalk(event.getUsername(),event.getMessage());break;
                        case quit:handleQuitter(event.getMessage());
                    }
                }
                else
                {
                    switch(event.getType()) {
                        case gameStarted :gameStarted=true;break;
                        case gameEnded:killActor();gameStarted=false;break;
                        case talk:handleTalk(event.getUsername(),event.getMessage());break;
                        case task:retrieveTask(event.getObject());break;
                        case askTag:handleAskTag(event);break;
                        case skipTask:askTag=false;break; //Reset to the initial status, even if we don't know if it was a normal task or a question
                        case quit:handleQuitter(event.getMessage());
                    }
                }
            }
            else {
                unhandled(message);
            } 
    }
    
    private void handleAskTag(GameEvent message)
    {
        askTagSketcher=message.getMessage();
        notifyAll(ChatKind.system, "Sketchness", Messages.get(LanguagePicker.retrieveLocale(),"asktag"));askTag=true;
    }
    
    private void handleJoin(Join message)
    {
        // Check if this username is free.
        if(playersMap.containsKey(message.getUsername())) {
            getSender().tell(Messages.get(LanguagePicker.retrieveLocale(),"usernameused"),this.getSelf());
        } 
        else if(!gameStarted) 
        {
            playersMap.put(message.getUsername(), message.getChannel());
            notifyAll(ChatKind.join, message.getUsername(), Messages.get(LanguagePicker.retrieveLocale(),"join"));
            notifyMemberChange();
            GameBus.getInstance().publish(new GameEvent(message.getUsername(), roomChannel,GameEventType.join));
            getSender().tell("OK",this.getSelf());
            Logger.debug("[CHAT] added player "+message.getUsername());
        }
        else
        {
            getSender().tell(Messages.get(LanguagePicker.retrieveLocale(),"matchstarted"),this.getSelf());
        }
    }
    
    
    private void handleTalk(String username, String text)
    {
            // Received a Talk message
            //If we are asking the sketcher for a tag, then save the tag
            if(askTag&&username.equals(askTagSketcher))
            {
                askTag=false;
                GameBus.getInstance().publish(new GameEvent(text,username,roomChannel,GameEventType.tag));
            }
            else
            if(gameStarted)
	    {
                 //Compare the message sent with the tag in order to establish if we have a right guess
                 levenshteinDistance distanza = new levenshteinDistance();
                 if(text!=null)
                 {
                    switch(distanza.computeLevenshteinDistance(text, currentGuess)){
                           case 0: GameBus.getInstance().publish(new GameEvent(username,roomChannel,GameEventType.guessed));break;
                           case 1: notifyAll(ChatKind.talkNear, username, text);break;
                           case 2: notifyAll(ChatKind.talkWarning, username, text);break;
                           default: notifyAll(ChatKind.talkError, username, text);break;
                   }
                }
            }
            else
                //The players are just chatting, not playing
                notifyAll(ChatKind.talk, username, text);
    }
    
    private  void handleQuitter(String quitter) throws InterruptedException {
        for (Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(quitter)) {
                //Close the websocket
                entry.getValue().close();
                playersMap.remove(quitter);
                notifyAll(ChatKind.quit, quitter, Messages.get(LanguagePicker.retrieveLocale(),"quit"));
                notifyMemberChange();
                Logger.debug("[CHAT] "+quitter+" has disconnected.");
                GameBus.getInstance().publish(new GameEvent(quitter,roomChannel,GameEventType.quit));
            } 
        }
    }
    
    private void retrieveTask(ObjectNode task) {
        currentGuess=task.get("tag").asText();
    }
    
    // Send a Json event to all members
    private void notifyAll(ChatKind kind, String user, String text) {
        for(WebSocket.Out<JsonNode> channel: playersMap.values()) {
            
            ObjectNode event = Json.newObject();
            event.put("type", kind.name());
            event.put("user", user);
            event.put("message", text);
            
            ArrayNode m = event.putArray("members");
            for(String u: playersMap.keySet()) {
                m.add(u);
            }
            
            channel.write(event);
        }
    }   
    
    // Send the updated list of members to the users
    private void notifyMemberChange() {
        for(WebSocket.Out<JsonNode> channel: playersMap.values()) {
            
            ObjectNode event = Json.newObject();
            event.put("type", ChatKind.membersChange.name());
            
            ArrayNode m = event.putArray("members");
            for(String u: playersMap.keySet()) {
                m.add(u);
            }
            
            channel.write(event);
        }
    } 
}
enum ChatKind
{
    system,join,quit,talk,talkNear,talkWarning,talkError,membersChange
}
