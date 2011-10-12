package edu.cmu.cs.anek.graph;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Class with static utility methods for dealing with
 * names of various sorts.
 * 
 * @author Nels E. Beckman
 *
 */
public final class Names {
    
    /**
     * Given a type this method will return an unambiguous name.
     * Generally this is just the fully qualified name, but in the
     * even that this is a local or anonymous class, the name
     * will be unique and distinguish it from other types. We
     * expect that this method will only be used for providing a
     * unique prefix for things like fields, therefore if the type
     * cannot have fields (primitives, arrays) and exception will
     * be thrown.
     */
    public static String unambiguousTypeName(ITypeBinding type) {
        if( type.isArray() || type.isPrimitive() )
            throw new IllegalArgumentException("Only object types are allowed");
        // I thought this was going to be a lot more complicated, but
        // only getKey seems to give me the ability to tell anonymous
        // types apart, so I am going with it!
        return type.getKey();
    }
}
