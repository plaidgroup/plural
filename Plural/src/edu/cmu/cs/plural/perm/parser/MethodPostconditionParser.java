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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.concrete.ImplicationResult;
import edu.cmu.cs.plural.concrete.VariablePredicate;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.linear.PermissionPredicate;
import edu.cmu.cs.plural.linear.ReleasePermissionImplication;
import edu.cmu.cs.plural.perm.parser.ParamInfoHolder.InfoHolderPredicate;
import edu.cmu.cs.plural.pred.MethodPostcondition;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
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

	private final Set<Pair<MethodPostconditionParser, MethodPostconditionParser>> impls = 
		new LinkedHashSet<Pair<MethodPostconditionParser, MethodPostconditionParser>>();
	
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
		 * declared implications
		 */
		for(Pair<MethodPostconditionParser, MethodPostconditionParser> impl : impls) {
			InfoHolderPredicate ant = impl.fst().createPredicate(vars);
			ParamImplication i = impl.snd().createImplication(ant, vars);
			callback.addImplication(ant.getVariable(), i);
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
	public Boolean visit(PermissionImplication permissionImplication) {
		MethodPostconditionParser anteVisitor = new MethodPostconditionParser(
				getSpaces(), isFrameToVirtual(), 
				! isNamedFractions() /* negate for antecedent */);
		MethodPostconditionParser consVisitor = new MethodPostconditionParser(
				getSpaces(), isFrameToVirtual(), isNamedFractions());
		permissionImplication.ant().accept(anteVisitor);
		permissionImplication.cons().accept(consVisitor);
		impls.add(Pair.create(anteVisitor, consVisitor));
		return null;
	}
	
	private InfoHolderPredicate createPredicate(SimpleMap<String, Aliasing> vars) {
		assert impls.isEmpty();
		assert getParams().size() == 1 : "Not a single antecedent: " + getParams().keySet();
		Map.Entry<String, ParamInfoHolder> singleton = getParams().entrySet().iterator().next();
		return singleton.getValue().createInfoPredicate(vars.get(singleton.getKey()));
	}
	
	private ParamImplication createImplication(InfoHolderPredicate antecedant, SimpleMap<String, Aliasing> vars) {
		assert impls.isEmpty();
		ArrayList<InfoHolderPredicate> cons = new ArrayList<InfoHolderPredicate>(getParams().size());
		for(Map.Entry<String, ParamInfoHolder> h : getParams().entrySet()) {
			cons.add(h.getValue().createInfoPredicate(vars.get(h.getKey())));
		}
		return new ParamImplication(antecedant, cons);
	}
	
	private static class ParamImplication implements Implication, ImplicationResult {
		
		private InfoHolderPredicate ant;
		private List<InfoHolderPredicate> cons;

		public ParamImplication(InfoHolderPredicate ant,
				List<InfoHolderPredicate> cons) {
			super();
			this.ant = ant;
			this.cons = Collections.unmodifiableList(cons);
		}

		@Override
		public ParamImplication createCopyWithNewAntecedant(Aliasing other) {
			return new ParamImplication(ant.createIdenticalPred(other), cons);
		}

		@Override
		public ParamImplication createCopyWithOppositeAntecedant(Aliasing other) {
			return new ParamImplication(ant.createOppositePred(other), cons);
		}

		@Override
		public ParamImplication createCopyWithoutTemporaryState() {
			List<InfoHolderPredicate> newPs = new LinkedList<InfoHolderPredicate>();
			for(InfoHolderPredicate p : cons) {
				p = p.createCopyWithoutTemporaryState();
				if(p != null)
					newPs.add(p);
			}
			if(newPs.isEmpty())
				return null; // all dropped...
			return new ParamImplication(ant, newPs);
		}

		@Override
		public VariablePredicate getAntecedant() {
			return ant;
		}

		@Override
		public boolean hasTemporaryState() {
			for(InfoHolderPredicate p : cons) {
				if(p.hasTemporaryState())
					return true;
			}
			return false;
		}

		@Override
		public boolean match(VariablePredicate pred) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ImplicationResult result() {
			return this;
		}

		@Override
		public boolean supportsMatch() {
			return false;
		}

		@Override
		public boolean isSatisfied(PluralTupleLatticeElement value) {
			final Aliasing anteVar = ant.getVariable();
			if(value.isKnownImplication(anteVar, this))
				return true;
			
			if(ant.isUnsatisfiable(value))
				// antecedent is false --> implication trivially holds
				return true;
			
			for(InfoHolderPredicate p : cons) {
				if(! p.isSatisfied(value))
					return false;
			}
			return true;
		}

		@Override
		public PluralTupleLatticeElement putResultIntoLattice(
				PluralTupleLatticeElement value) {
			ant.removeFromLattice(value);
			value.removeImplication(ant.getVariable(), this);
			for(InfoHolderPredicate p : cons) {
				p.putIntoLattice(value);
			}
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ant == null) ? 0 : ant.hashCode());
			result = prime * result + ((cons == null) ? 0 : cons.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ParamImplication other = (ParamImplication) obj;
			if (ant == null) {
				if (other.ant != null)
					return false;
			} else if (!ant.equals(other.ant))
				return false;
			if (cons == null) {
				if (other.cons != null)
					return false;
			} else if (!cons.equals(other.cons))
				return false;
			return true;
		}
		
	}

}
