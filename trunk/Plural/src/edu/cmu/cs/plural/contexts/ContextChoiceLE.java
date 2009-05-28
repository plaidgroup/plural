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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.plural.linear.DisjunctiveVisitor;

/**
 * Intended for the case where you need to prove something in 
 * one context, of several.  It corresponds to alternative conjunction
 * in linear logic, usually written <b>&amp;</b>.
 * 
 * @author Kevin Bierhoff
 * @since 4/16/2008
 */
public final class ContextChoiceLE implements LinearContext {
	
	/**
	 * Creates an alternative disjunction with the given elements.
	 * @param elements
	 * @return an alternative disjunction with the given elements.
	 */
	public static ContextChoiceLE choice(Set<LinearContext> elements) {
		return new ContextChoiceLE(elements);
	}

	private ContextChoiceLE() {
		elements = new LinkedHashSet<LinearContext>();
	}

	private ContextChoiceLE(Set<LinearContext> elements) {
		this.elements = elements;
		
	}
	
	//
	// DisjunctiveLE methods
	// 

	@Override
	public <T> T dispatch(DisjunctiveVisitor<T> visitor) {
		return visitor.choice(this);
	}
	
	/**
	 * Keeps only the most precise elements in the choice, since only one has to be satisfied.
	 * @tag artifact.explanation -id="676301" : ContextAllLE.compact is a near code-clone of this method
	 * @tag todo.general -id="676303" : get rid of hack for case where no freezing happens
	 * @see LinearContext#compact(ASTNode, boolean)
	 */
	@Override
	public LinearContext compact(ASTNode node, boolean freeze) {
		if(isFrozen())
			return this;
		LinkedHashSet<LinearContext> compacted = new LinkedHashSet<LinearContext>();
		
		next_elem:
		for(LinearContext e : getElements()) {
			e = e.compact(node, freeze);
			if(ContextFactory.isFalseContext(e))
				// one false makes the whole thing false, since false more precise than anything
				return ContextFactory.falseContext();
			if(ContextFactory.isTrueContext(e))
				// true contexts less precise than anything--drop it
				// dropping everything makes the whole thing true
				continue;
			if(this.getClass().equals(e.getClass())) {
				LinkedHashSet<LinearContext> subElems = 
					new LinkedHashSet<LinearContext>(((ContextChoiceLE) e).getElements());
				next_sub:
				for(Iterator<LinearContext> subIt = subElems.iterator(); subIt.hasNext(); ) {
					// compare all sub-elements in e with elements in compacted  
					LinearContext sub = subIt.next();
					for(Iterator<LinearContext> it = compacted.iterator(); it.hasNext(); ) {
						LinearContext in = it.next();
						// if in and sub are comparable keep the MORE precise one
						if(freeze && in.atLeastAsPrecise(sub, node)) {
							subIt.remove();
							continue next_sub; // skip sub
						}
						if(freeze && sub.atLeastAsPrecise(in, node))
							it.remove(); // replace in with sub
					}
				}
				compacted.addAll(subElems);
			}
			else {
				// compare e to elements already in compacted
				for(Iterator<LinearContext> it = compacted.iterator(); it.hasNext(); ) {
					LinearContext in = it.next();
					// if in and e are comparable keep the MORE precise one
					if(freeze && in.atLeastAsPrecise(e, node))
						continue next_elem; // skip e
					if(freeze && e.atLeastAsPrecise(in, node))
						it.remove();
				}
				compacted.add(e);
			}				
		}
		if(compacted.size() == 1)
			return compacted.iterator().next();
		setElements(freeze ? Collections.unmodifiableSet(compacted) : compacted);
		setFrozen(freeze);
		return this;
	}

	//
	// LatticeElement methods
	//

	@Override
	public boolean atLeastAsPrecise(LinearContext other, final ASTNode node) {
		this.freeze();
		if(this == other)
			return true;
		other.freeze();
		
		// This implements proving other with the given choices
		// For completeness, first break down other until atoms (tuples)
		// are found.  Then, break down the receiver using the helper
		// atLeastAsPrecise method.
		final DisjunctiveVisitor<Boolean> compVisitor = new DisjunctiveVisitor<Boolean>() {
			
			@Override
			public Boolean choice(ContextChoiceLE other) {
				for(LinearContext otherElem : other.getElements()) {
					if(! otherElem.dispatch(this))
						return false;
				}
				return true;
			}

			@Override
			public Boolean trueContext(TrueContext trueContext) {
				return true;
			}
			
			@Override
			public Boolean context(TensorContext other) {
				return ContextChoiceLE.this.atLeastAsPrecise(other.getTuple(), node);
			}

			@Override
			public Boolean falseContext(FalseContext falseContext) {
				return false;
			}
		};
		return other.dispatch(compVisitor);
	}

	@Override
	public boolean atLeastAsPrecise(TensorPluralTupleLE other, ASTNode node) {
		// all choices we could make have to be more precise than other
		for(LinearContext e : getElements()) {
			if(e.atLeastAsPrecise(other, node))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return elemString("UNIT", " & ");
	}
	
	/*
	 * AbstractDisjunctiveLE methods
	 */
	protected LinearContext create(Set<LinearContext> newElements) {
		return choice(newElements);
	}

	private Set<LinearContext> elements;
	private boolean frozen;
	
	public Set<LinearContext> getElements() {
		return elements;
	}
	
	/**
	 * This method must not be called when frozen.
	 * @param elements
	 */
	protected void setElements(Set<LinearContext> elements) {
		if(frozen)
			throw new IllegalStateException("Cannot modify frozen object");
		this.elements = elements;
	}

	protected final String elemString(String empty, String separator) {
		if(elements.isEmpty())
			return empty;
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for(LinearContext e : elements) {
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
	public LinearContext freeze() {
		if(!frozen) {
			frozen = true;
			LinkedHashSet<LinearContext> compacted = new LinkedHashSet<LinearContext>();
			for(LinearContext e : elements) {
				e = e.freeze();
				/*if(ContextFactory.isFalseContext(e)) {
					// drop it
				}
				else*/ if(this.getClass().equals(e.getClass())) {
					compacted.addAll(((ContextChoiceLE) e).getElements());
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
	public LinearContext mutableCopy() {
		LinkedHashSet<LinearContext> mutableSet = new LinkedHashSet<LinearContext>(elements.size());
		for(LinearContext e : elements) {
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
	public LinearContext join(LinearContext other, ASTNode node) {
		this.freeze();
		if(other == this)
			return this;
		other.freeze();
		
		LinkedHashSet<LinearContext> newElems = new LinkedHashSet<LinearContext>(getElements().size());
		for(LinearContext e : getElements()) {
			newElems.add(e.join(other, node));
		}
		return create(newElems);
	}

	@Override
	public LinearContext copy() {
		freeze();
		return this;
	}
}
