package edu.cmu.cs.anek.extractor;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import edu.cmu.cs.anek.eclipse.EclipseUtils;
import edu.cmu.cs.anek.eclipse.EclipseUtils.MethodHierarchyCallback;
import edu.cmu.cs.anek.graph.permissions.ConcretePermission;
import edu.cmu.cs.anek.graph.permissions.ConcretePermissionElement;
import edu.cmu.cs.anek.graph.permissions.Fraction;
import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.graph.permissions.PermissionKind;
import edu.cmu.cs.anek.graph.permissions.PermissionUse;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy.StateHierarchyNode;
import edu.cmu.cs.anek.graph.permissions.UnGroundPermission;
import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.perm.parser.Conjunction;
import edu.cmu.cs.plural.perm.parser.Identifier;
import edu.cmu.cs.plural.perm.parser.ParamReference;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.perm.parser.RefExpr;
import edu.cmu.cs.plural.perm.parser.TempPermission;
import edu.cmu.cs.plural.perm.parser.TopLevelPred;

/**
 * This class defines code to extract permissions
 * from Java annotations. These methods will generally
 * be called by {@link GraphExtractorVisitor} during the
 * extraction process.
 * 
 * @author Nels E. Beckman
 *
 */
final class PermissionExtractor {

    // modified by calls to StateHierarchyExtractor...
    private final Map<ITypeBinding,StateHierarchy> stateHierarchies;

    PermissionExtractor(Map<ITypeBinding,StateHierarchy> stateHierarchies) {
        this.stateHierarchies = stateHierarchies;
    }

    /**
     * Returns the @ResultXXX permission for the given method.
     * If there is not one, the permission will be an unground
     * permission.
     */
    Permission returnedPermission(IMethodBinding method) {
        // Do for every method overridden by this one...
        class MCallback extends MethodHierarchyCallback {
            // Start with unground, but replace if spec is found
            Permission result = UnGroundPermission.INSTANCE;
            @Override
            public boolean nextMethod(IMethodBinding method) {
                Permission p = returnedPermissionHelper(method);
                this.result = p;
                return p == UnGroundPermission.INSTANCE;
            }
        }
        
        MCallback result_callback = new MCallback();
        EclipseUtils.visitMethodHierarchy(result_callback, method, true);
        
        return result_callback.result;
    }
    
    private Permission returnedPermissionHelper(IMethodBinding method) {
        ITypeBinding return_type = method.getReturnType();
        if( !Utilities.isReferenceType(return_type) )
            throw new IllegalArgumentException("Primitive or void: " + method);

        // Do this just for effect... UGH
        // B/c it may not have annotations, but we still need its hierarchy
        StateHierarchyExtractor.lazilyExtractHierarchy(return_type, stateHierarchies);
        
        IAnnotationBinding[] bindings = method.getAnnotations(); 

        Set<ConcretePermissionElement> result = new HashSet<ConcretePermissionElement>();
        for( PermissionKind kind : PermissionKind.values() ) {
            Option<IAnnotationBinding> perm = 
                Utilities.findAnnotation(bindings, 
                        PermissionKind.kindResultAnnotation(kind));
            if( perm.isSome() )
                result.add(ofResult(perm.unwrap(), kind, return_type));
        }
        return result.isEmpty() ?
                UnGroundPermission.INSTANCE :
                new ConcretePermission(result);
    }

    /**
     * Returns the pre- and post-condition permissions for the
     * given variable. When parameters
     * have no annotations, this will be an unground permission.
     * 
     * TODO: The signature of this method will likely have to 
     * change once we support @Perm specifications.
     */
    Permission parameterPrePermissions(IMethodBinding method, int param_pos, ITypeBinding type) {
        return parameterPermissions(method, param_pos, type, true);    
    }

    Permission parameterPostPermissions(IMethodBinding method, int param_pos, ITypeBinding type) {
        return parameterPermissions(method, param_pos, type, false);    
    }

