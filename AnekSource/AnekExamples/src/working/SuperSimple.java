package working;

import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.Unique;

public final class SuperSimple {
	static void foo(Object o) {
		bar(o);
	}

	static void bar(@Full Object o) {

	}

	static void foo2(Object o) {
		bar2(o);
	}

	static void bar2(@Imm Object o) {

	}
	
	static void foo3(Object o) {
		bar3(o);
	}

	static void bar3(@Pure Object o) {

	}
	
	static void foo4(Object o) {
		bar4(o);
	}

	static void bar4(@Unique Object o) {

	}
	
	static void foo5(Object o) {
		bar5(o);
	}

	static void bar5(@Share Object o) {

	}
	
}
