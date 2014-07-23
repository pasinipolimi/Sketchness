package utils.CMS.models;

public class Pose extends CMSObject{
	
	private String location;
	private Integer x0;
	private Integer y0;
	private Integer x1;
	private Integer y1;
	
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public Integer getX0() {
		return x0;
	}
	public void setX0(Integer x0) {
		this.x0 = x0;
	}
	public Integer getY0() {
		return y0;
	}
	public void setY0(Integer y0) {
		this.y0 = y0;
	}
	public Integer getX1() {
		return x1;
	}
	public void setX1(Integer x1) {
		this.x1 = x1;
	}
	public Integer getY1() {
		return y1;
	}
	public void setY1(Integer y1) {
		this.y1 = y1;
	}
}
