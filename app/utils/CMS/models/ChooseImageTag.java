package utils.CMS.models;

public class ChooseImageTag extends CMSObject {

	private Integer image;
	private Integer tag;

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

        public ChooseImageTag(Integer image, Integer tag) {
            this.image = image;
            this.tag = tag;
        }
        
        public ChooseImageTag() {
            this.image = 0;
            this.tag = 0;
        }
}
