package utils.CMS.models;

import java.util.List;

public class Image extends CMSObject {


	private String mediaLocator;
	private Integer width;
	private Integer height;
	private List<Pose> pose;

	public String getMediaLocator() {
		return mediaLocator;
	}
	public void setMediaLocator(final String mediaLocator) {
		this.mediaLocator = mediaLocator;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(final Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(final Integer height) {
		this.height = height;
	}

	public List<Pose> getPose() {
		return pose;
	}

	public void setPose(final List<Pose> pose) {
		this.pose = pose;
	}




}
