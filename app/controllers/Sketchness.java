package controllers;

import play.mvc.*;

import org.codehaus.jackson.JsonNode;

import views.html.index;
import views.html.gameRoom;
import views.html.lobby;
import views.html.leaderboard;

import models.game.GameFactory;
import models.lobby.LobbyFactory;
import play.i18n.Lang;
import static play.mvc.Controller.flash;
import static play.mvc.Controller.request;
import static play.mvc.Results.ok;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.gamemanager.GameManager;

public class Sketchness extends Controller {

    /**
     * Display the home page.
     */
    public static Result index() {
        if (LanguagePicker.retrieveIsoCode().equals("")) {
            LanguagePicker.setLanguage(Lang.preferred(request().acceptLanguages()));
        }
        return ok(index.render());
    }

    /**
     * Display the chat room.
     */
    public static Result gameRoom(final String username, String roomName, Integer nPlayers) throws Exception {
        if (LanguagePicker.retrieveIsoCode().equals("")) {
            LanguagePicker.setLanguage(Lang.preferred(request().acceptLanguages()));
        }
        /*Fix the errors with all the possible cases*/
        if (username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Sketchness.index());
        }
        if (roomName.trim().equals("")) {
            flash("error", "Wrong room id.");
            return redirect(routes.Sketchness.lobby(username));
        }
        if (nPlayers < 0 || nPlayers > 4) {
            nPlayers = 3;
        }
        //Force the room name to be without spaces. We cannot create actors with 
        //spaces in it.
        roomName = roomName.replaceAll(" ", "");
        return ok(gameRoom.render(username, roomName,nPlayers));
    }


    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> lobbyStream(final String username) {
        return new WebSocket<JsonNode>() {
            // Called when the Websocket Handshake is done.
            @Override
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {

                // Join the chat room.
                try {
                    LobbyFactory.createLobby(username, in, out);
                } catch (Exception ex) {
                    LoggerUtils.error("LOBBY", ex);
                }
            }
        };
    }

    /**
     *
     * Handle the paintroom websocket
     */
    public static WebSocket<JsonNode> gameStream(final String username, final String roomName, final Integer players) {

        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(In<JsonNode> in, Out<JsonNode> out) {
                try {
                    GameFactory.createGame(username, roomName, players, in, out);
                } catch (Exception ex) {
                    LoggerUtils.error("PAINT", ex);
                }
            }
        };
    }

    /**
     * Display the lobby.
     */
    public static Result lobby(final String username) throws Exception {
        if (LanguagePicker.retrieveIsoCode().equals("")) {
            LanguagePicker.setLanguage(Lang.preferred(request().acceptLanguages()));
        }
        /*Fix the errors with all the possible cases*/
        if (username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Sketchness.index());
        }
        GameManager.getInstance().getCurrentGames();
        return ok(lobby.render(username, null));
    }

    /**
     * Display the leaderboard page.
     */
    public static Result leaderboard(final String username, String result) throws Exception {
        result = result.substring(1);
        String[] splitted = result.split(":");
        for (int x = 0; x < splitted.length; x = x + 2) {
            flash("score" + ((x / 2) + 1), "");
            flash("name" + ((x / 2) + 1), splitted[x]);
            flash("points" + ((x / 2) + 1), splitted[x + 1]);
        }
        return ok(leaderboard.render(username, null));
    }
}
