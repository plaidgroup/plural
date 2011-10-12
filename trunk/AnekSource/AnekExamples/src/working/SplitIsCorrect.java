package working;

import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Unique;

public final class SplitIsCorrect {

	static void takesImm(@Imm(returned=false) Object oti) {

	}

	static void foo(@Unique(returned=false) Object of) {
		takesImm(of);
		needsSomething(of);
	}

	static void needsSomething(Object ns) {
		needsPure(ns);
	}

	static void needsPure(@Pure  Object np) {
		
	}
	
}
