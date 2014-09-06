package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;


public class BadCharRemover {
	private static String 	mongoAddr = System.getProperty("mongo.addr");
	private static int 	mongoPort = Integer.parseInt(System.getProperty("mongo.port"));
	private static String 	mongoUser = System.getProperty("mongo.user");
	private static String 	mongoPass = System.getProperty("mongo.pwd");
	private static String 	mongoDBName = System.getProperty("mongo.db.name");

	private static String COLL_NAME = "srl";


	public static void main(String[] args) {

		// singleton mongoDB client for all threads
		MongoClient 	mongoClient = null;
		DB 				db = null;
		DBCollection 	collTo = null;



		try {
			mongoClient = new MongoClient( mongoAddr , mongoPort );
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		db = mongoClient.getDB(mongoDBName);
		db.authenticate(mongoUser, mongoPass.toCharArray());


		//		collFrom = db.getCollection("docs");
		collTo = db.getCollection(COLL_NAME);

  

		String xml10pattern = "[^"
				+ "\u0009\r\n"
				+ "\u0020-\uD7FF"
				+ "\uE000-\uFFFD"
				+ "\ud800\udc00-\udbff\udfff"
				+ "]";
		String badXmlFile = "/dev/shm/artem/docIDsInvalidXML";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(badXmlFile)));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DBCursor c = null;
		String line;
		int i = 1;
		int broken=0;
		String id = "";
		try {
			while ((line = br.readLine()) != null) {
				try{
					id = line;

					// find the doc by ID
					BasicDBObject searchQuery = new BasicDBObject().append("_id", id);			
					c  = collTo.find(searchQuery);			
					BasicDBObject doc = (BasicDBObject)c.next();

					// get the annotation xml and replace bad characters with blanks
					String goodSrl = doc.get("srlAnnot").toString().replaceAll(xml10pattern, " ");

					// update the doc with good srl xml
					BasicDBObject newDocument = new BasicDBObject();
					newDocument.append("$set", new BasicDBObject().append("srlAnnot", goodSrl));
					collTo.update(searchQuery, newDocument);

					System.out.println(i);
					i++;
				}catch(Exception e){
					broken++;
					System.err.println(id + " " + e.getMessage());
					//e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		c.close();
		
		System.out.println("broken:" + broken);

//		-Dfile.encoding=UTF-8
//				-Dmongo.addr=romulus -Dmongo.port=22222 
//				-Dmongo.user=atsc -Dmongo.pwd=123 
//				-Dmongo.db.name=all





	}


}
