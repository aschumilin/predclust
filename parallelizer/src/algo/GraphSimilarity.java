package algo;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

import parallelizer.Parallelizable;
import parallelizer.Worker;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Role;
import graph.SimilarityMeasure;

public class GraphSimilarity extends Parallelizable {

	private static String resultDir			= System.getProperty("result.dir");	
	private static String graphsSourceDir	= System.getProperty("graphs.dir");
	private static String graphsListFieName	= System.getProperty("graphs.list");


	private static String[] 	graphUIDs	= null;		// array with graph file names
	private static int			dimension 	= 0;		// length of this array

	private static double[][] 	m1			= null;		// predicate display name matching
	private static double[][] 	m1_2		= null;		// loose predicate displ anem matching
	private static double[][] 	m2			= null;		// predicate wordnet synset matching
	private static double[][] 	m4			= null;		// Out-neighbours nodes mathcing
	private static double[][] 	m6			= null;		// out-role label matching
	private static double[][] 	m8			= null;		// out-role # nodes matching
	private static double[][] 	mJC			= null;		// jaccard metric on DBp-categories in predicate out-neighbourhood
	private static double[][]	mSUM		= null;		// sum over the 7 separate metrics

	private static double		bigM		= 99999;	// high value for self-similarity
	
	private static boolean 		matricesInitialized = false;	// flag


	@Override
	public void runAlgo(String jobDescription, Logger L) {

		// 0. INIT RESOURCES: read graph list
		try{
			getGraphUIDs(L);
			
		}catch(IOException ioe){
			L.fatal("\t can not read graph list file...exiting", ioe);
			System.exit(1);
		}
		// 0. INIT RESOURCES: matrices
		initMatrices(L);



		// 1. READ GRAPHS FROM FILE
		String[] indices =  jobDescription.split(",");	//"i,j"
		int I			= Integer.parseInt(indices[0]);
		int J 			= Integer.parseInt(indices[1]);

		String g1FilePath = graphsSourceDir + graphUIDs[I];
		String g2FilePath = graphsSourceDir + graphUIDs[J];
		
																						System.out.print("doing " + graphUIDs[I] + " : " + graphUIDs[J]);

		DirectedSparseGraph<Argument, Role> g1 = null;
		DirectedSparseGraph<Argument, Role> g2 = null;

		try {
			FileInputStream fis = new FileInputStream(g1FilePath);
			ObjectInputStream ois = new ObjectInputStream(fis);
			g1 = (DirectedSparseGraph<Argument, Role>) ois.readObject();
			ois.close();
			fis.close();

		} catch (Exception e) {
			L.error("exception when reading graph: " + graphUIDs[I], e);
			Worker.decrBusyWorkers();
			return;
		} 
		try {
			FileInputStream fis = new FileInputStream(g2FilePath);
			ObjectInputStream ois = new ObjectInputStream(fis);
			g2 = (DirectedSparseGraph<Argument, Role>) ois.readObject();
			ois.close();
			fis.close();		
		} catch (Exception e) {
			L.error("exception when reading graph: " + graphUIDs[J], e);
			Worker.decrBusyWorkers();
			return;
		} 

		try{
			Argument root1 = SimilarityMeasure.getRoot(g1);
			Argument root2 = SimilarityMeasure.getRoot(g2);

			double simSum = 0;

			double m1Value 		= SimilarityMeasure.m1PredicateDisplName(root1, root2);
			updateMatrix(m1, I, J, m1Value);
			simSum += m1Value;

			double m1_2Value 	= SimilarityMeasure.m1PartialPredicateDisplName(root1, root2); 
			updateMatrix(m1_2, I, J, m1_2Value);
			simSum += m1_2Value;

			double m2Value	 	= SimilarityMeasure.m2WordnetSynset(root1,  root2);
			updateMatrix(m2, I, J, m2Value);
			simSum += m2Value;

			double m4Value		= SimilarityMeasure.m4OutNeighbourNodes(g1, g2, root1, root2);
			updateMatrix(m4, I, J, m4Value);
			simSum += m4Value;

			double m6Value		= SimilarityMeasure.m6OutRoleLabels(g1, g2, root1, root2);
			updateMatrix(m6, I, J, m6Value);
			simSum += m6Value;

			double m8Value		= SimilarityMeasure.m8OutRoleLabelsAndNodes(g1, g2, root1, root2);
			updateMatrix(m8, I, J, m8Value);
			simSum += m8Value;

			double mJCValue 	= SimilarityMeasure.simJaccardCategories(g1, g2, root1, root2);
			updateMatrix(mJC, I, J, mJCValue);
			simSum += mJCValue;

			updateMatrix(mSUM, I, J, simSum);
			
			Worker.decrBusyWorkers();
			
			System.out.println("   <- done !");
		}catch(Exception e){
				L.error("some exception during similarity calculation: " + jobDescription, e);
				Worker.decrBusyWorkers();
				return;
			}
		}




