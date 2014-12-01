package algo;


import org.apache.log4j.Logger;

import parallelizer.CoordinatorSimMatSparseZipped;
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
public class GraphSimilaritySparseZipped extends Parallelizable {




	@SuppressWarnings("unchecked")
	@Override
	public void runAlgo(String jobDescription, Logger L) {
		// jobDescription is line number in similarity matrix

		//		L.info("STARTING line "+ jobDescription);

		int row = Integer.parseInt(jobDescription);
		DirectedSparseGraph<Argument, Role> g1 = (DirectedSparseGraph<Argument, Role>) CoordinatorSimMatSparseZipped.GRAPHS[row];
		DirectedSparseGraph<Argument, Role> g2 = null;
		Argument root1, root2;
		
		for (int col = row+1; col < CoordinatorSimMatSparseZipped.GRAPHS.length; col++){
			g2 = (DirectedSparseGraph<Argument, Role>) CoordinatorSimMatSparseZipped.GRAPHS[col];

			try{
				double[] tempResults = new double[CoordinatorSimMatSparseZipped.simMeasureNames.length];

				root1 = SimilarityMeasure.getRoot(g1);
				root2 = SimilarityMeasure.getRoot(g2);

				double simSum = 0;


				double m1Value 		= SimilarityMeasure.m1PredicateDisplName(root1, root2);
				tempResults[0] 		= Math.round(m1Value*10000)/10000.;
				simSum 				+= m1Value;

				double m1_2Value 	= SimilarityMeasure.m1PartialPredicateDisplName(root1, root2); 
				tempResults[1] 		= Math.round(m1_2Value*10000)/10000.;
				simSum 				+= m1_2Value;

				double m2Value	 	= SimilarityMeasure.m2WordnetSynset(root1,  root2);
				tempResults[2]		= Math.round(m2Value*10000)/10000.;
				simSum 				+= m2Value;

				double m4Value		= SimilarityMeasure.m4OutNeighbourNodes(g1, g2, root1, root2);
				tempResults[3] 		= Math.round(m4Value*10000)/10000.;
				simSum 				+= m4Value;

				//				double m6Value		= SimilarityMeasure.m6OutRoleLabels(g1, g2, root1, root2);
				//				tempResults[4] 		= m6Value;
				///////////////////////////////////////////////////////////////////////////////////////////
				tempResults[4] 		= 0.0;
				///////////////////////////////////////////////////////////////////////////////////////////
				//				simSum 				+= m6Value;

				double m8Value		= SimilarityMeasure.m8OutRoleLabelsAndNodes(g1, g2, root1, root2);
				tempResults[5]		= Math.round(m8Value*10000)/10000.;
				simSum 				+= m8Value;

				double mJCValue 	= SimilarityMeasure.simJaccardCategories(g1, g2, root1, root2);
				tempResults[6] 		= Math.round(mJCValue*10000)/10000.;
				simSum 				+= mJCValue;

				tempResults[7] 	= Math.round(simSum*10000)/10000.;



				CoordinatorSimMatSparseZipped.updateResultBuffers(jobDescription, ""+col, tempResults);

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
