package notsogood;

import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.ResultPure;

// Methods that start with create are more likely to return 'unique
// permissions!
public class CreateMethod {

	public static Object createObject() {
		return new Object();
	}

	static void main() {
		needsPure(createObject());
	}

	static void needsPure(@Pure() Object o) {

	}

}
