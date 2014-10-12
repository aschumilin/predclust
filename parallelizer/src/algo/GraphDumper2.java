package algo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

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
import annotator.EntityIndex;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Node;
import graph.Predicate;
import graph.Ref;
import graph.Role;

public class GraphDumper2 extends Parallelizable {
	
	private static final String TEXT_COLLECTION_NAME = 	"docs";
	private static final String SRL_COLLECTION_NAME = "srl";

	// DBpedia categories
	private static TreeMap<String, ArrayList<String>> cats = null; 
	
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
	
	private static String catsMapFilePath 	= System.getProperty("cats.path");
	
	
	/**
	 * @param shortDocKey
	 * @param annotPath
	 * @param L
	 * @return jdom2.Document of detected topics OR null if error
	 * @throws IOException 
	 * @throws JDOMException 
	 */
	private Document getAnnotationsJdom(String shortDocKey, String annotPath, Logger L) throws IOException, JDOMException{
		// parse annot file, return as jdom2.Document
		String annotFileName = annotPath + shortDocKey;
		File annotFile = new File(annotFileName);
		SAXBuilder builder = new SAXBuilder();
		Document jdomDoc;
		
		try {
			jdomDoc = (Document) builder.build(annotFile);
			return jdomDoc;
		} catch (JDOMException e) {
			throw new JDOMException("<" + shortDocKey + ">\t JDOMException when parsing annot file");
		} catch (IOException e) {
			throw new IOException ("<" + shortDocKey + ">\t IOException when parsing annot file");
		}
		
	}
	
