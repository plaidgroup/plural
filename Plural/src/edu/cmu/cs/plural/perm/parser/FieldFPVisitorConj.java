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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.Permission.PermissionKind;


/**
 * This visitor is used for creating a list of FractionalPermissions
 * from an AccessPredicate. However, if any connectives other than
 * conjunction are used, an exception will be thrown. If there are
 * state info predicates, that information will not be returned from
 * this visitor, since we may not have the permission information required
 * to create a full PermissionFromAnnotation object.
 * 
 * 
 * @author Nels Beckman
 * @date Mar 26, 2008
 *
 */
public class FieldFPVisitorConj implements
		AccessPredVisitor<List<Pair<String, PermissionFromAnnotation>>> {

	final boolean createNamedVariables;
	final SimpleMap<String, StateSpace> stateInfos;
	private PermissionFactory pf = PermissionFactory.INSTANCE;
	
	public FieldFPVisitorConj(SimpleMap<String, StateSpace> stateInfos,
			boolean createNamedVariables) {
		this.createNamedVariables = createNamedVariables;
		this.stateInfos = stateInfos;
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(TempPermission perm) {
		/*
		 * If we have some kind of parameter permission, well things are wrong.
		 */
		if( !(perm.getRef() instanceof Identifier) )
			throw new RuntimeException("Method parameter reference in field permission.");
		
		String var = ((Identifier)perm.getRef()).getName();
		StateSpace state_space = this.stateInfos.get(var);
		PermissionKind perm_kind = PermissionKind.valueOf(perm.getType().toUpperCase());
		PermissionFromAnnotation result =
		pf.createOrphan(state_space, perm.getRoot(),
				perm_kind, perm.getStateInfo(), createNamedVariables);
		
		return Collections.singletonList(Pair.create(var, result));
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(Disjunction disj) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(Conjunction conj) {
		List<Pair<String, PermissionFromAnnotation>> l1 = conj.getP1().accept(this);
		List<Pair<String, PermissionFromAnnotation>> l2 = conj.getP2().accept(this);
		
		List<Pair<String, PermissionFromAnnotation>> result = 
			new ArrayList<Pair<String, PermissionFromAnnotation>>(l1.size()+l2.size());
		
		result.addAll(l1); result.addAll(l2);
		return result;
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(Withing withing) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(
			BinaryExprAP binaryExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(EqualsExpr equalsExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(
			NotEqualsExpr notEqualsExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(
			StateOnly stateOnly) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(
			PermissionImplication permissionImplication) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, PermissionFromAnnotation>> visit(
			EmptyPredicate emptyPredicate) {
		return Collections.emptyList();
	}	
}
