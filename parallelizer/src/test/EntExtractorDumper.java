package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Ref;
import graph.Role;

public class EntExtractorDumper {

	static String graphsBaseDir = "/home/pilatus/WORK/pred-clust/data/java-longarticles-25k/";
	static String entsTargetDir = "/home/pilatus/WORK/pred-clust/data/entities-longarticles-25k/";
	
	public static void main(String[] args) throws IOException {

		DirectedSparseGraph<Argument, Role> tempGraph = null;
		
		StringBuffer tempResultBuffer = new StringBuffer();
		
		BufferedWriter bw = null;
		
		for (String graphFileName : new File(graphsBaseDir).list()){
			
			System.out.println(graphFileName);
			
			tempGraph = graphreader.GraphReader.readOneGraphFromFile(graphsBaseDir + graphFileName);
			
//			// print json graphs to file
//			String jsonGraphPath = "/home/pilatus/WORK/pred-clust/data/25k-shorts-json/" + graphFileName.replace("graph", "json");
//			util.Funcs.stringToFile(jsonGraphPath, graph.GraphToJson.makeJson(graph.SimilarityMeasure.getRoot(tempGraph), tempGraph).toString());
//			System.out.println(graphFileName);	
//			if(true) continue;
			
			// 1. read all entity URIs and concat them in a String
			// 2. dump result string to result file
			
			String uri 			= null;
			String weight		= null;
			for (Argument arg : tempGraph.getVertices()){
				List<Ref> tempRefs = arg.getRefs();  
				
				if (! arg.isPredicate() && ! tempRefs.isEmpty()){
					
					for (Ref ref : tempRefs){
						
						if (ref.getKnowledgeBase().toLowerCase().startsWith("dbp")){
							uri = ref.getURI();
							weight = Double.toString(ref.getWeight());
							tempResultBuffer.append(uri + "\t" + weight + "\n");
						}
					} // loop over all refs of entity
				}
			} // loop over all entities and refs in graph
			
			// dump refs list of that graph to file
			if (tempResultBuffer.length() > 0){
				bw = new BufferedWriter(new FileWriter(entsTargetDir + graphFileName.replace("graph", "entities" )));
				bw.write(tempResultBuffer.toString());
				bw.close();
				tempResultBuffer = new StringBuffer();
			}
			
		} // loop over all graphs from baseDir
	}
	

}