	/**
	 * @param longDocKey
	 * @param L
	 * @return root Element (<item>)of srl xml OR null if error
	 * @throws IOException 
	 * @throws MongoException, JDOMException 
	 */
	private Document getSrl(String longDocKey, Logger L) throws MongoException, JDOMException, IOException{
	
		
		
		BasicDBObject query = new BasicDBObject("_id", longDocKey);	
		DBCursor dbResult = null;

		dbResult = collSrl.find(query).limit(1);

		if(dbResult.size() == 0){
			Worker.decrBusyWorkers();
			
			throw new MongoException("<" + longDocKey + ">\t srl doc not found in DB");
		}else{
			
			BasicDBObject dbDoc = (BasicDBObject)dbResult.next();
			SAXBuilder builder = new SAXBuilder();
			String srlString = dbDoc.get("srlAnnot").toString();
			Document jdomDoc = (Document) builder.build(new StringReader(srlString));
			
//			FileWriter fw = new FileWriter(new File(resultDir + "srl-" + shortDocKey));
//			fw.write(dbDoc.get("srlAnnot").toString());
//			fw.flush();
//			fw.close();
			
			return jdomDoc;
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
		// es-2726363.xml -> http://es.wikipedia.org/wiki?curid=2726363
		String[] parts = shortKey.split("-");
		return "http://" + parts[0] + ".wikipedia.org/wiki?curid=" + parts[1].substring(0,parts[1].length()-4);		
	}

	
	@Override
	public void runAlgo(String docKey, Logger L) {
		
//	if(docKey.equals("es-996098.xml")){
//		Worker.decrBusyWorkers();
//		L.info("##################################################################################");
//		return;
//	}
		try{
			getDB(L);	
		}catch(Exception e){
			L.fatal("\t can't perform getDB.. exiting...", e);
			System.exit(1);
		}
		
		try{
			getCatsMap(L);
		}catch(Exception e){
			L.fatal("Exception when deserializing Entity-Category map from " + catsMapFilePath);
			L.fatal("exiting now ...");
			System.exit(1);
		}
	
		L.info("starting \t" + docKey);
		try{
			// docKeys are file names of annotation dumps
			String longDocKey = transformKey(docKey);
			// 1. get index of detected topics (entities)
			EntityIndex annotationsIndex = null;
			try {
				annotationsIndex = new EntityIndex(getAnnotationsJdom(docKey, annotSourceDir, L));
			} catch (Exception e1) {
				L.error("caught from getAnnotationsJdom() : ", e1);
				Worker.decrBusyWorkers();
				return;
			}

			Document srlJdomDoc = null;
			try {
				// 2. get srl xml
				srlJdomDoc = getSrl(longDocKey, L);
			} catch(Exception e){
				L.error("<" + longDocKey + ">\t srl retrieval caused exception: ", e);
				Worker.decrBusyWorkers();
				return;
			}

			// 3. extract graph from srl
			// cumulated text length is computed in extractGraph function

			TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> sentenceGraphSetMap = null;

			try {
				sentenceGraphSetMap = composeAnnotatedGraphs(docKey, srlJdomDoc, annotationsIndex);
			} catch (Exception e) {
				L.error("<" + docKey +">\t exception composing graph list for sentence", e);
				Worker.decrBusyWorkers();
				return;
			}

			// 4. serialize result to file

			FileOutputStream f_out = null;
			ObjectOutputStream obj_out = null;
			try {
				f_out = new FileOutputStream(resultDir + docKey.substring(0, docKey.length()-4) + ".graph");
				obj_out = new ObjectOutputStream (f_out);
				obj_out.writeObject(sentenceGraphSetMap);
				obj_out.close();
			} catch (Exception e) {
				L.error("<" + docKey +">\t file not found exception when writing result file", e);
				Worker.decrBusyWorkers();
				return;
			} finally{
				try {
					obj_out.close();
				} catch (IOException e) {
					L.error("<" + docKey +">\t io exception closing object output stream");
				}
			}
		}catch(Exception e){
			L.error("<" + docKey + ">\t crashed");
		}
		L.info("done \t" + docKey);

		
// finished !		
		Worker.decrBusyWorkers();
	}
	
	
	public static void main(String[] args) throws JDOMException, IOException {

		String[] ids = new String[] {"en-26221135.xml"}; //"en-26221135.xml","en-63876.xml","en-690842.xml","es-14819.xml","es-54595.xml"};
		
		long anf = System.currentTimeMillis();
		GraphDumper2 gd = new GraphDumper2();
		
		for (String annotFileName : ids){
			System.out.println("doing " + annotFileName);
			
			gd.runAlgo(annotFileName, Logger.getLogger(GraphDumper.class));
			/*
			InputStream file = new FileInputStream(resultDir + annotFileName);
			ObjectInput input = new ObjectInputStream(file);
			try{
				TreeMap<String, List<DirectedSparseGraph<Argument,Role>>> map= (TreeMap<String, List<DirectedSparseGraph<Argument,Role>>>)input.readObject();
		
				for (String sentId : map.keySet()){
					for(DirectedSparseGraph<Argument,Role> g : map.get(sentId)){
						test.GRAPHTESTER.visGraph(g, sentId);
					}
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally{input.close();}
			*/
		}
		System.out.println(System.currentTimeMillis() - anf);
		
		// /home/pilatus/Desktop/annot-test/en-26221135.xml
//		result.dir=/home/pilatus/Desktop/annot-test2/
//		annots.dir=/home/pilatus/Desktop/annot-test2/
		

		
			
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
	 * Node is validated by getting their entity annotation.
	 * Node without annotation is also vaid.
	 * @param argument
	 * @param sentenceId
	 * @param srlDoc
	 * @param entIndex
	 * @return Object[]{graph.Node, Role} new node complete with all attributes and annotations from DBpedia and/or WordNet if available. 
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
			
			// look for categories list in the DBpedia ent-cat map (4,449,790 entities)
			ArrayList<String> dbpCategories = cats.get(bestNodeAnnotation[0]);
			if(dbpCategories != null){
				for (String cat : dbpCategories){
					newGraphNode.addCat(cat);
				}
			}
			
			return new Object[]{newGraphNode, new Role(role)} ;
			
		}else{
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
		}	
	}

	
	
