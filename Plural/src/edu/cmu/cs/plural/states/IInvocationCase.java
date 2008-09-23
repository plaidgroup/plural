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

import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;

/**
 * @author Kevin Bierhoff
 * @since 4/28/2008
 * @see IInvocationSignature#cases()
 */
public interface IInvocationCase {

	/**
	 * Returns the binding for the method whose specification is 
	 * represented in this method case.  Note that this is not necessarily
	 * the method for which a signature was requested since the signature
	 * could be inherited from an overridden method.
	 * @return the binding for the method whose specification is 
	 * represented in this method case.
	 */
	IMethodBinding getSpecifiedMethodBinding();
	
	Set<String> getRequiredParameterStates(int paramIndex);

	Set<String> getEnsuredParameterStates(int paramIndex);

	Set<String> getRequiredReceiverStates();

	Set<String> getEnsuredReceiverStates();
	
	/**
	 * @param forAnalyzingBody
	 * @param isSuperCall Ignore if <code>forAnalyzingBody</code> is <code>true</code>.
	 * @return
	 * @see IInvocationSignature#createPermissionsForCases(boolean, boolean)
	 */
	IInvocationCaseInstance createPermissions(boolean forAnalyzingBody, boolean isSuperCall);
	
	boolean isReentrant();

}
