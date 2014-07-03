package utils.CMS.models;

public class MicroTask extends CMSObject {

	// "tagging", "segmentation"
	private String type;
	private Integer task;
	private Integer action;
	private String created_at;
	private String completed_at;
	private Integer order;

	public MicroTask(final String type, final Integer task, final Integer order) {
		super();
		this.type = type;
		this.task = task;
		this.order = order;
	}
	public Integer getAction() {
		return action;
	}

	public void setAction(final Integer action) {
		this.action = action;
	}

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(final String created_at) {
		this.created_at = created_at;
	}

	public String getCompleted_at() {
		return completed_at;
	}

	public void setCompleted_at(final String completed_at) {
		this.completed_at = completed_at;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(final Integer order) {
		this.order = order;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Integer getTask() {
		return task;
	}

	public void setTask(final Integer task) {
		this.task = task;
	}

	public String getStatus() {
		if (completed_at != null) {
			return "1";
		}
		return "0";
	}
}
