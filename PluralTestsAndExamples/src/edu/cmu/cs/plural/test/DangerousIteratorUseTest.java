package edu.cmu.cs.plural.test;

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Unique;

@FailingTest(3)  
// was 5 when reporting state and permission failures separately
// should be 4 because one problem (need full for second parameter to concat)
// was reported twice before
@UseAnalyses("FractionalAnalysis")
public class DangerousIteratorUseTest {

	public static void dangerousIteratorUse(@Unique String[] strings) {
		ArrayIterator<String> it = 
			new ArrayIterator<String>(strings);
		while(it.hasNext()) {
			ArrayIterator.concat(it, it); 
			// error(s) around here because concat() needs full and share of it
			System.out.println(it.next());
		}
		// next line can have an error because of errors using "it" in the loop
		// errors may or may not propagate out of the loop
		it.dispose(); 
		strings[0] = "goodbye";
	}

}
