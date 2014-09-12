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
	 * @return 
	 */
	
	/**
	 * For a given srl node, find the annotation with the highest weight.
	 * @param mentions List of int[]{from,to}
	 * @return String[]{URL, mention, weight, from, to} of the best entity found between the indices OR null if no entity annotation found
	 */
	public String[] getBestAnnotation(List<int[]> mentions){
		String[] bestEntity = null;
		
//		LinkedList<String[]> candidates = new LinkedList<String[]>();
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
		String[] result = ind.getBestAnnotation(from, to);
		if(result != null){
			System.out.println(result[0] +  " weight: " + result[2]);
		}
	}
	
}
