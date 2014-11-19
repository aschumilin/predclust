package graph;

import java.io.Serializable;

import com.google.gson.annotations.Expose;

public class Ref implements Serializable {

	private static final long serialVersionUID = 2903064202001923035L;
	@Expose
	private String URI;
	@Expose
	private String displayName;
	@Expose
	private String knowledgeBase;
	@Expose
	private double weight;
	
	
	public Ref(String uri, String displayName, String knowledgeBase, double weight) {
		URI = uri;
		this.displayName = displayName;
		this.knowledgeBase = knowledgeBase;
		this.weight = weight;
	}


	public String getURI() {
		return URI;
	}


	public String getDisplayName() {
		return displayName;
	}


	public String getKnowledgeBase() {
		return knowledgeBase;
	}
	
	public double getWeight(){
		return weight;
	}
	
	
}
