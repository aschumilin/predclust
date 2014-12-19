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
public class CoordinatorSimMatSparseV2 {
	private final static Logger L = Logger.getLogger(CoordinatorSimMatSparseV2.class); 
	  
	private static int maxNumWorkers;

	private static Parallelizable algoClass 	= null;
	
	private static String resultDir				= System.getProperty("result.dir");	
	private static String graphsSourceDir		= System.getProperty("graphs.dir");

	
	private static final int maxBufferFlushSize = 40000000;	// 10*10^6 characters = 20m bytes
	
	private static StringBuffer[] 	buffers 		= null;
	private static FileWriter[] 	writers 		= null;

	private static String 	resultFileSuffix			= "sparse";
	public static String[] 	simMeasureNames 			= new String[]{"m1", "m12", "m2", "m4", "m62", "m8", "m82", "mJC", "mSum1.0", "mXLing", "mSum0.7", "mSum0.5", "mSum0.3", "mSum0.1"};

	public static Object[] GRAPHS = null;
	public static String[] graphUIDs = null;
	
	private static final int WAIT_TIME = 200000;
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
	
					/************************************************/				
					graphUIDs = new String[maxNumGraphsForThisJob];			
					int i = 0;
					br = new BufferedReader(new InputStreamReader(new FileInputStream(args[3]), Charset.forName("UTF-8")));
					/************************************************/	
					
					// filescollector contains other data chunks, e.g. URLs
					while (i<maxNumGraphsForThisJob && ((lineInFilesCollector = br.readLine()) != null)) {
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
		long numLines = numGraphs; // number of worker threads
		double numGraphPairs = (numGraphs*numGraphs - numGraphs)/2;
		
		int i=0;	


		L.info("dataset size (num graph pairs): " + numGraphPairs);
		L.info("maxNumWorkers: " + maxNumWorkers);
		L.info("worker class: " + algoClassName);
	
				
		while(true){
			
			if (numLines>0){ 
				
				// check if there are idle workers
				if(Worker.getBusyWorkers() < maxNumWorkers){

					
					
					// 12,102,graphIDx,graphIDy
//					String jobDescription = i + "," + j + "," + graphUIDs[i] + "," + graphUIDs[j];
					String jobDescription = "" + i ;
					
					Thread t = new Thread(new Worker(new DataChunk(jobDescription), algoClass, L));
					t.start();
					
					i++;
					
					numLines--;
					
					
				
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
			L.info("sleeping before closing streams");
			Thread.sleep(WAIT_TIME);
		} catch (InterruptedException ie) {
			L.error("thread sleep interrupted", ie);
		}
		
		// flush result buffers 
		for(int k=0; k < simMeasureNames.length; k++){		
			try {
				writers[k].write(buffers[k].toString());
				writers[k].close();
				L.info("closed result file " + simMeasureNames[k]);
			} catch (IOException e) {
				L.error("failed to write and close result file: " + simMeasureNames[k]);
			}
		}
		
		L.info("ALL DONE");
		
		
		

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
