package graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Argument implements Serializable {


	private static final long serialVersionUID = 1189089332581539650L;
	
	private String id;
	private String pos;
	private String lemma;
	private String displayName;
	private List<Ref> references;
	private boolean isRoot;
	
	
	public Argument(boolean isRoot, String id, String pos, String lemma, String displayName) {
		super();
		this.id = id;
		this.isRoot = isRoot;
		references = new ArrayList<Ref>();
		this.pos = pos;
		this.lemma = lemma;
		this.displayName = displayName;
	
	}
	
	
	
	public String getId(){
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return ((Argument)obj).getId().equals(this.id);
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return id.hashCode();
	}

	public List<Ref> getKBRefs() {
		return references;
	}

	public void addKBRef(Ref r){
		references.add(r);
	}


	public boolean isRoot(){
		return isRoot;
	}

	public String getPos() {
		return pos;
	}




	public String getLemma() {
		return lemma;
	}




	public String getDisplayName() {
		return displayName;
	}




	public List<Ref> getReferences() {
		return references;
	}
	
	
	
	
	
	
}
