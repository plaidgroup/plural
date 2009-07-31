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
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.ResultShare;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Use;


@Refine({@States(dim="Dim1", value={"Initialized1"}),
		 @States(dim="Dim2", value={"Initialized2"})})
@ClassStates({@State(name="Initialized1", inv="share(myField1,Init)"),
	@State(name="Initialized2", inv="share(myField2,Init)")})
@UseAnalyses({PluralAnalysis.PLURAL, PluralAnalysis.SYNTAX, PluralAnalysis.EFFECT})
@PassingTest
public class DimensionsForMonotonicInit {

	Initializable myField1;
	Initializable myField2;
	
	@Perm(requires="unique(this!fr)")
	void init() {
		this.myField1 = new Initializable();
		this.myField2 = new Initializable();
		this.myField1.init();
		this.myField2.init();
	}

	@Perm(requires="share(#0, Initialized1) * share(#0, Initialized2)")
	static void finishedWithInit(DimensionsForMonotonicInit dfmi) {
		dfmi.getField1();
		dfmi.getField2();
	}
	
	@Share(guarantee="Initialized1", use=Use.FIELDS)
	@ResultShare(guarantee="Init")
	Initializable getField1() {
		return this.myField1;
	}
	
	@Share(guarantee="Initialized2", use=Use.FIELDS)
	@ResultShare(guarantee="Init")
	Initializable getField2() {
		return this.myField2;
	}
	
}

class Initializable {
	
	@Perm(ensures="unique(this!fr)")
	public Initializable() {
		
	}
	
	@Full(ensures="Init", use=Use.FIELDS)
	void init() {}
}
