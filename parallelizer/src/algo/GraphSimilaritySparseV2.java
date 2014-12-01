package algo;


import org.apache.log4j.Logger;

import parallelizer.CoordinatorSimMatSparseV2;
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
 * 
 * line-wise worker !!! (not cell-wise)
 *
 */
public class GraphSimilaritySparseV2 extends Parallelizable {





	@Override
	public void runAlgo(String jobDescription, Logger L) {
		// jobDescription is line number in similarity matrix
		
//		L.info("STARTING line "+ jobDescription);
		
		int row = Integer.parseInt(jobDescription);
		@SuppressWarnings("unchecked")
		DirectedSparseGraph<Argument, Role> g1 = (DirectedSparseGraph<Argument, Role>) CoordinatorSimMatSparseV2.GRAPHS[row];
		

		for (int col = row+1; col < CoordinatorSimMatSparseV2.GRAPHS.length; col++){
			@SuppressWarnings("unchecked")
			DirectedSparseGraph<Argument, Role> g2 = (DirectedSparseGraph<Argument, Role>) CoordinatorSimMatSparseV2.GRAPHS[col];

			try{
				double[] tempResults = new double[CoordinatorSimMatSparseV2.simMeasureNames.length];
				
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

//				double m6Value		= SimilarityMeasure.m6OutRoleLabels(g1, g2, root1, root2);
//				tempResults[4] 		= m6Value;
///////////////////////////////////////////////////////////////////////////////////////////
				tempResults[4] 		= 0.0;
///////////////////////////////////////////////////////////////////////////////////////////
//				simSum 				+= m6Value;

				double m8Value		= SimilarityMeasure.m8OutRoleLabelsAndNodes(g1, g2, root1, root2);
				tempResults[5]		= m8Value;
				simSum 				+= m8Value;

				double mJCValue 	= SimilarityMeasure.simJaccardCategories(g1, g2, root1, root2);
				tempResults[6] 		= mJCValue;
				simSum 				+= mJCValue;
				
				tempResults[7] 	= simSum;
				

				
				CoordinatorSimMatSparseV2.updateResultBuffers(jobDescription, ""+col, tempResults);

				// private static String[] 	simMeasureNames 			= new String[]{"m1", "m1_2", "m2", "m4", "m6", "m8", "mJC", "mSum"};
				
				

				

			}catch(Exception e){
				L.error("some exception during similarity calculation: " + jobDescription, e);
				Worker.decrBusyWorkers();
				return;
			}
		} // loop over one whole matrix row
		
		L.info("FINISHED line " + jobDescription );
		
		Worker.decrBusyWorkers();
		
	}



	@Override
	public void cleanUpFinally(Logger L){
		L.info("writing results to csv files");

		

		L.info("done cleaning up");

	}

}
