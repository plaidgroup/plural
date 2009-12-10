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
package edu.cmu.cs.plural.polymorphism.ecoop;

import edu.cmu.cs.plural.annot.Apply;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PolyVar;
import edu.cmu.cs.plural.annot.ResultPolyVar;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Symmetric;
import edu.cmu.cs.plural.annot.Unique;
import edu.cmu.cs.plural.annot.Use;

@Symmetric("p")
@ClassStates(@State(name="alive", inv="unique(first)"))
public final class LinkedList<T> {

	@Apply("p")
	private Node<T> first = null;
	
	private int size = 0;
	
	@Perm(ensures="unique(this!fr)")
	public LinkedList() {}
	
	@Unique(use=Use.FIELDS)
	public void add(@PolyVar(value="p", returned=false) T item) {
		@Apply("p") Node<T> old_node = first;
		@Apply("p") Node<T> new_first = new Node<T>(item, old_node); 
		first = new_first;
	}
	
	@Unique(use=Use.FIELDS)
	@ResultPolyVar("p")
	public T get(int i) {
		if( first == null ) return null;
		else return first.get(i, 0);
	}
}

@Symmetric("p")
@ClassStates(@State(name="alive", inv="unique(next) * p(item)"))
class Node<T> {
	private T item;
	@Apply("p")
	private Node<T> next;
	
	@Perm(ensures="unique(this!fr)")
	Node(@PolyVar(value="p", returned=false) T item, @Apply("p") @Unique(returned=false) Node<T> next) {
		this.item = item;
		this.next = next;
	}
	
	@Unique(use=Use.FIELDS)
	@ResultPolyVar("p")
	T get(int i, int cur) {
		if( i == cur ) return item;
		else if( next == null ) return null;
		else return next.get(i, cur + 1);
	}
}