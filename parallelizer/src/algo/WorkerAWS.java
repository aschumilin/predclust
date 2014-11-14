package algo;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Node;
import graph.Predicate;
import graph.Ref;
import graph.Role;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import parallelizer.Parallelizable;
import parallelizer.Worker;
import annotator.AnnotationService;
import annotator.EntityIndex;


public class WorkerAWS {


	// DBpedia categories
	private static TreeMap<String, ArrayList<String>> cats = null; 	
			
	private static final String wid		= System.getProperty("workerid");	
	private static final String resultDir 		= System.getProperty("resultdir");
	private static final String joblistFileName = System.getProperty("joblist");
	private static final String dataSourceDir	= System.getProperty("datadir");
	private static final String annotConfDir	= System.getProperty("annotconf");
	private static String catsMapFilePath 		= System.getProperty("catsmappath");
	private static final Logger L 				= Logger.getLogger(WorkerAWS.class); 
	private static BufferedWriter bw = null; 
	private static BufferedReader br = null;

	private static AnnotationService annotatorEN = null;
	private static AnnotationService annotatorES = null;

	private static ArrayList<String[]> jobList = new ArrayList<String[]>();

	private static final int updateIntervalShort = 100; // every 100 docs
	private static final int updateIntervalLong = 1000; 
	private static final int updateThreshold = 1000;
	private static int finishedDocPairs = 0;
	private static int numAtReset = 0;

