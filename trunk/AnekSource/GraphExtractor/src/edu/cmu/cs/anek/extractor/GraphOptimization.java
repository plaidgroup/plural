package edu.cmu.cs.anek.extractor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.cmu.cs.anek.graph.FieldLoad;
import edu.cmu.cs.anek.graph.FieldStore;
import edu.cmu.cs.anek.graph.MergeNode;
import edu.cmu.cs.anek.graph.MethodGraph;
import edu.cmu.cs.anek.graph.Node;

/**
 * Code for optimizing graphs by removing unnecessary nodes.<br>
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 */
final class GraphOptimization {

    /**
     * Remove any MERGE nodes that have no children. This requires
     * nodes to be removed as children from any predecessors.
     */
    static MethodGraph removeMergeDeadends(MethodGraph method) {
        Set<Node> old_nodes = method.getNodes();
        Set<Node> new_nodes = new HashSet<Node>(old_nodes);
        Set<Node> to_remove = new LinkedHashSet<Node>();
        
        // First find all merges w/o children
        for( Node node : old_nodes ) {
            if( node.getSpecifics() instanceof MergeNode ) {
                // Does it have children?
                if( node.getAdjacentNodes().isEmpty() ) {
                    // Don't add it to new_nodes... AND
                    // remember to remove it as a child...
                    to_remove.add(node);
                }
            }
        }
        
        // Then, we don't want to remove the nodes if some
        // fields are using it as a receiver
        for( Node node : old_nodes ) {
            if( node.getSpecifics() instanceof FieldLoad ) {
                to_remove.removeAll(((FieldLoad) node.getSpecifics()).getReceivers());
            }
            else if( node.getSpecifics() instanceof FieldStore ) {
                to_remove.removeAll(((FieldStore) node.getSpecifics()).getReceivers());
            }
        }
        
        // Finally, remove those nodes from the adjacent sets
        // of the remaining nodes.
        for( Node node : new_nodes ) {
            for( Node removed_node : to_remove ) {
                // Usually this will do nothing...
                node.removeAdjacent(removed_node);
            }
        }
        
        return method.copyWithNewNodes(new_nodes);
    }
    
}
