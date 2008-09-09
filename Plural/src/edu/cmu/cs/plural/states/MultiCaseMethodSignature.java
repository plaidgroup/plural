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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.PermAnnotation;
import edu.cmu.cs.plural.util.Pair;

/**
 * Signature for methods with multiple cases defined.
 * @author Kevin Bierhoff
 * @since 4/30/2008
 * @see MultiCaseConstructorSignature
 */
public class MultiCaseMethodSignature extends AbstractMultiCaseSignature<IMethodCase>
		implements IMethodSignature {

	/**
	 * @param crystal
	 * @param binding
	 */
	public MultiCaseMethodSignature(AnnotationDatabase annoDB, IMethodBinding binding,
			ITypeBinding staticallyInvokedType, PermAnnotation... cases) {
		super(annoDB, binding, staticallyInvokedType, cases);
	}

	@Override
	protected IMethodCase createCase(AnnotationDatabase annoDB, IMethodBinding binding,
			PermAnnotation perm, ITypeBinding staticallyInvokedType) {
		return new MultiMethodCase(annoDB, binding, staticallyInvokedType, perm);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IMethodCase#getEnsuredResultStates()
	 */
	@Override
	public Set<Set<String>> getEnsuredResultStateOptions() {
		Set<Set<String>> result = new LinkedHashSet<Set<String>>(cases().size());
		for(IMethodCase c : cases()) {
			result.add(c.getEnsuredResultStates());
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IMethodCase#getRequiredReceiverStates()
	 */
	@Override
	public Set<Set<String>> getRequiredReceiverStateOptions() {
		Set<Set<String>> result = new LinkedHashSet<Set<String>>(cases().size());
		for(IMethodCase c : cases()) {
			result.add(c.getRequiredReceiverStates());
		}
		return result;
	}

	@Override
	public List<IMethodCaseInstance> createPermissionsForCases(
			boolean forAnalyzingBody, boolean isSuperCall) {
		List<IMethodCaseInstance> result = new ArrayList<IMethodCaseInstance>(cases().size());
		for(IMethodCase c : cases()) {
			result.add(c.createPermissions(forAnalyzingBody, false));
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#isConstructorSignature()
	 */
	@Override
	public boolean isConstructorSignature() {
		return false;
	}

	@Override
	public IMethodSignature getMethodSignature() {
		return this;
	}

	@Override
	public String getName() {
		return binding.getName();
	}

	@Override
	public boolean hasReceiver() {
		return (binding.getModifiers() & Modifier.STATIC) == 0;
	}

	private class MultiMethodCase extends AbstractBindingCase implements
	IMethodCase {

		public MultiMethodCase(AnnotationDatabase annoDB, IMethodBinding binding, ITypeBinding staticallyInvokedType, PermAnnotation perm) {
			super(annoDB, binding, staticallyInvokedType, perm);
		}

		/* Change visibility of inherited method
		 * @see edu.cmu.cs.plural.states.AbstractBindingCase#getEnsuredResultStates()
		 */
		@Override
		public Set<String> getEnsuredResultStates() {
			return super.getEnsuredResultStates();
		}
		
		@Override
		public IMethodCaseInstance createPermissions(boolean forAnalyzingBody, boolean isSuperCall) {
			final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> receiverPrePost;
 			if(hasReceiver()) {
 				// coerce == true iff dynamically dispatched call site
 				// TODO could consider not coercing for final methods / final classes?
				boolean coerce = !forAnalyzingBody && !isSuperCall && (binding.getModifiers() & Modifier.PRIVATE) == 0;
				receiverPrePost = receiverPermissions(forAnalyzingBody, preAndPostString, coerce, false);
 			}
			else {
				receiverPrePost = null;
			}
			final int argCount = binding.getParameterTypes().length;
			final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost = 
				new Pair[argCount];
			for(int arg = 0; arg < argCount; arg++) {
				argPrePost[arg] = parameterPermissions(arg, forAnalyzingBody, preAndPostString);
			}
			final PermissionSetFromAnnotations resultPost = 
				resultPermissions(forAnalyzingBody, preAndPostString.snd());
			return new IMethodCaseInstance() {

				@Override
				public Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> getReceiverPermissions() {
					return receiverPrePost;
				}
				
				@Override
				public Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] getParameterPermissions() {
					return argPrePost;
				}
				
				@Override
				public PermissionSetFromAnnotations getEnsuredResultPermissions() {
					return resultPost;
				}

				@Override
				public PermissionSetFromAnnotations getRequiredReceiverPermissions() {
					return receiverPrePost.fst();
				}

				@Override
				public PermissionSetFromAnnotations getEnsuredParameterPermissions(
						int paramIndex) {
					return argPrePost[paramIndex].snd();
				}

				@Override
				public PermissionSetFromAnnotations getEnsuredReceiverPermissions() {
					return receiverPrePost.snd();
				}

				@Override
				public PermissionSetFromAnnotations getRequiredParameterPermissions(
						int paramIndex) {
					return argPrePost[paramIndex].fst();
				}

				@Override
				public IMethodCaseInstance getMethodCaseInstance() {
					return this;
				}

				@Override
				public boolean isConstructorCaseInstance() {
					return false;
				}

				@Override
				public IMethodCase getInvocationCase() {
					return MultiMethodCase.this;
				}

				@Override
				public boolean[] areArgumentsBorrowed() {
					// TODO Auto-generated method stub
					return new boolean[argPrePost.length];
				}

				@Override
				public boolean isReceiverBorrowed() {
					// TODO Auto-generated method stub
					return false;
				}

			};
		}
		
	}
}
