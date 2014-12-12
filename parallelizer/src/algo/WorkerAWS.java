package algo;

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
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

import parallelizer.AnnotServicePool;
import parallelizer.Parallelizable;
import parallelizer.Worker;

import annotator.AnnotationService;
import annotator.EntityIndex;

import com.mongodb.MongoException;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Node;
import graph.Predicate;
import graph.Ref;
import graph.Role;


public class WorkerAWS extends Parallelizable {


	// DBpedia categories
	private static TreeMap<String, ArrayList<String>> cats = null; 	

	private static final String wid		= System.getProperty("workerid");	

	private static final String dataSourceDir	= System.getProperty("datadir");
	private static final String annotConfDir	= System.getProperty("annotconf");
	private static String catsMapFilePath 		= System.getProperty("catsmappath");

	
	private static AnnotServicePool annotatorPool = null;
	private static int numAnnotServices = Integer.parseInt(System.getProperty("annots.num"));
	
	private static final String P = "[" + wid + "] ";

	// docKey zB: /home/user/12-result/en-123_es-456/en-123...
	@Override
	public void runAlgo(String jobDescr, Logger L) {

//L.warn("<<<<<<<<<< in worker run algo");

		String[] parts = jobDescr.split("/");
		String docID = parts[parts.length -1];
		String[] docIDPair = new String[]{docID};
		int i = 0;

		/* 1. read joblist
		 	1.1. for each pair:	
		 		retrieve texts from files
				- do srl
				- do annot
				- if both are ok: 
				   -- create result dir: en-<ID>_es_<ID>
				   -- write annot & srl xml files for both languages
				   -- extract graphs and write them to result dir
				   
		save one file per single graph: 
		<lang>-<articleID>-<sentenceID>-<numOfGraphInSentence>
		directory structure: 
		en-<idEN>_es-<idES> / _srl.xml + _annot.xml + single graphs
		 */	
	
		getAnnotator(numAnnotServices, annotConfDir, L);
		
		
		try{
			getCatsMap(L);
		}catch(Exception e){
			L.fatal("Exception when deserializing Entity-Category map from " + catsMapFilePath);
			L.fatal("exiting now ...");
			System.exit(1);
		}
		
		
		
		String text = "";
		String annotResult = ""; 
		String srlResult = "";
		boolean docOkFlag = true;

		BufferedWriter bw = null; 
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(dataSourceDir + docIDPair[i]));
		}catch(FileNotFoundException e){ L.error(P + "FNFE: " + dataSourceDir + docIDPair[i] ); docOkFlag = false; Worker.decrBusyWorkers();return;}	

//L.warn("########## buffered reader ok");

		try {
			text = br.readLine(); 	// text is always the first and only line in document
			br.close();
			br = null;
		}catch(IOException e){ L.error(P + "IOE: " + dataSourceDir + docIDPair[i]); docOkFlag =false; Worker.decrBusyWorkers();return;}
		
		
//L.warn("******* text read ok");
		
				
		String lang = docIDPair[i].split("-")[0];

		/////////////////
		// 1. SRL
		srlResult = doSRL(text, lang, L);

//L.warn("******* got srl result");		
		
		if (srlResult == null){
			L.error(P + "srl-fail in " + docIDPair[i]);
			docOkFlag = false;
			Worker.decrBusyWorkers();
			return;
		}else{

			/////////////////
			// SRL ok --> continue to Annotation
			// call annotation service
			
			
			Object[] result = null;
			while ((result = annotatorPool.getFreeService(lang, docID)) == null){
				Thread.yield();
			}		
			String serviceId =(String) result[1];
			AnnotationService serv = (AnnotationService) result[0];
			try {
				annotResult = serv.process(text);
				annotatorPool.setServiceFree(serviceId);
			} catch (Exception e) {
				// RESET BUSY FLAG AFTER PROCESSING
				annotatorPool.setServiceFree(serviceId);
				L.error(P + "<" + docID + ">\t exception during annotatorPool.process()", e);
				Worker.decrBusyWorkers();
				return;
			}

			if(annotResult == null){

				L.error(P + "annot-fail in " + docIDPair[i]);
				docOkFlag =false;
			}
		}


		if(docOkFlag == true){


			/////////////////
			// save raw result xml

			try {
				bw = new BufferedWriter(new FileWriter(jobDescr  + "_srl.xml"));			
				bw.write(srlResult);
				bw.close();

				bw = new BufferedWriter(new FileWriter(jobDescr + "_annot.xml"));			
				bw.write(annotResult);
				bw.close();
				bw = null;
			}catch(IOException ioe){
				L.error(P + "result-fail: " + docIDPair[i] + " IOE writing raw result file");
			}

			/////////////////
			// extract and save graphs (one file per single graph: <lang>-<articleID>-<sentenceID>-<numOfGraphInSentence>)
			try{
				AWSGraphDumper graphDumper = (new WorkerAWS()).new AWSGraphDumper();
				String resultSubDir = jobDescr.substring(0, jobDescr.length() - docID.length());
				graphDumper.runAlgo(docIDPair[i], srlResult,annotResult , resultSubDir, L);
			} catch(Exception e){
				L.error(P + "graph-fail: caught in WorkerAWS.main \n", e);
			}


		} // if: pair is ok (has both valid srl and annotation)
		Worker.decrBusyWorkers();

	}

	@Override
	public void cleanUpFinally(Logger L){
		L.info("[" + wid + "] finished");
	}


	private static final String doSRL(String text, String lang, Logger L){
//L.warn("---in doSRL");
		
		String SRL_ENDPOINT = "";
		if(lang.equals("en")){
			SRL_ENDPOINT = "http://localhost:9090/axis2/services/analysis_en/analyze";
		}else if(lang.equals("es")){
			SRL_ENDPOINT = 	"http://localhost:8080/axis2/services/analysis_es/analyze";
		} else{
			L.error(P+ "unknown language: " + lang);
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
			
//L.warn("-------srl request sent");

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
				return null;
			}else{

				// finish with correct srl xml
//L.warn("--------srl successful");
				return response.toString();		
			}

		}catch (Exception e) {
			L.error(P+ "srl-fail: exception during http request handling: \n\t\t...waiting for service restart...", e);
			// !!! wait two minutes for service restart 
			try {
				Thread.sleep(100000);
			} catch (InterruptedException e1) {
				L.error(P+ "thread sleep interrupted");
			}
		} finally {
			if(connection != null)
				connection.disconnect(); 
		}
		return null;
	}

	
	/**
	 * 
	 * Uses the getBestEXACTAnnotation  !!!
	 * @author pilatus
	 *
	 */
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

