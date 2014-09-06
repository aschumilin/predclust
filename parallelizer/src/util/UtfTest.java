package util;

import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import parallelizer.Parallelizable;
import parallelizer.Worker;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graph.Argument;
import graph.Node;
import graph.Predicate;
import graph.Ref;
import graph.Role;


public class UtfTest extends Parallelizable {


	// singleton mongoDB client for all threads
	private static MongoClient 	mongoClient = null;
	private static DB 			db = null;
	private static DBCollection collSrl = null;
	private static DBCollection collGraph = null;
	private static DBCollection collDocs = null;
	private static String 		mongoAddr = "romulus";
	private static int 			mongoPort = 22222;
	private static String 	mongoUser = "reader";
	private static String 	mongoPass = "123";
	private static String 	mongoDBName = "all";

	private static final String ORIGIN_COLLECTION_NAME = 	"srl";
	private static final String TARGET_COLLECTION_NAME = 	"graph";
	private static final String DOCS_COLLECTION_NAME = "docs";


	public static void main(String[] args) {
		try{
			getDB();	
		}catch(Exception e){
			e.printStackTrace();
			return;
		}

		if( collSrl!=null && collGraph!=null){	


			// 1. query the DB for given docId (it is the primary key in the [srlxml] collection)

			/*
			http://en.wikipedia.org/wiki?curid=11375241
			http://en.wikipedia.org/wiki?curid=15798642
			http://en.wikipedia.org/wiki?curid=23179089
			http://en.wikipedia.org/wiki?curid=22454053 (Gedankenstrich)
			http://en.wikipedia.org/wiki?curid=21694580
			 */
			String badDocKey = "http://en.wikipedia.org/wiki?curid=21694580";
			String xmlPrefix ="<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n";
			String srlBad = "";
			String docText = "";
			
			BasicDBObject query = new BasicDBObject("_id", badDocKey);
			DBCursor results = collSrl.find(query);
			BasicDBObject doc = null;
			try {
				doc = (BasicDBObject)results.next();
			}catch(Exception e){
				System.err.println(badDocKey + "\t exception while getting result set");
				return;
			}finally {
				results.close();
			}

			if( doc!=null ){
				srlBad = doc.get("srlAnnot").toString();
			}



			// XML 1.0
			// #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
			String xml10pattern = "[^"
			                    + "\u0009\r\n"
			                    + "\u0020-\uD7FF"
			                    + "\uE000-\uFFFD"
			                    + "\ud800\udc00-\udbff\udfff"
			                    + "]";

			


			try{
				System.out.println("bad srl:");
				SAXBuilder builder = new SAXBuilder();
				final Document jdomDoc = (Document) builder.build(new StringReader(srlBad));			
				XPathFactory xFactory = XPathFactory.instance(); 
				System.out.println("funktioniert");
			}catch(Exception e){
				System.err.println("bad srl:" + e.getMessage());
				e.printStackTrace();
			}

			try{
				System.out.println("bad srl + prefix:");
				SAXBuilder builder = new SAXBuilder();
				final Document jdomDoc = (Document) builder.build(new StringReader(srlBad.replaceAll(xml10pattern, " ")));			
				XPathFactory xFactory = XPathFactory.instance();  
				System.out.println("funktioniert");
			}catch(Exception e){
				System.err.println("bad srl + prefix:" + e.getMessage());
				e.printStackTrace();
			}
			char ch = srlBad.charAt(10800-1);

		}

	}



	private static synchronized void getDB() throws UnknownHostException{
		if (mongoClient == null || db == null || collSrl == null || collDocs == null){
			mongoClient = new MongoClient( mongoAddr , mongoPort );
			//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
			db = mongoClient.getDB(mongoDBName);
			boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
			if( ! auth ){
				System.err.println("exiting because authentication failed for user " + mongoUser + " @ " + mongoAddr + ":" + mongoPort);
				System.exit(1);
			}else{

				collSrl = db.getCollection(ORIGIN_COLLECTION_NAME);
				collGraph = db.getCollection(TARGET_COLLECTION_NAME);
				collDocs = db.getCollection(DOCS_COLLECTION_NAME);

				System.out.println("bd ok");
			}


		}
	}




}
