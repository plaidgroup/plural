package edu.cmu.cs.anek.extractor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;


import edu.cmu.cs.anek.eclipse.EclipseUtils;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy.Dimension;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy.State;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy.StateHierarchyNode;
import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Unique;

/**
 * Code for extracting state hierarchy information from
 * types and methods.
 * 
 * @author Nels E. Beckman
 *
 */
final class StateHierarchyExtractor {

    private static final String ANONYMOUS_DIM = "ANONYMOUS_DIM_";

    /**
     * Get the state hierarchy for the given type. This is done in a lazy
     * manner, so you must provide a map that potentially already contains
     * the result. If it does not, a new result is created and stored in the
     * map. This method will synchronize on the map given, so that the check
     * and creation is an atomic operation.
     */
    public static synchronized StateHierarchy lazilyExtractHierarchy(ITypeBinding type,
            Map<ITypeBinding,StateHierarchy> map) {
        type = EclipseUtils.canonicalType(type);

        synchronized(map) {
            // maybe we've already done it.
            if( map.containsKey(type) )
                return map.get(type);

            // What if this type has a parent that also has states? Include them.
            Option<StateHierarchy> parent_h = interestingParentHierarchy(type, map);
            
            IAnnotationBinding[] type_bindings = type.getAnnotations();
            Collection<IAnnotationBinding> state_annotations =
                Utilities.findAnnotations(type_bindings, States.class, Refine.class);
            if( state_annotations.size() > 0 || parent_h.isSome() ) {
                // nodes is modified by each call to add new nodes
                // by their names.
                Map<String,StateHierarchy.StateHierarchyNode> nodes =
                    new HashMap<String, StateHierarchyNode>();
                
                if( parent_h.isSome() ) {
                    // copy parent hierarchy, put those states in a map, add
                    // additional states.
                    StateHierarchy parent_sh_copy = new StateHierarchy(parent_h.unwrap());
                    nodes.putAll(parent_sh_copy.nameMap());
                }
                
                for( IAnnotationBinding anno : state_annotations ) {
                    extractStatesAnnotation(anno, nodes);
                }
                State alive = (State)nodes.get("alive");
                StateHierarchy result = new StateHierarchy(alive, type);
                map.put(type, result);
            }
            else {
                StateHierarchy result = extractImplicitHierarchy(type, type.getDeclaredMethods());
                map.put(type, result);
            }
            return map.get(type);
        }
    }

    /**
     * If any super-type has an interesting state hierarchy, return that.
     * Here interesting means, has more than just the 'alive' state.
     */
    private static Option<StateHierarchy> interestingParentHierarchy(
            ITypeBinding type, Map<ITypeBinding,StateHierarchy> map) {
        // TODO: Only works if at most one supertype has an interesting
        // hierarchy...
        if( type.getQualifiedName().equals("java.lang.Object") ) {
            return Option.none();
        }
        
        // Super class?
        if( type.getSuperclass() != null ) {
            StateHierarchy super_h = lazilyExtractHierarchy(type.getSuperclass(), map);
            if( super_h.getRoot().getChildren().size() > 0 ) {
                return Option.some(super_h);
            }
        }
        
        // interfaces?
        for( ITypeBinding interface_ : type.getInterfaces() ) {
            // TODO: Multiple interfaces could be interesting!
            StateHierarchy inter_h = lazilyExtractHierarchy(interface_, map);
            if( inter_h.getRoot().getChildren().size() > 0 ) {
                return Option.some(inter_h);
            }
        }
        
        return Option.none();
    }

    private static Dimension getOrCreateD(String node, 
            Map<String, StateHierarchyNode> nodes) {
        if( !nodes.containsKey(node) ) {
            StateHierarchyNode result = new Dimension(node);
            nodes.put(node, result);
        }
        return (Dimension)nodes.get(node);
    }
    
    private static State getOrCreateS(String node,
            Map<String, StateHierarchyNode> nodes) {
        if( !nodes.containsKey(node) ) {
            StateHierarchyNode result = new State(node);
            nodes.put(node, result);
        }
        return (State)nodes.get(node);
    }
    
