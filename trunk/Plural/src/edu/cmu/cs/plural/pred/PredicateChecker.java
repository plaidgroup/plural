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
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * 
 * @author Kevin Bierhoff
 * @since Sep 10, 2008
 *
 */
public interface PredicateChecker {

	/**
	 * Callback interface.
	 * @author Kevin Bierhoff
	 * @since Sep 10, 2008
	 *
	 */
	public interface SplitOffTuple {
		/**
		 * Check that the given object is in the given states.
		 * @param var 
		 * @param stateInfos
		 * @param inFrame
		 * @return <code>true</code> if checking should continue, 
		 * <code>false</code> to fail the check.
		 */
		boolean checkStateInfo(Aliasing var, Set<String> stateInfos, boolean inFrame);

		/**
		 * Check that the given variable is definitely <code>null</code>.
		 * @param var
		 * @return <code>true</code> if checking should continue, 
		 * <code>false</code> to fail the check.
		 */
		boolean checkNull(Aliasing var);

		/**
		 * Check that the given variable is definitely <b>not</b> <code>null</code>.
		 * @param var
		 * @return <code>true</code> if checking should continue, 
		 * <code>false</code> to fail the check.
		 */
		boolean checkNonNull(Aliasing var);

		/**
		 * Check that the given variable is definitely <code>true</code>.
		 * @param var
		 * @return <code>true</code> if checking should continue, 
		 * <code>false</code> to fail the check.
		 */
		boolean checkTrue(Aliasing var);

		/**
		 * Check that the given variable is definitely <code>false</code>.
		 * @param var
		 * @return <code>true</code> if checking should continue, 
		 * <code>false</code> to fail the check.
		 */
		boolean checkFalse(Aliasing var);

		/**
		 * Check that the given implication is satisfied.
		 */
		boolean checkImplication(Aliasing var, Implication impl);
		
		/**
		 * Split the given permissions off the given object.
		 * @param var
		 * @param perms
		 * @return <code>true</code> if checking should continue, 
		 * <code>false</code> to fail the check.
		 */
		boolean splitOffPermission(Aliasing var, PermissionSetFromAnnotations perms);

		/**
		 * Post-processing, e.g., of delayed checks.
		 * @return <code>true</code> if checking should continue, 
		 * <code>false</code> to fail the check.
		 */
		boolean finishSplit();

		/**
		 * The given set of objects are borrowed in the method call being processed.
		 * This information may allow special treatment of borrowed objects.
		 * @param borrowedVars
		 */
		void announceBorrowed(Set<Aliasing> borrowedVars);

	}

	/**
	 * Call this method to check this predicate.  This check can be called multiple times.
	 * @param vars Maps parameter names to locations.  Expects <code>this</code>, 
	 * <code>this!fr</code>, and <code>result</code> to be defined where applicable.
	 * Parameters are numbered with a preceding hash sign.
	 * @param callback
	 * @return <code>false</code> if the check failed,
	 * <code>true</code> otherwise.
	 * 
	 */
	public boolean splitOffPredicate(SimpleMap<String, Aliasing> vars,
			SplitOffTuple callback);

}