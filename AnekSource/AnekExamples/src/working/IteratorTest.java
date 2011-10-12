package working;

import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;

/**
 * Time to test the iterator. A real problem we are having is that
 * if the hasNext() test only requires a pure, but the next requires
 * a full, we still want the full to propagate forward...
 * 
 * @author Nels E. Beckman
 *
 */
public class IteratorTest {

	void shouldNeedFull(Iterator<String> i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	String shouldNeedHasNext(Iterator<String> i) {
		return i.next();
	}
}

@States({ "HasNext", "DoesNotHasNext" })
abstract class Iterator<E> {
	@Pure()
	@TrueIndicates("HasNext")
	public abstract boolean hasNext();

	@Full(requires = "HasNext")
	public abstract E next();
}
