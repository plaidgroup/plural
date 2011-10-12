package working;

import edu.cmu.cs.plural.annot.Full;

/**
 * This test needs an interprocedural analysis!
 * @author t-nelbec
 *
 */
public class NeedsInterProc {

	void foo(Object o) {
		bar(o);
	}

	void bar(Object o) {
		baz(o);
	}

	void baz(@Full  Object o) {
	}
	
}
