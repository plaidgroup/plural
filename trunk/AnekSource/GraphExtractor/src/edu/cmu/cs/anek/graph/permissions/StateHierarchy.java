package edu.cmu.cs.anek.graph.permissions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.util.Option;

/**
 * The state hierarchy for a type. A hierarchy is a tree of
 * nodes which are states and dimensions. The root is always
 * 'alive.' This information is extracted from annotations on
 * types in the extraction phase.
 * 
 * @author Nels E. Beckman
 *
 */
public final class StateHierarchy {
    
    public interface StateHierarchyNode {
        public Option<? extends StateHierarchyNode> getParent();
        public Collection<? extends StateHierarchyNode> getChildren();
        public String name();
        public StateHierarchyNode copy(Option<? extends StateHierarchyNode> parent);
    }
    
    public static class State implements StateHierarchyNode {
        private Option<Dimension> parent;
        private final Set<Dimension> children = new HashSet<Dimension>();
        private final String name;
        
        public State(String name) {
            this.name = name;
        }
        @Override
        public Collection<Dimension> getChildren() {
            return Collections.unmodifiableCollection(children);
        }
        @Override
        public Option<Dimension> getParent() {
            return parent;
        }
        @Override
        public String name() {
            return name;
        }
        public void addChild(Dimension dim) {
            this.children.add(dim);
        }
        public void setParent(Dimension dim) {
            this.parent = Option.some(dim);
        }
        
        @Override
        public String toString() {
            return name();
        }
        @Override
        public State copy(Option<? extends StateHierarchyNode> parent) {
            State result = new State(this.name());
            result.parent = parent.isNone() ? Option.<Dimension>none() : 
                // THE cast...
                Option.some((Dimension)parent.unwrap());
            
            for( Dimension dim : this.children ) {
                result.addChild(dim.copy(Option.some(result)));
            }
            
            return result;
        }
    }
    
    public static class Dimension implements StateHierarchyNode {
        private Option<State> parent;
        private final Set<State> children = new HashSet<State>();
        private final String name;

        public Dimension(String name) {
            this.name = name;
        }

        @Override
        public Collection<State> getChildren() {
            return Collections.unmodifiableCollection(children);
        }

        @Override
        public Option<State> getParent() {
            return parent;
        }

        @Override
        public String name() {
            return name;
        }

        public void setParent(State refined) {
            this.parent = Option.some(refined);
        }

        public void addChild(State state) {
            this.children.add(state);
        }
        
        @Override
        public String toString() {
            return name();
        }

        @Override
        public Dimension copy(Option<? extends StateHierarchyNode> parent) {
            Dimension result = new Dimension(this.name());
            result.parent = parent.isNone() ? Option.<State>none() : 
                // THE cast...
                Option.some((State)parent.unwrap());
            
            for( State state : this.children ) {
                result.addChild(state.copy(Option.some(result)));
            }
            
            return result;
        }
    }
    
    private final StateHierarchy.State alive;
    private final ITypeBinding type;
    private final Map<String,StateHierarchyNode> byName;
    
    public StateHierarchy(StateHierarchy.State alive, ITypeBinding type) {
        checkWellFormed(alive);
        this.byName = generateNameMap(alive, new HashMap<String,StateHierarchyNode>());
        this.alive = alive;
        this.type = type;
    }

    /**
     * Copy constructor.
     */
    public StateHierarchy(StateHierarchy original) {
        this(original.alive.copy(Option.<StateHierarchyNode>none()), original.type);
        
        // for each node in the old one, create a new node with the
        // same properties. Use the state/name node from the old one
        // to create a new one.
        
//        Map<String,StateHierarchyNode> new_nodes = new HashMap<String,StateHierarchyNode>();
//        
//        // create nodes
//        for( StateHierarchyNode old_node : original.byName.values() ) {
//            StateHierarchyNode new_node;
//            if( old_node instanceof Dimension )
//                new_node = new Dimension(old_node.name());
//            else
//                new_node = new State(old_node.name());
//            new_nodes.put(new_node.name(), new_node);
//        }
//        
//        // Hook them together
//        for( StateHierarchyNode old_node : original.byName.values() ) {
//            StateHierarchyNode new_node = new_nodes.get(old_node.name());
//            Option<? extends StateHierarchyNode> parent_ = old_node.getParent();
//            
//            if( parent_.isSome() ) {
//                StateHierarchyNode new_parent = new_nodes.get(parent_.unwrap().name());
//                new_node.
//            }
//        }
    }

    private static Map<String, StateHierarchyNode> generateNameMap(
            StateHierarchyNode node, Map<String, StateHierarchyNode> map) {
        map.put(node.name(), node);
        for( StateHierarchyNode child : node.getChildren() ) {
            generateNameMap(child, map);
        }
        return map;
    }

    private static void checkWellFormed(StateHierarchyNode alive) {
        if( !"alive".equals(alive.name()) )
            throw new IllegalArgumentException();
    }
    
    public Map<String, StateHierarchyNode> nameMap() {
        return Collections.unmodifiableMap(this.byName);
    }
    
    public StateHierarchyNode findbyName(String node) {
        if( !this.byName.containsKey(node) )
            throw new IllegalArgumentException();
        return this.byName.get(node);
    }
    
    public ITypeBinding getType() {
        return this.type;
    }

    public StateHierarchy.State getRoot() {
        return this.alive;
    }
}
