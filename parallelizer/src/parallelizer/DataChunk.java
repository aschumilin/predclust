package parallelizer;

/**
 * @author aschumilin
 * 
 * 
 *
 */
public class DataChunk { 

	private String 		dataFileName;
	private boolean 	inProgress;
	private boolean 	done;

	DataChunk(String path){
		dataFileName = path;
		inProgress = false;
		done = false;
	}
	
	
	public void setInProgress(){
		assert !done: "ASSERT ERROR: setInProgress: done is true";
		inProgress = true;
	}
	
	public void setDone(){
		assert inProgress: "ASSERT ERROR: done but not inProgress";
		done = true;
		inProgress = false;
	}
	
	public String getDataFileName(){
		return dataFileName;
	}
	
	
	
	
}
