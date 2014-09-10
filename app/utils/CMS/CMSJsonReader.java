package utils.CMS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import play.Logger;
import play.Play;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

import com.fasterxml.jackson.databind.JsonNode;

public class CMSJsonReader {
	private final Integer timeout = Play.application().configuration()
			.getInt("cmsTimeout");



	public JsonNode readJsonFromUrl(final String url, final String service,
			final HashMap<String, String> params, final String response)
					throws CMSException {

		Promise<WS.Response> res;
		// res = WS.url(url).setTimeout(1000000).get();
		final WSRequestHolder wsurl = WS.url(url + "/" + service).setHeader("Accept", "application/json").setTimeout(timeout);
		if (params!=null) {
			final Iterator<Entry<String, String>> it = params.entrySet()
					.iterator();
			while (it.hasNext()) {
				final Map.Entry<java.lang.String, java.lang.String> param = it
						.next();
				wsurl.setQueryParameter(param.getKey(), param.getValue());
			}
		}

		res = wsurl.get();
		if (res != null) {
			final Response result = res.get(1000000L);
			final JsonNode json = result.asJson();
			// with a system out i can see that the json is parsed correctly

			if (json.get("status").asText().equals("OK")) {
				return json.get(response);
			} else {
				throw new CMSException(
						"Internal Server Error while invoking CMS: "
								+ json.get("error"));
			}
		} else {
			throw new IllegalStateException("CMS response timeout.");
		}

	}

	public JsonNode readJsonFromUrl2(final String url, final String service,
			final HashMap<String, String> params, final String response)
					throws CMSException {
		Promise<WS.Response> res;
		// res = WS.url(url).setTimeout(1000000).get();

		final WSRequestHolder wsurl = WS.url(url + "/" + service)
				.setHeader("Accept", "application/json").setTimeout(timeout);
		if (params != null) {

			final Iterator<Entry<String, String>> it = params.entrySet()
					.iterator();
			while (it.hasNext()) {
				final Map.Entry<java.lang.String, java.lang.String> param = it
						.next();
				wsurl.setQueryParameter(param.getKey(), param.getValue());
			}
		}

		res = wsurl.get();
		if (res != null) {
			final Response result = res.get(1000000L);
			final JsonNode json = result.asJson();
			// with a system out i can see that the json is parsed correctly

			if (json.get("status").asText().equals("OK")) {

				final JsonNode lista = json.get(response);

				// final ObjectNode redsult = Json.newObject();
				// redsult.put("lista", lista);

				// return redsult;
				return lista;
			} else {
				throw new CMSException(
						"Internal Server Error while invoking CMS: "
								+ json.get("error"));
			}
		} else {
			throw new IllegalStateException("CMS response timeout.");
		}

	}

	public JsonNode readJsonFromUrl(final String url, final String service,
			final String id, final HashMap<String, String> params,
			final String response)
					throws CMSException {
		return readJsonFromUrl(url, service + "/" + id, params, response);
	}

}
