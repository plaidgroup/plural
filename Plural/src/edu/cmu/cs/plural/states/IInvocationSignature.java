/**
 * Copyright (C) 2007, 2008 Carnegie Mellon University and others.
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
package edu.cmu.cs.plural.states;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;


/**
 * This interface is not meant to be implemented directly.
 * Use one of the concrete sub-interfaces instead.
 * @author Kevin Bierhoff
 * @see IConstructorSignature
 * @see IMethodSignature
 */
public abstract interface IInvocationSignature {
	
	/**
	 * Returns the binding for the method whose specification is 
	 * represented in this signature.  Note that this is not necessarily
	 * the method for which a signature was requested since the signature
	 * could be inherited from an overridden method.
	 * @return the binding for the method whose specification is 
	 * represented in this signature.
	 */
	IMethodBinding getSpecifiedMethodBinding();
	
	List<? extends IInvocationCase> cases();
	
	/**
	 * Indicates whether this is a constructor or a method
	 * signature.
	 * @return <code>true</code> if this is a constructor
	 * signature, and <code>false</code> if this is a method
	 * signature.
	 * @see #getConstructorSignature()
	 * @see #getMethodSignature()
	 */
	boolean isConstructorSignature();
	
	/**
	 * Returns this object as a constructor signature, if
	 * it is one.
	 * @return
	 * @throws IllegalStateException If this is not a
	 * constructor signature
	 * @see #isConstructorSignature()
	 */
	IConstructorSignature getConstructorSignature();
	
	/**
	 * Returns this object as a method signature, if it
	 * is one.
	 * @return
	 * @throws IllegalStateException If this is not a
	 * method signature
	 * @see #isConstructorSignature()
	 */
	IMethodSignature getMethodSignature();
	
	/** 
	 * Returns the required states for the given parameter
	 * for each method case.
	 * @return
	 * @see IMethodCase#getRequiredParameterStates(int)
	 */
	Set<Set<String>> getRequiredParameterStateOptions(int paramIndex);

	/** 
	 * Returns the ensured states for the given parameter
	 * for each method case.
	 * @return
	 * @see IMethodCase#getEnsuredParameterStates(int)
	 */
	Set<Set<String>> getEnsuredParameterStateOptions(int paramIndex);

	/** 
	 * Returns the required states for the receiver
	 * for each method case.
	 * @return
	 * @see IMethodCase#getRequiredReceiverStates()
	 */
	Set<Set<String>> getRequiredReceiverStateOptions();
	
	/** 
	 * Returns the ensured states for the receiver
	 * for each method case.
	 * @return
	 * @see IMethodCase#getEnsuredReceiverStates()
	 */
	Set<Set<String>> getEnsuredReceiverStateOptions();
	
	/**
	 * Calls {@link IInvocationCase#createPermissions(boolean, boolean)} for each {@link #cases() case}.
	 * @param forAnalyzingBody
	 * @param isSuperCall ignored if <code>forAnalyzingBody</code> is <code>true</code>.
	 * @return
	 */
	List<? extends IInvocationCaseInstance> createPermissionsForCases(boolean forAnalyzingBody, boolean isSuperCall);
	
}
