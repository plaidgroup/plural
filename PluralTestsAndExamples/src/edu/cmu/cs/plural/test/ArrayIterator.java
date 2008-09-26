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

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.FalseIndicates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Capture;
import edu.cmu.cs.plural.annot.Param;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Release;
import edu.cmu.cs.plural.annot.ResultUnique;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;
import edu.cmu.cs.plural.annot.Unique;

@FailingTest(6)  // 7 when reporting state and permission failures separately
@UseAnalyses("FractionalAnalysis")
@States({"available", "end"})
@Param(name = "array", releasedFrom = "alive")
public class ArrayIterator<T> {
	
	private final T[] array;
	private int pos = 0;

	@Perm(requires = "immutable(#0)", ensures = "unique(this!fr)")
	public ArrayIterator(@Capture(param = "array") T[] array) {
		this.array = array;
	}
	
	@Full(requires="available", fieldAccess = true)
	public T next() {
		return array[pos++];
	}
	
	@Pure(fieldAccess = true)
	@TrueIndicates("available")
	@FalseIndicates("end")
	public boolean hasNext() {
		return pos < array.length;
	}
	
	@Unique(returned = false)
	@Release("array")
	public void dispose() {
	}
	
	public static String concat(
			@Full(requires="available") ArrayIterator<String> it1,
			@Share(requires="available") ArrayIterator<String> it2) {
		// errors here because 
		// (1) it2's state switches to alive before being called
		// (2) it2.next() needs a full instead of a share permission
		return it1.next().concat(it2.next());
	}
	
	public static <T> void consume(@Share(returned = false) ArrayIterator<T> it) {
		return;
	}
	
	@ResultUnique
	private static String[] createArray() {
		return new String[] { "hello", "world" };
	}
	
	public static void arrayTest() {
		String[] strings = createArray();
		ArrayIterator<String> it = 
			new ArrayIterator<String>(strings);
//		strings[0] = "goodbye";
		while(it.hasNext()) {
//			concat(it, it);
			System.out.println(it.next());
		}
		it.next();
		while(it.hasNext()) {
//			it.dispose();
//			consume(it);
		}
		it.dispose();
		strings[0] = "goodbye";
	}

	public static void correctArrayTest() {
		String[] strings = createArray();
		ArrayIterator<String> it = 
			new ArrayIterator<String>(strings);
		while(it.hasNext()) {
			System.out.println(it.next());
		}
		it.dispose();
		strings[0] = "goodbye";
	}

	public static void concurrentModification() {
		String[] strings = createArray();
		ArrayIterator<String> it = 
			new ArrayIterator<String>(strings);
		strings[0] = "goodbye"; // error: concurrent modification
		while(it.hasNext()) {
			System.out.println(it.next());
		}
	}

	public static void cannotDispose() {
		String[] strings = createArray();
		ArrayIterator<String> it = 
			new ArrayIterator<String>(strings);
		while(it.hasNext()) {
			System.out.println(it.next());
		}
		while(it.hasNext()) {
			consume(it);
		}
		it.dispose(); // error: no unique permission for it
	}

	public static void disposeLoop() {
		String[] strings = createArray();
		ArrayIterator<String> it = 
			new ArrayIterator<String>(strings);
		while(it.hasNext()) {
			System.out.println(it.next());
		}
		while(it.hasNext()) {
			// error(s) around here because dispose could be called repeatedly
			it.dispose();
		}
		strings[0] = "goodbye";
	}

	// this is correct iterator usage
	public static void nestedIteration(@Full String[] strings) {
		ArrayIterator<String> it1 = 
			new ArrayIterator<String>(strings);
		while(it1.hasNext()) {
			String s = it1.next();
			if(s != null) {
				ArrayIterator<String> it2 = 
					new ArrayIterator<String>(strings); 
				while(it2.hasNext())
					System.out.println(it2.next());
				it2.dispose();
			}
		}
		it1.dispose();
	}

}
