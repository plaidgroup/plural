package edu.cmu.cs.anek.eclipse;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;


import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.util.Option;

/**
 * This class contains static helper methods specifically related
 * to Eclipse functionality.
 * <br>
 * अनेक<br>
 * Anek<br>
 * <br>
 * @author Nels E. Beckman
 *
 */
public final class EclipseUtils {

    /**
     * Given a selection known to be a structured selection, casts it and
     * returns its elements as IJavaElements. Will throw CastClassException
     * if any of the types are incorrect.
     */
    public static List<IJavaElement> structuredSelection(ISelection selection) {
    	IStructuredSelection structured = (IStructuredSelection)selection;
    	List<IJavaElement> elements = new LinkedList<IJavaElement>();
    	for( Object sel_ : structured.toArray() ) {
    		elements.add((IJavaElement)sel_);
    	}
    	return elements;
    }

    public static ITypeBinding canonicalType(ITypeBinding type) {
        // This will give us the proper generic type, even if the
        // original type is RAW or instantiated.
        while(type != type.getTypeDeclaration() )
            type = type.getTypeDeclaration();
        return type;
    }

    /**
     * Returns a fully-qualified method name FOR METHODS THAT MUST HAVE 
     * A FULLY-QUALIFIER METHOD NAME. Some types do not have qualified
     * names because they cannot be named (e.g., methods on anonymous or
     * local classes). This method throws an IllegalStateException in
     * that case.
     * @param method Method binding 
     * @return A fully-qualified name for method
     * @throws IllegalArgumentException If the type of the method does
     * not have a fully-qualified name.
     */
    public static Option<String> fullyQualifiedName(IMethodBinding method) throws IllegalArgumentException {
        ITypeBinding declaringClass = method.getDeclaringClass();
        String fq_type_name = fqTypeName(declaringClass);
        return  
            fq_type_name == null ?
                    Option.<String>none() :
                    Option.some(removeTypeParam(fq_type_name) + "." + method.getName());
    }

    /**
     * @see {@link #fullyQualifiedName(IMethodBinding)}.
     */
    public static Option<String> fullyQualifiedName(IVariableBinding field) {
        ITypeBinding declaring_class = field.getDeclaringClass();
        String fq_type_name = fqTypeName(declaring_class);
        return  
            fq_type_name == null ?
                    Option.<String>none() :
                    Option.some(removeTypeParam(fq_type_name) + "." + field.getName());
    }

    public static Option<String> fullyQualifiedName(ITypeBinding type) {
        String fq_type_name = fqTypeName(type);
        return  
            fq_type_name == null ?
                    Option.<String>none() :
                    Option.some(removeTypeParam(fq_type_name)); 
    }
    
    // Removes the trailing type parameter from a class name.
    private static String removeTypeParam(String class_name) {
        if( class_name.contains("<") ) {
            int remove_start = class_name.indexOf('<');
            return class_name.substring(0, remove_start);
        }
        else {
            return class_name;
        }   
    }
    
    private static String fqTypeName(ITypeBinding type) {
        type = canonicalType(type);
        // TODO Need to get rid of type variables
        String type_name = type.getQualifiedName();
        if( "".equals(type_name) ) {
            // only okay if this is a top-level class.
            if( type.getDeclaringClass() != null ) {
                return null;
            }
            // Will the 'else' branch _ever_ be taken?
        }
        return type.getQualifiedName();
    }

