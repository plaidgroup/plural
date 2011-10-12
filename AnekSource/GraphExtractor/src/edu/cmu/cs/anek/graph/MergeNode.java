package edu.cmu.cs.anek.graph;

import java.util.concurrent.atomic.AtomicLong;

import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.graph.permissions.UnGroundPermission;

/**
 * A merge node is a node where a number of permissions come together.
 * Merge nodes are typically inserted after method calls, as a joining
 * point for any permission that did NOT go into the method call.
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 *
 */
public final class MergeNode implements NodeSpecifics {

  private final Permission perm;
    
    private final String key;
    
    private static final AtomicLong MERGE_IDS = new AtomicLong();
    
    public MergeNode() {
        this(UnGroundPermission.INSTANCE, 
                Long.toString(MERGE_IDS.getAndIncrement()));
    }
    
    public MergeNode(Permission perm, String key) {
        super();
        this.perm = perm;
        this.key = key;
    }

    @Override
    public Permission getPermission() {
        return this.perm;
    }

    @Override
    public String id() {
        return "MERGE-" + key;
    }
    // un-does the above method
    public static String keyFromID(String id) {
        return id.substring("MERGE-".length());
    }
    
    private static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        return prefix +   
            "<plural:merge>" + newline +  
            getPermission().toXML(prefix + "  ") + newline + 
            prefix + "</plural:merge>";
    }
}
