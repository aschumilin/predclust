package algo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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

/**
 * @author aschumilin
 * 
 * creates graph from SRL xml output 
 *
 */
public class XmlToGraph extends Parallelizable {


	private static final String ORIGIN_COLLECTION_NAME = 	"srl";
	private static final String TARGET_COLLECTION_NAME = 	"graph";



	// singleton mongoDB client for all threads
	private static MongoClient 	mongoClient = null;
	private static DB 			db = null;
	private static DBCollection collSrl = null;
	private static DBCollection collGraph = null;
	private static String 		mongoAddr = System.getProperty("mongo.addr");
	private static int 			mongoPort = Integer.parseInt(System.getProperty("mongo.port"));
	private static String 	mongoUser = System.getProperty("mongo.user");
	private static String 	mongoPass = System.getProperty("mongo.pwd");
	private static String 	mongoDBName = System.getProperty("mongo.db.name");


public static void main(String[] args) {
	String id = "http://en.wikipedia.org/wiki?curid=9833554";
	util.Mongo m = new util.Mongo("romulus", "srl");
	String srl = m.getById(id).get("srlAnnot").toString();
	System.out.println(srl);

	
	
	try{
		// don't use buffering
		OutputStream file = new FileOutputStream("/home/pilatus/Desktop/graph.ser");
//		OutputStream buffer = new BufferedOutputStream(file);
		ObjectOutput output = new ObjectOutputStream(file);//buffer);
		try{
			ArrayList<DirectedSparseMultigraph<Argument,Role>> graph= composeGraph(srl, true);
			output.writeObject(graph);
			System.out.println("finished serializing");
			System.out.println(graph.size());
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{output.close();}
	}  
	catch(IOException ioe){
		System.out.println("could not serialize annotations map: ");
	}
	
	
	try {
		composeGraph(srl, true);
	} catch (JDOMException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	System.out.println();
}

	@Override
	public void runAlgo(String docKey, Logger L) {




		/////////////////////////////////////		
		// currently process only english wikinews docs 
		if(! docKey.contains("http://en.wikinews.org")){
			Worker.decrBusyWorkers();
			return;
		}
		///////////////////////////////////		






		try{
			getDB(L);	
		}catch(Exception e){
			L.fatal("\t can't perform getDB.. exiting...", e);
			Worker.decrBusyWorkers();
			return;
		}

		if( collSrl!=null && collGraph!=null){	


			// 1. query the DB for given docId (it is the primary key in the [srlxml] collection)

			BasicDBObject query = new BasicDBObject("_id", docKey);
			DBCursor results = collSrl.find(query);
			BasicDBObject doc = null;
			try {
				doc = (BasicDBObject)results.next();
			}catch(Exception e){
				L.error(docKey + "\t exception while getting result set");
				Worker.decrBusyWorkers();
				return;
			}finally {
				results.close();
			}

			if( doc!=null ){


				/* watch out for broken results of SRL pipeline
				 * srlAnnot : "<analysisServiceErrorResponse>analysis_en service failed: Invalid UTF-8</analysisServiceErrorResponse>"
				 */

				final String srlXml = doc.get("srlAnnot").toString();
				//////////////////////////////				
				if( ! srlXml.startsWith("<item>")){
					L.error(docKey + "\t exiting worker...bad srl xml: " + srlXml);
					Worker.decrBusyWorkers();
					return;
				}
				////////////////////////////								
				doc.removeField("_id");
				doc.removeField("srlAnnot");


				// parse xml and construct graph
	

				// add datadoc to mongoDB: {_id,{lang, project, docId, docTitle}, docKey, sentence, List<Graph>}
				// write new doc to the graph collection
//				try{
//					// new db object gets auto-generated _id field
//					L.debug("graphs size: " + graphs.size());
//					BasicDBObject graphDoc = new BasicDBObject(docCarcass)
//					.append("docKey", docKey)
//					.append("sentence", sentenceText)
//					.append("graphs", graphs);
//					collGraph.insert(graphDoc);
//				}catch(Exception e){
//					L.error(docKey + "\t exception writing to result collection", e);
//					Worker.decrBusyWorkers();
//					return;
//				}


			}
		}
}


	
	public static ArrayList<DirectedSparseMultigraph<Argument,Role>> composeGraph(String srlXMLString, boolean visualize) throws JDOMException, IOException{

		ArrayList<DirectedSparseMultigraph<Argument, Role>> oneGraphPerSentence = new ArrayList<DirectedSparseMultigraph<Argument, Role>>();
//		try {

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
						String lemma 	= frameToken.getAttributeValue("lemma");
						String frameDisplName = frame.getAttributeValue("displayName");
						String frameID = frame.getAttributeValue("id");
						// only first frame for each sentence can be root frame !!!
						boolean isRoot = (rootIndicator == 1);		
/* PREDICATE */			Predicate predicate = new Predicate(isRoot, frameID, POS, lemma, frameDisplName, isRoot);

						// add KB references to predicate
						Element descriptions = frame.getChild("descriptions");
						if (descriptions != null){
							List<Element> predRefs = descriptions.getChildren("description");
							for(Element predRef : predRefs){
								String uri = predRef.getAttributeValue("URI");
								String refDisplName = predRef.getAttributeValue("displayName");
								String refKB = predRef.getAttributeValue("knowledgeBase");
								Ref predReference = new Ref(uri, refDisplName, refKB, 123);

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
								argPlaceholder = null;//new Node(nodeId, nodeType, "?pos", "?lemma", nodeDisplName);

								// 4.6. read the node KB references
								expr = xFactory.compile("//node[@id='" + nodeId + "']", Filters.element());
								Element nodeDescriptions = expr.evaluateFirst(jdomDoc).getChild("descriptions");
								if (nodeDescriptions != null){
									List<Element> nodeRefs = nodeDescriptions.getChildren("description");
									for(Element nodeRef : nodeRefs){
										Ref r = new Ref(nodeRef.getAttributeValue("URI"), nodeRef.getAttributeValue("displayName"), nodeRef.getAttributeValue("knowledgeBase"), 123);
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
								
/* Inner PREDICATE */			argPlaceholder = new Predicate(Boolean.FALSE, argId, argFramePOS, argFrameLemma, argFrameDisplName, false);


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
	




private static synchronized void getDB(Logger L) throws UnknownHostException{
	if (mongoClient == null || db == null || collSrl == null){
		L.info("mongoClient null. Initializing...");
		mongoClient = new MongoClient( mongoAddr , mongoPort );
		//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
		db = mongoClient.getDB(mongoDBName);
		boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
		if( ! auth ){
			L.fatal("exiting because authentication failed for user " + mongoUser + " @ " + mongoAddr + ":" + mongoPort);
			System.exit(1);
		}else{

			collSrl = db.getCollection(ORIGIN_COLLECTION_NAME);
			collGraph = db.getCollection(TARGET_COLLECTION_NAME);

			// get all collection names
			L.info("db connection and authentication ok");
			L.info("listing collections for "+ mongoDBName + ":");
			for (String s : db.getCollectionNames())
				L.info("\t" + s);
		}


	}
}




}
