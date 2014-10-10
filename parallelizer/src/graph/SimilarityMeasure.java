package graph;

import java.util.ArrayList;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class SimilarityMeasure {

	/**
	 * @param g1
	 * @param g2
	 * @return 1 if both predicates have same WordNet URIs, else 0.
	 */
	public static double sim1WordnetSynset(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2){
		
		// different starting values
		// should get overridden if WordNet IDs are available
		String synsetId1 = "s1", synsetId2 = "s2";
		
		for(Argument node : g1.getVertices()){
			
			if (node.isGraphRoot()){	// get the graph root
				
				// get the WordNet ID if available
				ArrayList<Ref> refs = node.getRefs();
				for (Ref ref : refs){
//					System.out.println("---------" + ref.getKnowledgeBase());
					if(ref.getKnowledgeBase().equals("WordNet-3.0")){
						synsetId1 = ref.getURI();
						break;
					}
				}			
			}
		}
		
		for(Argument node : g2.getVertices()){
			
			if (node.isGraphRoot()){	// get the graph root
				
				// get the WordNet ID if available
				ArrayList<Ref> refs = node.getRefs();
				for (Ref ref : refs){
					
					if(ref.getKnowledgeBase().equals("WordNet-3.0")){
						synsetId2 = ref.getURI();
						break;
					}
				}			
			}
		}
		
		if(synsetId1.equals(synsetId2)){
			return 1;
		}else{
			return 0;
		}
	}

	public static double sim2Jaccard(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2){

		return 0;
	}

	public static double sim3Hyponym(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2){

		return 0;
	}

	
	public static double sim4Categories(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2){
		
		
		return 0;
	}
}
