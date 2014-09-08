package test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import com.mongodb.DBObject;

public class SrlUtfTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		String[] id = new String[]{"http://en.wikipedia.org/wiki?curid=163883", 
			    				"http://en.wikipedia.org/wiki?curid=125903",
			    				"http://es.wikipedia.org/wiki?curid=4945504"};
		
		util.Mongo m = new util.Mongo("romulus","docs");
		
		DBObject doc = m.getById(id[0]);
		
		String text = (String) doc.get("docText");
		String lang = (String) doc.get("lang");
		
		String ok = "This is <br>bullshit.";
		String nok = "La versión en vinilo contiene además dos pistas extras.";
		
//		
		text = StringUtils.replaceEach(text, new String[]{"<",  ">"}, new String[]{"", ""});
//		text = nok;
		
//		String srl = new util.CallSRL().makeUtfCall(text, lang);

		m = new util.Mongo("romulus","srl");
		List<DBObject> l = m.execQuery("{_id:\"http://en.wikipedia.org/wiki?curid=163883\"}");
System.out.println(l.get(0).get("docTitle").toString());
		
	}

}
