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

import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * This visitor visits an AccessPred and returns a list of variables.
 * If true is passed to the constructor, then these are variables that
 * must be null. If false is passed to the constructor, then these are
 * variables that must not be null.
 * 
 * @author Nels Beckman
 * @date Mar 27, 2008
 *
 */
public class NullVisitorConj implements AccessPredVisitor<List<Variable>> {

	private final SimpleMap<String, Variable> variableLocs;
	/*
	 * Do we get null variables, or non-null variables.
	 */
	private final boolean collectNullVars;

	public NullVisitorConj(boolean collectNullVars, SimpleMap<String, Variable> vlocs) {
		this.collectNullVars = collectNullVars;
		this.variableLocs = vlocs;
	}

	@Override
	public List<Variable> visit(TempPermission perm) {
		return Collections.emptyList();
	}

	@Override
	public List<Variable> visit(Disjunction disj) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public List<Variable> visit(Conjunction conj) {
		List<Variable> l1 = conj.getP1().accept(this);
		List<Variable> l2 = conj.getP2().accept(this);
		
		List<Variable> result = new ArrayList<Variable>(l1.size()+l2.size());
		
		result.addAll(l1); result.addAll(l2);
		return result;
	}

	@Override
	public List<Variable> visit(Withing withing) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public List<Variable> visit(BinaryExprAP binaryExpr) {
		return binaryExpr.getBinExpr().accept(this);
	}

	@Override
	public List<Variable> visit(EqualsExpr equalsExpr) {
		return collectNullVars ? 
				binaryHelper(equalsExpr, variableLocs) :
				Collections.<Variable>emptyList();
	}

	@Override
	public List<Variable> visit(NotEqualsExpr notEqualsExpr) {
		return (!collectNullVars) ?
				binaryHelper(notEqualsExpr, variableLocs) :
					Collections.<Variable>emptyList();				
	}
	
	/**
	 * Returns a (possibly empty) list of variables that are related to NULL
	 * as indicated by the given <code>null</code>.
	 * @param expr
	 * @return
	 */
	public static List<Variable> binaryHelper(BinaryExpr expr, SimpleMap<String, Variable> variableLocs) {
		if( expr.getE1().equals(Null.getInstance()) || 
				expr.getE2().equals(Null.getInstance()) ) {
			/*
			 * If both are null, do nothing.
			 */
			if( expr.getE1().equals(Null.getInstance()) && 
					expr.getE2().equals(Null.getInstance()) )  {
				return Collections.emptyList();
			}

			if( expr.getE1().equals(Null.getInstance()) ) {
				if( !(expr.getE2() instanceof Identifier) )
					throw new IllegalStateException("Type error");

				Identifier id = (Identifier)expr.getE2();
				return Collections.singletonList(variableLocs.get(id.getName()));
			}
			else {
				if( !(expr.getE1() instanceof Identifier) )
					throw new IllegalStateException("Type error");

				Identifier id = (Identifier)expr.getE1();
				return Collections.singletonList(variableLocs.get(id.getName()));
			}
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<Variable> visit(StateOnly stateOnly) {
		return Collections.emptyList();
	}

	@Override
	public List<Variable> visit(PermissionImplication permissionImplication) {
		return Collections.emptyList();
	}
}
