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
			coll = db.getCollection("user") ;			// get the annotation collection 
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
    

    /**
     * userSave: save data user in mongoDb
     * @param data - Format: Map<String, String[]>
     * @return true: successful saving - false: failed saving
     */
	public Map<String, String> userSave(Map<String, String[]> data){
		
		boolean ctrl = true;
		Map<String, String> result = new HashMap<String, String>();
		
		// Verify that the keys of the submitted fields are correct
		// Correct -> ctrl=true
		// Incorrect -> ctrl=false
		if(data.get("framework")[0] != null){
			for(String key :data.keySet()){
				if(!key.equals("mail") && !key.equals("password") && !key.equals("framework") && !key.equals("framework_id") && !key.equals("token_1")&& !key.equals("token_2") && !key.equals("user_id")){
					ctrl = false;
					result.put(key, "error");	// insert into result the incorrect values
				}
			}
		}else{
			ctrl = false;
			result.put("framework", "error");	// insert into result the incorrect values (framework)
		}
		
		if(ctrl){
			boolean checkResult = checkUserRegistration(data);	// check if the user is already registered
			if(checkResult == false){
				coll.insert(new BasicDBObject(data));	// user insert into mongodb
				result.put("queryResult", "ok");
			}else{
				result.put("User already registered", "error");
				result.put("queryResult", "ko");
			}
			
		}else{
			result.put("queryResult", "ko");
		}
		return result;
    }
	
	/**
	 * Funzione che verifica se l'utente è già iscritto al sito tramite uno dei tre framework (facebook, twitter o sketchness)
	 * @param data -> User data in Map<String, String[]> format
	 * @return true if the user is already registered, false otherwise
	 */
	public boolean checkUserRegistration(Map<String, String[]> data){
	    BasicDBObject query = new BasicDBObject();
	    query.put("framework", data.get("framework")[0].toString());
		
	    if(data.get("framework")[0].equals("sketchness")){
			query.put("mail", data.get("mail")[0].toString());
		}else if(data.get("framework")[0].equals("facebook")){
			query.put("framework_id", data.get("framework_id")[0].toString());
		}else if(data.get("framework")[0].equals("twitter")){
			query.put("framework_id", data.get("framework_id")[0].toString());
		}
		
		DBCursor cursor = coll.find(query);
		
		if(cursor.count() > 0){
			return true; // User already registered
		}else{
			return false; // User not registered yet
		}
	}
	
	public boolean sketchenessLogin(Map<String, String[]> data){
		BasicDBObject query = new BasicDBObject();
		query.put("mail", data.get("mail")[0].toString());
		query.put("password", data.get("password")[0].toString());
		DBCursor cursor = coll.find(query);
		if(cursor.count() == 1){
			return true;
		}else{
			return false;
		}
	}

}
