package utils.CMS.models;

import java.util.List;

public class Task extends CMSObject {


	private Integer image;
	private Integer tag;
	private List<Integer> users;

	private String completed_at;
	private String created_at;

	public Task(final Integer image) {
		super();
		this.image = image;
	}



	public Integer getImage() {
		return image;
	}

	public void setImage(final Integer image) {
		this.image = image;
	}


	public String getCompleted_at() {
		return completed_at;
	}

	public void setCompleted_at(final String completed_at) {
		this.completed_at = completed_at;
	}

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(final String created_at) {
		this.created_at = created_at;
	}

	public List<Integer> getUsers() {
		return users;
	}

	public void setUsers(final List<Integer> users) {
		this.users = users;
	}

	public String getStatus() {
		// TODO Auto-generated method stub
		return null;
	}



	public Integer getTag() {
		return tag;
	}



	public void setTag(Integer tag) {
		this.tag = tag;
	}

}
