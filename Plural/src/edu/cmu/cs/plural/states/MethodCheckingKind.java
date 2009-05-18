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

package edu.cmu.cs.plural.states;

import edu.cmu.cs.crystal.util.Utilities;

/**
 * A method checking kind is a scenario under which we are checking either
 * a constructor or method call site or implementation. Since the rules for
 * which permissions to create from the specification are slightly different
 * for each case, we want to know exactly which one we are looking at.
 * 
 * @author Nels E. Beckman
 * @since May 13, 2009
 *
 */
public enum MethodCheckingKind {

	METHOD_CALL_DYNAMIC_DISPATCH,
	METHOD_CALL_STATIC_DISPATCH,
	CONSTRUCTOR_NEW,
	CONSTRUCTOR_SUPER_CALL,
	CONSTRUCTOR_IMPL_CUR_NOT_VIRTUAL,
	CONSTRUCTOR_IMPL_CUR_IS_VIRTUAL,
	METHOD_IMPL_CUR_NOT_VIRTUAL,
	METHOD_IMPL_CUR_IS_VIRTUAL;
	
	/**
	 * Will I ever even talk about a frame permission in the given checking
	 * scenario? For example, this is important if you are trying to decide
	 * whether to put this!fr into the state space.
	 */
	public static boolean hasAFramePermission(MethodCheckingKind kind) {
		switch(kind) {	
		case CONSTRUCTOR_NEW:
		case METHOD_CALL_DYNAMIC_DISPATCH: 
		case METHOD_IMPL_CUR_IS_VIRTUAL:
		case CONSTRUCTOR_IMPL_CUR_IS_VIRTUAL:
			return false;
		case METHOD_CALL_STATIC_DISPATCH:
		case CONSTRUCTOR_SUPER_CALL:
		case CONSTRUCTOR_IMPL_CUR_NOT_VIRTUAL:
		case METHOD_IMPL_CUR_NOT_VIRTUAL: 
			return true;
		default:
			return Utilities.nyi("Must have added a new case");
		}
	}

	/**
	 * Returns whether or not we should have a virtual permission given
	 * the type of method invocation/body we are checking and whether or
	 * not the permission is virtual, frame or both.<br>
	 * <br>
	 * Note: We are assuming that if we check with DISP_FIELDS and the
	 * current frame equals the virtual frame that ONLY a virtual frame
	 * will be created.
	 * 
	 * @param checkingKind The kind of method call/body that is being checked.
	 * @param isVirtualPermission Is the permission virtual?
	 * @param isFramePermission Is the permission a frame?
	 */
	public static boolean needVirtual(MethodCheckingKind checkingKind,
			boolean isVirtualPermission, boolean isFramePermission) {
		assert(isFramePermission || isVirtualPermission);
		switch(checkingKind) {
		case METHOD_CALL_DYNAMIC_DISPATCH:
		case CONSTRUCTOR_NEW:
			return true;
		case METHOD_CALL_STATIC_DISPATCH:
		case CONSTRUCTOR_SUPER_CALL:
		case METHOD_IMPL_CUR_NOT_VIRTUAL:
		case CONSTRUCTOR_IMPL_CUR_NOT_VIRTUAL:
			return isVirtualPermission;
		case METHOD_IMPL_CUR_IS_VIRTUAL:
		case CONSTRUCTOR_IMPL_CUR_IS_VIRTUAL:
			return true;
		default:
			return Utilities.nyi("Impossible!");
		}
	}
	
	/**
	 * Returns whether or not we should have a frame permission given
	 * the type of method invocation/body we are checking and whether or
	 * not the permission is virtual, frame or both.<br>
	 * <br>
	 * Note: We are assuming that if we check with DISP_FIELDS and the
	 * current frame equals the virtual frame that ONLY a virtual frame
	 * will be created.
	 * 
	 * @param checkingKind The kind of method call/body that is being checked.
	 * @param isVirtualPermission Is the permission virtual?
	 * @param isFramePermission Is the permission a frame?
	 */
	public static boolean needFrame(MethodCheckingKind checkingKind,
			boolean isVirtualPermission, boolean isFramePermission) {
		assert( isFramePermission || isVirtualPermission );
		switch( checkingKind ) {
		case METHOD_CALL_DYNAMIC_DISPATCH:
		case CONSTRUCTOR_NEW:
			return false;
		case METHOD_CALL_STATIC_DISPATCH:
		case CONSTRUCTOR_SUPER_CALL:
		case METHOD_IMPL_CUR_NOT_VIRTUAL:
		case CONSTRUCTOR_IMPL_CUR_NOT_VIRTUAL:
			return isFramePermission;
		case METHOD_IMPL_CUR_IS_VIRTUAL:
		case CONSTRUCTOR_IMPL_CUR_IS_VIRTUAL:
			return false;
		default:
			return Utilities.nyi("Impossible");
		}
	}
}