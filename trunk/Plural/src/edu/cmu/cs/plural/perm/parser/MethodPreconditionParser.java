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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.pred.MethodPrecondition;
import edu.cmu.cs.plural.states.StateSpace;

/**
 * @author Kevin Bierhoff
 * @since 7/28/2008
 */
class MethodPreconditionParser extends AbstractParamVisitor 
			implements AccessPredVisitor<Boolean>, MethodPrecondition {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(MethodPreconditionParser.class.getName());
	
	private Set<String> notBorrowed;

	public static MethodPreconditionParser createPreconditionForCallSite(
			Map<String, PermissionSetFromAnnotations> perms,
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual, boolean ignoreReceiverVirtual, 
			Set<String> notBorrowed) {
		return new MethodPreconditionParser(perms, spaces, 
				frameToVirtual /* chosen by caller */, 
				ignoreReceiverVirtual /* chosen by caller */,
				FractionCreation.VARIABLE_UNIVERSAL /* variable fractions */,
				new LinkedHashSet<String>(notBorrowed));
	}	
	
	public static MethodPreconditionParser createPreconditionForAnalyzingBody(
			Map<String, PermissionSetFromAnnotations> perms,
			SimpleMap<String, StateSpace> spaces,
			Set<String> notBorrowed) {
		return new MethodPreconditionParser(perms, spaces, 
				false /* no frame-to-virtual coercion */, 
				false /* never ignore virtual receiver permissions in body analysis */, 
				FractionCreation.NAMED_UNIVERSAL /* named fractions */,
				new LinkedHashSet<String>(notBorrowed));
	}	
	
	private MethodPreconditionParser(
			Map<String, PermissionSetFromAnnotations> perms,
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual, boolean ignoreReceiverVirtual, 
			FractionCreation namedFractions, 
			Set<String> notBorrowed) {
		super(perms, spaces, frameToVirtual, ignoreReceiverVirtual, namedFractions);
		this.notBorrowed = notBorrowed;
	}
	
	@Override
	public boolean isReadOnly() {
		for(ParamInfoHolder h : getParams().values()) {
			PermissionSetFromAnnotations pa = h.getPerms();
			if(!pa.isReadOnly())
				return false;
		}
		return true;
	}

	@Override
	protected boolean beforeChecks(SimpleMap<String, Aliasing> vars,
			SplitOffTuple callback) {
		
		/*
		 * announce borrowed objects 
		 */
		
		Set<Aliasing> borrowed_vars = new HashSet<Aliasing>();
		for(String p : getParams().keySet()) {
			borrowed_vars.add(vars.get(p));
		}
		for(String p : notBorrowed) {
			borrowed_vars.remove(vars.get(p));
		}
		callback.announceBorrowed(borrowed_vars);

		return super.beforeChecks(vars, callback);
	}

	@Override
	protected void addPerm(String param, PermissionFromAnnotation pa) {
		notBorrowed.add(param); // assume that any parsed permission makes the parameter not borrowed
		super.addPerm(param, pa);
	}

	@Override
	protected AbstractParamVisitor createSubParser(FractionCreation namedFraction) {
		return new MethodPreconditionParser(Collections.<String, PermissionSetFromAnnotations>emptyMap(),
				getSpaces(),
				isFrameToVirtual(),
				ignoreReceiverVirtual(),
				namedFraction,
				notBorrowed);
	}

}
