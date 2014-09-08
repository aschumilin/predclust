package algo;

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



	// singleton mongoDB client for all threads
	private static MongoClient 	mongoClient = null;
	private static DB 				db = 		null;
	private static DBCollection 	collSrl = 	null;
	
	private static String 	mongoAddr = System.getProperty("mongo.addr");
	private static int 	mongoPort = Integer.parseInt(System.getProperty("mongo.port"));
	private static String 	mongoUser = System.getProperty("mongo.user");
	private static String 	mongoPass = System.getProperty("mongo.pwd");
	private static String 	mongoDBName = System.getProperty("mongo.db.name");
	
//	private static String failedDocsFileName = System.getProperty("failed.path");
//	private static String resultDir			= System.getProperty("result.dir");
//	private static String annotMapFileName = System.getProperty("annotmap.path");
//	private static String annotatorLang = System.getProperty("annot.lang");
	
	
	

	
	
	// sync access to these 3 variables:
//	private static Map<String, String[][]> annotMap = new TreeMap<String, String[][]>();
//	private static BufferedWriter failedDocFileWriter = null;
	private static AnnotationService annotatorEN = null;
	private static AnnotationService annotatorES = null;

	
	
//	
//	
//	@Override
//	public void runAlgo(String docKey, Logger L) {
//		
//		L.info(" DEBUG " + docKey);
//		
//		long size = -1;
//		synchronized(annotMap){
//			size = annotMap.size();
//		}
//		
//		L.info(" DEBUG \t\tcurrent annotMap size: " + size);	
//		
//		
//		
//		
//		
//		
//		
//		try{
//			getDB(L);	
//		}catch(Exception e){
//			L.fatal("\t can't perform getDB.. exiting...", e);
//			writeFailedDocID(L, docKey+ "\t db error");
//			try {
//				if(failedDocFileWriter!=null)
//					failedDocFileWriter.close();
//			} catch (IOException ioe) {
//				L.error("could not close failedDocsFile at " + failedDocsFileName, ioe);
//			}
//			System.exit(1);
//		}
//		
//		
//
//// 1. read srl document from DB
//		BasicDBObject query = new BasicDBObject("_id", docKey);	
//		DBCursor dbResult = null;
//		
//		String srlXmlString = null;
//		String project = null;
//		String language = null;
//		String wikiID = null;
//		/* 	
//		 * "docId" : "924868",
//	     * "lang" : "en",
//		 * "project" : "wikinews",
//		 */
//		
//		try{
//			 dbResult = collSrl.find(query).limit(1);
//			
//			if(dbResult.size() == 0){
//				Worker.decrBusyWorkers();
//				L.error("doc not found in DB: " + docKey);
//				writeFailedDocID(L, docKey + "\t not found in db");
//				return;
//			}else{
//				BasicDBObject dbDoc = (BasicDBObject)dbResult.next();
//				srlXmlString = dbDoc.getString("srlAnnot");
//				project = dbDoc.getString("project");
//				language = dbDoc.getString("lang");
//				wikiID = dbDoc.getString("docId");
//			}
//			
//		}catch(Exception e){
//			L.error(docKey + "exception during target coll lookup", e);
//			Worker.decrBusyWorkers();
//			writeFailedDocID(L, docKey+ " error during db lookup");
//			return;
//		}
//		
//		 
//// 2. Parse XML and get all nodes
//		SAXBuilder builder = new SAXBuilder();
//		Document doc = null;
//		List<Element> nodes = null;
//
//		
//		try {
//			doc = (Document) builder.build(new StringReader(srlXmlString));} 
//		catch (Exception e){
//			L.error(docKey + "failed to parse XML: " + docKey, e);
//			Worker.decrBusyWorkers();
//			writeFailedDocID(L, docKey + "\t saxbuilder error");
//			return;
//		}
//		
//		nodes = doc.getRootElement().getChild("nodes").getChildren("node");
//		
//		
//		
//// iterate over nodes
//		for(Element node : nodes){
//			
//			Set<String> wordSet = new HashSet<String>();
//			List<Element> mentions = null;
//			Element annotParent = new Element("annots");
//			
//// 3.1. add node displayName and all mention words to one wordSet
//			String nodeDispName = node.getAttributeValue("displayName");
///***************************** escape rules *************/
//			boolean ruleWordLength = 	(nodeDispName.length() > 50);
//			if(ruleWordLength)
//				continue;
///***************************** escape rules *************/
//			wordSet.add(nodeDispName);
//			
//			mentions = node.getChild("mentions").getChildren("mention");		
//			for (Element mention : mentions){
//				String mentionWord = mention.getAttributeValue("words");
///***************************** escape rules *************/
//				ruleWordLength = 	(mentionWord.length() > 50);
//				boolean ruleWordNumTokens = (mentionWord.split("\\s+").length > 5);
//				if(ruleWordLength || ruleWordNumTokens)
//					continue;
//	/***************************** escape rules *************/				
//				wordSet.add(mentionWord);
//			}
//			
//			
//// 3.2. annotate all entries in wordSet. check first if annotation was already stored in annotMap			
//			boolean contained = false;
//			Element annotChild = null;
//			Element entity = null;
//			String annotXML = null;
//			String[][] annotsStoredOrNew = null;
//			
//			for(String word : wordSet){
//				
//				annotsStoredOrNew = null;
//				
//				synchronized(annotMap){
//					annotsStoredOrNew = annotMap.get(word);
//				}
//
//				if(annotsStoredOrNew == null){
//					// new word: call service and process annots
//					// add new entry in map
//
//					try {
//						annotXML = annotate(word, L, word, docKey);
//						
//						annotsStoredOrNew = processAnnotationXML(annotXML, L);
//					} catch (Exception e) {
//						L.error("annotation service exception for <" + word + ">", e);
//						continue; // to next word
//					} 		
//					
//					synchronized(annotMap){annotMap.put(word, annotsStoredOrNew);}
//				}
//				
//				// are there any annotations for this word?
//				if (annotsStoredOrNew.length != 0){
//					annotChild = new Element("annot").setAttribute("mentionWord", word);
//
//					for(String[] annot : annotsStoredOrNew){
//						// format is [][displayName, dbpedia URL, weight]
//						entity = new Element("entity");
//						entity.setAttribute("label", annot[0]);
//						entity.setAttribute("url", annot[1]);
//						entity.setAttribute("weight", annot[2]);
//						annotChild.addContent(entity);
//					}
//
//					// add annotChildren to annotParent 
//					annotParent.addContent(annotChild);
//
//				} else{ 
//					continue; // to next word
//				}
//			}
//			
//			// add annotParent to node
//			node.addContent(annotParent);	
//			
//		
//		}
//		
//		
//		
//// dump result to file		
//		String resultFileName = resultDir + language + "-" + project + "-" + wikiID + ".xml";
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter(resultFileName));
//			
//			String docToString = new XMLOutputter(Format.getPrettyFormat()).outputString(doc);
//			writer.write(docToString);
//			writer.close();
//		}catch(IOException ioe){
//			L.error("failed to write result file: " + resultFileName, ioe);
//			Worker.decrBusyWorkers();
//			writeFailedDocID(L, docKey + "\t result file writeout failed");
//			return;
//		}
//		
//		
//		
//// finished !		
//		Worker.decrBusyWorkers();
//	}
//	
//	
//	
//	/**
//	 * 
//<?xml version="1.0" encoding="UTF-8"?>
//<Response>
//  <WikifiedDocument><![CDATA[[[freeware]]]]></WikifiedDocument>
//  <DetectedTopics>
//    <DetectedTopic URL="http://dbpedia.org/resource/Freeware" displayName="Freeware" id="11592" lang="en" mention="freeware" weight="1"/>
//  </DetectedTopics>
//</Response>
//	 * @param annotation XML from the annotator
//	 * @return 2dim string array [][displayName, dbpedia URL, weight]. Empty String[][] if sax builder fails to read the annotation xml or there are no DetectedTopics found.
//	 */
//	private String[][] processAnnotationXML(String annotation, Logger L){
//		
//		
//		// parse XML from file or String
//		Document doc = null;
//		List<Element> annots = null;
//		
//		try {
//			doc= (Document) builder.build(new StringReader(annotation));
//		}catch(Exception e){
////			L.error("saxbuilder error for annotation xml:\n" + annotation.substring(0, Math.min(annotation.length(), 300)));
//			return new String[0][0];
//		}
//		
//		try {
//			 annots =  doc.getRootElement().getChild("DetectedTopics").getChildren("DetectedTopic");
//		} catch (Exception e) {
////			L.error("error when getting topics for annotation xml:\n" + annotation);
//			return new String[0][0];
//		}
//		
//		String[][] result = new String[annots.size()][3];
//		
//		for(int i=0; i<annots.size(); i++){
//			result[i][0] = annots.get(i).getAttributeValue("displayName");
//			result[i][1] = annots.get(i).getAttributeValue("URL");
//			result[i][2] = annots.get(i).getAttributeValue("weight");
//		}
//		
//		return result;
//	}
//	
//		
//	
//	private static synchronized String annotate(String word, Logger L, String request, String docID ) throws Exception{
//		System.out.println("-----------" +docID + "        " + request);
//		
//		if (annotator==null){
//			L.info("initialising annotator service");
//			
//			annotator = new AnnotationService("configs/hub-template.xml",
//					"configs/wikipedia-template-"+ annotatorLang +".xml", annotatorLang, "en", "dbpedia");
//
//			L.info("annotator service ready");
//			
//			System.out.println("-----------------");
//			
//			return annotator.process(word);
//
//			
//
//		}else{
//			
//			System.out.println("-----------------");
//			
//			return annotator.process(word);
//		}
//		
//		
//	}
//	
//	
//	
//	@Override
//	public void cleanUpFinally(Logger L){
//		// 1. close failed IDs file
//		// 2. print time to finish
//		// 3. write annotation map to file
//		if (failedDocFileWriter != null){
//			try {
//				
//				L.info("closing " + failedDocsFileName);
//				failedDocFileWriter.close();
//				
//			} catch (IOException e) {
//				L.error("failed to close " + failedDocsFileName + "\n" + e.getMessage());
//				e.printStackTrace();
//			}
//			
//		}
//		
//		
//		L.info((System.currentTimeMillis() - Coordinator.start)/1000.0 + " sec to finish");
//		L.info("serializing annotations map");
//		
//		
//		try{
//			// don't use buffering
//			OutputStream file = new FileOutputStream(annotMapFileName);
////			OutputStream buffer = new BufferedOutputStream(file);
//			ObjectOutput output = new ObjectOutputStream(file);//buffer);
//			try{
//				output.writeObject(annotMap);
//				L.info("finished serializing");
//			}
//			finally{output.close();}
//		}  
//		catch(IOException ioe){
//			L.error("could not serialize annotations map: ", ioe);
//		}
//		
//		
//		
//		L.info("DONE");
//	}
//	
//	
//	private static synchronized void writeFailedDocID(Logger L, String docID){
//		if (failedDocFileWriter == null){
//			try {
//				
//				failedDocFileWriter = new BufferedWriter(new FileWriter(failedDocsFileName));
//				
//			} catch (IOException e) {
//				L.info("FATAL failed to open failedDocsFile at: " + failedDocsFileName, e);
//			}
//			
//		}else{
//			try {
//				
//				failedDocFileWriter.write(docID + "\n");
//				
//			} catch (IOException e) {
//				L.info("FATAL failed to write to failedDocsFile at: " + failedDocsFileName, e);
//			}
//		}
//	}
//	
//	
//	private static synchronized void getDB(Logger L) throws UnknownHostException{
//		if (mongoClient == null || db == null || collSrl == null){
//			L.info("mongoClient null. Initializing...");
//			mongoClient = new MongoClient( mongoAddr , mongoPort );
//			//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
//			db = mongoClient.getDB(mongoDBName);
//			boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
//			if( ! auth ){
//				L.fatal("exiting because authentication failed for user " + mongoUser + " @ " + mongoAddr + ":" + mongoPort);
//				System.exit(1);
//			}else{
//
//				collSrl = db.getCollection(ORIGIN_COLLECTION_NAME);
//
//				// get all collection names
//				L.info("db connection and authentication ok");
//				L.info("listing collections for "+ mongoDBName + ":");
//				for (String s : db.getCollectionNames())
//					L.info("\t" + s);
//			}
//
//
//		}
//	}

}
