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
package edu.cmu.cs.plural.examples;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.NoEffects;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;

/**
 * This class demonstrates how to control field accesses with Plural
 * by using dimensions.
 * The class defines two state dimensions, A and B, and two fields, a and b.
 * The invariants make Plural map field a (b) into dimension A (B).
 * (Notice that without the invariants, Plural would currently map both
 * fields into the root state, <i>alive</i>, but it would be better to
 * also have a way of explicitly mapping fields into dimensions for the
 * purpose of just controlling which methods access which fields. 
 * @author Kevin Bierhoff
 * @since Feb 18, 2009
 */
@Refine({
	@States(dim = "A", value = {}),  // dimension A, do not care about states within A
	@States(dim = "B", value = {})   // dimension B, do not care about states within B
})
@ClassStates({
	@State(name = "A", inv = "a != null"),  // force a to be mapped into A
	@State(name = "B", inv = "b != null")   // force b to be mapped into B
})
@PassingTest
@UseAnalyses({PluralAnalysis.SYNTAX, PluralAnalysis.EFFECT, PluralAnalysis.PLURAL})
public class FieldAccessControl {

	private Object a;
	private Object b;
	
	@Perm(requires = "#0 != null")
	@Full(value = "A", fieldAccess = true)
	public void setA(Object a) {
		this.a = a;
	}
	
	@NoEffects // pure method--not necessary for checking
	@Perm(ensures = "result != null")
	@Pure(value = "A", fieldAccess = true)
	public Object getA() {
		return a;
	}
	
	@Perm(requires = "#0 != null")
	@Full(value = "B", fieldAccess = true)
	public void setB(Object b) {
		this.b = b;
	}
	
	@NoEffects // pure method--not necessary for checking
	@Perm(ensures = "result != null")
	@Pure(value = "B", fieldAccess = true)
	public Object getB() {
		return b;
	}
	
}
