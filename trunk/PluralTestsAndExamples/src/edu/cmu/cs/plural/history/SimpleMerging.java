package edu.cmu.cs.plural.history;

import edu.cmu.cs.plural.annot.Cases;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.NoEffects;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.States;

@Refine({
@States(value={"Cool", "Uncool"})		
})
public class SimpleMerging {

	// Last time I checked, analysis of this method looks pretty
	// good.
	@Full(ensures="Cool")
	void ifMerge(boolean toCheck) {
		if( toCheck ) {
			makeCool();
		}
		else {
			makeUncool();
		}
		
		doNothing();
	}

	// Basically looks the same, which makes me believe that if there
	// are no choices, things look pretty good.
	@Full(ensures="Cool")
	void whileMerge(boolean toCheck, boolean checkAgain) {
		while( toCheck ) {
			if( checkAgain )
				makeCool();
			else
				makeUncool();
		}
		
		
		doNothing();
	}

	@Full(ensures="Uncool") private void makeUncool() { }

	@Full(ensures="Cool") private void makeCool() {}
	
	@NoEffects static void doNothing() {}

	@Full
	void ifChoiceOneBranch(boolean check) {
		if( check ) {
			this.twoPrinces();
		}
		
		doNothing();
	}
	
	@Full
	void whileChoice(boolean check) {
		while( check ) {
			this.twoPrinces();
		}
		
		doNothing();
	}
	
	@Cases({
		@Perm(requires="full(this)", ensures="full(this) in Cool"),
		@Perm(requires="immutable(this)", ensures="immutable(this) in Uncool")
	})
	void twoPrinces() { }
}
