package utils.CMS;

/**
 * Created with IntelliJ IDEA.
 * User: Riboni1989
 * Date: 12/11/13
 * Time: 12:49
 * To change this template use File | Settings | File Templates.
 */
public abstract class SortObjectAction implements Comparable<SortObjectAction> {

    private String id;
    private double quality;

    public void setId(String id){
        this.id = id;
    }
    public String getId(){
        return id;
    }
   
    public double getQuality() {
		return quality;
	}
	public void setQuality(double quality) {
		this.quality = quality;
	}


    @Override
    public int compareTo(SortObjectAction o) {
        String i = Double.toString(getQuality());
        String j = Double.toString(o.getQuality());
        return i.compareTo(j);
    }


    public int compare(){
        return 1;
    }

}
