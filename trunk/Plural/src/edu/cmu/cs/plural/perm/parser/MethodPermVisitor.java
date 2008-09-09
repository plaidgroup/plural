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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.concrete.VariablePredicate;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * @author Kevin Bierhoff
 * @since 7/11/2008
 */
class MethodPermVisitor implements AccessPredVisitor<Boolean> {
	
	private static final Logger log = Logger.getLogger(MethodPermVisitor.class.getName());
	
	private final Map<String, Variable> fractionVars;
	private final SimpleMap<String, Variable> vars;
	private final boolean namedFractions;
	private final Set<Pair<Variable, PermissionFromAnnotation>> result =
		new LinkedHashSet<Pair<Variable, PermissionFromAnnotation>>();
	private final Set<Pair<Variable, VariablePredicate>> delayedPredicates =
		new LinkedHashSet<Pair<Variable, VariablePredicate>>();
	private final PermissionFactory pf = PermissionFactory.INSTANCE;
	
	public MethodPermVisitor(Map<String, Variable> fractionVars,
			SimpleMap<String, Variable> vars,
			boolean namedFractions) {
		this.fractionVars = fractionVars;
		this.vars = vars;
		this.namedFractions = namedFractions;
	}

	@Override
	public Boolean visit(TempPermission perm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean visit(Disjunction disj) {
		log.warning("Unsupported: " + disj);
		return null;
	}

	@Override
	public Boolean visit(Conjunction conj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean visit(Withing withing) {
		log.warning("Unsupported: " + withing);
		return null;
	}

	@Override
	public Boolean visit(BinaryExprAP binaryExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean visit(EqualsExpr equalsExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean visit(NotEqualsExpr notEqualsExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean visit(StateOnly stateOnly) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean visit(PermissionImplication permissionImplication) {
		log.warning("Unsupported: " + permissionImplication);
		return null;
	}

}
