package parallelizer;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import annotator.AnnotationService;

public class AnnotServicePool {

	// Object [Service Instance Pointer] [Boolean serviceIsFree]
	private Map<String,Object[]> services;
	Logger L;
	
	public AnnotServicePool(int numServices, String ConfDirPath, Logger logger) throws Exception{
		L = logger;
		int enServs = numServices / 2;
		int esServs = numServices - enServs;	
		services = new TreeMap<String, Object[]>();
		
		for(int i = 0; i<enServs; i++){
			L.info("AnnotServicePool: init annot service EN" + i);
			
			String servId = "en" + i;
			Object[] service = new Object[]{new AnnotationService(
									ConfDirPath+"/hub-template.xml",
									ConfDirPath+"/wikipedia-template-en.xml", 
									"en", "en", "dbpedia"), new Boolean(true)};
			services.put(servId, service);
			
		}
		
		for(int j = 0; j<esServs; j++){
			L.info("AnnotServicePool: init annot service ES" + j);
			
			String servId = "es" + j;	
			Object[] service = new Object[]{new AnnotationService(
					ConfDirPath+"/hub-template.xml",
					ConfDirPath+"/wikipedia-template-es.xml", 
					"es", "en", "dbpedia"), new Boolean(true)};
			services.put(servId, service);
		}
	}


	/**
	 * @param lang
	 * @param docKey
	 * @return Object []{service ref, String serviceId} of the free service OR null if no free service found
	 */
	public synchronized Object[] getFreeService(String lang, String docKey){
		
		if (! (lang.equals("en") || lang.equals("es"))){
			throw new IllegalArgumentException("<" + docKey + ">\t wrong lang code: " + lang);
		}else{
			
			for(String servId : services.keySet()){
				if(servId.startsWith(lang)){
					Object[] serv = services.get(servId);
					Boolean isFree = (Boolean) serv[1];
					if (isFree == true){
						// SET BUSY FLAG AFTER PROCESSING
						serv[1] = new Boolean(false);
System.out.println("\t" + servId + " doing " + docKey);
						return new Object[]{serv[0], servId};
					}
				}
			}
			return null;
		}
	}
	
	public synchronized void setServiceFree(String serviceId){
		// set free flag to true
		Object [] res = services.get(serviceId);
		res[1] = new Boolean(true);
	}
	public synchronized void setServiceBusy(String serviceId){
		Object [] res = services.get(serviceId);
		res[1] = new Boolean(false);
	}
	
}
