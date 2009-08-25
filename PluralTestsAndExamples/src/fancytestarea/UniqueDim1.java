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
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.Unique;

/**
 * 1st test of what I am currently calling 'unique dimensions.'
 * This shows that we can go from one @Unique permission, from
 * a constructor, to two @Unique permissions for dimensions.
 *  
 * @author Nels E. Beckman
 * @since Aug 25, 2009
 *
 */
@Refine({
	@States(dim="SWEET", refined="alive", value={"SweetState"}),
	@States(dim="SOUR", refined="alive", value={"SourState"})
})
@UseAnalyses(PluralAnalysis.PLURAL)
@PassingTest
public class UniqueDim1 {

	/*
	 * Now something a little more tricky.
	 * Can we divide a perfectly good Unique permission into
	 * two unique dims?
	 */
	static void createAndSplit() {
		UniqueDim1 new_object = new UniqueDim1();
		consumeSweet(new_object);
		consumeSour(new_object);
	}
	
	/*
	 * A very weak first test...
	 */
	static void foo(@Unique(guarantee="SWEET", justToRoot=true) 
			UniqueDim1 ud1 ) {
		bar(ud1);
	}
	
	static void bar(@Unique(guarantee="SWEET", justToRoot=true) 
			UniqueDim1 ud1) {}
	
	
	
	@Perm(ensures="unique(this!fr)")
	public UniqueDim1() {
		
	}
	
	static void consumeSweet(
			@Unique(guarantee="SWEET", justToRoot=true, returned=false)
			UniqueDim1 arg1) {}
	
	static void consumeSour(
			@Unique(guarantee="SOUR", justToRoot=true, returned=false)
			UniqueDim1 arg1) {}	
}