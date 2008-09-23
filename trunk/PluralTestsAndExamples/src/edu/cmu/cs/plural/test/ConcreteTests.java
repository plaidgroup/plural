package edu.cmu.cs.plural.test;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Full;

@PassingTest
@UseAnalyses("FractionalAnalysis")
public class ConcreteTests {

	public static void concreteTest(@Full Switch s) {
		s.setState(true);
		s.requiresOn();
	}

	public static void implicationTest(@Full Switch s) {
		if(s.isOn())
			s.requiresOn();
	}

}
