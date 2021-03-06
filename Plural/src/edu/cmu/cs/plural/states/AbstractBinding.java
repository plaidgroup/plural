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

import java.lang.ref.WeakReference;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;

public class AbstractBinding {

	/** Binding carrying the invoked method's specification. */
	protected final IMethodBinding binding;
	
	/** The invoked method according to the type checker. */
	protected final IMethodBinding staticallyInvokedBinding;
	
	/** 
	 * Weakly reference annoDB as to not interfere with its garbage-collection.
	 * @see StateSpaceRepository
	 */
	protected final WeakReference<AnnotationDatabase> annoDB;
	
	/** <code>false</code> if the method declared to be non-reentrant, <code>true</code> otherwise. */ 
	private final boolean reentrant;
	
	/** <code>true</code> if the invoked method or an overridden method is declared as such. */
	private final boolean pure;

	/**
	 * @param annoDB
	 * @param specBinding The binding that contains specs to be used
	 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
	 * which can be different from <code>specBinding</code> if specifications are inherited.
	 */
	public AbstractBinding(AnnotationDatabase annoDB, IMethodBinding specBinding, IMethodBinding staticallyInvokedBinding) {
		assert annoDB != null;
		this.annoDB = new WeakReference<AnnotationDatabase>(annoDB);
		this.binding = specBinding;
		
		// TODO may want to move the following code to AbstractBindingSignature
		// As-is, this is evaluated again for every method case
		this.staticallyInvokedBinding = staticallyInvokedBinding;
		this.pure = EffectDeclarations.isPure(staticallyInvokedBinding, annoDB);
		// determine reentrancy from @NonReentrant annotation of invoked type
		for(ICrystalAnnotation a : annoDB.getAnnosForType(staticallyInvokedBinding.getDeclaringClass())) {
			if(a.getName().equals("edu.cmu.cs.plural.annot.NonReentrant")) {
				reentrant = false;
				return;
			}
		}
		reentrant = true;
	}
	
	public IMethodBinding getSpecifiedMethodBinding() {
		return binding;
	}

	protected AnnotationDatabase getAnnoDB() {
		AnnotationDatabase result = annoDB.get();
		assert result != null : "Annotation database was unexpectedly garbage-collected";
		return result;
	}

	protected AnnotationSummary getMethodSummary() {
		return getAnnoDB().getSummaryForMethod(binding);
	}
	
	protected StateSpace getStateSpace(ITypeBinding type) {
		return StateSpaceRepository.getInstance(getAnnoDB()).getStateSpace(type);
	}
	
	public boolean isReentrant() {
		return reentrant;
	}
	
	public boolean isEffectFree() {
		return pure;
	}

}