package util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.mongodb.BasicDBObject;

import parallelizer.Coordinator;
import parallelizer.Parallelizable;
import parallelizer.Worker;

public class HTTP_Request_Test {
	private static final String EN_SRL_ENDPOINT = 	"http://54.246.9.177:9090/axis2/services/analysis_en/analyze?conll=true";
	private static final String CONTENT_TYPE = 		"text/xml";
	private static final String CHARSET = 			"UTF-8";
	private static final String DATA_FORMAT = 		"<analyze><text>{0}</text><target>relations</target></analyze>";


	public static void main(String[] args){
		
		HttpURLConnection connection = null;

		String docText ="";
		String textData = MessageFormat.format(DATA_FORMAT, docText);

		try {
			//Create connection
			URL url = new URL(EN_SRL_ENDPOINT);
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", CONTENT_TYPE);
			connection.setRequestProperty("Content-Length", Integer.toString(textData.getBytes().length));
			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setConnectTimeout(5000);
			
			//Send request
			try{
				connection.connect();
			}catch(Exception e){
				System.out.println("ERROR: response code: ");// +connection.getResponseCode());
				
			}
			System.out.println("connection.connected");
			
			
//			HttpURLConnection.HTTP_OK
			
			System.out.println("writing out");
			DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
			wr.writeBytes (textData);
			wr.flush ();
			wr.close ();
			//Get Response	
			System.out.println("reading response in");

//			SocketTimeoutException con.getInputStream()
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
			}
			rd.close();

			System.out.println(response.substring(0, 100));
			
		} catch (Exception e) {
			System.out.println("\t exception during http request handling");
			e.printStackTrace();
		}


	}

}
