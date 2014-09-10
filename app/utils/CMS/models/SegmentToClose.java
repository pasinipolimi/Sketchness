package utils.CMS.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SegmentToClose {

	private Integer tag;
	private List<Point> points;
	private List<History> history;

	public Integer getTag() {
		return tag;
	}
	public void setTag(final Integer tag) {
		this.tag = tag;
	}
	public List<Point> getPoints() {
		return points;
	}
	public void setPoints(final List<Point> points) {
		this.points = points;
	}


	public SegmentToClose(final Integer tag, final List<Point> points,
			final List<History> historyPoints) {
		super();
		this.tag = tag;
		this.points = points;
		this.setHistory(historyPoints);
	}

	public SegmentToClose() {
		super();

	}

	public List<History> getHistory() {
		return history;
	}

	public void setHistory(final List<History> history) {
		this.history = history;
	}

}
