package parallelizer;

import org.apache.log4j.Logger;


public abstract class Parallelizable {
	
	
	/**
	 * @param dataFilePAth item to be processed by this Worker. Can be a filepath, a dbID, etc. 
	 * @param L Global log4j Logger.
	 */
	public void runAlgo(String dataFilePAth, Logger L){}
	public void runAlgo(String dataFilePAth){}
	public void runAlgo(String dataFilePAth, Logger L, Worker W){}

	
	/**
	 * This method is called by Coordinator after all Workers are finished.
	 * @param L Global log4j Logger.
	 */
	public void cleanUpFinally(Logger L){}
	
	

}
