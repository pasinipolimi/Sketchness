package utils.CMS.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Point {
	public Point() {

	}

	public Point(final Integer x, final Integer y) {
		super();
		this.x = x;
		this.y = y;
	}

	public Point(final Integer x, final Integer y, final boolean end) {
		super();
		this.x = x;
		this.y = y;
		this.end = end;
	}

	private Integer x;
	private Integer y;
	private Boolean end;


	public Boolean getEnd() {
		return end;
	}

	public void setEnd(final Boolean end) {
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

