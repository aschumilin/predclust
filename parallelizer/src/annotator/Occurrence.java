package annotator;

public class Occurrence implements Comparable<Occurrence> {

	private String label;
	private int from;
	private int to;
	
	public Occurrence(int startIndex, int endIndex, int num){
		from = startIndex;
		to = endIndex;
		label = "" + from + "-" + to + "-" + num;
	}
	
	public boolean hasOverlap(Occurrence otherOne){
		int from2 = otherOne.getFrom();
		int to2 = otherOne.getTo();
		
		if (from > to2 || to < from2){
			return true;
		}else{
			return false;
		}
		
	}
	@Override
	public int compareTo(Occurrence arg0) {
		// TODO Auto-generated method stub
		return this.label.compareTo(arg0.getLabel());
	}
	
	
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		Occurrence theOtherOne = (Occurrence) obj;
		return this.label.equals(theOtherOne.getLabel());
	}

	public int getFrom(){
		return from;
	}
	
	public int getTo(){
		return to;
	}
	
	public String getLabel(){
		return label;
	}
	

}
