package working;

import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Unique;

public class SetterTest {

	@Unique()
	void setFav(boolean b) {
	}

	static void main() {
		SetterTest st = new SetterTest();
		st.setFav(false);
		needsPure(st);
	}

	static void needsPure(@Pure() SetterTest o) {
	}

}
