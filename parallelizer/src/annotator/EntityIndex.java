package annotator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class EntityIndex {
	
	private Map<Integer, List<String[]>> index = new TreeMap<Integer, List<String[]>>();
	
	public EntityIndex(Document annotatorResponse){
		
		List<Element> topics = annotatorResponse.getRootElement().getChild("DetectedTopics").getChildren("DetectedTopic");

//<DetectedTopic URL="http://dbpedia.org/resource/Gun" displayName="Gun" from="653" mention="gun" to="656" weight="0.098"/>
		for (Element top : topics){
			
			int from = Integer.parseInt(top.getAttributeValue("from"));
			int to = Integer.parseInt(top.getAttributeValue("to"));
			
			String[] topicSummary = new String[]{top.getAttributeValue("URL"), 
												top.getAttributeValue("mention"), 
												top.getAttributeValue("weight"), 
												Integer.toString(from), 
												Integer.toString(to)};
			 
			for (int i = from; i<=to;i++){
				
				List<String[]> topicsAtIndex = index.get(i);
				if (topicsAtIndex == null){
					topicsAtIndex = new LinkedList<String[]>();
					topicsAtIndex.add(topicSummary);
				}else{
					topicsAtIndex.add(topicSummary);
				}
				index.put(i, topicsAtIndex);
			}
					

		}
	}
	
	/**
	 * Find best annotation with coordinates exaclty matching 
	 * the srl node coordinates.
	 * @param mentions List of int[]{from,to}
	 * @return String[]{URL, mention, weight, from, to} of the best entity found exactly at the given coordinates OR null if no entity annotation found
	 */
	public String[] getBestEXACTAnnotation(List<int[]> mentions){

		String[] bestEntity = null;
		
		double currentWeight = 0;
		
		for (int[] mention : mentions){
			int from = mention[0];
			int to = mention[1];
			
			// collect all candidates from index
			for (int i=from; i <= to; i++){
				List<String[]> candidates = index.get(i);
				if (candidates != null){
					for (String[] cand : candidates){
						
						
						int candFrom = Integer.parseInt(cand[3]);
						int candTo   = Integer.parseInt(cand[4]);
						
						// !!! consider only the those candidates with exaclty matchig coordinates !!!
						if ( from == candFrom && to == candTo){
							
							// track the one with the highest weight
							double candidateWeight = Double.parseDouble(cand[2]);
							if (candidateWeight > currentWeight){
								bestEntity = cand;
								currentWeight = candidateWeight;
							}
						}
					}
				}	
			}
		}
		
		return bestEntity;	
	}
	
	/**
	 * For a given srl node, find the annotation with the highest weight among the 
	 * the ones that overlap between the coordinates of the given srl node.
	 * 
	 * @param mentions List of int[]{from,to}
	 * @return String[]{URL, mention, weight, from, to} of the best entity found between the indices OR null if no entity annotation found
	 */
	public String[] getBestAnnotation(List<int[]> mentions){
		String[] bestEntity = null;
		
		double currentWeight = 0;
		
		for (int[] mention : mentions){
			int from = mention[0];
			int to = mention[1];
			
			for (int i=from; i <= to; i++){
				List<String[]> candidates = index.get(i);
				if (candidates != null){
					for (String[] cand : candidates){
						double candidateWeight = Double.parseDouble(cand[2]);
						if (candidateWeight > currentWeight){
							bestEntity = cand;
							currentWeight = candidateWeight;
						}
					}
				}	
			}
		}
		
		return bestEntity;		
	}
	
	public static void main(String[] args) throws JDOMException, IOException{
		long anf = System.currentTimeMillis();
		File srlTestFile = new File("/home/pilatus/Desktop/ann-en-test-text.xml");
		SAXBuilder builder = new SAXBuilder();
		Document jdomDoc = (Document) builder.build(srlTestFile);
		EntityIndex ind = new EntityIndex(jdomDoc);
		System.out.println(System.currentTimeMillis() - anf);
		
//		StringBuffer sb = new StringBuffer();
		for (Integer i : ind.index.keySet()){
			System.out.print(i);
//			sb.append(i);
			List<String[]> tops = ind.index.get(i);
			for (String[] top : tops){
				System.out.println("\t" + top[0] +"\t" + top[1] + "\t" + top[2] +"\t" +top[3] +"\t" +top[4]);
//				sb.append("\t" + top[0] +"\t" + top[1] + "\t" + top[2] +"\t" +top[3] +"\t" +top[4] + "\n");
			}
//			System.out.println();
		}
//		FileWriter fw = new FileWriter(new File("/home/pilatus/Desktop/bigAnnot.txt"));
//		fw.write(sb.toString());
//		fw.close();
		int from = 0;
		int to = 100;
//		String[] result = ind.getBestAnnotation(from, to);
//		if(result != null){
//			System.out.println(result[0] +  " weight: " + result[2]);
//		}
	}
	
}
