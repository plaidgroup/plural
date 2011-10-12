package edu.cmu.cs.anek.output;

import java.io.PrintWriter;
import java.util.List;

/**
 * A GraphML document. Can be output to text that should
 * satisfy a graph ML schema.
 * 
 * @author Nels E. Beckman
 *
 */
public final class GraphMLDoc {

    private static String newline = System.getProperty("line.separator");
    
    private static final String XML_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String GRAPHML_HEADER =
        "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"" + newline + 
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newline+
            "xmlns:plural=\"http://www.nelsbeckman.com\"" + newline + 
            "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns " + newline +
            "xml/graphml+plural.xsd\">";
    private static final String GRAPHML_FOOTER =
        "</graphml>";
    
    private final List<String> stateHierarchies;
    private final List<String> methodGraphs;

    public GraphMLDoc(List<String> stateHierarchies, List<String> methodGraphs) {
        this.stateHierarchies = stateHierarchies;
        this.methodGraphs = methodGraphs;
    }
    
    public void output(PrintWriter stream) {
        stream.println(XML_HEADER);
        stream.println(GRAPHML_HEADER);
        
        // state hierarchy header
        stream.println("<key for=\"graphml\" id=\"hierarchykey\"/>");
        stream.println("<key for=\"node\" id=\"nodespecskey\"/>");
        stream.println("<key for=\"node\" id=\"tname\" attr.name=\"typename\" attr.type=\"string\"/>");
        stream.println("<key for=\"node\" id=\"synced\" attr.name=\"synchronized\" attr.type=\"boolean\"/>");
        stream.println("<key for=\"graph\" id=\"mname\" attr.name=\"methodname\" attr.type=\"string\"/>");
        stream.println("<key for=\"graph\" id=\"isctr\" attr.name=\"isconstructor\" attr.type=\"boolean\"/>");
        stream.println("<key for=\"graph\" id=\"overrides\" attr.name=\"overriddenmethods\" attr.type=\"string\"/>");
        stream.println("<data key=\"hierarchykey\">");
        for( String sh : stateHierarchies ) {
            stream.println(sh);
        }
        stream.println("</data>");
        
        for( String methodGraph : methodGraphs ) {
            stream.println(methodGraph);
        }
        
        stream.println(GRAPHML_FOOTER);
    }
    
}
