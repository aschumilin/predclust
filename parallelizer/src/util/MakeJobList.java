package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Stack;

import parallelizer.DataChunk;
import parallelizer.Worker;

public class MakeJobList {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Stack<int[]> data = new Stack<int[]>();

		int k = 1;
		for (int i=0; i<200000; i++){
			if(i%1000000 == 0)	System.out.println(k++);
			for(int j=i+1; j<200000; j++){
				//				data.add(new int[]{i,j});
				//System.out.println(i +  " \t " + j);
				k++;
			}
		}

		int i=0, j=1;
		int l = 5;
		double X = (l*l-l)/2;
		System.out.println("total " + X);
		String[][] res = new String[l][l];
		while(true){

			if (X>0){ 

				// check if there are idle workers


				System.out.println(i + "\t" + j);
				res[i][j] = i + "-" + j;
				res[j][i] = "\"";
				res[i][i] = "/";
				
				X--;

				j++; 
				if(j == l){
					i++;
					if(i==l){
						break;
					}else {
						j=i+1;
					}
				}

			}else{
				break;
			}


		}
		
		for (String[] line : res){
			for(String s : line){
				System.out.print(s + "  ");
			}
			System.out.println();
		}
	}
	
		
	
	public static void mainA(String[] args) {
		String baseResultDir = args[0];
		long start = System.currentTimeMillis();
		File raphsArrayFile = new File(baseResultDir + "v2-ALLGRAPHS-Object[][key, graph]");
		String singleGraphFileTemp = "/dev/shm/artem/SINGLES/";
		String graphUidTemp			= "";
		Object[][] graphs = null;
		String[] graphUIDs = null;

		System.out.println("started reading total graphs");
		try {
			FileInputStream fis = new FileInputStream(raphsArrayFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			graphs = (Object[][]) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			System.out.println("exception when reading graphs array");
			e.printStackTrace();
		} 

		int totalTasks = graphs.length;
		int interval = totalTasks / 100; // 1% Schrittweite	

		System.out.println("first 100 entries in graphs:");
		for(int i=0;i<100;i++){
			System.out.println(graphs[i][0]);
		}

		System.out.println("read total graphs: " + totalTasks);
		System.out.println("in mins" + (System.currentTimeMillis()-start)/(1000*60));

		// my logger
		PrintWriter L = null, LL = null;
		try {
			L = new PrintWriter(new File(baseResultDir + "v2-graphs-list"));
			LL = new PrintWriter(new File("/ssd/users/arsc/" + "v2-pairs-list"));
		} catch (IOException e1) {e1.printStackTrace();}

		FileOutputStream fos = null;
		ObjectOutputStream oos = null;

		graphUIDs = new String[totalTasks];		

		start = System.currentTimeMillis();
		System.out.println("started atsaving single graphs");

		for(int i=0; i<totalTasks; i++){	// MAIN LOOP
			try{
				if(i % interval == 0)	System.out.println("PROGRESS: " + (100.0 * i / totalTasks) +  " %");
			}catch(Exception ae){System.out.println("error in progress printer");}

			// WRITE SINGLE GRAPH TO FILE
			try {
				graphUidTemp = (String) graphs[i][0];
				fos = new FileOutputStream(singleGraphFileTemp + graphUidTemp + ".graph");
				oos = new ObjectOutputStream(fos);
				oos.writeObject(graphs[i][1]);
				oos.close();
				fos.close();
				graphUIDs[i] = graphUidTemp + ".graph";
				L.println(graphUidTemp + ".graph");
			} catch (Exception e) {
				System.out.println("exception when writing result: " + graphUidTemp  + " : " + e.getMessage());
				e.printStackTrace();
			}	
		}
		L.close();
		System.out.println("finished in mins " + (System.currentTimeMillis()-start)/(1000*60));		


		start = System.currentTimeMillis();
		System.out.println("started printing pairs job file");

		for(int i=0; i<totalTasks; i++){			
			for(int j=i+1; j<totalTasks ; j++){
				LL.println(graphUIDs[i] +  "," + graphUIDs[j]);
			}
		}	
		LL.close();
		System.out.println("finished in mins " + (System.currentTimeMillis()-start)/(1000*60));		




	}

}
