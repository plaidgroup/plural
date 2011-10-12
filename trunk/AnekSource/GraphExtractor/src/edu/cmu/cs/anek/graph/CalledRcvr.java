package edu.cmu.cs.anek.graph;


import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.util.Option;

/**
 * A node representing a pre- or post-condition to the receiver
 * in a method being called inside the method under inference.
 * 
 * @author Nels E. Beckman
 *
 */
/**
 * @author Nels E. Beckman
 *
 */
public final class CalledRcvr implements NodeSpecifics {

    // Call site id
    private final long siteID;
    
    private final ParameterDirection dir;
    
    private final Permission perm;
    
    private final String methodKey;
    
    private final Option<String> methodQualifiedName;
    
    // TODO: Hackish...
    // Is the called method private?
    private final boolean isPrivate;

    public CalledRcvr(long siteID, ParameterDirection dir, Permission perm,
            String methodKey, Option<String> methodQualifiedName,
            boolean isPrivate) {
        this.siteID = siteID;
        this.dir = dir;
        this.perm = perm;
        this.methodKey = Utilities.legalNMToken(methodKey);
        this.methodQualifiedName = methodQualifiedName;
        this.isPrivate = isPrivate;
    }

    public CalledRcvr(long siteID, ParameterDirection dir, Permission perm,
            String methodKey, String methodQualifiedName, boolean isPrivate) {
        this(siteID, dir, perm, methodKey, 
                Option.some(methodQualifiedName), isPrivate);
    }
    
    @Override
    public String id() {
        return "Arg-Rcvr-" + dir + "-" + methodKey + "-" + siteID;
    }
    
    @Override
    public String toString() {
        return this.toXML("");
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        String m_q_name = methodQualifiedName.isNone() ? "" :
                "method=\"" + methodQualifiedName.unwrap() + "\"";
        return prefix + "<plural:called-receiver " +
            m_q_name + 
            " methodKey=\"" + methodKey +
            "\" direction=\"" + dir + "\" siteID=\"" + siteID +
            "\" isPrivate=\"" + Boolean.toString(isPrivate) +
            "\">" + newline
            + perm.toXML(prefix + "  ") +
            newline + prefix + "</plural:called-receiver>";
    }

    @Override
    public Permission getPermission() {
        return this.perm;
    }
}
