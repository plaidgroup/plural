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
package edu.cmu.cs.plural.fractions;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.plural.states.StateSpace;

/**
 * Permission sets are immutable and should implement {@link #equals(Object)} and
 * {@link #hashCode()} using structural comparison.
 * @author Kevin Bierhoff
 *
 * @param <P> Type of the constituent permissions.
 */
public abstract class AbstractFractionalPermissionSet<P extends AbstractFractionalPermission> {
	
	private static final Logger log = Logger.getLogger(AbstractFractionalPermissionSet.class.getName());

	/**
	 * Immutable list of permissions, must have orthogonal roots.
	 * @see StateSpace#areOrthogonal(String, String)
	 */
	protected final List<P> permissions;	         // immutable
	
	protected final List<P> framePermissions;		 // immutable
	
	/**
	 * Frozen constraints.
	 * @see FractionConstraints#freeze()
	 */
	protected final FractionConstraints constraints; // frozen; make a mutable copy to modify

	protected AbstractFractionalPermissionSet() {
		super();
		this.permissions = Collections.emptyList();
		this.framePermissions = Collections.emptyList();
		this.constraints = new FractionConstraints().freeze();
	}
	
	protected AbstractFractionalPermissionSet(List<? extends P> permissions) {
		super();
		if(permissions == null) {
			this.permissions = null;
			this.framePermissions = null;
		}
		else {
			this.permissions = Collections.unmodifiableList(permissions);
			this.framePermissions = Collections.emptyList();
			assert checkPermissionSet(permissions);
		}
		this.constraints = new FractionConstraints().freeze();
	}

	protected AbstractFractionalPermissionSet(
			List<? extends P> permissions,
			List<? extends P> framePermissions,
			FractionConstraints constraints) {
		super();
		this.permissions = Collections.unmodifiableList(permissions);
		this.framePermissions = Collections.unmodifiableList(framePermissions);
		this.constraints = constraints.freeze();
		assert checkPermissionSet(permissions);
		assert checkPermissionSet(framePermissions);
	}
	
	private static <P extends AbstractFractionalPermission> boolean checkPermissionSet(List<P> perms) {
		if(perms.size() <= 1)
			return true; // cannot be wrong
		
		Set<String> roots = new LinkedHashSet<String>(perms.size());
		for(P p : perms) {
			for(String s : roots) {
				if(p.getStateSpace().areOrthogonal(p.getRootNode(), s) == false) {
					log.warning("Conflicting permissions: " + perms);
					return false;
				}
			}
			if(roots.add(p.getRootNode()) == false)
				// shouldn't happen after the previous check
				throw new IllegalStateException();
		}
		return true;
	}

	/**
	 * Returns <code>true</code> if there are no permissions in this set.
	 * @return <code>true</code> if there are no permissions in this set, <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return permissions.isEmpty() && framePermissions.isEmpty();
	}

	public List<P> getPermissions() {
		return permissions;
	}
	
	/**
	 * @return
	 */
	public List<P> getFramePermissions() {
		return framePermissions;
	}

	public FractionConstraints getConstraints() {
		return constraints;
	}

	public abstract StateSpace getStateSpace();

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((constraints == null) ? 0 : constraints.hashCode());
		result = prime
				* result
				+ ((framePermissions == null) ? 0 : framePermissions.hashCode());
		result = prime * result
				+ ((permissions == null) ? 0 : permissions.hashCode());
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
		final AbstractFractionalPermissionSet other = (AbstractFractionalPermissionSet) obj;
		if (constraints == null) {
			if (other.constraints != null)
				return false;
		} else if (!constraints.equals(other.constraints))
			return false;
		if (framePermissions == null) {
			if (other.framePermissions != null)
				return false;
		} else if (!framePermissions.equals(other.framePermissions))
			return false;
		if (permissions == null) {
			if (other.permissions != null)
				return false;
		} else if (!permissions.equals(other.permissions))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result
//				+ ((constraints == null) ? 0 : constraints.hashCode());
//		result = prime * result
//				+ ((permissions == null) ? 0 : permissions.hashCode());
//		return result;
//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		AbstractFractionalPermissionSet other = (AbstractFractionalPermissionSet) obj;
//		if (constraints == null) {
//			if (other.constraints != null)
//				return false;
//		} else if (!constraints.equals(other.constraints))
//			return false;
//		if (permissions == null) {
//			if (other.permissions != null)
//				return false;
//		} else if (!permissions.equals(other.permissions))
//			return false;
//		return true;
//	}

}