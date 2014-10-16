package test;

import graph.Argument;
import graph.Ref;
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
		 	
		
		String fileName = "/ssd/users/arsc/SIM/50k-graph-ids";
		BufferedReader br =new BufferedReader(new InputStreamReader(new FileInputStream(fileName), Charset.forName("UTF-8")));
		
		PrintWriter L = null;
		try {
			L = new PrintWriter(new File("/ssd/users/arsc/SIM/50k-graph-details"));
		} catch (IOException e1) {e1.printStackTrace();}
		
		String graphID = "";
		String line ="";
		while((graphID = br.readLine()) != null){
			System.out.println(graphID);
			String ref = ";";
			
			
			
			Argument pred = SimilarityMeasure.getRoot(GraphReader.readOneGraphFromFile("/dev/shm/artem/SINGLES/" + graphID));
			List<Ref> refs = pred.getRefs();
			if (refs.size() > 0){
				ref = pred.getRefs().get(0).getURI();				
			}else{
				ref = "-";
			}
			
			line = graphID.trim() + " , " + pred.getDisplayName() + " , " + pred.getMention() + " , " + ref + " ;";
			
			L.println(line);
		}
		br.close();
		L.close();
		
		br = new BufferedReader(new InputStreamReader(new FileInputStream("/ssd/users/arsc/SIM/50k-graph-details"), Charset.forName("UTF-8")));
		
		while((line = br.readLine()) != null){
			System.out.println(line);
		}
		br.close();

	}

}
