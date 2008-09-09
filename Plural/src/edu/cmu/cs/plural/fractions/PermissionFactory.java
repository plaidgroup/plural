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

import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * @author Kevin Bierhoff
 *
 */
public class PermissionFactory {
	
	public static final PermissionFactory INSTANCE = new PermissionFactory();
	
	/**
	 * Creates a virtual permission with the given parameters.
	 * @param stateSpace
	 * @param rootNode
	 * @param kind
	 * @param stateInfo
	 * @param namedFractions
	 * @return
	 */
	public PermissionFromAnnotation createOrphan(StateSpace stateSpace, String rootNode, PermissionKind kind, 
			String[] stateInfo, boolean namedFractions) {
		switch(kind) {
		case UNIQUE:
			return createUniqueOrphan(stateSpace, rootNode, false, stateInfo);
		case FULL:
			return createFullOrphan(stateSpace, rootNode, false, stateInfo, namedFractions);
		case SHARE:
			return createShareOrphan(stateSpace, rootNode, false, stateInfo, namedFractions);
		case IMMUTABLE:
			return createImmutableOrphan(stateSpace, rootNode, false, stateInfo, namedFractions);
		case PURE:
			return createPureOrphan(stateSpace, rootNode, false, stateInfo, namedFractions);
		default:
			throw new IllegalArgumentException("Unknown permission kind: " + kind);
		}
	}

	/**
	 * Creates a virtual or frame permission with the given parameters.
	 * @param stateSpace
	 * @param rootNode
	 * @param kind
	 * @param isFramePermission
	 * @param stateInfo
	 * @param namedFractions
	 * @return
	 */
	public PermissionFromAnnotation createOrphan(StateSpace stateSpace, String rootNode, PermissionKind kind, 
			boolean isFramePermission, String[] stateInfo, boolean namedFractions) {
		switch(kind) {
		case UNIQUE:
			return createUniqueOrphan(stateSpace, rootNode, isFramePermission, stateInfo);
		case FULL:
			return createFullOrphan(stateSpace, rootNode, isFramePermission, stateInfo, namedFractions);
		case SHARE:
			return createShareOrphan(stateSpace, rootNode, isFramePermission, stateInfo, namedFractions);
		case IMMUTABLE:
			return createImmutableOrphan(stateSpace, rootNode, isFramePermission, stateInfo, namedFractions);
		case PURE:
			return createPureOrphan(stateSpace, rootNode, isFramePermission, stateInfo, namedFractions);
		default:
			throw new IllegalArgumentException("Unknown permission kind: " + kind);
		}
	}

	/**
	 * Creates a virtual or frame permission with the given parameters and adds any needed
	 * constraints to the given constraint set.
	 * @param stateSpace
	 * @param rootNode
	 * @param kind
	 * @param isFramePermission
	 * @param stateInfo
	 * @param namedFractions
	 * @param constraints
	 * @return
	 *
	public PermissionFromAnnotation create(StateSpace stateSpace, String rootNode, PermissionKind kind, 
			boolean isFramePermission, String stateInfo, boolean namedFractions, FractionConstraints constraints) {
		switch(kind) {
		case UNIQUE:
			return createUnique(stateSpace, rootNode, isFramePermission, stateInfo, constraints);
		case FULL:
			return createFull(stateSpace, rootNode, isFramePermission, stateInfo, namedFractions, constraints);
		case SHARE:
			return createShare(stateSpace, rootNode, isFramePermission, stateInfo, namedFractions, constraints);
		case IMMUTABLE:
			return createImmutable(stateSpace, rootNode, isFramePermission, stateInfo, namedFractions, constraints);
		case PURE:
			return createPure(stateSpace, rootNode, isFramePermission, stateInfo, namedFractions, constraints);
		default:
			throw new IllegalArgumentException("Unknown permission kind: " + kind);
		}
	}

	public PermissionFromAnnotation createUnique(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo, 
			FractionConstraints constraints) {
		FractionFunction f = FractionFunction.fixAll(stateSpace, rootNode, Fraction.one());
		return new PermissionFromAnnotation(stateSpace, rootNode, f, true, isFramePermission, Collections.singleton(stateInfo), constraints);
	}

	public PermissionFromAnnotation createFull(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo, 
			boolean namedFractions, FractionConstraints constraints) {
		FractionFunction f = FractionFunction.fixedBelow(stateSpace, rootNode, namedFractions, Fraction.one());
		return new PermissionFromAnnotation(stateSpace, rootNode, f, true, isFramePermission, Collections.singleton(stateInfo), constraints);
	}

	public PermissionFromAnnotation createShare(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo, 
			boolean namedFractions, FractionConstraints constraints) {
		FractionFunction f = FractionFunction.variableAll(stateSpace, rootNode, namedFractions);
		return new PermissionFromAnnotation(stateSpace, rootNode, f, true, isFramePermission, Collections.singleton(stateInfo), constraints);
	}

	public PermissionFromAnnotation createImmutable(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo, 
			boolean namedFractions, FractionConstraints constraints) {
		FractionFunction f = FractionFunction.variableAll(stateSpace, rootNode, namedFractions);
		return new PermissionFromAnnotation(stateSpace, rootNode, f, false, isFramePermission, Collections.singleton(stateInfo), constraints);
	}

	public PermissionFromAnnotation createPure(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo, 
			boolean namedFractions, FractionConstraints constraints) {
		FractionFunction f = FractionFunction.fixedBelow(stateSpace, rootNode, namedFractions, Fraction.zero());
		return new PermissionFromAnnotation(stateSpace, rootNode, f, false, isFramePermission, Collections.singleton(stateInfo), constraints);
	}*/

	public PermissionFromAnnotation createUniqueOrphan(StateSpace stateSpace, String rootNode, boolean isFramePermission, String... stateInfo) {
		FractionFunction f = FractionFunction.fixAll(stateSpace, rootNode, Fraction.one());
		return new PermissionFromAnnotation(stateSpace, rootNode, f, true, isFramePermission, stateInfo);
	}

	private PermissionFromAnnotation createFullOrphan(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo[], 
			boolean namedFractions) {
		FractionFunction f = FractionFunction.fixedBelow(stateSpace, rootNode, namedFractions, Fraction.one());
		return new PermissionFromAnnotation(stateSpace, rootNode, f, true, isFramePermission, stateInfo);
	}

	private PermissionFromAnnotation createShareOrphan(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo[], 
			boolean namedFractions) {
		FractionFunction f = FractionFunction.variableAll(stateSpace, rootNode, namedFractions);
		return new PermissionFromAnnotation(stateSpace, rootNode, f, true, isFramePermission, stateInfo);
	}

	public PermissionFromAnnotation createImmutableOrphan(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo[], 
			boolean namedFractions) {
		FractionFunction f = FractionFunction.variableAll(stateSpace, rootNode, namedFractions);
		return new PermissionFromAnnotation(stateSpace, rootNode, f, false, isFramePermission, stateInfo);
	}

	private PermissionFromAnnotation createPureOrphan(StateSpace stateSpace, String rootNode, boolean isFramePermission, String stateInfo[], 
			boolean namedFractions) {
		FractionFunction f = FractionFunction.fixedBelow(stateSpace, rootNode, namedFractions, Fraction.zero());
		return new PermissionFromAnnotation(stateSpace, rootNode, f, false, isFramePermission, stateInfo);
	}

}
