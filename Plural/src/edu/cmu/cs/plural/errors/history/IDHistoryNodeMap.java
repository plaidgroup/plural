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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.cmu.cs.plural.contexts.LinearContext;
import edu.cmu.cs.plural.errors.ChoiceID;

/**
 * A temporary data structure used to group linear contexts of the same
 * ChoiceID together and index them by id. Essentially is a
 * {@code Map<ChoiceID, HistoryNode>} with a smarter merge operation.
 * 
 * @author nbeckman
 * @since Jun 3, 2009
 *
 */
class IDHistoryNodeMap {

	private Map<ChoiceID, HistoryNode> map;
	
	IDHistoryNodeMap() {
		this.map = new LinkedHashMap<ChoiceID, HistoryNode>();
	}
	
	private IDHistoryNodeMap(Map<ChoiceID, HistoryNode> map) {
		this.map = map;
	} 
	
	/**
	 * @param other
	 * @return
	 */
	public void merge(IDHistoryNodeMap other) {
		Map<ChoiceID, HistoryNode> new_map = new HashMap<ChoiceID, HistoryNode>(map);
		for( Map.Entry<ChoiceID, HistoryNode> entry : other.map.entrySet() ) {
			if( new_map.containsKey(entry.getKey()) ) {
				new_map.get(entry.getKey()).concat(entry.getValue());
			}
			else {
				new_map.put(entry.getKey(), entry.getValue());
			}
		}
		this.map = new_map;
	}

	/**
	 * @param c
	 * @return
	 */
	public static IDHistoryNodeMap singleton(DisplayLinearContext c) {
		return new IDHistoryNodeMap(Collections.singletonMap(c.getContext().getChoiceID(), 
				new HistoryNode(c)));
	}

	public static IDHistoryNodeMap singleton(LinearContext ctx, ITACLocation loc) {
		return singleton(new DisplayLinearContext(ctx, loc));
	}
	
	/**
	 * @param fst
	 * @param ctx
	 */
	public void put(DisplayLinearContext ctx) {
		ChoiceID id = ctx.getContext().getChoiceID();
		if( this.map.containsKey(id) ) {
			this.map.get(id).append(ctx);
		}
		else {
			this.map.put(id, new HistoryNode(ctx));
		}
	}

	/**
	 * Get the history node associated with the given choice id.
	 */
	public HistoryNode get(ChoiceID parent_id) {
		return this.map.get(parent_id);
	}

	/**
	 * The entry set for this map, mapping choice ids to history nodes.
	 */
	public Set<Entry<ChoiceID, HistoryNode>> entrySet() {
		return this.map.entrySet();
	}

}
