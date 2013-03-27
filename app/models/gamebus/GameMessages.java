/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package models.gamebus;

/**
 *
 * @author Leyart
 */
public class GameMessages {

    public static class GameEvent
    {
        String channel;
        protected String message;

          public GameEvent(String channel) 
          {
              this.channel=channel;
          }
          
          public GameEvent(String message, String channel) 
          {
              this.channel=channel;
              this.message=message;
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
    
    public static class PlayerQuit extends PlayerJoin
    {

        public PlayerQuit(String message, String channel) {
            super(message, channel);
        }
    }
    
    public static class GameStart extends GameEvent
    {
        public GameStart(String channel) {
            super(channel);
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
