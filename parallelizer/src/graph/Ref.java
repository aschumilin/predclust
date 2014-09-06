package graph;

import java.io.Serializable;

public class Ref implements Serializable {

	private static final long serialVersionUID = 2903064202001923035L;
	private String URI;
	private String displayName;
	private String knowledgeBase;
	
	
	public Ref(String uri, String displayName, String knowledgeBase) {
		URI = uri;
		this.displayName = displayName;
		this.knowledgeBase = knowledgeBase;
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
	
	
	
	
}
