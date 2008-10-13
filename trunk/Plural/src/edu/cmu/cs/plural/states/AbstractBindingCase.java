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

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.perm.ResultPermissionAnnotation;
import edu.cmu.cs.plural.perm.parser.PermAnnotation;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.track.CrystalPermissionAnnotation;

/**
 * @author Kevin Bierhoff
 * @since 4/28/2008
 */
public abstract class AbstractBindingCase extends AbstractBinding implements IInvocationCase {

	protected Pair<String, String> preAndPostString;
	
	/**
	 * Creates a case without an @Perm annotation.
	 * @param crystal
	 * @param binding The binding that contains specs to be used
	 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
	 * which can be different from <code>specBinding</code> if specifications are inherited.
	 */
	protected AbstractBindingCase(AnnotationDatabase annoDB, IMethodBinding binding, 
			IMethodBinding staticallyInvokedBinding) {
		super(annoDB, binding, staticallyInvokedBinding);
		preAndPostString = Pair.create("", "");
	}

	/**
	 * Creates a case for the given @Perm annotation
	 * @param crystal
	 * @param specBinding The binding that contains specs to be used
	 * @param perm @Perm annotation
	 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
	 * which can be different from <code>specBinding</code> if specifications are inherited.
	 */
	protected AbstractBindingCase(AnnotationDatabase annoDB, IMethodBinding specBinding, 
			IMethodBinding staticallyInvokedBinding, PermAnnotation perm) {
		super(annoDB, specBinding, staticallyInvokedBinding);
		preAndPostString = Pair.create(perm.getRequires(), perm.getEnsures());
	}

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
		Pair<Set<String>, Set<String>> prePost = PermParser.getParameterStateInfo(
				preAndPostString.fst(), preAndPostString.snd(), paramIndex);
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
		Pair<Set<String>, Set<String>> prePost = PermParser.getParameterStateInfo(
				preAndPostString.fst(), preAndPostString.snd(), paramIndex);
		result.addAll(prePost.fst());
		return result;
	}
	
	/**
	 * This method only makes sense for methods; <b>do not call
	 * this method for constructors</b>.  We put it
	 * here in order to group all annotation accesses together.
	 * @see edu.cmu.cs.plural.states.IMethodSignature#getEnsuredResultStates()
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
		result.addAll(PermParser.getResultStateInfo(preAndPostString.snd()));
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
		result.addAll(PermParser.getReceiverStateInfo(
				preAndPostString.fst(), preAndPostString.snd()).snd());
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
		result.addAll(PermParser.getReceiverStateInfo(
				preAndPostString.fst(), preAndPostString.snd()).fst());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "@Perm(req=\"" + preAndPostString.fst() + "\", ens=\"" + preAndPostString.snd() + "\")";
	}

}
