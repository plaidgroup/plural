/**
 * Copyright (C) 2007-2010 Carnegie Mellon University and others.
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

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.TrueIndicates;

/**
 * Regression test for a bug where equaling the result of a state test method
 * (like {@link #hasNext()}) leads to an infinite recursion that ends in a
 * stack overflow.
 * The test is written as failing b/c exceptions apparently get caught before
 * they reach JUnit.  So if there's an exception there's one less failure.
 * @author kevin
 * @since Oct 22, 2011
 */
@FailingTest(2)
@UseAnalyses(PluralAnalysis.PLURAL)
@ClassStates(@State(name = "hasNext", inv = "next != null"))
public final class ImplicationCopyTest {

	private ImplicationCopyTest next;
	
	@Pure
	@TrueIndicates("hasNext")
	public boolean hasNext() { 
		if (next != null)
			return true;
		else
			return false; 
	}
	
	@Full(requires = "hasNext")
	public void next() { }
	
	public static void implicitTrueTest(@Full ImplicationCopyTest t) {
		if (!t.hasNext())
			t.next();
	}

	public static void explicitTrueTest(@Full ImplicationCopyTest t) {
		// the following used to create a stack overflow
		// from infinite recursion trying to copy t.hasNext()'s
		// state implication to "true"
		if (t.hasNext() == false)
			t.next();
	}
}