	// arg1: worker ID
	// arg2: 
	public static void main(String[] args) throws InterruptedException {

		try{ sendHeartBeat(); }catch(Exception e){};

		/* 1. read joblist
		 	1.1. for each pair:	
		 		retrieve texts from files
				- do srl
				- do annot
				- if both are ok: 
				   -- create result dir: en-<ID>_es_<ID>
				   -- write annot & srl xml files for both languages
				   -- extract graphs and write them to result dir
		 */	
		sendInf("starting.. english annotator");
		try {
			AnnotationService annotatorEN  = new AnnotationService(annotConfDir + "hub-template.xml",
					annotConfDir + "wikipedia-template-en.xml", "en", "en", "dbpedia");
			sendInf("en annotator ok");
		} catch (Exception e) {
			sendErr("annot-fail EN at init: " + e.getCause().getClass().getName() + " : " + e.getMessage());
			System.exit(1);
		}
		try{
			sendInf("starting spanish annotator");
			AnnotationService annotatorES = new AnnotationService(annotConfDir + "hub-template.xml",
					annotConfDir + "wikipedia-template-es.xml", "es", "en", "dbpedia");
			sendInf("es annotator ok");
		} catch (Exception e) {
			sendErr("annot-fail ES at init: " + e.getCause().getClass().getName() + " : " + e.getMessage());
			System.exit(1);
		}



		try {
			br = new BufferedReader(new FileReader(joblistFileName));
		} catch (FileNotFoundException e) {sendErr("FNFE: joblist file not found...exiting..."); System.exit(1);}

		String line = "";
		try{
			while((line = br.readLine()) != null){
				String[] parts = line.trim().split("\\t");
				jobList.add(new String[]{parts[0], parts[1]});
			}
			br.close();
			br = null; 		// for later reuse
		}catch(IOException ioe){sendErr("IOE: when reading joblist line..exiting.."); System.exit(1);}



		// main loop over all pairs
		for (String[] docIDPair : jobList){

			String[] text 		 = new String[2];
			String[] annotResult = new String[2]; 
			String[] srlResult	 = new String[2];


			// sub-loop over one pair
			boolean pairOkFlag = true;

			for(int i=0; i<2; i++){
				try{
					br = new BufferedReader(new FileReader(dataSourceDir + docIDPair[i]));
				}catch(FileNotFoundException e){ sendErr("FNFE: " + dataSourceDir + docIDPair[i] ); pairOkFlag = false; break;}	


				try {
					text[i] = br.readLine(); 	// text is always the first and only line in document
					br.close();
					br = null;
				}catch(IOException e){ sendErr("IOE: " + dataSourceDir + docIDPair[i]); pairOkFlag =false; break;}

				String lang = docIDPair[i].split("-")[0];

				/////////////////
				// 1. SRL
				srlResult[i] = doSRL(text[i], lang);

				if (srlResult[i] == null){
					sendErr("srl-fail in " + docIDPair[i]);
					pairOkFlag = false;
					break;
				}else{

					/////////////////
					// SRL ok --> continue to Annotation
					annotResult[i] = doAnnot(text[i], lang);

					if(annotResult[i] == null){
						sendErr("annot-fail in " + docIDPair[i]);
						pairOkFlag =false;
						break;
					}
				}
			} // for: sub-loop over one pair


			if(pairOkFlag == true){

				finishedDocPairs++;

				String resultDirName = resultDir + docIDPair[0] + "_" + docIDPair[1] + "/";
				(new File(resultDirName)).mkdir();

				/////////////////
				// save raw result xml
				for(int i=0; i<2; i++){
					try {
						bw = new BufferedWriter(new FileWriter(resultDirName + docIDPair[i] + "_srl.xml"));			
						bw.write(srlResult[i]);
						bw.close();

						bw = new BufferedWriter(new FileWriter(resultDirName + docIDPair[i] + "_annot.xml"));			
						bw.write(annotResult[i]);
						bw.close();
						bw = null;
					}catch(IOException ioe){
						sendErr("result-fail: " + docIDPair[i] + " IOE writing raw result file");
					}
				} // for: save results loop over one pair

				/////////////////
				// extract and save graphs
				for(int i=0; i<2; i++){
					
				} // for: extraxt graphs loop over one pair
			

		} // if: pair is ok (has both valid srl and annotation 

	} // for: main loop over all pairs

	sendInf("finished");




}


private static final void sendHeartBeat(){
	if (finishedDocPairs < updateThreshold){
		// short intervals
		if(numAtReset >= updateIntervalShort){
			numAtReset = 0;
			sendInf("UP: " + finishedDocPairs + " / " + jobList.size());

		}else{
			numAtReset++;
		}
	}else{
		// long intervals
		if(numAtReset >= updateIntervalLong){
			numAtReset = 0;
			sendInf("UP: " + finishedDocPairs + " / " + jobList.size());

		}else{
			numAtReset++;
		}
	}

}
private static final void sendInf(String logMessage){
	L.info("[" + wid + "] " + logMessage);
}
private static final void sendErr(String logMessage){
	L.error("[" + wid + "] " + logMessage);
}

private static final String doSRL(String text, String lang){

	String SRL_ENDPOINT = "";
	if(lang.equals("en")){
		SRL_ENDPOINT = "http://localhost:9090/axis2/services/analysis_en/analyze";
	}else if(lang.equals("es")){
		SRL_ENDPOINT = 	"http://localhost:8080/axis2/services/analysis_es/analyze";
	} else{
		sendErr("unknown language: " + lang);
		return null;
	}

	String CONTENT_TYPE = 		"text/xml";
	String CHARSET = 			"UTF-8";
	String DATA_FORMAT = 		"<analyze><text>{0}</text><target>relations</target></analyze>";

	// remove potentially messy characters
	////////////////////////////////////////////////////////////
	text = text.replace("<",  "").replace(">", "");
	//////////////////////////////////////////////////////////////

	String requestData = MessageFormat.format(DATA_FORMAT, text);
	// 3. call SRL service
	HttpURLConnection connection = null; 
	try {
		//Create connection
		URL url = new URL(SRL_ENDPOINT);
		connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", CONTENT_TYPE);
		connection.setRequestProperty("Content-Length", Integer.toString(requestData.getBytes().length));
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);


		//Send request
		DataOutputStream wr = new DataOutputStream (connection.getOutputStream());					
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, CHARSET));
		writer.write(requestData);
		writer.flush();
		writer.close();
		wr.close();					


		//Get Response	
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuffer response = new StringBuffer(); 
		while((line = rd.readLine()) != null) {
			response.append(line + "\n");
		}
		rd.close();

		// check for error message in SRL output
		if( ! response.toString().startsWith("<item>")){

			// sendErr("SRL-Fehler: \t "+ response.toString());
			return null;
		}else{

			// finish with correct srl xml
			return response.toString();		
		}

	}catch (Exception e) {
		sendErr("exception during http request handling: " + e.getCause().getClass().getName() + " : " + e.getMessage() + "\n\t\t...waiting for service restart...");
		// !!! wait two minutes for service restart 
		try {
			Thread.sleep(100000);
		} catch (InterruptedException e1) {
			sendErr("thread sleep interrupted");
		}
	} finally {
		if(connection != null)
			connection.disconnect(); 

	}
	return null;

}

private static final String doAnnot(String text, String lang){

	AnnotationService annotService = null;
	if(lang.equals("en") && annotatorEN != null){
		annotService = annotatorEN;	
	}else if(lang.equals("es") && annotatorES != null){
		annotService = annotatorES;
	} else{
		sendErr("unknown language: " + lang + " OR annotator is null");
		return null;
	}
	
	try{
		return annotService.process(text);
	}catch(Exception e){
		L.error("annot-fail when calling service", e);
		return null;
	}



}

