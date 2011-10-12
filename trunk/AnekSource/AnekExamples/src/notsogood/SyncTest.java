package notsogood;

import edu.cmu.cs.plural.annot.Imm;

public class SyncTest {

	synchronized void foo() {
		// should force foo receiver to be full.
		bar(); 
	}
	
	@Imm
	void bar() {
		
	}
	
	void foo2() {
		synchronized(this) {
			// should force foo receiver to be full.
			bar(); 
		}
	}
	
}
