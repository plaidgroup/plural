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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * @author Kevin Bierhoff
 * @since 7/28/2008
 */
public class ParamPostMergerConj extends AbstractParamVisitor implements AccessPredVisitor<Boolean> {
	
	private static final Logger log = Logger.getLogger(ParamPostMergerConj.class.getName());
	
	private final Set<Pair<ParamPostMergerConj, ParamPostMergerConj>> impls = 
		new LinkedHashSet<Pair<ParamPostMergerConj, ParamPostMergerConj>>();
	
	private ParamPostMergerConj(
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual) {
		super(new LinkedHashMap<String, PermissionSetFromAnnotations>(), 
				spaces, frameToVirtual, true /* named fractions */);
	}
	
	public ParamPostMergerConj(
			Map<String, PermissionSetFromAnnotations> perms,
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual) {
		super(perms, spaces, frameToVirtual, true /* named fractions */);
	}
	
	public void applyPostcondition(SimpleMap<String, Aliasing> vars, MergeIntoTuple callback) {
		
		/*
		 * 1. merge in permissions
		 */
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			Aliasing var = vars.get(param.getKey());
			callback.mergeInPermission(var, param.getValue().getPerms());
		}
		
		/*
		 * 2. add concrete predicates
		 */
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			Aliasing var = vars.get(param.getKey());
			callback.addStateInfo(var, param.getValue().getStateInfos());
			// TODO null / non-null, true / false
		}
	
		// TODO implications
		
		/*
		 * 4. finish post-condition
		 */
		callback.finishMerge();
	}
	
	@Override
	public Boolean visit(PermissionImplication permissionImplication) {
		ParamPostMergerConj anteVisitor = new ParamPostMergerConj(getSpaces(), isFrameToVirtual());
		ParamPostMergerConj consVisitor = new ParamPostMergerConj(getSpaces(), isFrameToVirtual());
		impls.add(Pair.create(anteVisitor, consVisitor));
		return null;
	}

}
