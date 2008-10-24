/**
 * Copyright (c) 2008 by Kevin Bierhoff
 */
package edu.cmu.cs.plural.examples;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.FalseIndicates;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;

/**
 * This class illustrates how data secrecy classifications can be
 * encoded with marker states in Plural.
 * @author Kevin Bierhoff
 * @since June 11, 2008
 */
@PassingTest
@UseAnalyses("FractionalAnalysis")
@Refine({
	@States(value = {"confidential"}, refined = "alive", marker = true),
	@States(value = {"secret", "public"}, refined = "confidential", marker = true)
})
public abstract class SecurityObject {
	
	@Imm(requires = "public")
	public abstract void sendToNewsAgencies();
	
	@Pure
	@TrueIndicates("secret")
	@FalseIndicates("public")
	public abstract boolean isSecret();
	
	@Imm
	public void maybePublish() {
		if(! isSecret()) {
			sendToNewsAgencies();
		}
	}

}
