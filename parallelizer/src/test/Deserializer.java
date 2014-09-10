package test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.jdom2.JDOMException;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graph.Argument;
import graph.Role;

public class Deserializer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 	
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
	}

}
