package fancytestarea;

import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Use;

/**
 * It's a little difficult to explain what this test does, but
 * here goes: If a receiver is unpacked and then packed in
 * one method and then unpacked again, those separate unpackings
 * should not have to occur within the same synchronized block.
 * I think that as of the time this test is created, there is
 * a bug so that this is not the case.
 * 
 * Update: Actually, this did work already!
 * 
 * @author Nels E. Beckman
 * @since Aug 26, 2009
 *
 */
@ClassStates(@State(name="alive", inv="share(myObject)"))
public class NotContinuousUnpacked {
	
	@SuppressWarnings("unused")
	private StupidObject myObject;
	
	@Full(use=Use.FIELDS)
	void unpackTwice() {
		
		synchronized(this) {
			// First unpack.
			this.myObject = new StupidObject();
			// Force pack.
			forcePack();
		}
		
		synchronized(this) {
			// Second unpack.
			this.myObject = new StupidObject();
		}
		
	}
	
	static void forcePack() {};
	
}

class StupidObject {
	@Share void doIt() {}
	
	@Perm(ensures="unique(this!fr)")
	StupidObject() {}
}
