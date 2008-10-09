package edu.cmu.cs.plural.states;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;

public class EffectDeclarations {

	/**
	 * Determines whether the given method is declared to have no effects.
	 * @param methodBinding
	 * @param annoDB Annotation database to look up annotations for methods.
	 * @return <code>true</code> if the given method should have no effects,
	 * <code>false</code> otherwise.
	 */
	public static boolean isPure(IMethodBinding methodBinding, AnnotationDatabase annoDB) {
		if(annoDB.getSummaryForMethod(methodBinding).
				getReturn("edu.cmu.cs.plural.annot.NoEffects") != null)
			return true;
		
		if(methodBinding.getDeclaringClass().getSuperclass() == null)
			// method in java.lang.Object
			return false;
		
		if(methodBinding.isDefaultConstructor()) {
			// for compiler-generated default constructors, find the invoked super-constructor
			// this does not work for constructors found in class files b/c those are not
			// known to be compiler-generated according to isDefaultConstructor javadoc
			for(IMethodBinding m : methodBinding.getDeclaringClass().getSuperclass().getDeclaredMethods()) {
				if(m.isConstructor() && m.getParameterTypes().length == 0)
					return isPure(m, annoDB);
			}
		}
		
		if(methodBinding.isConstructor() || Modifier.isStatic(methodBinding.getModifiers()))
			// constructors and static methods cannot inherit spec's
			return false;

		// walk super-types to see if the method is declared to be pure somewhere
		Set<ITypeBinding> seen = new HashSet<ITypeBinding>();
		seen.add(methodBinding.getDeclaringClass());
		if(isPure(methodBinding.getDeclaringClass().getSuperclass(), methodBinding, seen, annoDB))
			return true;
		for(ITypeBinding itf : methodBinding.getDeclaringClass().getInterfaces()) {
			if(isPure(itf, methodBinding, seen, annoDB))
				return true;
		}
		return false;
	}
	
	/**
	 * Helper method for walking supertypes looking for @NoEffects annotations.
	 * @param type
	 * @param methodBinding
	 * @param seen Set of types that were already visited and need not be checked again.
	 * @return <code>true</code> if a method in the given type that the given 
	 * method overrides is declared to have no effects, <code>false</code> if
	 * the given type was <code>null</code>, contained in seen, or did not contain
	 * any methods declared to have no effects that the given one overrides.
	 */
	private static boolean isPure(ITypeBinding type, IMethodBinding methodBinding,
			Set<ITypeBinding> seen, AnnotationDatabase annoDB) {
		if(type == null || seen.contains(type))
			return false;
		seen.add(type);
		for(IMethodBinding m : type.getDeclaredMethods()) {
			if(methodBinding.isSubsignature(m)) {
				if(annoDB.getSummaryForMethod(m).
						getReturn("edu.cmu.cs.plural.annot.NoEffects") != null)
					return true;
			}
		}
		if(isPure(type.getSuperclass(), methodBinding, seen, annoDB))
			return true;
		for(ITypeBinding itf : type.getInterfaces()) {
			if(isPure(itf, methodBinding, seen, annoDB))
				return true;
		}
		return false;
	}

}
