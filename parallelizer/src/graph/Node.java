package graph;

import java.io.Serializable;

public class Node extends Argument implements Serializable {
	

	private static final long serialVersionUID = 6961741340587011057L;
	
	
	private String type;

	
	public Node(String id, String type, String displayName, String mention) {
		super(id, displayName, mention);	
		this.type = type;
	}
	
	@Override
	public boolean isPredicate() {
		return false;
	}
	@Override
	public boolean isRoot(){
		return false;
	}


	public String getType() {
		return type;
	}




	public String toString(){
//		return "[" + super.getId() + "]" + super.getLemma() +"(" + super.getPos() +")";
		return "[" + super.getId() + "]" + super.getDisplayName();
	}
	
	
}


