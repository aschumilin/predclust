package graph;

import java.io.Serializable;


public class Predicate extends Argument implements Serializable {

	private static final long serialVersionUID = 3916606925750377816L;
	
	private String pos;
	private boolean rootFlag;

	public Predicate(boolean isRoot, String id, String pos, String displayName, String mention) {
		
		super(id, displayName, mention);
		this.pos = pos;
		rootFlag = isRoot;
	}

	
	@Override
	public boolean isPredicate() {
		return true;
	}
	@Override
	public boolean isRoot() {
		return rootFlag;
	}	


	public String toString(){
		String label = "(" + super.getMention() + ") " + 
				"[" + super.getDisplayName() + "] " + 
				 pos;
		if (rootFlag){			
			return "<R> "+ label;
		}else{
			return "<N-R> "+ label;
		}
	}

}
