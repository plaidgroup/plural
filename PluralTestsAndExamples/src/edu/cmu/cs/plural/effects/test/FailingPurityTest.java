package edu.cmu.cs.plural.effects.test;

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.NoEffects;

@FailingTest(6)
@UseAnalyses("EffectChecker")
public class FailingPurityTest {
	
	static int global = 0;
	int val = 0;
	
	@NoEffects
	FailingPurityTest() {
		global++;
	}
	
	@NoEffects
	FailingPurityTest(FailingPurityTest original) {
		original.val = val;
		val = original.val;
	}

	@NoEffects
	int getVal() {
		return val;
	}
	
	@NoEffects
	int calc() {
		val = val + val;
		return val;
	}
	
	@NoEffects
	int inc() {
		return ++val;
	}
	
	@NoEffects
	void setVal(int val) {
		this.val = val;
	}
	
	@NoEffects
	void clear() {
		subtract(val);
	}
	
	void subtract(int value) {
		val = val - value;
	}

}
