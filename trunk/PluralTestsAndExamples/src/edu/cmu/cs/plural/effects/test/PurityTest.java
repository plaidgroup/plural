package edu.cmu.cs.plural.effects.test;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.NoEffects;

@PassingTest
@UseAnalyses("EffectChecker")
public class PurityTest {
	
	int val = 0;

	@NoEffects
	int getVal() {
		return val;
	}
	
	@NoEffects
	int calc() {
		int x = val + val;
		return x;
	}
	
	void setVal(int val) {
		this.val = val;
	}
	
}
