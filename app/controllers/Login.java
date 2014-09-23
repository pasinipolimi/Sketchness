package controllers;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.exceptions.AuthException;
import com.feth.play.module.pa.providers.AuthProvider;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.user.AuthUser;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import models.User;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import play.libs.Akka;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import providers.MyUsernamePasswordAuthProvider;
import providers.MyUsernamePasswordAuthProvider.MyLogin;
import scala.concurrent.duration.Duration;
import utils.LoggerUtils;
import views.html.lobby;
import views.html.sketchness_login;
import play.Configuration;
import play.Logger;
import play.Play;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.Session;
import play.mvc.Result;

import play.*;
import play.Logger;
import play.libs.F.*;
import play.libs.F.Promise;
import play.mvc.*;
import play.mvc.Http.Response;
import static play.mvc.Results.*;

public class Login extends Controller {

	public static boolean actorActive = false;

	/**
	 * Retrieve the user from the session
	 */
         public static final String SETTING_KEY_PLAY_AUTHENTICATE = "play-authenticate";
	private static final String SETTING_KEY_AFTER_AUTH_FALLBACK = "afterAuthFallback";
	private static final String SETTING_KEY_AFTER_LOGOUT_FALLBACK = "afterLogoutFallback";
	private static final String SETTING_KEY_ACCOUNT_MERGE_ENABLED = "accountMergeEnabled";
	private static final String SETTING_KEY_ACCOUNT_AUTO_LINK = "accountAutoLink";
	private static final String SETTING_KEY_ACCOUNT_AUTO_MERGE = "accountAutoMerge";
        
         private static final String PAYLOAD_KEY = "p";

	public static void noCache(final Response response) {
		// http://stackoverflow.com/questions/49547/making-sure-a-web-page-is-not-cached-across-all-browsers
		response.setHeader(Response.CACHE_CONTROL, "no-cache, no-store, must-revalidate");  // HTTP 1.1
		response.setHeader(Response.PRAGMA, "no-cache");  // HTTP 1.0.
		response.setHeader(Response.EXPIRES, "0");  // Proxies.
	}
        
       public static Result authenticate(final String provider) {
		noCache(Controller.response());
                final String payload = Controller.request().getQueryString(PAYLOAD_KEY);
                Result obtained = handleAuthentication(provider, Controller.ctx(), payload);
                return obtained;
	}
       
       private static AuthUser signupUser(final AuthUser u, final Session session, final AuthProvider provider) throws AuthException {
        final Object id = PlayAuthenticate.getUserService().save(u);
		if (id == null) {
			throw new AuthException(
					Messages.get("playauthenticate.core.exception.signupuser_failed"));
		}
                provider.afterSave(u, id, session);
		return u;
	}
       
