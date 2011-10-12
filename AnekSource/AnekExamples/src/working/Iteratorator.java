package working;

import edu.cmu.cs.plural.annot.Full;

public interface Iteratorator {

	
	@Full(requires="HasNext")
	public Object next();
	
}
