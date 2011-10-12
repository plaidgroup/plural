package edu.cmu.cs.anek.graph;

import java.util.concurrent.atomic.AtomicLong;

import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.graph.permissions.UnGroundPermission;

/**
 * This node is a placeholder node that represents the split in
 * permissions that occurs in a few places in code, typically
 * right before a method call. A split occurs because, in the
 * case where the method call returns no permission, we still
 * want to signify the fact that somer permission could come
 * <i>around</i> the method call.
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 *
 */
public final class SplitNode implements NodeSpecifics {

    private final Permission perm;
    
    private final String key;
    
    private static final AtomicLong SPLIT_IDS = new AtomicLong();
    
    public SplitNode() {
        this(UnGroundPermission.INSTANCE, 
                Long.toString(SPLIT_IDS.getAndIncrement()));
    }
    
    public SplitNode(Permission perm, String key) {
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
        return "SPLIT-" + key;
    }
    // un-does the above method
    public static String keyFromID(String id) {
        return id.substring("SPLIT-".length());
    }
    
    private static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        return prefix +   
        "<plural:split>" + newline +  
        getPermission().toXML(prefix + "  ") + newline + 
        prefix + "</plural:split>";
    }
}
