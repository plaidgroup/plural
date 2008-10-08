package edu.cmu.cs.plural.effects.test;

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.NoEffects;

@FailingTest(3)
@UseAnalyses("EffectChecker")
public class FailingPurityTest {
	
	int val = 0;

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
		return val++;
	}
	
	@NoEffects
	void setVal(int val) {
		this.val = val;
	}

}
