package utils.CMS.models;

public class Alias {

	public Alias(final String language, final String name) {
		super();
		this.language = language;
		this.name = name;
	}

	public Alias() {
		super();
	}
	private String language;
	private String name;

	public String getLanguage() {
		return language;
	}
	public void setLanguage(final String language) {
		this.language = language;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}

}