	/**
	 * Validation also parses frames recursively resulting in graphs with one ore more predicates.
	 * Frames that are arguments of top-level predicates are removed from the sentence's frameTreeMap 
	 * in order not to be processed again.
	 * A frame is valid if it has >= 2 non-frame arguments. 
	 * @param topLevelFrame
	 * @param sentenceId
	 * @param srlDoc
	 * @return Null if frame is npt valid. Or List of graph building blocks: Object[]{Role, predicateArg, nodeOrPredicateArg}. 
	 * List of Node arguments OR null. Each argument comes with its corresponding role in a Object[]{Node, Role}.
	 */
	
	private List<Object[]> validateFrame(Predicate topLevelPredicate, TreeMap<String, Boolean> framesToDoMap, Element parentFrame, String sentenceId, Document srlDoc, EntityIndex entIndex, int[] cumulTextLength){
//		System.out.println("sentID: " + sentenceId + " , frameID " + topLevelFrame.getAttributeValue("id"));
		
		
		XPathFactory xpf = XPathFactory.instance();
		XPathExpression<Element> expr = null;
		
		List<Element> allArguments = parentFrame.getChildren("argument");
//		int totalArgs = arguments.size();
		int numValidNonPredicateArgs = 0;
		List<Object[]> resultingArguments = new LinkedList<Object[]>();
		
		
		// count valid node-arguments
		for (Element arg : allArguments){
			if(arg.getAttributeValue("frame") == null){
				 // determine if node is valid
				Object[] potentiallyValidNonPredicateNode = validateNodeArgument(arg, sentenceId, srlDoc, entIndex, cumulTextLength);
				if(potentiallyValidNonPredicateNode != null){
					numValidNonPredicateArgs ++;
					
					// add node to this top level predicate
					resultingArguments.add(new Object[]{(Role)potentiallyValidNonPredicateNode[1], topLevelPredicate, potentiallyValidNonPredicateNode[0]});
				}				
			}else{
				// !!! handle predicate arguments recursively only if this top-level frame is valid !!!			
//				frameArguments.add(arg);
			}
		}
		
		if(numValidNonPredicateArgs >= 2){
			
			//
			// after processing the valid non-predicate arguments
			// NOW go for predicate-arguments recursively !!!
			//
			for (Element subFrameArg : allArguments){
				if(subFrameArg.getAttributeValue("frame") != null){
					 
					// 1. construct subLevelPredicate				
					// extract frame attributes (isRoot, pos, lemma, displName) through tokenID
					
					String subFrameID 			= subFrameArg.getAttributeValue("id");
					// get from this frame argument to the actual frame element and grab tokenID from there
					expr = xpf.compile("/item/frames/frame[@id='" + subFrameID + "']", Filters.element());	
					Element actualSubFrame	= expr.evaluateFirst(srlDoc); 
					String frameTokenID 	= actualSubFrame.getAttributeValue("tokenId");
					expr = xpf.compile("/item/sentences/sentence[@id='" + sentenceId + "']/tokens/token[@id='"+frameTokenID + "']", Filters.element());
					Element frameToken 		= expr.evaluateFirst(srlDoc);		
					
					String pos 				= frameToken.getAttributeValue("pos");
					String frameMention 	= frameToken.getText();
					String frameDisplName 	= subFrameArg.getAttributeValue("displayName");
					
/* SUB-PREDICATE */	Predicate subPredicate = new Predicate(false, subFrameID, pos, frameDisplName, frameMention, false);
					
					// add KB references to predicate
					Element descriptions = subFrameArg.getChild("descriptions");
					if (descriptions != null){
						List<Element> predRefs = descriptions.getChildren("description");
						for(Element predRef : predRefs){
							
							String refKB = predRef.getAttributeValue("knowledgeBase");
							
							if(refKB.startsWith("W")){//startsWith("WordNet")){
								String uri = predRef.getAttributeValue("URI");
								String refDisplName = predRef.getAttributeValue("displayName");
								/* wordnet annotation gets a weight of 1 */
								Ref predReference = new Ref(uri, refDisplName, refKB, 1);
								subPredicate.addRef(predReference);
							}
						}
					}
				
					
					List<Object[]> nonPredicateArgsOfSubframe = null;
					// RECURSION HERE
					nonPredicateArgsOfSubframe = validateFrame(subPredicate, framesToDoMap, actualSubFrame, sentenceId, srlDoc, entIndex, cumulTextLength);
					

					
					if(nonPredicateArgsOfSubframe != null){		// valid sub-predicate
						// 1. make and add new predicate node 
						// 2. remove it from global frameMap
						// 3. add all of its returned non-predicate nodes
						
						
						// 2.
						framesToDoMap.put(subFrameID, true);
						
						// 3. 
						// add sub-predicate and its role to argument list						
						resultingArguments.add(new Object[]{new Role(subFrameArg.getAttributeValue("role")), topLevelPredicate, subPredicate});
						resultingArguments.addAll(nonPredicateArgsOfSubframe);
					}else{
						// sub-frame is not valid
					}
				}else{
					// non-predicate arguments already handled above			
				}
			} // for loop over sub-frame arguments 
			return resultingArguments;
		}else{
			// return null if frame has less than two valid arguments
			return null;
		}
	}
	
	
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
			
