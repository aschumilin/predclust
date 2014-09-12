package graph;

import java.util.LinkedList;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class SentenceGraphContainer {
	private String sentence;
	private LinkedList<DirectedSparseMultigraph<Argument, Role>> graph;
	
	public SentenceGraphContainer(String sentence, LinkedList<DirectedSparseMultigraph<Argument, Role>> graph){
		this.sentence = sentence;
		this.graph = graph;
//		DirectedSparseMultigraph d = new DirectedSparseMultigraph<Argument, Role>();
//		
	}

	public String getSentence() {
		return sentence;
	}

	public LinkedList<DirectedSparseMultigraph<Argument, Role>> getGraph() {
		return graph;
	}
	
	
}
