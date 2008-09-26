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

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Capture;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Lend;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Unique;

@FailingTest(3)
@UseAnalyses("FractionalAnalysis")
@ClassStates(@State(name = "alive", inv = "full(gct)"))
public class GCTest {
	
	private final GCTest gct;

	@Perm(ensures = "unique(this!fr)")
	public GCTest() {
		this.gct = null;
	}

	@Perm(requires = "full(#0)", ensures = "unique(this!fr)")
	public GCTest(@Capture(param = "p") GCTest gct) {
		this.gct = gct;
	}
	
	@Full
	public void needFull() {
	}
	
	@Pure
	public void needPure() {
	}
	
	@Perm(requires = "unique(this!fr)", ensures = "full(result)")
	@Lend(param = "outer")
	public GCTest getGct() {
		return gct; // error here b/c we pack before checking result
		// shouldn't pack at all here
	}
	
	public static void correctGcTest() {
		GCTest gct1 = new GCTest();
		GCTest gct2 = new GCTest(gct1);
		gct1.needPure();
		gct2.needFull();
		gct1.needFull();
	}
	
	public static void earlyUseTest() {
		GCTest gct1 = new GCTest();
		GCTest gct2 = new GCTest(gct1);
		gct1.needPure();
		gct1.needFull(); // error here because gct2 is still live
		gct2.needFull();
	}

	public static void correctBorrowingTest() {
		GCTest gct1 = new GCTest();
		GCTest gct2 = new GCTest(gct1);
		GCTest gotIt = gct2.getGct();
		gotIt.needFull();
		gct2.needFull();
		gct1.needFull();
	}

	public static void correctBorrowingReturn1(@Unique GCTest gct1) {
		GCTest gct2 = new GCTest(gct1);
		GCTest gotIt = gct2.getGct();
		gotIt.needFull();
		gct2.needFull();
		gct1.needFull();
	}

	public static void correctBorrowingReturn2(@Unique GCTest gct1) {
		GCTest gct2 = new GCTest(gct1);
		GCTest gotIt = gct2.getGct();
		gotIt.needFull();
		gct2.needFull();
	}

	public static void correctBorrowingReturn3(@Unique GCTest gct1) {
		GCTest gct2 = new GCTest(gct1);
		GCTest gotIt = gct2.getGct();
		gotIt.needFull();
	}

	public static void earlyBorrowingTest() {
		GCTest gct1 = new GCTest();
		GCTest gct2 = new GCTest(gct1);
		GCTest gotIt = gct2.getGct();
		gotIt.needFull();
		gct2.needFull(); // error here b/c gct2 is still borrowed to gotIt
		gotIt.needPure();
	}
}
