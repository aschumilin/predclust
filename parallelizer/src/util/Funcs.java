package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class Funcs {


	public static void main(String[] args) throws UnsupportedEncodingException {
		String fileName = "/ssd/users/arsc/enDocIDs";

		//		PrintWriter file = null;
		//		try {
		//			file = new PrintWriter(fileName);
		//		} catch (FileNotFoundException e1) {e1.printStackTrace();}
		//		
		//		DBCollection coll = null;
		//		
		//		
		//		try {
		//			coll = getMongoColl("localhost", 22222, "all", "srl", "reader", "123");
		//		} catch (MongoException e) {e.printStackTrace();} catch (UnknownHostException e) {e.printStackTrace();}
		//		
		//		
		//		BasicDBObject query = new BasicDBObject();
		//		BasicDBObject keys = new BasicDBObject("_id", 1);
		//		DBCursor results = coll.find(query, keys);
		//		
		//		System.out.println("total results: " + results.size());
		//		DBObject doc = null;
		//		while(results.hasNext()){
		//			doc = results.next();
		//			file.println(doc.get("_id"));
		//		}
		//		
		//		file.close();

		
		DBCollection coll= null;
		try {
			coll = getMongoColl("romulus", 22222, "all", "srl", "reader", "123");
		} catch (MongoException e) {e.printStackTrace();} catch (UnknownHostException e) {e.printStackTrace();}

		
		SAXBuilder builder = new SAXBuilder();
		XPathFactory xFactory = XPathFactory.instance();  
		String srlXMLString = coll.findOne().get("srlAnnot").toString();
		Document doc = null;
		try {
			doc= (Document) builder.build(new StringReader(srlXMLString));} catch (JDOMException e) {e.printStackTrace();} catch (IOException e) {e.printStackTrace();
			}



//		XPathExpression<Element> expr = xFactory.compile("//node", Filters.element());
//		List<Element> results = expr.evaluate(doc);
		List<Element> results = doc.getRootElement().getChild("nodes").getChildren("node");
		
		for(Element el : results){
			System.out.print(el.getAttributeValue("displayName") + ":");
			for (Element ment : el.getChild("mentions").getChildren("mention")){
				System.out.print( " " + ment.getAttributeValue("words"));
			}
			System.out.println();
				
		}
		
		
		
		//doc.addContent(doc.getRootElement().addContent(results.get(0).setAttribute("attrib", "value")));
		
		XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
		
		try {
			xo.output(doc.getRootElement().getChild("conll"), System.out);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println();
		
		String conll = doc.getRootElement().getChild("conll").getText();
		int i = 1;
		String line = "";
		String[] parts = conll.split("\\s");
		System.out.println(parts.length);
		for (String s: parts){
			if (! s.isEmpty()){
				line = line + s + "\t";
				if (i%16 == 0){
					System.out.println(line);
					line = "";
				}
				i++;
			}
		}

		System.out.println("main says: " + args[0]);

	}

	public static DBCollection getMongoColl(String host, int port, String dbName, String collName, String user, String pwd ) throws MongoException, UnknownHostException{
		MongoClient mongoClient = null;
		DB 			db = null;
		DBCollection coll = null;
		String mongoAddr = host;
		int 	mongoPort = port;
		String 	mongoUser = user;
		String 	mongoPass = pwd;
		String 	mongoDBName = dbName;
		String collectionName = collName;


		mongoClient = new MongoClient( mongoAddr , mongoPort );
		db = mongoClient.getDB(mongoDBName);
		boolean auth = db.authenticate(mongoUser, mongoPass.toCharArray());
		if( ! auth ){
			throw new MongoException("mongo authentication failed for user: " + user);
		}else{

			coll = db.getCollection(collectionName);

		}




		return coll;
	}




	public static void execBash(String bashCommand) throws java.io.IOException{
		//		execute with sudo
		//		"/bin/bash","-c","echo password| sudo -S ls"  

		Process pb = Runtime.getRuntime().exec(bashCommand);

		String line;
		java.io.BufferedReader input = new java.io.BufferedReader(new java.io.InputStreamReader(pb.getInputStream()));
		while ((line = input.readLine()) != null) {
			System.out.println("bash response:   " + line);
		}
		input.close();
	}



	/**
	 * The SRL service produces XML that may contain chars invalid in XML1.0.
	 * This causes the JDOMParser to crash. This function replaces invalid chars with blanks.
	 * @param inputXML result xml from the SRL Service
	 * @return XML where invalid chars are replaced with blanks.
	 */
	public static String removeInvalidXMLChars(String inputXML){ 
		String xml10pattern = "[^"
				+ "\u0009\r\n"
				+ "\u0020-\uD7FF"
				+ "\uE000-\uFFFD"
				+ "\ud800\udc00-\udbff\udfff"
				+ "]";
		return inputXML.replaceAll(xml10pattern, " ");
	}




	public static String getPostResponse(String endpoint, String dataPayload) throws java.io.IOException {
		HttpURLConnection connection = null;

		final String CONTENT_TYPE = "text/xml";
		final String REQUEST_TYPE = "POST";
		final String CHARSET =		" UTF-8";

		StringBuffer response = new StringBuffer();

		//Create connection
		URL url = new URL(endpoint);
		connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod(REQUEST_TYPE);
		connection.setRequestProperty("Content-Type", CONTENT_TYPE);
		connection.setRequestProperty("Content-Length", Integer.toString(dataPayload.getBytes().length));
		connection.setUseCaches (false);
		connection.setDoInput(true);
		connection.setDoOutput(true);

		//Send request
		connection.connect();

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new DataOutputStream (connection.getOutputStream()), CHARSET));
		writer.write(dataPayload);
		writer.close();


		//Get Response	
		BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), CHARSET));
		String line;

		while((line = rd.readLine()) != null) {
			response.append(line +  "\n");
		}
		rd.close();





		return response.toString();
	}
}
