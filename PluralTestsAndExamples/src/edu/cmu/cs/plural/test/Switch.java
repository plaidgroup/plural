/**
 * Copyright (c) 2008 by Kevin Bierhoff & Nels Beckman
 */
package edu.cmu.cs.plural.test;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Cases;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Use;

/**
 * This is a mock interface for tests with concrete predicates
 * in method pre-/post-condtions
 * @author Kevin Bierhoff
 * @see ConcreteTests
 * @see FailingConcreteTests
 */
@PassingTest
@UseAnalyses("FractionalAnalysis")
@ClassStates(@State(name = "on", inv = "f_on == true"))
public class Switch {

	private boolean f_on;

	@Cases({
		@Perm(requires = "#0 == true", ensures = "this!fr in on"),
		@Perm(requires = "#0 == false", ensures = "this!fr in off")
	})
	@Full(use = Use.FIELDS)
	public void setState(boolean on) {
		this.f_on = on;
	}
	
	@Perm(ensures = "result == true => this!fr in on")
	@Pure(use = Use.FIELDS)
	public boolean isOn() {
		return f_on;
	}
	
	@Full(requires = "on", ensures = "on")
	public void requiresOn() {
		requiresOn();
	}

}