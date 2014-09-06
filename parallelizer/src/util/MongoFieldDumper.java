package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;

public class MongoFieldDumper {

static String resultFilePath ="/dev/shm/en-articles.txt";

static BasicDBObject query = new BasicDBObject("project", "wikipedia");
static BasicDBObject keys =  new BasicDBObject("docId",1).append("docTitle", 1);


	public static void main(String[] args) throws IOException {
		DBCollection coll= null;
		System.out.println("begin");
		try {
			coll = Funcs.getMongoColl("localhost", 22222, "all", "srl", "atsc", "123");
		} catch (MongoException e) {e.printStackTrace();} catch (UnknownHostException e) {e.printStackTrace();}
		System.out.println("got db");
		
		
		
		DBCursor cursor = coll.find(query, keys);
		
		FileWriter fw = new FileWriter(new File(resultFilePath));
		System.out.println("got file, enter loop on cursor size: " + cursor.size());
		
		while(cursor.hasNext()){
			BasicDBObject o = (BasicDBObject)cursor.next();
			System.out.println(o.get("docTitle"));
//			System.out.println( o.get("_id")+" " +o.get("docTitle") );
			fw.write(o.get("docTitle") +  "\t" + o.get("docId") + "\n");
		}
		System.out.println("done");
		

		fw.close();
		
		
		
		
	}

}
