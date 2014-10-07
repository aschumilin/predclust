package test;

import java.util.ArrayList;
import java.util.List;

public class Deserializer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 	/*
		try{
			// don't use buffering
			InputStream file = new FileInputStream("/home/pilatus/Desktop/graph.ser");
//			OutputStream buffer = new BufferedOutputStream(file);
			ObjectInput input = new ObjectInputStream(file);//buffer);
			try{
				ArrayList<DirectedSparseMultigraph<Argument,Role>> graph= (ArrayList<DirectedSparseMultigraph<Argument,Role>>)input.readObject();
				System.out.println("finished de-serializing");
				
				
				for (DirectedSparseMultigraph g : graph){
					test.GRAPHTESTER.visGraph(g, "some title");
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally{input.close();}
		}  
		catch(IOException ioe){
			System.out.println("could not serialize annotations map: ");
		}
		*/
		List<Integer> x = new ArrayList<Integer>();
		
		x.add(new Integer(1));
		x.addAll(null);
		


	}

}
