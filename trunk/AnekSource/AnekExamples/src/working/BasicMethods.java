package working;

import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Share;

public class BasicMethods {

	Object foo(Object x, Object y) {
		bar(x);

		while (Math.random() == 2.0f) {
			bar(y);
		}

		y = x;
		return baz(y);

	}

	void bar(@Share Object o) {
	}

	Object baz(@Pure  Object o) {
		return new Object();
	}
	
}
