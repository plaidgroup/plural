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
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.TrueIndicates;

@FailingTest(3)
@UseAnalyses("FractionalAnalysis")
@ClassStates(@State(name="alive", inv="hasFull == true => full(data)"))
public class ImplicationWorks {

	boolean hasFull;
	
	LastObject data;
	
	@Perm(ensures = "unique(this!fr)")
	ImplicationWorks(Object o) { // ERROR: Since we are setting the field to true
		this.hasFull = true;     // we should have to come up with the permission too.
	}
	
	@Perm(ensures = "unique(this!fr)")
	ImplicationWorks() {
		this.hasFull = true;
		this.data = new LastObject();
	}
	
	@Full(fieldAccess=true)
	void testPermImplicationShouldWork() {
		if( hasFull == true ) {
			hasFull = false;
			data.modify();
			return;
		}
		else {
			return;
		}
	}
		
	@Share
	void testPermImplicationShouldFail(@Full LastObject lo) {
		boolean local = hasFull;
		
		if( hasFull == true ) {
			lo.modify();
		}
		else {
			lo.modify();
		}
		
		if( local == true ) {
			data.modify(); // ERROR: You should only be able to eliminate once.
			return;
		}
		else {
			return;
		}
	}
	
	
	static void testStateImplicationShouldFail(@Share OtherObject o, @Full LastObject lo) {
		
		boolean stored_bool = o.isClosed();
		
		lo.modify();
		
		if( stored_bool ) {
			o.open(); // ERROR: This call should fail
		}	
	}

	static void testStateImplicationShouldWork(@Share OtherObject o, @Full LastObject lo) {
		
		boolean stored_bool = o.isClosed();
				
		if( stored_bool ) {
			o.open();
		}	
	}
}

class OtherObject {
	
	@Share(fieldAccess = true)
	@TrueIndicates("closed")
	boolean isClosed() {
		return true;
	}
	
	@Share(requires="closed")
	void open() {
		
	}
}

class LastObject {
	@Perm(ensures="unique(this!fr)")
	LastObject() {
		
	}
	@Full
	void modify() {
		
	}
}