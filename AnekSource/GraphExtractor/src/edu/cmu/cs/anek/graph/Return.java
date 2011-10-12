package edu.cmu.cs.anek.graph;


import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.util.Utilities;

/**
 * A return from the method currently under analysis.
 * 
 * @author Nels E. Beckman
 *
 */
public final class Return implements NodeSpecifics {

    private final Permission perm;
    
    // The key of the method from which this returns
    private final String methodKey;
    
    public Return(Permission perm,String methodKey) {
        this.perm = perm;
        this.methodKey = Utilities.legalNMToken(methodKey);
    }

    @Override
    public String id() {
        return "RETURN-" + methodKey;
    }
    
    // Undoes the above method
    public static String methodKeyFromID(String id) {
        return id.substring(1 + id.indexOf('-'));
    }
    
    @Override
    public String toString() {
        return "<RETURN - " + perm.toString() + ">"; 
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        return prefix + "<plural:return>" + newline +
            perm.toXML(prefix + "  ") +
            newline + prefix + "</plural:return>";
    }

    @Override
    public Permission getPermission() {
        return this.perm;
    }
}
