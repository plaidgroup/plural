package edu.cmu.cs.anek.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.anek.graph.permissions.Permission;

/**
 * All nodes in a graph, regardless of what kind they are,
 * can return the next adjacent nodes in the graph. This type
 * captures that property.
 * @author Nels E. Beckman
 *
 */
public final class Node {
    
    private final String typeName;
    
    private final NodeSpecifics specifics;
    
    private final Set<Node> adjacentNodes = new HashSet<Node>();
    
    private boolean isSynchronized;
    
    public Node(String typeName, NodeSpecifics specifics) {
        this(typeName,specifics,false);
    }
    
    public Node(String typeName, NodeSpecifics specifics, boolean isSynchronized) {
        if( "boolean".equals(typeName) )
            throw new RuntimeException("Trying to create node with primitive type!" + specifics);
            
        this.typeName = typeName;
        this.specifics = specifics;
        this.isSynchronized = isSynchronized;
    }
    
    public Collection<Node> getAdjacentNodes() {
        return Collections.unmodifiableSet(this.adjacentNodes);
    }
    
    public void addAdjacentNode(Node node) {
        this.adjacentNodes.add(node);
    }
    
    public void removeAdjacent(Node to_remove) {
        this.adjacentNodes.remove(to_remove);
    }
    
    /**
     * Every node must have an ID the unambiguously refers to
     * that node.
     */
    public String nodeID() {
        return specifics.id();
    }
    
    /**
     * Is this node synchronized? Updates the field.
     */
    public void setSynchronized(boolean b) {
        this.isSynchronized = b;
    }
    
    @Override
    public String toString() {
        return specifics.toString();
    }

    public static String newline = System.getProperty("line.separator");
    
    /**
     * Generate a representation in GraphML + Plural.
     */
    public String toXML() {
        StringBuilder result_ = new StringBuilder("<node id=\"");
        result_.append(this.nodeID());
        result_.append("\">");
        result_.append(newline);
        // type name
        result_.append("  <data key=\"tname\">");
        result_.append(typeName);
        result_.append("</data>");
        result_.append(newline);
        // is synchronized?
        result_.append("  <data key=\"synced\">");
        result_.append(this.isSynchronized);
        result_.append("</data>");
        result_.append(newline);
        
        result_.append("  <data key=\"nodespecskey\">");
        result_.append(newline);
        result_.append("    <plural:node-specifics>");
        result_.append(newline);
        result_.append(this.specifics.toXML("      "));
        result_.append(newline);
        result_.append("    </plural:node-specifics>");
        result_.append(newline);
        result_.append("  </data>");
        result_.append(newline);
        result_.append("</node>");
        return result_.toString();
    }

    /**
     * Also known as the INSERT HACK HERE method.
     */
    public NodeSpecifics getSpecifics() {
        return this.specifics;
    }

    /**
     * Get the permissions associated with this node.
     */
    public Permission getPermission() {
        return this.specifics.getPermission();
    }
}
