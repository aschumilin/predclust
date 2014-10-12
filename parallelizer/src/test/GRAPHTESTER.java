package test;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.swing.JFrame;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import graph.Argument;
import graph.Role;


public class GRAPHTESTER {
	//	static Logger L = Logger.getLogger(GRAPHTESTER.class);;
	

	public static void main(String[] args) throws IOException, JDOMException{

//		CallSRL s = new CallSRL();
//		System.out.println(s.makeCall("hallo"));
		System.out.println(Integer.MAX_VALUE - 4);

		String annotation = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Response>\n  <WikifiedDocument><![CDATA[[[freeware]]]]></WikifiedDocument>\n  <DetectedTopics>\n    <DetectedTopic URL=\"http://dbpedia.org/resource/Freeware\" displayName=\"Freeware\" id=\"11592\" lang=\"en\" mention=\"freeware\" weight=\"1\"/><DetectedTopic URL=\"http://dbpedia.org/resource/Freeware\" displayName=\"Freeware\" id=\"11592\" lang=\"en\" mention=\"freeware\" weight=\"1\"/><DetectedTopic URL=\"http://dbpedia.org/resource/Freeware\" displayName=\"Freeware\" id=\"11592\" lang=\"en\" mention=\"freeware\" weight=\"1\"/>\n  </DetectedTopics>\n</Response>";
		
		Document doc = (Document) new SAXBuilder().build(new StringReader(annotation));
		List<Element> annots =  doc.getRootElement().getChild("DetectedTopics").getChildren("DetectedTopic");
		for(Element e : annots){
			System.out.println(e.getAttributeValue("URL"));
			e.addContent(new Element("new_tag").setText("content").setAttribute("atrib", "val"));
		}
		System.out.println(new XMLOutputter(Format.getPrettyFormat()).outputString(doc));
		
		
		
		long anf = System.currentTimeMillis();
		
		System.out.println((System.currentTimeMillis()-anf)/1000 + " s");
		// tools:
				
//		BufferedWriter bw = new BufferedWriter(new FileWriter("/home/pilatus/Arbeitsfläche/test.txt"));
//		bw.write("hi");
//		bw.write("world");
//		bw.close();

		
		
	/*
		DirectedGraph<Argument, Role> g = new DirectedSparseMultigraph<Argument, Role>();
		DirectedGraph<Argument, Role> g_all = new DirectedSparseMultigraph<Argument, Role>();
		DirectedGraph<Argument, Role> g2 = new DirectedSparseMultigraph<Argument, Role>();


		Node n1 = new Node("W1", "word", "NN", "obama", "OBAMA");
		Node n2 = new Node("W2", "word", "NU", "city", "SouthPark");
		Predicate p = new Predicate(Boolean.TRUE, "p", "VBA", "visit", "visitee");
		Role r1 = new Role("A0:Agent");
		Role r2 = new Role("A1:Patient");		
		g.addEdge(r1, p, new Node("W1", "word", "NN", "obama", "OBAMA"));
		g.addEdge(r2, new Node("W1", "word", "NN", "obama", "OBAMA"), n2);
		g.addVertex(new Node("W2", "word", "NU", "city", "SouthPark"));
		g.addVertex(new Node("W123", "word", "NU", "city", "SouthPark"));
		g.addVertex(new Node("W123", "word", "NU", "city", "SouthPark"));

		


		Node n3 = new Node("W3", "word", "WW", "city", "SouthPark");
		Predicate p2 = new Predicate(Boolean.TRUE, "p2", "VBA", "visit", "visitee");
		Role r3 = new Role("A0:Agent");
		g2.addEdge(r3, p2, n3);
		
		Collection<Argument> vs = new ArrayList<Argument>();
		vs.addAll(g.getVertices());
		vs.addAll(g2.getVertices());

		
		
		

		System.out.println(g.getEdgeCount());
		System.out.println(g.getVertexCount());
		System.out.println(g.outDegree(p));
		System.out.println(g.getEdgeCount());
		System.out.println(g.getEdgeCount());
		
		for (Object o: g.getVertices()){
			System.out.println(o instanceof Node);
		}
		

		visGraph(g, "graph test");
*/
	

				




	}
	public static void visGraph(Graph g, String title){
		Layout<Argument, Role> layout = new CircleLayout(g);
		layout.setSize(new Dimension(300,300));
		VisualizationViewer<Argument, Role> vv = new VisualizationViewer<Argument, Role>(layout);
		vv.setPreferredSize(new Dimension(700, 500));
		// Show vertex and edge labels
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());

		// Create our "custom" mouse here. We start with a PluggableGraphMouse
		// Then add the plugins you desire.
		PluggableGraphMouse gm = new PluggableGraphMouse(); 
		gm.add(new TranslatingGraphMousePlugin(MouseEvent.BUTTON1_MASK));
		gm.add(new ScalingGraphMousePlugin(new CrossoverScalingControl(), 0, 1.1f, 0.9f));
		gm.add(new PickingGraphMousePlugin<Argument, Role>(MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON2_MASK));

		vv.setGraphMouse(gm); 
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(vv);
		frame.pack();
		frame.setVisible(true); 
	}
}
