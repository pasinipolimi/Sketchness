package utils.CMS;

public class CMSException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8810971924974421101L;

	private String message;

	public CMSException(final String message) {
		super();
		this.setMessage(message);
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}


}
