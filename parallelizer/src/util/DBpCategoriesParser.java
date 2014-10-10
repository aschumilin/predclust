package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * DBpedia version: 2014-07-25T21:33:17Z
 * @author Artem Schumilin
 *
 */
public class DBpCategoriesParser {
	public static void main(String[] args){
		
		
//		
//		String test ="<http://dbpedia.org/resource/Albedo> <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:Climate_forcing> .";
//		System.out.println(test.split("\\s+")[2].substring(1, test.split("\\s+")[2].length()-1));
//		System.exit(1);
		
		
		
//		String catsFilePath = "/home/pilatus/WORK/pred-clust/data/dbp-categories/dbp-cats.nt";
		String catsFilePath = "/local/users/atsc/BACKUP/DBp-article-categories/article_categories_en.nt";

		BufferedReader br = null;
		TreeMap<String, ArrayList<String>> entityCategoryMap = new TreeMap<String, ArrayList<String>>();
		ArrayList<String> categoriesTemp = new ArrayList<String>();
		
		

		try {
			br= new BufferedReader(new FileReader(new File(catsFilePath)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		String line = "", ent = "", cat = "", previous = "";
		String[] parts = null;
		
		int i = 0;
		while(true){

			if (++i % 100000 == 0) System.out.println(i);
			
			try {
				line = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(line == null ){
				entityCategoryMap.put(ent, categoriesTemp);
				break;
			}else{
				if (line.substring(0,1).equals("<")){
					parts = line.split("\\s+");
					ent = parts[0].substring(1, parts[0].length() -1);
					cat = parts[2].substring(1, parts[2].length() - 1);
					
					if(ent.equals(previous)){
//						System.out.println("\t\t\t" + cat);
						categoriesTemp.add(cat);
					}else{
						entityCategoryMap.put(previous, categoriesTemp);
						categoriesTemp = new ArrayList<String>();
						categoriesTemp.add(cat);
						
//						System.out.println(ent);
//						System.out.println("\t\t\t" + cat);

					}
					
					
				}
			}
			previous = ent;
		}
		System.out.println("================================");
//		for (String key : entityCategoryMap.keySet()){
//			System.out.println(key);
//			for (String c : entityCategoryMap.get(key)){
//				System.out.println("\t\t\t" + c);
//			}
//		}
		System.out.println("distinct ents: "  + entityCategoryMap.size());
		
		FileOutputStream f_out = null;
		ObjectOutputStream obj_out = null;
		try {
//			f_out = new FileOutputStream("/home/pilatus/WORK/pred-clust/data/dbp-categories/EntityCategory.map_String-ArrayListOfString");
			f_out = new FileOutputStream("/local/users/atsc/BACKUP/DBp-article-categories/EntityCategory.map_String-ArrayListOfString");
			obj_out = new ObjectOutputStream (f_out);
			obj_out.writeObject(entityCategoryMap);
			obj_out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}
}
