package utils.CMS.models;

import java.util.List;

public class Collection extends CMSObject {

	private String name;
	private List<Integer> images;


	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<Integer> getImages() {
		return images;
	}

	public void setImages(final List<Integer> images) {
		this.images = images;
	}


}
