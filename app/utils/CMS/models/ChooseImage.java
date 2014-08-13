package utils.CMS.models;

public class ChooseImage extends CMSObject {

	private Integer image;
	private Integer count;

	public Integer getImage() {
		return image;
	}
	public void setImage(final Integer image) {
		this.image = image;
	}

	public ChooseImage() {
		image = 0;
	}

	public ChooseImage(final Integer image, final Integer count) {
		this.image = image;
		this.count = count;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(final Integer count) {
		this.count = count;
	}
}