    public static String bestNameableType(ITypeBinding type) {
        {
            String result_ = fqTypeName(type);
            if(result_ != null) 
                return removeTypeParam(result_);
        }

        if( type.isCapture() || type.isWildcardType() || type.isTypeVariable() ) {
            return bestNameableType(type.getErasure());
        }
        
        // Find an interface or superclass being implemented. 
        // For anonymous class, there can only be one. BUT for
        // local class, things are super f-ed up...
        if( type.isAnonymous() ) {
            // any interfaces?
            if( type.getInterfaces().length > 0 ) {
                String result = fqTypeName(type.getInterfaces()[0]);
                assert(result != null);
                return removeTypeParam(result);
            }
            else {
                String result = fqTypeName(type.getSuperclass());
                assert(result != null);
                return removeTypeParam(result); 
            }
        }
        
        if( type.isLocal() ) {
            // We can only make sense of this is there is just one
            // superclass+inferface.
            if( type.getInterfaces().length == 0 ) {
                // EASY!
                String result = fqTypeName(type.getSuperclass());
                assert(result != null);
                return removeTypeParam(result); 
            }
            else {
                // Well, maybe superclass is object?
                if( type.getSuperclass().getQualifiedName().equals(Object.class.getName()) ) {
                    String result = fqTypeName(type.getInterfaces()[0]);
                    assert(result != null);
                    return removeTypeParam(result);
                }
                else {
                    // Otherwise, return the superclass, I guess, but print a warning:
                    String result = fqTypeName(type.getSuperclass());
                    assert(result != null);
                    System.err.println("Local class has multiple supertypes..." + type.getBinaryName());
                    return removeTypeParam(result);
                } 
            }
        }
        
        return Utilities.impossible();
    }

    /**
     * An interface for call-backs if you want code to visit every
     * type in a type hierarchy.
     * @author Nels E. Beckman
     *
     */
    public static abstract class HierarchyCallback {
        /**
         * Called when a new type is encountered. Return
         * value determines whether or not the super-types
         * will continue to be visited. True if yes, false
         * if no.
         */
        public abstract boolean nextType(ITypeBinding type);
    }
    /**
     * An interface for call-backs if you want code to visit every
     * overridden method in a type hierarchy.
     * @author Nels E. Beckman
     *
     */
    public static abstract class MethodHierarchyCallback {
        public abstract boolean nextMethod(IMethodBinding method);
    }
    
    /**
     * This method will visit every super-type of the given type
     * and call back to the given callback, stopping when the
     * callback returns false. The boolean <code>start_at_root</code>
     * determines whether or not the given type should be visited.
     * If false, the super-types will be visited but not the root.<br>
     * <br>
     * This method ascends types in a depth-first manner.
     */
    public static void visitTypeHierarchy(HierarchyCallback callback,
            ITypeBinding root, boolean start_at_root) {
        // ignoring return value.
        typeHierarchyHelper(callback, root, start_at_root);
    }
    
    private static boolean typeHierarchyHelper(HierarchyCallback callback,
            ITypeBinding root, boolean start_at_root) {
        // First, visit the type, then collect super-types
        if( start_at_root ) {
            boolean result = callback.nextType(root);
            if(!result)
                return result;
        }
        {// super-class
            ITypeBinding sup = root.getSuperclass();
            if( sup != null ) {
                boolean result = typeHierarchyHelper(callback, sup, true);
                if(!result)
                    return result;
            }
        }
        // interfaces
        for( ITypeBinding face : root.getInterfaces() ) {
            boolean result = typeHierarchyHelper(callback, face, true);
            if(!result)
                return result;
        }
        return true; // if you get this far, return value doesn't matter.
    }
    
    /**
     * This method will visit every super-method of the given method
     * and call back to the given callback, stopping when the
     * callback returns false. The boolean <code>start_at_root</code>
     * determines whether or not the given method should be visited.
     * If false, the super-methods will be visited but not the root.
     */
    public static void visitMethodHierarchy(final MethodHierarchyCallback callback,
            final IMethodBinding root, final boolean start_at_root) {
        // create a type callback that does what we want.
        HierarchyCallback tcallback = new HierarchyCallback() {
            @Override
            public boolean nextType(ITypeBinding type) {
                // each time, go through all of the methods
                for( IMethodBinding method_ : type.getDeclaredMethods() ) {
                    if( root.overrides(method_) || root.equals(method_) ) {
                        // we don't need to check start_at_root since it
                        // will be checked for us in the other visit method.
                        boolean result = callback.nextMethod(method_);
                        if( !result )
                            return false;
                    }
                }
                return true;
            }
        };
        visitTypeHierarchy(tcallback, 
                root.getDeclaringClass(), start_at_root/*, ast*/);
    }
    
}
