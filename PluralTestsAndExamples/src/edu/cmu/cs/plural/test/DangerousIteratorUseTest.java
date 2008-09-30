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
import edu.cmu.cs.plural.annot.Unique;

@FailingTest(2)
// 3 when not using borrowing optimization
// was 5 when reporting state and permission failures separately
// should be 4 because one problem (need full for second parameter to concat)
// was reported twice before
@UseAnalyses("FractionalAnalysis")
public class DangerousIteratorUseTest {

	public static void dangerousIteratorUse(@Unique String[] strings) {
		ArrayIterator<String> it = 
			new ArrayIterator<String>(strings);
		while(it.hasNext()) {
			ArrayIterator.concat(it, it); 
			// error(s) around here because concat() needs full and share of it
			System.out.println(it.next());
		}
		// next line can have an error because of errors using "it" in the loop
		// errors may or may not propagate out of the loop
		it.dispose(); 
		strings[0] = "goodbye";
	}

}
