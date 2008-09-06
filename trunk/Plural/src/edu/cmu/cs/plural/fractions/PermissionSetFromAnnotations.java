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
package edu.cmu.cs.plural.fractions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.util.Pair;

/**
 * @author Kevin Bierhoff
 *
 */
public class PermissionSetFromAnnotations extends
		AbstractFractionalPermissionSet<PermissionFromAnnotation> {

	public static PermissionSetFromAnnotations createEmpty(StateSpace stateSpace) {
		return new PermissionSetFromAnnotations(stateSpace);
	}

	public static PermissionSetFromAnnotations createSingleton(PermissionFromAnnotation singleton) {
		FractionConstraints constraints = new FractionConstraints();
		singleton.registerFractions(constraints);
		singleton.addIsUsedConstraints(constraints);
		singleton.addIsFunctionConstraints(constraints);
		
		List<PermissionFromAnnotation> perms;
		List<PermissionFromAnnotation> framePerms;
		if(singleton.isFramePermission()) {
			perms = Collections.emptyList();
			framePerms = Collections.singletonList(singleton);
		}
		else {
			perms = Collections.singletonList(singleton);
			framePerms = Collections.emptyList();
		}
			
		return new PermissionSetFromAnnotations(
				singleton.getStateSpace(), 
				perms, framePerms,
				constraints);
	}

	private StateSpace stateSpace;
	
	protected PermissionSetFromAnnotations(StateSpace stateSpace) {
		super();
		this.stateSpace = stateSpace;
	}
	
	protected PermissionSetFromAnnotations(
			StateSpace stateSpace,
			List<PermissionFromAnnotation> permissions,
			List<PermissionFromAnnotation> framePermissions,
			FractionConstraints constraints) {
		super(permissions, framePermissions, constraints);
		this.stateSpace = stateSpace;
	}

	@Override
	public StateSpace getStateSpace() {
		return stateSpace;
	}

	/**
	 * Merge the given permission into the set of known permissions.
	 * If a permission with the same root as the given permission is already
	 * in the set then these two permissions will be coalesced immediately.
	 * @param permission New permission to be added to the set.
	 * @return New permission set with <code>permission</code> added.
	 */
	public PermissionSetFromAnnotations combine(PermissionFromAnnotation permission) {
		FractionConstraints constraints = this.constraints.mutableCopy();
		
		// maybe it would be better to use the permission factory to create
		// this permission in place
		permission.registerFractions(constraints);
		permission.addIsUsedConstraints(constraints);
		permission.addIsFunctionConstraints(constraints);

		boolean addToFramePerms = permission.isFramePermission();
		
		ArrayList<PermissionFromAnnotation> newPs = new ArrayList<PermissionFromAnnotation>(
				addToFramePerms ? framePermissions.size() : permissions.size() );
		String neededRoot = permission.getRootNode(); 
		boolean combined = false;
		for(PermissionFromAnnotation p : (addToFramePerms ? framePermissions : permissions) ) {
			if(p.getRootNode().equals(neededRoot)) {
				// eager merging of permissions for same root
				p = p.combine(permission, constraints);
				combined = true;
			}
			newPs.add(p);
		}
		if(! combined)
			newPs.add(permission);
		
		if(addToFramePerms)
			return createPermissions(permissions, newPs, constraints);
		else
			return createPermissions(newPs, framePermissions, constraints);
	}

	/**
	 * Turns this permission set into a regular 
	 * permission set that can be joined, split, and merged.
	 * @return
	 */
	public FractionalPermissions toLatticeElement() {
		return new FractionalPermissions(permissions, framePermissions, constraints);
	}

	/**
	 * Turns this permission set into a regular 
	 * permission set with the given 
	 * permission parameter that can be joined, split, and merged.
	 * @return
	 */
//	public FractionalPermissions toLatticeElement(
//			Map<String, Aliasing> parameters,
//			Map<Aliasing, PermissionSetFromAnnotations> parameterPermissions) {
//		return new FractionalPermissions(permissions, framePermissions, constraints, parameters, parameterPermissions, null);
//	}

	/**
	 * Returns <code>true</code> if there are no permissions in this set.
	 * @return <code>true</code> if there are no permissions in this set, <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return permissions.isEmpty() && framePermissions.isEmpty();
	}

	private PermissionSetFromAnnotations createPermissions(
			List<PermissionFromAnnotation> newPermissions,
			List<PermissionFromAnnotation> newFramePermissions,
			FractionConstraints newConstraints) {
		return new PermissionSetFromAnnotations(stateSpace, newPermissions, newFramePermissions, newConstraints);
	}

	/**
	 * Union of state infos from contained permissions.
	 * @return
	 * @deprecated Discouraged: this returns state info from both frame and virtual permissions.
	 * @see FractionalPermission#getStateInfo()
	 */
	@Deprecated
	public Set<String> getStateInfo() {
		LinkedHashSet<String> result = new LinkedHashSet<String>();
		for(PermissionFromAnnotation p : permissions) {
			result.addAll(p.getStateInfo());
		}
		for(PermissionFromAnnotation p : framePermissions) {
			result.addAll(p.getStateInfo());
		}
		return result;
	}
	
	public Set<String> getStateInfo(boolean inFrame) {
		LinkedHashSet<String> result = new LinkedHashSet<String>();
		for(PermissionFromAnnotation p : inFrame ? framePermissions : permissions) {
			result.addAll(p.getStateInfo());
		}
		return result;
	}

	/**
	 * Returns a pair of state infos for virtual and frame permissions.
	 * @return a pair of state infos for virtual and frame permissions.
	 */
	public Pair<Set<String>, Set<String>> getStateInfoPair() {
		return Pair.create(getStateInfo(false), getStateInfo(true));
	}

	@Override
	public String toString() {
		return permissions + " frame " + framePermissions + " with " + constraints;
	}

	public boolean isReadOnly() {
		for(PermissionFromAnnotation p : permissions) {
			if(p.isReadOnly() == false)
				return false;
		}
		return true;
	}

}
