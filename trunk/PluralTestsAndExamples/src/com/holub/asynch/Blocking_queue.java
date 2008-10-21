package com.holub.asynch;


//import java.util.LinkedList; NEB: I use my own, annotated LinkedList.
import java.util.NoSuchElementException;

import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.FalseIndicates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Pures;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;

/***********************************************************************
 * This is a thread-safe queue that blocks automatically if you
 *	try to dequeue from an empty queue. It's based on a linked list,
 *  so will never fill. (You'll never block on a queue-full condition
 *	because there isn't one.)
 *
 *	<p>
 *	This class uses the <code>LinkedList</code> class, introduced into
 *	the JDK at version 1.2. It will not work with earlier releases.
 *
 * <br><br>
 * <table border=1 cellspacing=0 cellpadding=5><tr><td><font size=-1><i>
 * <center>(c) 1999, Allen I. Holub.</center>
 * <p>
 * This code may not be distributed by yourself except in binary form,
 * incorporated into a java .class file. You may use this code freely
 * for personal purposes, but you may not incorporate it into any
 * commercial product without express permission of Allen I. Holub in
 * writing.
 * </td></tr></table>
 *
 * @author Allen I. Holub
 */
@Refine({
	@States(dim="STRUCTURE", value={"STRUCTURESTATE"}),
	@States(dim="PROTOCOL", value= {"CLOSED", "OPEN"})
})
@ClassStates({
	@State(name="STRUCTURE", 
			// NEB: Of course waiting_threads cannot be null, but we needed to map it
			// to the STRUCTURE dimension, and there was no other way to do this.
		   inv="share(elements) * reject_enqueue_requests == true => full(this,PROTOCOL) in OPEN"),
	@State(name="OPEN", inv="closed == false"), 
	@State(name="CLOSED", inv="closed == true")
})
public class Blocking_queue
{
	private LinkedList elements 				= new LinkedList();
	private boolean	   closed					= false;
	private boolean	   reject_enqueue_requests	= false;
	private int		   waiting_threads			= 0;

	// NEB: Original class had no constructor
	@Perm(ensures="unique(this!fr) in OPEN,STRUCTURESTATE")
	public Blocking_queue() {
	}
	
	/*******************************************************************
	 * The Closed exception is thrown if you try to used an explicitly
	 * closed queue. See {@link #close}.
	 */

	public class Closed extends RuntimeException
	{	
        // Unique SerialVersionUID (should NEVER change)
        static final long serialVersionUID = 0L;
        
        @Perm(ensures="unique(this!fr)")
        private Closed()
		{	super("Tried to access closed Blocking_queue");
		}
	}

	/*******************************************************************
	 *	Enqueue an object
	 **/
	@Share(value="STRUCTURE")
	@Full(requires="OPEN", ensures="OPEN", value="PROTOCOL")
	public synchronized final void enqueue( Object new_element )
														throws Closed
	{	if( closed || reject_enqueue_requests )
			throw new Closed();
		elements.addLast( new_element );
		notify(); 										//#notify
	}

	/*******************************************************************
	 * Enqueue an item, and thereafter reject any requests to enqueue
	 * additional items. The queue is closed automatically when the
	 * final item is dequeued.
	 */
	@Perm(requires="full(this,PROTOCOL) in OPEN * share(this,STRUCTURE)"
	//,ensures="pure(this,PROTOCOL) in CLOSING" NEB: We could do this...
	// It   
	)
	public synchronized final void enqueue_final_item( Object new_element )	//#final.start
														throws Closed
	{	
		enqueue( new_element );
		reject_enqueue_requests = true;
	}															//#final.end

