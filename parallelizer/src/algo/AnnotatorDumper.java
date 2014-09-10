package algo;

import parallelizer.AnnotServicePool;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import parallelizer.Coordinator;
import parallelizer.Parallelizable;
import parallelizer.Worker;
import annotator.AnnotationService;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

public class AnnotatorDumper extends Parallelizable {
	
	private static final String ORIGIN_COLLECTION_NAME = 	"docs";

	private static AnnotServicePool annotatorPool = null;

	// singleton mongoDB client for all threads
	private static MongoClient 		mongoClient = null;
	private static DB 				db = 		null;
	private static DBCollection 	collDocs = 	null;
	
	private static String 	mongoAddr = System.getProperty("mongo.addr");
	private static int 		mongoPort = Integer.parseInt(System.getProperty("mongo.port"));
	private static String 	mongoUser = System.getProperty("mongo.user");
	private static String 	mongoPass = System.getProperty("mongo.pwd");
	private static String 	mongoDBName = System.getProperty("mongo.db.name");
	
//	private static String failedDocsFileName = System.getProperty("failed.path");
	private static String resultDir			= System.getProperty("result.dir");
	
	private static int 		numAnnotServices = Integer.parseInt(System.getProperty("annots.num"));
	private static String 	annotConfDir		= System.getProperty("annots.confdir");
	

	
	
	
	
	@Override
	public void runAlgo(String docKey, Logger L) {
//System.out.println("begin run " + docKey);
		try{
			getDB(L);	
		}catch(Exception e){
			L.fatal("\t can't perform getDB.. exiting...", e);
			System.exit(1);
		}
	
		System.out.println("annots:");
		getAnnotator(numAnnotServices, annotConfDir, L);
		
		
//System.out.println("annots done");
		
		

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

	
	
	private static synchronized void getAnnotator(int numAnnotators, String confDir, Logger L){
		if(annotatorPool == null){
			L.info("initializing annotators...");
			try {
				annotatorPool = new AnnotServicePool(numAnnotators, confDir, L);
			}catch(Exception e){
				L.fatal("exception while initializing annotation service pool...exiting..", e);
				System.exit(1);
			}
			L.info("done initializing annotators");

		}
	}
	
	private static synchronized void getDB(Logger L) throws UnknownHostException{
		if (mongoClient == null || db == null || collDocs == null){
			L.info("mongoClient null. Initializing...");
			mongoClient = new MongoClient( mongoAddr , mongoPort );
			//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
			db = mongoClient.getDB(mongoDBName);
			boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
			if( ! auth ){
				L.fatal("exiting because authentication failed for user " + mongoUser + " @ " + mongoAddr + ":" + mongoPort);
				System.exit(1);
			}else{

				collDocs = db.getCollection(ORIGIN_COLLECTION_NAME);

				// get all collection names
				L.info("db connection and authentication ok");
				L.info("listing collections for "+ mongoDBName + ":");
				for (String s : db.getCollectionNames())
					L.info("\t" + s);
			}


		}
	}

}
