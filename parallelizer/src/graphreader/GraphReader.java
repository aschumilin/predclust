package graphreader;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;

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

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import graph.Argument;
import graph.Role;
import graph.SimilarityMeasure;
public class GraphReader {

	/////////////////////////////////
	// set this password
	private static String mongoPass = "";
	/////////////////////////////////


	private static String mongoAddr = "172.22.204.184";
	private static int mongoPort = 22222;
	private static String mongoDBName = "all";
	private static String collName = "srl";
	private static String mongoUser = "reader";


	public static void main(String[] args) {

//					String[] ids = new String[] {"en-26221135.graph","en-63876.graph","en-690842.graph","es-14819.graph","es-54595.graph"};
//					readAndVisualizeGraphs("/home/pilatus/Desktop/annots-test2/en-8556.graph");
//					String exampleSentenceKey = "en-26221135.xml-57";
//					visualizeGraph(readGraphsFromFile("/home/pilatus/Desktop/annot-test/res/en-26221135.graph").get(exampleSentenceKey).get(0), "cults banned");
//					TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> tempMap = readGraphsFromFile("/home/pilatus/Desktop/annots-test2/en-26221135.graph");
////					visualizeGraph(tempMap.get("en-8556.xml-1").get(0), "1");
//		for(String key : tempMap.keySet()){
//			for(DirectedSparseGraph<Argument, Role> g : tempMap.get(key)){
////				double sim =SimilarityMeasure.m8OutRoleLabelsAndNodes(tempMap.get("en-8556.xml-1").get(0), g, SimilarityMeasure.getRoot(tempMap.get("en-8556.xml-1").get(0)), SimilarityMeasure.getRoot(g));
//				
//				double sim =SimilarityMeasure.m8OutRoleLabelsAndNodes(tempMap.get("en-26221135.xml-1").get(0), g, SimilarityMeasure.getRoot(tempMap.get("en-26221135.xml-1").get(0)), SimilarityMeasure.getRoot(g));
//				if(sim>0) {
////					System.out.println("1 and "+key +" : " + sim);
//					visualizeGraph(g, key +  " / sim: " + sim);
//				}
//			}
//		}
		for (File f : new File("/home/pilatus/Desktop/sim-test/").listFiles()){
			if(f.getAbsolutePath().contains(".graph")) visualizeGraph(readOneGraphFromFile(f.getAbsolutePath()), f.getName());
		}
		
		
		DirectedSparseGraph<Argument, Role> g1 = readOneGraphFromFile("/home/pilatus/Desktop/sim-test/en-1110102.xml-18-0.graph");
				DirectedSparseGraph<Argument, Role> g2 = readOneGraphFromFile("/home/pilatus/Desktop/sim-test/es-589573.xml-44-0.graph");
				Argument root1 = SimilarityMeasure.getRoot(g1);
		Argument root2 = SimilarityMeasure.getRoot(g2);
		
		System.out.println("1-2: " + SimilarityMeasure.m6OutRoleLabels(g1, g2, root1, root2));
		System.out.println("---------");
		System.out.println("2-1: " + SimilarityMeasure.m6OutRoleLabels(g2, g1, root2, root1));
		
		System.out.println();
		System.out.println("1-2" + SimilarityMeasure.genericJaccard(new ArrayList<String>(Arrays.asList("a", "b", "c")), new ArrayList<String>(Arrays.asList("d", "c"))));
		System.out.println("---------");
		System.out.println("2-1" + SimilarityMeasure.genericJaccard(new ArrayList<String>(Arrays.asList("d", "c")), new ArrayList<String>(Arrays.asList("a", "b", "c"))));

		
					//			visualizeGraph(tempMap.get("es-14819.xml-125").get(0), "2");
					
	
		
		/*
		GraphReader gr = new GraphReader();
		TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> tempMap = null;
		List<DirectedSparseGraph<Argument, Role>> 					tempGList = null;
		String 														tempUri = null;
		String 														tempGraphFileName = null;
		String 														tempRoleString = null;
		ArrayList<Ref> 												tempRefList = null;

		// counting nodes 
		int numGEs=0, numGEn=0; // num graphs counter
		int numPEs=0, numPEn=0;	// num predicates counter
		int numNEs=0, numNEn=0; // num nodes counter
		int numNEsAnnotated=0, numNEnAnnotated=0;	//num of nodes with WordNet or DBpedia annotation
		int numSentEn=0, numSentEs=0;	// num sentences counter

		Counter entsEn = gr.new Counter();
		Counter entsEs = gr.new Counter();
		Counter entsAll = gr.new Counter();

		String baseResultDir = args[0]; // /home/pilatus/Desktop/annot-test/  /home/pilatus/Desktop/annot-test/graphs/
		File resultEntsEN = new File(baseResultDir + "v2-en-HashMap-String-intArr");
		File resultEntsES = new File(baseResultDir + "v2-es-HashMap-String-intArr");
		File resultEntsAll = new File(baseResultDir +"v2-all-HashMap-String-intArr");

		// counting edges 
		Counter edgesEn = gr.new Counter();
		Counter edgesEs = gr.new Counter();
		Counter edgesAll = gr.new Counter();

		File resultEdgesEN = new File(baseResultDir + "v2-en-EDGES-HashMap-String-intArr");
		File resultEdgesES = new File(baseResultDir + "v2-es-EDGES-HashMap-String-intArr");

		
		
		// my logger
		PrintWriter L = null;
		try {
			L = new PrintWriter(new File(baseResultDir + "stats.log"));
		} catch (IOException e1) {e1.printStackTrace();}


		String sourceDir = args[1];
		File graphsDir = new File(sourceDir);




		long workersStarted = 0;
		long totalTasks = 361972;
		long interval = totalTasks / 100; // Schrittweite
		try{
			for (File f : graphsDir.listFiles()){

				// print progress info
				workersStarted ++;
				try{
					if(workersStarted % interval == 0)
						System.out.println("PROGRESS: " + (100.0 * workersStarted / totalTasks) +  " %");
				}catch(Exception ae){System.out.println("error in progress printer");}


				tempGraphFileName = f.getName();
				tempMap = readGraphsFromFile(f.getAbsolutePath());

				if (tempGraphFileName.startsWith("es")){		// count spanish


					for (String key : tempMap.keySet()){	// iterate over each sentence in map
						numSentEs ++;

						tempGList = tempMap.get(key);

						for (DirectedSparseGraph<Argument, Role> g : tempGList){	// iterate over each graph in sentence

							numGEs ++;

							// Predicates and NODES ES
							for (Argument arg : g.getVertices()){
								if(! arg.isPredicate()){
									numNEs ++;
									tempRefList = arg.getRefs();
									if(tempRefList.size()>0){
										numNEsAnnotated ++;
										tempUri = tempRefList.get(0).getURI();
										entsEs.add(tempUri);
										entsAll.add(tempUri);
									}
									
								}else{
									numPEs ++;
								}
								
							}
							
							
							// EDGES ES

							for (Role role : g.getEdges()){

								tempRoleString = role.getRole();
								edgesEs.add(tempRoleString);
								edgesAll.add(tempRoleString);

							}
						}
					}
				}else if (tempGraphFileName.startsWith("en")){	// count english

					for (String key : tempMap.keySet()){	// iterate over each sentence in map

						numSentEn ++;

						tempGList = tempMap.get(key);

						for (DirectedSparseGraph<Argument, Role> g : tempGList){	// iterate over each graph in sentence

							numGEn ++;
							
							// Predicates and NODES EN
							for (Argument arg : g.getVertices()){
								if (!arg.isPredicate()){
									numNEn ++;
									tempRefList = arg.getRefs();
									if(tempRefList.size()>0){
										numNEnAnnotated ++;
										tempUri = tempRefList.get(0).getURI();
										entsEn.add(tempUri);
										entsAll.add(tempUri);
									}
								}else{
									numPEn ++;
								}
							}
							
							// EDGES EN
							for (Role role : g.getEdges()){
								tempRoleString = role.getRole();
								edgesEn.add(tempRoleString);
								edgesAll.add(tempRoleString);

							}
						}
					}
				}else{
					L.println("neither EN, nor ES..??");
				}
			}
		}catch(Exception e){
			L.println("Error in \t" + tempGraphFileName + "\t :" + e.getMessage());
			e.printStackTrace();
		}

		L.println("num EN sentences: " + numSentEn);		
		L.println("num EN graphs: " + numGEn);
		L.println("num EN predicates: " + numPEn);
		L.println("num EN nodes: " + numNEn);
		L.println("num EN annotated nodes: " + numNEnAnnotated);
		L.println("distinct EN ents: " + entsEn.getMap().keySet().size());
		L.println("distinct EN roles: " + edgesEn.getMap().keySet().size());

		L.println("num ES sentences: " + numSentEs);		
		L.println("num ES graphs: " + numGEs);
		L.println("num ES predicates: " + numPEs);
		L.println("num ES nodes: " + numNEs);
		L.println("num ES annotated nodes: " + numNEsAnnotated);
		L.println("distinct ES ents: " + entsEs.getMap().keySet().size());
		L.println("distinct ES roles: " + edgesEs.getMap().keySet().size());

		
		L.println("distinct ALL ents: " + entsAll.getMap().keySet().size());
		L.println("distinct ALL roles: " + edgesAll.getMap().keySet().size());


		L.close();


		try {
			FileOutputStream fos = new FileOutputStream(resultEntsEN);
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(entsEn.getMap());

			oos.close();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			FileOutputStream fos = new FileOutputStream(resultEntsES);
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(entsEs.getMap());

			oos.close();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			FileOutputStream fos = new FileOutputStream(resultEntsAll);
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(entsAll.getMap());

			oos.close();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		
		try {
			PrintWriter pw = new PrintWriter(resultEdgesEN);
			for (String role : edgesEn.getMap().keySet()){
				pw.println(role + "," + edgesEn.getMap().get(role)[0]);
			}

			pw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			PrintWriter pw = new PrintWriter(resultEdgesES);
			for (String role : edgesEs.getMap().keySet()){
				pw.println(role + "," + edgesEs.getMap().get(role)[0]);
			}

			pw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
*/
	}


