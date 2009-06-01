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

import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.crystal.util.CollectionMethods;
import edu.cmu.cs.plural.errors.ChoiceID;
import edu.cmu.cs.plural.fractions.FractionConstraints;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.linear.DisjunctiveVisitor;
import edu.cmu.cs.plural.linear.TestVisitor;

/**
 * Factory methods to create linear logic connectives.
 * 
 * @author Kevin Bierhoff
 * @since 4/16/2008
 */
public class ContextFactory {
	
	/**
	 * Creates a multiplicative conjunction as a wrapper around the given tuple.
	 * @param le
	 * @return
	 */
	public static LinearContext tensor(TensorPluralTupleLE le, ChoiceID parentChoiceID, ChoiceID choiceID) {
		return TensorContext.tensor(le, parentChoiceID, choiceID);
	}

	/**
	 * Creates an alternative disjunction with the given elements.
	 * @param elements
	 * @return
	 */
	public static LinearContext choice(Set<LinearContext> elements) {
		return ContextChoiceLE.choice(elements);
	}

	/**
	 * Creates an alternative disjunction with the given elements.
	 * Duplicates in the given array will be dropped.
	 * @param elements
	 * @return
	 */
	public static LinearContext choice(LinearContext... elements) {
		return ContextChoiceLE.choice(CollectionMethods.mutableSet(elements));
	}
	
	/**
	 * This is the <b>true</b> context, which can essentially prove nothing
	 * but <b>true</b> itself.  This corresponds to the linear logic <b>unit</b> 
	 * (universal truth, usually written <b>1</b>).
	 * @param parentChoiceID 
	 * @param choiceID
	 * @return A <b>true</b> context.
	 * 
	 */
	public static LinearContext trueContext(ChoiceID parentChoiceID, ChoiceID choiceID) {
		return new TrueContext(parentChoiceID, choiceID);
	}

	/**
	 * This is the <b>false</b> context, which can prove anything.
	 * This corresponds to the linear logic <b>void</b> predicate
	 * (usually written <b>0</b>).
	 * @param parentChoiceID The id of the parent of the choice node for this new context.
	 * @param choiceID The id of the choice node that this new context should have.
	 * @return the <b>false</b> context.
	 */
	public static LinearContext falseContext(ChoiceID parentChoiceID, ChoiceID choiceID) {
		return new FalseContext(parentChoiceID, choiceID);
	}
	
	/**
	 * Return a true context that holds information about which state we were
	 * trying to pack to when we realized we couldn't.
	 * 
	 * @param state Which state were we trying to pack to when we failed?
	 * @param invariant What was the particular invariant that failed?
	 * @return A context representing true (1) which also stores error info.
	 */
	public static LinearContext failedPack(String state, String invariant, ChoiceID parentChoiceID, ChoiceID choiceID) {
		return new FailingPackContext(state, invariant, parentChoiceID, choiceID);
	}
	
	/**
	 * Tests if the given disjunctive context is equivalent to the  
	 * {@link #falseContext()}.  That essentially means
	 * that {@link #choice(Set)} contexts 
	 * contain at least one context equivalent to false, and that 
	 * {@link #all(Set)} contexts are emtpy or contain only contexts
	 * that are equivalent to false.  Only bottom 
	 * {@link #tensor(TensorPluralTupleLE)} contexts and empty
	 * {@link #all(Set)} contexts count as
	 * equivalent to false.
	 * @param le
	 * @return <code>true</code> if this context is equivalent to 
	 * {@link #falseContext()}, <code>false</code> otherwise.
	 * @see #isImpossible(LinearContext)
	 */
	public static boolean isFalseContext(LinearContext le) {
		// simply test for emptiness of tuples; TestVisitor already does the right thing for empty contexts
		return le.dispatch(emptyVisitor);
	}
	
	/**
	 * Helper test visitor for {@link #isFalseContext(LinearContext)}.
	 * @author Kevin Bierhoff
	 */
	private static final TestVisitor emptyVisitor = new TestVisitor() {
		@Override
		public boolean testTuple(TensorPluralTupleLE tuple) {
			// any "real" tuple counts as non-empty
			return tuple.isBottom();
		}
	};
	
	
	
