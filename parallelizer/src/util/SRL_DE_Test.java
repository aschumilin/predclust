package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

public class SRL_DE_Test {
	private static final String CONTENT_TYPE = 		"text/xml";
	private static final String CHARSET = 			"UTF-8";
	static String endpoint1="http://lt.ffzg.hr:9090/xlike/analysis_de/analyze";
	static String endpoint2="http://vm:9090/services/analysis_de/analyze?conll=true";

	static String sentence = "Wegen ihres Übertritts zum Christentum wurde eine Frau im Sudan zum Tode verurteilt."; 
	static String shallowReqData="<analyze><text>" + sentence + "</text><target>relations</target><conll>true</conll></analyze>";

	static String deepReqFormat="<analyze><text>Wegen ihres Übertritts zum Christentum wurde eine Frau im Sudan zum Tode verurteilt.</text>" +
			"<input>entities</input>" +
			"<target>relations</target>" +
			"<wsd>true</wsd>" +
			"<srl>true</srl>" +
			"<conll>true</conll>" +
			//"<data>{0}</data>" +
			"</analyze>";






	public static void main(String[] args) {
		HttpURLConnection connection = null;
		StringBuffer response1 = null;
		try {
			//Create connection
			URL url = new URL(endpoint1);
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", CONTENT_TYPE);
			connection.setRequestProperty("Content-Length", Integer.toString(shallowReqData.getBytes().length));
			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			//Send request
			try{
				connection.connect();
			}catch(Exception e){
				System.out.println("ERROR 1: response code: " +connection.getResponseCode());

			}


			//			HttpURLConnection.HTTP_OK

			DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
//			wr.writeBytes (shallowReqData);
//			wr.flush ();
//			wr.close ();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, CHARSET));
			writer.write(shallowReqData);
			writer.close();
			wr.close();
			
			System.out.println("1: reading response in");
			InputStream is = connection.getInputStream();
			InputStreamReader isr = new InputStreamReader(is, CHARSET);
			System.out.println("------ in enc 1: " + isr.getEncoding());
			BufferedReader rd = new BufferedReader(isr);
			String line;
			response1 = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response1.append(line + "\n");
			}
			rd.close();

			System.out.println("shallow\n" + response1);
			
			
			
			
			
			
			
			System.out.println("=====================");
			
			/*
			 * get conll tag contents
			 */
			SAXBuilder builder = new SAXBuilder();
			final Document jdomDoc = (Document) builder.build(new StringReader(response1.toString()));	
			System.out.println("root: " + jdomDoc.getRootElement().getName());
			String conll = jdomDoc.getRootElement().getChild("conll").getText();
			
			
			
			
			

		

			String deepReqData = MessageFormat.format(deepReqFormat, conll);
			url = new URL(endpoint2);
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", CONTENT_TYPE);
			connection.setRequestProperty("Content-Length", Integer.toString(deepReqData.getBytes().length));
			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			//Send request
			try{
				connection.connect();
			}catch(Exception e){
				System.out.println("ERROR 2: response code: " + connection.getResponseCode());

			}

			System.out.println("writing out");
			wr = new DataOutputStream (connection.getOutputStream());
			writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
			writer.write(deepReqData);
			writer.close();
			wr.close();
			
			System.out.println("1: reading response in");
			is = connection.getInputStream();
			rd = new BufferedReader(new InputStreamReader(is));
			line = "";
			response1 = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response1.append(line + "\n");
			}
			rd.close();



			System.out.println("deep:\n" + response1);

		} catch (Exception e) {
			System.out.println("exception during http request handling");
			e.printStackTrace();
		}






	}

}
