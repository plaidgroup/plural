package edu.cmu.cs.anek.graph.permissions;

/**
 * An unground permission... a permission that we may be trying
 * to infer.
 * 
 * @author Nels E. Beckman
 *
 */
public final class UnGroundPermission implements Permission {

    public static final UnGroundPermission INSTANCE = 
        new UnGroundPermission();
    
    private UnGroundPermission() {}
    
    @Override
    public ConcretePermission getGround() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This permission is not ground.");
    }

    @Override
    public boolean isGround() {
        return false;
    }

    @Override
    public String toString() {
        return "UNGROUND";
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        return prefix + "<plural:permission>" + newline +
            prefix + "  <plural:unground-perm/>" + newline +
            prefix + "</plural:permission>";
        
    }

    @Override
    public Permission copyWithNewUsage(PermissionUse use) {
        return this;
    }
}
