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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.perm.ResultPermissionAnnotation;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.track.CrystalPermissionAnnotation;
import edu.cmu.cs.plural.util.Pair;

abstract class AbstractSingleCaseSignature extends AbstractBindingSignature 
		implements IInvocationCase {

	private static final Logger log = Logger.getLogger(AbstractSingleCaseSignature.class.getName());
	
	/**
	 * @param crystal
	 * @param binding The binding that contains specs to be used
	 * @param staticallyInvokedType The statically invoked type of this binding, which can 
	 * be different from <code>binding</code>'s declaring class if this is an inherited binding
	 */
	protected AbstractSingleCaseSignature(AnnotationDatabase annoDB,
			IMethodBinding binding, ITypeBinding staticallyInvokedType) {
		super(annoDB, binding, staticallyInvokedType);
	}

	//
	// IInvocationSignature methods
	// 
	
	@Override
	public Set<Set<String>> getEnsuredReceiverStateOptions() {
		return Collections.singleton(getEnsuredReceiverStates());
	}

	@Override
	public Set<Set<String>> getEnsuredParameterStateOptions(int paramIndex) {
		return Collections.singleton(getEnsuredParameterStates(paramIndex));
	}

	public Set<Set<String>> getRequiredReceiverStateOptions() {
		return Collections.singleton(getRequiredReceiverStates());
	}

	@Override
	public Set<Set<String>> getRequiredParameterStateOptions(int paramIndex) {
		return Collections.singleton(getRequiredParameterStates(paramIndex));
	}

	//
	// IInvocationCase methods
	// 
	
	@Override
	public Set<String> getEnsuredParameterStates(int paramIndex) {
		assert paramIndex >= 0 && paramIndex < binding.getParameterTypes().length;
		Set<String> result = new LinkedHashSet<String>();
		
		// required states from @Unique, @Full, etc. annotations
		for(ParameterPermissionAnnotation anno : 
			CrystalPermissionAnnotation.parameterAnnotations(getAnnoDB(), binding, paramIndex)) {
			if(anno.isReturned())
				for(String s : anno.getEnsures())
					result.add(s);
		}
		
		// required states from @Perm annotations
		Pair<Set<String>, Set<String>> prePost = PermParser.getParameterStateInfo(getMethodSummary(), paramIndex);
		result.addAll(prePost.snd());
		
		return result;
	}

	@Override
	public Set<String> getRequiredParameterStates(int paramIndex) {
		assert paramIndex >= 0 && paramIndex < binding.getParameterTypes().length;
		Set<String> result = new LinkedHashSet<String>();
		
		// required states from @Unique, @Full, etc. annotations
		for(ParameterPermissionAnnotation anno : 
			CrystalPermissionAnnotation.parameterAnnotations(getAnnoDB(), binding, paramIndex)) {
			for(String s : anno.getRequires())
				result.add(s);
		}
		
		// required states from @Perm annotations
		Pair<Set<String>, Set<String>> prePost = PermParser.getParameterStateInfo(getMethodSummary(), paramIndex);
		result.addAll(prePost.fst());

		return result;
	}
	
	public Set<String> getRequiredReceiverStates() {
		Set<String> result = new LinkedHashSet<String>();
		
		// required states from @Unique, @Full, etc. annotations
		for(ParameterPermissionAnnotation anno : 
			CrystalPermissionAnnotation.receiverAnnotations(getAnnoDB(), binding)) {
			for(String s : anno.getRequires())
				result.add(s);
		}
		
		// required states from @Perm annotations
		result.addAll(PermParser.getReceiverStateInfo(getMethodSummary()).fst());

		return result;
	}

	@Override
	public Set<String> getEnsuredReceiverStates() {
		Set<String> result = new LinkedHashSet<String>();
		
		// required states from @Unique, @Full, etc. annotations
		for(ParameterPermissionAnnotation anno : 
			CrystalPermissionAnnotation.receiverAnnotations(getAnnoDB(), binding)) {
			if(anno.isReturned())
				for(String s : anno.getEnsures())
					result.add(s);
		}
		
		// required states from @Perm annotations
		result.addAll(PermParser.getReceiverStateInfo(getMethodSummary()).snd());

		return result;
	}

	//
	// Methods for SimpleMethodSignature
	// (to group all annotation accesses into one class these methods are implemented here)
	//

	/**
	 * This method only makes sense for methods; <b>do not call
	 * this method for constructors</b>.  We put it
	 * here in order to group all annotation accesses together.
	 * @see edu.cmu.cs.plural.states.IMethodSignature#getEnsuredResultStateOptions()
	 */
	protected Set<Set<String>> getEnsuredResultStateOptions() {
		return Collections.singleton(getEnsuredResultStates());
	}

	/**
	 * This method only makes sense for methods; <b>do not call
	 * this method for constructors</b>.  We put it
	 * here in order to group all annotation accesses together.
	 * @see edu.cmu.cs.plural.states.IMethodCase#getEnsuredResultStates()
	 */
	protected Set<String> getEnsuredResultStates() {
		Set<String> result = new LinkedHashSet<String>();
		
		// required states from @Unique, @Full, etc. annotations
		for(ResultPermissionAnnotation anno : 
			CrystalPermissionAnnotation.resultAnnotations(getAnnoDB(), binding)) {
			for(String s : anno.getEnsures())
				result.add(s);
		}
		
		// required states from @Perm annotations
		result.addAll(PermParser.getResultStateInfo(getMethodSummary()));

		return result;
	}

	/**
	 * Returns the pre and post condition permissions for the receiver of the
	 * method call, given the method binding.
	 * @param frameAsVirtual 
	 */
	protected Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
	receiverPermissions(boolean namedFractions, boolean frameAsVirtual, boolean ignoreVirtual) {
		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		return receiverPermissions(namedFractions, preAndPostString, frameAsVirtual, ignoreVirtual);
	}
	
	/**
	 * Returns the pre and post condition permissions for the paramIndex-th 
	 * parameter of the binding method.
	 * @param paramIndex
	 * @param namedFractions
	 * @param requires 
	 * @param ensures 
	 */
	protected Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
	parameterPermissions(int paramIndex, boolean namedFractions) {
		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		return parameterPermissions(paramIndex, namedFractions, preAndPostString);
	}		

	protected PermissionSetFromAnnotations 
	resultPermissions(boolean namedFractions) {
		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		if(preAndPostString == null)
			return resultPermissions(namedFractions, "");
		return resultPermissions(namedFractions, preAndPostString.snd());
	}
}
