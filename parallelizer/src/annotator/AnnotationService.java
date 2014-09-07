package annotator;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

import edu.kit.aifb.gwifi.annotation.detection.Disambiguator;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.detection.TopicDetector;
import edu.kit.aifb.gwifi.annotation.preprocessing.DocumentPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.HtmlPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.PreprocessedDocument;
import edu.kit.aifb.gwifi.annotation.preprocessing.WikiPreprocessor;
import edu.kit.aifb.gwifi.annotation.tagging.DocumentTagger;
import edu.kit.aifb.gwifi.annotation.tagging.DocumentTagger.RepeatMode;
import edu.kit.aifb.gwifi.annotation.tagging.HtmlTagger;
import edu.kit.aifb.gwifi.annotation.tagging.WikiTagger;
import edu.kit.aifb.gwifi.annotation.weighting.ITopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.PageRankTopicWeighter;
import edu.kit.aifb.gwifi.crosslinking.LanguageLinksSearcher;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.HubConfiguration;
import edu.kit.aifb.gwifi.service.Service.LinkFormat;
import edu.kit.aifb.gwifi.service.Service.ResponseFormat;
import edu.kit.aifb.gwifi.service.Service.SourceMode;
import edu.kit.aifb.gwifi.service.WebContentRetriever;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class AnnotationService {

	private static float DEFAULT_MIN_PROB = 0.01f;
	private static boolean DEFAULT_TOOLTIPS = true;
	private static SourceMode DEFAULT_SOURCE_MODE = SourceMode.AUTO;
	private static LinkFormat DEFAULT_LINK_FORMAT = LinkFormat.AUTO;
	private static RepeatMode DEFAULT_REPEAT_MODE = RepeatMode.FIRST_IN_REGION;
	private static ResponseFormat DEFAULT_RESPONSE_FORMAT = ResponseFormat.XML;
	private static String linkClassName = "wm_wikifiedLink";

	private static Document doc = new DocumentImpl();
	private static DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(Locale.US);

	private final static String KB_WIKIPEDIA = "wikipedia";
	private final static String KB_DBPEDIA = "dbpedia";

	private final static String WIKIPEDIA_URL = ".wikipedia.org/wiki/";
	private final static String DBPEDIA_URL = ".dbpedia.org/resource/";

	private edu.kit.aifb.gwifi.model.Wikipedia wikipedia;
	private Disambiguator disambiguator;
	private TopicDetector topicDetector;
	private ITopicWeighter topicWeighter;
	private WebContentRetriever retriever;
	private DocumentPreprocessor preprocessor;
	private LanguageLinksSearcher langlinks;

	private SourceMode sourceMode;
	private LinkFormat linkFormat;
	private ResponseFormat responseFormat;

	private Language inputLang;
	private Language outputLang;
	private String kb = "wikipedia";


	// first arg : inpur lang
	// second arg: path to configs directory
	public static void main(String[] args) throws Exception {
//		System.out.println("main args[]: 1: input lang, 2: path to configs dir");
		String LANG_IN = args[0];
		String CONF_PATH = args[1];
		AnnotationService service = new AnnotationService(CONF_PATH+"/hub-template.xml",
				CONF_PATH+"/wikipedia-template-" + LANG_IN + ".xml", LANG_IN, "en", "dbpedia");
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Please input the source text:");
			String source = scanner.nextLine();
			if (source.startsWith("exit")) {
				break;
			}
			

			String result = service.process(source);
			System.out.println(result);
		}
		scanner.close();

	}

	/**
	 * This is the main method for annotation service.
	 * 
	 * @param hubconfig
	 *            the configuration file, which defines properties for a service
	 *            hub
	 * @param wikiconfig
	 *            the configuration file, which specifies properties for a
	 *            wikipedia dump of the input language
	 * @param inputLangLabel
	 *            the input language (the language of the input texts to be
	 *            annotated)
	 * @param outputLangLabel
	 *            the output language (the language of the resources in KB used
	 *            for annotation)
	 * @param kb
	 *            the knowledge base (KB) used for annotation (wikipedia or
	 *            dbpedia)
	 * 
	 * @return the annotation in xml
	 * 
	 * @exception Exception
	 */
	public AnnotationService(String hubconfig, String wikiconfig, String inputLangLabel, String outputLangLabel,
			String kb) throws Exception {
		inputLang = Language.getLanguage(inputLangLabel);
		outputLang = Language.getLanguage(outputLangLabel);
		this.kb = kb;

		HubConfiguration config = new HubConfiguration(new File(hubconfig));
		retriever = new WebContentRetriever(config);
		langlinks = new LanguageLinksSearcher(config.getLanglinksPath());

		File databaseDirectory = new File(wikiconfig);
		try{
			wikipedia = new Wikipedia(databaseDirectory, false);
		}catch(Exception e){
			
		}
		disambiguator = new Disambiguator(wikipedia);
		topicDetector = new TopicDetector(wikipedia, disambiguator, false, false);
		topicWeighter = new PageRankTopicWeighter(wikipedia, disambiguator);
	}

	public String process(String source) throws Exception {
		sourceMode = DEFAULT_SOURCE_MODE;
		linkFormat = DEFAULT_LINK_FORMAT;
		responseFormat = DEFAULT_RESPONSE_FORMAT;

		if (sourceMode == SourceMode.AUTO)
			sourceMode = resolveSourceMode(source);

		if (linkFormat == LinkFormat.AUTO) {
			if (sourceMode == SourceMode.WIKI)
				linkFormat = LinkFormat.WIKI;
			else
				linkFormat = LinkFormat.HTML_ID_WEIGHT;
		}

		if (sourceMode == SourceMode.URL)
			responseFormat = ResponseFormat.XML;

		if (sourceMode == SourceMode.WIKI)
			preprocessor = new WikiPreprocessor(wikipedia);
		else
			preprocessor = new HtmlPreprocessor();

		if (responseFormat == ResponseFormat.DIRECT) {
			String wikifiedDoc = buildUnwrappedResponse(source, inputLang, outputLang, kb, sourceMode, linkFormat);
			return wikifiedDoc;
		} else if (responseFormat == ResponseFormat.XML) {
			Element xmlResponse = buildWrappedResponse(doc, source, inputLang, outputLang, kb, sourceMode, linkFormat);

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			DOMSource dom = new DOMSource(xmlResponse);
			transformer.transform(dom, result);
			return writer.toString();
		} else {
			return source;
		}
	}

	public String buildUnwrappedResponse(String source, Language input, Language output, String kb,
			SourceMode sourceMode, LinkFormat linkFormat) throws Exception {
		ArrayList<Topic> detectedTopics = new ArrayList<Topic>();
	
		
		String wikifiedDoc = wikifyAndGatherTopics(source, detectedTopics, input, output, kb, sourceMode, linkFormat);

		return wikifiedDoc;
	}

	public Element buildWrappedResponse(Document doc, String source, Language input, Language output, String kb,
			SourceMode sourceMode, LinkFormat linkFormat) throws Exception {
		
		
		if (source == null || source.trim().length() == 0) {
			System.out.println("You must specify a source document to wikify");
		}

		ArrayList<Topic> detectedTopics = new ArrayList<Topic>();
		

		
		
		String wikifiedDoc = wikifyAndGatherTopics(source, detectedTopics, input, output, kb, sourceMode, linkFormat);

		// double docScore = 0;
		// for (Topic t : detectedTopics)
		// docScore = docScore + t.getRelatednessToOtherTopics();

		Element xmlResponse = createElement(doc, "Response");
		Element xmlWikifiedDoc = createCDATAElement(doc, "WikifiedDocument", wikifiedDoc);
		// xmlWikifiedDoc.setAttribute("sourceMode", sourceMode.toString());
		// xmlWikifiedDoc.setAttribute("documentScore", format(docScore));
		xmlResponse.appendChild(xmlWikifiedDoc);

		Element xmlDetectedTopics = createElement(doc, "DetectedTopics");
		for (Topic dt : detectedTopics) {

			if (dt.getWeight() <= DEFAULT_MIN_PROB){
				System.out.println("<=DEFAULT_MIN_PROB: " + dt.getDisplayName() + "\t" + dt.getWeight());
				
				continue;
			}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/*
				System.out.println("=====================================");
				System.out.println("=====================================");

				System.out.println(dt.getDisplayName()  + " -------->");
				for (Position p : dt.getPositions()){
					int i = p.getStart();
					int j = p.getEnd();
					System.out.println("\t" + i + " - " + j + "\t" + source.substring(i,j));
				}
				
				System.out.println("=====================================");
				System.out.println("=====================================");
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~				
 * 
 */
			Element detectedTopic = createElement(doc, "DetectedTopic");
//			detectedTopic.setAttribute("id", String.valueOf(dt.getId()));
//			detectedTopic.setAttribute("lang", output.toString());
			detectedTopic.setAttribute("displayName", dt.getDisplayName());
			detectedTopic.setAttribute("URL", dt.getURI());
			detectedTopic.setAttribute("weight", format(dt.getWeight()));
			detectedTopic.setAttribute("mention", dt.getReferences().get(0).getLabel().getText());
			detectedTopic.setAttribute("from", new Integer(dt.getPositions().get(0).getStart()).toString());
			detectedTopic.setAttribute("to", new Integer(dt.getPositions().get(0).getEnd()).toString());
			
			xmlDetectedTopics.appendChild(detectedTopic);
		}
		xmlResponse.appendChild(xmlDetectedTopics);

		return xmlResponse;
	}

	private String wikifyAndGatherTopics(String source, ArrayList<Topic> detectedTopics, Language input,
			Language output, String kb, SourceMode sourceMode, LinkFormat linkFormat) throws IOException, Exception {

		if (source == null || source.trim().equals(""))
			return "";

		String linkStyle = "";

		String markup;
		if (sourceMode == SourceMode.URL) {

			if (source.matches("(?i)^www\\.(.*)$"))
				source = "http://" + source;

			URL url = new URL(source);

			markup = retriever.getWebContent(url);
		} else {
			markup = source;
		}

		PreprocessedDocument doc = preprocessor.preprocess(markup);
		// for (Article bt: bannedTopicList)
		// doc.banTopic(bt.getId()) ;

		// TODO: find smarter way to resolve this hack, which stops wikifier
		// from detecting "Space (punctuation)" ;
		doc.banTopic(143856);

		RelatednessCache rc = new RelatednessCache(disambiguator.getArticleComparer());
		Vector<Topic> topics = topicDetector.getTopics(doc, rc);
		ArrayList<Topic> weightedTopics = topicWeighter.getWeightedTopics(topics, rc);
		TreeSet<Topic> bestTopics = new TreeSet<Topic>();

		for (Topic topic : weightedTopics) {
			int id = topic.getId();
			String title = topic.getTitle();
			String displayName = extractCrossDescription(id, title, input, output);
			if (displayName == null || displayName.equals(""))
				continue;
			String uri = getURI(displayName, input, output, kb);
			topic.setURI(uri);
			topic.setDisplayName(displayName);
			detectedTopics.add(topic);

			if (topic.getWeight() >= DEFAULT_MIN_PROB)
				bestTopics.add(topic);
		}

		DocumentTagger dt;
		if (linkFormat == LinkFormat.HTML || linkFormat == LinkFormat.HTML_ID
				|| linkFormat == LinkFormat.HTML_ID_WEIGHT)
			dt = new MyHtmlTagger(linkFormat, linkStyle);
		else
			dt = new MyWikiTagger(linkFormat);

		String taggedText = dt.tag(doc, bestTopics, DEFAULT_REPEAT_MODE);

		if (sourceMode == SourceMode.URL) {
			taggedText = taggedText.replaceAll("(?i)<html", "<base href=\"" + source + "\" target=\"_top\"/><html");

			if (DEFAULT_TOOLTIPS) {

				// String basePath = getBasePath(request);
				String basePath = "";

				if (!basePath.endsWith("/"))
					basePath = basePath + "/";

				StringBuffer newHeaderStuff = new StringBuffer();
				newHeaderStuff.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + basePath
						+ "/css/tooltips.css\"/>\n");
				newHeaderStuff.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + basePath
						+ "/css/jquery-ui/jquery-ui-1.8.14.custom.css\"/>\n");

				if (linkStyle != null && linkStyle.trim().length() > 0)
					newHeaderStuff.append("<style type='text/css'> ." + linkClassName + "{" + linkStyle
							+ ";}</style>\n");

				newHeaderStuff.append("<script type=\"text/javascript\" src=\"" + basePath
						+ "/js/jquery-1.5.1.min.js\"></script>\n");
				newHeaderStuff.append("<script type=\"text/javascript\" src=\"" + basePath
						+ "/js/tooltips.js\"></script>\n");
				newHeaderStuff.append("<script type=\"text/javascript\"> \n");
				newHeaderStuff.append("  var wm_host=\"" + basePath + "\" ; \n");
				newHeaderStuff.append("  $(document).ready(function() { \n");
				newHeaderStuff.append("    wm_addDefinitionTooltipsToAllLinks(null, \"" + linkClassName + "\") ; \n");
				newHeaderStuff.append("  });\n");
				newHeaderStuff.append("</script>\n");

				taggedText = taggedText.replaceAll("(?i)\\</head>", Matcher.quoteReplacement(newHeaderStuff.toString())
						+ "</head>");
			}

		}

		return taggedText;
	}

	private static SourceMode resolveSourceMode(String source) {

		// try to parse source as url
		try {
			// fix omitted http prefix
			if (source.matches("(?i)^www\\.(.*)$"))
				source = "http://" + source;

			URL url = new URL(source);
			return SourceMode.URL;
		} catch (MalformedURLException e) {
		}
		;

		// count html elements and wiki link elements
		int htmlCount = 0;
		Pattern htmlTag = Pattern.compile("<(.*?)>");
		Matcher m = htmlTag.matcher(source);
		while (m.find())
			htmlCount++;

		int wikiCount = 0;
		Pattern wikiTag = Pattern.compile("\\[\\[(.*?)\\]\\]");
		m = wikiTag.matcher(source);
		while (m.find())
			wikiCount++;

		if (htmlCount > wikiCount)
			return SourceMode.HTML;
		else
			return SourceMode.WIKI;

	}

	public static Element createElement(Document doc, String tagName) {
		return doc.createElement(tagName);
	}

	public static Text createTextNode(Document doc, String data) {
		return doc.createTextNode(data);
	}

	public static Element createCDATAElement(Document doc, String tagName, String data) {
		Element e = doc.createElement(tagName);
		e.appendChild(doc.createCDATASection(data));
		return e;
	}

	public static String format(double number) {
		return decimalFormat.format(number);
	}

	public static class MyHtmlTagger extends HtmlTagger {

		LinkFormat linkFormat;
		String linkStyle;

		public MyHtmlTagger(LinkFormat linkFormat, String linkStyle) {
			this.linkFormat = linkFormat;
			this.linkStyle = linkStyle;
			if (this.linkStyle != null)
				this.linkStyle = this.linkStyle.trim();
		}

		public String getTag(String anchor, Topic topic) {

			String url = topic.getURI();

			if (url == null) {
				return anchor;
			}

			StringBuffer tag = new StringBuffer("<a");
			tag.append(" href=\"" + url + "\"");

			tag.append(" class=\"" + linkClassName + "\"");

			if (linkFormat == LinkFormat.HTML_ID || linkFormat == LinkFormat.HTML_ID_WEIGHT)
				tag.append(" pageId=\"" + topic.getId() + "\"");

			if (linkFormat == LinkFormat.HTML_ID_WEIGHT)
				tag.append(" linkProb=\"" + format(topic.getWeight()) + "\"");

			if (linkStyle != null && linkStyle.length() > 0)
				tag.append(" style=\"" + linkStyle + "\"");

			tag.append(">");
			tag.append(anchor);
			tag.append("</a>");

			return tag.toString();
		}
	}

	public static class MyWikiTagger extends WikiTagger {

		LinkFormat linkFormat;

		public MyWikiTagger(LinkFormat linkFormat) {
			this.linkFormat = linkFormat;
		}

		public String getTag(String anchor, Topic topic) {

			int id = topic.getId();
			String url = topic.getURI();
			String displayName = topic.getDisplayName();

			if (url == null) {
				return anchor;
			}

			StringBuffer tag = new StringBuffer("[[");

			if (linkFormat == LinkFormat.WIKI_ID || linkFormat == LinkFormat.WIKI_ID_WEIGHT) {
				tag.append(id);

				if (linkFormat == LinkFormat.WIKI_ID_WEIGHT) {
					tag.append("|");
					tag.append(format(topic.getWeight()));
				}

				tag.append("|");
				tag.append(anchor);

			} else {

				if (displayName.compareToIgnoreCase(anchor) == 0)
					tag.append(anchor);
				else {
					tag.append(displayName);
					tag.append("|");
					tag.append(anchor);
				}
			}

			tag.append("]]");
			return tag.toString();
		}
	}

	public String extractCrossDescription(int pageId, String title, Language input, Language output) {
		if (input.equals(output))
			return title;
		String displayName = null;
		try {
			if (!input.equals(output)) {
				displayName = langlinks.getDescription(pageId, input.toString(), output.toString());
			}

			return displayName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getURI(String title, Language input, Language output, String kb) {
		String uri = null;
		if (kb.equals(KB_DBPEDIA)) {
			if (output.equals(Language.EN))
				uri = "http://" + DBPEDIA_URL.substring(1) + title.replace(" ", "_");
			else
				uri = "http://" + output.toString() + DBPEDIA_URL + title.replace(" ", "_");
		} else if (kb.equals(KB_WIKIPEDIA)) {
			uri = "http://" + output.toString() + WIKIPEDIA_URL + title.replace(" ", "_");
		} else {
			uri = "http://" + output.toString() + WIKIPEDIA_URL + title.replace(" ", "_");
		}
		return uri;
	}

}
