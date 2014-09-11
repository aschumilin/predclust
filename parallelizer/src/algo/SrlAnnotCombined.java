package algo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import parallelizer.Parallelizable;
import parallelizer.Worker;
import annotator.AnnotationService;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

public class SrlAnnotCombined extends Parallelizable {
	
	private static final String TEXT_COLLECTION_NAME = 	"docs";
	private static final String SRL_COLLECTION_NAME = "srl";


	// singleton mongoDB client for all threads
	private static MongoClient 		mongoClient = null;
	private static DB 				db = 		null;
	private static DBCollection 	collDocs = 	null;
	private static DBCollection		collSrl = null;
	
	private static String 	mongoAddr = System.getProperty("mongo.addr");
	private static int 		mongoPort = Integer.parseInt(System.getProperty("mongo.port"));
	private static String 	mongoUser = System.getProperty("mongo.user");
	private static String 	mongoPass = System.getProperty("mongo.pwd");
	private static String 	mongoDBName = System.getProperty("mongo.db.name");
	
//	private static String failedDocsFileName = System.getProperty("failed.path");
	private static String resultDir			= System.getProperty("result.dir");	
	private static String annotDir		= System.getProperty("annots.dir");
	

	
	/**
	 * @param shortDocKey
	 * @param annotPath
	 * @param L
	 * @return jdom2.Document of detected topics OR null if error
	 */
	private Document getAnnotations(String shortDocKey, String annotPath, Logger L){
		// parse annot file, return as jdom2.Document
		String annotFileName = annotPath + shortDocKey;
		File annotFile = new File(annotFileName);
		SAXBuilder builder = new SAXBuilder();
		Document jdomDoc;
		
		try {
			jdomDoc = (Document) builder.build(annotFile);
			return jdomDoc;
		} catch (JDOMException e) {
			L.error("<" + shortDocKey + ">\t JDOMException when parsing annot file");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			L.error("<" + shortDocKey + ">\t IOException when parsing annot file");
			e.printStackTrace();
			return null;
		}
		
	}
	
	private Element getSrl(String longDocKey, Logger L){
		
		return null;
		// get all nodes --> get all mentions for each node
		// for each node mention, 
	}
	
	
	/**
	 * @param longDocKey
	 * @param L
	 * @return document text OR null if error
	 */
	private String getText(String longDocKey, Logger L){
		BasicDBObject query = new BasicDBObject("_id", longDocKey);	
		DBCursor dbResult = null;
				
		try{
			 dbResult = collDocs.find(query).limit(1);
			
			if(dbResult.size() == 0){
				Worker.decrBusyWorkers();
				L.error("<" + longDocKey + ">\t doc not found in DB");
				return null;
			}else{
				BasicDBObject dbDoc = (BasicDBObject)dbResult.next();
				return dbDoc.get("docText").toString();
			}
			
		}catch(Exception e){
			L.error("<" + longDocKey + ">\texception during target coll lookup");
			Worker.decrBusyWorkers();
			return null;
		}
	}
	
	private String transformKey(String shortKey){
		// es-2726363 -> http://es.wikipedia.org/wiki?curid=2726363
		String[] parts = shortKey.split("-");
		return "http://" + parts[0] + ".wikipedia.org/wiki?curid=" + parts[1];
		
	}
	/**
	 * if (from1 > to2 || to1 < from2) : overlap else: no overlap
	 */
	
	
	@Override
	public void runAlgo(String docKey, Logger L) {
		try{
			getDB(L);	
		}catch(Exception e){
			L.fatal("\t can't perform getDB.. exiting...", e);
			System.exit(1);
		}
	
		String longDocKey = transformKey(docKey);

		
	
		
		
		
		
		SAXBuilder builder = new SAXBuilder();
		final Document jdomDoc = (Document) builder.build(new StringReader(srlXMLString));			
		
		
		
		
		
		
		

// 1. read srl document from DB
		BasicDBObject query = new BasicDBObject("_id", docKey);	
		DBCursor dbResult = null;
		String wikiID = null;
		String language = null;
		String docText = null;
		String annotResult = null;
		
		try{
			 dbResult = collDocs.find(query).limit(1);
			
			if(dbResult.size() == 0){
				Worker.decrBusyWorkers();
				L.error("doc not found in DB: " + docKey);
				return;
			}else{
				BasicDBObject dbDoc = (BasicDBObject)dbResult.next();
				language = dbDoc.getString("lang");
				wikiID = dbDoc.getString("docId");
				docText = dbDoc.getString("docText");
			}
			
		}catch(Exception e){
			L.error("<" + docKey + ">\texception during target coll lookup", e);
			Worker.decrBusyWorkers();
			return;
		}
		
		
// call annotation service
//		public String process(String text, String lang, String docKey) throws Exception{
		Object[] result = null;
		while ((result = annotatorPool.getFreeService(language, docKey)) == null){
			Thread.yield();
		}
		
		String serviceId =(String) result[1];
L.debug("using " + serviceId + " on " + docKey);
		AnnotationService serv = (AnnotationService) result[0];
		try {
			annotResult = serv.process(docText);
			annotatorPool.setServiceFree(serviceId);
		} catch (Exception e) {
			// RESET BUSY FLAG AFTER PROCESSING
			annotatorPool.setServiceFree(serviceId);
			L.error("<" + docKey + ">\t exception during annotatorPool.process()", e);
			Worker.decrBusyWorkers();
			return;
		}
		
		
	
		
		 

		
		
// dump result to file		
		String resultFileName = resultDir + language + "-" + wikiID + ".xml";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(resultFileName));			
			writer.write(annotResult);
			writer.close();
		}catch(IOException ioe){
			L.error("<" + docKey + ">\t result file writeout failed");
			Worker.decrBusyWorkers();
			return;
		}
		
		
		
// finished !		
		Worker.decrBusyWorkers();
	}
	
	
	

	
		
		
	
	@Override
	public void cleanUpFinally(Logger L){
		L.info("DONE cleaning up");
	}

	
	

	
	private static synchronized void getDB(Logger L) throws UnknownHostException{
		if (mongoClient == null || db == null || collDocs == null || collSrl == null){
			L.info("mongoClient null. Initializing...");
			mongoClient = new MongoClient( mongoAddr , mongoPort );
			//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
			db = mongoClient.getDB(mongoDBName);
			boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
			if( ! auth ){
				L.fatal("exiting because authentication failed for user " + mongoUser + " @ " + mongoAddr + ":" + mongoPort);
				System.exit(1);
			}else{

				collDocs = db.getCollection(TEXT_COLLECTION_NAME);
				collSrl = db.getCollection(SRL_COLLECTION_NAME);
				
				// get all collection names
				L.info("db connection and authentication ok");
				L.info("listing collections for "+ mongoDBName + ":");
				for (String s : db.getCollectionNames())
					L.info("\t" + s);
			}


		}
	}

}
