package algo;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import parallelizer.Parallelizable;
import parallelizer.Worker;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Role;
import graph.SimilarityMeasure;

public class GraphSimilarityNoArray extends Parallelizable {

	private static String resultDir			= System.getProperty("result.dir");	
	private static String graphsSourceDir	= System.getProperty("graphs.dir");
	private static String graphsListFieName	= System.getProperty("graphs.list");


	private static ArrayList<DirectedSparseGraph<Argument, Role>> graphs = null;

	private static String[] 	graphUIDs	= null;		// array with graph file names
	private static int			dimension 	= 0;		// length of this array

	private static double		bigM		= 99999;	// high value for self-similarity




	@Override
	public void runAlgo(String jobDescription, Logger L) {

		StringBuilder 	m1			= new StringBuilder();		// predicate display name matching
		StringBuilder 	m1_2		= new StringBuilder();		// loose predicate displ name matching
		StringBuilder 	m2			= new StringBuilder();		// predicate wordnet synset matching
		StringBuilder 	m4			= new StringBuilder();		// Out-neighbours nodes mathcing
		StringBuilder 	m6			= new StringBuilder();		// out-role label matching
		StringBuilder 	m8			= new StringBuilder();		// out-role # nodes matching
		StringBuilder 	mJC			= new StringBuilder();		// jaccard metric on DBp-categories in predicate out-neighbourhood
		StringBuilder	mSUM		= new StringBuilder();		// sum over the 7 separate metrics

		

		// 0. INIT RESOURCES: read graph list
		try{
			getGraphUIDs(L);		
		}catch(IOException ioe){
			L.fatal("\t can not read graph list file...exiting", ioe);
			System.exit(1);
		}

		// 0. INIT RESOURCES: read graphs 
		try{
			getGraphs(L);
		}catch(Exception e){
			L.fatal("\t can not read graphs...exiting", e);
			System.exit(1);
		}


		// 1. COMPUTE METRICS FOR THE ENTIRE LINE
		BufferedWriter bw = null;
		int myIndex = Integer.parseInt(jobDescription);	// i

		DirectedSparseGraph<Argument, Role> myG 	= graphs.get(myIndex);
		DirectedSparseGraph<Argument, Role> otherG 	= null;

		Argument myRoot 	= SimilarityMeasure.getRoot(myG);
		Argument otherRoot 	= null; //SimilarityMeasure.getRoot(otherG);

		double simSum, m1Value ,m1_2Value ,m2Value ,m4Value ,m6Value ,m8Value ,mJCValue;


		
		for(int j=0; j<dimension; j++){		// run over entire line and compute metric values


			simSum = 0;
			m1Value = 0;
			m1_2Value = 0;
			m2Value = 0;
			m4Value = 0;
			m6Value = 0;
			m8Value = 0;
			mJCValue = 0;


			if(j == myIndex){
				m1.append(bigM);  
				m1_2.append(bigM);  
				m2.append(bigM);  
				m4.append(bigM);  
				m6.append(bigM);  
				m8.append(bigM);  
				mJC.append(bigM);  
				mSUM.append(bigM);
			}else{
				// calculate metrics
				otherG = graphs.get(j);
				otherRoot = SimilarityMeasure.getRoot(otherG);

				try{

					m1Value = Math.round(SimilarityMeasure.m1PredicateDisplName(myRoot, otherRoot)*1000)/1000.;
					simSum += m1Value;
					m1.append(m1Value);

					m1_2Value 	= Math.round(SimilarityMeasure.m1PartialPredicateDisplName(myRoot, otherRoot)*1000)/1000.0; 
					simSum += Math.round(m1_2Value*1000)/1000.0;
					m1_2.append(m1_2Value);

					m2Value	 	= Math.round(SimilarityMeasure.m2WordnetSynset(myRoot,  otherRoot)*1000)/1000.0;
					simSum += Math.round(m2Value*1000)/1000.0;
					m2.append(m2Value);

					m4Value		= Math.round(SimilarityMeasure.m4OutNeighbourNodes(myG, otherG, myRoot, otherRoot)*1000)/1000.0;
					simSum += Math.round(m4Value*1000)/1000.0;
					m4.append(m4Value);

					m6Value		= Math.round(SimilarityMeasure.m6OutRoleLabels(myG, otherG, myRoot, otherRoot)*1000)/1000.0;
					simSum += Math.round(m6Value*1000)/1000.0;
					m6.append(m6Value);

					m8Value		= Math.round(SimilarityMeasure.m8OutRoleLabelsAndNodes(myG, otherG, myRoot, otherRoot)*1000)/1000.0;
					simSum += Math.round(m8Value*1000)/1000.0;
					m8.append(m8Value);

					mJCValue 	= Math.round(SimilarityMeasure.simJaccardCategories(myG, otherG, myRoot, otherRoot)*1000)/1000.0;
					simSum += Math.round(mJCValue*1000)/1000.0; 
					mJC.append(mJCValue);

					mSUM.append(Math.round(simSum*1000)/1000.0);

				}catch(Exception e){
					L.error("ERROR some exception during similarity calculation: " + graphUIDs[myIndex] + "(" + myIndex + ")", e);
					Worker.decrBusyWorkers();
					return;
				}
			}

			if(j == dimension - 1){	// end-of-line symbol
				m1.append(";\n");  
				m1_2.append(";\n");  
				m2.append(";\n");  
				m4.append(";\n");  
				m6.append(";\n");  
				m8.append(";\n");  
				mJC.append(";\n");  
				mSUM.append(";\n");  	
			}else{					// or value separator
				m1.append(",");  
				m1_2.append(",");  
				m2.append(",");  
				m4.append(",");  
				m6.append(",");  
				m8.append(",");  
				mJC.append(",");  
				mSUM.append(",");
			}
		}

		
		//2. WRITE CSV FILES
		try {
			// each worker puts his line into the directory of each metric
			String fileName = resultDir + "m1/" + myIndex + ".csv";
			bw = new BufferedWriter( new FileWriter(fileName));
			bw.write(m1.toString());
			bw.close();
			
			fileName = resultDir + "m1_2/" + myIndex + ".csv";
			bw = new BufferedWriter( new FileWriter(fileName));
			bw.write(m1_2.toString()); 
			bw.close();
			
			fileName = resultDir + "m2/" + myIndex + ".csv";
			bw = new BufferedWriter( new FileWriter(fileName));
			bw.write(m2.toString()); 
			bw.close();
			
			fileName = resultDir + "m4/" + myIndex + ".csv";
			bw = new BufferedWriter( new FileWriter(fileName));
			bw.write(m4.toString());
			bw.close();
			
			fileName = resultDir + "m6/" + myIndex + ".csv";
			bw = new BufferedWriter( new FileWriter(fileName));
			bw.write(m6.toString());
			bw.close();
			
			fileName = resultDir + "m8/" + myIndex + ".csv";
			bw = new BufferedWriter( new FileWriter(fileName));
			bw.write(m8.toString());
			bw.close();
			
			fileName = resultDir + "mJC/" + myIndex + ".csv";
			bw = new BufferedWriter( new FileWriter(fileName));
			bw.write(mJC.toString());
			bw.close();
			fileName = resultDir + "mSUM/" + myIndex + ".csv";
			bw = new BufferedWriter( new FileWriter(fileName));
			bw.write(mSUM.toString());
			bw.close();
		} catch (IOException e) {
			L.error("ERROR could not write this result file: " + graphUIDs[myIndex] + "(" + myIndex + ")", e);					
		}

		Worker.decrBusyWorkers();

	}



