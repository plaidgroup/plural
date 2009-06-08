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

package edu.cmu.cs.plural.errors.history;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.plural.errors.ChoiceID;

/**
 * A tree of the choices that occurred when analyzing a method. Used
 * for the {@link HistoryView}. This allows us to see which choices
 * occurred over time, and which ones failed, and which ones didn't.
 * This tree is admittedly a little strange in that it does not know
 * its root.
 * 
 * @author Nels E. Beckman
 * @since Jun 2, 2009
 *
 */
public class SingleCaseHistoryTree {

	/** A map from nodes to their parents. */
	private final Map<HistoryNode, HistoryNode> parentMap;
	
	/** A map from nodes to their children, of which there may be many. */
	private final Map<HistoryNode, Set<HistoryNode>> childMap;
	
	private SingleCaseHistoryTree(Map<HistoryNode, HistoryNode> parentMap,
			Map<HistoryNode, Set<HistoryNode>> childMap) {
		this.parentMap = parentMap;
		this.childMap = childMap;
	}
	
	/**
	 * Given a mapping from IDs of choice branches to the choice branches themselves,
	 * builds a HistoryTree.
	 */
	public static SingleCaseHistoryTree buildTree(IDHistoryNodeMap idToNodeMap) {
		// Basically to do this we want to go up first, and then down...
		Map<HistoryNode, HistoryNode> parentMap =
			extractParentMap(idToNodeMap);
		
		Map<HistoryNode, Set<HistoryNode>> childMap =
			extractChildMap(parentMap);
		
		return new SingleCaseHistoryTree(parentMap, childMap);
	}

	/**
	 * Create a map from nodes to their children, where each node may potentially
	 * have many children.
	 */
	private static Map<HistoryNode, Set<HistoryNode>> extractChildMap(
			Map<HistoryNode, HistoryNode> parentMap) {
		Map<HistoryNode, Set<HistoryNode>> result = 
			new HashMap<HistoryNode, Set<HistoryNode>>();
		
		for( Map.Entry<HistoryNode, HistoryNode> entry : parentMap.entrySet() ) {
			HistoryNode child = entry.getKey();
			HistoryNode parent = entry.getValue();
			
			if( result.containsKey(parent) ) {
				result.get(parent).add(child);
			}
			else {
				Set<HistoryNode> children = new HashSet<HistoryNode>();
				children.add(child);
				result.put(parent, children);
			}
		}
		return result;
	}

	/**
	 * From an id to node map, create a child to parent map.
	 */
	private static Map<HistoryNode, HistoryNode> extractParentMap(
			IDHistoryNodeMap idToNodeMap) {
		Map<HistoryNode, HistoryNode> result = 
			new LinkedHashMap<HistoryNode, HistoryNode>();
		// Iterate through map, and for each one, put it and 
		// its parent in the map.
		for( Map.Entry<ChoiceID, HistoryNode> node : idToNodeMap.entrySet() ) {
			HistoryNode cur_ctx = node.getValue();
			ChoiceID parent_id = cur_ctx.getParentChoiceID();
			HistoryNode parent_ctx = idToNodeMap.get(parent_id);
			
			result.put(cur_ctx, parent_ctx);
		}
		return result;
	}

	/**
	 * Return the number of children for the given node.
	 */
	public int numChildren(HistoryNode node) {
		if( this.childMap.containsKey(node) )		
			return this.childMap.get(node).size();
		else if( this.parentMap.containsKey(node) )
			return 0;
		else
			throw new IllegalArgumentException("Given a node not described by this tree.");
	}

	/**
	 * Is the given node a part of this tree?
	 */
	public boolean contains(HistoryNode node) {
		return this.parentMap.containsKey(node) ||
			this.childMap.containsKey(node);
	}

	/**
	 * Returns the children of the given history node.
	 */
	public Set<HistoryNode> getChildren(HistoryNode node) {
		if( this.childMap.containsKey(node) )		
			return this.childMap.get(node);
		else if( this.parentMap.containsKey(node) )
			return Collections.emptySet();
		else
			throw new IllegalArgumentException("Given a node not described by this tree.");		
	}

}