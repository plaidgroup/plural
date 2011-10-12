package edu.cmu.cs.anek.graph.permissions;

/**
 * Interface representing all permissions, both concrete and
 * unground.
 * 
 * @author Nels E. Beckman
 *
 */
public interface Permission {
    
    /**
     * Is this permission ground?
     */
    public boolean isGround();
    
    /**
     * @return This permission as a ConcretePermission.
     * @throws UnsupportedOperationException If the permission is not ground.
     */
    public ConcretePermission getGround() throws UnsupportedOperationException;

    /**
     * Create an XML representation of this permission
     * (according to the GRAPML+PLURAL schema).
     * @param prefix 
     */
    public String toXML(String prefix);
    
    /**
     * If the permission is concrete, returns a new permission
     * that is identical except that all permissions have been
     * changed to the given use. If the permission is unground,
     * returns the exact same permission.
     */
    public Permission copyWithNewUsage(PermissionUse use);
}
