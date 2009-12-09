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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.crystal.util.VOID;
import edu.cmu.cs.plural.perm.parser.AccessPredVisitor;
import edu.cmu.cs.plural.perm.parser.BinaryExprAP;
import edu.cmu.cs.plural.perm.parser.Conjunction;
import edu.cmu.cs.plural.perm.parser.Disjunction;
import edu.cmu.cs.plural.perm.parser.EmptyPredicate;
import edu.cmu.cs.plural.perm.parser.EqualsExpr;
import edu.cmu.cs.plural.perm.parser.Identifier;
import edu.cmu.cs.plural.perm.parser.NotEqualsExpr;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.perm.parser.PermissionImplication;
import edu.cmu.cs.plural.perm.parser.StateOnly;
import edu.cmu.cs.plural.perm.parser.TempPermission;
import edu.cmu.cs.plural.perm.parser.Withing;
import edu.cmu.cs.plural.polymorphic.internal.PolyTupleLattice.PackedNess;
import edu.cmu.cs.plural.states.annowrappers.ClassStateDeclAnnotation;
import edu.cmu.cs.plural.states.annowrappers.StateDeclAnnotation;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;

/**
 * The PackingManager is in charge of packing a lattice
 * to some appropriate state. 
 * 
 * @author Nels E. Beckman
 * @since Dec 9, 2009
 *
 */
public final class PackingManager {
	private PackingManager() {}
	
	/** 
	 * In the given lattice, pack the receiver to the given state.
	 * If this is not possible, return NONE. If the receiver is already
	 * unpacked, or if it is unknown, this method will return the
	 * incoming_lattice. THIS METHOD EXPECTS THE INCOMING LATTICE TO
	 * BE A COPY! COPY IT BEFORE CALLING THIS METHOD!
	 */
	public static Option<PolyTupleLattice> packRcvr(PolyTupleLattice incoming_lattice,
			Set<String> states_to_pack_to, AnnotationDatabase annoDB,
			final SimpleMap<String,Option<PolyVar>> varLookup,
			final SimpleMap<Variable,Aliasing> locs,
			ITypeBinding this_type) {
		if( !incoming_lattice.getPackedness().equals(PackedNess.UNPACKED) )
			return Option.some(incoming_lattice);
		
		PolyTupleLattice result = incoming_lattice;
		Map<Variable, PolyVar> invariants = 
			invariants(this_type, states_to_pack_to, annoDB, varLookup);
		
		for( Map.Entry<Variable, PolyVar> entry : invariants.entrySet() ) {
			Aliasing loc = locs.get(entry.getKey());
			PolyVarLE cur_le = result.get(loc);
			
			if( cur_le.isTop() || cur_le.name().isNone() ) {
				// CANNOT PACK!
				return Option.none();
			}

			PolyVar needs = entry.getValue();
			if( !cur_le.name().unwrap().equals(needs.getName()) ) {
				// CANNOT PACK!
				return Option.none();
			}
			
			if( !needs.getKind().equals(PolyVarKind.SYMMETRIC) ) {
				// All permission is removed
				result.put(loc, PolyVarLE.NONE);
			}
		}
		
		return Option.some(result);
	}

	/**
	 * Get the invariants in terms of a field to poly-var map.
	 */
	public static 
	Map<Variable, PolyVar> invariants(ITypeBinding this_type, Set<String> states,
			AnnotationDatabase annoDB, final SimpleMap<String,Option<PolyVar>> varLookup) {
		final SimpleMap<String,Variable> fields = PluralTupleLatticeElement.createFieldNameToVariableMapping(this_type);
		// Look up the class states, find the invariants, add them to the result.
		final Map<Variable,PolyVar> result = new HashMap<Variable,PolyVar>();
		List<ICrystalAnnotation> annos = annoDB.getAnnosForType(this_type);
		for( ICrystalAnnotation anno_ : annos ) {
			if( anno_ instanceof ClassStateDeclAnnotation ) {
				for( StateDeclAnnotation anno : ((ClassStateDeclAnnotation) anno_).getStates() ) {
					if( states.contains(anno.getStateName()) ) {
						String inv = anno.getInv();
						PermParser.accept(inv, new AccessPredVisitor<VOID>(){
							@Override
							public VOID visit(TempPermission perm) {
								Option<PolyVar> perm_var = varLookup.get(perm.getType());
								if( perm_var.isSome() ) {
									String field_name = ((Identifier)perm.getRef()).getName();
									Variable var = fields.get(field_name);
									result.put(var, perm_var.unwrap());
								}
								return VOID.V();
							}
	
							@Override public VOID visit(Disjunction disj) {return Utilities.nyi();}
							@Override public VOID visit(Withing withing) {return Utilities.nyi();}
							
							@Override
							public VOID visit(Conjunction conj) {
								conj.getP1().accept(this);
								conj.getP2().accept(this);
								return VOID.V();
							}
	
							@Override
							public VOID visit(BinaryExprAP binaryExpr) {
								return VOID.V();
							}
	
							@Override
							public VOID visit(EqualsExpr equalsExpr) {
								return VOID.V();
							}
	
							@Override
							public VOID visit(NotEqualsExpr notEqualsExpr) {
								return VOID.V();
							}
	
							@Override
							public VOID visit(StateOnly stateOnly) {
								return VOID.V();
							}
	
							@Override
							public VOID visit(
									PermissionImplication permissionImplication) {
								return VOID.V();
							}
	
							@Override
							public VOID visit(EmptyPredicate emptyPredicate) {
								return VOID.V();
							}});
					}
				}
			}
		}
		return result;
	}
}
