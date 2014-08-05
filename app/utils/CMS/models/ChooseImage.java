package utils.CMS.models;

public class ChooseImage extends CMSObject {

	private Integer image;

	public Integer getImage() {
		return image;
	}
	public void setImage(final Integer image) {
		this.image = image;
	}

        public ChooseImage() {
            image = 0;
        } 
        
        public ChooseImage(Integer image) {
            this.image = image;
        } 
}
