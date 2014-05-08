package utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import play.Logger;

/**
 * 
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class LoggerUtils {

	/*
	 * Save the stack trace of an error to the log.
	 * 
	 * @param caller String representing the name of the module that has called
	 * the error
	 * 
	 * @param ex Exception raised by the module
	 */
	public static void error(final String caller, final Exception ex) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		Logger.error("[" + caller + "] ERROR: " + sw.toString());
	}

	public static void error(final String caller, final String message) {
		Logger.error("[" + caller + "] ERROR: " + message);
	}
        
        public static void info(final String caller, final String message) {
		Logger.info("[" + caller + "] INFO: " + message);
	}
}
