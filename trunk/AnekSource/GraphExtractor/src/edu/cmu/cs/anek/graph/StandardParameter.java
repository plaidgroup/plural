package edu.cmu.cs.anek.graph;


import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.util.Utilities;

/**
 * Specifics for a standard method parameter. Parameters are of the
 * method currently under analysis. They are standard if they are
 * not fields, receivers or return values.
 * 
 * @author Nels E. Beckman
 *
 */
public final class StandardParameter implements NodeSpecifics {

    private final ParameterDirection direction;
    
    private final Permission permission;
    
    private final String key;
    private final String name;
    private final int paramPos;
    
    public StandardParameter(ParameterDirection direction,
            Permission permission, String key, String name,
            int paramPos) {
        this.direction = direction;
        this.permission = permission;
        this.key = Utilities.legalNMToken(key);
        this.name = name;
        this.paramPos = paramPos;
    }

    @Override
    public String id() {
        return "Param-" + key + "-" + direction;
    }

    // UNDOES the above method
    public static String methodKeyFromID(String id) {
        id = id.substring("Param-".length());
        return id.split("-(PRE|POST)$")[0];
    }
    
    @Override
    public String toString() {
        return "<" + name + ", " + direction + " - " + permission + ">";
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        return prefix + "<plural:standard-parameter direction=\"" + direction.toString() +
            "\" name=\"" + name + "\" pos=\"" + paramPos + "\">" +
            newline + permission.toXML(prefix + "  ") +
            newline + prefix + "</plural:standard-parameter>";
    }

    @Override
    public Permission getPermission() {
        return this.permission;
    }
}
