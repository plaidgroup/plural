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

package edu.cmu.cs.plural.polymorphic.internal;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.flow.ILatticeOperations;

/**
 * Lattice operations for the polymorphic checker.
 * 
 * @author Nels E. Beckman
 * @since Nov 11, 2009
 *
 */
public class PolyInternalLatticeOps implements
		ILatticeOperations<PolyVarLE> {

	@Override
	public boolean atLeastAsPrecise(PolyVarLE info, PolyVarLE reference,
			ASTNode node) {
		if( info.isBottom() )
			return true;
		else if( reference.isTop() )
			return true;
		else if( info.name().isSome() && reference.name().isSome() )
			return true;
		else if( info.name().isNone() && reference.name().isNone() )
			return true;
		else
			return false;
	}

	@Override
	public PolyVarLE copy(PolyVarLE original) {
		if( original.isBottom() || original.isTop() || original.name().isNone() )
			return original;
		else
			return PolyVarLE.HAVE_FACTORY.call(original.name().unwrap());
	}

	@Override
	public PolyVarLE join(PolyVarLE someInfo, PolyVarLE otherInfo, ASTNode node) {
		if( someInfo.isTop() && otherInfo.isTop() )
			return PolyVarLE.TOP;
		else if( someInfo.isBottom() && otherInfo.isBottom() )
			return PolyVarLE.BOTTOM;
		else if( someInfo.name().isNone() && otherInfo.name().isNone() )
			return PolyVarLE.NONE;
		else if( someInfo.equals(otherInfo) )
			return someInfo;
		else
			return PolyVarLE.TOP;
	}

	@Override
	public PolyVarLE bottom() {
		return PolyVarLE.BOTTOM;
	}
}