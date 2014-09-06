package graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Predicate extends Argument implements Serializable {

	private static final long serialVersionUID = 3916606925750377816L;





	public Predicate(boolean isRoot,String id, String pos, String lemma, String displayName) {
		super(isRoot, id, pos, lemma, displayName);

	}
	public String getPos() {
		return super.getPos();
	}
	public String getLemma() {
		return super.getLemma();
	}
	public String getDisplayName() {
		return super.getDisplayName();
	}



	public String toString(){
		if (super.isRoot()){
			return "ROOT<"+super.getId() + ">" + super.getLemma() +"(" + super.getPos() + ")";
		}else{
			return "<"+super.getId() + ">" + super.getLemma() +"(" + super.getPos() + ")";
		}
	}

}
