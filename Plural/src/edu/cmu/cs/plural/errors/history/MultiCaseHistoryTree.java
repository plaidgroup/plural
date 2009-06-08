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

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.contexts.LinearContext;

/**
 * 
 * 
 * @author Nels E. Beckman
 * @since Jun 3, 2009
 *
 */
class MultiCaseHistoryTree {

	private final List<Pair<HistoryRoot, SingleCaseHistoryTree>> trees;
	
	MultiCaseHistoryTree() {
		this.trees = new ArrayList<Pair<HistoryRoot, SingleCaseHistoryTree>>();
	}
	
	void addRoot(HistoryRoot root, SingleCaseHistoryTree tree) {
		this.trees.add(Pair.create(root, tree));
	}

	/**
	 * @return
	 */
	public int size() {
		return trees.size();
	}

	/**
	 * Return the root at the given index.
	 */
	public HistoryRoot getRoot(int index) {
		return trees.get(index).fst();
	}
	
	/**
	 * Return the tree at the given index.
	 */
	public SingleCaseHistoryTree getTree(int index) {
		return trees.get(index).snd();
	}

	/**
	 * Returns the number of children 
	 */
	public int getNumberOfChildren(HistoryNode node) {
		// Well here's a crappy way to implement this...
		// Go through each of the cases, and if it has children in one
		// of them, use that number.
		for( Pair<HistoryRoot, SingleCaseHistoryTree> single_tree : this.trees ) {
			SingleCaseHistoryTree tree = single_tree.snd();
			
			if( tree.contains(node) ) {
				return tree.numChildren(node);
			}
		}
		return Utilities.nyi("This shouldn't happen. It should be in one tree.");
	}

	/**
	 * Returns the children of this node.
	 */
	public List<HistoryNode> getChildren(HistoryNode node) {
		for( Pair<HistoryRoot, SingleCaseHistoryTree> single_tree : this.trees ) {
			SingleCaseHistoryTree tree = single_tree.snd();
			
			if( tree.contains(node) ) {
				// return tree.numChildren(node);
			}
		}
		return Utilities.nyi("This shouldn't happen. It should be in one tree.");
	}
}
