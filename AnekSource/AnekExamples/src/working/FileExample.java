package working;

import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Use;

public final class FileExample {

	static void doIt(
			@Full(requires = "ANONYMOUS_DIM_3", ensures = "ANONYMOUS_DIM_3") File f) {
		f.open();
	}

}

class File {
	@Full(use = Use.FIELDS, requires = "ANONYMOUS_DIM_3", ensures = "ANONYMOUS_DIM_3")
	void open() {

	}

}
