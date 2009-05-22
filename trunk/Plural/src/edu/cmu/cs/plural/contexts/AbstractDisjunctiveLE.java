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
package edu.cmu.cs.plural.contexts;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author Kevin Bierhoff
 * @since 4/16/2008
 */
public abstract class AbstractDisjunctiveLE implements DisjunctiveLE {
	
	private Set<DisjunctiveLE> elements;
	private boolean frozen;
	
	protected AbstractDisjunctiveLE() {
		elements = new LinkedHashSet<DisjunctiveLE>();
	}

	protected AbstractDisjunctiveLE(Set<DisjunctiveLE> elements) {
		this.elements = elements;
	}
	
	public Set<DisjunctiveLE> getElements() {
		return elements;
	}
	
	/**
	 * This method must not be called when frozen.
	 * @param elements
	 */
	protected void setElements(Set<DisjunctiveLE> elements) {
		if(frozen)
			throw new IllegalStateException("Cannot modify frozen object");
		this.elements = elements;
	}

	protected final String elemString(String empty, String separator) {
		if(elements.isEmpty())
			return empty;
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for(DisjunctiveLE e : elements) {
			if(first)
				first = false;
			else
				result.append(separator);
			result.append(e.toString());
		}
		return result.toString();
	}
	
	//
	// Freezable methods
	//

	@Override
	public DisjunctiveLE freeze() {
		if(!frozen) {
			frozen = true;
			LinkedHashSet<DisjunctiveLE> compacted = new LinkedHashSet<DisjunctiveLE>();
			for(DisjunctiveLE e : elements) {
				e = e.freeze();
				/*if(ContextFactory.isFalseContext(e)) {
					// drop it
				}
				else*/ if(this.getClass().equals(e.getClass())) {
					compacted.addAll(((AbstractDisjunctiveLE) e).getElements());
				}
				else {
					compacted.add(e);
				}
			}
			elements = Collections.unmodifiableSet(compacted);
		}
		return this;
	}
	
	@Override
	public DisjunctiveLE mutableCopy() {
		LinkedHashSet<DisjunctiveLE> mutableSet = new LinkedHashSet<DisjunctiveLE>(elements.size());
		for(DisjunctiveLE e : elements) {
			mutableSet.add(e.mutableCopy());
		}
		return create(mutableSet);
	}

	protected boolean isFrozen() {
		return frozen;
	}
	
	protected void setFrozen(boolean newFrozen) {
		if(this.frozen && !newFrozen)
			throw new IllegalStateException("Cannot unfreeze frozen object");
		this.frozen = newFrozen;
	}

	//
	// LatticeElement methods
	//

	@Override
	public DisjunctiveLE join(DisjunctiveLE other, ASTNode node) {
		this.freeze();
		if(other == this)
			return this;
		other.freeze();
		
		LinkedHashSet<DisjunctiveLE> newElems = new LinkedHashSet<DisjunctiveLE>(getElements().size());
		for(DisjunctiveLE e : getElements()) {
			newElems.add(e.join(other, node));
		}
		return create(newElems);
	}

	@Override
	public DisjunctiveLE copy() {
		freeze();
		return this;
	}

	protected abstract DisjunctiveLE create(Set<DisjunctiveLE> newElements);

}
