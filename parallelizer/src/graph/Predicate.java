package graph;

import java.io.Serializable;

import com.google.gson.annotations.Expose;


public class Predicate extends Argument implements Serializable {

	private static final long serialVersionUID = 3916606925750377816L;
	
	@Expose
	private String pos;
	
	private boolean sentenceRootFlag;
	private boolean graphRootFlag;

	public Predicate(boolean isSentenceRoot, String id, String pos, String displayName, String mention, boolean isGraphRoot) {
		
		super(id, displayName, mention);
		this.pos = pos;
		sentenceRootFlag = isSentenceRoot;
		graphRootFlag = isGraphRoot;
	}

	
	@Override
	public boolean isPredicate() {
		return true;
	}
	@Override
	public boolean isSentenceRoot() {
		return sentenceRootFlag;
	}	
	@Override
	public boolean isGraphRoot(){
		return graphRootFlag;
	}


	public String toString(){
		String label = "(" + super.getMention() + ") " + 
				"[" + super.getDisplayName() + "] " + 
				 pos;
		if (graphRootFlag){			
			return "<R> "+ label;
		}else{
			return "<N-R> "+ label;
		}
	}

}