	/**
	 * Tests if the given disjunctive context is equivalent to the  
	 * {@link #trueContext()}.  That essentially means
	 * that {@link #choice(Set)} contexts are empty or contain only
	 * contexts that are equivalent to true, and that 
	 * {@link #all(Set)} contexts contain at least one context
	 * that is equivalent to true.  Only empty {@link #choice(Set)} 
	 * contexts count as equivalent to true.
	 * @param le
	 * @return <code>true</code> if this context is equivalent to 
	 * {@link #trueContext()}, <code>false</code> otherwise.
	 * @see #isImpossible(LinearContext)
	 */
	public static boolean isTrueContext(LinearContext le) {
		// turns out the way TestVisitor works, we have to negate the result
		return ! le.dispatch(anyTupleVisitor);
	}
	
	/**
	 * Helper test visitor for {@link #isTrueContext(LinearContext)}.
	 * @author Kevin Bierhoff
	 */
	private static final TestVisitor anyTupleVisitor = new TestVisitor() {
		@Override
		public boolean testTuple(TensorPluralTupleLE tuple) {
			// any tuple counts 
			return true;
		}
	};
	
	/**
	 * Tests if the given disjunctive context is impossible.
	 * This is a lightweight test to detect errors in a context
	 * that does <b>not</b> involve solving fraction constraints.  
	 * {@link #choice(Set)} contexts are impossible if they are empty or
	 * contain only impossible contexts, and 
	 * {@link #all(Set)} contexts are impossible if they contain at least one
	 * context that's impossible.  
	 * {@link #tensor(TensorPluralTupleLE)} contexts are
	 * impossible if one of their constraint sets
	 * is {@link FractionConstraints#isImpossible()}.
	 * @param le
	 * @return <code>true</code> if this context is impossible, 
	 * <code>false</code> otherwise.
	 * @see #isFalseContext(LinearContext)
	 */
	public static boolean isImpossible(LinearContext le) {
		return le.dispatch(impossibleVisitor);
	}
	
	/**
	 * Helper descending visitor for {@link #isImpossible(LinearContext)}.
	 * @author Kevin Bierhoff
	 */
	private static final DisjunctiveVisitor<Boolean> impossibleVisitor = new DisjunctiveVisitor<Boolean>() {

		@Override
		public Boolean choice(ContextChoiceLE le) {
			for(LinearContext e : le.getElements()) {
				if(! isImpossible(e))
					return false;
			}
			return true;
		}

		@Override
		public Boolean trueContext(TrueContext trueContext) {
			return true;
		}
		
		@Override
		public Boolean context(TensorContext le) {
			for(Iterator<FractionalPermissions> it = le.getTuple().tupleInfoIterator(); it.hasNext(); ) {
				if(it.next().isImpossible())
					return true;
			}
			return false;
		}

		@Override
		public Boolean falseContext(FalseContext falseContext) {
			return false;
		}		
	};

	/**
	 * Tests whether the given lattice element contains a single
	 * {@link #tensor(TensorPluralTupleLE)} context.
	 * @param le A non-<code>null</code> lattice element.
	 * @return <code>true</code> if the given lattice element contains a single
	 * context, <code>false</code> otherwise.
	 */
	public static boolean isSingleContext(LinearContext le) {
		return le.dispatch(singleContextVisitor);
	}
	
	/**
	 * Helper descending visitor for {@link #isSingleContext(LinearContext)}.
	 * @author Kevin Bierhoff
	 */
	private static final DisjunctiveVisitor<Boolean> singleContextVisitor = new DisjunctiveVisitor<Boolean>() {

		@Override
		public Boolean choice(ContextChoiceLE le) {
			if(le.getElements().isEmpty() || le.getElements().size() > 1)
				return false;
			// visit the one element
			return le.getElements().iterator().next().dispatch(this);
		}

		@Override
		public Boolean trueContext(TrueContext trueContext) {
			return false;
		}
		
		@Override
		public Boolean context(TensorContext le) {
			// found the single element
			return true;
		}

		@Override
		public Boolean falseContext(FalseContext falseContext) {
			return false;
		}		
	};
}