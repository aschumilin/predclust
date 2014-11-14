package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.mongodb.DBObject;

public class SrlUtfTest {

	static int updateThreshold = 100;
	static int updateIntervalShort = 5;
	static int numAtReset = 0;
	static int updateIntervalLong = 1000;
	
	
	private static final void sendHeartBeat(int i){
		if (i < updateThreshold){
			// short intervals
			if(numAtReset == updateIntervalShort){
				numAtReset = 0;
				System.out.println("UP: " + i );
				
			}else{
				numAtReset++;
			}
		}else{
			// long intervals
			if(numAtReset == updateIntervalLong){
				numAtReset = 0;
				System.out.println("UP: " + i);

			}else{
				numAtReset++;
			}
		}

	}
	
	public static void main(String[] args) throws IOException, InterruptedException {

		/***************************/
		// very bad hearbeat example
//		timeOfReset = System.currentTimeMillis();	
		//Thread.sleep(2000);		
//		Thread t = new Thread(){
//			public void run(){
//				while(true){
//					if(System.currentTimeMillis() - timeOfReset >= updateInterval){
//						timeOfReset = System.currentTimeMillis();
//						System.out.println("UP: " + i);
//
//					}
//					Thread.yield();
//				}
//			}
//		};
//		t.start();
//		
//		
//		while(true){
//			try{
//				i++;
//			}catch(Exception e){
//				System.out.println("i overflow");
//				i = 0;
//			}
//			Thread.yield();
//			if (i < 0){
//				break;
//			}
//			
//		}
		/***************************/
		ArrayList<Integer> a = new ArrayList<Integer>(Arrays.asList(1,2,3));
		System.out.println(a);
		Collections.shuffle(a);
		System.out.println(a);
		System.exit(1);
		for (int i=0;i<=100000; i++){
			Thread.sleep(10);
			sendHeartBeat(i);
		}
	
		
		String[] id = new String[]{"http://en.wikipedia.org/wiki?curid=163883", 
			    				"http://en.wikipedia.org/wiki?curid=125903",
			    				"http://es.wikipedia.org/wiki?curid=4945504"};
		
		util.Mongo m = new util.Mongo("remus","docs");
		
		DBObject doc = m.getById(id[0]);
		
		String text = (String) doc.get("docText");
		String lang = (String) doc.get("lang");
		
		String ok = "This is <br>bullshit.";
		String nok = "La versión en vinilo contiene además dos pistas extras.";
		
//		
		text = StringUtils.replaceEach(text, new String[]{"<",  ">"}, new String[]{"", ""});
//		text = nok;
		
//		String srl = new util.CallSRL().makeUtfCall(text, lang);

		m = new util.Mongo("romulus","srl");
		List<DBObject> l = m.execQuery("{_id:\"http://en.wikipedia.org/wiki?curid=163883\"}");
System.out.println(l.get(0).get("docTitle").toString());
		
	}

}
