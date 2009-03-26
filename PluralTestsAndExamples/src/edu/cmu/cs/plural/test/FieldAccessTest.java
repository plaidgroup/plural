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
package edu.cmu.cs.plural.test;

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Use;

/**
 * This test case makes sure that accesses to fields
 * with invariants require a frame permission.
 * @author Kevin Bierhoff
 * @since Mar 13, 2009
 *
 */
@FailingTest(6)
@UseAnalyses(PluralAnalysis.PLURAL)
@ClassStates(@State(name = "A", inv = "o1 != null"))
public class FieldAccessTest {
	
	private Object o1; // need field access 
	private Object o2; // no field access needed
	
	@Full("A")
	@Perm(requires = "#0 != null")
	public void setA(Object o) {
		this.o1 = o; // error: need field access
	}
	
	@Full(guarantee = "A", use = Use.FIELDS)
	@Perm(requires = "#0 != null")
	public void setO1(Object o) {
		this.o1 = o; // ok: have field access
	}
	
	@Full("A")
	public void setB(Object o) {
		this.o2 = o; // ok: o2 is not part of invariant
	}

	@Full(guarantee = "B", use = Use.FIELDS)
	public void setO2(Object o) {
		this.o1 = o; // error: field access to wrong state
	}
	
	@Pure(guarantee = "A", use = Use.FIELDS)
	public Object getA() {
		Object result = o1;
		o1 = new Object(); // error: need modifying permission
		return result;
	}
	
	@Pure(guarantee = "B", use = Use.FIELDS)
	public Object getB() {
		Object result = o2;
		o2 = new Object(); // ok: o2 is not part of invariant
		return result;
	}
	
	@Pure(guarantee = "A", use = Use.FIELDS)
	@Perm(ensures = "result != null")
	public Object getO1() {
		return o1; // ok: have field access
	}
	
	@Pure(guarantee = "B")
	public Object getO2() {
		return o2; // ok: o2 not part of invariant
	}
	
	@Pure(guarantee = "A")
	@Perm(ensures = "result != null")
	public Object getNonNull() {
		return o1; // error: cannot unpack, so no invariant
	}
	
	@Pure(guarantee = "A")
	public Object getRef() {
		return o1; // error: need field access
	}
	
	@Full(guarantee = "A", use = Use.DISP_FIELDS)
	@Perm(requires = "#0 != null")
	public Object replace(Object o) {
		Object result = o1; // ok: have field access
		setO1(o); // ok: can dispatch, too!
		return result;
	}
	
	@Full(guarantee = "A", use = Use.FIELDS)
	@Perm(requires = "#0 != null")
	public Object failReplace(Object o) {
		Object result = o1; // ok: have field access
		setO1(o); // error: cannot do dynamic dispatch
		return result;
	}
	
}
