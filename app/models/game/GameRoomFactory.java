/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package models.game;

import models.factory.Factory;

public class GameRoomFactory extends Factory{


  public static void createGame(final String room) throws Exception
  {
      create(room, Game.class);
  }
}
