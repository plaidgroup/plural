package working;

import edu.cmu.cs.plural.annot.Unique;

/**
 * We are adding a new constraint that says the specification on a sub-method
 * should equal the specification on the super method (and importantly, this
 * will apply even for UNGROUND permissions).
 * 
 * This test will explore that new constraint.
 * 
 * @author Nels E. Beckman
 *
 */
public abstract class SuperEqualsSub {
	public abstract Object thisIsAMethod(Object o);
	
	public static void main(SuperEqualsSub ses) {
		needsUnique(ses.thisIsAMethod(null));
	}
	
	static void needsUnique(@Unique Object o) {}
}


class Sub extends SuperEqualsSub {

	@Override
	public Object thisIsAMethod(Object o) {
		throw new RuntimeException("NYI");
	}
}

class SubitySub extends Sub {

	@Override
	public Object thisIsAMethod(Object o) {
		throw new RuntimeException("NYI");
	}
}