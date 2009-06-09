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

package edu.cmu.cs.plural.contexts;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.plural.errors.ChoiceID;
import edu.cmu.cs.plural.linear.DisjunctiveVisitor;

/**
 * A context representing the value False, or <b>0</b> which is
 * printed VOID.
 * 
 * @author Nels E. Beckman
 * @since May 28, 2009
 *
 */
public class FalseContext implements LinearContext {

	private final ChoiceID parentChoiceID;
	private final ChoiceID choiceID;
	
	public FalseContext(ChoiceID parentChoiceID) {
		this.parentChoiceID = parentChoiceID;
		this.choiceID = new ChoiceID();
	}
	
	public FalseContext(ChoiceID parentChoiceID, ChoiceID choiceID) {
		this.parentChoiceID = parentChoiceID;
		this.choiceID = choiceID;
	}
	
	@Override
	public <T> T dispatch(DisjunctiveVisitor<T> visitor) {
		return visitor.falseContext(this);
	}

	/**
	 * Keeps only the least precise elements in the disjunction, since all have to be satisfied.
	 * @tag artifact.explanation -id="676302" : this is a near code-clone of ContextChoiceLE.compact
	 * @see LinearContext#compact(ASTNode, boolean)
	 */
	@Override
	public LinearContext compact(ASTNode node, boolean freeze) {
		return this;
	}

	//
	// LatticeElement methods
	//

	@Override
	public boolean atLeastAsPrecise(LinearContext other, final ASTNode node) {
		this.freeze();
		if(this == other)
			return true;
		other.freeze();
		
		return true;
	}

	@Override
	public boolean atLeastAsPrecise(TensorPluralTupleLE other, ASTNode node) {
		return true;
	}

	/*
	 * Overriden from Object
	 */

	@Override
	public String toString() {
		return "VOID";
	}

	@Override
	public LinearContext join(LinearContext other, ASTNode node) {
		this.freeze();
		if(other == this)
			return this;
		other.freeze();
		
		return this;
	}

	@Override
	public LinearContext copy() {
		return new FalseContext(this.parentChoiceID, this.choiceID);
	}

	@Override
	public LinearContext freeze() {
		return this;
	}

	@Override
	public LinearContext mutableCopy() {
		return new FalseContext(this.parentChoiceID, this.choiceID);
	}

	@Override
	public ChoiceID getChoiceID() {
		return this.choiceID;
	}

	@Override
	public ChoiceID getParentChoiceID() {
		return this.parentChoiceID;
	}

	@Override
	public String getHumanReadablePerms() {
		return this.toString();
	}
}