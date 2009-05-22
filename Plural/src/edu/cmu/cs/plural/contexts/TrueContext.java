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

import edu.cmu.cs.plural.linear.DisjunctiveVisitor;

/**
 * A context that corresponds to true, or 1 in the linear logic.
 * This concept used to be implemented as an empty 
 * {@link ContextChoiceLE}.
 * 
 * @author Nels E. Beckman
 * @since May 22, 2009
 *
 */
public class TrueContext implements DisjunctiveLE {

	@Override
	public boolean atLeastAsPrecise(TensorPluralTupleLE other, ASTNode node) {
		// Always false, this this contains no information.
		return false;
	}

	@Override
	public DisjunctiveLE compact(ASTNode node, boolean freeze) {
		return freeze ? this.freeze() : this.mutableCopy();
	}

	@Override
	public <T> T dispatch(DisjunctiveVisitor<T> visitor) {
		return visitor.trueContext(this);
	}

	@Override
	public boolean atLeastAsPrecise(DisjunctiveLE other, ASTNode node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DisjunctiveLE copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DisjunctiveLE join(DisjunctiveLE other, ASTNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DisjunctiveLE freeze() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DisjunctiveLE mutableCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
