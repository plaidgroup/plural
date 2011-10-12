package edu.cmu.cs.anek.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.cs.anek.util.Utilities;

/**
 * The graph from a single method body.
 * @author Nels E. Beckman
 *
 */
public final class MethodGraph {
    
    /**
     * This set contains all of the nodes in the graph.
     */
    private final Set<Node> nodes;

    private final String key;
    
    private final String methodName;
    
    private final boolean isConstructor;
    
    // KEYS for overridden methods
    private final Set<String> overriddenMethods;
    
    public MethodGraph(Set<Node> nodes, String key, 
            String methodName, boolean isConstructor, 
            Set<String> overriddenMethods) {
        this.nodes = nodes;
        this.key = Utilities.legalNMToken(key);
        this.methodName = methodName;
        this.isConstructor = isConstructor;
        this.overriddenMethods = makeNM(overriddenMethods);
    }

    private Set<String> makeNM(Collection<String> keys) {
        Set<String> result = new TreeSet<String>();
        for( String key : keys ) {
            result.add(Utilities.legalNMToken(key));
        }
        return result;
    }
    
    public MethodGraph copyWithNewNodes(Set<Node> new_nodes) {
        return new MethodGraph(new_nodes,key,methodName,isConstructor,overriddenMethods);
    }
    
    // Outputs the entire method graph.
    // This method performs depth-first search on the graph.
    @Override
    public String toString() {
        Set<Node> visited = new HashSet<Node>();
        StringBuilder result = new StringBuilder("");
             
        for( Node node : this.nodes ) {
            result.append(toStringHelper(node, visited));
        }
        
        return result.toString();
    }
    
    public static String newline = System.getProperty("line.separator");
    
    private static String toStringHelper(Node node, Set<Node> visited) {
        if( visited.contains(node) ) {
            return "";
        }
        else {
            StringBuilder result = new StringBuilder("");
            
            // Print children
            for( Node child : node.getAdjacentNodes() ) {
                result.append(node.toString());
                result.append(" ==> ");
                result.append(child.toString());
                result.append(newline);
            }
            
            visited.add(node);
            
            result.append(newline);
            
            // Continue down node
            for( Node child : node.getAdjacentNodes() ) {
                result.append(toStringHelper(child, visited));
            }
            return result.toString();
        }
    }

    public Set<Node> getNodes() {
        return Collections.unmodifiableSet(this.nodes);
    }

    public String id() {
        return this.key;
    }
    
    public String methodName() {
        return this.methodName;
    }
    
    public boolean isConstructor() {
        return this.isConstructor;
    }

    public Set<String> getOverridenNodes() {
        return Collections.unmodifiableSet(this.overriddenMethods);
    }
}
