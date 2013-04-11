/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils.gamebus;

import org.codehaus.jackson.node.ObjectNode;

/**
 *
 * @author Leyart
 */
public class GameMessages {

    public static class GameEvent
    {
        String channel;
        protected String type;
        protected String message;
        protected ObjectNode object;

          public GameEvent(String channel) 
          {
              this.channel=channel;
              this.message="none";
              this.type="generic";
          }
          
          public GameEvent(String message, String channel) 
          {
              this.channel=channel;
              this.message=message;
              this.type="generic";
          }
          
          public GameEvent(String message, String channel, String type)
          {
              this(message,channel);
              this.type=type;
          }

        public String getType() {
            return type;
        }

        public void setObject(ObjectNode object) {
            this.object = object;
        }

        public ObjectNode getObject() {
            return object;
        }
        
        
          
          

        public String getChannel() {
            return channel;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public static class PlayerJoin extends GameEvent
    {

        public PlayerJoin(String message, String channel) {
            super(message, channel);
        }
        
        public String getUser()
        {
            return message;
        }
    }
    
    public static class PlayerQuit extends GameEvent
    {

        public PlayerQuit(String message, String channel) {
            super(message, channel);
        }
        
        public String getUser()
        {
            return message;
        }
    }
    
    public static class GameStart extends GameEvent
    {
        public GameStart(String sketcher, String channel) {
            super(sketcher,channel);
        }
    }
    
    public static class Guessed extends GameEvent
    {
        String username;
        public Guessed(String guesser,String channel)
        {
            super(channel);
            username=guesser;
        }
    }
    
    public static class SystemMessage extends GameEvent
    {
        public SystemMessage(String message,String channel)
        {
            super(message,channel);
        }
    }
}
