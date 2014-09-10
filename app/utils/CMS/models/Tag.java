package utils.CMS.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tag extends CMSObject {

	public Tag(final Integer id, final String name, final List<Alias> aliases) {
		super(id);
		this.name = name;
		this.aliases = aliases;
	}

	public Tag() {
		super();
	}

	private String name;
	private List<Alias> aliases;

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<Alias> getAliases() {
		return aliases;
	}

	public void setAliases(final List<Alias> aliases) {
		this.aliases = aliases;
	}

	public Tag(final String name) {
		super();
		this.name = name;
	}

	public Tag(final Integer id) {
		super(id);
	}
}
