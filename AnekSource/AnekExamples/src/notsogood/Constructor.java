package notsogood;

import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;

public class Constructor {

	boolean field;

	public Constructor() {
		field = true;
	}

	public static void main(String[] args) {
		Constructor c = new Constructor();
		needsFull(c);
	}

	public static void needsFull(@Full() Object o) {
	}
}
