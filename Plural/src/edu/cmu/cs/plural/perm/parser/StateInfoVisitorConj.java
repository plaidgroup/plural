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

/**
 * If an access permission contains state info ast nodes,
 * this visitor will help you retrieve the information from
 * those nodes. The result is a list of Pairs, mapping 
 * variable names to the state they are declared to be in.
 * This visitor is so named because it cannot handle any
 * connective other than conjunction, and will throw an
 * exception if others are encountered.
 * 
 * @see edu.cmu.cs.plural.perm.parser.StateOnly
 * 
 * @date March 30, 2008
 * @author Nels Beckman
 *
 */
public class StateInfoVisitorConj implements AccessPredVisitor<List<Pair<String, String>>> {

	@Override
	public List<Pair<String, String>> visit(TempPermission perm) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, String>> visit(Disjunction disj) {
		throw new IllegalStateException("Did not expect disjunction.");
	}

	@Override
	public List<Pair<String, String>> visit(Conjunction conj) {
		List<Pair<String, String>> l1 = conj.getP1().accept(this);
		List<Pair<String, String>> l2 = conj.getP2().accept(this);
		
		List<Pair<String, String>> result = 
			new ArrayList<Pair<String,String>>(l1.size()+l2.size());
		
		result.addAll(l1); result.addAll(l2);
		return result; 
	}

	@Override
	public List<Pair<String, String>> visit(Withing withing) {
		throw new IllegalStateException("Did not expect with.");
	}

	@Override
	public List<Pair<String, String>> visit(BinaryExprAP binaryExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, String>> visit(EqualsExpr equalsExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, String>> visit(NotEqualsExpr notEqualsExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<String, String>> visit(StateOnly stateOnly) {
		String ref = ((Identifier) stateOnly.getVar()).getName();
		return Collections.singletonList(Pair.create(ref, stateOnly.getStateInfo()));
	}

	@Override
	public List<Pair<String, String>> visit(
			PermissionImplication permissionImplication) {
		return Collections.emptyList();
	}	
}
