<<<<<<< HEAD

package models;



public class Player {
    private int points;
    private int warningsReceived;

    public Player() {
        points=0;
        warningsReceived=0;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public int getWarningsReceived() {
        return warningsReceived;
    }

    public int getPoints() {
        return points;
    }

    public void setWarningsReceived(int warningsReceived) {
        this.warningsReceived = warningsReceived;
    }
    

}
=======

package models;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;


public class Player {
	// start initialize the variables needed for mongodb
	
	private DBCursor curs;
    private Mongo s;
    private DB db;
    private DBCollection coll;
    private BasicDBObject query;

	// stop initialize the variables needed for mongodb    
    private int points;
    private int warningsReceived;

    public Player() {
        points=0;
        warningsReceived=0;
        /* START integration part*/    	
        try {           
			s = new Mongo();							// initialize a new Mongo
			db = s.getDB( "sketchness" );				// get the sketchness db
			coll = db.getCollection("user") ;		// get the annotation collection 
        }
        catch (UnknownHostException e)
        {
    		System.out.println(e.getMessage());   	
        }     

        /* END integration part*/     
    }
    
    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public int getWarningsReceived() {
        return warningsReceived;
    }

    public int getPoints() {
        return points;
    }

    public void setWarningsReceived(int warningsReceived) {
        this.warningsReceived = warningsReceived;
    }
    
<<<<<<< HEAD
    
=======
    /**
     * userSave: save data user in mongoDb
     * @param data - Format: Map<String, String[]>
     * @return true: successful saving - false: failed saving
     */
	public Map<String, String> userSave(Map<String, String[]> data){
		boolean ctrl = true;
		Map<String, String> result = new HashMap<String, String>();
		for(String key :data.keySet()){
			if(!key.equals("mail") && !key.equals("password")){
				ctrl = false;
				result.put(key, "error");
			}
		}
		if(ctrl){
			coll.insert(new BasicDBObject(data));
			result.put("queryResult", "ok");
		}else{
			result.put("queryResult", "ko");
		}
		return result;
    }
>>>>>>> 0917228ce62aa8cf88199a32573e28fb5e9ae821
}
>>>>>>> 7eb3311855b9540afb2e17d86b47c1d5ad6cb4b2
