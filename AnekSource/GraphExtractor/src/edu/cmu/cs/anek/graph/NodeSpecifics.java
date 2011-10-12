package edu.cmu.cs.anek.graph;

import edu.cmu.cs.anek.graph.permissions.Permission;

/**
 * Our graph consists of nodes. In many ways it is useful to treat
 * those nodes uniformly. However, each node is also different in
 * man ways. Some nodes a rcvr nodes of the method under analysis.
 * Some nodes are arguments for a call function at a particular site.
 * Implementing classes of this interface will store the differences
 * between nodes.
 * 
 * @author Nels E. Beckman
 *
 */
public interface NodeSpecifics {
    /**
     * Return a unique representation for this node
     * based on its particulars.
     */
    public String id();

    /**
     * Generate a representation in GraphML + Plural.
     * @param prefix 
     */
    public String toXML(String prefix);

    /**
     * Gets the permission associated with this node.
     */
    public Permission getPermission();
}
