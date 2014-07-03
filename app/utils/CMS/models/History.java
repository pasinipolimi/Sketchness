package utils.CMS.models;

import java.util.List;

public class History {

	private List<Point> points;
	private String time;

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
