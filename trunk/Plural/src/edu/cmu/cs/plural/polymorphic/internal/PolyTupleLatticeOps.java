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

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.plural.polymorphic.internal.PolyTupleLattice.PackedNess;

/**
 * @author Lattice ops for the new PolyTupleLattice. If things go well,
 * it should just be delegating...
 * @since Dec 9, 2009
 *
 */
public final class PolyTupleLatticeOps implements ILatticeOperations<PolyTupleLattice> {

	private final TupleLatticeOperations<Aliasing,PolyVarLE> tupleOps =
		new TupleLatticeOperations<Aliasing,PolyVarLE>(new PolyInternalLatticeOps(), PolyVarLE.TOP);
	
	@Override
	public boolean atLeastAsPrecise(PolyTupleLattice info,
			PolyTupleLattice reference, ASTNode node) {
		TupleLatticeElement<Aliasing,PolyVarLE> info_tuple = info.getTuple();
		TupleLatticeElement<Aliasing,PolyVarLE> reference_tuple = reference.getTuple();
		boolean tuple_is_alap = tupleOps.atLeastAsPrecise(info_tuple, reference_tuple, node);
		
		PackedNess info_packed = info.getPackedness();
		PackedNess ref_packed = reference.getPackedness();
		
		boolean packed_is_alap;
		if( info_packed.equals(PackedNess.BOTTOM) ) 
			packed_is_alap = true;
		else if( ref_packed.equals(PackedNess.TOP) )
			packed_is_alap = true;
		else if( info_packed.equals(ref_packed) )
			packed_is_alap = true;
		else
			packed_is_alap = false;
		
		return tuple_is_alap && packed_is_alap;
	}

	@Override
	public PolyTupleLattice bottom() {
		return new PolyTupleLattice(tupleOps.bottom(), PackedNess.BOTTOM);
	}

	@Override
	public PolyTupleLattice copy(PolyTupleLattice original) {
		return new PolyTupleLattice(tupleOps.copy(original.getTuple()), original.getPackedness());
	}

	@Override
	public PolyTupleLattice join(PolyTupleLattice someInfo,
			PolyTupleLattice otherInfo, ASTNode node) {
		TupleLatticeElement<Aliasing,PolyVarLE> new_tuple = 
			tupleOps.join(someInfo.getTuple(), otherInfo.getTuple(), node);
		PackedNess new_packed;
		
		PackedNess some_packed = someInfo.getPackedness();
		PackedNess other_packed = otherInfo.getPackedness();
		if( some_packed.equals(other_packed) )
			new_packed = some_packed;
		else if( some_packed.equals(PackedNess.TOP) || other_packed.equals(PackedNess.TOP))
			new_packed = PackedNess.TOP;
		else if( some_packed.equals(PackedNess.BOTTOM) )
			new_packed = other_packed;
		else if( other_packed.equals(PackedNess.BOTTOM) )
			new_packed = some_packed;
		else
			new_packed = PackedNess.TOP;
		
		return new PolyTupleLattice(new_tuple, new_packed);
	}

	public PolyTupleLattice getDefault() {
		return new PolyTupleLattice(this.tupleOps.getDefault(), 
				PolyTupleLattice.PackedNess.PACKED);
	}
}
