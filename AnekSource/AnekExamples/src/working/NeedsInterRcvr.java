package working;

import edu.cmu.cs.plural.annot.Share;

public class NeedsInterRcvr {
	void foo() {
		bar();
	}

	void bar() {
		baz();
	}

	@Share
	void baz() {
	}
}
