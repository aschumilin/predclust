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
public class Coordinator {
	public static long start = 0;
	private final static Logger L = Logger.getLogger(Coordinator.class); 
	private static RollingFileAppender fa = new RollingFileAppender();
	  
	private static final int PROCS = Runtime.getRuntime().availableProcessors();
	private static int maxNumWorkers;

	private static Stack<DataChunk> dataSet = null;
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
		Logger.getLogger(Coordinator.class).addAppender(fa);
		  
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
		
		// TODO: fix external class file loading
		// can't load external class files: ClassCastException		
		// works in eclipse, but not with executable jar !!!
		
		// arg 3
//		File algoClassFile = new File(args[1]);
//		assert algoClassFile.exists(): "Error: Coordinator main: algo class file does not exist";
//		
//		try {
//			URL url = algoClassFile.toURI().toURL();
//			URL[] urls = new URL[]{url};
//			ClassLoader cl = new URLClassLoader(urls);
//			try {
//		
//				
//		
//				algoClass = (Parallelizable)cl.loadClass(algoClassName).newInstance();
//			} catch (InstantiationException e) {
//				L.fatal("exception while loading algo class:");
//				e.printStackTrace();
//				System.exit(1);
//			} catch (IllegalAccessException e) {
//				L.fatal("exception while loading algo class:");
//				e.printStackTrace();
//				System.exit(1);
//			}
//		}catch(MalformedURLException mue){
//			L.fatal("malformed expression");
//			mue.printStackTrace();
//			System.exit(1);
//		}catch (ClassNotFoundException e) {
//			L.fatal("algo class not found");
//			e.printStackTrace();
//			System.exit(1);
//		}
//		assert algoClass!=null: ("Coordinator: algoClass not loaded");

		// arg 1
		maxNumWorkers = Integer.parseInt(args[1]);
		
		// arg 2 mode: files/ nofiles
		final String MODE = args[2];

		// arg 3
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(args[3]), Charset.forName("UTF-8")));
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
						dataSet.add(new DataChunk(lineInFilesCollector));						
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
		
		L.info("dataset size: " + dataSet.size());
		L.info("maxNumWorkers: " + maxNumWorkers);
		L.info("worker class: " + algoClassName);
		
		Coordinator.start = System.currentTimeMillis();
		long dataSetSize = dataSet.size();
		
		long ticks = 1000;
		long step = dataSetSize / ticks;
		long workersStarted = 0;
		while(true){
//			if(System.currentTimeMillis()%20000==0){
//				L.info(Worker.getBusyWorkers() + " busy workers " + " / " + dataSet.size() + " files left");
//			}
			
			
			// check if there are idle workers
			if (dataSetSize>0){ 
				// print progress info
				if(step>0 && workersStarted % step == 0){
					L.info("PROGRESS: " + Math.round(100.0 * workersStarted / dataSetSize) +  " %");
				}
				
				if(Worker.getBusyWorkers() < maxNumWorkers){
//					int bef = Worker.getBusyWorkers();
					Thread t = new Thread(new Worker(dataSet.pop(), algoClass, L));
					workersStarted++;
					dataSetSize--;
					t.start();
//					int aft = Worker.getBusyWorkers();
//					L.info("----- # busy before/after: " + bef+"/"+aft);
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
