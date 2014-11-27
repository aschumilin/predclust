package parallelizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;



/**
 * @author aschumilin
 *
 */
public class CoordinatorSimMatSparse {
	private final static Logger L = Logger.getLogger(CoordinatorSimMatSparse.class); 
	  
	private static int maxNumWorkers;

	private static Parallelizable algoClass 	= null;
	
	private static String resultDir				= System.getProperty("result.dir");	
	private static String graphsSourceDir		= System.getProperty("graphs.dir");

	
	private static final int maxBufferFlushSize = 10000000;	// 10m characters = 20m bytes
	
	private static StringBuffer[] 	buffers 		= null;
	private static FileWriter[] 	writers 		= null;

	private static String 	resultFileSuffix			= "sparse";
	public static String[] 	simMeasureNames 			= new String[]{"m1", "m1_2", "m2", "m4", "m6", "m8", "mJC", "mSum"};
	
	public static Object[] GRAPHS = null; 
	
	private static final void initResultInfra() throws IOException{
		L.info("init result infrastructure");
		buffers = new StringBuffer[simMeasureNames.length];
		writers = new FileWriter[simMeasureNames.length];
		for (int i =0; i<simMeasureNames.length; i++){
			buffers[i] = new StringBuffer();
			boolean appendToFile = true;
			writers[i] = new FileWriter(resultDir + simMeasureNames[i] + "-" + resultFileSuffix + ".csv", appendToFile);
		}
		L.info("finished init result infrastructure");
		
	}
	
	public static void initGraphs(String[] graphFileNames){
		L.info("batch-reading graphs from files...");
		GRAPHS = new Object [graphFileNames.length];
		
		for(int i=0; i<graphFileNames.length; i++){
			try{
			GRAPHS[i] = graphreader.GraphReader.readOneGraphFromFile(graphsSourceDir + graphFileNames[i]);
			}catch(Exception e){
				L.error("error reading graph file: " + graphFileNames[i], e);
			}
		}
		L.info("done batch-reading graphs");
	}


	/**
	 * @param args 
	 * 		args[0]: fully qualified algorithm class name 
	 * 		args[1] maximal number of workers
	 * 		args[2] path to the data-files file
	 */
	public static void main(String[] args) {
		
		
		  
		L.info("==============================================");
		L.info("==============================================");
		L.info("==============================================");

	
		String[] graphUIDs = null;
		
		// arg 0
		String algoClassName = args[0];
		try {
			algoClass = (Parallelizable) Class.forName(algoClassName).newInstance();
		} catch (Exception e) {
			L.fatal("exception while loading algo class:");
			e.printStackTrace();
		} 
		

		// arg 1
		maxNumWorkers = Integer.parseInt(args[1]);
		
		// arg 2 mode: files/ nofiles
		final String MODE = args[2];

		// arg 4: max number of graphs for similarity matrix
		int maxNumGraphsForThisJob = Integer.parseInt(args[4]);
		
		
		// arg 3 joblist file
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(args[3]), Charset.forName("UTF-8")));
		} catch (FileNotFoundException fnfe) {
			System.err.println("ERROR: parallelizer.Coordinator: file not found:" + args[3]);
		}
		try {
			if(br!=null){
	
				
				String lineInFilesCollector = null;
				if(MODE.equals("nofiles")){
					
//					int numZeilen = 0;
//					while (br.readLine() != null) {
//						numZeilen++;
//					}
					/************************************************/				
					graphUIDs = new String[maxNumGraphsForThisJob];			
					int i = 0;
					br = new BufferedReader(new InputStreamReader(new FileInputStream(args[3]), Charset.forName("UTF-8")));
					/************************************************/	
					
					// filescollector contains other data chunks, e.g. URLs
					while (i<maxNumGraphsForThisJob && (lineInFilesCollector = br.readLine()) != null) {
						graphUIDs[i] = lineInFilesCollector;	
						i++;
					}
				}else{
					L.error("wrong mode: " + MODE + "  exiting...");
					System.exit(1);
				}
			}else{
				L.error("buffered reader is null");
				System.exit(1);
			}
		} catch (IOException e) {
			L.error("ioe while reading file-names file at line");
			e.printStackTrace();
			System.exit(1);
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				L.error("ioe while closing data file reader");
				e.printStackTrace();
			}
		}
		
		
		
		// INIT RESLT INFRSTRUCTURE
		try {
			initResultInfra();
		} catch (IOException ioe) {
			L.error("failed to init results infrastructure: exiting...", ioe);
			System.exit(1);
		}
		
		// INIT GRAPHS
		initGraphs(graphUIDs);
		
		
		
		long numGraphs = graphUIDs.length;
		double numGraphPairs = (numGraphs*numGraphs - numGraphs)/2;
		
