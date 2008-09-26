package edu.cmu.cs.plural.test;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Unique;

/*
 * In this test, we see if we can use a state implication in a
 * state invariant.
 */
@PassingTest
@UseAnalyses("FractionalAnalysis")
@ClassStates(@State(name="SPECIAL", 
		     inv="full(data) * hasAState == true => data in ASTATE"))
public class ImplicationWorks2 {
	LastObject data;
	boolean hasAState;
	
	// Should work fine
	@Unique(fieldAccess=true,requires="SPECIAL")
	void foo() {
		if( this.hasAState ) {
			data.mustBeInAState();
		}
	}
	
	private class LastObject {
		@Perm(ensures="unique(this!fr)")
		LastObject() {
			
		}
		@Full
		void modify() {
			
		}
		
		@Full(requires="ASTATE")
		void mustBeInAState() {
			
		}
	}
}

