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
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;
import edu.cmu.cs.plural.annot.Use;

@FailingTest(5) 
//  7 when not using borrowing optimization
// 12 when reporting state and permission failures separately
@UseAnalyses("FractionalAnalysis")
@States({"open", "closed"})
public class StreamProtocol {
	
	@Perm(ensures = "unique(this!fr) in open")
	public StreamProtocol() {
		super();
	}
	
	@Full("open")
	public int read() {
		return -1;
	}
	
	@Pure("open")
	public int available() {
		return 0;
	}
	
	@Full(requires = "open", ensures = "closed", use = Use.FIELDS)
	public void close() {
	}
	
	@Pure(use = Use.FIELDS)
	@TrueIndicates("closed")
	@FalseIndicates("open")
	public boolean isClosed() {
		return false;
	}
	
	public static void process(@Full("open") StreamProtocol s) {
		System.out.println(s.read());
	}
	
	private static void dangerousProcess(@Full(requires = "open") StreamProtocol s) {
		System.out.println(s.read()); 
		s.close();
	}
	
	public static void streamTest() {
		StreamProtocol s = new StreamProtocol();
		while(s.available() > 0)
			// error(s) around here due to call to dangerousProcess (might close) 
			dangerousProcess(s);
		s.close();
		s.read(); // error: read after close
	}

	// this is a correct method
	public static void correctStreamUsage() {
		StreamProtocol s = new StreamProtocol();
		while(s.available() > 0)
			process(s);
		s.close();
	}

	public static void readAfterClose() {
		StreamProtocol s = new StreamProtocol();
		while(s.available() > 0)
			process(s);
		s.close();
		s.read(); // error
	}

}
