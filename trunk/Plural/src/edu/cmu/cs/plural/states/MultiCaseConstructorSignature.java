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
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.PermAnnotation;
import edu.cmu.cs.plural.pred.MethodPostcondition;
import edu.cmu.cs.plural.pred.MethodPrecondition;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.pred.PredicateMerger;

/**
 * Signature for constructors with multiple cases defined.
 * @author Kevin Bierhoff
 * @since 4/30/2008
 * @see MultiCaseMethodSignature
 */
class MultiCaseConstructorSignature extends AbstractMultiCaseSignature<IConstructorCase> 
		implements IConstructorSignature {

	/**
	 * @param annoDB
	 * @param specBinding The binding that contains specs to be used
	 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
	 * which can be different from <code>specBinding</code> if specifications are inherited.
	 * @param permAnnotation
	 */
	public MultiCaseConstructorSignature(AnnotationDatabase annoDB, IMethodBinding specBinding, 
			IMethodBinding staticallyInvokedBinding, PermAnnotation... permAnnotation) {
		super(annoDB, specBinding, staticallyInvokedBinding, permAnnotation);
	}

	@Override
	protected IConstructorCase createCase(AnnotationDatabase annoDB,
			IMethodBinding binding, PermAnnotation perm, IMethodBinding staticallyInvokedBinding) {
		if(perm == null)
			return new MultiConstructorCase(annoDB, binding, staticallyInvokedBinding);
		return new MultiConstructorCase(annoDB, binding, staticallyInvokedBinding, perm);
	}

	@Override
	public List<IConstructorCaseInstance> createPermissionsForCases(
			boolean forAnalyzingBody, boolean isConstructorCall) {
		List<IConstructorCaseInstance> result = new ArrayList<IConstructorCaseInstance>(cases().size());
		for(IConstructorCase c : cases()) {
			result.add(c.createPermissions(forAnalyzingBody, isConstructorCall));
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
		 * Creates a case without an @Perm annotation.
		 * @param crystal
		 * @param specBinding The binding that contains specs to be used
		 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
		 * which can be different from <code>specBinding</code> if specifications are inherited.
		 */
		public MultiConstructorCase(AnnotationDatabase annoDB,
				IMethodBinding specBinding, IMethodBinding staticallyInvokedBinding) {
			super(annoDB, specBinding, staticallyInvokedBinding);
		}

		/**
		 * @param crystal
		 * @param specBinding The binding that contains specs to be used
		 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
		 * which can be different from <code>specBinding</code> if specifications are inherited.
		 * @param perm
		 */
		public MultiConstructorCase(AnnotationDatabase annoDB, IMethodBinding specBinding, 
				IMethodBinding staticallyInvokedBinding, PermAnnotation perm) {
			super(annoDB, specBinding, staticallyInvokedBinding, perm);
		}
		
		@Override
		public boolean isVirtualFrameSpecial() {
			return MultiCaseConstructorSignature.this.requiresVirtualFrameCheck(
					preAndPostString == null ? null : preAndPostString.fst());
		}
		
		/**
		 *
		 * @tag todo.general -id="1233909" : take out superseded code (also in method signatures)
		 *
		 */
		@Override
		public IConstructorCaseInstance createPermissions(final boolean forAnalyzingBody, boolean isConstructorCall) {
			final Pair<MethodPrecondition, MethodPostcondition> preAndPost;
			if(forAnalyzingBody) {
				preAndPost = preAndPost(
						forAnalyzingBody, preAndPostString, 
						false, 
						false, 
						isConstructorCall);
			}
			else {
				preAndPost = preAndPost(
						forAnalyzingBody, preAndPostString, !isConstructorCall, 
						!isConstructorCall, !isConstructorCall);
			}
			
			return new IConstructorCaseInstance() {

				@Override
				public PredicateMerger getPostconditionMerger() {
					assert ! forAnalyzingBody : "Did not request case instance for checking call site";
					return preAndPost.snd();
				}

				@Override
				public PredicateChecker getPreconditionChecker() {
					assert ! forAnalyzingBody : "Did not request case instance for checking call site";
					return preAndPost.fst();
				}

				@Override
				public boolean isEffectFree() {
					return MultiConstructorCase.this.isEffectFree();
//					return preAndPost.fst().isReadOnly();
				}

				@Override
				public PredicateChecker getPostconditionChecker() {
					assert forAnalyzingBody : "Did not request case instance for analyzing body";
					return preAndPost.snd();
				}

				@Override
				public PredicateMerger getPreconditionMerger() {
					assert forAnalyzingBody : "Did not request case instance for analyzing body";
					return preAndPost.fst();
				}

				@Override
				public boolean isConstructorCaseInstance() {
					return true;
				}

				@Override
				public IMethodCaseInstance getMethodCaseInstance() {
					throw new IllegalStateException("This is not a method signature: " + this);
				}

				@Override
				public IConstructorCase getInvocationCase() {
					return MultiConstructorCase.this;
				}

				@Override
				public Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] getParameterPermissions() {
					throw new UnsupportedOperationException("Deprecated");
//					return argPrePost;
				}
				
				@Override
				public Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> getReceiverPermissions() {
					throw new UnsupportedOperationException("Deprecated");
//					return receiverPrePost;
				}
				
				@Override
				public PermissionSetFromAnnotations getEnsuredParameterPermissions(
						int paramIndex) {
					throw new UnsupportedOperationException("Deprecated");
//					return argPrePost[paramIndex].snd();
				}

				@Override
				public PermissionSetFromAnnotations getEnsuredReceiverPermissions() {
					throw new UnsupportedOperationException("Deprecated");
//					return receiverPrePost.snd();
				}

				@Override
				public PermissionSetFromAnnotations getRequiredParameterPermissions(
						int paramIndex) {
					throw new UnsupportedOperationException("Deprecated");
//					return argPrePost[paramIndex].fst();
				}

				@Override
				public PermissionSetFromAnnotations getRequiredReceiverPermissions() {
					throw new UnsupportedOperationException("Deprecated");
//					return receiverPrePost.fst();
				}

				@Override
				public boolean[] areArgumentsBorrowed() {
					throw new UnsupportedOperationException("Implement this.");
				}

				@Override
				public boolean isReceiverBorrowed() {
					throw new UnsupportedOperationException("Implement this.");
				}

			};
		}
	
	}
}
