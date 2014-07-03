package utils.CMS.models;

import java.util.List;


public class Segmentation extends CMSObject {

	public Segmentation(final List<Point> points,
			final List<History> historyPoints) {
		super();

		this.points = points;
		this.history = historyPoints;
	}

	private Double quality;
	private List<Point> points;
	private List<History> history;



	public Double getQuality() {
		return quality;
	}

	public void setQuality(final Double quality) {
		this.quality = quality;
	}

	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(final List<Point> points) {
		this.points = points;
	}

	public List<History> getHistory() {
		return history;
	}

	public void setHistory(final List<History> history) {
		this.history = history;
	}

}


