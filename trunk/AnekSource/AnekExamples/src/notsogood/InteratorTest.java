package notsogood;

import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;
import edu.cmu.cs.plural.annot.Unique;

/**
 * This test is 'cleverly' named.
 * 
 * It is an interprocedural test for the iterator inference
 * which we are currently trying to verify for PMD.
 * 
 * @author Nels E. Beckman
 *
 */
public class InteratorTest {

//	static void shouldNeedFullThree(Iterator<String> i) {
//		shouldNeedFullToo(i);
//	}

	static void shouldNeedFullToo(Iterator<String> i) {
		shouldNeedFull(i);
	}

	static void shouldNeedFull(Iterator<String> i) {
		while (i.hasNext()) {
			i.next();
		}
	}

}

@States({ "HasNext", "DoesNotHasNext" })
abstract class Iterator<E> {
	@Pure()  
	@TrueIndicates("HasNext")
	public abstract boolean hasNext();

	@Full(requires="HasNext")  
	public abstract E next();
}
