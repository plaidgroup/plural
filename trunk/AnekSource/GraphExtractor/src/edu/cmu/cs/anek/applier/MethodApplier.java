package edu.cmu.cs.anek.applier;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;


import edu.cmu.cs.anek.graph.MethodGraph;
import edu.cmu.cs.anek.graph.Node;
import edu.cmu.cs.anek.graph.ParameterDirection;
import edu.cmu.cs.anek.graph.Receiver;
import edu.cmu.cs.anek.graph.Return;
import edu.cmu.cs.anek.graph.StandardParameter;
import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.graph.permissions.UnGroundPermission;
import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;

/**
 * This class contains the code for applying a single MethodGraph
 * (with inferred permissions) to a single MethodDeclaration.
 * <br>
 * अनेक<br>
 * Anek<br>
 * 
 * @author Nels E. Beckman
 *
 */
public final class MethodApplier {

    /**
     * Apply the permissions in mGraph to the given method.
     */
    public static Collection<AnnotationDiff> apply(MethodGraph mGraph, MethodDeclaration method) {
        Map<String,Node> idToNodeMap =
            createIDToNodeMap(mGraph);
        return (new MethodApplier(idToNodeMap)).visit(method);
    }

    
    private static Map<String, Node> createIDToNodeMap(MethodGraph mGraph) {
        Map<String,Node> result = new HashMap<String,Node>();
        for( Node n : mGraph.getNodes() ) {
            result.put(n.nodeID(), n);
        }
        return result;
    }

    private final Map<String,Node> idToNodeMap;
    
    public MethodApplier(Map<String, Node> idToNodeMap) {
        this.idToNodeMap = idToNodeMap;
    }

    private Collection<AnnotationDiff> visit(MethodDeclaration d) {
        List<AnnotationDiff> result = new LinkedList<AnnotationDiff>();
        
        AtPermCreator apc = AtPermCreator.EMPTY;
        
        int param_num = 0;
        for( Object param_ : d.parameters() ) {
            SingleVariableDeclaration param =
                (SingleVariableDeclaration) param_;
            Pair<AnnotationDiff, AtPermCreator> p_result = parameter(param, param_num);
            
            apc = apc.combine(p_result.snd());
            result.add(p_result.fst());
            
            param_num++;
        }
        
        if( !Modifier.isStatic(d.resolveBinding().getModifiers()) ) {
            // do the receiver annotation, if the method is not
            // a static one.
            Pair<AnnotationDiff, AtPermCreator> p_result = receiver(d);
            
            apc = apc.combine(p_result.snd());
            result.add(p_result.fst());
        }
        
        ITypeBinding return_type = d.resolveBinding().getReturnType();
        if( Utilities.isReferenceType(return_type) ) {
            // If return type is not void, or a primitive, array... then
            // we care!
            AnnotationDiff p_result = returnSpec(d);
            
            result.add(p_result);
        }
        
        // remove ALL PLURAL METHOD ANNOTATIONS
        AnnotationDiff remove_diff =
            AnnotationGeneration.removeAllPluralAnnotations(d);
        
        result.add(remove_diff);
        result.add(apc.toDiff(d.getStartPosition()));
        return result;
    }

    private AnnotationDiff returnSpec(MethodDeclaration d) {
        Return spec =
            new Return(UnGroundPermission.INSTANCE, d.resolveBinding().getKey());
        String spec_id = spec.id();
        if( this.idToNodeMap.containsKey(spec_id) ) {
            Node n = idToNodeMap.get(spec_id);
            int offset = d.getStartPosition();
            return
                AnnotationGeneration.insertResultAnnotation(n.getPermission(), offset);
        }
        return NullDiff.INSTANCE;
    }


    // creates diffs for a receiver specification of the given method.
    private Pair<AnnotationDiff, AtPermCreator> receiver(MethodDeclaration d) {
        Receiver pre_spec =
            new Receiver(ParameterDirection.PRE,
                    // TODO this is kind of bad, we are relying on the
                    // fact that the permission is not involved in the key.
                    UnGroundPermission.INSTANCE, d.resolveBinding().getKey());
        // TODO Unnecessary legalNMToken call??
        String pre_key = Utilities.legalNMToken(pre_spec.id());
        
        Receiver post_spec =
            new Receiver(ParameterDirection.POST,
                    // TODO same warning as above
                    UnGroundPermission.INSTANCE, d.resolveBinding().getKey());
        // TODO Unnecessary legalNMToken call??
        String post_key = Utilities.legalNMToken(post_spec.id());
        
        Option<Permission> pre = Option.none();
        if( this.idToNodeMap.containsKey(pre_key) && !d.resolveBinding().isConstructor() ) {
            // For constructors, we just pretend there is nothing...
            Node pre_ = this.idToNodeMap.get(pre_key);
            pre = Option.some(pre_.getPermission());
        }
        
        Option<Permission> post = Option.none();
        if( this.idToNodeMap.containsKey(post_key) ) {
            Node post_ = this.idToNodeMap.get(post_key);
            post = Option.some(post_.getPermission());
        }
        
        String name_in_at_perm = "this";
        int offset = d.getStartPosition();
        return AnnotationGeneration.insertPermAnnotation(pre, post, name_in_at_perm, offset);
    }


    // Applies the pre and post nodes of the given parameter
    // if they can be found.
    private Pair<AnnotationDiff, AtPermCreator> 
        parameter(SingleVariableDeclaration param, int paramNum) {
        
        IVariableBinding m_binding = param.resolveBinding();
        StandardParameter pre_spec =
            new StandardParameter(ParameterDirection.PRE,
                    // TODO this is kind of bad, we are relying on the
                    // fact that the permission is not involved in the key.
                    UnGroundPermission.INSTANCE, m_binding.getKey(), 
                    m_binding.getName(), paramNum);
        String pre_key = Utilities.legalNMToken(pre_spec.id());
        
        StandardParameter post_spec =
            new StandardParameter(ParameterDirection.POST,
                    // TODO same warning as above
                    UnGroundPermission.INSTANCE, m_binding.getKey(), 
                    m_binding.getName(), paramNum);
        String post_key = Utilities.legalNMToken(post_spec.id());
        
        
        Option<Permission> pre = Option.none();
        if( this.idToNodeMap.containsKey(pre_key) ) {
            Node pre_ = this.idToNodeMap.get(pre_key);
            pre = Option.some(pre_.getPermission());
        }
        
        Option<Permission> post = Option.none();
        if( this.idToNodeMap.containsKey(post_key) ) {
            Node post_ = this.idToNodeMap.get(post_key);
            post = Option.some(post_.getPermission());
        }
        
        
        String name_in_at_perm = "#" + Integer.toString(paramNum);
        int offset = param.getStartPosition();
        return AnnotationGeneration.insertPermAnnotation(pre, post,
                    name_in_at_perm, offset);
    }
    
    
}
