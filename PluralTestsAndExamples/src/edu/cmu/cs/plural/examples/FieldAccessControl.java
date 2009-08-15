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
import edu.cmu.cs.plural.annot.Fulls;
import edu.cmu.cs.plural.annot.NoEffects;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Use;

/**
 * This class demonstrates how to control field accesses with Plural
 * by using dimensions.
 * The class defines two state dimensions, A and B, and two fields, a and b.
 * The invariants make Plural map field a (b) into dimension A (B).
 * (Notice that without the invariants, Plural would currently map both
 * fields into the root state, <i>alive</i>, but it would be better to
 * also have a way of explicitly mapping fields into dimensions for the
 * purpose of just controlling which methods access which fields.
 * This example is inspired by Daniel Popescu.
 * @author Kevin Bierhoff
 * @since Feb 18, 2009
 */
@Refine({
	@States(dim = "A", value = {}),  // dimension A, do not care about states within A
	@States(dim = "B", value = {"B1", "B2"}),  // dimension B, do not care about states within B
	
})
@ClassStates({
	@State(name = "A", inv = "a != null"),  // force a to be mapped into A
	@State(name = "B1", inv = "b == null"),  
	@State(name = "B2", inv = "b != null")   // force b to be mapped into B
})
@PassingTest
@UseAnalyses({PluralAnalysis.SYNTAX, PluralAnalysis.EFFECT, PluralAnalysis.PLURAL})
public class FieldAccessControl {

	private Object a;
	private int b;
	
	@Perm(ensures = "unique(this!fr)")
	public FieldAccessControl() {
		this.a = new Object();
	}
	
	@Perm(requires = "#0 != null")
	@Full(value = "A", use = Use.FIELDS)
	public void setA(Object a) {
		this.a = a;
	}
	
	@NoEffects // pure method--not necessary for checking
	@Perm(ensures = "result != null")
	@Pure(value = "A", use = Use.FIELDS)
	public Object getA() {
		return a;
	}
	
	@Full(value = "B", use = Use.FIELDS)
	public void setB(int b) {
		this.b = b;
	}
	
	@NoEffects // pure method--not necessary for checking
	@Pure(value = "B", use = Use.FIELDS)
	public int getB() {
		return b;
	}
	
	@Fulls({ 
		@Full(value = "A", use = Use.FIELDS), 
		@Full(value = "B", use = Use.FIELDS) 
	})
	@Perm(requires = "#0 != null")
	public void setAandB(Object a, int b) {
		this.a = a;
		// Plural infers that it needs to switch from using 
		// permission for A to permission for B
		this.b = b;
	}
	
	/**
	 * This class shows a hypothetical client and how it could
	 * access the fields of {@link FieldAccessControl} through
	 * its own separate dimensions.
	 * Plural does not infer the necessary packing and unpacking
	 * completely if multiple dimensions of the field are accessed
	 * through different receiver permissions
	 * ({@link #setAandBWithSeparatePermissions()}), while
	 * it works fine when one combined receiver permission is used
	 * ({@link #setAandBThroughCombinedPermission()}).
	 * {@link FieldAccessControl#setAandB(Object, int) shows that
	 * Plural can also infer direct field accesses through multiple
	 * permissions; problems only arise with <b>calls</b> on fields
	 * made through different permissions.
	 * @author Kevin Bierhoff
	 * @since Aug 15, 2009
	 */
	@Refine({
		@States(dim = "cA", value = { "hasA", "calledA" }),
		@States(dim = "cB", value = { "hasB", "calledB" })
	})
	@ClassStates({
		@State(name = "hasA", inv = "full(control, A)"),
		@State(name = "hasB", inv = "full(control, B)")
	})
	public static class FieldAccessClient {
		
		private FieldAccessControl control;
		
		@Perm(ensures = "unique(this!fr)")
		FieldAccessClient() {
			control = new FieldAccessControl();
		}
		
		@Full(requires = {"hasA", "hasB"}, ensures = {"hasA", "hasB"}, use = Use.FIELDS) 
		public void setAandBThroughCombinedPermission() {
			control.setA(new Object());
			control.setB(5);
		}
		
		@Fulls({ 
			@Full(value = "cA", requires = "hasA", ensures = "hasA", use = Use.FIELDS), 
			@Full(value = "cB", requires = "hasB", ensures = "hasB", use = Use.FIELDS) 
		})
		public void setAandBWithSeparatePermissions() {
			Object a = new Object();
			int b = 5;
			control.setA(a);
			// object is in calledA, and separately we have full(control, A)
			// calling forcePack, which requires hasA, forces Plural to unpack
			// to cA again and put the field permission back in, packing to hasA
			// ideally, we would like Plural to recognize this automatically
			forcePack();
			control.setB(b);
			// object is in calledB, and separately we have full(control, B)
			// because of the post-condition, Plural unpacks automatically
			// and re-establishes hasB, so we don't need the forcePack trick here
		}
		
		@Pure(value = "cA", requires = "hasA", ensures = "hasA", use = Use.FIELDS)
		private void forcePack() {}
		
	}
	
}
