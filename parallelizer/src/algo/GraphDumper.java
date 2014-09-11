package algo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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
import test.GRAPHTESTER;

import annotator.EntityIndex;

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

public class GraphDumper extends Parallelizable {
	
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
	private Document getAnnotationsJdom(String shortDocKey, String annotPath, Logger L){
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
	
	/**
	 * @param longDocKey
	 * @param L
	 * @return root Element (<item>)of srl xml OR null if error
	 */
	private Element getSrl(String longDocKey, Logger L){
		BasicDBObject query = new BasicDBObject("_id", longDocKey);	
		DBCursor dbResult = null;
				
		try{
			 dbResult = collSrl.find(query).limit(1);
			
			if(dbResult.size() == 0){
				Worker.decrBusyWorkers();
				L.error("<" + longDocKey + ">\t srl doc not found in DB");
				return null;
			}else{
				BasicDBObject dbDoc = (BasicDBObject)dbResult.next();
				SAXBuilder builder = new SAXBuilder();
				String srlString = dbDoc.get("srlAnnot").toString();
				Document jdomDoc = (Document) builder.build(new StringReader(srlString));
				return jdomDoc.getRootElement();
			}
			
		}catch(Exception e){
			L.error("<" + longDocKey + ">\t exception during srl coll lookup");
			Worker.decrBusyWorkers();
			return null;
		}
	}
	
	
	/**
	 * @param longDocKey
	 * @param L
	 * @return document text OR null if error
	 */
	/*
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
	
	*/
	
	private String transformKey(String shortKey){
		// es-2726363 -> http://es.wikipedia.org/wiki?curid=2726363
		String[] parts = shortKey.split("-");
		return "http://" + parts[0] + ".wikipedia.org/wiki?curid=" + parts[1];		
	}

	

	
	
	@Override
	public void runAlgo(String docKey, Logger L) {
		try{
			getDB(L);	
		}catch(Exception e){
			L.fatal("\t can't perform getDB.. exiting...", e);
			System.exit(1);
		}
	
		// docKeys are file names of annotation dumps
		String longDocKey = transformKey(docKey);
		
		// 1. get index of detected topics (entities)
		EntityIndex entIndex = new EntityIndex(getAnnotationsJdom(docKey, annotDir, L));
		
		// 2. get srl xml
		Element srlRoot = getSrl(longDocKey, L);
		
		// 3. iterate over srl nodes, get all node mentions
		
		// 4. get indexed entities for each mention, select the one with highes weight
		
		// 5. make graph, add entity annotations 
	
		

		
		
		
// finished !		
		Worker.decrBusyWorkers();
	}
	
	
	public static ArrayList<DirectedSparseMultigraph<Argument,Role>> composeAnnotatedGraph(String srlXMLString, boolean visualize) throws JDOMException, IOException{

		ArrayList<DirectedSparseMultigraph<Argument, Role>> oneGraphPerSentence = new ArrayList<DirectedSparseMultigraph<Argument, Role>>();
//		try {

		// 1. build jdom2.Document from srl xml string 
		SAXBuilder builder = new SAXBuilder();
		final Document jdomDoc = (Document) builder.build(new StringReader(srlXMLString));			
		XPathFactory xFactory = XPathFactory.instance();  



//			try{
				// 1. select all sentences
				XPathExpression<Element> expr = xFactory.compile("//sentence", Filters.element());
				List<Element> sentences = expr.evaluate(jdomDoc);

				// 2. for each sentence			 
				for (Element sent : sentences) {

					//////// result variables
					String sentenceText = sent.getChildText("text");
					DirectedSparseMultigraph<Argument, Role> sentenceSRLGraph = new DirectedSparseMultigraph<Argument, Role>();
					////////


					String sentId = sent.getAttributeValue("id");

					// 3. get all frames for that sentence
					expr = xFactory.compile("//frame[@sentenceId='"+sentId + "']", Filters.element());
					List<Element> frames = expr.evaluate(jdomDoc);

					int rootIndicator = 0;		// to keep track of first frame in each sentence
/******************/for(Element frame: frames){
						rootIndicator ++;


						////////							
						// each frame represents a predicate



						// 1. extract frame attributes (isRoot, pos, lemma, displName) through tokenID
						String frameTokenID = frame.getAttributeValue("tokenId");
						expr = xFactory.compile("//token[@id='"+frameTokenID + "']", Filters.element());
						Element frameToken = expr.evaluateFirst(jdomDoc);		// this element must exist
						String POS 		= frameToken.getAttributeValue("pos");
//						String lemma 	= frameToken.getAttributeValue("lemma");
						String frameDisplName = frame.getAttributeValue("displayName");
						String frameID = frame.getAttributeValue("id");
						// only first frame for each sentence can be root frame !!!
						boolean isRoot = (rootIndicator == 1);		
/* PREDICATE */			Predicate predicate = new Predicate(isRoot, frameID, POS, lemma, frameDisplName);

						// add KB references to predicate
						Element descriptions = frame.getChild("descriptions");
						if (descriptions != null){
							List<Element> predRefs = descriptions.getChildren("description");
							for(Element predRef : predRefs){
								String uri = predRef.getAttributeValue("URI");
								String refDisplName = predRef.getAttributeValue("displayName");
								String refKB = predRef.getAttributeValue("knowledgeBase");
								Ref predReference = new Ref(uri, refDisplName, refKB);

								predicate.addRef(predReference);
							}
						}


						Argument argPlaceholder = null;

						// 2. extract frame arguments
						List<Element> arguments = frame.getChildren("argument");
/* ARGUMENTS */			for(Element argument : arguments){



							// 4.1. is this argument a node? query the frame attribute of each argument
							if (argument.getAttributeValue("frame") == null){


								// 4.2. get some node attributes
								String nodeDisplName = argument.getAttributeValue("displayName");
								String nodeId = argument.getAttributeValue("id");
								expr = xFactory.compile("//node[@id='" + nodeId + "']", Filters.element());
								String nodeType = expr.evaluateFirst(jdomDoc).getAttributeValue("type");


								// 4.2.2 track the occurrence of this node in the treemap
//								if(nodeTracker.containsKey(nodeId)){
//									nodeTracker.put(nodeId, nodeTracker.get(nodeId)+1);
//								}else{
//									nodeTracker.put(nodeId, 1);
//								}
//System.out.println("\n" + srlXml + "\n");									
//System.out.println("frame" +  frameID + " / node: " + nodeDisplName + "(" + nodeId +")" + nodeId + "." + nodeTracker.get(nodeId));								
								// 4.3. using sentenceID and nodeID, get the corresponding mention token id 

//								XPathExpression e = xFactory.compile("count(//node[@id='" + nodeId +"']//mention)"); 
//								double maxMentions = (Double)e.evaluate(jdomDoc).get(0);
////System.out.println("---" + maxMentions);
//								String exactNodeID = nodeId + "." + new Double(Math.min(maxMentions, nodeTracker.get(nodeId))).intValue();
//
//								expr = xFactory.compile("//mention[@id='" + exactNodeID + "']//mention_token", Filters.element());											
//								String nodeMentionTokenID = expr.evaluateFirst(jdomDoc).getAttributeValue("id");
//
//								// 4.4. find pos and lemma using the nodeMentionTokenID
//								expr = xFactory.compile("//token[@id='" + nodeMentionTokenID + "']", Filters.element());
//								Element nodeEl = expr.evaluateFirst(jdomDoc);
//								String nodePos = nodeEl.getAttributeValue("pos");
//								String nodeLemma = nodeEl.getAttributeValue("lemma");

								// 4.5 init new Node instance
///* NODE */									argPlaceholder = new Node(nodeId + "." + nodeTracker.get(nodeId), nodeType, nodePos, nodeLemma, nodeDisplName);
								argPlaceholder = new Node(nodeId, nodeType, "?pos", "?lemma", nodeDisplName);
								
								// 4.6. read the node KB references
								expr = xFactory.compile("//node[@id='" + nodeId + "']", Filters.element());
								Element nodeDescriptions = expr.evaluateFirst(jdomDoc).getChild("descriptions");
								if (nodeDescriptions != null){
									List<Element> nodeRefs = nodeDescriptions.getChildren("description");
									for(Element nodeRef : nodeRefs){
										Ref r = new Ref(nodeRef.getAttributeValue("URI"), nodeRef.getAttributeValue("displayName"), nodeRef.getAttributeValue("knowledgeBase"));
										argPlaceholder.addRef(r);
									}

								}




							}else{
								// 4.2. is this argument another frame?
								// 1. extract frame attributes (isRoot, pos, lemma, displName) through tokenID
								String argId = argument.getAttributeValue("id");
								String argFrameDisplName = argument.getAttributeValue("displayName");
								expr = xFactory.compile("//frame[@id='" + argId + "']", Filters.element());
								// from argument to frame
								Element argFrame = expr.evaluateFirst(jdomDoc);
								String argFrameTokenID = argFrame.getAttributeValue("tokenId");
								expr = xFactory.compile("//token[@id='"+argFrameTokenID + "']", Filters.element());
								Element argFrameToken = expr.evaluateFirst(jdomDoc);		// this element must exist
								String argFramePOS 		= argFrameToken.getAttributeValue("pos");
								String argFrameLemma 	= argFrameToken.getAttributeValue("lemma");
								
/* Inner PREDICATE */			argPlaceholder = new Predicate(Boolean.FALSE, argId, argFramePOS, argFrameLemma, argFrameDisplName);


							}

							// init new role instance (edge between two nodes in graph)
							sentenceSRLGraph.addEdge(new Role(argument.getAttributeValue("role")), predicate, argPlaceholder);
/* GRAPH */
						}				
					}


/******************/
					oneGraphPerSentence.add(sentenceSRLGraph);
					if(visualize)
						GRAPHTESTER.visGraph(sentenceSRLGraph, sentenceText);	
					
/******************/					
					
				}

			/**}catch(Exception e){
				System.err.println("while parsing srl xml");
				e.printStackTrace();
			}

		}catch(Exception e){
			System.err.println("while reading in xml file");
			e.printStackTrace();
		}
		**/
		

		return oneGraphPerSentence;


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
