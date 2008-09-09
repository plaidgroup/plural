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

import java.util.LinkedHashSet;
import java.util.Set;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;

/**
 * @author Kevin Bierhoff
 * @since 8/04/2008
 */
public class ParamInfoHolder {
	
	private enum Primitive { NULL, NONNULL, FALSE, TRUE, SEVERAL };
	
	private PermissionSetFromAnnotations perms;
	
	private Set<String> stateInfo = new LinkedHashSet<String>();
	
	private Primitive prim;
	
	/**
	 * @return the perms
	 */
	public PermissionSetFromAnnotations getPerms() {
		return perms;
	}

	/**
	 * @param perms the perms to set
	 */
	public void setPerms(PermissionSetFromAnnotations perms) {
		this.perms = perms;
	}

	/**
	 * @return the stateInfos
	 */
	public Set<String> getStateInfos() {
		return stateInfo;
	}

	/**
	 * @param pa
	 */
	public void addPerm(PermissionFromAnnotation pa) {
		if(perms == null)
			perms = PermissionSetFromAnnotations.createSingleton(pa);
		else
			perms = perms.combine(pa);
	}

	/**
	 * @param value
	 * @param var
	 */
	public void putIntoLattice(PluralTupleLatticeElement value,
			Aliasing var) {
		assert var != null;
		
		if(prim != null) {
			switch(prim) {
			case NULL:
				value.addNullVariable(var);
				// null implies bottom permissions -> do nothing else
				return;
			case NONNULL:
				value.addNonNullVariable(var);
				break;
			case TRUE:
				value.addTrueVarPredicate(var);
				break;
			case FALSE:
				value.addFalseVarPredicate(var);
				break;
			default:
				// nothing
			}
		}
		
		FractionalPermissions ps = value.get(var);
		ps = ps.mergeIn(perms);
		
		for(String s : stateInfo) {
			// TODO learn all states at once
			ps = ps.learnTemporaryStateInfo(s);
		}
		
		value.put(var, ps);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((perms == null) ? 0 : perms.hashCode());
		result = prime * result + ((prim == null) ? 0 : prim.hashCode());
		result = prime * result
				+ ((stateInfo == null) ? 0 : stateInfo.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ParamInfoHolder other = (ParamInfoHolder) obj;
		if (perms == null) {
			if (other.perms != null)
				return false;
		} else if (!perms.equals(other.perms))
			return false;
		if (prim == null) {
			if (other.prim != null)
				return false;
		} else if (!prim.equals(other.prim))
			return false;
		if (stateInfo == null) {
			if (other.stateInfo != null)
				return false;
		} else if (!stateInfo.equals(other.stateInfo))
			return false;
		return true;
	}

}
