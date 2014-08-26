package utils.CMS.models;

import java.util.List;


public class Mask extends CMSObject {

	private Double quality;
	private Integer segmentations;
	private String updated_at;
	private Integer image;
	private Integer tag;
	private String mediaLocator;


	public Double getQuality() {
		return quality;
	}

	public void setQuality(final Double quality) {
		this.quality = quality;
	}

	public Integer getSegmentations() {
		return segmentations;
	}

	public void setSegmentations(final Integer segmentations) {
		this.segmentations = segmentations;
	}

	public String getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(final String updated_at) {
		this.updated_at = updated_at;
	}

	public Integer getImage() {
		return image;
	}

	public void setImage(final Integer image) {
		this.image = image;
	}

	public Integer getTag() {
		return tag;
	}

	public void setTag(final Integer tag) {
		this.tag = tag;
	}

	public String getMediaLocator() {
		return mediaLocator;
	}

	public void setMediaLocator(final String mediaLocator) {
		this.mediaLocator = mediaLocator;
	}

}
