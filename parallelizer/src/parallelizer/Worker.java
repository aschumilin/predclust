package parallelizer;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

/**
 * @author aschumilin
 *
 */
public class Worker implements Runnable{

	private static int busyWorkers = 0;
	
	private DataChunk data = null;;
	private Parallelizable task = null;
	private String timeStarted = "not started";
	private Logger logger = null;
	
	
	Worker(DataChunk data, Parallelizable task, Logger L){
		this.data = data;
		this.task = task;
		logger = L;
	}

	
	@Override
	public void run() {
		Worker.incrBusyWorkers();
//		logger.info(" worker starting ..." + this.toString());
		task.runAlgo(data.getDataFileName(), logger);		
	}

	@Override
	public String toString() {
//		return MessageFormat.format("{0} started {1}", data.getDataFileName(), timeStarted);
		return data.getDataFileName();
	}
	
	public static synchronized int getBusyWorkers(){
		return busyWorkers;
	}
	public static synchronized void incrBusyWorkers(){
		busyWorkers+=1;
	}
	public static synchronized void decrBusyWorkers(){
		busyWorkers-=1;
	}
	
	public void cleanUpFinally(){
		task.cleanUpFinally(logger);
	}
	
}