			// build a map of frames with frameID as key and frame element as value
			TreeMap<String, Element> frameTreeMap = new TreeMap<String, Element>();
			TreeMap<String, Boolean> framesToDoTracker = new TreeMap<String, Boolean>();
			for (Element frame : frames){
				String id = frame.getAttributeValue("id");
				frameTreeMap.put(id, frame);
				framesToDoTracker.put(id, false);
			}
			
			// only first frame for each sentence can be root frame 
			int rootIndicator = 0;		// to keep track of first frame in each sentence

			
			for(String frameID: frameTreeMap.keySet()){
				
				////////////////////
				// move to next frame if this one was already done recursively
				if(framesToDoTracker.get(frameID) == true){
					continue;
				}
				////////////////////
				
				//////// 
				// result variables
				DirectedSparseGraph<Argument, Role> oneGraphPerFrame = new DirectedSparseGraph<Argument, Role>();
				////////
				
				rootIndicator ++;
				
				Element currentFrame = frameTreeMap.get(frameID);
				
				// 3.0. construct new predicate					
				// each frame represents a predicate
				// 1. extract frame attributes (isRoot, pos, lemma, displName) through tokenID
				boolean isRoot = (rootIndicator == 1);
				String frameTokenID 	= currentFrame.getAttributeValue("tokenId");
				expr = xpf.compile("/item/sentences/sentence[@id='" + sentenceId + "']/tokens/token[@id='"+frameTokenID + "']", Filters.element());
				Element frameToken 		= expr.evaluateFirst(srlJdomDoc);		
				String pos 				= frameToken.getAttributeValue("pos");
				String frameMention 	= frameToken.getText();
				String frameDisplName 	= currentFrame.getAttributeValue("displayName");
				
/* PREDICATE */	Predicate topLevelPredicate = new Predicate(isRoot, frameID, pos, frameDisplName, frameMention, true);
				
				// add KB references to predicate
				Element descriptions = currentFrame.getChild("descriptions");
				if (descriptions != null){
					List<Element> predRefs = descriptions.getChildren("description");
					for(Element predRef : predRefs){
						
						String refKB = predRef.getAttributeValue("knowledgeBase");
						
						if(refKB.startsWith("W")){//startsWith("WordNet")){
							String uri = predRef.getAttributeValue("URI");
							String refDisplName = predRef.getAttributeValue("displayName");
							/* wordnet annotation gets a weight of 1 */
							Ref predReference = new Ref(uri, refDisplName, refKB, 1);
							topLevelPredicate.addRef(predReference);
						}
					}
				}
				
				// 3. check if this frame is valid
				List<Object[]> validatedNodesList = null;
				try{
					validatedNodesList = validateFrame(topLevelPredicate, framesToDoTracker, frameTreeMap.get(frameID), sentenceId, srlJdomDoc, entIndex, cumulatedTextLength);
				}catch(StackOverflowError ee){
					System.out.println("#############################");	
					continue;
				}
				
				
				if (validatedNodesList != null){
					
					// !!! mark this top-level frame as done on the todo list of frames. This is also done in validateFrame() !!!
					framesToDoTracker.put(frameID, true);
										
					
					// 3.2. compose graph					
					for (Object[] nodeContainer : validatedNodesList){
/* GRAPH */				oneGraphPerFrame.addEdge((Role)nodeContainer[0], (Argument)nodeContainer[1], (Argument)nodeContainer[2]);
						
					}
					graphsInSentence.add(oneGraphPerFrame);
					
//GRAPHTESTER.visGraph(oneGraphPerFrame, "sentence " + sentenceId);
					
				} else {
					continue;
				}
		
			} // loop over frames finished

			
			
