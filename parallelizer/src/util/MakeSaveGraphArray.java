package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.TreeMap;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Role;
import graphreader.GraphReader;

public class MakeSaveGraphArray {

	public static void main(String[] args){
		// 7186933 en graphs + 3399089 es graphs= 10586022
		// graph uid schema: <lang>-<docID.xml-sentenceID>-<IDinGraphArray>

		int numGraphs = 10586022;

		int[] a = new int[3];
		Object[][] globalGraphs = new Object[numGraphs][2];


		TreeMap<String, List<DirectedSparseGraph<Argument, Role>>> tempMap = null;
		List<DirectedSparseGraph<Argument, Role>> 					tempGList = null;
		String 														tempGraphUID = null;
		DirectedSparseGraph<Argument, Role>							tempGraph = null;
		
		String baseResultDir = args[0]; // /dev/shm/artem/SIMILARITY-v2/
		File resultArrayFile = new File(baseResultDir + "v2-ALLGRAPHS-Object[][key, graph]");
		String sourceDir = args[1];		// /dev/shm/artem/GRAPH-v2/
		File graphsDir = new File(sourceDir);


		// my logger
		PrintWriter L = null;
		try {
			L = new PrintWriter(new File(baseResultDir + "errors.log"));
		} catch (IOException e1) {e1.printStackTrace();}


		




		long workersStarted = 0;
		long totalTasks = numGraphs;
		long interval = totalTasks / 100; // 1% Schrittweite
		
		int graphCounter = 0;
		try{
			for (File f : graphsDir.listFiles()){

				// print progress info
				workersStarted ++;
				try{
					if(workersStarted % interval == 0)
						System.out.println("PROGRESS: " + (100.0 * workersStarted / totalTasks) +  " %");
				}catch(Exception ae){System.out.println("error in progress printer");}


				tempMap = GraphReader.readGraphsFromFile(f.getAbsolutePath());



				for (String sentenceKey : tempMap.keySet()){	// iterate over each sentence in map

					tempGList = tempMap.get(sentenceKey);

					for(int i=0; i<tempGList.size(); i++){	// iterate over each graph in sentence
						
						tempGraphUID = sentenceKey + "-" + i;	// construct graph uid
						tempGraph = tempGList.get(i);
						
						globalGraphs [graphCounter][0] = tempGraphUID;	// store
						globalGraphs [graphCounter][1] = tempGraph;
						
						graphCounter ++;								// increment
						
						
					}
				}		
			}
			
			// write result to file
			try {
				FileOutputStream fos = new FileOutputStream(resultArrayFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);

				oos.writeObject(globalGraphs);

				oos.close();
				fos.close();

			} catch (FileNotFoundException e) {
				L.println("fnfe when writing result file");
				e.printStackTrace();
			} catch (IOException e) {
				L.println("ioe when writing result file");
				e.printStackTrace();
			}
			
		}catch(Exception e){
			L.println("error in " + tempGraphUID);
			e.printStackTrace();
		}
	}
	
	

}

