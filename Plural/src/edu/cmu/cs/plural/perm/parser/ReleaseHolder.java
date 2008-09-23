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
package edu.cmu.cs.plural.perm.parser;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;

/**
 * @author Kevin Bierhoff
 * @since Sep 15, 2008
 */
public class ReleaseHolder {

	private final PermissionSetFromAnnotations perms;
	
	private final String releasedFromState;
	
	public ReleaseHolder(PermissionSetFromAnnotations perms) {
		this.perms = perms != null ? perms.withoutStateInfo() : null;
		this.releasedFromState = StateSpace.STATE_ALIVE;
	}
	
	public ReleaseHolder(PermissionSetFromAnnotations perms, String releasedFromState) {
		assert releasedFromState != null;
		this.perms = perms != null ? perms.withoutStateInfo() : null;
		this.releasedFromState = releasedFromState;
	}
	
	/**
	 * @param value Lattice to put captured permissions into.
	 * @param var Object for which permissions are captured in this release holder.
	 * @param capturingVar Object capturing the permissions in this release holder.
	 * @param force <code>true</code> will release the captured permission even
	 * if <code>capturingVar</code> is not in a state that releases the captured permission.
	 */
	public void putIntoLattice(PluralTupleLatticeElement value,
			Aliasing var, Aliasing capturingVar, boolean force) {
		if(perms == null)
			return; // nothing to release
		assert var != null && capturingVar != null;

		if(! value.get(capturingVar).isInState(releasedFromState))
			return;
		
		FractionalPermissions ps = value.get(var);
		ps = ps.mergeIn(perms);
		value.put(var, ps);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((perms == null) ? 0 : perms.hashCode());
		result = prime
				* result
				+ ((releasedFromState == null) ? 0 : releasedFromState
						.hashCode());
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
		ReleaseHolder other = (ReleaseHolder) obj;
		if (perms == null) {
			if (other.perms != null)
				return false;
		} else if (!perms.equals(other.perms))
			return false;
		if (releasedFromState == null) {
			if (other.releasedFromState != null)
				return false;
		} else if (!releasedFromState.equals(other.releasedFromState))
			return false;
		return true;
	}
	
}
