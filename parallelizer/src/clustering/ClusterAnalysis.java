package clustering;



public class ClusterAnalysis {
	public static void main(String[] args){
		Clusterer spectralClustering = new SpectralClustering(options);
		spectralClustering.feedData(data); // double[][] or RealMatrix
		spectralClustering.clustering();
		display(spectralClustering.getIndicatorMatrix());
	}
}
