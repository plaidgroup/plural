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

package edu.cmu.cs.plural.test;

import java.util.Arrays;
import java.util.List;

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.FalseIndicates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.ResultUnique;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;
import edu.cmu.cs.plural.annot.Unique;

@FailingTest(2)
@UseAnalyses("FractionalAnalysis")
@Refine({
	@States(value={"available", "end"}, dim = "next"),
	@States(value={"retrieved", "removed"}, dim = "current")
})
public class ModifyingIterator<T> {
	
	private List<T> list;
	private int next;

	@Perm(requires = "full(#0)", ensures = "unique(this!fr)")
	public ModifyingIterator(List<T> list) {
		this.list = list;
	}
	
	@Pure(fieldAccess = true)
	@TrueIndicates("available")
	@FalseIndicates("end")
	public boolean hasNext() {
		return next < list.size();
	}
	
	@Full(requires = "available", ensures = "retrieved", fieldAccess = true)
	public T next() {
		return list.get(next++);
	}
	
	@Full(value = "current", requires = "retrieved", ensures = "removed", fieldAccess = true)
	public void remove() {
		list.remove(--next);
	}
	
	@ResultUnique
	public static List<String> createList() {
		// post-condition fails because Arrays.asList() is not annotated
		return Arrays.asList(new String[] { "hello", "world" });
	}
	
	// this is correct iterator usage
	public static void arrayTest() {
		List<String> l = createList();
		ModifyingIterator<String> it = new ModifyingIterator<String>(l);
		while(it.hasNext()) {
			String s = it.next();
			if(s.length() > 5)
				it.remove();
		}
	}

	public static void doubleRemove() {
		List<String> l = createList();
		ModifyingIterator<String> it = new ModifyingIterator<String>(l);
		while(it.hasNext()) {
			String s = it.next();
			if(s.length() > 5)
				it.remove();
			it.remove(); // error: cannot remove twice
		}
	}
	
	// this is correct iterator usage
	public static void removeBeforeNext(@Full ModifyingIterator<String> it) {
		if(it.hasNext()) {
			it.next();
			while(it.hasNext()) {
				it.remove();
				it.next();
			}
		}
	}

}
