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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * This class is a state space implementation for classes and interfaces. Its
 * fields can be populated using several package-private methods.
 * 
 * @author Kevin Bierhoff
 * 
 */
public class StateSpaceImpl implements StateSpace {

	/**
	 * If this flag is set to true, this class will "tolerate" calls to
	 * {@link StateSpace} methods with unknown states. States are considered
	 * "known" iff they are keys in the {@link #parents} map (this includes the
	 * root state, which is mapped to <code>null</code>). Unknown states will
	 * be treated as direct subnodes of the root state.
	 */
	private static final boolean TOLERATE_UNKNOWN_STATES = true;

	/**
	 * Maps nodes to its direct subnodes, i.e. states to their refining
	 * dimensions and dimensions to their state sets.
	 */
	private final Map<String, Set<String>> subnodes;

	/**
	 * Maps nodes to their parents; the root node must be mapped to
	 * <code>null</code>.
	 */
	private final Map<String, String> parents;

	/** Map of all states mapped to whether they are "marker" states or not. */
	private final Map<String, Boolean> statesMarked;

	/**
	 * Unique name of this state space, e.g., the fully qualified name of the
	 * represented type.
	 */
	private String identifier;

	/**
	 * Used to generate unique names for anonymous state dimensions (together
	 * with {@link #identifier}.
	 */
	private int anonCount = 0;

	/**
	 * Maps field names to the node in the state hierarchy that the field is
	 * mapped to.
	 */
	private Map<String, String> fieldMap;

