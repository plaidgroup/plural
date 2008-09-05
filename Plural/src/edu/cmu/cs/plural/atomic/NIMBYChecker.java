/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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
package edu.cmu.cs.plural.atomic;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.FieldAccess;

import edu.cmu.cs.plural.fractions.Fraction;
import edu.cmu.cs.plural.fractions.FractionConstraint;
import edu.cmu.cs.plural.fractions.FractionalPermission;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;
import edu.cmu.cs.plural.track.SingleTruthFractionalAnalysis;
import edu.cmu.cs.plural.track.SingleTruthFractionalTransfer;

/**
 * Not In My Back Yard:
 * Run an analysis that attempts to check if atomic blocks are used correctly.
 * 
 * @author Nels Beckman
 * @date Mar 4, 2008
 *
 */
public class NIMBYChecker extends SingleTruthFractionalAnalysis {

	/*
	 * Override the FACTORY method to return our specific transfer function.
	 */
	@Override
	protected SingleTruthFractionalTransfer createNewFractionalTransfer() {
		return new NIMBYTransferFunction(analysisInput.getAnnoDB(), this);
	}
	
	@Override
	protected FractionalChecker createASTWalker() {
		return new NIMBYVisitor();
	}

	/**
	 * My private AST visitor, inherits most of its functionality from the
	 * protected inner class FractionalChecker, however we must do some
	 * extra work to make sure that if we unpack a thread-shared permission
	 * we are actually inside an atomic block.
	 */
	private class NIMBYVisitor extends FractionalChecker {
		
		/**
		 * Are we treating FULL permissions as thread-shared permissions? Should
		 * be set to true for weak atomicity semantics.
		 */
		private static final boolean fullCountsAsThreadShared = true;

		/**
		 * This is really bad for performance reasons. I am already doing this
		 * exact same analysis on the same methods inside of NIMBYTransferFunction.
		 */
		private final IsInAtomicAnalysis isInAtomicAnalysis = new IsInAtomicAnalysis();
		
		private boolean didUnpackOccurHere(ASTNode node) {
			return 
			    NIMBYChecker.this.getFa().getResultsBefore(node).isRcvrPacked() &&
			   !NIMBYChecker.this.getFa().getResultsAfter(node).isRcvrPacked();
		}

		/**
		 * Report an error to the user if we are not inside an atomic block and the
		 * permission that we just unpacked at this node is thread-shared.
		 */
		private void assertInAtomicIfTShared(ASTNode node) {	
//			final PluralTupleLatticeElement lattice =
//				NIMBYChecker.this.getFa().getResultsAfter(node);
//			
//			FractionalPermissions this_perm = lattice.get(node, 
//					getTf().getAnalysisContext().getThisVariable());
//			FractionalPermission unpacked_perm = this_perm.getUnpackedPermission();
//			/*
//			 * Is permission thread-shared?
//			 */
//			Fraction belowF = unpacked_perm.getFractions().getBelowFraction();
//			if(unpacked_perm.isReadOnly()) {
//				if(this_perm.getConstraints().testConstraint(
//						FractionConstraint.createEquality(belowF, Fraction.zero())) == false)
//					// unpacked permission cannot be pure -> must be immutable
//						return;
//			}
//			else {
//				Fraction aliveF = unpacked_perm.getFractions().get(StateSpace.STATE_ALIVE);
//				if(this_perm.getConstraints().testConstraint(
//						FractionConstraint.createLessThan(aliveF, Fraction.one())) == false)
//					// unpacked permission must be unique
//					return;
//				if(!fullCountsAsThreadShared && this_perm.getConstraints().testConstraint(
//						FractionConstraint.createLessThan(belowF, Fraction.one())) == false) {
//					// unpacked permission must be full, and we don't worry about that
//					return;
//				}
//			}
//			/*
//			 * Are we in an atomic block?
//			 */
//			if( !isInAtomicAnalysis.isInAtomicBlock(node) ) {
//				crystal.reportUserProblem(
//						"Thread-shared permission is being unpacked outside of an atomic block. " +
//						"Put this (and possibly other) statement(s) inside atomic!",
//						node, NIMBYChecker.this);
//			}
		}
		
		/*
		 * Override possible unpacking nodes.
		 */
		@Override
		public void endVisit(Assignment node) {
			super.endVisit(node);
			assertInAtomicIfTShared(node);
		}

		@Override
		public void endVisit(FieldAccess node) {
			super.endVisit(node);
			assertInAtomicIfTShared(node);
		}
	}
}