			// construct informative sentence id from <lang>-<articleID>-<sentenceID>
			String globalSentenceID = shortDocKey + "-" + sentenceId;
			if(graphsInSentence.size() > 0){
/* MAP */		sentenceToGraphSetMap.put(globalSentenceID, graphsInSentence);
			}

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
	
	private static synchronized void getCatsMap(Logger L) throws IOException, ClassNotFoundException {
		if (cats == null){
			L.info("DBPedia categories map null. Initializing...");

			FileInputStream fis = new FileInputStream(new File(catsMapFilePath));
			ObjectInputStream ois = new ObjectInputStream(fis);

			cats = (TreeMap<String, ArrayList<String>>) ois.readObject();

		}

	}




//private static boolean isValidFrame(Element frame){
//
//	List<Element> arguments = frame.getChildren("argument");
//	int size = arguments.size();
//
//	if( size > 2){
//
//		boolean frameValidationResult = true;
//
//		for(Element arg : arguments){
//
//			if(arg.getAttributeValue("frame") != null){
//				return frameValidationResult & isValidFrame(arg);
//			}			
//		}
//
//		return frameValidationResult;
//	}else{
//		return false;
//	}
//}
//
//
//private static void magic2 (List<Element> frames){
//
//	TreeMap<String, LinkedList<Element>> global = new TreeMap<String, LinkedList<Element>> ();
//	String parentId = null;
//
//	for (Element me : frames){
//		List<Element> myArguments = me.getChildren("argument");
//		String myId = me.getAttributeValue("id");
//
//		LinkedList<Element> kids = null;
//		boolean hasKids = false;
//		for(Element arg : myArguments){
//
//			if(arg.getAttributeValue("frame") != null){
//
//			}			
//		}
//
//		if (hasKids){
//			global.put(myId, kids);
//		}else{
//			global.put(myId, null);
//		}
//
//	}
//
//}
//private static void magic(Element me, TreeMap<String, List<Element>> global, String parentId, TreeSet<String> todo){
//
//	List<Element> myArguments = me.getChildren("argument");
//	String myId = me.getAttributeValue("id");
//
//
//	if (parentId == null){
//
//		// separate graph
//		ArrayList<Element> container = new ArrayList<Element>();
//		container.add(me);
//		global.put(myId, container);
//
//
//		for(Element arg : myArguments){				
//			if(arg.getAttributeValue("frame") != null){
//				magic(arg, global, myId, todo);
//			}			
//		}	
//	}else{
//		// graph is subgraph
//
//		List<Element> container = global.get(parentId);
//		container.add(me);
//		global.put(parentId, container);
//
//		for(Element arg : myArguments){
//
//			if(arg.getAttributeValue("frame") != null){
//				magic(arg, global, parentId, todo);
//			}			
//		}
//	}
//}
}

