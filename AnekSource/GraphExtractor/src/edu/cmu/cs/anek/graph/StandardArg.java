package edu.cmu.cs.anek.graph;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.util.Option;

/**
 * Holds the specifics for an argument to a call site. It's 
 * standard because it's not a return value, rcvr or field.
 * 
 * @author Nels E. Beckman
 *
 */
public final class StandardArg implements NodeSpecifics {

    // Call site id
    private final long siteID;
    // argument position
    private final int argPos;
    
    private final ParameterDirection dir;
    
    private final Permission perm;
    
    private final Option<String> methodQualifiedName;
    
    private final String methodKey;

    
    public StandardArg(long siteID, int argPos, ParameterDirection dir,
            Permission perm, Option<String> qualifiedName, String key) {
        this.siteID = siteID;
        this.argPos = argPos;
        this.dir = dir;
        this.perm = perm;
        this.methodQualifiedName = qualifiedName;
        this.methodKey = Utilities.legalNMToken(key);
    }

    public StandardArg(long siteID, int argPos, ParameterDirection dir,
            Permission perm, String qualifiedName, String key) {
        this(siteID,argPos,dir,perm,Option.some(qualifiedName),key);
    }
    
    @Override
    public String id() {
        return "Arg-" + argPos + "-" + dir + "-" + methodKey + "-" + siteID;
    }
    
    @Override
    public String toString() {
        return this.toXML("");
    }
    
    private static long NEXT_ID = 0l;
    // TODO Really need to get rid of this map b/c it'll just build up over time.
    private static final Map<ASTNode,Long> ids = 
        new WeakHashMap<ASTNode,Long>();
    
    /**
     * Given a method invocation, returns a long that
     * uniquely identifies that call site.
     */
    public static synchronized long siteID(ASTNode key) {
        if( !ids.containsKey(key) ) {
            ids.put(key, NEXT_ID++);
        }
        return ids.get(key);
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        String m_q_name = methodQualifiedName.isNone() ? "" :
            "method=\"" + methodQualifiedName.unwrap() + "\"";
        return prefix + "<plural:standard-argument " +
            m_q_name + " methodKey=\"" + methodKey +
            "\" argPos=\"" + argPos + "\" direction=\"" + dir +
            "\" siteID=\"" + siteID + "\">" + newline +
            perm.toXML(prefix + "  ") + 
            newline + prefix + "</plural:standard-argument>";
    }

    @Override
    public Permission getPermission() {
        return this.perm;
    }
}
