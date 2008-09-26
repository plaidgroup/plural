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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.concrete.ImplicationResult;
import edu.cmu.cs.plural.concrete.VariablePredicate;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.ParamInfoHolder.InfoHolderPredicate;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;
import edu.cmu.cs.plural.track.Permission.PermissionKind;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * Common helper methods for parsing method parameter annotations
 * 
 * @author Kevin Bierhoff
 * @since 7/28/2008
 */
public abstract class AbstractParamVisitor 
		implements AccessPredVisitor<Boolean>, PredicateChecker, PredicateMerger {

	/**
	 * This class allows us to have implications in the post-conditions of methods.
	 * It is the implication itself, and was created instead of using the
	 * existing implications in the plural.concrete package. 
	 */
	protected static class ParamImplication implements Implication, ImplicationResult {
		
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
		public String toString() {
			return ant + " implies TENS " + cons;
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

		@Override
		public Set<Aliasing> getConsequenceVariables() {
			HashSet<Aliasing> result = new HashSet<Aliasing>();
			for(InfoHolderPredicate p : cons) {
				result.add(p.getVariable());
			}
			return result;
		}
		
	}

	private static final Logger log = Logger.getLogger(AbstractParamVisitor.class.getName());

	private final SimpleMap<String, StateSpace> spaces;

	private final boolean frameToVirtual;
	
	private final Map<String, ParamInfoHolder> params;
	
	private PermissionFactory pf = PermissionFactory.INSTANCE;

	protected final Set<Pair<AbstractParamVisitor, AbstractParamVisitor>> impls = 
		new LinkedHashSet<Pair<AbstractParamVisitor, AbstractParamVisitor>>();

	private final boolean named;

	protected AbstractParamVisitor(Map<String, PermissionSetFromAnnotations> perms, 
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual, boolean namedFractions) {
		this.params = new LinkedHashMap<String, ParamInfoHolder>(perms.size());
		for(Map.Entry<String, PermissionSetFromAnnotations> p : perms.entrySet()) {
			ParamInfoHolder h = new ParamInfoHolder();
			h.setPerms(p.getValue());
			params.put(p.getKey(), h);
		}
		this.spaces = spaces;
		this.frameToVirtual = frameToVirtual;
		this.named = namedFractions;
	}
	
	//
	// PredicateChecker template
	// 

	@Override
	public boolean splitOffPredicate(SimpleMap<String, Aliasing> vars, SplitOffTuple callback) {
		
		/*
		 * 1. Before-checks in subclasses. 
		 */
		
		if(! beforeChecks(vars, callback))
			return false;
		
		/*
		 * 2. check concrete predicates
		 */
		
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			String paramName = param.getKey();
			Aliasing var = vars.get(paramName);
			final ParamInfoHolder h = param.getValue();
			if(h.hasStateInfo() && ! callback.checkStateInfo(var, h.getStateInfos(), "this!fr".equals(paramName))) {
				// TODO figure out whether we're checking frame or virtual states
				return false;
			}
			if(h.hasNull() && ! callback.checkNull(var)) {
				return false;
			}
			if(h.hasNonNull() && ! callback.checkNonNull(var)) {
				return false;
			}
			if(h.hasTrue() && ! callback.checkTrue(var)) {
				return false;
			}
			if(h.hasFalse() && ! callback.checkFalse(var)) {
				return false;
			}
		}
		
		/*
		 * 3. split off permissions
		 */
		
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			Aliasing var = vars.get(param.getKey());
			if(! callback.splitOffPermission(var, param.getValue().getPerms()))
				return false;
		}
		
		/*
		 * 4. additional checks in subclasses.
		 */
		
		if(! finishSplit(vars, callback))
			return false;
		
		/*
		 * 5. post-processing (e.g., packing before call)
		 */
		
		return callback.finishSplit();
		
	}
	
	/** 
	 * Override this method to do something before {@link #splitOffPredicate(SimpleMap, SplitOffTuple)}
	 * starts its dirty work.  By default, this method returns <code>true</code>.
	 * @param vars
	 * @param callback
	 * @return <code>true</code> if checking should proceed, <code>false</code>
	 * otherwise.
	 */
	protected boolean beforeChecks(SimpleMap<String, Aliasing> vars,
			SplitOffTuple callback) {
		return true;
	}
	
	/**
	 * Override this method to do additional checks after the default ones.
	 * By default, this method returns <code>true</code>.
	 * @param callback 
	 * @param vars 
	 * @return <code>true</code> if checking should continue, <code>false</code>
	 * otherwise.
	 */
	protected boolean finishSplit(SimpleMap<String, Aliasing> vars, SplitOffTuple callback) {
		return true;
	}

	//
	// PredicateMerger template
	//

	public void mergeInPredicate(SimpleMap<String, Aliasing> vars, MergeIntoTuple callback) {
		
		/*
		 * 1. Pre-processing in subclasses.
		 */
		
		beforeMerge(vars, callback);
		
		/*
		 * 2. merge in permissions
		 */
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			Aliasing var = vars.get(param.getKey());
			callback.mergeInPermission(var, param.getKey(), param.getValue().getPerms());
		}
		
		/*
		 * 3. add concrete predicates
		 */
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			Aliasing var = vars.get(param.getKey());
			final ParamInfoHolder h = param.getValue();
			if(h.hasStateInfo())
				callback.addStateInfo(var, param.getKey(), h.getStateInfos(), false);
			if(h.hasNull())
				callback.addNull(var, param.getKey());
			if(h.hasNonNull())
				callback.addNonNull(var, param.getKey());
			if(h.hasTrue())
				callback.addTrue(var, param.getKey());
			if(h.hasFalse())
				callback.addFalse(var, param.getKey());
		}
		
		/*
		 * 4. declared implications
		 */
		for(Pair<AbstractParamVisitor, AbstractParamVisitor> impl : impls) {
			InfoHolderPredicate ant = impl.fst().createPredicate(vars);
			ParamImplication i = impl.snd().createImplication(ant, vars);
			callback.addImplication(ant.getVariable(), i);
		}
		
		/*
		 * 5. additional processing in subclasses.
		 */
		finishMerge(vars, callback);
	
		/*
		 * 6. finish post-condition
		 */
		callback.finishMerge();
	}

	/**
	 * Override this method to do additional stuff after
	 * the standard merge operations were executed.  By default, this
	 * method does nothing.
	 * @param vars
	 * @param callback
	 */
	protected void finishMerge(SimpleMap<String, Aliasing> vars,
			MergeIntoTuple callback) {
	}
	
	/**
	 * Override this method to do additional stuff after
	 * the standard merge operations were executed.  By default,
	 * this method does nothing.
	 * @param vars
	 * @param callback
	 */
	protected void beforeMerge(SimpleMap<String, Aliasing> vars,
			MergeIntoTuple callback) {
	}

	//
	// Predicate visitor
	//

	@Override
	public Boolean visit(TempPermission perm) {
		RefExpr ref = perm.getRef();
		PermissionKind p_type = PermissionKind.valueOf(perm.getType().toUpperCase());
		StateSpace space = getStateSpace(ref);
		if(space != null) {
			Pair<String, Boolean> refPair = getRefPair(ref);
			boolean isFrame = refPair.snd();
			PermissionFromAnnotation pa = 
				pf.createOrphan(space, perm.getRoot(), p_type, isFrame, perm.getStateInfo(), named);
			addPerm(refPair.fst(), pa);
		}
		return null;
	}

	/**
	 * @param ref
	 * @return Pair (parameter name, frame permission) for the given ref.
	 */
	protected Pair<String, Boolean> getRefPair(RefExpr ref) {
		String param;
		boolean isFrame = false;
		if(ref instanceof Identifier) {
			param = ((Identifier) ref).getName();
			// this flag decides whether a frame or virtual permission is created, so frameToVirtual matters
			isFrame = !isFrameToVirtual() && ((Identifier) ref).isFrame();
			if(isFrame /*((Identifier) ref).isFrame()*/)
				// TODO keep permissions declared for receiver frame and virtual separate, even if coerced
				param = param + "!fr";
		}
		else if(ref instanceof ParamReference) {
			param = ((ParamReference) ref).getParamString();
		}
		else
			throw new IllegalArgumentException("Unknown ref: " + ref);
		return Pair.create(param, isFrame);
	}

	/**
	 * @param ref
	 * @param pa
	 */
	protected void addPerm(String param, PermissionFromAnnotation pa) {
		ParamInfoHolder ps = getInfoHolder(param);
		ps.addPerm(pa);
	}

	/**
	 * @param param
	 * @return
	 */
	private ParamInfoHolder getInfoHolder(String param) {
		ParamInfoHolder ps = params.get(param);
		if(ps == null) {
			ps = new ParamInfoHolder();
			params.put(param, ps);
		}
		return ps;
	}

	@Override
	public Boolean visit(Disjunction disj) {
		log.warning("Ignore disjunction: " + disj);
		return null;
	}

	@Override
	public Boolean visit(Conjunction conj) {
		conj.getP1().accept(this);
		conj.getP2().accept(this);
		return null;
	}

	@Override
	public Boolean visit(Withing withing) {
		log.warning("Ignore with: " + withing);
		return null;
	}

	@Override
	public Boolean visit(BinaryExprAP binaryExpr) {
		handleBinary(binaryExpr.getBinExpr());
		return null;
	}

	private void handleBinary(BinaryExpr expr) {
		boolean negate = expr instanceof NotEqualsExpr;
		
		Object e1 = mapPrimary(expr.getE1());
		Object e2 = mapPrimary(expr.getE2());
		
		if(e1 == e2) {
			if(negate)
				log.warning("Ignore contradiction: " + expr);
			// ignore trivial equality
		}
		else if(e1 == null && e2 instanceof String) {
			equate(expr, (String) e2, e1, negate);
		}
		else if(e1 instanceof String) {
			equate(expr, (String) e1, e2, negate);
		}
		else if(e2 instanceof Aliasing) {
			equate(expr, (String) e2, e1, negate);
		}
		else {
			log.warning("Ignore expression: " + expr);
		}
	}

	private void equate(BinaryExpr expr, String x, Object other, boolean negate) {
		ParamInfoHolder h = getInfoHolder(x);
		if(other == null) {
			h.addNull(negate);
		}
		else if(other instanceof Boolean) {
			boolean b = (Boolean) other;
			h.addBoolean(negate ? !b : b);
		}
		else if(other instanceof String) {
			log.warning("Ignoring variable relation: " + expr);
			// TODO support for relations between variables
		}
		else
			throw new IllegalArgumentException("Don't know what to do: " + expr);
	}

	/**
	 * Returns the corresponding primitive value for {@link Null} and
	 * {@link BoolLiteral}s and an {@link Aliasing} object for {@link
	 * Identifier}.
	 * @param e
	 * @return a pair of a boolean that is <code>true</code> if
	 * the primary is to be ignored (because it corresponds to the assigned-to
	 * field and the corresponding primitive value for {@link Null} and
	 * {@link BoolLiteral}s and an {@link Aliasing} object for {@link
	 * Identifier}s.
	 */
	private Object mapPrimary(PrimaryExpr e) {
		return e.dispatch(new PrimaryExprVisitor<Object>() {

			@Override
			public Object visitBool(BoolLiteral bool) {
				return bool.hasValue(true);
			}

			@Override
			public Object visitId(Identifier id) {
				if(id.isFrame())
					log.warning("Frame referenece in primary not supported: " + id);
				return id.getName();
			}

			@Override
			public Object visitNull(Null nul) {
				return null;
			}

			@Override
			public Object visitParam(ParamReference paramReference) {
				return paramReference.getParamString();
			}
			
		});
	}

	@Override
	public Boolean visit(EqualsExpr equalsExpr) {
		handleBinary(equalsExpr);
		return null;
//		throw new IllegalStateException("Should be handled as BinaryExprAP");
	}

	@Override
	public Boolean visit(NotEqualsExpr notEqualsExpr) {
		handleBinary(notEqualsExpr);
		return null;
//		throw new IllegalStateException("Should be handled as BinaryExprAP");
	}

	@Override
	public Boolean visit(StateOnly stateOnly) {
		Pair<String, Boolean> r = getRefPair(stateOnly.getVar());
		String param = r.fst();
		getInfoHolder(param).getStateInfos().add(stateOnly.getStateInfo());
		return null;
	}
	
	/**
	 * @param ref
	 * @return
	 */
	protected StateSpace getStateSpace(RefExpr ref) {
		String n;
		if(ref instanceof Identifier) {
			Identifier id = (Identifier) ref;
			n = id.getName();
		}
		else if(ref instanceof ParamReference) {
			ParamReference pref = (ParamReference) ref;
			n = pref.getParamString();
		}
		else {
			log.warning("Unknown reference: " + ref);
			return null;
		}
		return spaces.get(n);
	}

	/**
	 * @return the pf
	 */
	protected PermissionFactory getPf() {
		return pf;
	}

	/**
	 * @param pf the pf to set
	 */
	public void setPf(PermissionFactory pf) {
		this.pf = pf;
	}

	/**
	 * @return the frameToVirtual
	 */
	protected boolean isFrameToVirtual() {
		return frameToVirtual;
	}

	/**
	 * @return the perms
	 */
	Map<String, ParamInfoHolder> getParams() {
		return params;
	}

	/**
	 * @return the spaces
	 */
	protected SimpleMap<String, StateSpace> getSpaces() {
		return spaces;
	}

	@Override
	public Boolean visit(PermissionImplication permissionImplication) {
		AbstractParamVisitor anteVisitor = createSubParser(!isNamedFractions()); 
			
//			new MethodPostconditionParser(
//				getSpaces(), isFrameToVirtual(), 
//				! isNamedFractions() /* negate for antecedent */);
		AbstractParamVisitor consVisitor = createSubParser(isNamedFractions()); 
//			new MethodPostconditionParser(
//				getSpaces(), isFrameToVirtual(), isNamedFractions());
		permissionImplication.ant().accept(anteVisitor);
		permissionImplication.cons().accept(consVisitor);
		impls.add(Pair.create(anteVisitor, consVisitor));
		return null;
	}

	protected abstract AbstractParamVisitor createSubParser(boolean namedFraction);

	protected InfoHolderPredicate createPredicate(SimpleMap<String, Aliasing> vars) {
		assert impls.isEmpty();
		assert getParams().size() == 1 : "Not a single antecedent: " + getParams().keySet();
		Map.Entry<String, ParamInfoHolder> singleton = getParams().entrySet().iterator().next();
		return singleton.getValue().createInfoPredicate(vars.get(singleton.getKey()));
	}

	protected ParamImplication createImplication(InfoHolderPredicate antecedant, SimpleMap<String, Aliasing> vars) {
		assert impls.isEmpty();
		ArrayList<InfoHolderPredicate> cons = new ArrayList<InfoHolderPredicate>(getParams().size());
		for(Map.Entry<String, ParamInfoHolder> h : getParams().entrySet()) {
			cons.add(h.getValue().createInfoPredicate(vars.get(h.getKey())));
		}
		return new ParamImplication(antecedant, cons);
	}

	protected boolean isNamedFractions() {
		return named;
	}

}
