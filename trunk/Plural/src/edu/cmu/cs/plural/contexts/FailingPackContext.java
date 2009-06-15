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

import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.errors.ChoiceID;
import edu.cmu.cs.plural.errors.JoiningChoices;

/**
 * A context that is the result of a failed attempt to pack!
 * It is exactly like a {@link TrueContext} (i.e., 1) except
 * that it also contains information about what state we tried
 * to pack to and which invariant could not be satisfied. 
 * 
 * @author Nels E. Beckman
 * @since May 22, 2009
 *
 */
public class FailingPackContext extends TrueContext {

	private final String packingState;
	private final String invariant;
	
	public FailingPackContext(String packingState, 
			String invariant, ChoiceID parentChoiceID, ChoiceID choiceID) {
		super(parentChoiceID, choiceID);
		this.packingState = packingState;
		this.invariant = invariant;
	}

	/**
	 * @return The state that the analysis attempted to pack to when it
	 * failed to pack.
	 */
	public String failingState() {
		return packingState;
	}
	
	/**
	 * @return The invariant that the analysis could not satisfy when it
	 * failed to pack.
	 */
	public String failingInvariant() {
		return invariant;
	}
	
	@Override
	public LinearContext copy() {
		return this;
	}

	@Override
	public LinearContext freeze() {
		return this;
	}

	@Override
	public LinearContext join(LinearContext other, ASTNode node, JoiningChoices jc) {
		if( other instanceof FailingPackContext ) {
			if( !this.equals(other) )
				return Utilities.nyi("I wasn't ready for this.");
			else 
				return this;
		}
		else {
			return super.join(other, node, jc);
		}
	}

	@Override
	public LinearContext mutableCopy() {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((invariant == null) ? 0 : invariant.hashCode());
		result = prime * result
				+ ((packingState == null) ? 0 : packingState.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FailingPackContext other = (FailingPackContext) obj;
		if (invariant == null) {
			if (other.invariant != null)
				return false;
		} else if (!invariant.equals(other.invariant))
			return false;
		if (packingState == null) {
			if (other.packingState != null)
				return false;
		} else if (!packingState.equals(other.packingState))
			return false;
		return true;
	}	
}