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

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.PermAnnotation;
import edu.cmu.cs.plural.polymorphic.instantiation.RcvrInstantiationPackage;
import edu.cmu.cs.plural.pred.MethodPostcondition;
import edu.cmu.cs.plural.pred.MethodPrecondition;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.pred.PredicateMerger;

/**
 * Signature for methods with multiple cases defined.
 * @author Kevin Bierhoff
 * @since 4/30/2008
 * @see MultiCaseConstructorSignature
 */
class MultiCaseMethodSignature extends AbstractMultiCaseSignature<IMethodCase>
		implements IMethodSignature {

	/**
	 * @param annoDB
	 * @param specBinding
	 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
	 * which can be different from <code>specBinding</code> if specifications are inherited.
	 */
	public MultiCaseMethodSignature(AnnotationDatabase annoDB, IMethodBinding specBinding,
			IMethodBinding staticallyInvokedBinding, PermAnnotation... cases) {
		super(annoDB, specBinding, staticallyInvokedBinding, cases);
	}

	@Override
	protected IMethodCase createCase(AnnotationDatabase annoDB, IMethodBinding binding,
			PermAnnotation perm, IMethodBinding staticallyInvokedBinding) {
		if(perm == null)
			return new MultiMethodCase(annoDB, binding, staticallyInvokedBinding);
		return new MultiMethodCase(annoDB, binding, staticallyInvokedBinding, perm);
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

	@Override
	public List<IMethodCaseInstance> createPermissionsForCases(
			MethodCheckingKind checkingKind,
			boolean forAnalyzingBody, boolean isSuperCall,
			Option<RcvrInstantiationPackage> ip) {
		List<IMethodCaseInstance> result = new ArrayList<IMethodCaseInstance>(cases().size());
		for(IMethodCase c : cases()) {
			result.add(c.createPermissions(checkingKind, forAnalyzingBody, isSuperCall, ip));
		}
		return result;
	}

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
		return (!Modifier.isStatic(binding.getModifiers()));
	}

	private class MultiMethodCase extends AbstractBindingCase implements
	IMethodCase {

		public MultiMethodCase(AnnotationDatabase annoDB,
				IMethodBinding specBinding, IMethodBinding staticallyInvokedBinding) {
			super(annoDB, specBinding, staticallyInvokedBinding);
		}

		public MultiMethodCase(AnnotationDatabase annoDB, IMethodBinding specBinding, 
				IMethodBinding staticallyInvokedBinding, PermAnnotation perm) {
			super(annoDB, specBinding, staticallyInvokedBinding, perm);
		}

		/* Change visibility of inherited method
		 * @see edu.cmu.cs.plural.states.AbstractBindingCase#getEnsuredResultStates()
		 */
		@Override
		public Set<String> getEnsuredResultStates() {
			return super.getEnsuredResultStates();
		}
		
		@Override
		public boolean isVirtualFrameSpecial() {
			return MultiCaseMethodSignature.this.requiresVirtualFrameCheck(
					preAndPostString == null ? null : preAndPostString.fst());
		}
		
		@Override
		public IMethodCaseInstance createPermissions(
				MethodCheckingKind checkingKind,
				final boolean forAnalyzingBody, boolean isSuperCall,
				Option<RcvrInstantiationPackage> ip) {
			final Pair<MethodPrecondition,MethodPostcondition> preAndPost;
			if(forAnalyzingBody) {
				preAndPost = preAndPost(forAnalyzingBody, preAndPostString, 
						checkingKind,
						false,
						false,
						isSuperCall,
						ip);
			}
			else {
				boolean coerce;
				if(hasReceiver()) {
					// coerce == true iff dynamically dispatched call site
					coerce = !isSuperCall && !Modifier.isPrivate(binding.getModifiers());
				}
				else
					coerce = false;
				preAndPost = preAndPost(forAnalyzingBody, preAndPostString, 
						checkingKind,
						coerce, 
						false,
						false,
						ip);
			}
			
			
			return new IMethodCaseInstance() {
				@Override public String toString() { return MultiMethodCase.this.toString(); }
				
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
					return MultiMethodCase.this.isEffectFree();
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
				public Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> getReceiverPermissions() {
					throw new UnsupportedOperationException("Deprecated");
//					return receiverPrePost;
				}
				
				@Override
				public Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] getParameterPermissions() {
					throw new UnsupportedOperationException("Deprecated");
//					return argPrePost;
				}
				
				@Override
				public PermissionSetFromAnnotations getEnsuredResultPermissions() {
					throw new UnsupportedOperationException("Deprecated");
//					return resultPost;
				}

				@Override
				public PermissionSetFromAnnotations getRequiredReceiverPermissions() {
					throw new UnsupportedOperationException("Deprecated");
//					return receiverPrePost.fst();
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
}
