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

		//		System.out.print("doing " + graphUIDs[I] + " : " + graphUIDs[J]);

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
			//			updateMatrix(m1, I, J, m1Value);
			simSum += m1Value;

			double m1_2Value 	= SimilarityMeasure.m1PartialPredicateDisplName(root1, root2); 
			//			updateMatrix(m1_2, I, J, m1_2Value);
			simSum += m1_2Value;

			double m2Value	 	= SimilarityMeasure.m2WordnetSynset(root1,  root2);
			//			updateMatrix(m2, I, J, m2Value);
			simSum += m2Value;

			double m4Value		= SimilarityMeasure.m4OutNeighbourNodes(g1, g2, root1, root2);
			//			updateMatrix(m4, I, J, m4Value);
			simSum += m4Value;

			double m6Value		= SimilarityMeasure.m6OutRoleLabels(g1, g2, root1, root2);
			//			updateMatrix(m6, I, J, m6Value);
			simSum += m6Value;

			double m8Value		= SimilarityMeasure.m8OutRoleLabelsAndNodes(g1, g2, root1, root2);
			//			updateMatrix(m8, I, J, m8Value);
			simSum += m8Value;

			double mJCValue 	= SimilarityMeasure.simJaccardCategories(g1, g2, root1, root2);
			//			updateMatrix(mJC, I, J, mJCValue);
			simSum += mJCValue;

			updateMatrix(mSUM, I, J, simSum);

			Worker.decrBusyWorkers();

			//			System.out.println("   <- done !");
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

		Logger L = Logger.getLogger(GraphSimilarity.class);

		String baseDir = "/home/pilatus/WORK/pred-clust/data/1-small-sample/"; 
		String[] docpairs = new String[]{"es-13325_en-5935", "es-135227_en-808402", "es-164872_en-1361541", "es-124841_en-53316", "es-1646154_en-27634910", "es-1496540_en-19001", "es-15708_en-19283151", "es-1696790_en-31693888", "es-14908_en-25882", "es-162563_en-9483238", "es-106380_en-38092", "es-1084332_en-12400564", "es-134723_en-5012024", "es-144945_en-582373", "es-1128810_en-1966504", "es-100190_en-178816", "es-1418687_en-705022", "es-150161_en-255849", "es-116120_en-207083", "es-115922_en-38241", "es-157735_en-8260404", "es-1385355_en-14924219", "es-17097_en-11395198", "es-1185698_en-690254", "es-153391_en-8626", "es-1067598_en-9256803"};

		
		
		for (String pair : docpairs){
			
			String currentPairResultDir = baseDir + pair + "/SIMMAT/";

			// get spanish and english graphs for each docpair
			int numEs, numEN;
			
			
			// read graphs from files here 
				// list dir
				// filter es and en
				// parse graphs
			// init numEs, numEN
			
			// TODO
			// TODO
			
			
			
			String[] labEs = new String[numEs];
			String[] labEN = new String[numEN+1];
			labEN[0] = " "; // empty top left corner of matrix
			
			
			// 0. INIT RESOURCES: matrices
			m1 = new double[numEs][numEN];
			m1_2 = new double[numEs][numEN];
			m2 = new double[numEs][numEN];
			m4 = new double[numEs][numEN];
			m6 = new double[numEs][numEN];
			m8 = new double[numEs][numEN];
			mJC = new double[numEs][numEN];
			mSUM = new double[numEs][numEN];

			for (int I=0; I<numEs; I++){
				for (int J=I+1; J<numEN; J++){
					
					
					
					


					String g1FilePath = graphsSourceDir + graphUIDs[I];
					String g2FilePath = graphsSourceDir + graphUIDs[J];

					//			System.out.print("doing " + graphUIDs[I] + " : " + graphUIDs[J]);

					DirectedSparseGraph<Argument, Role> g1 = null;
					DirectedSparseGraph<Argument, Role> g2 = null;



					try{
						Argument root1 = SimilarityMeasure.getRoot(g1);
						Argument root2 = SimilarityMeasure.getRoot(g2);

						double simSum = 0;

						double m1Value 		= SimilarityMeasure.m1PredicateDisplName(root1, root2);
						updateMatrix(m1, I, J, m1Value);
						updateMatrix(m1, J, I, m1Value);
						simSum += m1Value;

						double m1_2Value 	= SimilarityMeasure.m1PartialPredicateDisplName(root1, root2); 
						updateMatrix(m1_2, I, J, m1_2Value);
						updateMatrix(m1_2, J, I, m1_2Value);
						simSum += m1_2Value;

						double m2Value	 	= SimilarityMeasure.m2WordnetSynset(root1,  root2);
						updateMatrix(m2, I, J, m2Value);
						updateMatrix(m2, J, I, m2Value);
						simSum += m2Value;

						double m4Value		= SimilarityMeasure.m4OutNeighbourNodes(g1, g2, root1, root2);
						updateMatrix(m4, I, J, m4Value);
						updateMatrix(m4, J, I, m4Value);

						simSum += m4Value;

						double m6Value		= SimilarityMeasure.m6OutRoleLabels(g1, g2, root1, root2);
						updateMatrix(m6, I, J, m6Value);
						updateMatrix(m6, J, I, m6Value);
						simSum += m6Value;

						double m8Value		= SimilarityMeasure.m8OutRoleLabelsAndNodes(g1, g2, root1, root2);
						updateMatrix(m8, I, J, m8Value);
						updateMatrix(m8, J, I, m8Value);

						simSum += m8Value;

						double mJCValue 	= SimilarityMeasure.simJaccardCategories(g1, g2, root1, root2);
						updateMatrix(mJC, I, J, mJCValue);
						updateMatrix(mJC, J, I, mJCValue);

						simSum += mJCValue;

						updateMatrix(mSUM, I, J, simSum);
						updateMatrix(mSUM, J, I, simSum);

					}catch(Exception e){L.error("some exception during similarity calculation:", e);}
		
				}
			} // loop over all graph-pairs, calculate pairwise sim values
	
			writeMatrices(L, labEN, labEs, currentPairResultDir);
		} // loop over each doc-pair

	}
	
	public static void writeMatrices(Logger L, String[] enLabs, String[] esLabs, String matricesResultDir){
		L.info("writing results to csv files");

		Object[] metrics = new Object[]{m1, m1_2, m2, m4, m6, m8, mJC, mSUM};
		//Object[] metrics = new Object[]{ mSUM};
		String[] resultFileNames = new String[]{"m1.csv", "m1_2.csv", "m2.csv", "m4.csv", "m6.csv", "m8.csv", "mJC.csv", "mSUM.csv"};
		PrintWriter pw= null;

		// buffer keeps entire matrix
		StringBuffer buf = null;
		
		for(int k=0; k<metrics.length; k++){
			buf = new StringBuffer();
			
			String destinationFilePath = matricesResultDir + resultFileNames[k];
			try {
				// file appenders
				pw = new PrintWriter( new BufferedWriter(new FileWriter(new File(destinationFilePath), true)));
				
				// create first line with english captions
				for (String label : enLabs){
					buf.append(label).append(",");
				}
				buf.deleteCharAt(buf.length()-1).append(";\n");
				

				// create buffer and dump it
				double[][] currentMatrix = (double[][])metrics[k];
				
				for (int i = 0; i<currentMatrix.length; i++){

					// add row caption toeach new line
					buf.append(esLabs[i]).append(",");

					for (int j=0; j<currentMatrix[i].length;j++){
						buf.append( currentMatrix[i][j] ).append(",");
					}
					
					// add EOL symbol
					buf.deleteCharAt(buf.length()-1).append(";\n");


				}
					
				pw.write(buf.toString());
				pw.close();
			} catch (IOException ioe) {
				L.fatal("could not write this result file: " + resultFileNames[k], ioe);					
			}catch (Exception e) {
				L.fatal("some exc in writing file at " + resultFileNames[k], e);					
			}
		} // loop over all sim-matrices
		
		



		L.info("done writing all matrices ");

	}


	@Override
	public void cleanUpFinally(Logger L){
		L.info("writing results to csv files");

		Object[] metrics = new Object[]{m1, m1_2, m2, m4, m6, m8, mJC, mSUM};
		//			Object[] metrics = new Object[]{ mSUM};
		String[] resultFileNames = new String[]{"m1.csv", "m1_2.csv", "n2.csv", "m4.csv", "m6.csv", "m8.csv", "mJC.csv", "mSUM.csv"};
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
