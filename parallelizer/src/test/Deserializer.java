package test;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Ref;
import graph.Role;
import graph.SimilarityMeasure;
import graphreader.GraphReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

public class Deserializer {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		 	
		String someGraph = "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald-en-graphs/en-194-1-1.graph";
		DirectedSparseGraph <Argument, Role> g = graphreader.GraphReader.readOneGraphFromFile(someGraph);
//		test.GRAPHTESTER.visGraph(g, "ölkö");
	
		System.out.println();
		for(Argument a : g.getVertices()){
			System.out.println(a.getDisplayName());
		}
	}

}
