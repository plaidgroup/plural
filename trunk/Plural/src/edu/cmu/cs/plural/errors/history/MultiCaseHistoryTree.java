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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.Utilities;

/**
 * A temporary tree structure that holds all of the method case trees.
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
	int size() {
		return trees.size();
	}

	/**
	 * Return the root at the given index.
	 */
	HistoryRoot getRoot(int index) {
		return trees.get(index).fst();
	}
	
	/**
	 * Return the tree at the given index.
	 */
	SingleCaseHistoryTree getTree(int index) {
		return trees.get(index).snd();
	}

	/**
	 * Returns the number of children 
	 */
	int getNumberOfChildren(HistoryNode node) {
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
	Set<HistoryNode> getChildren(HistoryNode node) {
		for( Pair<HistoryRoot, SingleCaseHistoryTree> single_tree : this.trees ) {
			SingleCaseHistoryTree tree = single_tree.snd();
			
			if( tree.contains(node) ) {
				return tree.getChildren(node);
			}
		}
		return Utilities.nyi("This shouldn't happen. It should be in one tree.");
	}
	
	/**
	 * From all of the intermediate information we have in this tree,
	 * create a {@link ResultingDisplayTree}, which is suitable to be
	 * displayed in an actual tree view.
	 */
	ResultingDisplayTree createTreeForDisplay(Object parent) {
		// Iterating, even though recursion would be much more convenient
		ResultingDisplayTree root = 
			new ResultingDisplayTree(Option.<ResultingDisplayTree>none(), 
					parent);
		
		List<ResultingDisplayTree> root_children = 
			new ArrayList<ResultingDisplayTree>(this.trees.size());
		for( Pair<HistoryRoot, SingleCaseHistoryTree> pair : this.trees ) {
			ResultingDisplayTree cur = new ResultingDisplayTree(Option.some(root),
					pair.fst());
			List<ResultingDisplayTree> cur_children = finishTree(pair.snd(), pair.fst(), cur);
			cur.setChildren(cur_children);
			
			root_children.add(cur);
		}
		root.setChildren(root_children);
		
		return root;
	}

	/**
	 * This method has to do the hard thing of making an obviously recursive
	 * algorithm into a iterative one. It goes down the tree copying it to
	 * the ResultingDsiplayTreeType.
	 */
	private List<ResultingDisplayTree> finishTree(SingleCaseHistoryTree cur_tree,
			HistoryRoot historyRoot, ResultingDisplayTree parent) {
		HistoryNode node = historyRoot.getRoot();
		// We'll just be recursive for now.
		return this.displayTreeHelper(cur_tree, node, parent);
	}
	
	private List<ResultingDisplayTree> displayTreeHelper(SingleCaseHistoryTree cur_tree,
			HistoryNode parent_node,
			ResultingDisplayTree parent) {
		List<ResultingDisplayTree> children = new ArrayList<ResultingDisplayTree>();
		
		// Print add the linear context children
		for( int i = 0; i < parent_node.numElements(); i++ ) {
			if( (i+1) == parent_node.numElements() ) {
				//  Last
				DisplayLinearContext ctx = parent_node.getContext(i);
				ResultingDisplayTree cur = new ResultingDisplayTree(Option.some(parent),ctx);
				List<ResultingDisplayTree> cur_children = new ArrayList<ResultingDisplayTree>();
				
				for( HistoryNode choice : cur_tree.getChildren(parent_node) ) {
					ResultingDisplayTree choice_ = new ResultingDisplayTree(Option.some(cur),
							choice);
					// Recur
					List<ResultingDisplayTree> choice_children = displayTreeHelper(cur_tree, choice, choice_);
					choice_.setChildren(choice_children);
					cur_children.add(choice_);
				}
				
				cur.setChildren(cur_children);
				children.add(cur);
			}
			else {
				// Regular Linear Context child
				// Has no children
				DisplayLinearContext ctx = parent_node.getContext(i);
				ResultingDisplayTree cur = new ResultingDisplayTree(Option.some(parent),ctx);
				cur.setChildren(Collections.<ResultingDisplayTree>emptyList());
				children.add(cur);
			}
		}
		
		return children;
	}	
}
