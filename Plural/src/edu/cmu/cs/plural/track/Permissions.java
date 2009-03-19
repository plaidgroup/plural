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
package edu.cmu.cs.plural.track;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.simple.LatticeElement;

/**
 * @author Kevin
 *
 */
public class Permissions implements LatticeElement<Permissions> {
	
	private static final Logger log = Logger.getLogger(Permissions.class.getName());
	
	public static final Permissions BOTTOM = new Permissions();
	
	private Set<Permission> permissions;

	/**
	 * 
	 */
	public Permissions() {
		super();
		this.permissions = Collections.emptySet();
	}
	
	private Permissions(Set<Permission> permissions) {
		super();
		this.permissions = Collections.unmodifiableSet(permissions);
	}
	
	/**
	 * @param p
	 */
	private Permissions(Permission p) {
		super();
		this.permissions = Collections.singleton(p);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	public boolean atLeastAsPrecise(Permissions other, ASTNode node) {
		return other.permissions.containsAll(this.permissions);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#copy()
	 */
	public Permissions copy() {
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#join(edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	public Permissions join(Permissions other, ASTNode node) {
		HashSet<Permission> joinedPermissions = new HashSet<Permission>();
		for(Permission p : this.permissions)
			for(Permission q : other.permissions) {
				Permission j = p.join(q);
				if(j != null) joinedPermissions.add(j);
			}
		//joinedPermissions.addAll(this.permissions);
		//joinedPermissions.retainAll(other.permissions);
		return new Permissions(joinedPermissions);
	}
	
	@Override
	public String toString() {
		return permissions.toString();
	}

	/**
	 * @param singletonElem
	 * @return
	 */
	public static Permissions createSingleton(Permission singletonElem) {
		return new Permissions(singletonElem);
	}

	/**
	 * @param perms
	 * @param annotation
	 * @return
	 */
	public static Permissions borrowPermissionWithStateUntouched(
			Permissions perms, 
			Permission borrow) {
		HashSet<Permission> applicable = new HashSet<Permission>();
		for(Permission p : perms.permissions) {
			if(p.contains(borrow)) {
				applicable.add(p);
			}
		}
		return new Permissions(applicable);
	}

	/**
	 * @param perms
	 * @param annotation
	 * @return
	 */
	public static Permissions borrowPermission(
			Permissions perms, 
			Permission borrow,
			String ensuredState) {
		HashSet<Permission> applicable = new HashSet<Permission>();
		for(Permission p : perms.permissions) {
			if(p.contains(borrow)) {
				if(p.isUnique() || p.isFull() || p.isShare() || p.getStateSpace().firstBiggerThanSecond(p.getStateInfo(), ensuredState))
					applicable.add(p.copyNewState(ensuredState));
				else
					// do not weaken existing state info for read-only permissions
					applicable.add(p);
			}
		}
		if(applicable.isEmpty()) {
			if(log.isLoggable(Level.WARNING))
				log.warning("no permission available to borrow " + borrow);
			// add fake permission to move on
			applicable.add(borrow.copyNewState(ensuredState));
		}
		return new Permissions(applicable);
	}

	/**
	 * @param perms
	 * @param annotation
	 * @return
	 */
	public static Permissions capturePermission(Permissions perms, Permission capture) {
		HashSet<Permission> remaining = new HashSet<Permission>();
		for(Permission p : perms.permissions) {
			if(p.contains(capture)) {
				Permission remainder = p.splitOff(capture);
				// TODO Additional state info for remainder?
				if(remainder != null)
					remaining.add(remainder);
			}
			// TODO split off permissions for other dimensions
		}
		return new Permissions(remaining);
	}

	/**
	 * @param p
	 * @param a
	 * @return
	 */
	public static Permissions filterPermissions(Permissions p, SimplePermissionAnnotation a) {
		if(a == null)
			return p;
		Permissions result;
		if(a.isReturned())
			result = Permissions.borrowPermission(p, a.getRequires(), a.getEnsures().getStateInfo());
		else
			result = Permissions.capturePermission(p, a.getRequires());
		return result;
	}

	/**
	 * @param p
	 * @param a
	 * @return
	 */
	public static Permissions subtractPermissions(Permissions p, SimplePermissionAnnotation a) {
		if(a == null)
			return p;
		return Permissions.capturePermission(p, a.getRequires());
	}

	/**
	 * @param perms
	 * @param needed
	 * @return
	 */
	public static boolean checkPermissions(Permissions perms, Permission needed) {
		if(needed == null)
			return true;
		for(Permission p : perms.permissions) {
//			Crystal.getInstance().debugOut().println("compare: " + p + " to " + needed);
			if(p.contains(needed))
				return true;
		}
		return false;
	}

	/**
	 * @param perms
	 * @param anno
	 * @return
	 */
	public static boolean checkPermissions(Permissions perms, SimplePermissionAnnotation anno) {
		return anno == null ? true : checkPermissions(perms, anno.getRequires());
	}

	/**
	 * @param newState
	 * @return
	 */
	public static Permissions morePreciseState(Permissions perms, String newState) {
		HashSet<Permission> updatedPermissions = new HashSet<Permission>();
		for(Permission p : perms.permissions) {
			if(p.getStateSpace().firstBiggerThanSecond(p.getStateInfo(), newState))
				updatedPermissions.add(p.copyNewState(newState));
			else
				updatedPermissions.add(p);
		}
		return new Permissions(updatedPermissions);
	}

	/**
	 * @param newState
	 * @return
	 */
	public static boolean isConsistentState(Permissions perms, String newState) {
		for(Permission p : perms.permissions) {
			// TODO Take state dimensions into account...
			if(p.getStateSpace().firstBiggerThanSecond(p.getStateInfo(), newState))
				continue;
			if(p.getStateSpace().firstBiggerThanSecond(newState, p.getStateInfo()))
				continue;
			return false;
		}
		return true;
	}

	public static boolean isReadOnlyAccess(Permissions perms) {
		for(Permission p : perms.permissions) {
			if(p.isReadOnly() == false)
				return false;
		}
		return true;
	}

}
