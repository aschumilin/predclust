package graph;

import java.util.ArrayList;
import java.util.Collection;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class SimilarityMeasure {

	/**
	 * GRAPHREADER
	 * 
	 * @param g1
	 * @param g2
	 * @return 1 if both predicates have same WordNet URIs, else 0.
	 */
	
	public static double m1PredicateDisplName(Argument p1, Argument p2){
		// compare predicate display names
		if(p1.getDisplayName().equalsIgnoreCase(p2.getDisplayName())){
			return 1.0;
		}else{
			return 0.0;
		}
	}
	
	public static double m1PartialPredicateDisplName(Argument p1, Argument p2){
		// compare predicate display names without the digits
		// e.g. record.01|tape.02 <-> record.02
		
		String separatorVerbs = "\\|";
		String separatorFlavours = "\\.";
		
		String dispN1 = p1.getDisplayName();
		String dispN2 = p2.getDisplayName();
		
		String[] parts1, parts2;
		parts1 = dispN1.split(separatorVerbs);
		for (int i=0; i<parts1.length; i++){
			parts1[i] = parts1[i].split(separatorFlavours)[0];
		}
		
		
		parts2 = dispN2.split(separatorVerbs);
		for (int i=0; i<parts2.length; i++){
			parts2[i] = parts2[i].split(separatorFlavours)[0];
		}

		
		// return 1 if the two sets have at least on element in intersection
		for(String verb1 : parts1){
			for (String verb2 : parts2){
				if(verb1.equalsIgnoreCase(verb2)) return 1.0;
			}
		}
		return 0.0;
		
	}
	
	public static double m2WordnetSynset(Argument p1, Argument p2){
		// different starting values
		// are overridden if WordNet IDs are available
		String synsetId1 = "s1", synsetId2 = "s2";



		// get the WordNet ID if available
		ArrayList<Ref> refs = p1.getRefs();
		for (Ref ref : refs){
			//					System.out.println("---------" + ref.getKnowledgeBase());
			if(ref.getKnowledgeBase().equals("WordNet-3.0")){
				synsetId1 = ref.getURI();
				break;
			}

		}


		// get the WordNet ID if available
		refs = p2.getRefs();
		for (Ref ref : refs){

			if(ref.getKnowledgeBase().equals("WordNet-3.0")){
				synsetId2 = ref.getURI();
				break;
			}

		}

		if(synsetId1.equals(synsetId2)){
			return 1.0;
		}else{
			return 0.0;
		}
	}

	public static double m2WordnetSynsetOnGraphRoots(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2){

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
	
	public static double m3Hyponyms(Argument p1, Argument p2){
		return 0.0;
	}
	
	public static double m4OutNeighbourNodes(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2, Argument p1, Argument p2){
		
		Collection<Argument> outNeighbours1 = new ArrayList<Argument>();
		Collection<Argument> outNeighbours2 = new ArrayList<Argument>();
		 
		//select l non-root arguments as out-neighbours
		for(Argument arg : g1.getSuccessors(p1)){
			if( ! arg.isPredicate()){
				outNeighbours1.add(arg);
//				System.out.println(arg.getDisplayName());
			}
		}		
		for(Argument arg : g2.getSuccessors(p2)){
			if( ! arg.isPredicate()){
				outNeighbours2.add(arg);
//				System.out.println(arg.getDisplayName());
			}
		}
		
		// calculate Jaccard on neighbours 
		
		return nodesJaccard(outNeighbours1, outNeighbours2);
	}
	
	public static double m6OutRoleLabels(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2, Argument p1, Argument p2){
//		if(r1.getRole().equalsIgnoreCase(r2.getRole())){
//			return 1.0;
//		}else{
//			return 0.0;
//		}
		ArrayList<String> roles1 = new ArrayList<String>(); 
		ArrayList<String> roles2 = new ArrayList<String>();
		
		for(Role role : g1.getOutEdges(p1)){
			roles1.add(role.getRole());
		}
		for(Role role : g2.getOutEdges(p2)){
			roles2.add(role.getRole());
		}
		
		
		return genericJaccard(roles1, roles2);
	}
	
	public static double m8OutRoleLabelsAndNodes(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2, Argument p1, Argument p2){
		// ignore predicate-arguments, just the nodes
		
		Collection <Object[]> c1 = new ArrayList<Object[]>();		
		Collection <Object[]> c2 = new ArrayList<Object[]>();
		
		for (Role role : g1.getOutEdges(p1)){
			Argument arg = g1.getDest(role);
			
			if(! arg.isPredicate()){
				c1.add(new Object[]{role.getRole(), arg});
			}
		}
		
		for (Role role : g2.getOutEdges(p2)){
			Argument arg = g2.getDest(role);
			
			if(! arg.isPredicate()){
				c2.add(new Object[]{role.getRole(), arg});
			}
		}
		
		return rolesNodesJaccard(c1, c2);
		
	}

	/**
	 * @param g1
	 * @param g2
	 * @return Sim value based on relative overlap of the combined category sets of the the nodes adjacent to each graph's root predicate. 
	 */
	public static double simJaccardCategories(DirectedSparseGraph<Argument, Role> g1, DirectedSparseGraph<Argument, Role> g2, Argument p1, Argument p2){

		
		ArrayList<String> cats1=new ArrayList<String>();
		ArrayList<String> cats2=new ArrayList<String>();
		
		for(Argument arg : g1.getSuccessors(p1)){
			cats1.addAll(arg.getCats());
		}
		
		for(Argument arg : g2.getSuccessors(p2)){
			cats2.addAll(arg.getCats());
		}
		double sim = genericJaccard(cats1, cats2);
//		if(sim>0){
//			System.out.println();
//			for (String s:cats1){
//				System.out.println("1\t" + s);
//			}
//			for(String s : cats2){
//				System.out.println("2\t" + s);
//			}
//			System.out.println();
//		}
		return sim ;
/*
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
		*/

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
		System.out.println("a".equals("A"));
		System.out.println("a".equals("a"));

	}

	public static Argument getRoot(DirectedSparseGraph<Argument, Role> g){
		for(Argument node : g.getVertices()){
			if (node.isGraphRoot()) return node;	// get the graph root
		}
		return null;
	}
	
	private static double compareNodes(Argument n1, Argument n2){
		// first, check if they have at least one matching entity reference
		// then, check the display name
		
		
		for(Ref ref1 : n1.getRefs()){
			for(Ref ref2 : n2.getRefs()){
				if(ref1.getURI().equals(ref2.getURI())) return 1.0;
			}
		}
		
		// if no mathich uri found, compare display names
		if(n1.getDisplayName().equalsIgnoreCase(n2.getDisplayName())){
			return 1.0;
		}else{
				return 0.0;			
		}
				
	}
	
	private static double nodesJaccard(Collection<Argument> c1, Collection<Argument> c2){
		int size1 = c1.size();
		int size2 = c2.size();
		
		if (size1>0 && size2>0){	// else return 0
			
			double intersectionSize = 0.0;
						
			// intersection size: iterate over smaller list
			if(size1 <= size2){
				// iterate over 1
				for(Argument n1 : c1){
					for(Argument n2 : c2){
						if (compareNodes(n1, n2) == 1.0){
							intersectionSize ++;
							break;
						}
					}
					
				}
			}else{
				// iterate over 2
				for(Argument arg2 : c2){
					for (Argument arg1 : c1){
						if (compareNodes(arg1, arg2) == 1.0){
							intersectionSize ++;
							break;
						}
						
					}
				}
			}
			
			
			if(intersectionSize > 0.0){		// dont calculate union if intersection is empty !
				//union size
				double unionSize = size1 + size2;
				for(Argument arg2 : c2){
					for(Argument arg1 : c1){
						if(compareNodes(arg1, arg2) == 1.0)	{
							unionSize--;
							break;
						}
					}

				}
				
//				System.out.println("s1:" + size1 + " s2:"+ size2 + " u:" + unionSize + " i:" + intersectionSize);
				return intersectionSize / unionSize;
			}else{	// intersection is 0
				return 0.0;
			}
		}else{ 
			return 0.0;
		}
	}
	
	private static double compareRolesAndNodes(String role1, String role2, Argument n1, Argument n2){
		// first, check the equality of role Strings
		// then compare the nodes as in compareNodes()
		
		if(role1.equalsIgnoreCase(role2)){
			return compareNodes(n1, n2);
		}else{
			return 0.0;
		}
	}
	
	private static double rolesNodesJaccard(Collection<Object[]> c1, Collection<Object[]> c2){
		// Collection<Object[]> c1 is a list of Object[]{role string, adjacent argument(node)}
		
		int size1 = c1.size();
		int size2 = c2.size();
		
		if (size1>0 && size2>0){	// else return 0
			
			double intersectionSize = 0.0;
						
			// intersection size: iterate over smaller list
			String role1=null, role2=null;
			Argument arg1=null, arg2=null;
			if(size1 <= size2){
				// iterate over 1 if it has less size
				
				
				for(Object[] roleNodeContainer1 : c1){
					
					role1 = (String) roleNodeContainer1[0];
					arg1 = (Argument) roleNodeContainer1[1];
					
					for(Object[] roleNodeContainer2 : c2){
						
						role2 = (String) roleNodeContainer2[0];
						arg2 = (Argument) roleNodeContainer2[1];
						
						if (compareRolesAndNodes(role1, role2, arg1, arg2) == 1.0){
							intersectionSize ++;
							break;
						}
					}
					
				}
			}else{
				// iterate over 2 if it has less size
				for(Object[] roleNodeContainer2 : c2){
					
					role2 = (String) roleNodeContainer2[0];
					arg2 = (Argument) roleNodeContainer2[1];
					
					for (Object[] roleNodeContainer1 : c1){
						
						role1 = (String) roleNodeContainer1[0];
						arg1 = (Argument) roleNodeContainer1[1];
						
						if (compareRolesAndNodes(role1, role2, arg1, arg2) == 1.0){
							intersectionSize ++;
							break;
						}
						
					}
				}
			}
			
			
			if(intersectionSize > 0.0){		// dont calculate union if intersection is empty !
				//union size
				double unionSize = size1 + size2;
				
				for(Object[] roleNodeContainer2 : c2){
					role2 = (String) roleNodeContainer2[0];
					arg2 = (Argument) roleNodeContainer2[1];
					
					for(Object[] roleNodeContainer1 : c1){
						
						role1 = (String) roleNodeContainer1[0];
						arg1 = (Argument) roleNodeContainer1[1];
						
						if(compareRolesAndNodes(role1, role2, arg1, arg2) == 1.0)	{
							unionSize --;
							break;
						}
					}

				}
				
//				System.out.println("s1:" + size1 + " s2:"+ size2 + " u:" + unionSize + " i:" + intersectionSize);
				return intersectionSize / unionSize;
			}else{	// intersection is 0
				return 0.0;
			}
		}else{ 
			return 0.0;
		}
	}
	
	private static <T> double genericJaccard(Collection<T> c1, Collection<T> c2){
		int size1 = c1.size();
		int size2 = c2.size();
		
		if (size1>0 && size2>0){
			
			double intersectionSize = 0.0;
						
			// intersection size: iterate over smaller list
			if(size1 <= size2){
				// iterate over 1
				for(T cat : c1){
					if(c2.contains(cat)) intersectionSize++;
				}
			}else{
				// iterate over 2
				for(T cat : c2){
					if(c1.contains(cat)) intersectionSize++;
				}
			}
			
			
			if(intersectionSize > 0.0){		// dont calculate union if intersection is empty !
				//union size
				double unionSize = size1;
				for(T cat : c2){
					if(!c1.contains(cat)) unionSize++;
				}
				
//				System.out.println("s1:" + size1 + " s2:"+ size2 + " u:" + unionSize + " i:" + intersectionSize);
				return intersectionSize / unionSize;
			}else{
				return 0.0;
			}
		}else{
			return 0.0;
		}
	}
	
}
