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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A visitor class that finds the fields mentioned in an access pred.
 * This visitor will always ignore variables named "this" and "super."
 * 
 * @author Nels E. Beckman
 * @date Dec 11, 2008
 *
 */
public class FieldFinderVisitor implements AccessPredVisitor<Set<String>> {

	@Override
	public Set<String> visit(TempPermission perm) {
		RefExpr ref = perm.getRef();
		return getFieldSetFromRef(ref);	
	}

	/**
	 * For the given ref, what is the field that it uses mentions?
	 * Will always return an empty or singleton set.
	 */
	private static Set<String> getFieldSetFromRef(RefExpr ref) {
		if( ref instanceof Identifier ) {
			Identifier id = (Identifier)ref;
			return getFieldSetFromId(id);
		}
		return Collections.emptySet();
	}

	/**
	 * Given an id, returns a singleton set containing the
	 * field it mentions, or an empty set.
	 */
	private static Set<String> getFieldSetFromId(Identifier id) {
		if( !"super".equals(id.getName()) && 
			!"this".equals(id.getName())) {
			return Collections.singleton(id.getName());
		}
		return Collections.emptySet();
	}

	/** 
	 * Take the union of two sets. Will not return an empty
	 * set if both sets are empty. 
	 */
	private static <T> Set<T> union(Set<T> s1, Set<T> s2) {
		if( s1.isEmpty() ) return s2;
		if( s2.isEmpty() ) return s1;
		else {
			Set<T> result = new HashSet<T>(s1);
			result.addAll(s2);
			return result;
		}
	}
	
	@Override
	public Set<String> visit(Disjunction disj) {
		Set<String> r1 = disj.getP1().accept(this);
		Set<String> r2 = disj.getP2().accept(this);
		return union(r1,r2);
	}

	@Override
	public Set<String> visit(Conjunction conj) {
		Set<String> r1 = conj.getP1().accept(this);
		Set<String> r2 = conj.getP2().accept(this);
		return union(r1,r2);
	}

	@Override
	public Set<String> visit(Withing withing) {
		Set<String> r1 = withing.getP1().accept(this);
		Set<String> r2 = withing.getP2().accept(this);
		return union(r1,r2);
	}

	@Override
	public Set<String> visit(BinaryExprAP binaryExpr) {
		return binaryExpr.getBinExpr().accept(this);
	}

	@Override
	public Set<String> visit(EqualsExpr equalsExpr) {
		Set<String> r1 = equalsExpr.getE1().dispatch(new PrimaryExprFieldFinder());
		Set<String> r2 = equalsExpr.getE2().dispatch(new PrimaryExprFieldFinder());
		return union(r1,r2);
	}

	@Override
	public Set<String> visit(NotEqualsExpr notEqualsExpr) {
		Set<String> r1 = notEqualsExpr.getE1().dispatch(new PrimaryExprFieldFinder());
		Set<String> r2 = notEqualsExpr.getE2().dispatch(new PrimaryExprFieldFinder());
		return union(r1,r2);
	}

	@Override
	public Set<String> visit(StateOnly stateOnly) {
		return getFieldSetFromRef(stateOnly.getVar());
	}

	@Override
	public Set<String> visit(PermissionImplication permissionImplication) {
		Set<String> r1 = permissionImplication.ant().accept(this);
		Set<String> r2 = permissionImplication.cons().accept(this);
		return union(r1,r2);
	}

	private class PrimaryExprFieldFinder implements PrimaryExprVisitor<Set<String>> {
		@Override public Set<String> visitBool(BoolLiteral bool) {
			return Collections.emptySet();
		}
		@Override public Set<String> visitId(Identifier id) {
			return FieldFinderVisitor.getFieldSetFromId(id);
		}
		@Override public Set<String> visitNull(Null nul) {
			return Collections.emptySet();
		}
		@Override public Set<String> visitParam(ParamReference paramReference) {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<String> visit(EmptyPredicate emptyPredicate) {
		return Collections.emptySet();
	}
}
