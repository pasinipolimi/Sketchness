package utils.CMS.models;

public class User extends CMSObject {

	
	private Integer app_id;
	private String app_user_id;
	private Double quality;
	private Statistics statistics;
	
	public User() {

	}

	public String getApp_user_id() {
		return app_user_id;
	}

	public void setApp_user_id(final String app_user_id) {
		this.app_user_id = app_user_id;
	}

	public Integer getApp_id() {
		return app_id;
	}

	public void setApp_id(final Integer app_id) {
		this.app_id = app_id;
	}

	public Double getQuality() {
		return quality;
	}

	public User(final String app_user_id) {
		super();
		this.app_user_id = app_user_id;
		this.app_id = 9;
	}

	public void setQuality(final Double quality) {
		this.quality = quality;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	public void setStatistics(final Statistics statistics) {
		this.statistics = statistics;
	}

	public class Statistics {
		
		public Statistics() {

		}
		
		private Integer sessions;
		private Integer actions;

		public Statistics(final Integer sessions, final Integer actions) {
			super();
			this.sessions = sessions;
			this.actions = actions;
		}
		public Integer getActions() {
			return actions;
		}
		public void setActions(final Integer actions) {
			this.actions = actions;
		}
		public Integer getSessions() {
			return sessions;
		}
		public void setSessions(final Integer sessions) {
			this.sessions = sessions;
		}
	}

}
