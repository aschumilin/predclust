package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;

public class ZipStreamer {

public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
	ObjectInputStream oos = new ObjectInputStream(new FileInputStream("/home/pilatus/WORK/pred-clust/data/dbp-categories/4449790Entity-Category.map_String-ArrayListOfString"));
	
	System.out.println("start");
	TreeMap<String, ArrayList<String>> catsmap = (TreeMap<String, ArrayList<String>>) oos.readObject();
	System.out.println("fin");
	String in = "";
	Scanner scanner = new Scanner(System.in);
	while (true) {
		System.out.println("Please input:");
		in = scanner.nextLine().trim();
		if (in.startsWith("exit")) {
			break;
		}
		System.out.println(in);
		System.out.println(catsmap.get(in).toString());
	}
	scanner.close();
}
	public static void main1(String[] args) throws IOException, InterruptedException {
		FileWriter fw = new FileWriter("/home/pilatus/Desktop/RES.csv");


		File f = new File("/home/pilatus/Desktop/Z.zip");
		util.BufferedZipFileWriter zos = new util.BufferedZipFileWriter(f.getAbsolutePath(), 0.025, "second.csv");
		System.out.println("before loop "+ f.length());
		
		for (int i =0; i<100000; i++){
			zos.writeLine((" 1 down vote favorite.I am trying to make my Java program interact with Linux bash but something goes wrong. I have a simple executable prog that reads the one integer from stdin and outputs its square. Executing")
					);
			
			String result = i+ "," + f.length();
			System.out.println(result);		
			fw.write(result + "\n");

		}
fw.close();
zos.close();

	
		
		
		
	
	
	}

}
