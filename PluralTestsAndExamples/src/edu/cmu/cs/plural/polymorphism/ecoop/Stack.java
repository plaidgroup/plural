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
package edu.cmu.cs.plural.polymorphism.ecoop;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Apply;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.PolyVar;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.ResultApply;
import edu.cmu.cs.plural.annot.ResultPolyVar;
import edu.cmu.cs.plural.annot.ResultUnique;
import edu.cmu.cs.plural.annot.Similar;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Unique;
import edu.cmu.cs.plural.annot.Use;

/**
 * The Stack class, originally presented in Figure 1. 
 * The Node class is renamed to StackNode and not a member class 
 * because the Crystal
 * state analysis framework does not support member classes. 
 * For legacy reasons, the @Invariants annotation in the paper
 * is actually @ClassStates.
 * 
 * @author Nels E. Beckman
 * @since Dec 10, 2009
 *
 * @param <T> Type of element held by this stack.
 */
//For automated testing.
@PassingTest
@UseAnalyses({PluralAnalysis.PLURAL, PluralAnalysis.SYNTAX,
	          PluralAnalysis.EFFECT, "PolyInternalChecker"})
//Invariants & polyrmophic permission
@Similar("p")
@ClassStates(@State(name="alive", inv="unique(first) in BOTH"))
public final class Stack<T> {
	@Apply("p") private StackNode<T> first;
	
	@Perm(ensures="unique(this!fr)")
	public Stack() {
		first = null;
	}
	
	@Unique(use=Use.FIELDS)
	public void push(@PolyVar(value="p", returned=false) T item) {
		@Apply("p") StackNode<T> new_first = new StackNode<T>(first, item);
		first = new_first;
	}
	
	@Unique(use=Use.FIELDS)
	@ResultPolyVar("p")
	public T pop() {
		if( first == null ) return null;
		else {
			T result = first.getItem();
			first = first.getNext();
			return result;
		}
	}
}

/**
 * Plural does not allow reading of fields from other variables
 * besides the method receiver, therefore this implementation has
 * to be a little different from what is in the paper. It requires
 * both a getItem() and getNext() method. But on top of that, because
 * these methods will be called one at a time, we need states to
 * ensure that those permissions persist. Those states are NEXT and
 * BOTH.
 */
@Similar("p")
@Refine(
  {@States(value={"NEXT"}),
   @States(refined="NEXT", value={"BOTH"})}
)
@ClassStates(
	{@State(name="NEXT", inv="unique(next) in BOTH"),
	 @State(name="BOTH", inv="p(item)")
})
class StackNode<T> {
	@Apply("p") private StackNode<T> next;
	private T item;

	@Perm(ensures="unique(this!fr) in BOTH")
	StackNode(@Unique(returned=false,requires="BOTH") @Apply("p") StackNode<T> next, 
			@PolyVar(value="p", returned=false) T item) {
		this.next = next;
		this.item = item;
	}

	@Unique(requires="NEXT",use=Use.FIELDS)
	@ResultUnique(ensures="BOTH")
	@ResultApply("p")
	public StackNode<T> getNext() {
		return next;
	}

	@Unique(requires="BOTH",ensures="NEXT", use=Use.FIELDS)
	@ResultPolyVar("p")
	public T getItem() {
		return item;
	}
}
