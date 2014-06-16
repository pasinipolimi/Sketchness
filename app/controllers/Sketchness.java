package controllers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.json.JSONException;

import models.User;
import models.game.GameFactory;
import models.lobby.LobbyFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.WebSocket;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.Renderer;
import utils.gamemanager.GameManager;
import views.html.gameRoom;
import views.html.leaderboard;
import views.html.handleError;
import views.html.lobby;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;

import static play.mvc.Controller.flash;
import static play.mvc.Results.redirect;

public class Sketchness extends Controller {

	/**
	 * Display the home page.
	 */
	public static Result index() {
		if (LanguagePicker.retrieveIsoCode().equals("")) {
			LanguagePicker.setLanguage(Lang.preferred(request()
					.acceptLanguages()));
		}
		return redirect(routes.Application.login());
	}

	/**
	 * Retrieve the user from the session
	 */
	public static User getLocalUser(final Http.Session session) {
		final AuthUser currentAuthUser = PlayAuthenticate.getUser(session);
		return User.findByAuthUserIdentity(currentAuthUser);
	}

	/**
	 * Display the chat room.
	 */
	@Restrict(@Group(Application.USER_ROLE))
    public static Result gameRoom() throws Exception {

		final User localUser = getLocalUser(session());
		final String username = localUser.name;
		LoggerUtils.debug("SIGNUP","Opening the chatroom for user: " + username);

		String number = "3";

		final Map<String, String[]> values = request().body()
				.asFormUrlEncoded();
		String roomName = values.get("room")[0];
		if (values.containsKey("nPlayers")) {
                    number = values.get("nPlayers")[0];
		}
                Integer nPlayers = Integer.valueOf(number);

		if (LanguagePicker.retrieveIsoCode().equals("")) {
			LanguagePicker.setLanguage(Lang.preferred(request()
					.acceptLanguages()));
		}

                if(!(roomName.contains("[en]")||roomName.contains("[it]"))){
                    String lang = LanguagePicker.retrieveLocale().language();
                    roomName += "["+lang+"]";
                }
		/* Fix the errors with all the possible cases */
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
		// with
		// spaces in it.
		roomName = roomName.replaceAll(" ", "");
                return ok(gameRoom.render(localUser, roomName,nPlayers));
	}


	/**
          * Handle the chat websocket.
          * @return Opened Websocket 
	 */
	@Restrict(@Group(Application.USER_ROLE))
	public static WebSocket<JsonNode> lobbyStream() {

		final User localUser = getLocalUser(session());
		final String username = localUser.name;
                try {
                    return new WebSocket<JsonNode>() {
                            // Called when the Websocket Handshake is done.
                            @Override
                            public void onReady(final WebSocket.In<JsonNode> in,
                                            final WebSocket.Out<JsonNode> out) {

                                    // Join the chat room.
                                    try {
                                            LobbyFactory.createLobby(username, in, out);
                                    } catch (final Exception ex) {
                                            LoggerUtils.error("LOBBY", ex);
                                            GameFactory.handleError("LOAD", in, out);
                                    }
                            }
                    };
                }
                catch(final Exception e) {
                   return new WebSocket<JsonNode>() {

                       @Override
                       public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
                           LoggerUtils.error("LOBBY", e);
                           GameFactory.handleError("LOAD", in, out);
                       }
                   }; 
                }
	}

	/**
	 * 
         * Handle the gameStream websocket
	 */
	@Restrict(@Group(Application.USER_ROLE))
        public static WebSocket<JsonNode> gameStream(final String roomName, final Integer players) {
                Http.Session current = session();
		final User localUser = getLocalUser(session());
		final String username = localUser.name;
                try {
                    return new WebSocket<JsonNode>() {
                            @Override
                            public void onReady(final In<JsonNode> in, final Out<JsonNode> out) {
                                    try {
                                        GameFactory.createGame(username, roomName, players, in, out);
                                    } catch (final Exception ex) {
                                            LoggerUtils.error("GAME", ex);
                                            GameFactory.handleError("LOAD", in, out);
                                    }
                            }
                    };
                }
                catch(final Exception e) {
                   return new WebSocket<JsonNode>() {

                       @Override
                       public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
                           LoggerUtils.error("GAME", e);
                           GameFactory.handleError("LOAD", in, out);
                       }
                   }; 
                }
	}

	/**
	 * Display the lobby.
	 */
	@Restrict(@Group(Application.USER_ROLE))
	public static Result lobby() throws Exception {
		final User localUser = getLocalUser(session());
		final String username = localUser.name;

		if (LanguagePicker.retrieveIsoCode().equals("")) {
			LanguagePicker.setLanguage(Lang.preferred(request()
					.acceptLanguages()));
		}
		/* Fix the errors with all the possible cases */
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
	public static Result leaderboard(final String username, String result)
			throws Exception {
                String[] splitted;
                if(result.equals("")||result==null) {
                    result="";
                    flash("score" + (1), "error");
                    flash("name" + (1), "error");
                    flash("points" + (1), "error");
                }
                else {
                    result = result.substring(1);
                    splitted = result.split(":");
                    for (int x = 0; x < splitted.length; x = x + 2) {
                            flash("score" + ((x / 2) + 1), "");
                            flash("name" + ((x / 2) + 1), splitted[x]);
                            flash("points" + ((x / 2) + 1), splitted[x + 1]);
                    }
                }
		return ok(leaderboard.render(username, null));
	}
        
        @Restrict(@Group(Application.USER_ROLE))
	public static Result handleError() {
		return ok(handleError.render());
	}

    /**
     * Keep a player online
     */
	public static Result keepOnline() {

		final User localUser = getLocalUser(session());
                if(localUser!=null) {
                    final String username = localUser.name;
                    models.IsOnline.keepOnline(username);
                }
		return ok();
	}
	
	/**
	 * Retrieve segmentations of an image to simulate sketcher (bot)
	 * @return the first polyline segmentation of the image
	 * @throws JSONException
	 */
	public static Result segmentationImageCall() throws JSONException {
		
		final String imageId = request().getHeader("selected");
		final String result = Renderer.segmentationImageCall(imageId);
		return ok(result);

	}		

	/**
	 * Retrieve tags of an image to simulate guesser (bot)
	 * @return the first tag of the image
	 * @throws JSONException
	 */
	public static Result taggingImageCall() throws JSONException {
		
		final String imageId = request().getHeader("selected");
		final String result = Renderer.taggingImageCall(imageId);
		return ok(result);

		
	}	


	public static Result bentleyOttmann() throws JSONException {
		
		final String points = request().getHeader("points");

		
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory factory = mapper.getJsonFactory();
		JsonParser jp;
		JsonNode actualObj = null;
		try {
			jp = factory.createJsonParser(points);
			actualObj = mapper.readTree(jp);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String finalPoints = Renderer.retrievePoints(actualObj);
		
		return ok(finalPoints);

	}
	
	
	
}

//}
