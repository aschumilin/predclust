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
public class CoordinatorAWS {
	public static long start = 0;
	private final static Logger L = Logger.getLogger(CoordinatorAWS.class); 
	  
	private static final String wid		= System.getProperty("workerid");	
	private static final String resultDir 		= System.getProperty("resultdir");
	private static final String joblistFileName = System.getProperty("joblist");
	
	private static int maxNumWorkers;

	private static Stack<DataChunk> dataSet = null;
	private static Parallelizable algoClass = null;
	

	private static final int updateIntervalShort = 50; // every 100 docs
	private static final int updateIntervalLong = 500; 
	private static final int updateThreshold = 200; // nach 200 fertigen Docs, mache update nur alle 500 Docs
	private static int finishedDocPairs = 0;
	private static int allDocs = 0;
	private static int numAtReset = 0;
	
	public static void main(String[] args) {

		L.info("[" + wid + "] starting");
		
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

		// arg 3
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(joblistFileName), Charset.forName("UTF-8")));
		} catch (FileNotFoundException fnfe) {
			System.err.println("ERROR: parallelizer.Coordinator: file not found:" + args[3]);
		}
		try {
			if(br!=null){
				dataSet = new DataSet();
				String lineInFilesCollector = null;
				if(MODE.equals("files")){
					// filescollector contains filenames
					while ((lineInFilesCollector = br.readLine()) != null) {
						for(String f : listRecursive(lineInFilesCollector)){
							dataSet.add(new DataChunk(f));
						}
					}
				}else if(MODE.equals("nofiles")){
					// filescollector contains other data chunks, e.g. URLs
					while ((lineInFilesCollector = br.readLine()) != null) {
						
						String[] docIDPair = lineInFilesCollector.trim().split("\\t");
						String resultDirName = resultDir + docIDPair[0] + "_" + docIDPair[1] + "/";
						(new File(resultDirName)).mkdir();
						

						// z.B. /home/user/12-result/en-123_es-456/en-123... _srl.xml 
						dataSet.add(new DataChunk(resultDirName  + docIDPair[0]));
						dataSet.add(new DataChunk(resultDirName  + docIDPair[1]));

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
		
		// init main parallelizer loop
		assert dataSet!=null: "ERROR: Coordinator: dataSet not initialized";
		
		
		int dataSetSize = dataSet.size();
		allDocs = dataSetSize;
		

		
		
			
		
							
		
		
		
		
		
		
		while(true){
			
			// check if there are idle workers
			if (dataSetSize>0){ 
				

				
				
				
				if(Worker.getBusyWorkers() < maxNumWorkers){
					
					Thread t = new Thread(new Worker(dataSet.pop(), algoClass, L));
					dataSetSize--;
					finishedDocPairs++;
					t.start();
//L.warn("+++++++started worker");
					try{ sendHeartBeat(); }catch(Exception e){};

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
	
	


	private static final void sendHeartBeat(){
		if (finishedDocPairs < updateThreshold){
			// short intervals
			if(numAtReset >= updateIntervalShort){
				numAtReset = 0;
				L.info("[" + wid +"] UP: " + finishedDocPairs + " / " + allDocs);

			}else{
				numAtReset++;
			}
		}else{
			// long intervals
			if(numAtReset >= updateIntervalLong){
				numAtReset = 0;
				L.info("[" + wid +"] UP: " + finishedDocPairs + " / " + allDocs);

			}else{
				numAtReset++;
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
			//L.warn("neither dir nor file: " + f.getAbsolutePath());
		}
		return files;
	}


}
