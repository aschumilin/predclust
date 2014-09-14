package algo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jdom2.DefaultJDOMFactory;
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

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Node;
import graph.Predicate;
import graph.Ref;
import graph.Role;

public class GraphDumper extends Parallelizable {
	
	private static final String TEXT_COLLECTION_NAME = 	"docs";
	private static final String SRL_COLLECTION_NAME = "srl";


//	 singleton mongoDB client for all threads
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
	private static String annotSourceDir	= System.getProperty("annots.dir");
	
	
	
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
	private Document getSrl(String longDocKey, Logger L){
		
		if (true){
			Document doc = null;
			try {
				doc = new SAXBuilder().build(new File("/home/pilatus/Desktop/srl-en-test-text.xml"));
			} catch (JDOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return doc;
			
			
			
		}else{
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
				return jdomDoc;
			}
			
		}catch(Exception e){
			L.error("<" + longDocKey + ">\t exception during srl coll lookup");
			Worker.decrBusyWorkers();
			return null;
		}
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
		EntityIndex annotationsIndex = new EntityIndex(getAnnotationsJdom(docKey, annotSourceDir, L));
		
		// 2. get srl xml
		Document srlJdomDoc = getSrl(longDocKey, L);
		
		// 3. extract graph from srl
		// cumulated text length is computed in extractGraph function
	
		TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> sentenceGraphSetMap = null;
		
		try {
			sentenceGraphSetMap = composeAnnotatedGraphs(docKey, srlJdomDoc, annotationsIndex);
		} catch (JDOMException e) {
			L.error("<" + docKey +">\t jdom exception composing graph", e);
		} catch (IOException e) {
			L.error("<" + docKey +">\t io exception composing graph", e);
		}

		// 4. serialize result to file
		
		FileOutputStream f_out = null;
		ObjectOutputStream obj_out = null;
		try {
			f_out = new FileOutputStream(resultDir + docKey + ".graph");
			obj_out = new ObjectOutputStream (f_out);
			obj_out.writeObject(sentenceGraphSetMap);
			obj_out.close();
		} catch (FileNotFoundException e) {
			L.error("<" + docKey +">\t file not found exception when writing result file", e);
		} catch (IOException e) {
			L.error("<" + docKey +">\t io exception when writing result file", e);
		} finally{
			try {
				obj_out.close();
			} catch (IOException e) {
				L.error("<" + docKey +">\t io exception closing object output stream");
			}
		}
		
		
		
		
// finished !		
		Worker.decrBusyWorkers();
	}
	
	
	public static void main(String[] args) throws JDOMException, IOException {

//		String[] ids = new String[] {"en-26221135.xml","en-63876.xml","en-690842.xml","es-14819.xml","es-54595.xml","es-79562.xml"};
//
//		for (String file : ids){
//			
//		}
		
//		result.dir=/home/pilatus/Desktop/annot-test/res/ 
//				annots.dir=/home/pilatus/Desktop/annot-test/
		System.out.println("1");
		GraphDumper gd = new GraphDumper();
		System.out.println("2");
		long anf = System.currentTimeMillis();
		gd.runAlgo("en-testannot.xml", Logger.getLogger(GraphDumper.class));
		System.out.println(System.currentTimeMillis() - anf);
		System.out.println("3");
		
//		File annotFile = new File("/home/pilatus/Desktop/srl-en-test-text.xml");
//		SAXBuilder builder = new SAXBuilder();
//		Document jdomDoc = (Document) builder.build(annotFile);
//
//		XPathFactory xpf = XPathFactory.instance();
//		XPathExpression<Element> expr = null;
//	
//		DefaultJDOMFactory jdf = new DefaultJDOMFactory();
//
//		
//		
//		
//		// 1. collect all mentions
//		
//		
//		String nodeId = "W28";
//		
//		// get all mention-token elements
//		expr = xpf.compile("/item/nodes/node[@id='" + nodeId + "']", Filters.element());
//		Element node = expr.evaluateFirst(jdomDoc);
//		Element n = (Element) node.clone();
//		n.detach();
//		expr = xpf.compile("/node/mentions/mention[@sentenceId='" + "3" + "']/mention_token", Filters.element());
//		Document d = jdf.document(n);
//		System.out.println("---");
//		List<Element> nodeMentionTokens = expr.evaluate(d); 
//		for (Element ll : nodeMentionTokens){
//			System.out.println(ll.getAttributeValue("id"));
//		}
//		System.out.println("---");
//		
//		expr = xpf.compile("/node/mentions/mention[@sentenceId='" + "3" + "']/mention_token", Filters.element());
//		nodeMentionTokens = expr.evaluate(d); 
//		for (Element ll : nodeMentionTokens){
//			System.out.println(ll.getAttributeValue("id"));
//		}
//		System.out.println("---");
//		System.out.println("---");
		
		
//expr = xpf.compile("/item/nodes/node[@id='" + "W28" + "']/mentions/mention[@sentenceId='3']/mention_token", Filters.element());
//		
//		List<Element> l = expr.evaluate(jdomDoc);
//		for (Element e : l){
//			System.out.println(e.getAttributeValue("id"));
//		}
//		expr = xpf.compile("//frame", Filters.element());
//		List<Element> fs = expr.evaluate(jdomDoc);
//		for(Element f: fs){
////			System.out.println(f.getAttributeValue("id") + " valid " + validateFrame(f));
//		}
//		
//		TreeMap<String, ArrayList<Element>> map = new TreeMap<String, ArrayList<Element>>();
//		TreeSet<String> s = new TreeSet<String>();
//		s.add("hi");
//		s.add("bye");
//		System.out.println(s);
//		s.remove("hi");
//		System.out.println(s);
		
//		expr = xpf.compile("//argument", Filters.element());
//		
//		List<Element> argS = expr.evaluate(jdomDoc);
//		for (Element a : argS){
//			Object[] res = validateNodeArgument(a, a.getAttributeValue("));
//			if (res != null){
//				System.out.println(res);
//			}
//		}
	}

	
	/**
	 * Compute this once for each srl xml.
	 * @param srlRoot
	 * @return array with cumulated text length for global token positioning (adjustment to annotation).
	 */
	private int[] computeCumulatedTextLength(List<Element> sentences){

		int numSentences = sentences.size();
		int cumulTextLength[] = new int[numSentences];
		cumulTextLength[0] = 0;
		int i = 1;
		
		for(Element sent : sentences){
			if (i == numSentences){
				break;
			}
			String sentenceText = sent.getChild("text").getText();
			cumulTextLength[i] = cumulTextLength[i-1] + sentenceText.length();
//			System.out.println("sent " + i + " = " + cumulTextLength[i]);
			i++;
		}
		return cumulTextLength;
	}
	/**
	 * Srl output provides the from/to indices of tokens on individual sentence level.
	 * This methode computes the global article-level indices for any token in a given sentence.
	 * @param sentenceId number of the sentence as given by the ID attribute value
	 * @param from token begin index on sentence level
	 * @param to token end index on sentence level
	 * @return int[]{globalFrom, globalTo}
	 */
	private int[] getGlobalIndices(String sentenceId, String from, String to, int[] cumulTextLength){
		
		int globalFrom, globalTo;
		
		int sentId = Integer.parseInt(sentenceId);
		
		globalFrom = Integer.parseInt(from) + cumulTextLength[sentId-1];
		globalTo = Integer.parseInt(to) + cumulTextLength[sentId -1];
//		System.out.println(from + "-" + to +" = " + globalFrom + "-" + globalTo);
		
		return new int[]{globalFrom, globalTo};	
	}

	
	
	
	
	
	/**
	 * Node is valid if it has at least one DBPedia annotation for all its mentions in a given sentence.
	 * @param argument
	 * @param sentenceId
	 * @param srlDoc
	 * @param entIndex
	 * @return Object[]{graph.Node, Role} new node complete with all attributes and annotations OR null if no annotation found
	 */
	private Object[] validateNodeArgument(Element argument, String sentenceId, Document srlDoc, EntityIndex entIndex, int[] cumulTextLength){
		
		XPathFactory xpf = XPathFactory.instance();
		XPathExpression<Element> expr = null;
		DefaultJDOMFactory jdf = new DefaultJDOMFactory();

		
		
		
		// 1. collect all mentions
		
		
		String nodeId = argument.getAttributeValue("id");
		expr = xpf.compile("/item/nodes/node[@id='" + nodeId + "']", Filters.element());		
		Element n = expr.evaluateFirst(srlDoc);
		Element node = (Element) n.clone();
		node.detach();
		Document docNode = jdf.document(node);	// eval xpath on smaller document for speedup
		
		expr = xpf.compile("/node/mentions/mention[@sentenceId='" + sentenceId + "']/mention_token", Filters.element());
		List<Element> nodeMentionTokens = expr.evaluate(docNode); 
		
		// for each mention, get its coordinates
		LinkedList<int[]> mentionCoordinates = new LinkedList<int[]>();
		
		for (Element mentionToken : nodeMentionTokens){
			
			String tokenId = mentionToken.getAttributeValue("id");
			expr = xpf.compile("/item/sentences/sentence[@id='" + sentenceId + "']/tokens/token[@id='" + tokenId + "']", Filters.element());
			Element token = expr.evaluateFirst(srlDoc);
			
			String from = token.getAttributeValue("start");
			String to = token.getAttributeValue("end");
			
			// !!! convert to global indices !!!
			mentionCoordinates.add(getGlobalIndices(sentenceId, from, to, cumulTextLength));
		}
		
		// get the Annotations OR null
		String[] bestNodeAnnotation = entIndex.getBestAnnotation(mentionCoordinates);
		
		if(bestNodeAnnotation != null){
			
			
			
			
			
			// id, type(_class), displayname, mention, Annotations
			String type, nodeClass, role, displayName, mention;
			type = node.getAttributeValue("type");
			nodeClass = null;
			if((nodeClass = node.getAttributeValue("class")) != null)
				type = type + "_" + nodeClass;
			role = argument.getAttributeValue("role");
			displayName = node.getAttributeValue("displayName");
			expr = xpf.compile("/node/mentions/mention[@sentenceId='" + sentenceId + "']", Filters.element());
			mention = expr.evaluateFirst(docNode).getAttributeValue("words");
			
			

			Node newGraphNode = new Node(nodeId, type, displayName, mention);
			// String[]{URL, mention, weight, from, to} = bestNodeAnnotation
			Ref dbpeadiaRef = new Ref(bestNodeAnnotation[0], bestNodeAnnotation[1], "dbpedia", Double.parseDouble(bestNodeAnnotation[2]));
			newGraphNode.addRef(dbpeadiaRef);
			
			Ref wordnetRef = null;
			
			if (node.getChild("descriptions") != null){
				expr = xpf.compile("/node/descriptions/description[@knowledgeBase='WordNet-3.0']", Filters.element());
				Element wordnetDescr = expr.evaluateFirst(docNode);
				if(wordnetDescr != null){
					/* wordnet annotation gets a weight of 1 */
					wordnetRef = new Ref(wordnetDescr.getAttributeValue("URI"), wordnetDescr.getAttributeValue("displayName"), "wordnet-3.0", 1);
					newGraphNode.addRef(wordnetRef);
				}
			}
			
			return new Object[]{newGraphNode, new Role(role)} ;
			
		}else{
			return null;		 
		}	
	}

	
	
