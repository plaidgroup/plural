package edu.cmu.cs.nimby.test.interesting;
import com.holub.asynch.Blocking_queue;

import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Unique;


public class OneReaderOneWrtier {

	public static void main(String[] args) {
		
		Blocking_queue queue = new Blocking_queue();
		
		(new Reader(queue)).start();
		
		for(int i = 0;i<5;i++) {
			queue.enqueue("OBJECT: " + i);
		}
		
		queue.close();
	}
	
	@ClassStates(@State(name="alive", 
			            inv="pure(queue,PROTOCOL) * share(queue,STRUCTURE)"))
	public static class Reader extends Thread {
		
		private final Blocking_queue queue;
				
		@Perm(requires="pure(#0,PROTOCOL) * share(#0,STRUCTURE)",
			  ensures="unique(this!fr)")
		public Reader(Blocking_queue queue) {
			this.queue = queue;
		}

		@Override
		@Unique(fieldAccess=true)
		public void run() {
			
			boolean was_closed = false;
			do {
				Object obj = null;
				atomic: {
					was_closed = queue.is_closed();
					
					if( !was_closed ) {
						try{obj = queue.dequeue();} 
						  catch(InterruptedException e){} 
						  // NEB: Won't be necessary once we're using retry 
					}
					else {
						obj = null;
					}
				}
				System.out.println("Got item: " + obj);
			} while(!was_closed);
			
		}

		@Override
		@Unique(returned=false)
		public synchronized void start() {
			super.start();
		}
	}
	
}
