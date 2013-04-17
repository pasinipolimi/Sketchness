package models;

import java.util.ArrayList;
import java.util.ListIterator;





public class Segment{
    
    private ArrayList<ArrayList<Point>> points;
    private String eraser;

    public Segment(String eraser) {
        points = new ArrayList<>();
        this.eraser=eraser;
    }
    

    public void ensureCapacity(int row,int column)
    {
	while(row >= getRowSize())
	{
	   points.add(new ArrayList<Point>());
	}
        while(column >= getColumnSize(row))
        {
            points.get(row).add(new Point());
        }
    }
    
    public void setPoint(int row, int column, Point point)
    {
        ensureCapacity(row,column);
        points.get(row).set(column, point);
    }
    
    public Point getPoint(int row, int column)
    {
        return points.get(row).get(column);
    }
    
    public int getRowSize()
    {
        return points.size();
    }
    
    public int getColumnSize(int row)
    {
        if(getRowSize()>0)
            return points.get(row).size();
        else
            return 0;
    }
        
    
    
    //Reconstruct the final segment, removing the traces that have been erased
    public ArrayList<Point> filter()
    {
        
        ArrayList<Point> toReturn = new ArrayList<>();
        ArrayList<PositionedPoint> finalSegment = new ArrayList<>();
        ArrayList<PositionedPoint> copy = new ArrayList<>();
        
        //Make a copy of the traces storing the orders in which the points have been saved
        for(int i = 0;i<points.size();i++)
        {
            for(int j = 0;j<points.get(i).size();j++)
            {
                finalSegment.add(new PositionedPoint(i,points.get(i).get(j)));
                copy.add(new PositionedPoint(i,points.get(i).get(j)));
            }
        }
        
        //Remove the points that have been erased, keeping the ones that have 
        //not
        ListIterator it = finalSegment.listIterator(finalSegment.size());
        while(it.hasPrevious())
        {
            PositionedPoint current = (PositionedPoint)it.previous();
            Point currentPoint = current.getPoint();
            if(currentPoint.getColor().equals(eraser))
            {
                ListIterator copyIt = copy.listIterator(copy.size());
                while(copyIt.hasPrevious())
                {
                    PositionedPoint check = (PositionedPoint)copyIt.previous();
                    Point checkPoint = check.getPoint();
                    if(checkPoint.inRange(currentPoint.getX(), currentPoint.getY(), currentPoint.getSize()) && !checkPoint.getColor().equals(eraser) && check.getPosition()<=current.getPosition())
                    {
                        copyIt.remove();
                    }
                }
            }
        }
        
        //Return just the ordered list of points to be traced, without the eraser
        //information
        ListIterator copyIt = copy.listIterator();
        while(copyIt.hasNext())
        {
            Point current=((PositionedPoint)copyIt.next()).getPoint();
            if(!current.getColor().equals(eraser))
                toReturn.add(current);
        }
        
        return toReturn;
    }
}


class PositionedPoint
{
    int position;
    Point point;

    public PositionedPoint(int position, Point point) {
        this.position = position;
        this.point = point;
    }

    public Point getPoint() {
        return point;
    }

    public int getPosition() {
        return position;
    }

}