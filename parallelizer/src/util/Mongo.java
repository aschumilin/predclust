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
		
	
			Mongo m = new Mongo("romulus", "srl");
		
			DBObject o = m.getById("http://en.wikipedia.org/wiki?curid=997414");
			// http://en.wikipedia.org/wiki/Imperial_Roman_army 
			FileWriter fw = new FileWriter(new File("/home/pilatus/Desktop/annots-test2/en-997414-srl.xml"));
			fw.write((String)o.get("srlAnnot"));
			fw.close();


	}

}