		private static synchronized void updateMatrix(double[][] matrix, int i, int j, double value){
			matrix[i][j] = value;
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




		private static synchronized void initMatrices(Logger L){
			if(! matricesInitialized && dimension != 0){
				m1 = new double[dimension][dimension];
				m1_2 = new double[dimension][dimension];
				m2 = new double[dimension][dimension];
				m4 = new double[dimension][dimension];
				m6 = new double[dimension][dimension];
				m8 = new double[dimension][dimension];
				mJC = new double[dimension][dimension];
				mSUM = new double[dimension][dimension];
				
				matricesInitialized = true;
				
				L.info("metrics matrices initialized ok");

			}
		}



		public static void main(String[] args) {
			System.out.println("asdasd-asdad.asdda-12,asdasd-dljjs87-98".split(",")[0]);
			m1 = new double[dimension][dimension];
			
		}


		@Override
		public void cleanUpFinally(Logger L){
			L.info("writing results to csv files");
			
//		
			Object[] metrics = new Object[]{m1, m1_2, m2, m4, m6, m8, mJC, mSUM};
			String[] resultFileNames = new String[]{"m1.csv", "m1_2.csv", "m2.csv", "m4.csv", "m6.csv", "m8.csv", "mCategories.csv", "mSUM.csv"};
			PrintWriter[] pws = new PrintWriter[metrics.length];
			String[] lines	= new String[metrics.length];
			
			for(int k=0; k<metrics.length; k++){
				String destinationFilePath = resultDir + resultFileNames[k];
				try {
					// file appenders
					pws[k] = new PrintWriter( new BufferedWriter(new FileWriter(new File(destinationFilePath), true)));
				} catch (IOException e) {
					L.fatal("could not write this result file: " + resultFileNames[k], e);					
				}
			}
	
			
			
			for(int i=0; i<dimension; i++){	//LINES
				
				
				for(int k=0; k<lines.length; k++)	lines[k] = "";	// empty the current line for each metric
				
				
				for(int j=0; j<dimension; j++){ //VALUES
					
					for(int k=0; k<metrics.length; k++){			// append individual values for each metric 
						
						if(i==j){ 						// bigM
							lines[k] = lines[k] + bigM;
						}else if(i > j){ 				// lower left half of matrix empty --> mirror this value
							lines[k] = lines[k] + ((double[][])metrics[k])[j][i];
						}else{							// normal , recorded value
							lines[k] = lines[k] + ((double[][])metrics[k])[i][j];
						}

						if(j == dimension - 1){			// append end-of-line semicolon here
							lines[k] += ";\n";
						}else{
							lines[k] += ",";				// or continue the line
						}
					
					}
					
				} //VALUES
				
				for(int k=0; k<lines.length; k++){
					// write k-th line with the k-th print writer
					pws[k].print(lines[k]);
				}
			} //LINES
			
			// close the streams!!!!!!!!!!!!!
			for(int k=0; k<pws.length; k++){
				pws[k].close();
			}
			
			L.info("done cleaning up");
			
		}

	}
