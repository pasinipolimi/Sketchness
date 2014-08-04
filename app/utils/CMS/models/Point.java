package utils.CMS.models;

public class Point {
	public Point() {

	}
	
	public Point(final Integer x, final Integer y) {
		super();
		this.x = x;
		this.y = y;
	}

	private Integer x;
	private Integer y;
	private Boolean end;


	public Boolean getEnd() {
		return end;
	}

	public void setEnd(Boolean end) {
		this.end = end;
	}

	public Integer getX() {
		return x;
	}

	public void setX(final Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(final Integer y) {
		this.y = y;
	}

	
}

