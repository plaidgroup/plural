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
import java.util.LinkedList;
import java.util.Map;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.linear.PermissionPredicate;
import edu.cmu.cs.plural.linear.ReleasePermissionImplication;
import edu.cmu.cs.plural.perm.parser.ParamInfoHolder.InfoHolderPredicate;
import edu.cmu.cs.plural.pred.MethodPostcondition;
import edu.cmu.cs.plural.states.StateSpace;

/**
 * This class is will parse and create permissions from @Perm annotations,
 * from their 'ensures' fields.
 * 
 * @author Kevin Bierhoff
 * @since 7/28/2008
 */
class MethodPostconditionParser extends AbstractParamVisitor 
			implements AccessPredVisitor<Boolean>, MethodPostcondition {
	
	public static MethodPostconditionParser createPostconditionForCallSite(
			Map<String, PermissionSetFromAnnotations> perms,
			Map<String, ReleaseHolder> captured,
			String capturing, 
			Map<String, String> released,
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual) {
		return new MethodPostconditionParser(perms,  
				captured, capturing, released,
				spaces,
				frameToVirtual /* chosen by caller */, 
				true /* named fractions */);
	}

	public static MethodPostconditionParser createPostconditionForAnalyzingBody(
			Map<String, PermissionSetFromAnnotations> perms,
			Map<String, ReleaseHolder> captured,
			String capturing, 
			Map<String, String> released,
			SimpleMap<String, StateSpace> spaces) {
		return new MethodPostconditionParser(perms,  
				captured, capturing, released,
				spaces,
				false /* no frame-to-virtual coercion */, 
				false /* variable fractions */);
	}

	private final Map<String, ReleaseHolder> captured;
	
	private final Map<String, String> released;

	private String capturing;
	
	/**
	 * Use this constructor to create a new visitor for a given permission expression
	 * @param perms
	 * @param captured
	 * @param capturing
	 * @param released
	 * @param spaces
	 * @param frameToVirtual
	 * @param namedFractions
	 */
	private MethodPostconditionParser(
			Map<String, PermissionSetFromAnnotations> perms,
			Map<String, ReleaseHolder> captured,
			String capturing, 
			Map<String, String> released,
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual, boolean namedFractions) {
		super(perms, spaces, frameToVirtual, namedFractions);
		this.captured = captured;
		this.capturing = capturing;
		this.released = released;
	}

	/**
	 * This constructor is used for recursing into implications.
	 * @param spaces
	 * @param frameToVirtual
	 * @param namedFractions
	 */
	private MethodPostconditionParser(
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual, boolean namedFractions) {
		super(new LinkedHashMap<String, PermissionSetFromAnnotations>(), 
				spaces, frameToVirtual, namedFractions);
		this.captured = null;
		this.released = null;
	}
	
	@Override
	protected void finishMerge(SimpleMap<String, Aliasing> vars,
			MergeIntoTuple callback) {
		
		/*
		 * captured parameters
		 */
		
		if(captured != null && ! captured.isEmpty()) {
			ParamInfoHolder result_holder = getParams().get(capturing);
			Aliasing var = vars.get(capturing);
			assert result_holder != null && var != null : 
				"Capturing object unknown: " + capturing;
			
			LinkedList<Pair<Aliasing, ReleaseHolder>> l = new LinkedList<Pair<Aliasing, ReleaseHolder>>();
			for(Map.Entry<String, ReleaseHolder> cp : captured.entrySet()) {
				l.add(Pair.create(vars.get(cp.getKey()), cp.getValue()));
			}
			callback.addImplication(var, 
					new ReleasePermissionImplication(
							new PermissionPredicate(var, result_holder.getPerms()), 
							l));

		}
		
		/*
		 * Explicitly released parameters
		 */
		if(released != null && ! released.isEmpty()) {
			for(Map.Entry<String, String> r : released.entrySet()) {
				Aliasing var = vars.get(r.getKey());
				callback.releaseParameter(var, r.getValue());
			}
		}
		
		super.finishMerge(vars, callback);
	}

	@Override
	protected AbstractParamVisitor createSubParser(boolean namedFraction) {
		return new MethodPostconditionParser(getSpaces(),isFrameToVirtual(),namedFraction);
	}

}
