package utils.CMS.models;

public class Point {
	public Point(final Integer x, final Integer y, final Boolean removed) {
		super();
		this.x = x;
		this.y = y;
		this.end = removed;
	}

	private Integer x;
	private Integer y;
	private Boolean end;

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



	public Boolean getRemoved() {
		return end;
	}

	public void setRemoved(final Boolean removed) {
		this.end = removed;
	}


}
