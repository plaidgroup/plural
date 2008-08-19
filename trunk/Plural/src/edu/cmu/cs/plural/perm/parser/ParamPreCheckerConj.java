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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * @author Kevin Bierhoff
 * @since 7/28/2008
 */
public class ParamPreCheckerConj extends AbstractParamVisitor implements AccessPredVisitor<Boolean> {
	
	private static final Logger log = Logger.getLogger(ParamPreCheckerConj.class.getName());
	
	private final Map<String, PermissionSetFromAnnotations> resultParams;

	private final Map<String, String> resultParamInstantiations;
	
	public ParamPreCheckerConj(
			Map<String, PermissionSetFromAnnotations> perms,
			Map<String, String> resultParamInstantiations,
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual) {
		super(perms, spaces, frameToVirtual, false /* variable fractoins */);
		this.resultParamInstantiations = resultParamInstantiations;
		this.resultParams = new LinkedHashMap<String, PermissionSetFromAnnotations>(resultParamInstantiations.size());
	}
	
	public boolean applyPrecondition(SimpleMap<String, Aliasing> vars, SplitOffTuple callback) {

		/*
		 * 1. check concrete predicates
		 */
		
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			Aliasing var = vars.get(param.getKey());
			if(! callback.checkStateInfo(var, param.getValue().getStateInfos()))
				return false;
			// TODO null / non-null, true / false
		}
		
		/*
		 * 2. split off permissions
		 */
		
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			Aliasing var = vars.get(param.getKey());
			if(! callback.splitOffPermission(var, param.getValue().getPerms()))
				return false;
		}

		/*
		 * 3. opportunity to do any post-processing (e.g., packing before call)
		 */
		
		return callback.finishSplit();
		
	}
	
	@Override
	protected void addPerm(String param, PermissionFromAnnotation pa) {
		String resultParam = getResultParam(param);
		if(resultParam != null)
			addResultParam(resultParam, pa);
		super.addPerm(param, pa);
	}

	/**
	 * @param resultParam
	 * @param pa
	 */
	private void addResultParam(String resultParam, PermissionFromAnnotation pa) {
		pa = pa.forgetStateInfo();
		PermissionSetFromAnnotations ps = resultParams.get(resultParam);
		if(ps == null)
			ps = PermissionSetFromAnnotations.createSingleton(pa);
		else
			ps = ps.combine(pa);
		resultParams.put(resultParam, ps);
	}

	/**
	 * @param param
	 * @return
	 */
	private String getResultParam(String param) {
		return resultParamInstantiations.get(param);
	}

	@Override
	public Boolean visit(PermissionImplication permissionImplication) {
		log.warning("Ignore implication: " + permissionImplication);
		return null;
	}

}