	private static synchronized void getGraphs(Logger L) throws IOException, ClassNotFoundException{

		if(graphs == null && graphUIDs != null){
			L.info("start reading in " + graphUIDs.length + " graphs");

			graphs = new ArrayList<DirectedSparseGraph<Argument, Role>>(graphUIDs.length); 
			String graphFilePathTemp = "";
			FileInputStream fis 	= null;
			ObjectInputStream ois 	= null;

			for(int i=0; i<dimension; i++){
//	System.out.println(i +  "  " + graphUIDs[i]);
				graphFilePathTemp = graphsSourceDir + graphUIDs[i];
				fis = new FileInputStream(graphFilePathTemp);
				ois= new ObjectInputStream(fis);
				graphs.add(i,(DirectedSparseGraph<Argument, Role>) ois.readObject());
				ois.close();
				fis.close();
			}

			L.info("done reading graphs");

		}
	}

	private static synchronized void getGraphUIDs(Logger L) throws IOException  {
		if (graphUIDs == null){

			BufferedReader br =new BufferedReader(new InputStreamReader(new FileInputStream(graphsListFieName), Charset.forName("UTF-8")));

			int numZeilen = 0;
			while (br.readLine() != null) {
				numZeilen++;
			}		
			dimension = numZeilen;
			graphUIDs = new String[dimension];
			br.close();
			br =new BufferedReader(new InputStreamReader(new FileInputStream(graphsListFieName), Charset.forName("UTF-8")));

			String line = "";
			int i = 0;
			while ((line = br.readLine()) != null) {
				graphUIDs[i] = line.trim();	
				i++;
			}
			br.close();

			L.info("UID array ok");
		}
	}





	public static void main(String[] args) {
		System.out.println("asdasd-asdad.asdda-12,asdasd-dljjs87-98".split(",")[0]);
		double[][] a = new double[20000][20000];
		a[2343][8767] = 1.0;
		a[1][0] = 1233;

	}


	@Override
	public void cleanUpFinally(Logger L){
		L.info("nothing to clean up");
	}

}
