package controllers;

import java.util.concurrent.TimeUnit;

import models.User;
import play.data.Form;
import play.i18n.Messages;
import play.libs.Akka;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import providers.MyUsernamePasswordAuthProvider;
import providers.MyUsernamePasswordAuthProvider.MyLogin;
import scala.concurrent.duration.Duration;
import views.html.lobby;
import views.html.sketchness_login;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.user.AuthUser;
import utils.LoggerUtils;

public class Login extends Controller {

	public static boolean actorActive = false;

	/**
	 * Retrieve the user from the session
	 */
	public static User getLocalUser(final Http.Session session) {
		final AuthUser currentAuthUser = PlayAuthenticate.getUser(session);
		final User localUser = User.findByAuthUserIdentity(currentAuthUser);
		return localUser;
	}

	public static Result login() {
		LoggerUtils.debug("LOGIN","Requested login form");

		if (!actorActive) {
			Akka.system()
					.scheduler()
					.schedule(Duration.create(0, TimeUnit.MILLISECONDS),
							Duration.create(2, TimeUnit.MINUTES),
							new Runnable() {
								@Override
								public void run() {
									actorActive = true;
									models.IsOnline.checkOnline();

								}
							}, Akka.system().dispatcher());
		}

		final User localUser = getLocalUser(session());
		if (localUser != null) {
			LoggerUtils.debug("LOGIN","User already logged in, enter in the lobby");
			return ok(lobby.render(localUser));
		} else {
			LoggerUtils.debug("LOGIN","User is not already logged in, enter in the login form page");
			return ok(sketchness_login
					.render(MyUsernamePasswordAuthProvider.LOGIN_FORM));
		}

	}

	public static Result doLogin() {
		LoggerUtils.debug("LOGIN","Performing login");
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<MyLogin> filledForm = MyUsernamePasswordAuthProvider.LOGIN_FORM
				.bindFromRequest();

		final User localUser = getLocalUser(session());

		if (filledForm.hasErrors()) {
			// User did not fill everything properly
			LoggerUtils.error("LOGIN","User did not fill everything properly in the login form");
			return badRequest(sketchness_login.render(filledForm));
		} else {
			// if there is already someone logged with the same browser prevent
			// the login
			if (localUser != null) {
				LoggerUtils.error("LOGIN","There is already someone logged with the same browser prevent the login, return error");
				flash(Application.FLASH_ERROR_KEY,
						Messages.get("error.userInBrowser"));
				return badRequest(sketchness_login.render(filledForm));
			}
			// Everything was filled and no other users in session
			LoggerUtils.debug("LOGIN","The login form was filled properly, handling login..");
			return UsernamePasswordAuthProvider.handleLogin(ctx());
		}
	}

}
