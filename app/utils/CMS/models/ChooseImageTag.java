package utils.CMS.models;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChooseImageTag extends CMSObject {

	private Integer image;
	private Integer tag;
	private Integer count;

	public Integer getTag() {
		return tag;
	}
	public void setTag(final Integer tag) {
		this.tag = tag;
	}
	public Integer getImage() {
		return image;
	}
	public void setImage(final Integer image) {
		this.image = image;
	}

	public ChooseImageTag(final Integer image, final Integer tag) {
		this.image = image;
		this.tag = tag;
	}

	public ChooseImageTag(final Integer image, final Integer tag,
			final Integer count) {
		this.image = image;
		this.tag = tag;
		this.count = count;
	}

	public ChooseImageTag(final Integer tag) {
		this.tag = tag;
	}

	public ChooseImageTag() {
		this.image = 0;
		this.tag = 0;
	}
	public Integer getCount() {
		return count;
	}
	public void setCount(final Integer count) {
		this.count = count;
	}
}
