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

/*
 * This is a program that cannot be checked by the Jacobs approach.
 * It uses a state invariant that depends on a thread-shared object.
 */

import java.util.concurrent.ConcurrentLinkedQueue;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.TrueIndicates;
import edu.cmu.cs.plural.annot.Unique;

@PassingTest
@UseAnalyses("FractionalAnalysis")
@ClassStates(@State(name="alive", inv="pure(reqPipe)")) 
class Handler implements Runnable {
	
	private RequestPipe reqPipe;
	
	@Perm(requires = "pure(#0)", ensures = "unique(this!fr)")
	Handler(RequestPipe pipe) {
		this.reqPipe = pipe;
		return;
	}
	
	@Unique(fieldAccess = true)
	public void run() {
		for(;;) {
			atomic: {
				if( this.reqPipe.isOpen() ) {
					String s = this.reqPipe.get();
					if( s != null ) {
						System.out.println("Got the message: " + s);
					}
				}
				else {
					break;
				}
			}
		}
		return;
	}
}

@ClassStates({@State(name="IDLE", inv="full(requestPipe) in closed"),
		      @State(name="RUNNING", inv="full(requestPipe) in open")})
public class RequestGenerator {

	private RequestPipe requestPipe = new RequestPipe();
	
	@Unique(requires="IDLE", ensures="RUNNING", fieldAccess = true)
	void start() {
		this.requestPipe.open();
		
		(new Thread(new Handler(this.requestPipe))).start();
		(new Thread(new Handler(this.requestPipe))).start();
		
		return;
	}

	@Unique(requires="RUNNING", ensures="RUNNING", fieldAccess = true)
	void send(String str) {
		this.requestPipe.send(str);
		return;
	}
	
	@Unique(requires="RUNNING", ensures="IDLE", fieldAccess = true)
	void stop() {
		this.requestPipe.close();
		return;
	}
}

class Thread extends java.lang.Thread {
	@Perm(requires = "unique(#0)", ensures = "unique(this!fr)")
	public Thread(Runnable r) {
		super(r);
	}
	
	@Unique(returned=false, fieldAccess = true)
	public void start() {
		super.start();
		return;
	}
}

class RequestPipe {
	
	private ConcurrentLinkedQueue<String> queue;
	
	@Pure(fieldAccess = true)
	@TrueIndicates("open")
	boolean isOpen() {
		return true;
	}
	
	@Full(requires="closed", ensures="open", fieldAccess = true)
	void open() {
		atomic: {
			this.queue = new ConcurrentLinkedQueue<String>();
		}
		return;
	}
	
	@Full(requires="open", ensures="closed", fieldAccess = true)
	void close() {
		atomic: {
			this.queue = null;
		}
		return;
	}
	
	@Full(requires="open", ensures="open", fieldAccess = true)
	void send(String str) {
		atomic: {
			this.queue.add(str);
			return;
		}
	}
	
	@Pure(requires="open", ensures="open", fieldAccess = true)
	String get() {
		atomic: {
			return this.queue.poll();
		}
	}
}