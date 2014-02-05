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

public class Login extends Controller {

	public static boolean actorActive = false;

	/**
	 * Retrive the user from the session
	 */
	public static User getLocalUser(final Http.Session session) {
		final AuthUser currentAuthUser = PlayAuthenticate.getUser(session);
		final User localUser = User.findByAuthUserIdentity(currentAuthUser);
		return localUser;
	}

	public static Result login() {

		if (!actorActive) {
			Akka.system()
					.scheduler()
					.schedule(Duration.create(0, TimeUnit.MILLISECONDS),
							Duration.create(2, TimeUnit.MINUTES),
							new Runnable() {
								@Override
								public void run() {
									// Logger.debug("ciao");
									actorActive = true;
									models.IsOnline.checkOnline();

								}
							}, Akka.system().dispatcher());
		}

		final User localUser = getLocalUser(session());

		if (localUser != null) {

			return ok(lobby.render(localUser));
		} else {
			return ok(sketchness_login
					.render(MyUsernamePasswordAuthProvider.LOGIN_FORM));
		}

	}

	public static Result doLogin() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<MyLogin> filledForm = MyUsernamePasswordAuthProvider.LOGIN_FORM
				.bindFromRequest();
		String loggedMail;
		final User localUser = getLocalUser(session());

		if (localUser != null) {

			loggedMail = localUser.email;
		} else {
			loggedMail = filledForm.field("email").value();
		}
		if (filledForm.hasErrors()) {
			// User did not fill everything properly
			return badRequest(sketchness_login.render(filledForm));
		} else {
			// if there is already someone logged with the same browser prevent
			// the login
			if (!loggedMail.equals(filledForm.field("email").value())) {
				flash(Application.FLASH_ERROR_KEY,
						Messages.get("error.userInBrowser"));
				return badRequest(sketchness_login.render(filledForm));
			}
			// Everything was filled and no other users in session
			return UsernamePasswordAuthProvider.handleLogin(ctx());
		}
	}

}
