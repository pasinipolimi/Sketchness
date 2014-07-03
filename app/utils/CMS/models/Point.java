package utils.CMS.models;

public class Point {
	public Point(final Integer x, final Integer y, final String color,
			final Boolean removed, final Integer size) {
		super();
		this.x = x;
		this.y = y;
		this.color = color;
		this.removed = removed;
		this.size = size;
	}

	private Integer x;
	private Integer y;
	private Integer size;
	private String color;
	private Boolean removed;

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

	public String getColor() {
		return color;
	}

	public void setColor(final String color) {
		this.color = color;
	}

	public Boolean getRemoved() {
		return removed;
	}

	public void setRemoved(final Boolean removed) {
		this.removed = removed;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(final Integer size) {
		this.size = size;
	}

}
