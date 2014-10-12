package parallelizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;

/**
 * @author aschumilin
 *
 */
public class CoordinatorSimMat {
	public static long start = 0;
	private final static Logger L = Logger.getLogger(CoordinatorSimMat.class); 
	private static RollingFileAppender fa = new RollingFileAppender();
	  
	private static int maxNumWorkers;

//	private static Parallelizable algoClass = new algo.Test();
	private static Parallelizable algoClass = null;
	
	private static String logfilePath = System.getProperty("logfile");

	/**
	 * @param args 
	 * 		args[0]: fully qualified algorithm class name 
	 * 		args[1] maximal number of workers
	 * 		args[2] path to the data-files file
	 */
	public static void main(String[] args) {
		
		fa.setName("L");
		fa.setFile(logfilePath);
		fa.setLayout(new org.apache.log4j.PatternLayout("%d{dd MMM yyyy HH:mm:ss} %p %t %c - %m%n"));
		fa.setThreshold(org.apache.log4j.Level.DEBUG);
		fa.setAppend(true);
		fa.setMaxFileSize("500MB");
		fa.activateOptions();
		Logger.getLogger(CoordinatorSimMat.class).addAppender(fa);
		  
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

		
		int maxNumGraphsForThisJob = Integer.parseInt(args[4]);
		
		
		// arg 3
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
		
		
		
		
		CoordinatorSimMat.start = System.currentTimeMillis();
		
		long numGraphs = graphUIDs.length;
		double numGraphPairs = (numGraphs*numGraphs - numGraphs)/2;
		int i=0, j=1;


		long workersStarted = 0;
		double totalTasks = numGraphPairs;
		double interval = totalTasks /1000; // 0,1% Schrittweite	

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
						if(workersStarted % interval == 0) L.info("PROGRESS: " + (100.0 * workersStarted / totalTasks) +  " %");
					}catch(Exception ae){L.info("error in progress printer", ae);}
					
					
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
						i++;
						if(i==numGraphs){
							break;
						}else {
							j=i+1;
						}
					}
				
				}else{
					// do nothing
				}
			}else{
				L.info("all data files distributed to workers");
				
				// do post-processing using a dummy worker: close streams, do final logging etc.
				Worker lastMan = new Worker(new DataChunk("#dummy data entry#"), algoClass, L);
				lastMan.cleanUpFinally();
				
				break;
			}
			
		}
		
		

	}

	public static List<String> listRecursive(String fileName){
		File f = new File(fileName);
		LinkedList<String> files = new LinkedList<String>();
		if(f.isDirectory()){
			for(File ff: f.listFiles()){
				files.addAll(listRecursive(ff.getAbsolutePath()));
			}
		}else if(f.isFile()){	
			files.add(f.getAbsolutePath());
			L.info("file added: " + f);
		}else{
			L.warn("neither dir nor file: " + f.getAbsolutePath());
		}
		return files;
	}


}
