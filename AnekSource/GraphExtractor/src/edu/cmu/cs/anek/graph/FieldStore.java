package edu.cmu.cs.anek.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;


import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.util.Utilities;

/**
 * This node represents an assignment to some field.
 * @author Nels E. Beckman
 *
 */
public final class FieldStore implements NodeSpecifics {

    //private final IVariableBinding field;
    private final long siteID;
    private final Permission perm;
    
    private final Set<Node> receiverNodes;
    
    private final boolean isStatic;
    
    private final String key;
    
    private final String qualifiedName;
    
    public FieldStore(long siteID, Permission perm, Set<Node> receiverNodes,
            boolean isStatic, String key, String qualifiedName) {
        this.siteID = siteID;
        this.perm = perm;
        this.receiverNodes = receiverNodes;
        this.isStatic = isStatic;
        this.key = Utilities.legalNMToken(key);
        this.qualifiedName = qualifiedName;
    }

    @Override
    public String id() {
        return "FieldStore-" + key + "-" + siteID;
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        String is_static = Boolean.toString(isStatic);
        return prefix + "<plural:field-store siteID=\"" + siteID + 
            "\" field-name=\"" + qualifiedName +
            "\" static=\"" + is_static + "\">" + newline + 
            perm.toXML(prefix + "  ") + newline + 
            rcvrNodesToXML(receiverNodes, prefix + "  ") + 
            prefix + "</plural:field-store>";
    }
    
    static String rcvrNodesToXML(Set<Node> rns, String prefix) {
        StringBuilder result = new StringBuilder();
        for( Node rn : rns ) {
            result.append(prefix);
            result.append("<plural:receiver id=\"");
            result.append(rn.nodeID());
            result.append("\"/>");
            result.append(newline);
        }
        return result.toString();
    }

    @Override
    public Permission getPermission() {
        return this.perm;
    }

    public Collection<Node> getReceivers() {
        return Collections.unmodifiableSet(this.receiverNodes);
    }
}
