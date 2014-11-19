package algo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import parallelizer.Parallelizable;
import parallelizer.Worker;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.GraphToJson;
import graph.Role;
import graph.SimilarityMeasure;
import graphreader.GraphReader;

public class GraphJSONDumper extends Parallelizable{

	
	// "/local/users/arsc/data/srl+annot+graphs/"
	private static String baseDir = System.getProperty("sourcebasedir");
	// "/local/users/arsc/data/graphs-json/"
	private static String resultBaseDir = System.getProperty("resultbasedir");
	
	private static String ID; 
	public static void main(String[] args) {
		String dataFilePath = "/home/pilatus/Desktop/en-2616303-1-1.graph";
		String[] parts = dataFilePath.split("/");
		// "en-29843272-6-1.json"
		String graphName = parts[parts.length -1].split("\\.")[0] + ".json";
		String targetDir = resultBaseDir + dataFilePath.substring(0, dataFilePath.length() - graphName.length()-1);
		
		System.out.println(graphName);
		System.out.println(targetDir);
		
	}

	@Override
	// "1-result/es-4295879_en-29843272/en-29843272-6-1.graph"
	public void runAlgo(String dataFilePath, Logger L) {
		ID = "[" + dataFilePath + "] ";
		
		// 1. /////////////////////////////////////////
		String[] parts = dataFilePath.split("/");
		// "en-29843272-6-1.json"
		String graphName = parts[parts.length -1].split("\\.")[0] + ".json";
		String targetDir = resultBaseDir + dataFilePath.substring(0, dataFilePath.length() - graphName.length() - 1);
		(new File(targetDir)).mkdirs();
		
		// 2. /////////////////////////////////////////
		DirectedSparseGraph<Argument, Role> graph = GraphReader.readOneGraphFromFile(baseDir + dataFilePath);
		Argument root = SimilarityMeasure.getRoot(graph);
		
		// 3. /////////////////////////////////////////
		StringBuffer graphJson = null;
		try{
			graphJson = GraphToJson.makeJson(root, graph);
		}catch(Exception e){
			L.error(ID + "GraphToJson-fail: ", e);
			Worker.decrBusyWorkers();
			return;
		}
				
		// 4. /////////////////////////////////////////
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(targetDir + graphName));		
			bw.write(graphJson.toString());
		} catch (IOException ioe) {
			L.error(ID + "result-write-fail: ", ioe);
			Worker.decrBusyWorkers();
			return;
		}finally{
			try {
				bw.close();
			} catch (IOException ioe) {
				L.error(ID + "close-fail: ", ioe);
			}
		}

		Worker.decrBusyWorkers();	
	}
	

	@Override
	public void cleanUpFinally(Logger L) {
		L.info(ID + "finished");
	}
	
	

}
