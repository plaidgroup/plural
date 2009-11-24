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
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;

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
	 * @param specBinding The binding that contains specs to be used
	 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
	 * which can be different from <code>specBinding</code> if specifications are inherited.
	 * @param cases
	 */
	protected AbstractMultiCaseSignature(AnnotationDatabase annoDB, IMethodBinding specBinding,
			IMethodBinding staticallyInvokedBinding, PermAnnotation... cases) {
		super(annoDB, specBinding, staticallyInvokedBinding);
		if(cases.length == 0) {
			this.cases = Collections.singletonList(createCase(annoDB, specBinding, null, staticallyInvokedBinding));
		}
		else { 
			this.cases = new ArrayList<T>(cases.length);
			for(PermAnnotation perm : cases) {
				this.cases.add(createCase(annoDB, specBinding, perm, staticallyInvokedBinding));
			}
		}
	}

	/**
	 * Create a case for the given @Perm annotation (optional)
	 * @param annoDB
	 * @param binding
	 * @param perm @Perm annotation or <code>null</code> if no such annotation is given.
	 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
	 * which can be different from <code>specBinding</code> if specifications are inherited.
	 * @return
	 */
	abstract protected T createCase(AnnotationDatabase annoDB, IMethodBinding binding,
			PermAnnotation perm, IMethodBinding staticallyInvokedBinding);

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#cases()
	 */
	@Override
	public List<T> cases() {
		return cases;
	}
}
