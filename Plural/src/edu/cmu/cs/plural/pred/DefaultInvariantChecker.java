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
package edu.cmu.cs.plural.pred;

import java.util.Set;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.linear.AbstractPredicateChecker;
import edu.cmu.cs.plural.linear.TensorPluralTupleLE;

/**
 * This class is an analogue to DefaultInvariantMerger. It provides
 * the call-back functionality, SplitOffTuple, that will be used to
 * check that all invariants are satisfied at pack-time.
 * 
 * @author Nels E. Beckman
 * @since Sep 26, 2008
 * @see {@link DefaultInvariantMerger}
 */
public class DefaultInvariantChecker extends AbstractPredicateChecker {

	private final boolean purify;
	
	public DefaultInvariantChecker(TensorPluralTupleLE value,
			Aliasing thisLoc, boolean purify) {
		super(value, thisLoc);
		this.purify = purify;
	}

	@Override
	public boolean splitOffPermission(Aliasing var, String var_name,
			PermissionSetFromAnnotations perms) {
		// Purify if we need to.
		return super.splitOffPermission(var, var_name, purify ? perms.purify() : perms);
	}

	// Weirdo methods...

	@Override
	public void announceBorrowed(Set<Aliasing> borrowedVars) {
		Utilities.nyi("I wasn't ever expecting this method to be called.");		
	}

	@Override
	public boolean finishSplit() {
		return true;
	}
}
