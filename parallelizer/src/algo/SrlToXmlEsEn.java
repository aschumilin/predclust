package algo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

import parallelizer.Coordinator;
import parallelizer.Parallelizable;
import parallelizer.Worker;

import util.Funcs;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

/**
 * @author aschumilin
 * 
 * 
 * FINALE VERSION DES TEXT->SRLXML TEILSTUECKS
 * 
 * 
 * push the texts through SRL and store the results in mongoDB collection 
 * 
 *  * schema example:
 * {
	"_id" : "http://en.wikipedia.org/wiki?curid=609582",
	"docId" : "609582",
	"docTitle" : "Sinergy",
	"lang" : "en",
	"project" : "wikipedia",
	"srlAnnot" : "<item><services>SRL output" 
	
	IFF SRL pipeline returned an error message:
	
	"badSrl" : "1" 
	}
 *
 */
public class SrlToXmlEsEn extends Parallelizable {
/*
 * invalid utf-8 in text:
http://en.wikipedia.org/wiki?curid=125903
 */

	private static final String ORIGIN_COLLECTION_NAME = 	"docs";
	private static final String TARGET_COLLECTION_NAME = 	"srl";

//	private static String srlEndpointIP =				System.getProperty("srl.endpoint.ip");
	private static final String EN_SRL_ENDPOINT = 	"http://localhost:9090/axis2/services/analysis_en/analyze?conll=true";
	private static final String ES_SRL_ENDPOINT = 	"http://localhost:9090/axis2/services/analysis_es/analyze?conll=true";

	private static final String CONTENT_TYPE = 		"text/xml";
	private static final String CHARSET = 			"UTF-8";
	private static final String DATA_FORMAT = 		"<analyze><text>{0}</text><target>relations</target></analyze>";





	// Sample call to the SRL service:

	// curl -H 'Content-type:text/xml' -d '<analyze><text>The library is fully occupied.</text><target>relations</target></analyze>' 'http://141.52.218.56:9090/axis2/services/analysis_en/analyze?conll=true'


	/*
	 * watch out for broken results of SRL pipeline
	 * srlAnnot : "<analysisServiceErrorResponse>analysis_en service failed: Invalid UTF-8</analysisServiceErrorResponse>"
	 */

	// singleton mongoDB client for all threads
	private static MongoClient 	mongoClient = null;
	private static DB 			db = null;
	private static DBCollection originColl = null;
	private static DBCollection targetColl = null;
	private static String 		mongoAddr = System.getProperty("mongo.addr");
	private static int 	mongoPort = Integer.parseInt(System.getProperty("mongo.port"));
	private static String 	mongoUser = System.getProperty("mongo.user");
	private static String 	mongoPass = System.getProperty("mongo.pwd");
	private static String 	mongoDBName = System.getProperty("mongo.db.name");










