package edu.cmu.cs.plural.test;

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Full;

@FailingTest(3)
@UseAnalyses("FractionalAnalysis")
public class FailingConcreteTests {

	public static void failingConcreteTest1(@Full Switch s) {
		s.setState(false);
		s.requiresOn();
	}

	public static void failingConcreteTest2(@Full Switch s) {
		s.requiresOn();
	}

	public static void failingImplicationTest(@Full Switch s) {
		if(! s.isOn())
			s.requiresOn();
		else
			s.requiresOn();
	}

}
