/**
 * Copyright (C) 2007, 2008 Carnegie Mellon University and others.

 *
 * This file is part of Plural.
 *
 * Plural is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * Plural is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Plural; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking Plural statically or dynamically with other modules is
 * making a combined work based on Plural. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of Plural
 * give you permission to combine Plural with free software programs or
 * libraries that are released under the GNU LGPL and with code
 * included in the standard release of Eclipse under the Eclipse Public
 * License (or modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the
 * GNU GPL for Plural and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of Plural are not
 * obligated to grant this special exception for their modified
 * versions; it is their choice whether to do so. The GNU General
 * Public License gives permission to release a modified version
 * without this exception; this exception also makes it possible to
 * release a modified version which carries forward this exception.
 */
package fancytestarea;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Unique;
import edu.cmu.cs.plural.annot.Use;



/**
 * Here we want to show that if we unpack a 'unique dimension'
 * that we do not need to protect it with mutual exclusion.
 * 
 * @author Nels E. Beckman
 * @since Aug 26, 2009
 *
 */
@Refine({
	@States(dim="TLOCAL", value={"TLocal"}),
	@States(dim="TSHARED", value="TShared")
})
/* Admittedly this test is kind of dumb because the fields themselves are
 * shared... */
@ClassStates({@State(name="TLOCAL", inv="share(myFile)"),
		      @State(name="TSHARED", inv="share(mySharedFile)")})
@PassingTest
@UseAnalyses({PluralAnalysis.SOS, PluralAnalysis.SOS_PRE, 
	PluralAnalysis.EFFECT, PluralAnalysis.SYNTAX})
public class UniqueDimNoProtection {

	private File myFile;
	
	private File mySharedFile;
	
	@Perm(ensures="unique(this!fr)")
	UniqueDimNoProtection() {
		this.myFile = new File();
		this.mySharedFile = new File();
	}
	
	/*
	 * I will attempt to make this example as close to the thread
	 * example as possible. Here we create a new thread, consume
	 * a permission to the shared dimension, and then call run,
	 * which requires unique permission to the thread local dimension.
	 */
	static void createThread() {
		UniqueDimNoProtection new_thread = new UniqueDimNoProtection();
		consumeShareDim(new_thread);
		new_thread.run();
	}
	
	static void consumeShareDim(@Share(guarantee="TSHARED", returned=false)
			UniqueDimNoProtection thread) {}
	
	@Unique(guarantee="TLOCAL", use=Use.FIELDS)
	@Share(guarantee="TSHARED", use=Use.FIELDS)
	void run() {
		/*
		 * Do things to force an unpack so that we know 
		 * we do not need synchronization
		 */
		this.myFile.doSomething();
		this.myFile = new File();
		this.myFile.doSomething();
		
		// Plural does not try to pack a unique permission
		// before a method call, not even a 'unique dimension'
		// one. This makes perfect sense in my opinion, and it
		// makes writing methods like this a bit more complicated
		// but still do-able.
		this.forcePack();
		
		synchronized(this) {
			this.mySharedFile.doSomething();
			this.mySharedFile = new File();
			this.mySharedFile.doSomething();
		}
	}
	
	@Unique(guarantee="TLOCAL", use=Use.FIELDS)
	private void forcePack() {}
}

class File {
	
	@Perm(ensures="unique(this!fr)")
	File() {}
	
	@Share void doSomething() {};
}