	@Override
	public void runAlgo(String docKey, Logger L) {

		String SRL_ENDPOINT = "";
		///////////////////////////////////////////////
		// waehle zwischen EN und ES 
		if(docKey.startsWith("http://en.")){
			
			SRL_ENDPOINT = EN_SRL_ENDPOINT;
			
		}else if (docKey.startsWith("http://es.")){
			
			SRL_ENDPOINT = ES_SRL_ENDPOINT;
			
		}else{
			Worker.decrBusyWorkers();
			return;
		}
		/////////////////////////////////////////////////
		
		
		
		try{
			getDB(L);	
		}catch(Exception e){
			L.fatal("\t can't perform getDB.. exiting...", e);
			Worker.decrBusyWorkers();
			return;
		}

		if( originColl!=null && targetColl!=null){	

			BasicDBObject query = new BasicDBObject("_id", docKey);
			
			// 0. check if doc has not already been processed
			/*
			try{
				DBCursor results = targetColl.find(query).limit(1);
				
				if(results.size() != 0){
					// close worker: this doc has already been processed
					// L.debug(docKey + "\t done already");
					Worker.decrBusyWorkers();
					return;
				}
			}catch(Exception e){
				L.error("<" + docKey + ">\t exception during target coll lookup");
			}
			*/
			
			// 1. query the DB for given docId (it is the primary key in the [docs] collection)
			query = new BasicDBObject("_id", docKey);
			DBCursor results = originColl.find(query).limit(1);
			BasicDBObject doc = null;
			try {
				doc = (BasicDBObject)results.next();
			}catch(Exception e){
				//L.error("<" +docKey + ">\t exception while getting result set", e);
				Worker.decrBusyWorkers();
				return;
			}finally {
				results.close();
			}

			

			
			
			
			if( doc!=null ){
				// 2. get docText
				String articleText = doc.get("docText").toString();
				
				// 2.1. remove potentially messy characters
				////////////////////////////////////////////////////////////
				articleText = articleText.replace("<",  "").replace(">", "");
				//////////////////////////////////////////////////////////////
				
				String textData = MessageFormat.format(DATA_FORMAT, articleText);
				
				
				
				// 3. call SRL service
				HttpURLConnection connection = null; 
				try {
					//Create connection
					URL url = new URL(SRL_ENDPOINT);
					connection = (HttpURLConnection)url.openConnection();
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Content-Type", CONTENT_TYPE);
					connection.setRequestProperty("Content-Length", Integer.toString(textData.getBytes().length));
					connection.setUseCaches(false);
					connection.setDoInput(true);
					connection.setDoOutput(true);
//					connection.setc


					//Send request
//					DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
//					wr.writeBytes(textData);
//					wr.flush ();
//					wr.close ();
					DataOutputStream wr = new DataOutputStream (connection.getOutputStream());					
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, CHARSET));
					writer.write(textData);
					writer.flush();
					writer.close();
					wr.close();					
				// http://en.wikipedia.org/wiki?curid=163883
			    // http://en.wikipedia.org/wiki?curid=125903
			    // http://es.wikipedia.org/wiki?curid=4945504
					
					//Get Response	
					InputStream is = connection.getInputStream();
					BufferedReader rd = new BufferedReader(new InputStreamReader(is));
					String line;
					StringBuffer response = new StringBuffer(); 
					while((line = rd.readLine()) != null) {
						response.append(line + "\n");
					}
					rd.close();

					
					
					// 4 convert output to XML
					// not yet

					// 5.a store XML to collection [srl-xml]
					// (5.b process XML and store frames -- not yet)
					try{
						doc.removeField("docText");
						BasicDBObject srlXmlDoc = null;
						
						// check for error message in SRL output
						if( ! response.toString().startsWith("<item>")){
							srlXmlDoc = doc.append("badSrl", "1").append("srlAnnot", response.toString());
							L.error("<" +docKey + ">\t SRL-Fehler: \t "+ response.toString());
						}else{
							srlXmlDoc = doc.append("badSrl", "0").append("srlAnnot", Funcs.removeInvalidXMLChars(response.toString()));		
						}
						
						targetColl.insert(srlXmlDoc);
						
					}catch(Exception e){
						L.error("<" +docKey + ">\t exception while writing to result collection ", e);
						Worker.decrBusyWorkers();
						return;
					}

				} catch (Exception e) {
					L.error("<" +docKey + ">\t exception during http request handling");
					// !!! wait two minutes for service restart 
					try {
						Thread.sleep(100000);
					} catch (InterruptedException e1) {
						L.debug("thread sleep interrupted: " + e.getCause().getClass().getName() + " : " + e.getMessage());
						e1.printStackTrace();
					}
					Worker.decrBusyWorkers();
					return;

				} finally {
					if(connection != null)
						connection.disconnect(); 
				}

			}else{
				L.error("<" +docKey + ">\t result is null");
				Worker.decrBusyWorkers();
				return;
			}

			


		}else{
			L.fatal("\t exiting... com.mongodb.DB od DBCollection is null");
			Worker.decrBusyWorkers();
			return;
		}

		
		// here everything is normal
		Worker.decrBusyWorkers();


//		if(Worker.getBusyWorkers() == 0){
//			// this one is the last worker
//			L.info((System.currentTimeMillis() - Coordinator.start)/1000.0 + " s to finish");
//			//			L.info("RESULT: \t " + SRLAccess.docCount + " documents written to DB");
//		}
	}

	
	





	private static synchronized void getDB(Logger L) throws UnknownHostException{
		if (mongoClient == null || db == null || originColl == null){
			L.info("mongoClient null. Initializing...");
			mongoClient = new MongoClient( mongoAddr , mongoPort );
			//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
			db = mongoClient.getDB(mongoDBName);
			boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
			if( ! auth ){
				L.fatal("exiting because authentication failed for user " + mongoUser + " @ " + mongoAddr + ":" + mongoPort);
				System.exit(1);
			}else{

				originColl = db.getCollection(ORIGIN_COLLECTION_NAME);
				targetColl = db.getCollection(TARGET_COLLECTION_NAME);

				// get all collection names
				L.info("db connection and authentication ok");
				L.info("listing collections for "+ mongoDBName + ":");
				for (String s : db.getCollectionNames())
					L.info("\t" + s);
			}


		}
	}




}
