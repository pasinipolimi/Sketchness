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

          public GameEvent(String channel) 
          {
              this.channel=channel;
          }
          
          public GameEvent(String message, String channel) 
          {
              this.channel=channel;
          }

        public String getChannel() {
            return channel;
        }
    }
    
    public static class PlayerJoin extends GameEvent
    {

        public PlayerJoin(String message, String channel) {
            super(message, channel);
        }
    }
    
    public static class PlayerQuit extends GameEvent
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
}