    private Permission parameterPermissions(IMethodBinding method, 
            final int param_pos, final ITypeBinding type, final boolean pre) {        
        if( !Utilities.isReferenceType(type) )
            throw new IllegalArgumentException("Primitive");
        
        class MCallback extends MethodHierarchyCallback {
            // Start with unground, but replace if spec is found
            Permission result = UnGroundPermission.INSTANCE;
            @Override
            public boolean nextMethod(IMethodBinding method) {
                IAnnotationBinding[] bindings = method.getParameterAnnotations(param_pos);
                Option<IAnnotationBinding> perm_anno_ = Utilities.findAnnotation(method.getAnnotations(), Perm.class);
                if( perm_anno_.isSome() ) {
                    // copy array, including @Perm
                    int old_size = bindings.length;
                    bindings = Arrays.copyOf(bindings, old_size + 1);
                    bindings[old_size] = perm_anno_.unwrap();
                }
                
                Permission p = rcvrParamPermissions(pre, bindings, type, Integer.toString(param_pos));
                this.result = p;
                return p == UnGroundPermission.INSTANCE;
            }
        }
        
        MCallback callback = new MCallback();
        EclipseUtils.visitMethodHierarchy(callback, method, true);
        return callback.result;
    }
    
    /**
     * Returns the pre-condition permissions for the
     * receiver. When parameters
     * have no annotations, this will be an unground permission. 
     */
    Permission rcvrPrePermissions(IMethodBinding method) {
        return rcvrPermissions(method, true);
    }
    /**
     * Returns the post-condition permissions for the
     * receiver. When parameters
     * have no annotations, this will be an unground permission. 
     */
    Permission rcvrPostPermissions(IMethodBinding method) {
        return rcvrPermissions(method, false);
    }

    private Permission rcvrPermissions(IMethodBinding method, final boolean pre_condition) {
        if( Modifier.isStatic(method.getModifiers()) )
            throw new IllegalArgumentException("No receiver " + method);
        
        
        // Do this for every method overridden in the hierarchy...
        class MCallback extends MethodHierarchyCallback {
            // Start with unground, but replace if spec is found
            Permission result = UnGroundPermission.INSTANCE;
            @Override
            public boolean nextMethod(IMethodBinding method) {
                // This code used to be in the body of the outer method...
                IAnnotationBinding[] bindings = method.getAnnotations();
                ITypeBinding rcvr_type = method.getDeclaringClass();
                Permission p = rcvrParamPermissions(pre_condition, bindings, rcvr_type,"this");
                this.result = p;
                // Continue until all methods have been exhausted or the permission
                // is ground.
                return p == UnGroundPermission.INSTANCE;
            }
        }
        MCallback result_callback = new MCallback();
        EclipseUtils.visitMethodHierarchy(result_callback, method, true);
        
        return result_callback.result;
    }

    /**
     * The only actual method that looks at the annotations and converts them
     * into permissions.
     */
    private Permission rcvrParamPermissions(boolean pre_condition,
            IAnnotationBinding[] bindings, ITypeBinding rcvr_type, String param_name) {
        Set<ConcretePermissionElement> result = new HashSet<ConcretePermissionElement>();
        
        // Do this just for effect... UGH
        // B/c it may not have annotations, but we still need its hierarchy
        StateHierarchyExtractor.lazilyExtractHierarchy(rcvr_type, stateHierarchies);
        
        boolean unground = true;
        for( PermissionKind kind : PermissionKind.values() ) {
            for( IAnnotationBinding kind_anno : 
                Utilities.findAnnotations(bindings, 
                        PermissionKind.kindAnnotation(kind), 
                        PermissionKind.kindsAnnotation(kind)) ) {
                unground = false;
                ConcretePermissionElement perm = ofKind(kind_anno, rcvr_type, kind, pre_condition);
                if( pre_condition )
                    result.add(perm);
                else if( !pre_condition && returned(kind_anno) )
                    result.add(perm);
            }
                
        }
        if( unground ) {
            Option<IAnnotationBinding> perm_anno_ = Utilities.findAnnotation(bindings, Perm.class);
            if( perm_anno_.isSome() ) {
                Set<ConcretePermissionElement> rcvr_perms =
                    ofAtPerm(perm_anno_.unwrap(), rcvr_type, param_name, pre_condition);
                if( !rcvr_perms.isEmpty() ) {
                    unground = false;
                    result.addAll(rcvr_perms);
                }
            }
        }
        return unground ? 
                UnGroundPermission.INSTANCE :
                new ConcretePermission(result);
    }
    
    // maps annotations to fractions for the case when a 
    // annotation is a borrowing annotation.
    private final Map<IAnnotationBinding,Fraction> fractionCache =
        new HashMap<IAnnotationBinding,Fraction>();
    
