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

package edu.cmu.cs.plural.polymorphic.instantiation;

import java.util.Arrays;

import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * Represents a ground instantiation of a polymorphic permission. Ground means
 * share, pure, immutable, unique, or full, as opposed to the instantiation of
 * one polymorphic permission from another. We expect instances of this class
 * to be created by a parser, which will be parsing some <code>@Apply</code>
 * annotation. The constructors of this class take care of the important defaults.
 * 
 * @author Nels E. Beckman
 * @since Nov 19, 2009
 *
 */
public final class GroundInstantiation {
	
	private final PermissionKind kind;
	
	private final boolean isFrame;
	
	private final boolean isVirtual;
	
	private final String root;
	
	private final String[] states;

	// E.g., share
	public GroundInstantiation(PermissionKind kind) {
		this( kind, false, true, "alive", new String[]{"alive"} );
	}
	
	// E.g., share in Open
	public GroundInstantiation(PermissionKind kind, String[] states) {
		this( kind, false, true, "alive", states );
	}
	
	// E.g., share(alive,fr) in Open, HasNext 
	public GroundInstantiation(PermissionKind kind, boolean isFrame,
			boolean isVirtual, String root, String[] states) {
		this.kind = kind;
		this.isFrame = isFrame;
		this.isVirtual = isVirtual;
		this.root = root;
		this.states = states;
	}

	/**
	 * Returns the kind.
	 * @return the kind.
	 */
	public PermissionKind getKind() {
		return kind;
	}

	/**
	 * Returns the isFrame.
	 * @return the isFrame.
	 */
	public boolean isFrame() {
		return isFrame;
	}

	/**
	 * Returns the isVirtual.
	 * @return the isVirtual.
	 */
	public boolean isVirtual() {
		return isVirtual;
	}

	/**
	 * Returns the root.
	 * @return the root.
	 */
	public String getRoot() {
		return root;
	}

	/**
	 * Returns the states.
	 * @return the states.
	 */
	public String[] getStates() {
		return states;
	}

	@Override
	public String toString() {
		String vr_or_fr = isVirtual && isFrame ? "vrfr" : 
			(isVirtual ? "vr" : "fr");
		return this.kind.toString() + "(" + this.root + "," + vr_or_fr + ") in " +
			Arrays.toString(this.states);
	}
	
	
}
