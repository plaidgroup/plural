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
package edu.cmu.cs.plural.linear;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author Kevin Bierhoff
 * @since 4/16/2008
 */
public class LinearContextLE implements DisjunctiveLE {
	
	static LinearContextLE tensor(TensorPluralTupleLE tuple) {
		return new LinearContextLE(tuple);
	}

	private final TensorPluralTupleLE tuple;
	
	private LinearContextLE(TensorPluralTupleLE tuple) {
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
	public DisjunctiveLE compact(ASTNode node, boolean freeze) {
		if(freeze)
			tuple.freeze();
		return this;
	}

	/*
	 * Freezable methods
	 */

	@Override
	public LinearContextLE freeze() {
		tuple.freeze();
		return this;
	}

	@Override
	public LinearContextLE mutableCopy() {
		return create(tuple.mutableCopy());
	}

	/*
	 * LatticeElement methods
	 */

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.flow.LatticeElement, org.eclipse.jdt.core.dom.ASTNode)
	 */
	@Override
	public boolean atLeastAsPrecise(DisjunctiveLE other, final ASTNode node) {
		this.freeze();
		if(this == other)
			return true;
		other.freeze();
		final DisjunctiveVisitor<Boolean> compVisitor = new DisjunctiveVisitor<Boolean>() {
			
			@Override
			public Boolean choice(ContextChoiceLE other) {
				for(DisjunctiveLE otherElem : other.getElements()) {
					if(otherElem.dispatch(this))
						return true;
				}
				return false;
			}

			@Override
			public Boolean context(LinearContextLE other) {
				return LinearContextLE.this.tuple.atLeastAsPrecise(other.tuple, node);
			}

			@Override
			public Boolean all(ContextAllLE other) {
				for(DisjunctiveLE otherElem : other.getElements()) {
					if(! otherElem.dispatch(this))
						return false;
				}
				return true;
			}
		};
		return other.dispatch(compVisitor);
	}

	@Override
	public DisjunctiveLE join(DisjunctiveLE other, final ASTNode node) {
		this.freeze();
		if(this == other)
			return this;
		other.freeze();
		final DisjunctiveVisitor<DisjunctiveLE> joinVisitor = new DisjunctiveVisitor<DisjunctiveLE>() {

			@Override
			public DisjunctiveLE choice(ContextChoiceLE other) {
				return ContextChoiceLE.choice(joinElems(other.getElements()));
			}

			@Override
			public DisjunctiveLE context(LinearContextLE other) {
//				if(LinearContextLE.this.tuple.isUnsatisfiable() || other.tuple.isUnsatisfiable())
//					return ContextFactory.falseContext();
				TensorPluralTupleLE result = LinearContextLE.this.tuple.join(other.tuple, node);
				result.freeze();
//				if(result.isUnsatisfiable())
//					return ContextFactory.falseContext();
//				else
					return create(result);
			}

			@Override
			public DisjunctiveLE all(ContextAllLE other) {
				return ContextAllLE.all(joinElems(other.getElements()));
			}
			
			private Set<DisjunctiveLE> joinElems(Set<DisjunctiveLE> otherElems) {
				LinkedHashSet<DisjunctiveLE> joinedElems = 
					new LinkedHashSet<DisjunctiveLE>(otherElems.size());
				for(DisjunctiveLE otherElem : otherElems) {
					joinedElems.add(otherElem.dispatch(this));
				}
				return joinedElems;
			}
		};
		return other.dispatch(joinVisitor);
	}
	
	@Override
	public DisjunctiveLE copy() {
		freeze();
		return this;
	}

	/*
	 * Helper methods
	 */

	private LinearContextLE create(TensorPluralTupleLE mutableCopy) {
		return new LinearContextLE(mutableCopy);
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

//	/* (non-Javadoc)
//	 * @see java.lang.Object#hashCode()
//	 */
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((tuple == null) ? 0 : tuple.hashCode());
//		return result;
//	}
//
//	/* (non-Javadoc)
//	 * @see java.lang.Object#equals(java.lang.Object)
//	 */
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		LinearContextLE other = (LinearContextLE) obj;
//		if (tuple == null) {
//			if (other.tuple != null)
//				return false;
//		} else if (!tuple.equals(other.tuple))
//			return false;
//		return true;
//	}

}
