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
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Use;

/**
 * These are test cases for providing state information
 * in a post-condition when the annotated permission is consumed.
 * @author Kevin Bierhoff
 * @since Jan 2, 2010
 */
@FailingTest(2)
@UseAnalyses(PluralAnalysis.PLURAL)
public class ConsumeNewStateTest {

	@Perm(ensures = "unique(this!fr) in A")
	public ConsumeNewStateTest() {
	}

	/**
	 * This method fails because {@link #consume1()} does
	 * not establish the pre-condition for {@link #requiresB()}.
	 */
	public static void failing() {
		ConsumeNewStateTest c = new ConsumeNewStateTest();
		c.consume1();
		c.requiresB();
	}

	/**
	 * This method succeeds because {@link #consume2()} 
	 * establishes the pre-condition for {@link #requiresB()}.
	 */
	public static void succeeding() {
		ConsumeNewStateTest c = new ConsumeNewStateTest();
		c.consume2();
		c.requiresB();
	}

	/**
	 * This method fails because {@link #consume3()} is
	 * ill-specified.
	 */
	public static void illSpecifiedFailing() {
		ConsumeNewStateTest c = new ConsumeNewStateTest();
		c.consume3();
		c.requiresB();
	}

	@Pure("B")
	public void requiresB() {
	}

	/**
	 * This method consumes the given receiver permission and
	 * leaves the receiver (implicitly) in the <i>alive<i> state.
	 */
	@Full(requires = "A", returned = false)
	public void consume1() {
	}

	/**
	 * This method shows how to provide a post-condition state
	 * for a consumed permission, using an <b>in</b> clause
	 * in {@link Perm}.
	 * The <i>use</i> attribute allows the specified state change.
	 */
	@Perm(ensures = "this!fr in B")
	@Full(requires = "A", returned = false, use = Use.FIELDS)
	public void consume2() {
	}

	/**
	 * This method is ill-specified: the <i>ensures</i> attribute
	 * is ignored in {@link Full} etc if not <i>returned</i>.
	 * TODO The syntax checker should complain about this pattern.
	 */
	@Full(requires = "A", ensures = "B", returned = false, use = Use.FIELDS)
	public void consume3() {
	}
}
