/**
 * Copyright (C) 2007-2009 Carnegie Mellon University and others.
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
package edu.cmu.cs.plural.syntaxtest;

import edu.cmu.cs.crystal.annotations.FailingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.ResultUnique;
import edu.cmu.cs.plural.annot.States;

/**
 * @author Kevin
 * @since Mar 7, 2009
 *
 */
@FailingTest(6)
@UseAnalyses(PluralAnalysis.SYNTAX)
@States(dim = "A", value = {"x", "y"})
public interface PermissionAnnotationChecks {

	/* @Perm tests */
	
	// error: parameter #0 doesn't exist
	@Perm(requires = "#0 == null")
	public void noParam();
	
	// ok
	@Perm(requires = "#0 == null")
	public void oneParam(Object o);
	
	// error: cannot mention result in pre-condition
	@Perm(requires = "result in open")
	public Object resultInPre();
	
	// two state errors: z not in A, A not in x
	@Perm(requires = "immutable(this!fr, A) in z", ensures = "unique(result, x) in A")
	PermissionAnnotationChecks copyWithWrongStates();
	
	// ok
	@Perm(requires = "immutable(this!fr, A) in x", ensures = "unique(result) in z")
	PermissionAnnotationChecks copy();
	
	// ok
	@Perm(requires = "immutable(this) in z", ensures = "immutable(result, z)")
	PermissionAnnotationChecks identity();
	
	
	/* @Full etc. tests */
	
	// error: no result
	@ResultUnique
	void noResult();
	
	// error: z not in A
	@Full(guarantee = "A", requires = "x", ensures = "z")
	void xToUnknown();
	
	// ok
	@Pure(value = "A", requires = "x", ensures = "y")
	void xToY();
	
}
