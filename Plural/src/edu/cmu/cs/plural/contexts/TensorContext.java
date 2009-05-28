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

import edu.cmu.cs.plural.linear.DisjunctiveVisitor;

/**
 * {@code LinearContextLE} wraps a {@code TensorPluralTupleLE}.
 * 
 * @author Kevin Bierhoff
 * @since 4/16/2008
 */
public class TensorContext implements LinearContext {
	
	public static TensorContext tensor(TensorPluralTupleLE tuple) {
		return new TensorContext(tuple);
	}

	private final TensorPluralTupleLE tuple;
	
	private TensorContext(TensorPluralTupleLE tuple) {
		this.tuple = tuple;
	}

	/**
	 * @return the tuple
	 */
	public TensorPluralTupleLE getTuple() {
		return tuple;
	}

	/*
	 * DisjunctiveLE methods
	 */

	@Override
	public <T> T dispatch(DisjunctiveVisitor<T> visitor) {
		return visitor.context(this);
	}
	
	@Override
	public LinearContext compact(ASTNode node, boolean freeze) {
		if(freeze)
			tuple.freeze();
		return this;
	}

	/*
	 * Freezable methods
	 */

	@Override
	public TensorContext freeze() {
		tuple.freeze();
		return this;
	}

	@Override
	public TensorContext mutableCopy() {
		return create(tuple.mutableCopy());
	}

	/*
	 * LatticeElement methods
	 */

	@Override
	public boolean atLeastAsPrecise(LinearContext other, final ASTNode node) {
		this.freeze();
		if(this == other)
			return true;
		other.freeze();
		
		// This implements proving other with the given tuple.
		// For completeness, first break down other until atoms (tuples)
		// are found.  Then, compare those tuples with the receiver's.
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
				return TensorContext.this.atLeastAsPrecise(other.tuple, node);
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
		return this.tuple.atLeastAsPrecise(other, node);	
	}

	@Override
	public LinearContext join(LinearContext other, final ASTNode node) {
		this.freeze();
		if(this == other)
			return this;
		other.freeze();
		final DisjunctiveVisitor<LinearContext> joinVisitor = new DisjunctiveVisitor<LinearContext>() {

			@Override
			public LinearContext choice(ContextChoiceLE other) {
				return ContextChoiceLE.choice(joinElems(other.getElements()));
			}

			@Override
			public LinearContext trueContext(TrueContext trueContext) {
				return trueContext;
			}
			
			@Override
			public LinearContext context(TensorContext other) {
//				if(LinearContextLE.this.tuple.isUnsatisfiable() || other.tuple.isUnsatisfiable())
//					return ContextFactory.falseContext();
				TensorPluralTupleLE result = TensorContext.this.tuple.join(other.tuple, node);
				result.freeze();
//				if(result.isUnsatisfiable())
//					return ContextFactory.falseContext();
//				else
					return create(result);
			}
			
			@Override
			public LinearContext falseContext(FalseContext falseContext) {
				Set<LinearContext> emptySet = Collections.<LinearContext>emptySet();
				return falseContext;
			}
			
			private Set<LinearContext> joinElems(Set<LinearContext> otherElems) {
				LinkedHashSet<LinearContext> joinedElems = 
					new LinkedHashSet<LinearContext>(otherElems.size());
				for(LinearContext otherElem : otherElems) {
					joinedElems.add(otherElem.dispatch(this));
				}
				return joinedElems;
			}
		};
		return other.dispatch(joinVisitor);
	}
	
	@Override
	public LinearContext copy() {
		freeze();
		return this;
	}

	/*
	 * Helper methods
	 */

	private TensorContext create(TensorPluralTupleLE mutableCopy) {
		return new TensorContext(mutableCopy);
	}

	/*
	 * Object methods
	 */

	@Override
	public String toString() {
		if(tuple.isBottom())
			return "BOT";
		else
			return "CTX";
//		return tuple.toString();
	}

}
