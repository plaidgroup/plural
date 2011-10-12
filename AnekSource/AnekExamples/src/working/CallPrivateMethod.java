package working;

import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Use;

public class CallPrivateMethod {

	
	public void foo() {
		this.thisMethodIsPrivate();
	}

	@Full(use = Use.FIELDS)
	private void thisMethodIsPrivate() {}

}