	/**
	 * A frame is valid if it has >= 2 valid non-frame arguments.
	 * @param frame
	 * @param sentenceId
	 * @param srlDoc
	 * @return true or false
	 */
	private List<Object[]> validateFrame(Element frame, String sentenceId, Document srlDoc, EntityIndex entIndex, int[] cumulTextLength){
		List<Element> arguments = frame.getChildren("argument");
//		int totalArgs = arguments.size();
		int numValidNodeArgs = 0;
		List<Element> frameArguments = new LinkedList<Element>();
		List<Object[]> nodeArguments = new LinkedList<Object[]>();
		
		// count valid node-arguments
		for (Element arg : arguments){
			if(arg.getAttributeValue("frame") == null){
				 // determine if node is valid
				Object[] potentiallyValidNode = validateNodeArgument(arg, sentenceId, srlDoc, entIndex, cumulTextLength);
				if(potentiallyValidNode != null){
					numValidNodeArgs ++;
					nodeArguments.add(potentiallyValidNode);
				}				
			}else{
				// save the frame-arguments for later
				frameArguments.add(arg);
			}
		}
		
		if(numValidNodeArgs >= 2){
			// make nodes from frame-arguments
			String id, type, dispName;
			Role role;
			for(Element frameArg : frameArguments){
				
				id 		= frameArg.getAttributeValue("id");
				dispName = frameArg.getAttributeValue("displayName");
				role 	= new Role(frameArg.getAttributeValue("role"));
				type 	= "PREDICATE";
				Node frameNode = new Node(id, type, dispName, "");
				
				nodeArguments.add(new Object[]{frameNode, role});
			}
			return nodeArguments;
		}else{
			
			// return null if frame has less than two valid arguments
			return null;
		}
	}
	
