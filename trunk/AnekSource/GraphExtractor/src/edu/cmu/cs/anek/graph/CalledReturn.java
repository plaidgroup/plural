package edu.cmu.cs.anek.graph;


import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.util.Option;

/**
 * A return from a called method. 
 * @author Nels E. Beckman
 *
 */
public final class CalledReturn implements NodeSpecifics {

    private final long siteID;
    
    private final Permission perm;
    
    private final String methodKey;
    
    private final Option<String> methodQualifiedName;

    
    
    public CalledReturn(long siteID, Permission perm, String methodKey,
            Option<String> methodQualifiedName) {
        this.siteID = siteID;
        this.perm = perm;
        this.methodKey = Utilities.legalNMToken(methodKey);
        this.methodQualifiedName = methodQualifiedName;
    }

    public CalledReturn(long siteID, Permission perm, String methodKey,
            String methodQualifiedName) {
        this(siteID, perm, methodKey, Option.some(methodQualifiedName));
    }

    @Override
    public String id() {
        return "Return-" + methodKey + "-" + siteID;
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        String m_q_name = methodQualifiedName.isNone() ? "" :
            "method=\"" + methodQualifiedName.unwrap() + "\"";
        return prefix + "<plural:called-return " +
            m_q_name + " methodKey=\"" + methodKey + 
            "\" siteID=\"" + siteID + "\">" + newline + 
            perm.toXML(prefix + "  ") +
            newline + prefix + "</plural:called-return>";
    }


    @Override
    public Permission getPermission() {
        return this.perm;
    }

}
