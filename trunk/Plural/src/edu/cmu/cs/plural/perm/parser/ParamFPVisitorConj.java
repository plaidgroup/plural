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

import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * This class is especially unusual because it does things so differently than
 * FieldConjunectionFPVisitor. Unlike that one, this class doesn't create the
 * permissions immediately. It creates a summary object that can then be
 * queried for permissions for each of the parameters, receiver and result.
 * 
 * @author Nels Beckman
 * @date Apr 3, 2008
 *
 */
public class ParamFPVisitorConj implements AccessPredVisitor<ParsedParameterSummary> {

	@Override
	public ParsedParameterSummary visit(TempPermission perm) {
		ParsedParameterSummary result = new ParsedParameterSummary();
		RefExpr ref = perm.getRef();
		PermissionKind p_type = PermissionKind.valueOf(perm.getType().toUpperCase());
		
		if( ref instanceof Identifier ) {
			Identifier id = (Identifier)ref;
			if( "this".equals(id.getName()) ) {
				result.addRcvr(perm.getRoot(), perm.getStateInfo(), p_type, id.isFrame());
			}
			else if( "result".equals(id.getName()) ) {
				result.addResult(perm.getRoot(), perm.getStateInfo(), p_type, id.isFrame());
			}
			else {
				throw new RuntimeException("You are probably using a parameter " +
						"name instead of a parameter number.");
			}
		}
		else if( ref instanceof ParamReference ) {
			ParamReference pref = (ParamReference)ref;
			result.addParam(pref.getParamPosition(), perm.getRoot(), perm.getStateInfo(), p_type);
		}
		else {
			assert(false) : "Impossible";
		}
		return result;
	}

	@Override
	public ParsedParameterSummary visit(Conjunction conj) {
		ParsedParameterSummary s1 = conj.getP1().accept(this);
		ParsedParameterSummary s2 = conj.getP2().accept(this);
		ParsedParameterSummary result = ParsedParameterSummary.union(s1, s2);
		return result;
	}

	@Override
	public ParsedParameterSummary visit(Disjunction disj) {
		throw new RuntimeException("We don't deal with &.");
	}
	
	@Override
	public ParsedParameterSummary visit(Withing withing) {
		throw new RuntimeException("We don't deal with &.");
	}

	@Override
	public ParsedParameterSummary visit(BinaryExprAP binaryExpr) {
		throw new RuntimeException("We don't deal with general expressions.");
	}

	@Override
	public ParsedParameterSummary visit(EqualsExpr equalsExpr) {
		throw new RuntimeException("We don't deal with general expressions.");
	}

	@Override
	public ParsedParameterSummary visit(NotEqualsExpr notEqualsExpr) {
		throw new RuntimeException("We don't deal with general expressions.");
	}

	@Override
	public ParsedParameterSummary visit(StateOnly stateOnly) {
		throw new RuntimeException("We don't deal with stateOnly in method annotations.");
	}

	@Override
	public ParsedParameterSummary visit(
			PermissionImplication permissionImplication) {
		throw new RuntimeException("We don't deal with implications in method annotations.");
	}
}
