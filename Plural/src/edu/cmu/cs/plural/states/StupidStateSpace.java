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
package edu.cmu.cs.plural.states;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * @author Kevin Bierhoff
 *
 */
public class StupidStateSpace implements StateSpace {

	public String getRootState() {
		return STATE_ALIVE;
	}

	/**
	 * The contract for
	 * <code>StateSpace.firstBiggerThanSecond(first, second)</code> is to
	 * return true iff first "contains" second, i.e. second is a substate of
	 * first in the state hierarchy. StateSpace is supposed to represent that
	 * type hierarchy. For example, for a hierarchy where [open, closed] refine
	 * alive and [within, eof] refine open, firstBiggerThanSecond(alive,
	 * open)==true, firstBiggerThanSecond(alive, eof)==true,
	 * firstBiggerThanSecond(closed, closed)==true, firstBiggerThanSecond(open,
	 * closed)==false, firstBiggerThanSecond(within, open)==false. Thus you're
	 * essentially asking, is second at least as precise a state as first.<br>
	 * 
	 * Thus, to find out whether some state s is "inside" a permission p, you
	 * want to compare s to p's "root node", let's call it t, with
	 * firstBiggerThanSecond(t, s).<br>
	 * 
	 * <code>firstBiggerThanSecond</code> (and the <code>StateSpace</code>
	 * class as a whole) is really simple right now: It just treats any state s !=
	 * alive as immediate substate of alive. That's why it's doing this funny
	 * comparison. In a sense this is the approach that your formal system for
	 * the ECOOP paper takes, I think. StateSpace doesn't even know what the set
	 * of allowed states is; thus if you pass in some "bogus" state, say, due to
	 * a typo, that will still be a substate of alive and of itself.<br>
	 * 
	 * I'm planning on fixing this mess when implementing support for state
	 * dimensions, because then StateSpace has to really know the exact state
	 * hierarchy. But (hopefully) the contract for firstBiggerThanSecond
	 * remains.
	 */
	public boolean firstBiggerThanSecond(String node1, String node2) {
		if (node1.equals(node2))
			return true;
		if (node1.equals(getRootState()))
			return true;
		return false;
	}

	@Override
	public boolean firstImpliesSecond(String known, String unknown) {
		return firstBiggerThanSecond(unknown, known);
	}

	@Override
	public Set<String> getDimensions(String state) {
		return Collections.emptySet();
	}

	@Override
	public boolean isDimension(String n) {
		return false;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof StateSpace)
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		return STATE_ALIVE.hashCode();
	}

	public Iterator<String> stateIterator(String rootNode) {
		return new StateIterator(rootNode);
	}

	private class StateIterator implements Iterator<String> {

		public StateIterator(String startNode) {
			next = startNode;
		}

		private String next;

		public boolean hasNext() {
			return next != null;
		}

		public String next() {
			if (next == null)
				throw new NoSuchElementException(
						"Iteration past state space root");
			String result = next;
			if (next.equals(StupidStateSpace.this.getRootState()))
				next = null;
			else
				next = StupidStateSpace.this.getRootState();
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException("Not possible");
		}

	}

	@Override
	public String getFieldRootNode(IVariableBinding field) {
		return this.getRootState();
	}

	@Override
	public boolean areOrthogonal(String node1, String node2) {
		return false;
	}

	@Override
	public Set<String> getAllNodes() {
		return Collections.singleton(this.getRootState());
	}

	@Override
	public Set<String> getAllStates() {
		return Collections.singleton(this.getRootState());
	}

	@Override
	public boolean isMarker(String node) {
		return node.equals(getRootState());
	}
}
