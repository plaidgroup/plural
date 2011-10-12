package working;

import edu.cmu.cs.plural.annot.Share;

public class NeedsInterReturn {

	void bar() {
		baz(foo());
	}

	private Object foo() {
		return new Object();
	}

	void baz(@Share Object o) {
	}
	
}
