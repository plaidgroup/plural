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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.plural.perm.parser.PermAnnotation;

/**
 * @author Kevin Bierhoff
 * @since 4/30/2008
 */
abstract class AbstractMultiCaseSignature<T extends IInvocationCase> extends AbstractBindingSignature {

	private final List<T> cases;

	/**
	 * @param crystal
	 * @param binding The binding that contains specs to be used
	 * @param staticallyInvokedType The statically invoked type of this binding, which can 
	 * be different from <code>binding</code>'s declaring class if this is an inherited binding
	 * @param cases
	 */
	protected AbstractMultiCaseSignature(AnnotationDatabase annoDB, IMethodBinding binding,
			ITypeBinding staticallyInvokedType, PermAnnotation... cases) {
		super(annoDB, binding, staticallyInvokedType);
		if(cases.length == 0)
			throw new IllegalArgumentException("Must have at least one method case--use SimpleConstructorSignature for methods without specs.");
		this.cases = new ArrayList<T>(cases.length);
		for(PermAnnotation perm : cases) {
			this.cases.add(createCase(annoDB, binding, perm, staticallyInvokedType));
		}
	}

	abstract protected T createCase(AnnotationDatabase annoDB, IMethodBinding binding,
			PermAnnotation perm, ITypeBinding staticallyInvokedType);

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#cases()
	 */
	@Override
	public List<T> cases() {
		return cases;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#getEnsuredParameterStateOptions(int)
	 */
	@Override
	public Set<Set<String>> getEnsuredParameterStateOptions(int paramIndex) {
		Set<Set<String>> result = new LinkedHashSet<Set<String>>(cases().size());
		for(T c : cases) {
			result.add(c.getEnsuredParameterStates(paramIndex));
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#getEnsuredReceiverStateOptions()
	 */
	@Override
	public Set<Set<String>> getEnsuredReceiverStateOptions() {
		Set<Set<String>> result = new LinkedHashSet<Set<String>>(cases().size());
		for(T c : cases) {
			result.add(c.getEnsuredReceiverStates());
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#getRequiredParameterStateOptions(int)
	 */
	@Override
	public Set<Set<String>> getRequiredParameterStateOptions(int paramIndex) {
		Set<Set<String>> result = new LinkedHashSet<Set<String>>(cases().size());
		for(T c : cases) {
			result.add(c.getRequiredParameterStates(paramIndex));
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#getEnsuredReceiverStateOptions()
	 */
	@Override
	public Set<Set<String>> getRequiredReceiverStateOptions() {
		Set<Set<String>> result = new LinkedHashSet<Set<String>>(cases().size());
		for(T c : cases) {
			result.add(c.getRequiredReceiverStates());
		}
		return result;
	}

}
