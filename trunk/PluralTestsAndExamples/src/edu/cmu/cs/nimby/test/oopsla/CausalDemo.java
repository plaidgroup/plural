/**
 * This class is part of the JGroups project (www.jgroups.org) and is
 * (c) its creators. It was released under the LGPL v2.1 which allows
 * for its modification and redistribution. This class was modified
 * on 2008/09/15.
 */

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

//$Id: CausalDemo.java,v 1.2 2008/09/17 14:12:28 nbeckman Exp $
import java.util.Random;
import java.util.Vector;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Unique;
import edu.cmu.cs.plural.annot.Use;



/**
 * This class is part of JGroups (www.jgroups.org) and is (c) its creators<br>
 * <br>
 * Simple causal demo where each member bcast a consecutive letter from the
 * alphabet and picks the next member to transmit the next letter. Start a
 * few instances of CausalDemo and pass a paramter "-start" to a CausalDemo
 * that initiates transmission of a letter A. All participanting members should
 * have correct alphabet. DISCARD layer has been added to simulate lost messages,
 * thus forcing delaying of delivery of a certain alphabet letter until the causally
 * prior one has been received.  Remove CAUSAL from the stack and witness how FIFO
 * alone doesn't provide this guarantee.<br>
 * <br>
 * 9/15/08<br>
 * This class has been changed to include atomic primitives. It has also been
 * modified since OOPSLA publication to include the new-style PLURAL annotations.
 *
 * @author Vladimir Blagojevic
 */
@PassingTest
@UseAnalyses("NIMBYChecker")
public class CausalDemo implements Runnable
{
	//private Channel channel;
	private final Vector alphabet = new Vector();
	private boolean starter = false;
	private int doneCount=0;
	private Log log=LogFactory.getLog(getClass());

	private final String props = "causal.xml";

	@Perm(ensures="unique(this!fr)")
	public CausalDemo(boolean start)
	{
		starter = start;
	}

	public String getNext(String c)
	{
		char letter = c.charAt(0);
		return new String(new char[]{++letter});
	}

	@Pure
	public void listAlphabet()
	{
		System.out.println(alphabet);
	}
			
	//@Unique(use = Use.FIELDS, returned=false)
	//@Full(use = Use.FIELDS)
	@Perm(requires="pure(this) * full(this!fr)")
	public void run()
	{
		Object obj;
		Message msg;
		Random r = new Random();

		//JChannel channel = null;

		try
		{
			final JChannel channel = new JChannel(props);
			channel.connect("CausalGroup");
			System.out.println("View:" + channel.getView());
			if (starter) 
				channel.send(new Message(null, null, new CausalMessage("A", (Address) channel.getView().getMembers().get(0))));


			Runtime.getRuntime().addShutdownHook(
					new Thread2(new ShutDownThread(channel, this)));

			while (true)
			{
				try
				{
					CausalMessage cm = null;
					
					atomic: {
						if( channel.isConnected() )
							obj = channel.receive(0); // no timeout
						else 
							return;
					}
					
					if (obj instanceof Message)
					{
						msg = (Message) obj;
						cm = (CausalMessage) msg.getObject();
						
						Vector members;
						atomic: {
							if( channel.isConnected() )
								members = channel.getView().getMembers();
							else
								return;
						}
						
						String receivedLetter = cm.message;

						if("Z".equals(receivedLetter))
						{
							atomic: {
								Message to_send = new Message(null, null, new CausalMessage("done", null));
								if(  channel.isConnected() )
									channel.send(to_send);
								else
									return;
							}
						}
						if("done".equals(receivedLetter))
						{
							if(++doneCount >= members.size())
							{
								System.exit(0);
							}
							continue;
						}

						alphabet.add(receivedLetter);
						listAlphabet();

						//am I chosen to transmit next letter?
						Address addr;
						atomic: {
							if( channel.isConnected() )
								addr = channel.getLocalAddress();
							else
								return;
						}
						
						if (cm.member.equals(addr))
						{
							int nextTarget = r.nextInt(members.size());

							//chose someone other than yourself
							while (nextTarget == members.indexOf(addr))
							{
								nextTarget = r.nextInt(members.size());
							}
							Address next = (Address) members.get(nextTarget);
							String nextChar = getNext(receivedLetter);
							if (nextChar.compareTo("Z") < 1)
							{
								System.out.println("Sending " + nextChar);
								
								atomic: {
									Message to_send = new Message(null, null, new CausalMessage("done", null));
									if( channel.isConnected() )
										channel.send(to_send);
									else
										return;
								}
							}
						}
					}
				}
				catch (ChannelNotConnectedException conn)
				{
					break;
				}
				catch (Exception e)
				{
					log.error(e);
				}
			}

		}
		catch (Exception e)
		{
			System.out.println("Exception" + e);
		}
	}


	public static void main(String args[])
	{
		CausalDemo test = null;
		boolean    start=false;

		for(int i=0; i < args.length; i++) {
			if("-help".equals(args[i])) {
				System.out.println("CausalDemo [-help] [-start]");
				return;
			}
			if("-start".equals(args[i])) {
				start=true;
				continue;
			}
		}

		//if parameter start is passed , start the demo
		test = new CausalDemo(start);
		try
		{
			new Thread2(test).start();
		}
		catch (Exception e)
		{
			System.err.println(e);
		}

	}

}

/**
 * In the original example, this class was a nested inner class. Due to
 * limitations in our tool, it was extracted, and the CausalDemo field
 * was added to this one.
 */
@ClassStates(@State(name="alive", inv="share(channel) * pure(causalDemo)"))
class ShutDownThread implements Runnable {
	
	final JChannel channel;
	final CausalDemo causalDemo; // New field 
	
	@Perm(requires="share(#0) in connected * pure(#1)", ensures="unique(this!fr)")
	public ShutDownThread(JChannel channel,
			              CausalDemo causalDemo) {
		this.channel = channel;
		this.causalDemo = causalDemo;
	}
	
	@Unique(use = Use.FIELDS)
	public void run()
	{
		this.causalDemo.listAlphabet();
		this.channel.disconnect();
		this.channel.close();
	}
}

/*
 * The following are classes that were originally defined in the JGroups
 * project but that I didn't want to include here in the test Project, or
 * library classes that I needed to annotate.
 */

class Thread2 extends java.lang.Thread {

	@Perm(ensures="unique(this!fr)")
	public Thread2(@Unique(returned=false) Runnable r) {
		super(r);
	}

	public Thread2(ThreadGroup globalThreadGroup, String string) {
	}
}