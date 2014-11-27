package algo;


import org.apache.log4j.Logger;

import parallelizer.CoordinatorSimMatSparse;
import parallelizer.Parallelizable;
import parallelizer.Worker;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import graph.Argument;
import graph.Role;
import graph.SimilarityMeasure;

/**
 * @author Artem
 * 
 * !!! Values on diagonal not calculated !!!
 * i,j,
 *
 */
public class GraphSimilaritySparse extends Parallelizable {





	@Override
	public void runAlgo(String jobDescription, Logger L) {
		// 12,102,graphIDx,graphIDy
		String[] parts = jobDescription.split(",");
//		String rowID 		= parts[0];
//		String colID 		= parts[1];
//		String graphFile1 	= parts[2];
//		String graphFile2	= parts[3];
		
		@SuppressWarnings("unchecked")
		DirectedSparseGraph<Argument, Role> g1 = (DirectedSparseGraph<Argument, Role>) CoordinatorSimMatSparse.GRAPHS[Integer.parseInt(parts[0])];
		@SuppressWarnings("unchecked")
		DirectedSparseGraph<Argument, Role> g2 = (DirectedSparseGraph<Argument, Role>) CoordinatorSimMatSparse.GRAPHS[Integer.parseInt(parts[1])];

		// 0.  read graph pair
//		try{
//			g1 = graphreader.GraphReader.readOneGraphFromFile(graphsSourceDir + graphFile1);
//			g2 = graphreader.GraphReader.readOneGraphFromFile(graphsSourceDir + graphFile2);
//
//		}catch(Exception e){
//			L.error("\t can not read graphs..." , e);
//			Worker.decrBusyWorkers();
//			return;
//		}
		




		try{
			double[] tempResults = new double[CoordinatorSimMatSparse.simMeasureNames.length];
			
			Argument root1 = SimilarityMeasure.getRoot(g1);
			Argument root2 = SimilarityMeasure.getRoot(g2);

			double simSum = 0;

			double m1Value 		= SimilarityMeasure.m1PredicateDisplName(root1, root2);
			tempResults[0] 		= m1Value;
			simSum 				+= m1Value;

			double m1_2Value 	= SimilarityMeasure.m1PartialPredicateDisplName(root1, root2); 
			tempResults[1] 		= m1_2Value;
			simSum 				+= m1_2Value;

			double m2Value	 	= SimilarityMeasure.m2WordnetSynset(root1,  root2);
			tempResults[2]		= m2Value;
			simSum 				+= m2Value;

			double m4Value		= SimilarityMeasure.m4OutNeighbourNodes(g1, g2, root1, root2);
			tempResults[3] 		= m4Value;
			simSum 				+= m4Value;

			double m6Value		= SimilarityMeasure.m6OutRoleLabels(g1, g2, root1, root2);
			tempResults[4] 		= m6Value;
			simSum 				+= m6Value;

			double m8Value		= SimilarityMeasure.m8OutRoleLabelsAndNodes(g1, g2, root1, root2);
			tempResults[5]		= m8Value;
			simSum 				+= m8Value;

			double mJCValue 	= SimilarityMeasure.simJaccardCategories(g1, g2, root1, root2);
			tempResults[6] 		= mJCValue;
			simSum 				+= mJCValue;
			
			tempResults[7] 	= simSum;
			

			
			CoordinatorSimMatSparse.updateResultBuffers(parts[0], parts[1], tempResults);

			// private static String[] 	simMeasureNames 			= new String[]{"m1", "m1_2", "m2", "m4", "m6", "m8", "mJC", "mSum"};
			
			

			Worker.decrBusyWorkers();

		}catch(Exception e){
			L.error("some exception during similarity calculation: " + jobDescription, e);
			Worker.decrBusyWorkers();
			return;
		}
	}



	@Override
	public void cleanUpFinally(Logger L){
		L.info("writing results to csv files");

		

		L.info("done cleaning up");

	}

}
