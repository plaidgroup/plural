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
 * {@code ContextAllLE} is currently unused, but it's intended for the case where
 * you need to prove something in all contexts.  It corresponds to alternative
 * disjunction in linear logic, which we write as <b>+</b> (omitting the conventional
 * circle around the plus symbol).
 * 
 * @author Kevin Bierhoff
 * @since 4/16/2008
 */
public final class ContextAllLE extends AbstractDisjunctiveLE implements DisjunctiveLE {
	
	public static ContextAllLE all(Set<DisjunctiveLE> elements) {
		return new ContextAllLE(elements);
	}
	
	public static ContextAllLE falseContext() {
		return new ContextAllLE();
	}

	private ContextAllLE() {
		super();
	}

	private ContextAllLE(Set<DisjunctiveLE> elements) {
		super(elements);
	}

	//
	// DisjunctiveLE methods
	// 

	@Override
	public <T> T dispatch(DisjunctiveVisitor<T> visitor) {
		return visitor.all(this);
	}

	/**
	 * Keeps only the least precise elements in the disjunction, since all have to be satisfied.
	 * @tag artifact.explanation -id="676302" : this is a near code-clone of ContextChoiceLE.compact
	 * @see DisjunctiveLE#compact(ASTNode, boolean)
	 */
	@Override
	public DisjunctiveLE compact(ASTNode node, boolean freeze) {
		if(isFrozen())
			return this;
		LinkedHashSet<DisjunctiveLE> compacted = new LinkedHashSet<DisjunctiveLE>();
		
		next_elem:
		for(DisjunctiveLE e : getElements()) {
			e = e.compact(node, freeze);
			if(ContextFactory.isTrueContext(e))
				// one true makes the whole thing true, since true is less precise than anything
				ContextFactory.trueContext();
			if(ContextFactory.isFalseContext(e))
				// false context more precise than anything--drop it
				// dropping all contexts makes the whole thing false
				continue; 
			if(this.getClass().equals(e.getClass())) {
				LinkedHashSet<DisjunctiveLE> subElems = 
					new LinkedHashSet<DisjunctiveLE>(((AbstractDisjunctiveLE) e).getElements());
				next_sub:
				for(Iterator<DisjunctiveLE> subIt = subElems.iterator(); subIt.hasNext(); ) {
					// compare all sub-elements in e with elements in compacted  
					DisjunctiveLE sub = subIt.next();
					for(Iterator<DisjunctiveLE> it = compacted.iterator(); it.hasNext(); ) {
						DisjunctiveLE in = it.next();
						// if in and sub are comparable then keep the LESS precise one
						if(freeze && sub.atLeastAsPrecise(in, node)) {
							subIt.remove();
							continue next_sub; // skip sub
						}
						if(freeze && in.atLeastAsPrecise(sub, node))
							it.remove(); // replace in with sub
					}
				}
				compacted.addAll(subElems);
			}
			else {
				// compare e to elements already in compacted
				for(Iterator<DisjunctiveLE> it = compacted.iterator(); it.hasNext(); ) {
					DisjunctiveLE in = it.next();
					// if in and e are comparable then keep the LESS precise one
					if(freeze && e.atLeastAsPrecise(in, node))
						continue next_elem; // skip e
					if(freeze && in.atLeastAsPrecise(e, node))
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
	public boolean atLeastAsPrecise(DisjunctiveLE other, final ASTNode node) {
		this.freeze();
		if(this == other)
			return true;
		other.freeze();
		
		if(getElements().isEmpty())
			// false context always more precise
			return true;
		
		// This implements proving other with the given disjuncts.
		// For completeness, first break down other until atoms (tuples)
		// are found.  Then, break down the receiver using the helper
		// atLeastAsPrecise method.
		final DisjunctiveVisitor<Boolean> compVisitor = new DisjunctiveVisitor<Boolean>() {
			
			@Override
			public Boolean choice(ContextChoiceLE other) {
				for(DisjunctiveLE otherElem : other.getElements()) {
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
			public Boolean context(LinearContextLE other) {
				return ContextAllLE.this.atLeastAsPrecise(other.getTuple(), node);
			}

			@Override
			public Boolean all(ContextAllLE other) {
				for(DisjunctiveLE otherElem : other.getElements()) {
					if(otherElem.dispatch(this))
						return true;
				}
				return false;
			}
		};
		return other.dispatch(compVisitor);
	}

	@Override
	public boolean atLeastAsPrecise(TensorPluralTupleLE other, ASTNode node) {
		// since we have to succeed with all of the elements,
		// all our elements must be more precise than other
		for(DisjunctiveLE e : getElements()) {
			if(! e.atLeastAsPrecise(other, node))
				return false;
		}
		return true;
	}

	/*
	 * Overriden from Object
	 */

	@Override
	public String toString() {
		return super.elemString("VOID", " + ");
	}

	/*
	 * AbstractDisjunctiveLE methods
	 */

	@Override
	protected DisjunctiveLE create(Set<DisjunctiveLE> newElements) {
		return all(newElements);
	}

}
