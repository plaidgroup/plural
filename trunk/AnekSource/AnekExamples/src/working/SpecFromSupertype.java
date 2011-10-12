package working;

public class SpecFromSupertype {

	void main() {
		IterSub is = new IterSub();
		is.next();
	}
	
}


class IterSub implements Iteratorator {

	public IterSub() {
		
	}
	
	@Override
	public Object next() {
		throw new RuntimeException("NYI");
	}
	
}
