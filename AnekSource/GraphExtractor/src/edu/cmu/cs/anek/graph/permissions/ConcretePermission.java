package edu.cmu.cs.anek.graph.permissions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ConcretePermission implements Permission {

    private static final String TENSOR_STR = " (X) ";
    private final Set<ConcretePermissionElement> perms;
    
    public ConcretePermission(Set<ConcretePermissionElement> perms) {
        this.perms = perms;
    }

    @Override
    public ConcretePermission getGround() {
        return this;
    }

    @Override
    public boolean isGround() {
        return true;
    }

    @Override
    public String toString() {
        if( perms.isEmpty() ) return "1";
        
        StringBuilder result = new StringBuilder();
        for( ConcretePermissionElement p : perms ) {
            result.append(p.toString());
            result.append(TENSOR_STR);
        }
        // Remove last tensor
        result.delete(result.length() - TENSOR_STR.length(), result.length());
        return result.toString();
    }

    public static String newline = System.getProperty("line.separator");
    
    @Override
    public String toXML(String prefix) {
        StringBuilder result_ = new StringBuilder(prefix);
        result_.append("<plural:permission>");
        result_.append(newline);
        result_.append(prefix);
        result_.append("  <plural:concrete-perm>");
        result_.append(newline);
        for( ConcretePermissionElement perm : perms ) {
            result_.append(perm.toXML(prefix + "    "));
            result_.append(newline);
        }
        result_.append(prefix);
        result_.append("  </plural:concrete-perm>");
        result_.append(newline);
        result_.append(prefix);
        result_.append("</plural:permission>");
        return result_.toString();
    }

    public Set<ConcretePermissionElement> getPermissions() {
        return Collections.unmodifiableSet(this.perms);
    }

    @Override
    public Permission copyWithNewUsage(PermissionUse use) {
        Set<ConcretePermissionElement> perms = 
            new HashSet<ConcretePermissionElement>();
        for( ConcretePermissionElement perm : this.perms ) {
            perms.add(perm.copyWithNewUsage(use));
        }
        return new ConcretePermission(perms);
    }
}
