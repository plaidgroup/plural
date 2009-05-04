/**
 * Copyright (C) 2007-2009 Carnegie Mellon University and others.
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

package edu.cmu.cs.plural.concurrent.syncorswim;

import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.concurrent.ConcurrentChecker;
import edu.cmu.cs.plural.concurrent.MutexWalker;
import edu.cmu.cs.plural.track.FractionalTransfer;

/**
 * Sync or swim is a static analysis that uses the permission-based
 * reasoning of NIMBY on Java programs that use synchronized blocks
 * as the primary means of mutual exclusion.<br>
 * <br>
 * This analysis is very similar to Plural's FractionalAnalysis, which
 * it extends, but it must forget some extra things and adds some
 * additional constraints.<br>
 * <br>
 * Plan:<br> 
 * - Proactively drop state to alive for non-synchronized pure & share<br>
 * - make sure we have synchronized if we unpack for pure/share/full
 * @author Nels E. Beckman
 * @since May 4, 2009
 */
public class SyncOrSwim extends ConcurrentChecker {

	@Override
	protected FractionalChecker createASTWalker() {
		return new SynchronizedVisitor();
	}

	@Override
	protected FractionalTransfer createNewFractionalTransfer() {
		return super.createNewFractionalTransfer();
		//		return Utilities.nyi();
	}
	
	private class SynchronizedVisitor extends ConcurrentVisitor {

		private final IsSynchronizedRefAnalysis isSynchronizedRef = 
			new IsSynchronizedRefAnalysis();
		
		@Override
		protected MutexWalker getMutexWalker() {
			return this.isSynchronizedRef;
		}

		@Override
		protected String getUnpackedErrorMsg() {
			return "The receiver is unpacked and has full, pure or share permission, " +
					"but is not synchronized.";
		}
	}
	
}
