package utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

public class JsonReader {

	/*
	 * Method that returns a Json object from a request sent to the CMS
	 * 
	 * @param url Url of the API of the CMS to be called
	 */
	public JsonNode readJsonArrayFromUrl(final String url,
			final HashMap<String, String> params) {
		// May be improved with
		// http://stackoverflow.com/questions/15453905/promise-timeouts-and-ws-get-timeout-in-playframework-2-1-java
		Promise<WS.Response> res;
		try {
			// res = WS.url(url).setTimeout(1000000).get();
			final WSRequestHolder wsurl = WS.url(url).setTimeout(60000);

			final Iterator<Entry<String, String>> it = params.entrySet()
					.iterator();
			while (it.hasNext()) {
				final Map.Entry<java.lang.String, java.lang.String> param = it
						.next();
				wsurl.setQueryParameter(param.getKey(), param.getValue());
			}
			res = wsurl.get();
			if (res != null) {
				final Response result = res.get(1000000L);
				final JsonNode json = result.asJson();
				// with a system out i can see that the json is parsed correctly
				return json;
			} else {
				throw new IllegalStateException("CMS response timeout.");
			}
		} catch (final Exception mf) {
			throw new IllegalArgumentException(
					"The URL that has been provided is not valid");
		}
	}

	public JsonNode readJsonArrayFromUrl(final String url) {
		// May be improved with
		// http://stackoverflow.com/questions/15453905/promise-timeouts-and-ws-get-timeout-in-playframework-2-1-java
		Promise<WS.Response> res;
		try {
			// res = WS.url(url).setTimeout(1000000).get();
			final WSRequestHolder wsurl = WS.url(url).setTimeout(60000);

			res = wsurl.get();
			if (res != null) {
				final Response result = res.get(1000000L);
				final JsonNode json = result.asJson();
				// with a system out i can see that the json is parsed correctly
				return json;
			} else {
				throw new IllegalStateException("CMS response timeout.");
			}
		} catch (final Exception mf) {
			throw new IllegalArgumentException(
					"The URL that has been provided is not valid");
		}
	}
}
