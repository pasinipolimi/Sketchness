package controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import models.User;
import play.mvc.*;

import org.codehaus.jackson.JsonNode;

import views.html.gameRoom;
import views.html.lobby;
import views.html.leaderboard;

import models.game.GameFactory;
import models.lobby.LobbyFactory;
import play.i18n.Lang;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.gamemanager.GameManager;

import java.util.Map;

public class Sketchness extends Controller {

    /**
     * Display the home page.
     */
    public static Result index() {
        if (LanguagePicker.retrieveIsoCode().equals("")) {
            LanguagePicker.setLanguage(Lang.preferred(request().acceptLanguages()));
        }
        return redirect(routes.Application.login());
    }

    /**
     * Retrive the user from the session
     */
    public static User getLocalUser(final Http.Session session) {
        final AuthUser currentAuthUser = PlayAuthenticate.getUser(session);
        final User localUser = User.findByAuthUserIdentity(currentAuthUser);
        return localUser;
    }

    /**
     * Display the chat room.
     */
    @Restrict(@Group(Application.USER_ROLE))
    public static Result gameRoom() throws Exception {


        final User localUser = getLocalUser(session());
        String username = localUser.name;
        String numero = "3";

        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        String roomName = values.get("room")[0];
        if(values.containsKey("nPlayers")){
            numero = values.get("nPlayers")[0];
        }
        Integer nPlayers = Integer.valueOf(numero);

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
            return redirect(routes.Sketchness.lobby());
        }
        if (nPlayers < 0 || nPlayers > 4) {
            nPlayers = 3;
        }
        //Force the room name to be without spaces. We cannot create actors with
        //spaces in it.
        roomName = roomName.replaceAll(" ", "");
        return ok(gameRoom.render(localUser, roomName,nPlayers));
    }


    /**
     * Handle the chat websocket.
     */
    @Restrict(@Group(Application.USER_ROLE))
    public static WebSocket<JsonNode> lobbyStream() {

        final User localUser = getLocalUser(session());
        final String username = localUser.name;
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
    @Restrict(@Group(Application.USER_ROLE))
    public static WebSocket<JsonNode> gameStream( final String roomName, final Integer players) {

        final User localUser = getLocalUser(session());
        final String username = localUser.name;

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
    @Restrict(@Group(Application.USER_ROLE))
    public static Result lobby() throws Exception {

        final User localUser = getLocalUser(session());
        String username = localUser.name;

        if (LanguagePicker.retrieveIsoCode().equals("")) {
            LanguagePicker.setLanguage(Lang.preferred(request().acceptLanguages()));
        }
        /*Fix the errors with all the possible cases*/
        if (username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Sketchness.index());
        }
        GameManager.getInstance().getCurrentGames();
        return ok(lobby.render(localUser));
    }

    /**
     * Display the leaderboard page.
     */
    @Restrict(@Group(Application.USER_ROLE))
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

    /**
     * Keep a player online
     */
    public static Result keepOnline(){

        final User localUser = getLocalUser(session());
        String username = localUser.name;
        models.IsOnline.keepOnline(username);

        return ok();
    }


}