//		int i=0, j=0;
		int i=0, j=1;	// skip values on the  diagonal

		long workersStarted = 0;
		double totalTasks = numGraphPairs;
		double interval = 1000000; // 0,001% Schrittweite	

		L.info("dataset size (num graph pairs): " + numGraphPairs);
		L.info("maxNumWorkers: " + maxNumWorkers);
		L.info("worker class: " + algoClassName);
	
				
		while(true){
			
			if (numGraphPairs>0){ 
				
				// check if there are idle workers
				if(Worker.getBusyWorkers() < maxNumWorkers){

					// print progress info
					workersStarted ++;
					try{
						if(workersStarted % interval == 0) L.info("UP " + (100.0 * workersStarted / totalTasks) +  " %");
					}catch(Exception ae){L.info("error in progress printer", ae);}
					
					// 12,102,graphIDx,graphIDy
//					String jobDescription = i + "," + j + "," + graphUIDs[i] + "," + graphUIDs[j];
					String jobDescription = i + "," + j;
					
					Thread t = new Thread(new Worker(new DataChunk(jobDescription), algoClass, L));
					t.start();
					
					numGraphPairs--;
					
					// similarity matrix is triangular - only traverse the upper right corner like this:
//					null  0-1  0-2  0-3  0-4  
//					-  null  1-2  1-3  1-4  
//					-  -  null  2-3  2-4  
//					-  -  -  null  3-4  
//					-  -  -  -  null 
					
					j++; 
					if(j == numGraphs){
						// go to next line
						i++;
						if(i==numGraphs){
							// all lines processed -> exit
							break;
						}else {
//							j=i; 
							j=i+1;	// skip values on the diagonal
						}
					}
				
				}else{
					// do nothing
				}
			}else{
				L.info("all data files distributed to workers");
				
				
				break;
			}
			
		} // main while loop
		
		
		// wait for all worker threads to finish, then flush ad close files
		try {
			Thread.sleep(20000);
		} catch (InterruptedException ie) {
			L.error("thread sleep interrupted", ie);
		}
		
		// flush result buffers 
		for(int k=0; k < simMeasureNames.length; k++){		
			L.info("finishing buffer flush");
			try {
				writers[k].write(buffers[k].toString());
				L.info("closing result file " + simMeasureNames[k]);
				writers[k].close();
			} catch (IOException e) {
				L.error("failed to write and close result file: " + simMeasureNames[k]);
			}
		}
		
		L.info("finished flushing buffers");
		
		
		

	} // main method

	
	
	public synchronized static final void updateResultBuffers(String rowID, String colID, double[] simValues) throws IOException{
		String prefix 		= rowID + "," + colID + ",";
		String prefixDiag 	= colID + "," + rowID + ",";
		
		for(int i=0; i < simValues.length; i++){
			
			// !!! skip ZERO values !!!

			if( simValues[i] > 0.0 ){
			
				buffers[i].append(prefix)
							.append(simValues[i])
							.append("\n");
				
				// close value over diagonal
				buffers[i].append(prefixDiag)
							.append(simValues[i])
							.append("\n");

				// check if Buffer ready to be flushed
				if(buffers[i].length() > maxBufferFlushSize){
					writers[i].write(buffers[i].toString());
					buffers[i] = new StringBuffer();
//					L.info("flushed buffer " + simMeasureNames[i] );
				}
			}
		}
	}
	
	
	

}