	public static TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> readGraphsFromFile(String fullFilePath){

		File file = new File(fullFilePath);

		TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> graphsMap = null;
		try{
			// don't use buffering
			InputStream fis = new FileInputStream(file);
			ObjectInput input = new ObjectInputStream(fis);//buffer);

			graphsMap = 
					(TreeMap<String, List<DirectedSparseGraph<Argument, Role>>>)
					input.readObject();	
			input.close();
			fis.close();
		}catch(Exception e){
			System.out.println("could not deserialize annotations map: ");
			e.printStackTrace();
		}

		return graphsMap;


	}
	
	public static DirectedSparseGraph<Argument, Role> readOneGraphFromFile(String fullFilePath){

		File file = new File(fullFilePath);

		DirectedSparseGraph<Argument, Role> graph = null;
		try{
			// don't use buffering
			InputStream fis = new FileInputStream(file);
			ObjectInput input = new ObjectInputStream(fis);//buffer);

			graph = 
					(DirectedSparseGraph<Argument, Role>)input.readObject();	
			input.close();
			fis.close();
		}catch(Exception e){
			System.out.println("could not deserialize graph object from file: " + fullFilePath);
			e.printStackTrace();
		}

		return graph;


	}

	private static String makeLongKey(String graphFileName){		
		File file = new File(graphFileName);		
		System.out.println(file.getPath());
		String[] parts = file.getName().split("-");
		return "http://" + parts[0] + ".wikipedia.org/wiki?curid=" + parts[1].substring(0,parts[1].length()-6);
	}


