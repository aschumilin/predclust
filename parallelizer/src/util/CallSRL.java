package util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;

public class CallSRL {
	private static final String EN_SRL_ENDPOINT = 	"http://54.246.9.177:9090/axis2/services/analysis_en/analyze?conll=true";
	private static final String CONTENT_TYPE = 		"text/xml";
	private static final String DATA_FORMAT = 		"<analyze><text>{0}</text><target>relations</target></analyze>";

	
	public CallSRL(){
	}
	
	
	/**
	 * @param textToAnalyse
	 * @return string containing srl results <item>...</item>
	 * @throws IOException 
	 */
	public String makeCall(String textToAnalyse) throws IOException {
		
		String data = MessageFormat.format(DATA_FORMAT, textToAnalyse);

		// 3. call SRL service
		HttpURLConnection connection = null; 
		//Create connection
		URL url = new URL(EN_SRL_ENDPOINT);
		connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", CONTENT_TYPE);
		connection.setRequestProperty("Content-Length", Integer.toString(data.getBytes().length));
		connection.setUseCaches (false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		//Send request
		DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
		wr.writeBytes (data);
		wr.flush ();
		wr.close ();
		//Get Response	
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuffer response = new StringBuffer(); 
		while((line = rd.readLine()) != null) {
			response.append(line);
		}
		rd.close();
		connection.disconnect();
		return connection.getResponseCode() + " " + response.toString();


	}
}