    private static void extractStatesAnnotation(IAnnotationBinding anno,
            Map<String, StateHierarchyNode> nodes) {
        String[] states_s = statesStates(anno);
        String dim_s = dimStates(anno);
        String refined_s = refinedStates(anno);
        
        // dim is child of refined
        Dimension dim = getOrCreateD(dim_s, nodes);
        State refined = getOrCreateS(refined_s, nodes);
        dim.setParent(refined);
        refined.addChild(dim);
        
        // for each child, dim is parent
        for( String state_s : states_s ) {
            State state = getOrCreateS(state_s, nodes);
            dim.addChild(state);
            state.setParent(dim);
        }
    }

    // Assuming the given annotation is @States, return the refined
    // parameter.
    private static String refinedStates(IAnnotationBinding anno) {
        for( IMemberValuePairBinding pair : anno.getAllMemberValuePairs() ) {
            if( "refined".equals(pair.getName()) ) {
                return (String)pair.getValue();
            }
        }
        return Utilities.impossible();
    }

    // Assuming the given annotation is @States, return the dim
    // parameter, and if it's blank, make up an anonymous dimension.
    private static String dimStates(IAnnotationBinding anno) {
        for( IMemberValuePairBinding pair : anno.getAllMemberValuePairs() ) {
            if( "dim".equals(pair.getName()) ) {
                String dim = (String)pair.getValue();
                if( !"".equals(dim) )
                    return dim;
                else {
                    long id = ANON_DIM_ID.getAndIncrement();
                    return ANONYMOUS_DIM + id;
                }   
            }
        }
        return Utilities.impossible();
    }

    // Assuming the given annotation is @States, return the
    // value parameter.
    private static String[] statesStates(IAnnotationBinding anno) {
        for( IMemberValuePairBinding pair : anno.getAllMemberValuePairs() ) {
            if( "value".equals(pair.getName()) ) {
                Object value = pair.getValue();
                if( value instanceof String )
                    return new String[] { (String)value };
                else {
                    Object[] result = (Object[])pair.getValue();
                    return Arrays.copyOf(result, result.length, String[].class);
                }
            }
        }
        return Utilities.impossible();
    }
    
    // Atomic b/c I think it would be really easy to make this whole
    // GraphExtractor concurrent, and I don't want to ruin that
    // possibility.
    private final static AtomicLong ANON_DIM_ID = new AtomicLong(0l);

    // Get the hierarchy that is implied by the states mentioned in
    // pre- and post- conditions. This is not so bad, actually, since
    // it's a flat hierarchy. you just have to look at every method.
    private static StateHierarchy extractImplicitHierarchy(ITypeBinding type,
            IMethodBinding[] declaredMethods) {
        // there will be just one anonymous dimension, and it will be
        // a child of alive.
        State alive = new State("alive");
        Dimension anon = new Dimension(ANONYMOUS_DIM+ANON_DIM_ID.getAndIncrement());
        alive.addChild(anon);
        anon.setParent(alive);
        
        // so we don't get any duplicate states
        Set<String> found_states = new HashSet<String>();
        for( IMethodBinding method : declaredMethods ) {
            addRcvrStatesIfPresent(method, anon, found_states);
        }
        // But if we didn't find anything, we don't want the anon_dim.
        if( found_states.isEmpty() ) {
            alive = new State("alive");
        }
        return new StateHierarchy(alive, type);
    }

    // For the given method, find any annotations that are for the
    // receiver (share, pure, etc.) 
    private static void addRcvrStatesIfPresent(IMethodBinding method,
            Dimension parent, Set<String> foundStates) {
        for( IAnnotationBinding anno : method.getAnnotations() ) {
            String type = anno.getAnnotationType().getQualifiedName();
            if( Unique.class.getName().equals(type) || 
                Full.class.getName().equals(type) ||
                Pure.class.getName().equals(type) ||
                Share.class.getName().equals(type) ||
                Imm.class.getName().equals(type)) {
                
                for( String req_state : PermissionExtractor.requires(anno) ) {
                    if( !foundStates.contains(req_state) ) {
                        foundStates.add(req_state);
                        State req = new State(req_state);
                        req.setParent(parent);
                        parent.addChild(req);
                    }
                }
                
                for( String ens_state : PermissionExtractor.ensures(anno) ) {
                    if( !foundStates.contains(ens_state) ) {
                        foundStates.add(ens_state);
                        State ens = new State(ens_state);
                        ens.setParent(parent);
                        parent.addChild(ens);
                    }
                }
                
            }
            else if( Perm.class.getName().equals(type) ) {
                // TODO parse @Perm... arg....
                edu.cmu.cs.crystal.util.Utilities.nyi();
            }
        }
    }
}
