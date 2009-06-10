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

import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.plural.contexts.LinearContext;
import edu.cmu.cs.plural.errors.ChoiceID;

/**
 * Each node in the history has an ID, a parent, and a list of 
 * LinearContexts (that all originally had the same id).
 * 
 * @author Nels E. Beckman
 * @since Jun 3, 2009
 *
 */
class HistoryNode {
	
	private final ChoiceID id;
	
	private final ChoiceID parentID;
	
	/** An ordered list of contexts, each with the same ID,
	 *  and the nodes at which they were found. */
	private final List<DisplayLinearContext> contexts;
	
	public HistoryNode(LinearContext ctx, ITACLocation loc) {
		this(new DisplayLinearContext(ctx, loc));
	}
	
	public HistoryNode(DisplayLinearContext ctx) {
		this.contexts = new LinkedList<DisplayLinearContext>();
		this.contexts.add(ctx);
		this.id = ctx.getContext().getChoiceID();
		this.parentID = ctx.getContext().getParentChoiceID();
	}
	
	public void append(LinearContext ctx, ITACLocation loc) {
		this.append(new DisplayLinearContext(ctx, loc));
	}
	
	public void append(DisplayLinearContext ctx) {
		assert(ctx.getContext().getChoiceID().equals(id));
		assert(ctx.getContext().getParentChoiceID().equals(parentID));
		contexts.add(ctx);
	}
	
	public void concat(HistoryNode node) {
		assert(node.id.equals(id));
		assert(node.parentID.equals(parentID));
		contexts.addAll(node.contexts);
	}

	/**
	 * Get the parent id for this node.
	 */
	public ChoiceID getParentChoiceID() {
		return parentID;
	}

	/**
	 * Gets the id of this node.
	 */
	public ChoiceID getChoiceID() {
		return this.id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contexts == null) ? 0 : contexts.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((parentID == null) ? 0 : parentID.hashCode());
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
		HistoryNode other = (HistoryNode) obj;
		if (contexts == null) {
			if (other.contexts != null)
				return false;
		} else if (!contexts.equals(other.contexts))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (parentID == null) {
			if (other.parentID != null)
				return false;
		} else if (!parentID.equals(other.parentID))
			return false;
		return true;
	}

	/**
	 * Returns the number of elements held by this node.
	 */
	public int numElements() {
		return this.contexts.size();
	}

	/**
	 * Return the linear context contained at the
	 * given index.
	 */
	public DisplayLinearContext getContext(int index) {
		return this.contexts.get(index);
	}
}