    /**
     * @param param_name 
     * @Perm -> Permission
     * Since there may be no receiver permission here, we have
     * to return NONE if we find out one isn't there...
     */
    private Set<ConcretePermissionElement> ofAtPerm(IAnnotationBinding anno, 
            ITypeBinding rcvr_type, String param_name, boolean pre_condition) {
        String str = pre_condition ? (String)attribute(anno,"requires") : (String)attribute(anno,"ensures");
        StateHierarchy hier = StateHierarchyExtractor.lazilyExtractHierarchy(rcvr_type, this.stateHierarchies);
        return tempPerms(str,param_name,hier);
    }
    
    private Set<ConcretePermissionElement> tempPerms(String perm_string,String var_name, StateHierarchy hier) {
        Option<TopLevelPred> pred = PermParser.parse(perm_string);
        if( pred.isSome() ) {
            return tempPerms(pred.unwrap(),var_name,hier);
        }
        else return Collections.emptySet();
    }
    
    private Set<ConcretePermissionElement> tempPerms(TopLevelPred pred,String var_name, StateHierarchy hier) {
        if( pred instanceof Conjunction ) {
            Conjunction conj = (Conjunction)pred;
            Set<ConcretePermissionElement> result = new HashSet<ConcretePermissionElement>();
            result.addAll(tempPerms(conj.getP1(),var_name,hier));
            result.addAll(tempPerms(conj.getP2(),var_name,hier));
            return result;
        }
        else if( pred instanceof TempPermission ) {
            TempPermission result_ = (TempPermission)pred;
            RefExpr ref = result_.getRef();
            boolean applies = false;
            PermissionUse use = null;
            if( ref instanceof Identifier && ((Identifier)ref).getName().equals(var_name) ) {
                applies = true;
                Identifier id = (Identifier)ref;
                use = PermissionUse.fromPluralUse(id.getUse());
            }
            else if( ref instanceof ParamReference && ((ParamReference)ref).getParamString().endsWith(var_name) ) {
                applies = true;
                use = PermissionUse.Virtual;
            }
            if( applies ) {
                Set<StateHierarchyNode> states_;
                StateHierarchyNode guarantee = hier.findbyName(result_.getRoot());
                if( result_.getStateInfo().length == 0 ) {
                    states_ = Collections.singleton(guarantee);
                }
                else {
                    states_ = new HashSet<StateHierarchyNode>();
                    for( String ensures : result_.getStateInfo() ) {
                        StateHierarchyNode ensures_ = hier.findbyName(ensures);
                        states_.add(ensures_);
                    }
                }
                ConcretePermissionElement result = new ConcretePermissionElement(
                        PermissionKind.valueOf(result_.getType().toUpperCase()),
                        new Fraction(),use,guarantee,states_);
                return Collections.singleton(result);
            }
            else {
                return Collections.emptySet();
            }
        }
        else {
            return edu.cmu.cs.crystal.util.Utilities.nyi();
        }
    }
    
    /**
     * @Kind -> Permission
     */
    private ConcretePermissionElement ofKind(IAnnotationBinding a, 
            ITypeBinding type, PermissionKind k, boolean pre) {
        StateHierarchy hier = StateHierarchyExtractor.lazilyExtractHierarchy(type, this.stateHierarchies);
        String guarantee_ = 
            "".equals(guarantee(a)) ? valueToString(a) : guarantee(a);
        StateHierarchyNode guarantee = hier.findbyName(guarantee_);
        String[] states = pre ? requires(a) : ensures(a);
        
        Set<StateHierarchyNode> states_;
        if( states.length == 0 ) {
            states_ = Collections.singleton(guarantee);
        }
        else {
            states_ = new HashSet<StateHierarchyNode>();
            for( String state : states ) {
                StateHierarchyNode ensures_ = hier.findbyName(state);
                states_.add(ensures_);
            }
        }
        
        // what's the usage?
        PermissionUse usage = usage(a);
        
        // Borrowing
        // This method is ONLY called for borrowing
        // annotations
        if( !fractionCache.containsKey(a) ) {
            fractionCache.put(a, new Fraction());
        }
        Fraction fract = fractionCache.get(a);
        
        return new ConcretePermissionElement(k, fract, usage, guarantee, states_);
    }

