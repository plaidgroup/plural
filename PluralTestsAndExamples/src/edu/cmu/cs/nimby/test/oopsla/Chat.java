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

package edu.cmu.cs.nimby.test.oopsla;



import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.TrueIndicates;
import edu.cmu.cs.plural.annot.Unique;

/**
 * Here is a pedagogical example. It is meant to be a program similar in architecture to
 * a chat application I saw on the Internet once.<br>
 * <br>
 * 9/12/08<br>
 * This program has changed since publication. It has been updated to
 * use the new invariant syntax, and the @Perm annotation. 
 */
@PassingTest
@UseAnalyses("NIMBYChecker")
public class Chat {
	
	public static void main(String[] args) {
		Connection net = new Connection();
		GUI chat_gui = new GUI(net);
		NetworkListener listener = new NetworkListener(net);
		
		(new Thread(chat_gui)).start();
		(new Thread(listener)).start();
	}
	
}


/**
 * A thread that is listening to the network.
 * Simulate the call-back thread from the network that can disconnect the device at
 * will.
 */
@ClassStates(@State(name="alive",
		            inv="share(myNetwork)"))
class NetworkListener implements Runnable {
	
	final private Connection myNetwork;
	
	@Unique(fieldAccess = true) 
	public void run() {
		for( int i = 0; i < 3; i++ ) {
			try {
				Thread.sleep(1000);
				
				atomic: synchronized(Chat.class) {
					if( myNetwork.isConnected() ) {
						myNetwork.disconnect();
					}
				}
			} catch(Exception e) { 
				break;
			}
		}
		return;
	}

	@Perm(requires="share(#0)", ensures="unique(this!fr)")
	NetworkListener(Connection myNetwork) {
		this.myNetwork = myNetwork;
	}
}

class BufferedWriter extends java.io.BufferedWriter {
	@Perm(ensures="unique(this!fr)")
	public BufferedWriter(FileWriter f) {
		super(f);
	}
}

@ClassStates({@State(name="alive", inv="full(socket)"),
	          @State(name="IDLE", inv="socket == null"),
	          @State(name="CONNECTED", inv="socket != null")})
class Connection {
	private BufferedWriter socket;
	
	@Perm(ensures="unique(this!fr)")
	Connection() {
		socket = null;
	}
	
	@Share(fieldAccess = true, requires="CONNECTED", ensures="IDLE")
	void disconnect() throws IOException {
		atomic: synchronized(Chat.class) {
			socket.flush();
			socket.close();
			this.socket = null;
			return;
		}
	}
	
	@Pure(fieldAccess = true)
	@TrueIndicates("IDLE")
	public boolean isIdle() {
		atomic: synchronized(Chat.class) {
			if( this.socket == null ) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	@Pure(fieldAccess = true)
	@TrueIndicates("CONNECTED")
	public boolean isConnected() {
		atomic: synchronized(Chat.class)
		{
			if( this.socket != null ) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	@Share(fieldAccess = true, requires="CONNECTED", ensures="CONNECTED")
	void send(String txt) throws IOException {
		atomic: synchronized(Chat.class) {
			this.socket.write(txt + "\n");
			return;
		}
	}
	
	@Share(fieldAccess = true, requires="IDLE", ensures="CONNECTED")
	void connect(String ip) throws IOException {
		atomic: synchronized(Chat.class) {
			this.socket = new BufferedWriter(new FileWriter("foo.out", true));
			return;
		}
	}
}

/**
 * A 'GUI' for the chat application.
 * Simulates random button presses on GUI buttons via a thread.
 */
@ClassStates(@State(name="alive", inv="share(myNetwork)"))
class GUI implements Runnable {
	
	final private Connection myNetwork;
	
	@Perm(requires="share(#0)", ensures="unique(this!fr)")
	GUI(Connection myNetwork) {
		this.myNetwork = myNetwork;
	}
	
	/*
	 * Simulate random button presses from the user on enabled buttons.
	 */
	@Unique(fieldAccess = true)	
	public void run() {
		Random r = new Random();
		try {
			for(int i = 0; i < 20; i++) {
				Thread.sleep(300);
				
				switch(r.nextInt(6)) {
				case 0:
					atomic: synchronized(Chat.class) {
						if( myNetwork.isIdle() ) {
							myNetwork.connect("fake.fake.fake.fake");
						}
					}
					break;
				case 1:
					atomic: synchronized(Chat.class) {
						if( myNetwork.isConnected() ) {
							myNetwork.disconnect();
						}
					}
					break;
				case 2:
				case 3:
				case 4:
				case 5:
					atomic: synchronized(Chat.class) {
						if( myNetwork.isConnected() ) {
							myNetwork.send("Yo dude let's talk?!");
						}
					}
					break;
				default:
					break;
				}
			} 
		} catch (Exception e) { 
			return; 
		}
		finally {
			atomic: synchronized(Chat.class) {
				if( myNetwork.isConnected() ) {
					try { myNetwork.disconnect(); } catch(Exception e) {}
				}
			}
		}
	}
}