       public static Result handleAuthentication(final String provider,
			final Context context, final Object payload) {
		final AuthProvider ap = PlayAuthenticate.getProvider(provider);
		if (ap == null) {
			// Provider wasn't found and/or user was fooling with our stuff -
			// tell him off:
			return Controller.notFound(Messages.get(
					"playauthenticate.core.exception.provider_not_found",
					provider));
		}
		try {
			final Object o = ap.authenticate(context, payload);
			if (o instanceof String) {
				return Controller.redirect((String) o);
			} else if (o instanceof Result) {
				return (Result) o;
			} else if (o instanceof AuthUser) {

				final AuthUser newUser = (AuthUser) o;
				final Session session = context.session();

				// We might want to do merging here:
				// Adapted from:
				// http://stackoverflow.com/questions/6666267/architecture-for-merging-multiple-user-accounts-together
				// 1. The account is linked to a local account and no session
				// cookie is present --> Login
				// 2. The account is linked to a local account and a session
				// cookie is present --> Merge
				// 3. The account is not linked to a local account and no
				// session cookie is present --> Signup
				// 4. The account is not linked to a local account and a session
				// cookie is present --> Linking Additional account

				// get the user with which we are logged in - is null if we
				// are
				// not logged in (does NOT check expiration)

				AuthUser oldUser = PlayAuthenticate.getUser(session);

				// checks if the user is logged in (also checks the expiration!)
				boolean isLoggedIn = PlayAuthenticate.isLoggedIn(session);

				Object oldIdentity = null;

				// check if local user still exists - it might have been
				// deactivated/deleted,
				// so this is a signup, not a link
				if (isLoggedIn) {
					oldIdentity = PlayAuthenticate.getUserService().getLocalIdentity(oldUser);
					isLoggedIn = oldIdentity != null;
					if (!isLoggedIn) {
						// if isLoggedIn is false here, then the local user has
						// been deleted/deactivated
						// so kill the session
						PlayAuthenticate.logout(session);
						oldUser = null;
					}
				}

				final Object loginIdentity = PlayAuthenticate.getUserService().getLocalIdentity(
						newUser);
				final boolean isLinked = loginIdentity != null;

				final AuthUser loginUser;
				if (isLinked && !isLoggedIn) {
					// 1. -> Login
					loginUser = newUser;

				} else if (isLinked) {
					// 2. -> Merge

					// merge the two identities and return the AuthUser we want
					// to use for the log in
					if (PlayAuthenticate.isAccountMergeEnabled()
							&& !loginIdentity.equals(oldIdentity)) {
						// account merge is enabled
						// and
						// The currently logged in user and the one to log in
						// are not the same, so shall we merge?

						if (PlayAuthenticate.isAccountAutoMerge()) {
							// Account auto merging is enabled
							loginUser = PlayAuthenticate.getUserService()
									.merge(newUser, oldUser);
						} else {
							// Account auto merging is disabled - forward user
							// to merge request page
							final Call c = PlayAuthenticate.getResolver().askMerge();
							if (c == null) {
								throw new RuntimeException(
										Messages.get(
												"playauthenticate.core.exception.merge.controller_undefined",
												SETTING_KEY_ACCOUNT_AUTO_MERGE));
							}
							PlayAuthenticate.storeMergeUser(newUser, session);
							return Controller.redirect(c);
						}
					} else {
						// the currently logged in user and the new login belong
						// to the same local user,
						// or Account merge is disabled, so just change the log
						// in to the new user
						loginUser = newUser;
					}

				} else if (!isLoggedIn) {
					// 3. -> Signup
					loginUser = signupUser(newUser, session, ap);
				} else {
					// !isLinked && isLoggedIn:

					// 4. -> Link additional
					if (PlayAuthenticate.isAccountAutoLink()) {
						// Account auto linking is enabled

						loginUser = PlayAuthenticate.getUserService().link(oldUser, newUser);
					} else {
						// Account auto linking is disabled - forward user to
						// link suggestion page
						final Call c = PlayAuthenticate.getResolver().askLink();
						if (c == null) {
							throw new RuntimeException(
									Messages.get(
											"playauthenticate.core.exception.link.controller_undefined",
											SETTING_KEY_ACCOUNT_AUTO_LINK));
						}
						PlayAuthenticate.storeLinkUser(newUser, session);
						return Controller.redirect(c);
					}

				}

				return PlayAuthenticate.loginAndRedirect(context, loginUser);
			} else {
				return Controller.internalServerError(Messages
						.get("playauthenticate.core.exception.general"));
			}
		} catch (final AuthException e) {
			final Call c = PlayAuthenticate.getResolver().onException(e);
			if (c != null) {
				return Controller.redirect(c);
			} else {
				final String message = e.getMessage();
				if (message != null) {
					return Controller.internalServerError(message);
				} else {
					return Controller.internalServerError();
				}
			}
		}
	}
       
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
                        Map<java.lang.String,java.util.List<ValidationError>> found = filledForm.errors();
                        for (Map.Entry<String, List<ValidationError>> entry : found.entrySet()) {
                            String string = entry.getKey();
                            flash().put(string, "error");
                        }
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
			Map<String,String> data = filledForm.data();
                        String email="";
                        String password="";
                        for (Map.Entry<String, String> entry : data.entrySet()) {
                            if(entry.getKey().equals("email"))
                                email=entry.getValue();
                            else if(entry.getKey().equals("password"))
                                password=entry.getValue();
                        }
                        
                        User result = User.findByEmail(email);
                        if(result==null)
                            flash().put("failmail", "error");
                        else
                            flash().put("failmailorpass", "error");
                        LoggerUtils.debug("LOGIN","The login form was filled properly, handling login..");
			return UsernamePasswordAuthProvider.handleLogin(ctx());
		}
	}

        private enum Case {
		SIGNUP, LOGIN
	}

}

