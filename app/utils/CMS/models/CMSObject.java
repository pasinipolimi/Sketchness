package utils.CMS.models;

public class CMSObject {

	public CMSObject(final Integer id) {
		super();
		this.id = id;
	}

	public CMSObject() {
		super();

	}

	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}
}
