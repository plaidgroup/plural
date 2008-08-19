/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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
package edu.cmu.cs.plural.track;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.flow.LatticeElement;

/**
 * @author Kevin Bierhoff
 *
 */
public class MutablePairLE<P extends LatticeElement<P>, Q extends LatticeElement<Q>> implements LatticeElement<MutablePairLE<P, Q>> {
	
	private P element1;
	private Q element2;

	public MutablePairLE(P element1, Q element2) {
		this.element1 = element1;
		this.element2 = element2;
	}

	public P getElement1() {
		return element1;
	}

	public void setElement1(P element1) {
		this.element1 = element1;
	}

	public Q getElement2() {
		return element2;
	}

	public void setElement2(Q element2) {
		this.element2 = element2;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	public boolean atLeastAsPrecise(MutablePairLE<P, Q> other, ASTNode node) {
		return this.element1.atLeastAsPrecise(other.element1, node)
			&& this.element2.atLeastAsPrecise(other.element2, node);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#copy()
	 */
	public MutablePairLE<P, Q> copy() {
		return new MutablePairLE<P, Q>(element1.copy(), element2.copy());
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#join(edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	public MutablePairLE<P, Q> join(MutablePairLE<P, Q> other, ASTNode node) {
		return new MutablePairLE<P, Q>(
				this.element1.join(other.element1, node),
				this.element2.join(other.element2, node));
	}

}
