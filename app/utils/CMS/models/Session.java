package utils.CMS.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Session extends CMSObject {

	private String completed_at;
	private String started_at;




	public String getStarted_at() {
		return started_at;
	}

	public void setStarted_at(final String started_at) {
		this.started_at = started_at;
	}



	public String getCompleted_at() {
		return completed_at;
	}

	public void setCompleted_at(final String completed_at) {
		this.completed_at = completed_at;
	}

}
