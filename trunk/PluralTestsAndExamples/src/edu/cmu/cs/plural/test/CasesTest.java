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

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Cases;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;

/**
 * This is a test for method cases.  There should be no errors in this class.
 * @author Kevin
 * @since 5/01/2008
 */
@PassingTest
@UseAnalyses("FractionalAnalysis")
public class CasesTest {
	
	interface Iterable<T> {
		
		@Cases({
			@Perm(requires = "full(this)", ensures = "unique(result) in modifying"),
			@Perm(requires = "immutable(this)", ensures = "unique(result) in readonly")
		})
		Iterator<T> iterator();
	}
	
	@Refine({
		@States(dim = "next", value = {"available", "end"}),
		@States(dim = "current", value = {"retrieved", "removed"}),
		@States(dim = "kind", value = {"readonly", "modifying"}, marker = true)
	})
	interface Iterator<T> {
		@Pure("next")
		@TrueIndicates("available")
		boolean hasNext();
		
		@Full(requires = "available", ensures = "retrieved")
		T next();
		
		@Full(value = "current", requires = "retrieved", ensures = "removed")
		@Pure(value = "modifying")
		void remove();
	}
	
	@Perm(requires = "full(#0)")
	public static <T> void modify(Iterable<T> c) {
		Iterator<T> it = c.iterator();
		while(it.hasNext()) {
			it.next();
			it.remove();
		}
	}

	@Perm(requires = "full(#0)", ensures = "immutable(#0)")
	public static <T> void twoIters(Iterable<T> c) {
		Iterator<T> it1 = c.iterator();
		Iterator<T> it2 = c.iterator();
		while(it1.hasNext()) {
			T t = it1.next();
			if(t != null && it2.hasNext()) {
				it2.next();
			}
		}
	}

	@Perm(requires = "immutable(#0)", ensures = "immutable(#0)")
	public static <T> void twoReadonlyIters(Iterable<T> c) {
		Iterator<T> it1 = c.iterator();
		Iterator<T> it2 = c.iterator();
		while(it1.hasNext()) {
			T t = it1.next();
			if(t != null && it2.hasNext()) {
				it2.next();
			}
		}
	}

	@Perm(requires = "full(#0)", ensures = "immutable(#0)")
	public static <T> void nested(Iterable<T> c) {
		Iterator<T> it1 = c.iterator();
		while(it1.hasNext()) {
			T t = it1.next();
			if(t != null) {
//				Iterator<T> it2 = c.iterator(); 
//				if(it2.hasNext()) {
//					it2.next();
//				}
				for(Iterator<T> it2 = c.iterator(); it2.hasNext(); ) {
					it2.next();
				}
			}
		}
	}

}
