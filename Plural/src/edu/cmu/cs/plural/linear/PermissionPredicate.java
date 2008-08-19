/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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
package edu.cmu.cs.plural.linear;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.concrete.VariablePredicate;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;

/**
 * @author Kevin Bierhoff
 * @since 8/04/2008
 */
public class PermissionPredicate implements VariablePredicate {
	
	private final Aliasing var;
	private final PermissionSetFromAnnotations perm;
	
	PermissionPredicate(Aliasing var, PermissionSetFromAnnotations perm) {
		this.var = var;
		this.perm = perm;
	}

	@Override
	public VariablePredicate createIdenticalPred(Aliasing other) {
		return new PermissionPredicate(other, perm);
	}

	@Override
	public VariablePredicate createOppositePred(Aliasing other) {
		throw new UnsupportedOperationException("Don't know how to negate permission: " + perm);
	}

	@Override
	public boolean denotesBooleanFalsehood() {
		return false;
	}

	@Override
	public boolean denotesBooleanTruth() {
		return false;
	}

	@Override
	public boolean denotesNonNullVariable() {
		return false;
	}

	@Override
	public boolean denotesNullVariable() {
		return false;
	}

	@Override
	public Aliasing getVariable() {
		return var;
	}

	@Override
	public boolean isUnsatisfiable(PluralTupleLatticeElement value) {
		FractionalPermissions valuePs = value.get(var);
		valuePs = valuePs.splitOff(perm);
		return valuePs.isUnsatisfiable();
	}

	@Override
	public PluralTupleLatticeElement putIntoLattice(
			PluralTupleLatticeElement value) {
		FractionalPermissions valuePs = value.get(var);
		valuePs = valuePs.mergeIn(perm);
		value.put(var, valuePs);
		return value;
	}

	@Override
	public boolean isSatisfied(PluralTupleLatticeElement value) {
		return ! isUnsatisfiable(value);
//		FractionalPermissions valuePs = value.get(var);
//		for(FractionalPermission p : perm.isFramePermission() ? valuePs.getFramePermissions() : valuePs.getPermissions()) {
//			if(p.getRootNode().equals(perm.getRootNode())) {
//				FractionAssignment a = valuePs.getConstraints().simplify();
//				Iterator<String> it = perm.getStateSpace().stateIterator(perm.getRootNode());
//				while(it.hasNext()) {
//					String n = it.next();
//					if(! a.areEquivalent(perm.getFractions().get(n), p.getFractions().get(n)))
//						return false;
//				}
//				return a.areEquivalent(perm.getFractions().getBelowFraction(), p.getFractions().getBelowFraction());
//			}
//		}
//		return false;
	}

	/**
	 * @param value
	 */
	public <P extends PluralTupleLatticeElement> P removeFromLattice(P value) {
		FractionalPermissions valuePs = value.get(var);
		valuePs = valuePs.splitOff(perm);
		value.put(var, valuePs);
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((perm == null) ? 0 : perm.hashCode());
		result = prime * result + ((var == null) ? 0 : var.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PermissionPredicate other = (PermissionPredicate) obj;
		if (perm == null) {
			if (other.perm != null)
				return false;
		} else if (!perm.equals(other.perm))
			return false;
		if (var == null) {
			if (other.var != null)
				return false;
		} else if (!var.equals(other.var))
			return false;
		return true;
	}

}
