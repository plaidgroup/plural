/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.PermAnnotation;
import edu.cmu.cs.plural.util.Pair;

/**
 * Signature for constructors with multiple cases defined.
 * @author Kevin Bierhoff
 * @since 4/30/2008
 * @see MultiCaseMethodSignature
 */
class MultiCaseConstructorSignature extends AbstractMultiCaseSignature<IConstructorCase> 
		implements IConstructorSignature {

	/**
	 * @param crystal
	 * @param binding The binding that contains specs to be used
	 * @param staticallyInvokedType The statically invoked type of this binding, which can 
	 * be different from <code>binding</code>'s declaring class if this is an inherited binding
	 * @param cases
	 */
	public MultiCaseConstructorSignature(AnnotationDatabase annoDB, IMethodBinding binding, 
			ITypeBinding staticallyInvokedType, PermAnnotation... cases) {
		super(annoDB, binding, staticallyInvokedType, cases);
	}

	@Override
	protected IConstructorCase createCase(AnnotationDatabase annoDB,
			IMethodBinding binding, PermAnnotation perm, ITypeBinding staticallyInvokedType) {
		return new MultiConstructorCase(annoDB, binding, staticallyInvokedType, perm);
	}

	@Override
	public List<IConstructorCaseInstance> createPermissionsForCases(
			boolean forAnalyzingBody, boolean isSuperCall) {
		List<IConstructorCaseInstance> result = new ArrayList<IConstructorCaseInstance>(cases().size());
		for(IConstructorCase c : cases()) {
			result.add(c.createPermissions(forAnalyzingBody, isSuperCall));
		}
		return result;
	}

	@Override
	public boolean isConstructorSignature() {
		return true;
	}

	@Override
	public IConstructorSignature getConstructorSignature() {
		return this;
	}

	@Override
	public String getConstructedClassName() {
		return binding.getDeclaringClass().getName();
	}

	private class MultiConstructorCase extends AbstractBindingCase implements IConstructorCase {

		/**
		 * @param crystal
		 * @param binding The binding that contains specs to be used
		 * @param staticallyInvokedType The statically invoked type of this binding, which can 
		 * be different from <code>binding</code>'s declaring class if this is an inherited binding
		 * @param perm
		 */

		public MultiConstructorCase(AnnotationDatabase annoDB, IMethodBinding binding, 
				ITypeBinding staticallyInvokedType, PermAnnotation perm) {
			super(annoDB, binding, staticallyInvokedType, perm);
		}
		
		@Override
		public IConstructorCaseInstance createPermissions(boolean forAnalyzingBody, boolean isSuperCall) {
			final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> receiverPrePost =
				receiverPermissions(forAnalyzingBody, preAndPostString, !forAnalyzingBody && !isSuperCall, !forAnalyzingBody && !isSuperCall);
			final int argCount = binding.getParameterTypes().length;
			final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost = 
				new Pair[argCount];
			for(int arg = 0; arg < argCount; arg++) {
				argPrePost[arg] = parameterPermissions(arg, forAnalyzingBody, preAndPostString);
			}
			return new IConstructorCaseInstance() {

				@Override
				public Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] getParameterPermissions() {
					return argPrePost;
				}
				
				@Override
				public Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> getReceiverPermissions() {
					return receiverPrePost;
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
				public PermissionSetFromAnnotations getRequiredReceiverPermissions() {
					return receiverPrePost.fst();
				}

				@Override
				public IMethodCaseInstance getMethodCaseInstance() {
					throw new IllegalStateException("This is not a method signature: " + this);
				}

				@Override
				public boolean isConstructorCaseInstance() {
					return true;
				}

				@Override
				public IConstructorCase getInvocationCase() {
					return MultiConstructorCase.this;
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
