package algo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;



/**
 * @author aschumilin
 * 
 * read and clean wikinews and wikipedia text docs as xml
 * and dump them to mongoDB
 * 
 * schema example:
 * {
	"_id" : "http://en.wikipedia.org/wiki?curid=609582",
	"docId" : "609582",
	"docTitle" : "Sinergy",
	"lang" : "en",
	"project" : "wikipedia",
	"docText" : "Sinergy was a Finnish..."
	}
 *
 */

public class TextDumper {

	private static int i = 1;
	private static int j = 1;
	public static void main(String[] args) {
		String[] chunks = new String[]{"wiki_00","wiki_02","wiki_04","wiki_06","wiki_08","wiki_10","wiki_12","wiki_14", "wiki_16","wiki_18","wiki_01","wiki_03","wiki_05","wiki_07","wiki_09","wiki_11","wiki_13","wiki_15","wiki_17"};
		String path = "/local/users/arsc/data/es-wiki-2012-05-15/";
		
		for (String file : chunks){
			runAlgo(path + file);
			j++;
		}
		
	}
	
	/**
	 * Perform different checks on the document text to decide 
	 * whether this doc should enter the database.
	 * 
	 * 1. criterium: number of sentences 
	 * 2. criterium: text length
	 * 
	 * 
	 * @return true if document text passes the check, else false 
	 */
	private static boolean approveDoc(String text){
		
		boolean crit_1 = ((text.split("\\.")).length >= 5); 
		boolean crit_2 = (text.length() >= 200); 
		return crit_1 & crit_2;
	}
	
	
	
	
	
	public static void runAlgo(String dataFilePath) {
		
		
		BufferedReader br = null;
		
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(dataFilePath), Charset.forName("UTF-8")));
			} catch (FileNotFoundException fnfe) {
				System.out.println(dataFilePath +"\t file not found...exiting");
				return;			
			}
		
			
			try {
				if(br!=null){
					String line = "";
					
					while ((line = br.readLine())!= null) {
						
						if(line.startsWith("<doc id=\"")){			// beginning of a new document
							
							
							// 1. parse the id, url and title
							
							String[] parts = line.split(" ");
							String id =parts[1].substring(4, parts[1].length()-1);

							
							// 1. skip the next line because it's the title
							br.readLine();
							
							// 2. read until the next "</doc>"
							String text = "";
							while((line = br.readLine()) != null){
								
								
								if(! line.equals("</doc>")){
										text = text.concat(line.trim() + " ");
								}else{
									
									// 3. finalize new DB object with text
												
										text = text.trim();
										if (approveDoc(text)){
											
											// write doc text to file
											String filepath = "/local/users/arsc/data/es-wiki-text/es-" + id;
											
											PrintWriter pw = new PrintWriter(new File(filepath));
											pw.write(text);
											pw.close();
											System.out.println("done " + j +  " " + i);
											i++;
										}
										
											
											
										
									
									break;
								}	
							}	 
						}	
					}	
				}
			} catch (IOException e) {
				System.out.println("\t io error...aborting worker: " + dataFilePath);
				return;
			}finally{
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("\t io error closing the BufferedReader");
				}
			}
		}
		
	
		
	
	


	

	
	
	

}
