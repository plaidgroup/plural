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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.pred.MethodPostcondition;
import edu.cmu.cs.plural.pred.MethodPrecondition;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.util.Pair;

/**
 * This class implements single-case method signatures by double-functioning
 * as the single method case.
 * @author Kevin Bierhoff
 * @deprecated Use MultiCaseMethodSignature instead
 */
@Deprecated
class SimpleMethodSignature extends AbstractSingleCaseSignature implements
		IMethodSignature, IMethodCase {

	private final List<IMethodCase> cases;

	protected SimpleMethodSignature(AnnotationDatabase annoDB, IMethodBinding binding,
			ITypeBinding staticallyInvokedType) {
		super(annoDB, binding, staticallyInvokedType);
		assert binding.isConstructor() == false;
		cases = Collections.<IMethodCase>singletonList(this);
	}
	
	//
	// IMethodSignature methods
	//

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IMethodSignature#getName()
	 */
	@Override
	public String getName() {
		return binding.getName();
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#isConstructorSignature()
	 */
	@Override
	public boolean isConstructorSignature() {
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.AbstractBindingSignature#getMethodSignature()
	 */
	@Override
	public IMethodSignature getMethodSignature() {
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IMethodSignature#hasReceiver()
	 */
	@Override
	public boolean hasReceiver() {
		return (binding.getModifiers() & Modifier.STATIC) == 0;
	}

	@Override
	public List<IMethodCase> cases() {
		return cases;
	}

	//
	// IMethodSignature methods
	//

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IMethodSignature#getEnsuredReceiverStateOptions()
	 */
	@Override
	public Set<Set<String>> getEnsuredReceiverStateOptions() {
		assert hasReceiver();
		return super.getEnsuredReceiverStateOptions();
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IMethodSignature#getRequiredReceiverStateOptions()
	 */
	@Override
	public Set<Set<String>> getRequiredReceiverStateOptions() {
		assert hasReceiver();
		return super.getRequiredReceiverStateOptions();
	}

	@Override
	public Set<Set<String>> getEnsuredResultStateOptions() {
		// change visibility
		return super.getEnsuredResultStateOptions();
	}

	//
	// IMethodCase methods
	//

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IMethodSignature#getEnsuredReceiverStateOptions()
	 */
	@Override
	public Set<String> getEnsuredReceiverStates() {
		assert hasReceiver();
		return super.getEnsuredReceiverStates();
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IMethodSignature#getRequiredReceiverStateOptions()
	 */
	@Override
	public Set<String> getRequiredReceiverStates() {
		assert hasReceiver();
		return super.getRequiredReceiverStates();
	}

	@Override
	public Set<String> getEnsuredResultStates() {
		// change visibility
		return super.getEnsuredResultStates();
	}

	@Override
	public List<IMethodCaseInstance> createPermissionsForCases(
			boolean forAnalyzingBody, boolean isSuperCall) {
		return Collections.singletonList(createPermissions(forAnalyzingBody, isSuperCall));
	}

	@Override
	public IMethodCaseInstance createPermissions(final boolean forAnalyzingBody, boolean isSuperCall) {
		boolean coerce;
		if(hasReceiver()) {
			// coerce == true iff dynamically dispatched call site
			// TODO could consider not coercing for final methods / final classes?
			coerce = !forAnalyzingBody && !isSuperCall && (binding.getModifiers() & Modifier.PRIVATE) == 0;
		}
		else
			coerce = false;
		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		final Pair<MethodPrecondition, MethodPostcondition> preAndPost = preAndPost(
				forAnalyzingBody, preAndPostString, coerce, false);
		
		final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> receiverPrePost;

		if(hasReceiver()) {
			receiverPrePost = receiverPermissions(forAnalyzingBody, coerce, false);
		}
		else
			receiverPrePost = null;
		final int argCount = binding.getParameterTypes().length;
		final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost = 
			new Pair[argCount];
		for(int arg = 0; arg < argCount; arg++) {
			argPrePost[arg] = parameterPermissions(arg, forAnalyzingBody);
		}
		final PermissionSetFromAnnotations resultPost = 
			resultPermissions(!forAnalyzingBody);
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
				return SimpleMethodSignature.this;
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
				return preAndPost.fst().isReadOnly();
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

		};
	}

}
