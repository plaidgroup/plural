package edu.cmu.cs.plural.test;

import edu.cmu.cs.plural.annot.Capture;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.ResultUnique;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Unique;
import edu.cmu.cs.plural.annot.Use;

@States({"init"})
@ClassStates({@State(name="init",inv="unique(getsCaptured) in Open")})
public final class CaptureField {
	private GetsCaptured getsCaptured;
	
	@Unique(requires="init", ensures="init", use=Use.FIELDS)
	public void makeCapturerDoIt() {
		Capturer c = this.getsCaptured.createNewCapturer();
		c.doIt();
	}
}

@States({"Open"})
class GetsCaptured {
	
	@Capture(param="underlying")
	@ResultUnique
	@Unique(requires="Open", returned=false)
	public Capturer createNewCapturer() {
		return new Capturer();
	}
	
}

class Capturer {
	
	@Perm(ensures="unique(this!fr)")
	public Capturer(){}
	
	@Unique
	void doIt() {}
	
}
