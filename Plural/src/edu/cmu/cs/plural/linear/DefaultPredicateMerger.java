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

package edu.cmu.cs.plural.linear;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.alias.ParamVariable;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.contexts.TensorPluralTupleLE;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.ReleaseHolder;
import edu.cmu.cs.plural.pred.PredicateMerger.MergeIntoTuple;

/**
 * Callbacks for merging a predicate into a {@link TensorPluralTupleLE}.
 * This implementation just forwards the calls to the tuple.  It also has
 * a flag {@link #isVoid()} that will be set to <code>true</code> when
 * {@link #addVoid()} is called.  Clients of this class should check this flag.
 * @author Kevin Bierhoff
 * @since Sep 15, 2008
 */
public class DefaultPredicateMerger implements MergeIntoTuple {

	protected TensorPluralTupleLE value;
	private boolean isVoid = false;
	private ASTNode astNode;

	public DefaultPredicateMerger(ASTNode astNode, TensorPluralTupleLE value) {
		this.astNode = astNode;
		this.value = value;
	}
	
	public boolean isVoid() {
		return isVoid;
	}

	@Override
	public void addImplication(Aliasing var, Implication implication) {
		value.addImplication(var, implication);
	}

	@Override
	public void addStateInfo(Aliasing var, String var_name, Set<String> stateInfo, boolean inFrame) {
		if(!stateInfo.isEmpty()) {
			FractionalPermissions perms = value.get(var);
			for(String s : stateInfo)
				perms = perms.learnTemporaryStateInfo(s, inFrame);
			value.put(var, perms);
		}
	}

	@Override
	public void mergeInPermission(Aliasing a, String var_name, 
			PermissionSetFromAnnotations perms) {
//		if(borrowed.containsKey(a)) {
//			// just update state info for borrowed locations
//			// (permissions were fixed in constructor)
//			FractionalPermissions ps = value.get(a);
//			for(String s : perms.getStateInfo(false))
//				ps = ps.learnTemporaryStateInfo(s, false);
//			for(String s : perms.getStateInfo(true))
//				ps = ps.learnTemporaryStateInfo(s, true);
//			value.put(a, ps);
//		}
//		else
			value = LinearOperations.mergeIn(a, value, perms);
	}

	@Override
	public void releaseParameter(Aliasing var, String param) {
		Aliasing loc = value.getLocationsBefore(astNode, new ParamVariable(var, param, null));

		if(loc == null || loc.getLabels().isEmpty())
			// parameter not instantiated --> throw away released permission
			return;
		
		List<ReleaseHolder> paramInfo = value.findImpliedParameter(var, loc);
		
		if(paramInfo == null || paramInfo.isEmpty())
			// no permission associated with parameter --> skip
			return;
		
		for(ReleaseHolder p : paramInfo) {
			p.putIntoLattice(value, loc, var, true);
		}
	}

	@Override
	public void addVoid() {
		isVoid = true;
	}
	
	@Override
	public void finishMerge() {
	}

	@Override
	public void addFalse(Aliasing var, String var_name) {
		value.addFalseVarPredicate(var);
	}

	@Override
	public void addNonNull(Aliasing var, String var_name) {
		value.addNonNullVariable(var);
	}

	@Override
	public void addNull(Aliasing var, String var_name) {
		value.addNullVariable(var);
	}

	@Override
	public void addTrue(Aliasing var, String var_name) {
		value.addTrueVarPredicate(var);
	}

}