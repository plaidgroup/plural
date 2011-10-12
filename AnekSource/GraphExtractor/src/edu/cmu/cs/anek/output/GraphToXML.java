package edu.cmu.cs.anek.output;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


import edu.cmu.cs.anek.graph.Graph;
import edu.cmu.cs.anek.graph.MethodGraph;
import edu.cmu.cs.anek.graph.Node;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy.Dimension;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy.State;

/**
 * Static methods for converting a {@link Graph} into
 * an XML document.
 * 
 * @author Nels E. Beckman
 *
 */
public final class GraphToXML {

    /**
     * Takes a graph and returns a XML document, which
     * is represented with an instance of the {@link GraphMLDoc}
     * class.
     */
    public static GraphMLDoc graphToXML(Graph g) {
        List<String> hierarchies = hierarchies(g);
        List<String> methodGraphs = methods(g);
        return new GraphMLDoc(hierarchies, methodGraphs);
    }
    
    private static String newline = System.getProperty("line.separator");
    
    private static List<String> methods(Graph g) {
        List<String> result = new LinkedList<String>();
        for( MethodGraph method : g.getMethods() ) {
            StringBuilder builder = new StringBuilder("<graph id=\"");
            builder.append(method.id());
            builder.append("\" edgedefault=\"directed\">"); 
            builder.append(newline);
            builder.append(method(method));
            builder.append(newline);
            builder.append("</graph>");
            builder.append(newline);
            result.add(builder.toString());
        }
        return result;
    }

    private static List<String> hierarchies(Graph g) {
        List<String> result = new LinkedList<String>();
        for( StateHierarchy hier : g.getHierarchies() ) {
            StringBuilder builder = new StringBuilder("<plural:state-hierarchy type=\"");
            builder.append(hier.getType().getQualifiedName());
            builder.append("\">");
            builder.append(newline);
            builder.append(states(Collections.singleton(hier.getRoot()), ""));
            builder.append(newline);
            builder.append("</plural:state-hierarchy>");
            result.add(builder.toString());
        }
        return result;
    }
        
    private static String states(Collection<StateHierarchy.State> states, String prefix) {
        StringBuilder result_ = new StringBuilder();
        for( StateHierarchy.State state : states ) {
            result_.append(state(state,prefix));
            result_.append(newline);
        }
        return result_.toString();
    }

    private static String state(State state, String prefix) {
        return prefix +
            "<plural:state name=\"" + state.name() + "\">" +
            newline + dimensions(state.getChildren(), prefix+"\t") +
            prefix + "</plural:state>";
    }

    private static String dimensions(Collection<Dimension> dims, String prefix) {
        StringBuilder result_ = new StringBuilder();
        for( StateHierarchy.Dimension dim : dims ) {
            result_.append(dim(dim,prefix));
            result_.append(newline);
        }
        return result_.toString();
    }

    private static Object dim(Dimension dim, String prefix) {
        return prefix +  
        "<plural:dim name=\"" + dim.name() + "\">" + newline + 
        states(dim.getChildren(), prefix+"\t") +
        prefix + "</plural:dim>";
    }

    private static String method(MethodGraph method) {
        StringBuilder builder = new StringBuilder();
        
        // method name
        builder.append("<data key=\"mname\">");
        builder.append(method.methodName());
        builder.append("</data>");
        builder.append(newline);
        
        // is constructor?
        builder.append("<data key=\"isctr\">");
        builder.append(method.isConstructor());
        builder.append("</data>");
        builder.append(newline);
        
        // overrides
        for( String overridden : overridden(method) ) {
            builder.append(overridden);
            builder.append(newline);
        }
        
        for( String edge : edges(method) ) {
            builder.append(edge);
            builder.append(newline);
        }
        
        builder.append(newline);
        
        for( String node : nodes(method) ) {
            builder.append(node);
            builder.append(newline);
        }
        
        return builder.toString();
    }

    private static Iterable<String> overridden(MethodGraph method) {
        // <key for="graph" id="overrides" attr.name="overriddenmethods" attr.type="string"/>
        Set<String> result = new HashSet<String>();
        for( String key : method.getOverridenNodes() ) {
            String str = "<data key=\"overrides\">" + key + "</data>";
            result.add(str);
        }
        return result;
    }

    private static Iterable<String> edges(MethodGraph method) {
        // set prevents duplicate edges (impossible?)
        Set<String> result = new HashSet<String>();
        for( Node node : method.getNodes() ) {
            // modifies result
            edgesHelper(node, result);
        }
        return result;
    }
    
    private static void edgesHelper(Node node1, Set<String> result) {
        for( Node node2 : node1.getAdjacentNodes() ) {
            String edge = edge(node1, node2);
            if( !result.contains(edge) ) {
                result.add(edge);
                edgesHelper(node2, result);
            }
        }
    }
    
    private static String edge(Node node1, Node node2) {
        return "<edge source=\"" + node1.nodeID() + "\" " +
        		"target=\"" + node2.nodeID() + "\"/>";
    }

    private static Iterable<String> nodes(MethodGraph method) {
        Set<String> result = new HashSet<String>();
        for( Node node : method.getNodes() ) {
            // modifies result
            nodesHelper(node, result);
        }
        return result;
    }

    private static void nodesHelper(Node node, Set<String> result) {
        String node_str = node(node);
        if( !result.contains(node_str) ) {
            result.add(node_str);
            for( Node child : node.getAdjacentNodes() ) {
                nodesHelper(child, result);
            }
        }
    }

    private static String node(Node node) {
        return node.toXML() + newline;
    }  
}