	private static String getSentence(String docId, String sentenceId){


		MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient( mongoAddr , mongoPort );
		} catch (UnknownHostException e) {
			System.out.println("unknown host exception getting mongo db client");
			e.printStackTrace();
		}
		//			mongoClient.setWriteConcern(WriteConcern.JOURNALED);
		DB db = mongoClient.getDB(mongoDBName);
		boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
		if( ! auth ){
			System.out.println("could not authenticate mongo db access");
		}else{

			DBCollection collSrl = db.getCollection(collName);

			BasicDBObject query = new BasicDBObject("_id", docId);	
			DBCursor dbResult = null;

			dbResult = collSrl.find(query).limit(1);

			if(dbResult.size() == 0){
				System.out.println("");
				return null;
			}else{

				BasicDBObject dbDoc = (BasicDBObject)dbResult.next();
				SAXBuilder builder = new SAXBuilder();
				String srlString = dbDoc.get("srlAnnot").toString();
				Document jdomDoc = null;
				try {
					jdomDoc = (Document) builder.build(new StringReader(srlString));
				} catch (JDOMException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
				XPathFactory xpf = XPathFactory.instance();
				XPathExpression<Element> expr = xpf.compile("/item/sentences/sentence[@id='" + sentenceId + "']/text", Filters.element());
				Element sentText = expr.evaluateFirst(jdomDoc);
				return sentText.getText();
			}


		}


		return null;

	}

	
	public static void readAndVisualizeGraphs(String graphFilePath) {
		readGraphsFromFile(graphFilePath);
		String longKey = makeLongKey(graphFilePath);

		TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> graphsMap = readGraphsFromFile(graphFilePath);
		for (String s : graphsMap.keySet()){
			List<DirectedSparseGraph<Argument, Role>> l = graphsMap.get(s);
			for (DirectedSparseGraph g : l){
				visualizeGraph(g, s + ": " +getSentence(longKey, s.split("-")[2]));
			}
		}
	}



	private static void visualizeGraph(Graph g, String sentence){
		Layout<Argument, Role> layout = new CircleLayout(g);
		layout.setSize(new Dimension(300,300));
		VisualizationViewer<Argument, Role> vv = new VisualizationViewer<Argument, Role>(layout);
		vv.setPreferredSize(new Dimension(700, 500));
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());

		// Create our "custom" mouse here. We start with a PluggableGraphMouse
		// Then add the plugins you desire.
		PluggableGraphMouse gm = new PluggableGraphMouse(); 
		gm.add(new TranslatingGraphMousePlugin(MouseEvent.BUTTON1_MASK));
		gm.add(new ScalingGraphMousePlugin(new CrossoverScalingControl(), 0, 1.1f, 0.9f));
		gm.add(new PickingGraphMousePlugin<Argument, Role>(MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON2_MASK));

		vv.setGraphMouse(gm); 
		JFrame frame = new JFrame(sentence);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(vv);
		frame.pack();
		frame.setVisible(true); 
	}


	private class Counter implements Serializable{

		private static final long serialVersionUID = 1112313123L;
		HashMap<String, int[]> counter = null;	

		public Counter(){
			counter = new HashMap<String, int[]>();
		}


		public HashMap<String, int[]> getMap(){
			return counter;
		}

		public void add(String key){

			int[] valueWrapper = counter.get(key);

			if (valueWrapper == null) {
				counter.put(key, new int[] { 1 });
			} else {
				valueWrapper[0]++;
			}

		}
	}
}
