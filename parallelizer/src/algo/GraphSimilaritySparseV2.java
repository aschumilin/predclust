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


public static void main(String[] args) {
	String s1 = "/es/-93-1-1.graph";
String s2 = "en-14531824-100-1.graph";
 System.out.println(s1.substring(s1.lastIndexOf("/") + 1));
 System.out.println(s2.substring(s2.lastIndexOf("/") + 1));

}


	@Override
	public void runAlgo(String jobDescription, Logger L) {
		// jobDescription is line number in similarity matrix
		
//		L.info("STARTING line "+ jobDescription);
		
		int row = Integer.parseInt(jobDescription);
		@SuppressWarnings("unchecked")
		DirectedSparseGraph<Argument, Role> g1 = (DirectedSparseGraph<Argument, Role>) CoordinatorSimMatSparseV2.GRAPHS[row];
		// get the graph file name from the bigger graph id, e.g. 1-graphs/es-4261544_en-14531824/en-14531824-100-1.graph
		String graphID1 = CoordinatorSimMatSparseV2.graphUIDs[row].substring(CoordinatorSimMatSparseV2.graphUIDs[row].lastIndexOf("/") + 1 );

		for (int col = row+1; col < CoordinatorSimMatSparseV2.GRAPHS.length; col++){
			@SuppressWarnings("unchecked")
			DirectedSparseGraph<Argument, Role> g2 = (DirectedSparseGraph<Argument, Role>) CoordinatorSimMatSparseV2.GRAPHS[col];
			String graphID2 = CoordinatorSimMatSparseV2.graphUIDs[col].substring( CoordinatorSimMatSparseV2.graphUIDs[col].lastIndexOf("/") + 1 );
			
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

				double m2Value	 	= SimilarityMeasure.m2WordnetSynset(root1, root2);
				tempResults[2]		= m2Value;
				simSum 				+= m2Value;

				double m4Value		= SimilarityMeasure.m4OutNeighbourNodes(g1, g2, root1, root2);
				tempResults[3] 		= m4Value;
				simSum 				+= m4Value;

				double m62Value		= SimilarityMeasure.m62TransformedOutRoleLabels(g1, g2, root1, root2);
				tempResults[4] 		= m62Value;
				simSum 				+= m62Value;
///////////////////////////////////////////////////////////////////////////////////////////
				// leave m6 metric out of calculation
//				tempResults[4] 		= 0.0;
///////////////////////////////////////////////////////////////////////////////////////////
					

				double m8Value		= SimilarityMeasure.m8OutRoleLabelsAndNodes(g1, g2, root1, root2);
				tempResults[5]		= m8Value;
				simSum 				+= m8Value;

				double m82Value		= SimilarityMeasure.m82TransformedOutRoleLabelsAndNodes(g1, g2, root1, root2);
				tempResults[6]		= m82Value;
				simSum				+= m82Value;
						
				double mJCValue 	= SimilarityMeasure.simJaccardCategories(g1, g2, root1, root2);
				tempResults[7] 		= mJCValue;
				simSum 				+= mJCValue;
				
				tempResults[8] 		= simSum;
				
				tempResults[9]		= SimilarityMeasure.mXLingIndicator(graphID1, graphID2);
				
				tempResults[10]		= simSum - m62Value * 0.3; 		// 70% weight on m62 role labels metric
				tempResults[11]		= simSum - m62Value * 0.5;		// 50% weight on m62 role labels metric
				tempResults[12] 	= simSum - m62Value * 0.7;		// 30% weight on m62 role labels metric
				tempResults[13]		= simSum - m62Value * 0.9;		// 10% weight on m62 role labels metric
				
				CoordinatorSimMatSparseV2.updateResultBuffers(jobDescription, ""+col, tempResults);
				// 										0		1		2	3		4	5		6		7		8		9			10			11			12		13
				//sim measure file names:  new String[]{"m1", "m12", "m2", "m4", "m62", "m8", "m82", "mJC", "mSum1.0", "mXLing", "mSum0.7", "mSum0.5", "mSum0.3", "mSum0.1"}

				
				

				

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
