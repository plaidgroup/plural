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
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.States;

/**
 * This class is intended to test that specifications
 * are correctly inherited from overridden methods
 * and that ambiguities are resolved where needed
 * 
 * @author Kevin Bierhoff
 * @since 7/16/2008
 */
@FailingTest(2)
// 3 when not using borrowing optimization
@UseAnalyses({"FractionalAnalysis","PluralAnnotationAnalysis"})
public class SpecInheritanceTest {
	
	@States({"a", "b"})
	interface I {
		
		@Full("b")
		void needsB();
		
		@Full("a")
		void ambiguous();
	}
	
	interface J {
		
		@Pure
		void ambiguous();
		
	}

	abstract class Super extends Object implements I, J {
		
		@Full("a")
		public void needsA() {
			
		}
		
		public void needsB() {
			readB();
		}
		
		@Pure("b")
		private void readB() {
			
		}
		
		@Full("a")
		void symmetric(@Full("b") Super other) {
			this.needsA();
		}
		
		// error: needs spec
		public void ambiguous() {
			
		}
	}
	
	class Sub extends Super implements I {

		@Full("a")
		public void needsA() {
			needsB(); // error: have full(a) but need full(b)
		}
		
		void symmetric(Super other) {
			this.needsA();
			other.needsB();
		}
		
		@Pure
		public void ambiguous() {
			this.ambiguous();
//			super.ambiguous(); // error: call to ambiguous method (problems with super)
		}
		
	}
	
}