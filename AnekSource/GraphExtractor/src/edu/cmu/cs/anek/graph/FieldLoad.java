package edu.cmu.cs.anek.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;


import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.util.Utilities;

/**
 * This node represents the pre- or post-condition on a field
 * before the  
 * 
 * @author Nels E. Beckman
 *
 */
public final class FieldLoad implements NodeSpecifics {

    private final long siteID;
    private final Permission perm;
    
    private final Set<Node> rcvrNodes = new HashSet<Node>();
    
    private final String key;
    
    private final String qualifiedName;
    
    private final boolean isStatic;

    public FieldLoad(long siteID, Permission perm, String key,
            String qualifiedName, boolean isStatic) {
        this.siteID = siteID;
        this.perm = perm;
        this.key = Utilities.legalNMToken(key);
        this.qualifiedName = qualifiedName;
        this.isStatic = isStatic;
    }

    @Override
    public String id() { 
        return "FieldLoad-" + key + "-" + siteID;
    }

    public static String keyFromID(String id) {
        return
            id.subSequence(id.indexOf('-'), id.lastIndexOf('-')).toString();
    }
    
    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        String is_static = Boolean.toString(isStatic);
        return prefix + "<plural:field-load siteID=\"" + siteID + 
            "\" field-name=\"" + qualifiedName +
            "\" static=\"" + is_static + "\">" + newline +
            perm.toXML(prefix + "  ") + newline + 
            FieldStore.rcvrNodesToXML(rcvrNodes, prefix + "  ") + 
            newline + prefix + "</plural:field-load>";
    }
    
    private static long NEXT_ID = 0l;
    // TODO Really need to get rid of this map b/c it'll just build up over time.
    private static final Map<ASTNode,Long> ids = 
        new HashMap<ASTNode,Long>();
    
    /**
     * Given a method invocation, returns a long that
     * uniquely identifies that call site.
     */
    public static synchronized long nodeToSiteID(ASTNode fieldAccess) {
        if( !ids.containsKey(fieldAccess) ) {
            ids.put(fieldAccess, NEXT_ID++);
        }
        return ids.get(fieldAccess);
    }

    /**
     * Hack method...
     */
    public void addRcvrNode(Node rcvrNode) {
        rcvrNodes.add(rcvrNode);
    }

    public Collection<Node> getReceivers() {
        return Collections.unmodifiableSet(this.rcvrNodes);
    }
    
    @Override
    public Permission getPermission() {
        return this.perm;
    }
}