	/*
	private static boolean isValidFrame(Element frame){

		List<Element> arguments = frame.getChildren("argument");
		int size = arguments.size();

		if( size > 2){

			boolean frameValidationResult = true;

			for(Element arg : arguments){

				if(arg.getAttributeValue("frame") != null){
					return frameValidationResult & isValidFrame(arg);
				}			
			}

			return frameValidationResult;
		}else{
			return false;
		}
	}
	private static void magic2 (List<Element> frames){

		TreeMap<String, LinkedList<Element>> global = new TreeMap<String, LinkedList<Element>> ();
		String parentId = null;

		for (Element me : frames){
			List<Element> myArguments = me.getChildren("argument");
			String myId = me.getAttributeValue("id");

			LinkedList<Element> kids = null;
			boolean hasKids = false;
			for(Element arg : myArguments){

				if(arg.getAttributeValue("frame") != null){

				}			
			}

			if (hasKids){
				global.put(myId, kids);
			}else{
				global.put(myId, null);
			}

		}

	}
	private static void magic(Element me, TreeMap<String, List<Element>> global, String parentId, TreeSet<String> todo){

		List<Element> myArguments = me.getChildren("argument");
		String myId = me.getAttributeValue("id");


		if (parentId == null){

			// separate graph
			ArrayList<Element> container = new ArrayList<Element>();
			container.add(me);
			global.put(myId, container);


			for(Element arg : myArguments){				
				if(arg.getAttributeValue("frame") != null){
					magic(arg, global, myId, todo);
				}			
			}	
		}else{
			// graph is subgraph

			List<Element> container = global.get(parentId);
			container.add(me);
			global.put(parentId, container);

			for(Element arg : myArguments){

				if(arg.getAttributeValue("frame") != null){
					magic(arg, global, parentId, todo);
				}			
			}
		}
	}

*/
	private TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> composeAnnotatedGraphs(String shortDocKey, Document srlJdomDoc, EntityIndex entIndex) throws JDOMException, IOException{
		
		////////result variables
		TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> sentenceToGraphSetMap = new TreeMap<String, List<DirectedSparseGraph<Argument, Role>>>();
		////////

		XPathFactory xpf = XPathFactory.instance();
		XPathExpression<Element> expr = null;
		Element srlRoot = srlJdomDoc.getRootElement();
		
		List<Element> sentences = srlRoot.getChild("sentences").getChildren("sentence");
		
		// 1. prepare the local-to-global token indexing 
		int[] cumulatedTextLength = computeCumulatedTextLength(sentences);
		
		// 2. for each sentence			 
		for (Element sent : sentences) {

			//////// result variables
			List<DirectedSparseGraph<Argument, Role>> graphsInSentence = new LinkedList<DirectedSparseGraph<Argument, Role>>();
			////////


			String sentenceId = sent.getAttributeValue("id");

			// 3. get all frames for that sentence
			expr = xpf.compile("/item/frames/frame[@sentenceId='"+sentenceId + "']", Filters.element());
			List<Element> frames = expr.evaluate(srlJdomDoc);

			// only first frame for each sentence can be root frame 
			int rootIndicator = 0;		// to keep track of first frame in each sentence

			
			for(Element frame: frames){
				//////// result variables
				DirectedSparseGraph<Argument, Role> oneGraphPerFrame = new DirectedSparseGraph<Argument, Role>();
				////////
				
				rootIndicator ++;

				// 3. check if this frame is valid
				List<Object[]> validatedNodesList = validateFrame(frame, sentenceId, srlJdomDoc, entIndex, cumulatedTextLength);

				if (validatedNodesList != null){

					
						
					// 3.1. construct new predicate					
					// each frame represents a predicate
					// 1. extract frame attributes (isRoot, pos, lemma, displName) through tokenID
					boolean isRoot = (rootIndicator == 1);
					String frameTokenID 	= frame.getAttributeValue("tokenId");
					expr = xpf.compile("/item/sentences/sentence[@id='" + sentenceId + "']/tokens/token[@id='"+frameTokenID + "']", Filters.element());
					Element frameToken 		= expr.evaluateFirst(srlJdomDoc);		
					String pos 				= frameToken.getAttributeValue("pos");
					String frameMention 	= frameToken.getText();
					String frameDisplName 	= frame.getAttributeValue("displayName");
					String frameID 			= frame.getAttributeValue("id");
					
/* PREDICATE */		Predicate predicate = new Predicate(isRoot, frameID, pos, frameDisplName, frameMention);
					
					// add KB references to predicate
					Element descriptions = frame.getChild("descriptions");
					if (descriptions != null){
						List<Element> predRefs = descriptions.getChildren("description");
						for(Element predRef : predRefs){
							
							String refKB = predRef.getAttributeValue("knowledgeBase");
							
							if(refKB.startsWith("W")){//startsWith("WordNet")){
								String uri = predRef.getAttributeValue("URI");
								String refDisplName = predRef.getAttributeValue("displayName");
								/* wordnet annotation gets a weight of 1 */
								Ref predReference = new Ref(uri, refDisplName, refKB, 1);
								predicate.addRef(predReference);
							}
						}
					}
					
					
					// 3.2. compose graph					
					for (Object[] nodeContainer : validatedNodesList){
/* GRAPH */				oneGraphPerFrame.addEdge((Role)nodeContainer[1], predicate, (Node)nodeContainer[0]);
						
					}
					graphsInSentence.add(oneGraphPerFrame);
					
GRAPHTESTER.visGraph(oneGraphPerFrame, "sentence " + sentenceId);
					
				} else {
					continue;
				}
		
			} // loop over frames finished

			
			
			// construct informative sentence id from <lang>-<articleID>-<sentenceID>
			String globalSentenceID = shortDocKey + "-" + sentenceId;
/* MAP */	sentenceToGraphSetMap.put(globalSentenceID, graphsInSentence);

			
		} // loop over sentences finished
				
		
		return sentenceToGraphSetMap;
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
