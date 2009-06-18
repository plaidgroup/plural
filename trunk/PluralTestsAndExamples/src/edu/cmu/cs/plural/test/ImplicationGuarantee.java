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
package edu.cmu.cs.plural.test;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Use;

/**
 * The point of this test is to see if I can have a 
 * boolean == true implies perm type of state invariant. It seems like
 * it's not working in the blocking queue.
 * @author nbeckman
 * @since May 20, 2009
 *
 */
@PassingTest
@UseAnalyses(PluralAnalysis.PLURAL)
@Refine({
	@States(dim="SPEED", value={"FAST","SLOW"}),
	@States(dim="DUM", value={"Dumb"})
})
@ClassStates(@State(name="DUM", inv="i_have_full == true => full(this,SPEED) in FAST"))
public class ImplicationGuarantee {

	boolean i_have_full;
	
	@Share(guarantee="DUM", use=Use.DISP_FIELDS)
//	@Pure(guarantee="SPEED", use=Use.DISPATCH)
	void couldCallFullMethod() {
		if( this.i_have_full ) {
			this.i_have_full = false;
			this.fullMethod();
			// either need a return here or not have @Pure permission
			// KB: I believe this is because in the join we lose
			// the exact fraction for the pure permission from the
			// pre-condition, preventing us from proving the post-condition.
			// This could be avoided with Terauchi/Aiken-style join.
		}
	}
	
	@Full(guarantee="SPEED", requires="FAST")
	void fullMethod() {}
}
