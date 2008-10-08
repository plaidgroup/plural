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

package edu.cmu.cs.nimby.test.interesting;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Capture;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.FalseIndicates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.Release;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;
import edu.cmu.cs.plural.annot.Unique;

@PassingTest
@UseAnalyses("NIMBYChecker")
public class ThreadSharedAndBack {

	public static void foo() {
		PingPong ping_pong = new PingPong();
		OtherThread other_thread = new OtherThread(ping_pong); // takes share(ping_pong,1/2)

		// Other thread 'captured' the permission to ping pong, so
		// we will get back exactly 1/2 when we call join.
		
		other_thread.start(); // take full(INSIDE), leaves full(OUTSIDE)
		
		for( int i=0; i<5; i++ ) {
			atomic: {
				if( ping_pong.isPing() ) {
					ping_pong.pong(); // We atomically know object is PING
				}
				else {
					ping_pong.ping(); // We atomically know object is PONG
				}
			}
		}
		
		try {
			other_thread.myJoin(); // Gives us back share(ping_pong,1/2), but we
			                       // need full(OUTSIDE) to ensure we only do it once.
			ping_pong.requiresUnique();
		} catch (InterruptedException e) {}
		
	}	
}

@ClassStates({
	@State(name="PING", inv="isPing == true"),
	@State(name="PONG", inv="isPing == false")
})
class PingPong {
	
	private boolean isPing = true; 
	
	@Perm(ensures="unique(this!fr) in PING")
	PingPong() {
		
	}
	
	@Pure(fieldAccess=true)
	@TrueIndicates("PING")
	@FalseIndicates("PONG")
	boolean isPing() {
		atomic: {return isPing;}
	}
	
	@Share(fieldAccess=true, requires="PONG", ensures="PING")
	void ping() {
		atomic: {this.isPing = true;}
	}
	
	@Share(fieldAccess=true, requires="PING",ensures="PONG")
	void pong() {
		atomic:{this.isPing = false;}
	}
	
	@Unique void requiresUnique() {}
}

@Refine({
	@States(dim="INSIDE", value="INSIDESTATE"),
	@States(dim="OUTSIDE", value= "OUTSIDESTATE")
})
@ClassStates(
    @State(name="INSIDE", inv="share(myPingPong)")
)
class OtherThread extends java.lang.Thread {
	final PingPong myPingPong;
	
	@Perm(requires="share(#0)", ensures="unique(this!fr)")
	OtherThread(@Capture(param="p") PingPong myPingPong) {
		this.myPingPong = myPingPong;
	}

	@Override
	@Full(fieldAccess=true,value="INSIDE")
	public void run() {
		for( int i=0; i<5; i++ ) {
			atomic: {
				if( this.myPingPong.isPing() ) {
					this.myPingPong.pong();
				}
				else {
					this.myPingPong.ping();
				}
			}
		}
	}

	@Override
	@Full(value="INSIDE", returned=false)
	public synchronized void start() {
		super.start();
	}
	
	@Release(value="p")
	@Full(value="OUTSIDE", returned=false)
	public void myJoin() throws InterruptedException {
		/*
		 * In a fully-verified version of this program,
		 * the join method would check and retry to see
		 * if the thread itself was dead. When the thread
		 * has died, giving it full to the outside will
		 * return the share(myPingPong,1/2).
		 */
		this.join();
	}
}