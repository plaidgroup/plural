package edu.cmu.cs.anek.graph;


import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.util.Utilities;

/**
 * The receiver of the method currently under analysis.
 * 
 * @author Nels E. Beckman
 *
 */
public final class Receiver implements NodeSpecifics {

    private final ParameterDirection direction;
    
    private final Permission permission;
    
    // The key of the method from which this is a return
    private final String methodKey;
    
    public Receiver(ParameterDirection direction, 
            Permission permission,String methodKey) {
        this.direction = direction;
        this.permission = permission;
        this.methodKey = Utilities.legalNMToken(methodKey);
    }

    @Override
    public String id() {
        return "THIS-" + methodKey +"-"+ direction;
    }

    // essentially un-does the above method
    public static String keyFromID(String id) {
        int start = id.indexOf('-');
        int end = id.lastIndexOf('-');
        return id.substring(start+1, end);
    }
    
    @Override
    public String toString() {
        return "<THIS, " + direction + " - " + permission + ">";
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        return prefix + "<plural:this direction=\""+ direction +"\">" +
            newline + permission.toXML(prefix + "  ") +
            newline + prefix + "</plural:this>";
    }

    @Override
    public Permission getPermission() {
        return this.permission;
    }
    
}
