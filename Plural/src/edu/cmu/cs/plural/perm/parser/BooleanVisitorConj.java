/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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
import java.util.List;

import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.util.CollectionMethods;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * This method will return all of the fields that must be true or must be
 * false (depending on the argument fed to its constructor) in a state
 * invariant. It's use and implementation is quite similar to NullVisitorConj.
 * 
 * @author Nels Beckman
 * @date Apr 22, 2008
 * @see edu.cmu.cs.plural.perm.parser.NullVisitorConj
 */
public class BooleanVisitorConj implements AccessPredVisitor<List<Variable>> {

	private final SimpleMap<String, Variable> variableLocs;
	/*
	 * Do we get true variables, or false variables?
	 */
	private final boolean collectTrueVars;

	/**
	 * Constructor
	 * @param collectTrueVars Set to true if you want the visitor to collect the variables
	 * in the predicate that must be true, otherwise set to false.
	 * @param vlocs A map from variable names to Variable objects for variables that 
	 * we should expect to encounter.
	 */
	public BooleanVisitorConj(boolean collectTrueVars, SimpleMap<String, Variable> vlocs) {
		this.collectTrueVars = collectTrueVars;
		this.variableLocs = vlocs;
	}
	
	@Override
	public List<Variable> visit(TempPermission perm) {
		return Collections.emptyList();
	}

	@Override
	public List<Variable> visit(Disjunction disj) {
		throw new RuntimeException("Does not work for disjunction.");
	}

	@Override
	public List<Variable> visit(Conjunction conj) {
		List<Variable> l1 = conj.getP1().accept(this);
		List<Variable> l2 = conj.getP2().accept(this);
		return CollectionMethods.concat(l1,l2);
	}

	@Override
	public List<Variable> visit(Withing withing) {
		throw new RuntimeException("Does not work for '&.'");
	}

	@Override
	public List<Variable> visit(BinaryExprAP binaryExpr) {
		return binaryExpr.accept(this);
	}

	@Override
	public List<Variable> visit(EqualsExpr equalsExpr) {
		return this.binaryHelper(equalsExpr, collectTrueVars);
	}

	private List<Variable> binaryHelper(BinaryExpr bin_expr,
			boolean find_if_this) {
		if( bin_expr.getE1() instanceof BoolLiteral ||
			bin_expr.getE2() instanceof BoolLiteral) {
			BoolLiteral constant;
			PrimaryExpr var;
			if( bin_expr.getE1() instanceof BoolLiteral ) {
				constant = (BoolLiteral)bin_expr.getE1();
				var = bin_expr.getE2();
			}
			else {
				constant = (BoolLiteral)bin_expr.getE2();
				var = bin_expr.getE1();
			}
			
			if( constant.hasValue(find_if_this) ) {
				// Now that we have a constant, we may have a variable.
				if( var instanceof Identifier ) {
					Identifier id = (Identifier)var;
					return Collections.singletonList(this.variableLocs.get(id.getName()));
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public List<Variable> visit(NotEqualsExpr notEqualsExpr) {
		return this.binaryHelper(notEqualsExpr, !collectTrueVars);
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
