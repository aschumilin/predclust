package graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Node extends Argument implements Serializable {
	

	private static final long serialVersionUID = 6961741340587011057L;
	
	
	private String type;

	
	public Node(String id, String type, String pos, String lemma,
			String displayName) {
		super(Boolean.FALSE, id, pos, lemma, displayName);
		this.type = type;

	}
	
	
	public void addKBRef(Ref r){
		super.addKBRef(r);
	}



	public String getId() {
		return super.getId();
	}


	public String getType() {
		return type;
	}




	public String toString(){
//		return "[" + super.getId() + "]" + super.getLemma() +"(" + super.getPos() +")";
		return "[" + super.getId() + "]" + super.getDisplayName();
	}
	
	
}


