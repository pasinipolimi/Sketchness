package models.chat;

import models.factory.Factory;
import org.codehaus.jackson.JsonNode;
import play.mvc.WebSocket;






public class ChatRoomFactory extends Factory{


  public static void createChat(final String username, final String room, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception
  {
      create(username, room, in, out, Chat.class);
  }
}
