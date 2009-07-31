package edu.cmu.cs.plural.test;

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.NonReentrant;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Use;

@States({"S1","S2"})
@ClassStates(@State(name="alive", inv="full(myField)"))
@NonReentrant
//@FailingTest(1)
//@UseAnalyses({PluralAnalysis.PLURAL, PluralAnalysis.EFFECT, PluralAnalysis.SYNTAX})
public class Issue17Test {

	private Object myField;
	
	@Full(guarantee="S1", use=Use.FIELDS)
	void canICallPure() {
		needsPure(myField);
	}
	
	private static void needsPure(@Pure Object obj) {}
	
	@Full(guarantee="S1", use=Use.FIELDS)
	void canICallFull() {
		needsFull(myField);
	}
	
	private static void needsFull(@Full Object obj) {}
	
}
