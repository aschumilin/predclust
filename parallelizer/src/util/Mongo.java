package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;

public class Mongo {

 
static final int port = 22222;
static final String dbName = "all";
static final String user = "atsc";
static final String pwd = "123";

DBCollection coll= null;

	public Mongo(String host, String collection){
		try {
			coll = Funcs.getMongoColl(host, 22222, dbName, collection, user, pwd);
		} catch (MongoException e) {e.printStackTrace();} catch (UnknownHostException e) {e.printStackTrace();}
		
	}
	public DBObject getById(String id){
		BasicDBObject query = new BasicDBObject("_id", id);
		
		DBCursor cursor = coll.find(query).limit(1); 
		if (cursor.size() > 0){
			return cursor.next();
		}else{
			return null;
		}
	}
	
	public List<DBObject> execQuery(String queryString){
		
		DBObject query  = (DBObject) JSON.parse(queryString);
		DBCursor cursor = coll.find(query);
		return cursor.toArray();
	}
	
	
	public static void main(String[] args) throws IOException {
		
	
		
//		DBCursor cursor = coll.find(query, keys)
//	    System.out.println("got file, enter loop on cursor size: " + cursor.size());
//		
//		while(cursor.hasNext()){
//			BasicDBObject o = (BasicDBObject)cursor.next();
//			System.out.println(o.get("docTitle"));
		
		


	}

}
