package graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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



		// get the WordNet ID if available
		ArrayList<Ref> refs = getRoot(g1).getRefs();
		for (Ref ref : refs){
			//					System.out.println("---------" + ref.getKnowledgeBase());
			if(ref.getKnowledgeBase().equals("WordNet-3.0")){
				synsetId1 = ref.getURI();
				break;
			}

		}


		// get the WordNet ID if available
		refs = getRoot(g2).getRefs();
		for (Ref ref : refs){

			if(ref.getKnowledgeBase().equals("WordNet-3.0")){
				synsetId2 = ref.getURI();
				break;
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


	/**
	 * @param g1
	 * @param g2
	 * @return Sim value based on relative overlap of the combined category sets of the the nodes adjacent to each graph's root predicate. 
	 */
	public static double sim4Categories(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2){

		Argument root1=getRoot(g1), root2=getRoot(g2);

		ArrayList<String> cats1=new ArrayList<String>();
		ArrayList<String> cats2=new ArrayList<String>();
		
		for(Argument arg : g1.getNeighbors(root1)){
			cats1.addAll(arg.getCats());
		}
		
		for(Argument arg : g2.getNeighbors(root2)){
			cats2.addAll(arg.getCats());
		}
		

		int size1 = cats1.size();
		int size2 = cats2.size();
		
		if (size1>0 && size2>0){
			
			double intersectionSize = 0.0;
						
			// intersection size: iterate over smaller list
			if(size1 <= size2){
				// iterate over 1
				for(String cat : cats1){
					if(cats2.contains(cat)) intersectionSize++;
				}
			}else{
				// iterate over 2
				for(String cat : cats2){
					if(cats1.contains(cat)) intersectionSize++;
				}
			}
			
			
			if(intersectionSize > 0.0){		// dont calculate union if intersection is empty !
				//union size
				double unionSize = size1;
				for(String cat : cats2){
					if(!cats1.contains(cat)) unionSize++;
				}
				System.out.println("s1:" + size1 + " s2:"+ size2 + " u:" + unionSize + " i:" + intersectionSize);
				return intersectionSize / unionSize;
			}else{
				return 0.0;
			}
		}else{
			return 0.0;
		}

	}

	public static void main (String[] args){
		ArrayList<String> a = new ArrayList<String>();
		ArrayList<String> b = new ArrayList<String>();
		a.add("a");
		a.add("a");
		b.add("b");
		b.add("a");
		System.out.println(a);
		System.out.println(b);
		System.out.println(a.retainAll(b));
		System.out.println(a);
	}

	private static Argument getRoot(DirectedSparseGraph<Argument, Role> g){
		for(Argument node : g.getVertices()){
			if (node.isGraphRoot()) return node;	// get the graph root
		}
		return null;
	}
}
