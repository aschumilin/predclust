package algo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import parallelizer.Coordinator;
import parallelizer.Parallelizable;
import parallelizer.Worker;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

/**
 * @author aschumilin
 * 
 * read and clean wikinews and wikipedia text docs as xml
 * and dump them to mongoDB
 * 
 * schema example:
 * {
	"_id" : "http://en.wikipedia.org/wiki?curid=609582",
	"docId" : "609582",
	"docTitle" : "Sinergy",
	"lang" : "en",
	"project" : "wikipedia",
	"docText" : "Sinergy was a Finnish..."
	}
 *
 */

public class MongoDumper extends Parallelizable {


	private static final String COLLECTION_NAME = 		"docs";
	private static final int 	MIN_SENTECES_IN_DOC = 	5;
	private static final int 	MIN_CHARS_IN_DOC = 	5;



	// singleton mongoDB client for all threads
	private static MongoClient mongoClient = null;
	private static DB 		db = null;
	private static DBCollection coll = null;
	private static String 	mongoAddr = System.getProperty("mongo.addr");
	private static int 	mongoPort = Integer.parseInt(System.getProperty("mongo.port"));
	private static String 	mongoUser = System.getProperty("mongo.user");
	private static String 	mongoPass = System.getProperty("mongo.pwd");
	private static String 	mongoDBName = System.getProperty("mongo.db.name");

	private static long docCount = 0;
	
	private static synchronized void incrementVars(long docsParsed){
		MongoDumper.docCount += docsParsed;
	}
	
	
	
	/**
	 * Perform different checks on the document text to decide 
	 * whether this doc should enter the database.
	 * 
	 * 1. criterium: number of sentences 
	 * 2. criterium: text length
	 * 
	 * 
	 * @return true if document text passes the check, else false 
	 */
	private boolean approveDoc(String text){
		
		boolean crit_1 = ((text.split("\\.")).length >= 5); 
		boolean crit_2 = (text.length() >= 200); 
		return crit_1 & crit_2;
	}
	
	
	
	
	@Override
	public void runAlgo(String dataFilePath, Logger L) {
		final String id = dataFilePath.split("/")[dataFilePath.split("/").length - 1];
		long docsFound = 0;
		BufferedReader br = null;
		
		try{
			getDB(L);	
		}catch(Exception e){
			L.fatal(id + "\t can't perform getDB.. exiting...", e);
			return;
		}
		
		if (coll != null){	
			
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(dataFilePath), Charset.forName("UTF-8")));
			} catch (FileNotFoundException fnfe) {
				L.error(id +"\t file not found...aborting worker: " + dataFilePath, fnfe);
				// exit this worker when file not found
				return;
				
			}
			try {
				if(br!=null){
					String line = "";
					
					while ((line = br.readLine())!= null) {
						
						if(line.startsWith("<doc id=\"")){			// beginning of a new document
							
							
							// 1. parse the id, url and title
							BasicDBObject doc = null; 
							Map<String, String> docFields = extractFields(line, L, id);
							if (docFields != null){
								doc = new BasicDBObject("_id", docFields.get("url"))	// dictionary: [id, url, title, lang, proj]
										.append("docId", docFields.get("id"))
										.append("docTitle", docFields.get("title"))
										.append("lang", docFields.get("lang"))
										.append("project", docFields.get("proj"));
							}else{
								
								continue;
							}
							
							// 1. skip the next line because it's the title
							br.readLine();
							
							// 2. read until the next "</doc>"
							String text = "";
							while((line = br.readLine()) != null){
								
								
								if(! line.equals("</doc>")){
										text = text.concat(line.trim() + " ");
								}else{
									
									// 3. finalize new DB object with text
									if (doc != null){				
										doc.append("docText", text.trim());
										try{
											
											
											
											// 4. perform sanity check on the doc before writing it to DB
											if( approveDoc(text) ){
												coll.insert(doc);
												
												docsFound++;
												
											}else{
												//L.debug(id + "\t doc not approved: " + docFields.get("url"));
											}
											
											
											
											
										}catch(Exception e){
											L.error(id + "\t collection insertion error:", e);
										}
									}
									break;
								}	
							}	 
						}	
					}	
				}
			} catch (IOException e) {
				L.error(id +"\t io error...aborting worker: " + dataFilePath, e);
				return;
			}finally{
				try {
					br.close();
				} catch (IOException e) {
					L.error(id + "\t io error closing the BufferedReader", e);
				}
			}
		}else{
			L.fatal(id + "\t exiting... com.mongodb.DB od DBCollection is null");
			return;
		}
		
		Worker.decrBusyWorkers();
		MongoDumper.incrementVars(docsFound);
		
		
		if(Worker.getBusyWorkers() == 0){
			// this one is the last worker
			L.info((System.currentTimeMillis() - Coordinator.start)/1000.0 + " s to finish");
			L.info("RESULT: \t " + MongoDumper.docCount + " documents written to DB");
		}
	}


	
	/**
	 * @param headline e.g.<doc id="2" url="http://de.wikinews.org/wiki?curid=2" title="Hauptseite">
	 * @param L Logger
	 * @param id worker id
	 * @return map with article's [id, url, title, lang, project]
	 */
	private Map<String, String> extractFields(String headline, Logger L, String id){
		// <doc id="2" url="http://de.wikinews.org/wiki?curid=2" title="Hauptseite">
		TreeMap<String, String> result = new TreeMap<String, String>();
		
		String[] parts = headline.split(" ");
		
		// title can contain spaces !!
		
		if(parts.length >= 4 && headline.endsWith(">")){					// ensure line ends with ">"
			try{
				result.put("id", parts[1].substring(4, parts[1].length()-1));			// docID
				result.put("url", parts[2].substring(5, parts[2].length()-1));			// url
				String title = headline.split(" title=\"")[1];
				result.put("title", title.substring(0, title.length()-2));				// title
				
				String url = result.get("url");											// lang and project
				if(url.contains("http://de.wikinews.org")){
					result.put("lang", "de");
					result.put("proj", "wikinews");
				}else if(url.contains("http://en.wikinews.org")){
					result.put("lang", "en");
					result.put("proj", "wikinews");
				}else if(url.contains("http://de.wikipedia.org")){
					result.put("lang", "de");
					result.put("proj", "wikipedia");
				}else if(url.contains("http://en.wikipedia.org")){
					result.put("lang", "en");
					result.put("proj", "wikipedia");
				}else{
					result.put("lang", "UNKNOWN");
					result.put("proj", "UNKNOWN");
				}
				return result;
			}catch(Exception e){
				L.error(id + "\t exception in parsing headline...skipping " + headline, e);
				return null;
			}
			
					
		}else{
			L.error(id + "\t bad headline...skipping " + headline);
			return null;
		}	
	}

	
	
	
	private static synchronized void getDB(Logger L) throws UnknownHostException{
		if (mongoClient == null || db == null || coll == null){
			L.info("mongoClient null. Initializing...");
			mongoClient = new MongoClient( mongoAddr , mongoPort );
//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
			db = mongoClient.getDB(mongoDBName);
			boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
			if( ! auth ){
				L.fatal("exiting because authentication failed for user " + mongoUser + " @ " + mongoAddr + ":" + mongoPort);
				System.exit(1);
			}else{
				
				coll = db.getCollection(COLLECTION_NAME);
				
				// get all collection names
				L.info("db connection and authentication ok");
				L.info("listing collections for "+ mongoDBName + ":");
				for (String s : db.getCollectionNames())
					L.info("\t" + s);
			}

			
		}
	}
	
	
	

}
