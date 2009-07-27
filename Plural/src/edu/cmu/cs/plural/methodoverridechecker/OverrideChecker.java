/**
 * Copyright (C) 2007-2009 Carnegie Mellon University and others.
 *
 * This file is part of Plural.
 *
 * Plural is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 * published by the Free Software Foundation.
 *
 * Plural is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Plural; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking Plural statically or dynamically with other modules is
 * making a combined work based on Plural. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of Plural
 * give you permission to combine Plural with free software programs or
 * libraries that are released under the GNU LGPL and with code
 * included in the standard release of Eclipse under the Eclipse Public
 * License (or modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the
 * GNU GPL for Plural and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of Plural are not
 * obligated to grant this special exception for their modified
 * versions; it is their choice whether to do so. The GNU General
 * Public License gives permission to release a modified version
 * without this exception; this exception also makes it possible to
 * release a modified version which carries forward this exception.
 */

package edu.cmu.cs.plural.methodoverridechecker;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;

/**
 * In Plural, a method that overrides another method or implements a
 * interface method must have a specification that is compatible with
 * the specification of the method that it is overriding. This notion
 * of "compatibility" is basically behavioral subtyping. Say that a
 * method has pre- and post-conditions PRE_sub and POST_sub, and that
 * method overrides a method with specification PRE_super and POST_super.
 * Then for soundness we really should require that:<br>
 * <br>
 * 1.) PRE_super |- PRE_sub<br>
 * 2.) POST_sub  |- POST_super<br>
 * <br>
 * This is basically the same rule for subtyping on a method's type
 * signature. This check is necessary for soundness, and sometimes
 * my own sanity when performing case studies. However, we decided
 * to make this into a separate analysis for two reasons; first, I
 * believe it can nicely be made into an orthogonal analysis, but
 * second, this check may become very annoying for some case studies
 * and therefore people may want to turn it off.
 * 
 * @author Nels E. Beckman
 * @since Jul 27, 2009
 *
 */
public class OverrideChecker extends AbstractCrystalMethodAnalysis {

	@Override
	public void analyzeMethod(MethodDeclaration d) {
		// The process is pretty simple here. For every method we
		// encounter, find all methods that this one could be
		// overriding, then perform the appropriate pre/post
		// condition entailment checks.
		ITypeBinding declaringClass = d.resolveBinding().getDeclaringClass();
		ITypeBinding superclass = declaringClass.getSuperclass();
		ITypeBinding[] super_interfaces = declaringClass.getInterfaces();
		
		checkMethod(d, arrayAppend(super_interfaces, superclass));
	}

	/**
	 * Returns a possibly-new array that is the value of the original array with
	 * the extra interface appended to its end.
	 */
	private ITypeBinding[] arrayAppend(ITypeBinding[] super_interfaces,
			ITypeBinding superclass) {
		if( superclass == null )
			return super_interfaces;
		
		ITypeBinding[] result = new ITypeBinding[super_interfaces.length + 1];
		System.arraycopy(super_interfaces, 0, result, 0, super_interfaces.length);
		result[super_interfaces.length] = superclass;
		
		return result;
	}

	// Modifies set in place! Adds each element from the array.
	private void addArrayToSet(ITypeBinding[] types, Set<ITypeBinding> set) {
		for( ITypeBinding type : types ) {
			set.add(type);
		}
	}
	
	// Recursive method that kicks off the chceking, given super types.
	private void checkMethod(MethodDeclaration d, ITypeBinding[] super_types) {
		// We create just one set of super types, checking them at the end. This
		// is because if we reach the same interface through multiple paths
		// we really only want to report those errors one time.
		Set<ITypeBinding> next_super_types = new HashSet<ITypeBinding>();
		for( ITypeBinding super_type : super_types ) {
			if( super_type == null ) continue; // This corresponds to super of Object, and others.
			
			checkSingleSupertype(d, super_type);
			
			next_super_types.add(super_type.getSuperclass());
			addArrayToSet(super_type.getInterfaces(), next_super_types);
		}
	}

	private void checkSingleSupertype(MethodDeclaration d,
			ITypeBinding super_type) {
		throw new RuntimeException("NYI");
	}	
}
