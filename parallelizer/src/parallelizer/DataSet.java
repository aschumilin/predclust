package parallelizer;

import java.util.Iterator;
import java.util.Stack;

/**
 * @author aschumilin
 *
 */
public class DataSet extends Stack<DataChunk>{
	
	private int size;
	private Stack <DataChunk> chunks;
	
	DataSet(){
		chunks = new Stack<DataChunk>();
	}

	
	public void addDataChunk(DataChunk d){
		chunks.add(d);
		size +=1 ;
	}
	
	public Iterable<DataChunk> getStack(){
		return chunks;
	}
	
	public int getSize(){
		return size;
	}
	
	
	

}