	/*******************************************************************
	 *	Dequeues an element; blocks if the queue is empty
	 *	(until something is enqueued). Be careful of nested-monitor
	 *  lockout if you call this function. You must ensure that
	 *	there's a way to get something into the queue that does
	 *  not involve calling a synchronized method of whatever
	 *  class is blocked, waiting to dequeue something. A timeout is
	 *	not supported because of a potential race condition (see text).
	 *  You can {@link Thread#interrupt interrupt} the dequeueing thread
	 *  to break it out of a blocked dequeue operation, however.
	 *
	 *  @see #enqueue
	 *  @see #drain
	 *  @see #nonblocking_dequeue
	 *	@return the dequeued object or null if the wait timed out and
	 *			nothing was dequeued.
	 */
	@Perm(requires="share(this!fr,STRUCTURE) * pure(this!fr,PROTOCOL) in OPEN",
		  ensures="share(this!fr,STRUCTURE) * pure(this!fr,PROTOCOL)")
	public synchronized final Object dequeue( ) 
									throws InterruptedException, Closed
	{	
		atomic: { // NEB: Added atomic block
			if( closed )
				throw new Closed();
			try
			{	
				// If the queue is empty, wait. I've put the spin lock inside
				// an if so that the waiting_threads count doesn't jitter
				// while inside the spin lock. A thread is not considered to
				// be done waiting until it's actually acquired an element
	
				if( elements.size() <= 0 )
				{	++waiting_threads;
					//if( elements.size() <= 0 ) NEB: Used for testing
					while( elements.size() <= 0 )
					{	wait();						//#wait
						if( closed )
						{	--waiting_threads;
							throw new Closed();
						}
					}
					--waiting_threads;
				}
	
				Object head = elements.removeFirst();
				if( elements.size() == 0 && reject_enqueue_requests ) {
					reject_enqueue_requests = false; // NEB: New code
					close(); // just removed final item, close the queue.
					return head; // NEB: New code
				}
	
				return head;
			}
			catch( NoSuchElementException e )	// Shouldn't happen
			{	throw new Error(
						"Internal error (com.holub.asynch.Blocking_queue)");
			}
		}
	}
	
	/*******************************************************************
	 *	The is_empty() method is inherently unreliable in a
	 *  multithreaded situation. In code like the following,
	 *	it's possible for a thread to sneak in after the test but before
	 *	the dequeue operation and steal the element you thought you
	 *	were dequeueing.
	 *	<PRE>
	 *	Blocking_queue queue = new Blocking_queue();
	 *	//...
	 *	if( !some_queue.is_empty() )
	 *		some_queue.dequeue();
	 *	</PRE>
	 *	To do the forgoing reliably, you must synchronize on the
	 *	queue as follows:
	 *	<PRE>
	 *	Blocking_queue queue = new Blocking_queue();
	 *	//...
	 *	synchronized( queue )
	 *	{   if( !some_queue.is_empty() )
	 *			some_queue.dequeue();
	 *	}
	 *	</PRE>
	 *	The same effect can be achieved if the test/dequeue operation
	 *	is done inside a synchronized method, and the only way to
	 *	add or remove queue elements is from other synchronized
	 *	methods.
	 */
	 @Pures({
		 @Pure("STRUCTURE"),
		 @Pure(requires="OPEN",ensures="OPEN",value="PROTOCOL")
	 })
	 public final boolean is_empty()
	 {	
		 atomic: { // NEB: Added atomic block
		 	return elements.size() <= 0;
	 	 }
	 }

	/*******************************************************************
	 * Return the number of threads waiting for a message on the
	 * current queue. See {@link is_empty} for warnings about
	 * synchronization.
	 */
     @Pure("STRUCTURE")
	 public final int waiting_threads()
	 {	return waiting_threads;
	 }
	 	
     
     // NEB: I added the is_closed() method... because there wasn't one. :-(
     @Pure(fieldAccess=true,value="PROTOCOL")
     @TrueIndicates("CLOSED")
     @FalseIndicates("OPEN")
     public final synchronized boolean is_closed() {
    	 atomic: { // NEB: Added atomic block
    	 	return closed;
     	 }
     }

	/*******************************************************************
	 * Close the blocking queue. All threads that are blocked
	 * [waiting in dequeue() for items to be enqueued] are released.
	 * The {@link dequeue()} call will throw a {@link Blocking_queue.Closed}
	 * runtime
	 * exception instead of returning normally in this case.
	 * Once a queue is closed, any attempt to enqueue() an item will
	 * also result in a Blocking_queue.Closed exception toss.
	 *
	 * The queue is emptied when it's closed, so if the only references
	 * to a given object are those stored on the queue, the object will
	 * become garbage collectable.
	 */
     @Full(fieldAccess=true,value="PROTOCOL",requires="OPEN",ensures="CLOSED")
	 public synchronized void close()
	 {	
    	 atomic : { // NEB: Added atomic block
    	 	closed 	 = true;
    	 	elements = null;
    	 	notifyAll();
     	}
	 }
}
