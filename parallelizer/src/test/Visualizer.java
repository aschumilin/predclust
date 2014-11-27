package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graph.Argument;
import graph.Role;

public class Visualizer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws java.io.IOException{
		// TODO Auto-generated method stub
		String graphsHome = "/home/pilatus/WORK/pred-clust/data/1-small-sample/";
		String graphPairsList = "/home/pilatus/WORK/pred-clust/data/1-small-sample/mostSimilarGraphs.csv"; //"/home/pilatus/WORK/pred-clust/data/1-small-sample/es-13325_en-5935/SIMMAT/v2_top";
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(graphPairsList)));
		} catch (FileNotFoundException e) {
			System.out.println("list file not found: " + graphPairsList);		
		}
		
		String line = "";
		while((line = br.readLine())!=null){
			String gEsName = line.split(",")[0];
			String gENName = line.split(",")[1];
			String sim = line.split(",")[2];
			
			String graphDir = graphsHome + gEsName.split("-")[0] +"-"+ gEsName.split("-")[1] + "_" + gENName.split("-")[0] +"-" + gENName.split("-")[1] + "/";
			
			DirectedSparseMultigraph<Argument, Role> total = new DirectedSparseMultigraph<Argument, Role>();
			DirectedSparseGraph<Argument, Role> g1, g2;
			g1 = graphreader.GraphReader.readOneGraphFromFile(graphDir +  gEsName);
			g2 = graphreader.GraphReader.readOneGraphFromFile(graphDir + gENName);

			for (Role r : g1.getEdges() ){
				total.addEdge(r,  g1.getSource(r), g1.getDest(r));		
			}
			
			for(Role r: g2.getEdges()){
				total.addEdge(r, g2.getSource(r), g2.getDest(r));
			}
			
			test.GRAPHTESTER.visGraph(total, line.replaceAll(",", "   "));
		}
	}

}
