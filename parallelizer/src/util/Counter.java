package util;
import java.io.Serializable;
import java.util.HashMap;
import org.apache.commons.lang.mutable.MutableInt;


public class Counter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 123423423413L;
	HashMap<String, int[]> counter = null;	
	 
	
	
	public Counter(){
		counter = new HashMap<String, int[]>();
	}

	
	public HashMap<String, int[]> getMap(){
		return counter;
	}
	
	public void add(String key){

		int[] valueWrapper = counter.get(key);
		 
		if (valueWrapper == null) {
			counter.put(key, new int[] { 1 });
		} else {
			valueWrapper[0]++;
		}
		
	}
}
