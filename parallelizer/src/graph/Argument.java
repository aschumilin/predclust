package graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Argument implements Serializable {


	private static final long serialVersionUID = 1189089332581539650L;
	
	private String id;
	private String mention;
	private String displayName;
	private ArrayList<Ref> references;
	private ArrayList<String> dbpCategories;
	
	
	public Argument(String id, String displayName, String mention) {
		super();
		this.id = id;
		this.mention = mention;
		this.displayName = displayName;
		references = new ArrayList<Ref>();
		dbpCategories = new ArrayList<String>();
	}
	
	
	public abstract boolean isSentenceRoot();
	public abstract boolean isGraphRoot();
	public abstract boolean isPredicate();
	
	
	public String getId(){
		return id;
	}
	public String getDisplayName() {
		return displayName;
	}
	public String getMention(){
		return mention;
	}
	public ArrayList<String> getCats(){
		return dbpCategories;
	}

	@Override
	public boolean equals(Object obj) {
		return ((Argument)obj).getId().equals(this.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}


	public void addCat(String c){
		dbpCategories.add(c);
	}
	
	public void addRef(Ref r){
		references.add(r);
	}
	public ArrayList<Ref> getRefs() {
		if(references==null){
			return new ArrayList<Ref>();
		}else{
			return references;
		}
	}
	
}
