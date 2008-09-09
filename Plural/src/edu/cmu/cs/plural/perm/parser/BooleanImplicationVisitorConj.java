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
import java.util.List;

import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.util.CollectionMethods;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * A visitor for collecting the boolean implications in a state invariant and
 * returning them as a list. If a certain field's being true or false implies
 * another fact, this visitor will tell us. It will only work on trees of access
 * predicates separated by conjunctions. If either disjunctions or '&' is found,
 * an exception will be thrown.
 * 
 * @author Nels Beckman
 * @date Apr 22, 2008
 * @see edu.cmu.cs.plural.perm.parser.PermissionImplication
 */
public class BooleanImplicationVisitorConj implements
		AccessPredVisitor<List<Pair<Pair<Variable, Boolean>,
		                            List<Pair<Variable, PermissionFromAnnotation>>>>> {

	private final SimpleMap<String,Variable> variableLocs;
	private final SimpleMap<String,StateSpace> varStateSpaces;
	private final boolean createNamedVariables;
	
	/**
	 * @param varLocs Requires a mapping from variable names to variable
	 * objects for those variables that we might expect to encounter.
	 */
	public BooleanImplicationVisitorConj(SimpleMap<String,Variable> varLocs,
			SimpleMap<String,StateSpace> stateSpaces, boolean createNamedVariables) {
		this.variableLocs = varLocs;
		this.varStateSpaces = stateSpaces;
		this.createNamedVariables = createNamedVariables;
	}
	
	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			TempPermission perm) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			Conjunction conj) {
		List<Pair<Pair<Variable, Boolean>,
		List<Pair<Variable, PermissionFromAnnotation>>>> l1 = conj.getP1().accept(this);
		List<Pair<Pair<Variable, Boolean>,
		List<Pair<Variable, PermissionFromAnnotation>>>> l2 = conj.getP2().accept(this);
		return CollectionMethods.concat(l1,l2);
	}

	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			Disjunction disj) {
		throw new RuntimeException("This visitor was not written to handle &.");
	}
	
	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			Withing withing) {
		throw new RuntimeException("This visitor was not written to handle &.");
	}

	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			BinaryExprAP binaryExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			EqualsExpr equalsExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			NotEqualsExpr notEqualsExpr) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			StateOnly stateOnly) {
		return Collections.emptyList();
	}

	@Override
	public List<Pair<Pair<Variable, Boolean>,
	            List<Pair<Variable, PermissionFromAnnotation>>>> visit(
			PermissionImplication permissionImplication) {

		// Get the antecedent.
		
		// This is kind of silly because the visitor returns a list, but
		// we really expect one or the other. The visitor was made the way
		// it was so that it would be as similar to NullVisitorConj as
		// possible.
		List<Variable> true_ant = 
			permissionImplication.accept(new BooleanVisitorConj(true, variableLocs));
		List<Variable> false_ant =
			permissionImplication.accept(new BooleanVisitorConj(false, variableLocs));
		if(true_ant.size() + false_ant.size() > 1) {
			throw new RuntimeException("Invalid implication specification");
		}
		
		// Get the consequent
		List<Pair<String,PermissionFromAnnotation>> perms =
		permissionImplication.accept(new FieldFPVisitorConj(this.varStateSpaces, 
				this.createNamedVariables));
		
		// Map into something we actually want.
		// Please ignore the hideous typing annotation.
		List<Pair<Variable,PermissionFromAnnotation>> v_perms =
		CollectionMethods.map(perms, new CollectionMethods.Mapping<Pair<String,PermissionFromAnnotation>, Pair<Variable,PermissionFromAnnotation>>(){
			@Override
			public Pair<Variable, PermissionFromAnnotation> eval(
					Pair<String, PermissionFromAnnotation> elem) {
				return Pair.create(variableLocs.get(elem.fst()), elem.snd());
			}});
		
		// set up the antecedent part of the result 
		Variable var;
		Boolean bool;
		if( true_ant.size() == 1 ) {
			var = true_ant.get(0);
			bool = Boolean.TRUE;
		}
		else if( false_ant.size() == 1) {
			var = false_ant.get(0);
			bool = Boolean.FALSE;
		}
		else {
			return Collections.emptyList();
		}
		
		// Combine the above and return a singleton list.
		Pair<Pair<Variable, Boolean>,
		     List<Pair<Variable, PermissionFromAnnotation>>> singleton = 
			Pair.create(Pair.create(var, bool), v_perms);
		return Collections.singletonList(singleton);
	}
}