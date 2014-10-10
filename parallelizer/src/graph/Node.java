package graph;

import java.io.Serializable;
import java.util.List;

public class Node extends Argument implements Serializable {
	

	private static final long serialVersionUID = 6961741340587011057L;
	
	
	private String type; // e.g. word or entity_person/organization.. or entity_other

	
	public Node(String id, String type, String displayName, String mention) {
		super(id, displayName, mention);	
		this.type = type;
	}
	
	@Override
	public boolean isPredicate() {
		return false;
	}
	@Override
	public boolean isSentenceRoot(){
		return false;
	}
	@Override public boolean isGraphRoot(){
		return false;
	}


	public String getType() {
		return type;
	}


	public String toString(){
//		if(type.equals("PREDICATE")){
//			return "[" + super.getId() +"->" + super.getDisplayName() + "]";
//		}else{
		
		String refs = "";
		for(Ref ref : super.getRefs()){
			refs += "[" + ref.getURI() + "(" + ref.getWeight() + ")" + "] ";
		}
		
		String cat = "# ";
		if (super.getCats().size() > 0){
			cat += super.getCats().get(0);
		}

		String label = "(" + super.getMention() + ") " +
				//							"[srl display name] " + super.getDisplayName() + " | " +
				//							"[srl class] " + 		super.getClass() + " | " + 
				//							"[srl node type] " + 	getType() + " | " +
				refs + cat ;

		return label;//"[" + super.getMention() +"] "+ super.getRefs().get(0).getURI() + " (" + super.getRefs().get(0).getWeight() + ")";
		//		}
		
	}
	
	
}


