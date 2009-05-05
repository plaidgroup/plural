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
package edu.cmu.cs.plural.concurrent.nimby;

import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.concurrent.ConcurrentChecker;
import edu.cmu.cs.plural.concurrent.MutexWalker;
import edu.cmu.cs.plural.linear.PluralDisjunctiveLE;
import edu.cmu.cs.plural.track.FractionalTransfer;

/**
 * Not In My Back Yard:
 * Run an analysis that attempts to check if atomic blocks are used correctly.
 * 
 * @author Nels Beckman
 * @date Mar 4, 2008
 *
 */
public class NIMBYChecker extends ConcurrentChecker {

	/*
	 * Override the FACTORY method to return our specific transfer function.
	 */
	@Override
	protected FractionalTransfer createNewFractionalTransfer() {
		return new NIMBYTransferFunction(analysisInput, this);
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
	private class NIMBYVisitor extends ConcurrentVisitor {
		
		/**
		 * Are we treating FULL permissions as thread-shared permissions? Should
		 * be set to true for weak atomicity semantics.
		 */
		private static final boolean FULL_PERMISSION_MUST_UNPACK_IN_ATOMIC = true;

		/**
		 * This is really bad for performance reasons. I am already doing this
		 * exact same analysis on the same methods inside of NIMBYTransferFunction.
		 */
		private final IsInAtomicAnalysis isInAtomicAnalysis = new IsInAtomicAnalysis();

		@Override
		protected boolean doesRcvrPermissionRequireProtection(PluralDisjunctiveLE lattice) {
			if( FULL_PERMISSION_MUST_UNPACK_IN_ATOMIC ) {
				return lattice.isRcvrFullSharePureInAnyDisjunct();
			} else {
				return Utilities.nyi();
			}
		}

		@Override
		protected MutexWalker getMutexWalker() {
			return isInAtomicAnalysis;
		}

		@Override
		protected String getUnpackedErrorMsg() {
			return "Receiver is unpacked outside of atomic block, " +
					"but doesn't have Unique or Immutable permission.";
		}
	}
}