    // @ResultXXX -> Permission
    private ConcretePermissionElement ofResult(IAnnotationBinding a, PermissionKind kind,
            ITypeBinding type) {
        String[] states = ensures(a);
        StateHierarchy hier = StateHierarchyExtractor.lazilyExtractHierarchy(type, this.stateHierarchies);
        StateHierarchyNode guarantee = hier.findbyName(guarantee(a));

        Set<StateHierarchyNode> states_;
        if( states.length == 0 ) {
            states_ = Collections.singleton(guarantee);
        }
        else {
            states_ = new HashSet<StateHierarchyNode>();
            for( String ensures : states ) {
                StateHierarchyNode ensures_ = hier.findbyName(ensures);
                states_.add(ensures_);
            }
        }
        // Always a new fraction. No borrowing possible.
        Fraction fract = new Fraction();
        // Result is always virtual...
        PermissionUse usage = PermissionUse.Virtual;
        return new ConcretePermissionElement(kind, fract, usage, 
                guarantee, states_);
    }

    private static boolean returned(IAnnotationBinding a) {
        for( IMemberValuePairBinding pair : a.getAllMemberValuePairs() ) {
            if( "returned".equals(pair.getName()) ) {
                return (Boolean)pair.getValue();
            }
        }
        throw new IllegalArgumentException("No returned field.");
    }
    
    private static Object attribute(IAnnotationBinding a, String attr_name) {
        for( IMemberValuePairBinding pair : a.getAllMemberValuePairs() ) {
            if( attr_name.equals(pair.getName()) ) {
                return (String)pair.getValue();
            }
        }
        throw new IllegalArgumentException("No attribute named " + attr_name + ".");
    }
    
    private static String guarantee(IAnnotationBinding a) {
        for( IMemberValuePairBinding pair : a.getAllMemberValuePairs() ) {
            if( "guarantee".equals(pair.getName()) ) {
                return (String)pair.getValue();
            }
        }
        throw new IllegalArgumentException("No guarantee field.");
    }

    static String[] ensures(IAnnotationBinding anno) {
        for( IMemberValuePairBinding pair : anno.getAllMemberValuePairs() ) {
            if( "ensures".equals(pair.getName()) ) {
                Object value = pair.getValue();
                if( value instanceof String ) 
                    return new String[] { (String)value };
                else {
                    Object[] result = (Object[])value;
                    return Arrays.copyOf(result, result.length, String[].class); 
                }
            }
        }
        throw new IllegalArgumentException("No ensures field.");
    }
    
    static String[] requires(IAnnotationBinding anno) {
        for( IMemberValuePairBinding pair : anno.getAllMemberValuePairs() ) {
            if( "requires".equals(pair.getName()) ) {
                Object value = pair.getValue();
                if( value instanceof String ) 
                    return new String[] { (String)value };
                else {
                    Object[] result = (Object[])value;
                    return Arrays.copyOf(result, result.length, String[].class); 
                }
            }
        }
        return Utilities.impossible();
    }
    
    private PermissionUse usage(IAnnotationBinding a) {
        for( IMemberValuePairBinding pair : a.getAllMemberValuePairs() ) {
            if( "use".equals(pair.getName()) ) {
                Object value = pair.getValue();
                IVariableBinding enum_binding = (IVariableBinding) value;
                edu.cmu.cs.plural.perm.parser.PermissionUse pu =
                    edu.cmu.cs.plural.perm.parser.PermissionUse.valueOf(enum_binding.getName());
                return PermissionUse.fromPluralUse(pu);
            }
        }
        throw new IllegalArgumentException("No ensures field.");
    }
    
    static String valueToString(IAnnotationBinding anno) {
        for( IMemberValuePairBinding pair : anno.getAllMemberValuePairs() ) {
            if( "value".equals(pair.getName()) ) {
                return (String)pair.getValue();
            }
        }
        throw new IllegalArgumentException("No value field.");
    }

    
    /**
     * Finds the field permission as annotated.
     * Currently does nothing! It always returns an
     * unground permission, except that it updates the state hierarchies
     * with the field's type.
     */
    Permission extractField(IVariableBinding field) {
        // for the time-being, this method does basically nothing except for
        // ensuring that the type hierarchy for the field's type gets 
        // loaded.
        ITypeBinding field_type = field.getType();
        
        // DO THIS FOR EFFECT
        StateHierarchyExtractor.lazilyExtractHierarchy(field_type, stateHierarchies);
        
        return UnGroundPermission.INSTANCE;
    }
}
