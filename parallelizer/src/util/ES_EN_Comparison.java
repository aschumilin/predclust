package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeSet;

public class ES_EN_Comparison {

	static String enFilePath = "/home/pilatus/WORK/pred-clust/data/ES/en-articles.txt";
	static String llFilePath = "/home/pilatus/WORK/pred-clust/data/ES/es-en-langlinks.txt";
	

	
	public static void main(String[] args) throws IOException {
		String test = "asdasd\\\"ooo\\'yyyy";
		System.out.println(test);
System.out.println(test.contains("\\'"));
		String neo = test.replaceAll("[\\\"]", "\"").replaceAll("\\'", "'");
		System.out.println(neo);
//		TreeSet set = new TreeSet<String>();
//		BufferedReader br = new BufferedReader(new FileReader(new File(llFilePath)));
//		
//		String line ="";
//		
//		while ((line = br.readLine()) != null){
//			try{
//				System.out.println(line.split("\\t")[1]);
//				if (line.contains("\\\"")){
//					System.out.println("\t\t\t\t\t" + line.replaceAll(regex, replacement));
//				}
//				for (byte s : line.getBytes()){
//					set.add(s);
//				}
//			}catch(ArrayIndexOutOfBoundsException aioobe){
//				
//			}
//		}
	}

}
