package utils.CMS.models;

import java.util.List;

public class History {

	private List<Point> points;
	private String time;
	private int size;
	private String color;

	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public String getTime() {
		return time;
	}
	public void setTime(final String time) {
		this.time = time;
	}
	public List<Point> getPoints() {
		return points;
	}
	public void setPoints(final List<Point> points) {
		this.points = points;
	}

}
