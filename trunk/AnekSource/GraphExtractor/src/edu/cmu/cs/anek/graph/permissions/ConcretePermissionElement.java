package edu.cmu.cs.anek.graph.permissions;

import java.util.Collections;
import java.util.Set;

import edu.cmu.cs.anek.graph.permissions.StateHierarchy.StateHierarchyNode;

/**
 * A concrete permission. Represents something like,  
 * share(a, GUARANTEE) in STATE. 
 * @author Nels E. Beckman
 *
 */
public final class ConcretePermissionElement {

    private final PermissionKind kind;
    /** 
     *  Fraction is just an object whose identity serves to denote that
     *  two objects must be or may not be equal.
     */
    private final Fraction fraction;
    
    private final PermissionUse use;
    
    private final StateHierarchyNode guarantee;
    private final Set<StateHierarchyNode> states;
    
    
    public ConcretePermissionElement(PermissionKind kind, Fraction fract, 
            PermissionUse use, StateHierarchyNode guarantee,
            Set<StateHierarchyNode> states) {
        this.kind = kind;
        this.fraction = fract;
        this.use = use;
        this.states = states;
        this.guarantee = guarantee;
    }

    public PermissionKind getKind() {
        return kind;
    }
    
    @Override
    public String toString() {
        return kind.toString().toLowerCase() +
            "(" + guarantee + ") in " + states.toString();
    }

    public static String newline = System.getProperty("line.separator");
    
    public String toXML(String prefix) {
        StringBuilder result_ = new StringBuilder(prefix);
        result_.append("<plural:concrete-perm-element ");
        result_.append("kind=\"");
        result_.append(kind.toString());
        result_.append("\" guarantee=\"");
        result_.append(guarantee.name());
        result_.append("\" fraction-id=\"");
        result_.append(fraction.fractionID());
        result_.append("\" usage=\"");
        result_.append(use.toString());
        result_.append("\">");
        result_.append(newline);
        
        for( StateHierarchyNode state : states ) {
            result_.append("  ");
            result_.append(prefix);
            result_.append("<plural:state name=\"");
            result_.append(state.name());
            result_.append("\"/>");
            result_.append(newline);
        }
        result_.append(prefix);
        result_.append("</plural:concrete-perm-element>");
        return result_.toString();
    }

    /**
     * Is this permission of the same fraction as the other, given
     * permission?
     */
    public boolean sameFraction(ConcretePermissionElement other) {
        return this.fraction.equals(other.fraction);
    }

    public StateHierarchyNode getGuarantee() {
        return this.guarantee;
    }

    public Set<StateHierarchyNode> getStates() {
        return Collections.unmodifiableSet(this.states);
    }

    public PermissionUse getUsage() {
        return this.use;
    }

    /**
     * Make an identical permission except with the given permission usage.
     */
    public ConcretePermissionElement copyWithNewUsage(PermissionUse use) {
        return new ConcretePermissionElement(kind,fraction,use,guarantee,states);
    }
}