private class AWSGraphDumper{
		
	

		
		
		
		
		/**
		 * @param shortDocKey
		 * @param annotPath
		 * @param L
		 * @return jdom2.Document of detected topics OR null if error
		 * @throws IOException 
		 * @throws JDOMException 
		 */
		private Document getAnnotationsJdom(String annotXmlString) throws IOException, JDOMException{
		
			SAXBuilder builder = new SAXBuilder();
			Document jdomDoc = null;
			
			try {
				jdomDoc = (Document) builder.build(new StringReader(annotXmlString));
				return jdomDoc;
			} catch (JDOMException e) {
				throw new JDOMException("JDOMException when parsing annot file");
			}
			
		}
		
		/**
		 * @param longDocKey
		 * @param L
		 * @return root Element (<item>)of srl xml OR null if error
		 * @throws IOException 
		 * @throws MongoException, JDOMException 
		 */
		private Document getSrl(String srlXmlString) throws MongoException, JDOMException, IOException{
			

				SAXBuilder builder = new SAXBuilder();
				Document jdomDoc = (Document) builder.build(new StringReader(srlXmlString));
				
				return jdomDoc;
			
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
		
	
		
		public void runAlgo(String docID, String srlXml, String annotXml, String resultSubDir, Logger L) {
	
			
			try{
				getCatsMap(L);
			}catch(Exception e){
				WorkerAWS.sendErr("Ex when deserializing Entity-Category map from " + WorkerAWS.catsMapFilePath);
				System.exit(1);
			}
		
			try{

				// 1. get index of detected topics (entities)
				EntityIndex annotationsIndex = null;
				try {
					annotationsIndex = new EntityIndex(getAnnotationsJdom(annotXml));
				} catch (Exception e1) {
					L.error("caught from getAnnotationsJdom() : ", e1);
					return;
				}

				Document srlJdomDoc = null;
				try {
					// 2. get srl xml
					srlJdomDoc = getSrl(srlXml);
				} catch(Exception e){
					L.error(" srl retrieval caused exception: ", e);
					return;
				}

				// 3. extract graph from srl
				// cumulated text length is computed in extractGraph function

				TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> sentenceGraphSetMap = null;

				try {
					sentenceGraphSetMap = composeAnnotatedGraphs(docID, srlJdomDoc, annotationsIndex);
				} catch (Exception e) {
					WorkerAWS.sendErr("graph-fail: <" + docID + "> exception composing graph list for sentence: " + e.getMessage() );
					return;
				}

				// 4. serialize result to file

				FileOutputStream f_out = null;
				ObjectOutputStream obj_out = null;
				try {
					f_out = new FileOutputStream(resultSubDir + docID + ".graph");
					obj_out = new ObjectOutputStream (f_out);
					obj_out.writeObject(sentenceGraphSetMap);
					obj_out.close();
				} catch (Exception e) {
					WorkerAWS.sendErr("graph-fail: <" + docID +"> file not found exception when writing result file: " + e.getMessage());
					return;
				} finally{
					try {
						obj_out.close();
					} catch (IOException e) {
						WorkerAWS.sendErr("graph-fail: <" + docID +"> io exception closing object output stream");
					}
				}
			}catch(Exception e){
				WorkerAWS.sendErr("<" + docID + ">\t crashed");
			}

	
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
//				System.out.println("sent " + i + " = " + cumulTextLength[i]);
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
//			System.out.println(from + "-" + to +" = " + globalFrom + "-" + globalTo);
			
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
//			System.out.println("sentID: " + sentenceId + " , frameID " + topLevelFrame.getAttributeValue("id"));
			
			
			XPathFactory xpf = XPathFactory.instance();
			XPathExpression<Element> expr = null;
			
			List<Element> allArguments = parentFrame.getChildren("argument");
//			int totalArgs = arguments.size();
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
//					frameArguments.add(arg);
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

		
		private void getCatsMap(Logger L) throws IOException, ClassNotFoundException {
			if (WorkerAWS.cats == null){
				L.info("DBPedia categories map null. Initializing...");

				FileInputStream fis = new FileInputStream(new File(WorkerAWS.catsMapFilePath));
				ObjectInputStream ois = new ObjectInputStream(fis);

				cats = (TreeMap<String, ArrayList<String>>) ois.readObject();

			}

		}

}
