package utils.CMS.models;

import java.util.List;

public class History {

	private Integer size;
	private String color;

	private List<Point> points;
	private Integer time;

	public Integer getTime() {
		return time;
	}

	public void setTime(final Integer time) {
		this.time = time;
	}
	public List<Point> getPoints() {
		return points;
	}
	public void setPoints(final List<Point> points) {
		this.points = points;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

}
