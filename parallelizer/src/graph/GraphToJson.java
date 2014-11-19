package graph;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.*;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class GraphToJson {

	/**
	 * @param args
	 */

	private static final Gson gson = new GsonBuilder()
										.excludeFieldsWithoutExposeAnnotation()
										.setPrettyPrinting()
										.create();

	public static void main(String[] argss) {


		String gf = "/home/pilatus/Desktop/en-2616303-1-1.graph";

		//String gf = "/home/pilatus/Dropbox/misc/en-1000677_es-998524/es-998524-10-1.graph";
		DirectedSparseGraph<Argument, Role> g = graphreader.GraphReader.readOneGraphFromFile(gf);
		test.GRAPHTESTER.visGraph(g, "test");
		Argument root = SimilarityMeasure.getRoot(g);

		Argument p2 = new Predicate(false, "p2", "VB", "dn2", "ment2", false);
		Argument n4 = new Node("n4", "word", "dn4", "ment4");
		Argument n5 = new Node("n5", "word", "dn5", "ment5");
		Role r2 = new Role("r --> p2");
		Role r4 = new Role("test4");
		Role r5 = new Role("test5");
		Role r6 = new Role("strange");
		g.addEdge(r2, root, p2);
		g.addEdge(r4, p2, n4);
		g.addEdge(r5, p2, n5);
		g.addEdge(r6,  root, n5);
		test.GRAPHTESTER.visGraph(g, "test2");
		
		System.out.println(makeJson(root, g).toString());



		for(Role r : g.getInEdges(p2)){
			System.out.println(r.getRole());
		}


	}


	/**
	 * @param arg root predicate of the graph
	 * @param graph 
	 * @return StringBuffer with the complete json representation of the graph
	 */
	public static final StringBuffer makeJson(Argument arg, DirectedSparseGraph<Argument, Role> graph){

		Collection<Role> incomingEdges = graph.getInEdges(arg);
		Role role = null;
		if (incomingEdges.size() > 0){
			role = (Role)incomingEdges.toArray()[0];
		}
		
		StringBuffer b = new StringBuffer("{");


		if (arg.isPredicate()){

			// { "role" : "A1" , "predicate" : {..}, "arguments":[] }
 

			// take care of empty role for the root predicate
			if(arg.isGraphRoot()) {
				b.append("\"role\" : \"<ROOT-PREDICATE>\",");
			}else {
				b.append("\"role\" : \"").append(role.getRole()).append("\",");
			}

			
			
			b.append("\"predicate\" : ").append(gson.toJson(arg)).append(",");

			b.append("\"arguments\":[");

			for (Argument a : graph.getSuccessors(arg)){
				b.append(makeJson(a, graph)).append(",");
			}
			// delete the last comma
			b.deleteCharAt(b.length()-1);

			b.append("]");

		}else{
			// { "role" : "A1" , "node" : {..} }
			b.append("\"role\" : \"").append(role.getRole()).append("\",");
			b.append("\"node\" : ").append(gson.toJson(arg));
		}

		b.append("}");
		return b;
	}
}