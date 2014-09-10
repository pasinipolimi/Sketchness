package utils.CMS.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action extends CMSObject {




	private String completed_at;
	private Integer image;
	private Integer session;
	private Segmentation segmentation;
	private List<Point> points;
	private List<History> history;
	private String created_at;

	// "tagging", "segmentation"
	private String type;
	private Integer tag;
	private Integer user;

	// tre valido, default true
	private Boolean validity;



	public Integer getImage() {
		return image;
	}

	public void setImage(final Integer image) {
		this.image = image;
	}





	public static Action createSkipActionTagging(final Integer session,
			final Integer image, final Integer user, final Boolean validity) {
		final Action a = new Action();
		a.image = image;
		a.session = session;
		a.user = user;
		a.validity = validity;
		a.type = "tagging";
		return a;
	}

	public static Action createSkipActionSeg(final Integer session,
			final Integer image, final Integer user, final Boolean validity,
			final Integer tag) {
		final Action a = new Action();
		a.image = image;
		a.session = session;
		a.tag = tag;
		a.user = user;
		a.validity = validity;
		a.type = "segmentation";
		return a;
	}

	public Integer getTag() {
		return tag;
	}

	public void setTag(final Integer tag) {
		this.tag = tag;
	}


	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getCompleted_at() {
		return completed_at;
	}

	public void setCompleted_at(final String completed_at) {
		this.completed_at = completed_at;
	}

	public Integer getUser() {
		return user;
	}

	public void setUser(final Integer user) {
		this.user = user;
	}

	public Boolean getValidity() {
		return validity;
	}

	public void setValidity(final Boolean validity) {
		this.validity = validity;
	}

	public Integer getSession() {
		return session;
	}

	public void setSession(final Integer session) {
		this.session = session;
	}

	 public Segmentation getSegmentation() {
	 return segmentation;
	 }
	
	 public void setSegmentation(final Segmentation segmentation) {
	 this.segmentation = segmentation;
	 }



	public Action() {
	}

	public static Action createSegmentationAction(final Integer image,
			final Integer session, final Integer tag, final Integer user,
			final Boolean validity, final List<Point> points,
			final List<History> history) {

		// public static Action createSegmentationAction(final Integer image,
		// final Integer session, final Integer tag, final Integer user,
		// final Boolean validity, final Segmentation segmentation) {
		final Action a = new Action();
		a.image = image;
		a.session = session;
		a.tag = tag;
		a.user = user;
		a.validity = validity;
		a.type = "segmentation";
		a.points = points;
		a.history = history;
		// a.segmentation = segmentation;
		return a;
	}

	public static Action createSegmentationAction(final Integer image,
			final Integer session, final Integer tag, final Integer user,
			final Boolean validity) {
		final Action a = new Action();
		a.image = image;
		a.session = session;
		a.tag = tag;
		a.user = user;
		a.validity = validity;
		a.type = "segmentation";
		return a;
	}

	public static Action createTagAction(final Integer image,
			final Integer session, final Integer tag, final Integer user,
			final Boolean validity) {
		final Action a = new Action();
		a.image = image;
		a.session = session;
		a.tag = tag;
		a.user = user;
		a.validity = validity;
		a.type = "tagging";
		return a;
	}

	public static Action createTagAction(final Integer image,
			final Integer session, final Integer user, final Boolean validity) {
		final Action a = new Action();
		a.image = image;
		a.session = session;
		a.user = user;
		a.validity = validity;
		a.type = "tagging";
		return a;
	}

	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(final List<Point> points) {
		this.points = points;
	}

	public List<History> getHistory() {
		return history;
	}

	public void setHistory(final List<History> history) {
		this.history = history;
	}

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(final String created_at) {
		this.created_at = created_at;
	}

}


