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
package edu.cmu.cs.plural.states;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * @author Kevin
 * 
 */
public interface StateSpace {

	public static final String STATE_ALIVE = "alive";

	public static final StateSpace SPACE_TOP = new StupidStateSpace();

	public String getRootState();

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
	public boolean firstBiggerThanSecond(String node1, String node2);

	/**
	 * This method is meant to be used for comparing state information
	 * (rather than permission root nodes) and determines whether the 
	 * first node implies the second node.
	 * Any node implies itself and any bigger nodes.  In addition,
	 * one dimension of a state implies any other dimension of that
	 * state and bigger states.  Thus, this method is less
	 * restrictive than {@link #firstBiggerThanSecond(String, String)}.
	 *     
	 * @param known
	 * @param unknown
	 * @return <code>true</code> iff the first node implies
	 * the second node.
	 */
	public boolean firstImpliesSecond(String known, String unknown);

	/**
	 * Returns an iterator to walk <b>up</b> the state space from the
	 * given start node to the state space's root node.
	 * @param startNode
	 * @return an iterator to walk <b>up</b> the state space.
	 */
	public Iterator<String> stateIterator(String startNode);

	/**
	 * Indicates whether a given node is a state or a dimension.
	 * @param n
	 * @return <code>true</code> if the given node is a dimension and 
	 * <code>false</code> if the node is a state.
	 */
	public boolean isDimension(String n);

	/**
	 * Returns the set of dimensions defined for a given state.
	 * This set may be empty if the state is directly refined with states.
	 * Most of the time, though, states should have dimensions.
	 * @param state This must be a state, not a dimension.
	 * @return the set of dimensions defined for a given state.
	 */
	public Set<String> getDimensions(String state);

	/**
	 * @return Returns the set of all nodes in this state space.
	 */
	public Set<String> getAllNodes();
	
	/**
	 * @return Returns the set of nodes that are direct child nodes
	 * of the given parent. If the parent is unknown, this method will
	 * return the empty set. Note that this method will only return
	 * <i>known</i> subnodes.
	 */
	public Set<String> getChildNodes(String parent);
	
	/**
	 * @return Returns a set of all of the states in this state space, but
	 * not dimensions.
	 */
	public Set<String> getAllStates();
	
	/**
	 * Return the lowest node in the state hierarchy that completely covers
	 * all references to the given field.
	 * @param resolveFieldBinding
	 * @return
	 */
	public String getFieldRootNode(IVariableBinding field);

	/**
	 * Indicates whether the two given nodes are defined in orthogonal
	 * state dimensions.
	 * @param node1
	 * @param node2
	 * @return <code>true</code> if the two nodes are defined in orthogonal
	 * state dimensions, <code>false</code> otherwise.
	 */
	public boolean areOrthogonal(String node1, String node2);

	/**
	 * Indicates whether the given node is a marker node, i.e., cannot be
	 * forgotten once established.
	 * @param node
	 * @return <code>true</code> if the given node is a marker node, 
	 * <code>false</code> otherwise.
	 */
	public boolean isMarker(String node);
}
