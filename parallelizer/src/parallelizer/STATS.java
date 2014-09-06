package parallelizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;

import algo.XmlToGraph;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import graph.Predicate;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graph.Argument;
import graph.Node;
import graph.Ref;
import graph.Role;


public class STATS {
	private final static Logger L = Logger.getLogger(Coordinator.class); 
	private static String 	mongoAddr = System.getProperty("mongo.addr");
	private static int 	mongoPort = Integer.parseInt(System.getProperty("mongo.port"));
	private static String 	mongoUser = System.getProperty("mongo.user");
	private static String 	mongoPass = System.getProperty("mongo.pwd");
	private static String 	mongoDBName = System.getProperty("mongo.db.name");
	
	private static String COLL_NAME = "srl";
	
	
	public static void main(String[] args) throws IOException, JDOMException{

		// singleton mongoDB client for all threads
		MongoClient 	mongoClient = null;
		DB 				db = null;
		DBCollection 	collFrom = null;
		DBCollection 	collTo = null;



		mongoClient = new MongoClient( mongoAddr , mongoPort );
		//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
		db = mongoClient.getDB(mongoDBName);
		db.authenticate(mongoUser, mongoPass.toCharArray());


//		collFrom = db.getCollection("docs");
		collTo = db.getCollection(COLL_NAME);





	

		DBCursor res = collTo.find();
		int dbSize = res.size();
		ArrayList<DirectedSparseMultigraph<Argument,Role>> allGraphs = new ArrayList<DirectedSparseMultigraph<Argument,Role>>();

		int processedOk = 0;
		
		L.info("articles to process: " +  dbSize);
		L.info("starting main loop");
		
		for(DBObject doc : res){
			
			
			String srlXml = doc.get("srlAnnot").toString();

			
			
			if( srlXml.startsWith("<item>")){
				try{
					allGraphs.addAll(XmlToGraph.composeGraph(srlXml, false));
					processedOk ++;
				}catch(Exception e){
					L.debug(doc.get("_id") + " xml ex: " + e.getMessage());
				}


			}else{

			}

		}
		
		L.info("main loop finished" );
		L.info("starting stats calculation main loop on " + allGraphs.size() +  " graphs");
		
		// RESULTS
		long sentsTotal = allGraphs.size();
		
		
		
		L.info("########## Number of Sentences total:\t\t"+ sentsTotal);
		L.info("########## Number of Docs processed: \t\t" + processedOk);
		L.info("########## Total articles in mongoDB \t\t" + dbSize);
		
		
		// count vertives and edges
		long sumPreds = 0;
		long sumNodes = 0;
		long sumEdges = 0;
		long sumRefs = 0;
		

		
		TreeMap<String, Integer> roles = new TreeMap<String, Integer>();
		TreeMap<String, Integer> roots = new TreeMap<String, Integer>();
		TreeMap<String, Integer> types = new TreeMap<String, Integer>();
		TreeMap<String, Integer> dbs = new TreeMap<String, Integer>();
		
		for(DirectedSparseMultigraph<Argument,Role> g : allGraphs){
						
			sumEdges += g.getEdgeCount();
			
			for (Role r : g.getEdges()){
				
				// build roles dictionary
				String rN = r.getRole();
				if(roles.containsKey(rN)){
					roles.put(rN, roles.get(rN) + 1);
				}else{
					roles.put(rN, 1);
				}
			}
			
			for(Object arg : g.getVertices()){
				
				
//L.info("\t" + ((Argument)arg).getDisplayName());
				
				if( arg instanceof Predicate){
//System.err.println("p " + ((Argument)arg).getDisplayName());
					sumPreds ++;
					
					if (((Argument)arg).isRoot()){
						String rootName = ((Argument)arg).getDisplayName();
						if(roots.containsKey(rootName)){
							roots.put(rootName, roots.get(rootName) + 1);
						}else{
							roots.put(rootName, 1);
						}
					}
				}else {
					sumNodes ++;
//System.err.println("n " + ((Argument)arg).getDisplayName());					
					String type = ((Node)arg).getType();
//L.info(type);
					if(types.containsKey(type)){
						types.put(type, types.get(type) + 1);
					}else{
						types.put(type, 1);
					}
					
					
					
					List<Ref> kbRefs= ((Node)arg).getKBRefs();
					for(Ref ref : kbRefs){
						sumRefs ++;
						
						String refN = ref.getKnowledgeBase();
						if(dbs.containsKey(refN)){
							dbs.put(refN,  dbs.get(refN) + 1);
						}else{
							dbs.put(refN,  1);
						}
					}
					
				}
			}
		}
		
		
		
		
		
		L.info("stats calculation finished");
		L.info("=======================Number of Edges: \t" + sumEdges);
		L.info("=======================Number of Nodes: \t" + sumNodes);
		L.info("=======================Number of Predicates: \t" + sumPreds);
		
		L.info("=======================ROLE DICTIONARY=======================total: " + roles.size());
		L.debug("ROLES: " + roles);
//		for(String r : roles.keySet()){
//			L.info(r + "\t\t" + roles.get(r));
//		}
		
		L.info("------------ Number of Node-References to KBs: \t" + sumRefs);
		L.info("------------ Number of distinct KBs: " + dbs.size());
		L.debug("KBs: " + dbs);
		L.info("++++++++++++ Number of distinct Node types: \t" + types.size());
		L.debug("TYPES: " + types);
		L.info("*************** Sentence-Roots:");
		L.info("ROOTS: " + roots);




		//		BasicDBObject docApple, docTesla;

		//		doc = new BasicDBObject("_id", "id123")	// dictionary: [id, url, title, lang, proj]
		//		.append("lang", "en")
		//		.append("project", "wiki");
		//		L.info("original" + doc.toString());
		//		doc.removeField("lang");
		//		L.info("after remove" + doc.toString());
		//
		//		BasicDBObject copy = doc;
		//		L.info("copy" + copy.toString());

		//		String tesla ="Tesla Motors announces that it will allow competitors to use its patents without paying royalties.";
		//		String apple ="Apple was founded by Steve Jobs, Steve Wozniak, and Ronald Wayne on April 1, 1976, to develop and sell personal computers.";
		//		docApple = new BasicDBObject("_id","tesla")
		//					.append("lang", "en").append("project", "wikinews").append("docTitle", "tesla news").append("docId", "teslaID")
		//					.append("srlAnnot", new CallSRL().makeCall(tesla));
		//		docTesla = new BasicDBObject("_id","apple")
		//					.append("lang", "en").append("project", "wikinews").append("docTitle", "apple news").append("docId", "appleID")
		//					.append("srlAnnot", new CallSRL().makeCall(apple));	
		//		L.info("apple inserted " + collTo.insert(docApple));
		//		L.info("tesla inserted " + collTo.insert(docTesla));


		//		DBCursor res = collTo.find(new BasicDBObject("_id", "http://en.wikinews.org/wiki?curid=926682"));	














		//		for(DBObject d : res){
		//			   L.info("----");
		//		       L.info(d.get("_id") + " -- " + d.get("srlAnnot").toString());
		//		}

		//		XMLParsing x = new XMLParsing();
		////		x.parseXML(res.next().get("srlAnnot").toString());
		//		String xml = new parallelizer.CallSRL().makeCall("The library is full of people. Obama besame president.");
		//		L.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" + xml + "\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		//		x.parseXML(xml);
		//		String s ="";
		//		for (NodeInSRL n : x.getListOfNodes()) {		
		//						s += "displayname :\t" + n.getDisplayName() + "\t\t";
		//						if (n.hasKBlink("DBpedia")) {
		//							s += n.getKBlink("DBpedia").getUri();
		//						}
		//						s += "\n";	
		//		}
		//		
		//		
		//		L.info(s);
		//
		//		System.out.print("frames: ");
		//		for (NodeInSRL node : x.getListOfNodes()) {
		//			System.out.print(node.getDisplayName() + " ");
		//		}
		//		L.info();
		//		L.info("np: " + x.getListOfNounPhrases());
		//		L.info("nodes: " + x.getListOfNodes());
		//		L.info("frames: " + x.getListOfFrames());
		//		L.info("hi");


	}


}
