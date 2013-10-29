package controllers;

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import play.data.Form;
import play.libs.Akka;
import play.mvc.Controller;
import play.mvc.Result;
import providers.MyUsernamePasswordAuthProvider;
import providers.MyUsernamePasswordAuthProvider.MyLogin;
import scala.concurrent.duration.Duration;
import views.html.sketchness_login;

import java.util.concurrent.TimeUnit;

public class Login extends Controller {

    public static boolean actorActive = false;

    public static Result login() {

        if(!actorActive){
            Akka.system().scheduler().schedule(
                    Duration.create(0, TimeUnit.MILLISECONDS),
                    Duration.create(2, TimeUnit.MINUTES),
                    new Runnable() {
                        @Override
                        public void run() {
                            //Logger.debug("ciao");
                            actorActive = true;
                            models.IsOnline.checkOnline();

                        }
                    }, Akka.system().dispatcher()
            );
        }



        return ok(sketchness_login.render(MyUsernamePasswordAuthProvider.LOGIN_FORM));

    }


    public static Result doLogin() {
        com.feth.play.module.pa.controllers.Authenticate.noCache(response());
        final Form<MyLogin> filledForm = MyUsernamePasswordAuthProvider.LOGIN_FORM
                .bindFromRequest();
        if (filledForm.hasErrors()) {
            // User did not fill everything properly
            return badRequest(sketchness_login.render(filledForm));
        }
        else {
            // Everything was filled
            return UsernamePasswordAuthProvider.handleLogin(ctx());
        }
    }


}