//L.warn("............. extracting graphs");

		



			// 1. get index of detected topics (entities)
			EntityIndex annotationsIndex = null;
			try {
				annotationsIndex = new EntityIndex(getAnnotationsJdom(annotXml));
			} catch (Exception e1) {
				L.error(P+ "caught from getAnnotationsJdom() : " + e1.getMessage(), e1);
				return;
			}

			Document srlJdomDoc = null;
			try {
				// 2. get srl xml
				srlJdomDoc = getSrl(srlXml);
			} catch(Exception e){
				L.error(P+ "srl to jdom doc caused exception: " +  e.getMessage());
				return;
			}

			// 3. extract graph from srl
			// cumulated text length is computed in extractGraph function

			TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> sentenceGraphSetMap = null;

			

			try {
				sentenceGraphSetMap = composeAnnotatedGraphs(docID, srlJdomDoc, annotationsIndex);
			} catch (Exception e) {
				L.error(P+ "graph-fail: <" + docID + "> exception composing graphs for article. " + e.getMessage() );
				return;
			}

			// 4. serialize result to file

			FileOutputStream f_out = null;
			ObjectOutputStream obj_out = null;
			for (String docSentenceKey : sentenceGraphSetMap.keySet()){
				List<DirectedSparseGraph<Argument, Role>> temp = sentenceGraphSetMap.get(docSentenceKey);

				// write each single graph to separate file
				int j = 1;
				for (DirectedSparseGraph<Argument, Role> singleGraph : temp){
					try {

						f_out = new FileOutputStream(resultSubDir + docSentenceKey + "-" + j + ".graph");					
						obj_out = new ObjectOutputStream (f_out);
						obj_out.writeObject(singleGraph);
						j++;
						obj_out.close();

					} catch (Exception e) {
						L.error(P+ "graph-fail: <" + docID +"> file not found exception when writing result file: " + e.getMessage());
						continue;
					} finally{
						try {
							obj_out.close();
						} catch (IOException e) {
							L.error(P+ "graph-fail: <" + docID +"> io exception closing object output stream");
						}
					}
				}
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
			String[] bestNodeAnnotation = entIndex.getBestEXACTAnnotation(mentionCoordinates);

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

		/**
		 * Special treatment for the question frames from qald-4 training data.
		 * Frame is valid even if it has 0 valid nodes.
		 * @param topLevelPredicate
		 * @param framesToDoMap
		 * @param parentFrame
		 * @param sentenceId
		 * @param srlDoc
		 * @param entIndex
		 * @param cumulTextLength
		 * @return
		 */
		private List<Object[]> validateQALDFrame(Predicate topLevelPredicate, TreeMap<String, Boolean> framesToDoMap, Element parentFrame, String sentenceId, Document srlDoc, EntityIndex entIndex, int[] cumulTextLength){
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
			
			//
			//
			//
			//frame is fine without arguments
			//
			//
			//
			
			if(numValidNonPredicateArgs >= 0){

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
						System.out.println("stack overflow error in composeAnnotatedGraph()");	
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
	
	@SuppressWarnings("unchecked")
	private static synchronized void getCatsMap(Logger L) throws IOException, ClassNotFoundException {
		if (cats == null){
			L.info(P + "DBPedia categories map null. Initializing...");

			FileInputStream fis = new FileInputStream(new File(catsMapFilePath));
			ObjectInputStream ois = new ObjectInputStream(fis);

			cats = (TreeMap<String, ArrayList<String>>) ois.readObject();
			ois.close();
			L.info(P + "done cats map init.");
		}

	}

}