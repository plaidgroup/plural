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
package edu.cmu.cs.plural.contexts;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.bridge.LatticeElement;
import edu.cmu.cs.crystal.util.Copyable;
import edu.cmu.cs.crystal.util.Freezable;
import edu.cmu.cs.plural.errors.ChoiceID;
import edu.cmu.cs.plural.errors.JoiningChoices;
import edu.cmu.cs.plural.linear.DisjunctiveVisitor;

/**
 * This interface represents the abstract super-type of classes used for
 * encoding linear logic proof search.
 * @author Kevin Bierhoff
 * @since 4/16/2008
 */
public interface LinearContext extends /*LatticeElement<LinearContext>,*/ Copyable<LinearContext>, Freezable<LinearContext>{
	
	/**
	 * Double-dispatch on the given visitor.  Concrete implementations
	 * of this method will call one of the methods in the given visitor
	 * and return the result.
	 * @param <T>
	 * @param visitor
	 * @return The result of the visitor method being invoked.
	 */
	public <T> T dispatch(DisjunctiveVisitor<T> visitor);
	
	/**
	 * Throw out redundant content and return the resulting context, which
	 * may or may not be the given one.
	 * @param node Node on which the method is called, or <code>null</code>, 
	 * for calls to {@link #atLeastAsPrecise(LinearContext, ASTNode)}.
	 * @param freeze <code>true</code> freezes the resulting context.
	 * @return the resulting context, which
	 * may or may not be the given one.
	 */
	public LinearContext compact(ASTNode node, boolean freeze);

	/**
	 * Helper method to compare the receiver with a given individual tuple.  
	 * This method is used by {@link #atLeastAsPrecise(LinearContext, ASTNode)}.
	 * It is intended to break down con- and disjuncts in the receiver until
	 * individual tuples can compared with <code>other</other>.
	 * @param other The tuple to compare with
	 * @param node Node on which the comparison occurs, or <code>null</code>
	 * if not available.
	 * @see LatticeElement#atLeastAsPrecise(LatticeElement, ASTNode)
	 */
	public boolean atLeastAsPrecise(TensorPluralTupleLE other, ASTNode node);

	/**
	 * Joins two lattices.
	 * @param jc Use to determine whether or not we are joining multiple choices.
	 */
	public LinearContext join(LinearContext other, ASTNode node, JoiningChoices jc);
	
	/**
	 * Checks to see if this linear context is at least as precise
	 * as the other one.
	 */
	public boolean atLeastAsPrecise(LinearContext other, ASTNode node);	
	
	/**
	 * What is this choice ID of this object? In other words, assuming that
	 * this context is one branch of a choice, what is the unique id of that
	 * choice? New ids are only introduced at the introduction of choices
	 * (packing, unpacking, call to a method w/ multiple cases, join of a
	 * choice). Otherwise, consecutive contexts will have the same ID.
	 */
	public ChoiceID getChoiceID();
	
	/**
	 * The id of the lattice that this lattice was created from as part
	 * of a choice introduction (packing, unpacking, call to method w/
	 * multiple cases, join of a choice).
	 */
	public ChoiceID getParentChoiceID();

	/**
	 * Returns a human-readable display of the permissions held by this
	 * linear context.
	 */
	public String getHumanReadablePerms();
}