package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ZipStreamer {


	public static void main(String[] args) throws IOException, InterruptedException {
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