	/**
	 * Creates a new state space with a given unique identifier. The identifier
	 * is used to uniquely name anonymous state dimensions.
	 * 
	 * @param identifier
	 *            Unique name of this state space, e.g., the fully qualified
	 *            name of the represented type.
	 */
	StateSpaceImpl(String identifier) {
		this.identifier = identifier;

		// initialize internal maps
		subnodes = new HashMap<String, Set<String>>();
		parents = new HashMap<String, String>();
		statesMarked = new HashMap<String, Boolean>();
		fieldMap = Collections.emptyMap();

		// make root state known
		parents.put(getRootState(), null);
		statesMarked.put(getRootState(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.plural.states.StateSpace#firstBiggerThanSecond(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public boolean firstBiggerThanSecond(String node1, String node2) {
		assertKnown(node1);
		String node = assertKnown(node2);
		while (node != null) {
			if (node.equals(node1))
				return true;
			node = getParent(node);
		}
		return false;
	}

	@Override
	public boolean firstImpliesSecond(String known, String unknown) {
		if(known != null && isDimension(known))
			known = getParent(known);
		while(unknown != null && isDimension(unknown))
			unknown = getParent(unknown);
		return firstBiggerThanSecond(unknown, known);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.plural.states.StateSpace#getRootState()
	 */
	@Override
	public String getRootState() {
		return StateSpace.STATE_ALIVE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.plural.states.StateSpace#stateIterator(java.lang.String)
	 */
	@Override
	public Iterator<String> stateIterator(String startNode) {
		return new StateIterator(assertKnown(startNode));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.plural.states.StateSpace#getDimensions(java.lang.String)
	 */
	@Override
	public Set<String> getDimensions(String n) {
		if (isDimension(n) == false) {
			Set<String> result = subnodes.get(assertKnown(n));
			if (result == null)
				// this should only happen for leaf states or "alive", if
				// unknown states are tolerated
				return Collections.emptySet();
			else
				return result;
		}
		throw new IllegalArgumentException("Not a state: " + n);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.plural.states.StateSpace#isDimension(java.lang.String)
	 */
	@Override
	public boolean isDimension(String n) {
		// n must be known and not a known state in order to be a dimension
		return isKnown(n) && (statesMarked.containsKey(n) == false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.plural.states.StateSpace#getFieldRootNode(org.eclipse.jdt.core.dom.IVariableBinding)
	 */
	@Override
	public String getFieldRootNode(IVariableBinding field) {
		// TODO take declaring class into account in case of shadowed superclass fields
		String fieldName = field.getName();
		String result = fieldMap.get(fieldName);
		return result; // returns null if field is not mapped, as desired
//		if (result != null)
//			return result;
//		return getRootState();
	}

	/**
	 * Indicates whether the given field is mapped to a node
	 * (usually per some invariant declaration)
	 * @param field
	 * @return
	 */
	public boolean isMapped(IVariableBinding field) {
		// TODO take declaring class into account in case of shadowed superclass fields
		return fieldMap.containsKey(field.getName());
	}

	/**
	 * Returns the immediate parent node of the given node.
	 * 
	 * @param node
	 * @return the immediate parent node of the given node or <code>null</code>
	 *         if the given node is the root node.
	 */
	public String getParent(String node) {
		if (TOLERATE_UNKNOWN_STATES) {
			if (parents.containsKey(node))
				return parents.get(node);
			return getRootState();
		}
		return parents.get(assertKnown(node));
	}

	/**
	 * Indicates whether the given node is known in this state space. Use
	 * {@link #assertKnown(String)} to fail if a node is unknown.
	 * 
	 * @param n
	 * @return <code>True</code> if the given node is known,
	 *         <code>false</code> otherwise.
	 */
	public boolean isKnown(String n) {
		return parents.containsKey(n);
	}

	/**
	 * Makes sure that the given node is known.
	 * 
	 * @param node
	 * @return The given node, if it's known.
	 * @throws IllegalArgumentException
	 *             If the given node is not known.
	 */
	private String assertKnown(String node) {
		if (TOLERATE_UNKNOWN_STATES || parents.containsKey(node))
			return node;
		throw new IllegalArgumentException("Unknown node: " + node);
	}

	/**
	 * Returns the "most precise" node that is a parent of both of the given
	 * nodes, i.e., no nodes refining the returned node are a parent of both of
	 * the given nodes.
	 * 
	 * @param state_1
	 * @param state_2
	 * @return Returns the "most precise" node that is a parent of both of the
	 *         given nodes.
	 */
	public String getLowestCommonParent(String state_1, String state_2) {

		String cur_node = state_1;
		while (!this.firstBiggerThanSecond(cur_node, state_2)
				&& cur_node != null) {
			cur_node = this.getParent(cur_node);
		}

		if (cur_node == null) {
			/*
			 * Should not occur...
			 */
			return this.getRootState();
		} else {
			return cur_node;
		}
	}

	/**
	 * Add a dimension to this state space with a made-up name. This method
	 * generates a unique name for the dimension and then calls
	 * {@link #addNamedDimension(String, String[], String, boolean)}.
	 * 
	 * @param states
	 * @param refined
	 * @param marker
	 * @return Set of problems found while adding the dimension (for
	 *         error-reporting purposes).
	 */
	Set<String> addAnonymousDimension(String[] states, String refined,
			boolean marker) {
		String dimName = identifier + "#anon_" + (anonCount++);
		return addNamedDimension(dimName, states, refined, marker);
	}

	/**
	 * Add a dimension to this state space with a given name.
	 * 
	 * @param dimName
	 * @param states
	 * @param refined
	 * @param marker
	 * @return Set of problems found while adding the dimension (for
	 *         error-reporting purposes).
	 */
	Set<String> addNamedDimension(String dimName, String[] states,
			String refined, boolean marker) {
		Set<String> problems = new LinkedHashSet<String>();
		
		// You can't refine yourself...
		if( refined.equals(dimName) ) {
			problems.add("Dimension " + dimName + " is attempting to refine itself!");
			return problems;
		}
		
		// find guaranteed-unique name for dimension
		if (parents.containsKey(dimName)) {
			problems.add("Dimension already defined: " + dimName);
			// recover by finding a new dimension name
			int i = 0;
			while (parents.containsKey(dimName + i))
				i++;
			dimName = dimName + i;
		}

		// construct state set and make dimension the parent of all states
		LinkedHashSet<String> stateSet = new LinkedHashSet<String>(
				states.length);
		for (String s : states) {
			if (stateSet.add(s)) {
				if (parents.put(s, dimName) == null)
					statesMarked.put(s, marker);
				else
					problems.add("State already defined: " + s);
			} else
				problems.add("Repeated state in refinement: " + s);
		}
		// insert states for dimension
		// state set cannot be extended later, so make it unmodifiable
		subnodes.put(dimName, Collections.unmodifiableSet(stateSet));

		// make refined state parent of new dimension
		parents.put(dimName, refined);
		// add dimension to refined state's set
		Set<String> dims = subnodes.get(refined);
		if (dims == null) {
			dims = new LinkedHashSet<String>();
			// more dimensions can be added, so this set remains modifiable
			subnodes.put(refined, dims);
		}
		dims.add(dimName);

		return problems;
	}

	/**
	 * This is a error recovery method that should be used sparingly.
	 * Makes the given state a direct child of the {@link #getRootState() root node},
	 * simulating a state that is not explicitly declared.
	 * If the given state already exists, this call has no effect.
	 * @param state
	 */
	void addAnonymousState(String state) {
		// make known
		if(parents.containsKey(state)) {
			// already defined
			return; // ignore--this is a error recovery anyway
		}
		parents.put(state, getRootState());
		// make a state
		if(statesMarked.containsKey(state)) {
			// already a state (should be impossible)
			return;
		}
		statesMarked.put(state, false /* conservative */);
	}

	/**
	 * Sets the field map.
	 * 
	 * @param fieldMap
	 *            The new field map.
	 */
	void setFieldMap(Map<String, String> fieldMap) {
		this.fieldMap = Collections.unmodifiableMap(fieldMap);
	}

	@Override
	public String toString() {
		return "Space:" + identifier;
	}

	/**
	 * This class is used by {@link StateSpaceImpl#stateIterator(String)}. It
	 * walks "up" the state hierarchy from an initial node to the root.
	 * 
	 * @author Kevin Bierhoff
	 */
	private class StateIterator implements Iterator<String> {

		/**
		 * Node to be returned by the next call to {@link #next()}.
		 * <code>null</code> if the iterator has reached the root already
		 * (ending the iteration).
		 */
		private String next;

		public StateIterator(String startNode) {
			this.next = startNode;
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public String next() {
			if (next == null)
				throw new NoSuchElementException(
						"Iteration past state space root");
			String result = next;
			next = StateSpaceImpl.this.getParent(next);
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"Cannot remove nodes from state spaces");
		}

	}

	@Override
	public boolean areOrthogonal(String node1, String node2) {
		if(node1.equals(node2))
			// nodes are not orthogonal to themselves --> anti-reflexive relation
			return false;
		String p1 = getParent(node1);
		String p2 = getParent(node2);
		if(p1 == null || p2 == null)
			// node1 or node2 was alive --> not orthogonal to any node
			return false;
		if(!isKnown(node1) && isKnown(node2))
			// known nodes (except alive) and unknown nodes are orthogonal
			return true;
		if(isKnown(node1) && !isKnown(node2))
			// known nodes (except alive) and unknown nodes are orthogonal
			return true;
		if(p1.equals(p2))
			// same parent --> node1 and node2 must be dimensions of that parent
			return isDimension(node1) && isDimension(node2);
		return areOrthogonal(node1, p2) || areOrthogonal(p1, node2) || areOrthogonal(p1, p2);
	}

	@Override
	public Set<String> getAllNodes() {
		return Collections.unmodifiableSet(new HashSet<String>(parents.keySet()));
	}

	@Override
	public Set<String> getAllStates() {
		return Collections.unmodifiableSet(new HashSet<String>(statesMarked.keySet()));
	}

	@Override
	public boolean isMarker(String node) {
		Boolean result = statesMarked.get(assertKnown(node));
		if(result != null)
			return result; // declared state
		// otherwise, node is a dimension or undeclared state
		while(node != null && isDimension(node))
			node = getParent(node);
		if(node != null)
			result = statesMarked.get(assertKnown(node));
		if(result != null)
			// declared dimension
			return result;
		else
			// undeclared states
			return false;
	}

	@Override
	public Set<String> getChildNodes(String parent) {
		if( subnodes.containsKey(parent) )
			return Collections.unmodifiableSet(subnodes.get(parent));
		else
			return Collections.emptySet();
	}

	@Override
	public String findLeastCommonAncestor(Set<String> states) {
		// Well if the root state's already inside...
		if( states.contains(this.getRootState()) )
			return this.getRootState();

		// As specified in contract...
		if( states.isEmpty() )
			return this.getRootState();
		
		// But otherwise, go through the states one at a time
		// and see if it's greater than the current least...
		Iterator<String> iter = states.iterator();
		String least = iter.next();
		
		while( iter.hasNext() ) {
			String cur_state = iter.next();
			least = this.getLowestCommonParent(cur_state, least);
		}
		return least;
	}
}
