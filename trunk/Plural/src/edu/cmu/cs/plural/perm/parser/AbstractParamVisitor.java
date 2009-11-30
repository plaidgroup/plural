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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.ParamInfoHolder.InfoHolderPredicate;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * Common helper methods for parsing method parameter annotations
 * 
 * @author Kevin Bierhoff
 * @since 7/28/2008
 */
public abstract class AbstractParamVisitor 
		implements AccessPredVisitor<Boolean>, PredicateChecker, PredicateMerger {
	
	/**
	 * Determines how quantified fractions should be created.
	 * @author Kevin Bierhoff
	 * @since Nov 4, 2008
	 *
	 */
	public enum FractionCreation {
		/** 
		 * Creates {@link edu.cmu.cs.plural.fractions.VariableFraction}s
		 * representing existentially quantified fractions.  
		 * {@link edu.cmu.cs.plural.fractions.NamedFraction}s therefore
		 * represent universally quantified fractions.
		 */
		VARIABLE_EXISTENTIAL {
			@Override public boolean createNamed() { return false; }
			@Override public boolean isNamedUniversal() { return true; }
			@Override public FractionCreation opposite() { return NAMED_UNIVERSAL; }
		}, 
		/** 
		 * Creates {@link edu.cmu.cs.plural.fractions.VariableFraction}s
		 * representing universally quantified fractions.  
		 * {@link edu.cmu.cs.plural.fractions.NamedFraction}s therefore
		 * represent existentially quantified fractions.
		 */
		VARIABLE_UNIVERSAL {
			@Override public boolean createNamed() { return false; }
			@Override public boolean isNamedUniversal() { return false; }
			@Override public FractionCreation opposite() { return NAMED_EXISTENTIAL; }
		}, 
		/** 
		 * Creates {@link edu.cmu.cs.plural.fractions.NamedFraction}s
		 * representing existentially quantified fractions.  
		 * {@link edu.cmu.cs.plural.fractions.VariableFraction}s therefore
		 * represent universally quantified fractions.
		 */
		NAMED_EXISTENTIAL {
			@Override public boolean createNamed() { return true; }
			@Override public boolean isNamedUniversal() { return false; }
			@Override public FractionCreation opposite() { return VARIABLE_UNIVERSAL; }
		}, 
		/** 
		 * Creates {@link edu.cmu.cs.plural.fractions.NamedFraction}s
		 * representing universally quantified fractions.  
		 * {@link edu.cmu.cs.plural.fractions.VariableFraction}s therefore
		 * represent existentially quantified fractions.
		 */
		NAMED_UNIVERSAL {
			@Override public boolean createNamed() { return true; }
			@Override public boolean isNamedUniversal() { return true; }
			@Override public FractionCreation opposite() { return VARIABLE_EXISTENTIAL; }
		};
		
		/** 
		 * Indicates whether fraction names are turned into 
		 * {@link edu.cmu.cs.plural.fractions.NamedFraction}s.
		 * @return <code>true</code> if fraction names are turned into
		 * {@link edu.cmu.cs.plural.fractions.NamedFraction}s, 
		 * <code>false</code> if they are turned into
		 * {@link edu.cmu.cs.plural.fractions.VariableFraction}s.
		 */
		public abstract boolean createNamed();
		
		/**
		 * Indicates whether {@link edu.cmu.cs.plural.fractions.NamedFraction}s 
		 * represent universally quantified fractions. 
		 * @return <code>true</code> if {@link edu.cmu.cs.plural.fractions.NamedFraction}s
		 * represent universally quantified fractions, <code>false</code> if they
		 * represent existentially quantified fractions.
		 */
		public abstract boolean isNamedUniversal();
		
		/**
		 * This is used to create fractions in implication antecedents.
		 * Returns the inverse <code>FractionCreation</code> policy,
		 * swapping "universal" with "existential" and "named" with "variable".
		 * @return the inverse <code>FractionCreation</code> policy
		 */
		public abstract FractionCreation opposite();
	}

	private static final Logger log = Logger.getLogger(AbstractParamVisitor.class.getName());

	private final SimpleMap<String, StateSpace> spaces;

	private final boolean frameToVirtual;
	
	private final boolean ignoreReceiverVirtual;
	
	private final Map<String, ParamInfoHolder> params;
	
	private PermissionFactory pf = PermissionFactory.INSTANCE;

	protected final Set<Pair<AbstractParamVisitor, AbstractParamVisitor>> impls = 
		new LinkedHashSet<Pair<AbstractParamVisitor, AbstractParamVisitor>>();

	private final FractionCreation named;

	protected AbstractParamVisitor(Map<String, PermissionSetFromAnnotations> perms, 
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual,
			boolean ignoreReceiverVirtual,
			FractionCreation namedFractions) {
		this.params = new LinkedHashMap<String, ParamInfoHolder>(perms.size());
		for(Map.Entry<String, PermissionSetFromAnnotations> p : perms.entrySet()) {
			ParamInfoHolder h = new ParamInfoHolder();
			h.setPerms(p.getValue());
			params.put(p.getKey(), h);
		}
		this.spaces = spaces;
		this.frameToVirtual = frameToVirtual;
		this.ignoreReceiverVirtual = ignoreReceiverVirtual;
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
			if(h.hasStateInfo() && ! callback.checkStateInfo(
					var, paramName, h.getStateInfos(), "this!fr".equals(paramName))) {
				// TODO figure out whether we're checking frame or virtual states
				return false;
			}
			if(h.hasNull() && ! callback.checkNull(var, paramName)) {
				return false;
			}
			if(h.hasNonNull() && ! callback.checkNonNull(var, paramName)) {
				return false;
			}
			if(h.hasTrue() && ! callback.checkTrue(var, paramName)) {
				return false;
			}
			if(h.hasFalse() && ! callback.checkFalse(var, paramName)) {
				return false;
			}
		}
		
		/*
		 * 3. Check implications.
		 */
		for(Pair<AbstractParamVisitor, AbstractParamVisitor> impl : impls) {
			// NEB: This is definitely weird. Why create a param implication?
			InfoHolderPredicate ant = impl.fst().createPredicate(vars);
			ImplicationOfAPermission i = impl.snd().createImplication(ant, vars);
			
			if( !callback.checkImplication(ant.getVariable(), i) )
				return false;
		}
		
		/*
		 * 4. split off permissions
		 */
		
		for(Map.Entry<String, ParamInfoHolder> param : getParams().entrySet()) {
			Aliasing var = vars.get(param.getKey());
			if( param.getValue().getPerms() != null && 
				!callback.splitOffPermission(var, param.getKey(), param.getValue().getPerms()) )
				return false;
		}
		
		/*
		 * 5. additional checks in subclasses.
		 */
		
		if(! finishSplit(vars, callback))
			return false;
		
		/*
		 * 6. post-processing (e.g., packing before call)
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
			ImplicationOfAPermission i = impl.snd().createImplication(ant, vars);
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

	/**
	 * Given a string returns the permission kind for this string, or
	 * if it is not a string returns a default, which is Share.
	 */
	private Option<PermissionKind> kindFromStringWithDefault(String type) {
		try {
			return
			Option.some(PermissionKind.valueOf(type.toUpperCase()));
		} catch(IllegalArgumentException iae) {
			return Option.none();
		}
	}
	
	@Override
	public Boolean visit(TempPermission perm) {
		RefExpr ref = perm.getRef();
		Option<PermissionKind> p_type = kindFromStringWithDefault(perm.getType().toUpperCase());
		StateSpace space = getStateSpace(ref);
		if(space != null && p_type.isSome() ) {
			Pair<String, PermissionUse> refPair = getRefPair(ref);
			if(refPair.snd() == null)
				// ignore
				return null;
			// may need both a virtual and a frame permission
			if(refPair.snd().isVirtual()) {
				assert !"super".equals(refPair.fst());
				PermissionFromAnnotation pa = 
					pf.createOrphan(space, perm.getRoot(), p_type.unwrap(), false, perm.getStateInfo(), named.createNamed());
				addPerm(refPair.fst(), pa);
			}
			if(refPair.snd().isFrame()) {
				PermissionFromAnnotation pa = 
					pf.createOrphan(space, perm.getRoot(), p_type.unwrap(), true, perm.getStateInfo(), named.createNamed());
				// construct canonical name for frame reference
				String r = refPair.fst();
				// super by definition refers to a frame, but turn this into this!fr
				if(!"super".equals(r))
					r += "!fr";
				addPerm(r, pa);
			}
		}
		return null;
	}

	@Override
	public Boolean visit(EmptyPredicate emptyPredicate) {
		return null;
	}

	/**
	 * @param ref
	 * @return {@link Pair} (parameter name, {@link PermissionUse}) for the given ref;
	 * second component is <code>null</code> if this reference should be ignored 
	 * because of {@link #ignoreReceiverVirtual()}.
	 */
	protected Pair<String, PermissionUse> getRefPair(RefExpr ref) {
		String param;
		PermissionUse use = PermissionUse.DISPATCH;
		if(ref instanceof Identifier) {
			param = ((Identifier) ref).getName();
			if("super".equals(param)) {
				// super always refers to a frame
				use = PermissionUse.FIELDS;
			}
			else {
				if("this".equals(((Identifier) ref).getName()) && ignoreReceiverVirtual()) {
					if(((Identifier) ref).getUse().equals(PermissionUse.DISPATCH))
						return Pair.create(param, null);
					if(!isFrameToVirtual())
						return Pair.create(param, PermissionUse.FIELDS);
				}
				if(!isFrameToVirtual())
					use = ((Identifier) ref).getUse();
				// else always use DISPATCH
			}
		}
		else if(ref instanceof ParamReference) {
			param = ((ParamReference) ref).getParamString();
		}
		else
			throw new IllegalArgumentException("Unknown ref: " + ref);
		return Pair.create(param, use);
	}

	/**
	 * @return
	 */
	protected final boolean ignoreReceiverVirtual() {
		return ignoreReceiverVirtual;
	}

	/**
	 * Adds the given permission to the info holder for the given parameter.
	 * @param param
	 * @param pa
	 */
	protected void addPerm(String param, PermissionFromAnnotation pa) {
		ParamInfoHolder ps = getInfoHolder(param);
		ps.addPerm(pa, named.isNamedUniversal());
	}

	/**
	 * Returns the info holder for the given parameter.  If
	 * the parameter was previously unknown then an empty info holder is returned.
	 * @param param
	 * @return the info holder for the given parameter, never <code>null</code>.
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
				if(PermissionUse.DISPATCH != id.getUse())
					log.warning("Frame reference in primary doesn't make sense: " + id);
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
	}

	@Override
	public Boolean visit(NotEqualsExpr notEqualsExpr) {
		handleBinary(notEqualsExpr);
		return null;
	}

	@Override
	public Boolean visit(StateOnly stateOnly) {
		Pair<String,PermissionUse> r = getRefPair(stateOnly.getVar());
		if(r.snd() == null)
			// ignore
			return null;
		String param = r.fst();
		if(r.snd().isVirtual()) {
			getInfoHolder(param).getStateInfos().add(stateOnly.getStateInfo());
		}
		if(r.snd().isFrame()) {
			if(!"super".equals(param))
				param += "!fr";
			getInfoHolder(param).getStateInfos().add(stateOnly.getStateInfo());
		}
		return null;
	}
	
	/**
	 * Returns the state space for the given reference.
	 * @param ref
	 * @return the state space for the given reference.
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
	 * Returns the permission factory in use.
	 * @return the pf
	 */
	protected PermissionFactory getPf() {
		return pf;
	}

	/**
	 * Sets the permission factory.
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
	 * @return the params
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
		AbstractParamVisitor anteVisitor = createSubParser(named.opposite()); 
		AbstractParamVisitor consVisitor = createSubParser(named); 
		permissionImplication.ant().accept(anteVisitor);
		permissionImplication.cons().accept(consVisitor);
		impls.add(Pair.create(anteVisitor, consVisitor));
		return null;
	}

	protected abstract AbstractParamVisitor createSubParser(FractionCreation named);

	protected InfoHolderPredicate createPredicate(SimpleMap<String, Aliasing> vars) {
		assert impls.isEmpty();
		assert getParams().size() == 1 : "Not a single antecedent: " + getParams().keySet();
		Map.Entry<String, ParamInfoHolder> singleton = getParams().entrySet().iterator().next();
		return singleton.getValue().createInfoPredicate(
				singleton.getKey(), vars.get(singleton.getKey()));
	}

	protected ImplicationOfAPermission createImplication(InfoHolderPredicate antecedant, SimpleMap<String, Aliasing> vars) {
		assert impls.isEmpty();
		ArrayList<InfoHolderPredicate> cons = new ArrayList<InfoHolderPredicate>(getParams().size());
		for(Map.Entry<String, ParamInfoHolder> h : getParams().entrySet()) {
			cons.add(h.getValue().createInfoPredicate(h.getKey(), vars.get(h.getKey())));
		}
		return new ImplicationOfAPermission(antecedant, cons);
	}

}
