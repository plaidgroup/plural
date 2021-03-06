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
package edu.cmu.cs.plural.contexts;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Modifier;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.bridge.LatticeElement;
import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Box;
import edu.cmu.cs.crystal.util.Freezable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.concrete.ImplicationResult;
import edu.cmu.cs.plural.errors.JoiningChoices;
import edu.cmu.cs.plural.fractions.Fraction;
import edu.cmu.cs.plural.fractions.FractionConstraint;
import edu.cmu.cs.plural.fractions.FractionalPermission;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.linear.DescendingVisitor;
import edu.cmu.cs.plural.linear.ErrorReportingVisitor;
import edu.cmu.cs.plural.linear.LinearOperations;
import edu.cmu.cs.plural.linear.RewritingVisitor;
import edu.cmu.cs.plural.linear.TestVisitor;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedTypeAnalysis;
import edu.cmu.cs.plural.polymorphic.instantiation.RcvrInstantiationPackage;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.states.IConstructorCaseInstance;
import edu.cmu.cs.plural.states.IConstructorSignature;
import edu.cmu.cs.plural.states.IMethodCaseInstance;
import edu.cmu.cs.plural.states.IMethodSignature;
import edu.cmu.cs.plural.states.MethodCheckingKind;
import edu.cmu.cs.plural.track.FractionAnalysisContext;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement.VariableLiveness;

/**
 * There are many different context classes in Plural, but this one is the top level context
 * which actually is has its methods called directly by the dataflow analysis engine of
 * Crystal.
 * 
 * @author Kevin Bierhoff
 *
 */
public class PluralContext implements LatticeElement<PluralContext>, Freezable<PluralContext> {
	
	private static final Logger log = Logger.getLogger(PluralContext.class.getName());
	
	private static final PluralContext BOTTOM = new PluralContext();
	
	/**
	 * @param start
	 * @return
	 */
	public static PluralContext createLE(LinearContext start,
			ITACAnalysisContext tacContext, FractionAnalysisContext fractContext) {
		InstantiatedTypeAnalysis typeAnalysis = new InstantiatedTypeAnalysis(tacContext, fractContext.getAnnoDB());
		return new PluralContext(start, new LinearOperations(tacContext, fractContext), typeAnalysis);
	}
	
	/**
	 * Returns a special "bottom" lattice element.
	 * @return Bottom
	 * TODO Use false context for bottom
	 */
	public static PluralContext bottom() {
		return BOTTOM;
	}

	/**
	 * Linear proof contexts or <code>null</code> if this is bottom.
	 */
	private LinearContext le;
	private final LinearOperations op;
	private final InstantiatedTypeAnalysis itypeAnalysis;
	
	private PluralContext() {
		this.le = null;
		this.op = null;
		this.itypeAnalysis = null;
	}
	
	/**
	 * @param le
	 * @param crystal 
	 * @param tacContext
	 * @param thisVar 
	 * @param fractContext
	 * @param reEntrant 
	 */
	private PluralContext(LinearContext le, AnnotationDatabase annoDB, 
			ITACAnalysisContext tacContext, 
			FractionAnalysisContext fractContext) {
		assert le != null;
		this.le = le;
		this.op = new LinearOperations(tacContext, fractContext);
		this.itypeAnalysis = new InstantiatedTypeAnalysis(tacContext, annoDB);
	}

	/**
	 * @param le
	 */
	private PluralContext(LinearContext le, LinearOperations op, InstantiatedTypeAnalysis typeAnalysis) {
		assert le != null;
		this.le = le;
		this.op = op;
		this.itypeAnalysis = typeAnalysis;
	}

	/**
	 * Returns a copy of the {@link LinearContext} held by this
	 * context.
	 */
	public LinearContext getLinearContext() {
		if( le == null )
			return null;
		else
			return le.copy();
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.flow.LatticeElement, org.eclipse.jdt.core.dom.ASTNode)
	 */
	@Override
	public boolean atLeastAsPrecise(PluralContext other, ASTNode node) {
		freeze();
		if(this == other)
			return true;
		if(other.isBottom())
			return false;
		other.freeze();
		if(this.isBottom())
			return true;
		assert this.op == other.op;
		return this.le.atLeastAsPrecise(other.le, node);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#copy()
	 */
	@Override
	public PluralContext copy() {
		freeze();
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#join(edu.cmu.cs.crystal.flow.LatticeElement, org.eclipse.jdt.core.dom.ASTNode)
	 */
	@Override
	public PluralContext join(PluralContext other, ASTNode node) {
		freeze();
		if(this == other)
			return this;
		other.freeze();
		if(other.isBottom())
			return this;
		if(this.isBottom())
			return other;
		assert this.op == other.op;
		return new PluralContext(this.le.join(other.le, node, 
				JoiningChoices.NOT_JOINING_CHOICES).compact(node, true), op, this.itypeAnalysis);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.internal.Freezable#freeze()
	 */
	@Override
	public PluralContext freeze() {
		if(!isBottom())
			le = le.compact(null, true);
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.internal.Freezable#mutableCopy()
	 */
	@Override
	public PluralContext mutableCopy() {
		return new PluralContext(le.mutableCopy(), op, this.itypeAnalysis);
	}
	
	//
	// Operations on the lattice for transfer functions
	//

	/**
	 * @return
	 */
	public boolean isBottom() {
		return le == null;
	}

	public boolean isImpossible() {
		return ContextFactory.isImpossible(le);
	}
	
	/**
	 * @param node
	 * @return
	 */
	public PluralContext storeCurrentAliasingInfo(final ASTNode node) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.storeCurrentAliasingInfo(node);
				return true;
			}
		});
		return this;
	}

	/**
	 * @param x
	 * @see edu.cmu.cs.plural.concrete.DynamicStateLogic#addTrueVarPredicate(Aliasing)
	 */
	public void addTrueVarPredicate(final Variable x) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.addTrueVarPredicate(tuple.getLocations(x));
				return true;
			}
		});
	}

	/**
	 * @param x
	 * @see edu.cmu.cs.plural.concrete.DynamicStateLogic#addFalseVarPredicate(Aliasing)
	 */
	public void addFalseVarPredicate(final Variable x) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.addFalseVarPredicate(tuple.getLocations(x));
				return true;
			}
		});
	}

	/**
	 * @param x
	 * @param y
	 * @see edu.cmu.cs.plural.concrete.DynamicStateLogic#addEquality(Aliasing, Aliasing)
	 */
	public void addEquality(final Variable x, final Variable y) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.addEquality(tuple.getLocations(x), tuple.getLocations(y));
				return true;
			}
		});
	}

	/**
	 * @param x
	 * @param y
	 * @see edu.cmu.cs.plural.concrete.DynamicStateLogic#addInequality(Aliasing, Aliasing)
	 */
	public void addInequality(final Variable x, final Variable y) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.addInequality(tuple.getLocations(x), tuple.getLocations(y));
				return true;
			}
		});
	}

	/**
	 * @param x
	 * @see edu.cmu.cs.plural.concrete.DynamicStateLogic#addNullVariable(Aliasing)
	 */
	public void addNullVariable(final Variable x) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.addNullVariable(tuple.getLocations(x));
				return true;
			}
		});
	}
	
	/**
	 * @param loc
	 */
	public void addNonNullVariable(final Variable x) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.addNonNullVariable(tuple.getLocations(x));
				return true;
			}
		});
	}

	/**
	 * @param ant
	 * @param indicatedVar
	 * @param indicatedState
	 */
	public void addTrueImplication(final Variable ant, final Variable indicatedVar,
			final String indicatedState) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				// slam the new permissions into every tuple
				tuple.addTrueImplication(tuple.getLocations(ant), 
						tuple.getLocations(indicatedVar), indicatedState);
				return true;
			}
		});
	}

	/**
	 * @param ant
	 * @param indicatedVar
	 * @param indicatedState
	 */
	public void addFalseImplication(final Variable ant, final Variable indicatedVar,
			final String indicatedState) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				// slam the new permissions into every tuple
				tuple.addFalseImplication(tuple.getLocations(ant), 
						tuple.getLocations(indicatedVar), indicatedState);
				return true;
			}
		});
	}

	/**
	 * This convenience method calls solve, learnTempStateInfo, and modifies
	 * the lattice in place to include those new facts.
	 * 
	 * @param vs Variables for which we should try to solve.
	 */
	public void addNewlyDeducedFacts(final Variable... vs) {
		// TODO can we keep dynamic state logic in PluralDisjunctiveLE
		// and just call f.putResultIntoLattice on every tuple?
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				Aliasing[] as = tuple.getLocationsAfter(vs);
				List<ImplicationResult> new_facts = tuple.solveWithHints(as);
				for( ImplicationResult f : new_facts ) {
					f.putResultIntoLattice(tuple); 					
				}
				return true;
			}

		});
	}
	
	/**
	 * Learn the given state info for the given variable.
	 * @param instr
	 * @param x
	 * @param info
	 * @param inFrame
	 */
	public void learnTemporaryStateInfo(final TACInstruction instr, final Variable x,
			final String info, final boolean inFrame) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				// learn info in every tuple
				FractionalPermissions perms = tuple.get(instr, x);
				perms = perms.learnTemporaryStateInfo(info, inFrame);
				tuple.put(instr, x, perms);
				return true;
			}
		});
	}

	/**
	 * Returns {@code true} if, in any branch of this lattice, the
	 * receiver is unpacked.
	 */
	public boolean isRcvrUnpackedInAnyDisjunct() {
		final Box<Boolean> result = Box.box(false);
		le.dispatch(new DescendingVisitor(){
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				if( !tuple.isRcvrPacked() ) {
					result.setValue(true);
					return false;	
				}
				else {
					return true;
				}
			}
		});
		
		return result.getValue().booleanValue();
	}
	
	/**
	 * Returns a list of ASTNodes where the receiver was unpacked,
	 * one for every element in the disjunction.
	 */
	public List<ASTNode> whereWasRcvrUnpacked() {
		final List<ASTNode> result = new LinkedList<ASTNode>();
		le.dispatch(new DescendingVisitor(){
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				if( !tuple.isRcvrPacked() ) {
					result.add(tuple.getNodeWhereUnpacked());
				}
				return true;
			}
		});
		return result;
	}
	
	/**
	 * Returns {@code true} if, in any branch of this lattice the
	 * receiver has Full, Share, or Pure permission. (Expects the
	 * receiver to be unpacked, but works either way.)
	 */
	public boolean isRcvrFullSharePureInAnyDisjunct() {
		final Box<Boolean> result = Box.box(false);
		le.dispatch(new DescendingVisitor(){
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				if( tuple.isRcvrPacked() ) return true;
				
				// Here we are getting the unpacked permission for the receiver.
				Variable this_var = op.getThisVar();
				Aliasing this_loc = tuple.getLocations(this_var);
				FractionalPermissions frac_perms = tuple.get(this_loc);
				FractionalPermission unpacked_perm = frac_perms.getUnpackedPermission();
				
				Fraction root_fraction = 
					unpacked_perm.getFractions().get(unpacked_perm.getRootNode());
				
				if( !unpacked_perm.isReadOnly() ) {
					// Test for unique-ity
					// NEB: Actually, this doesn't test for @Unique, it
					// tests for Unique in the sense of the OOPSLA07 paper.
					FractionConstraint test = FractionConstraint.createEquality(root_fraction, Fraction.one()); 
					if( frac_perms.getConstraints().testConstraint(test) ) {
						// definitely unique! So we're done.
						return true;
					}
				
					// Must be Full or Share
					result.setValue(true);
					return false;
				}
				else {
					// Pure or Imm?
					Fraction below_frac = unpacked_perm.getFractions().getBelowFraction();
					FractionConstraint test = FractionConstraint.createLessThan(Fraction.zero(), below_frac);
					if( frac_perms.getConstraints().testConstraint(test) ) {
						// We've got an immutable!
						return true;
					}
					
					// Must be Pure
					result.setValue(true);
					return false;
				}
			}});
		
		return result.getValue().booleanValue();
	}

	/**
	 * Try to pack the receiver in all contexts to the given states. If
	 * they are packed to a different state, unpack them and re-pack them.
	 */
	final public void wrangleReceiverIntoStates(
			final ASTNode node, 
			final Set<String> neededStates) {
		final LinearOperations lo = this.op;
		
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public LinearContext context(TensorContext le) {
				return lo.prepareAnalyzedMethodReceiverForReturn(node, 
						le, neededStates);
			}
		});
	}
	
	public void handleMethodCall(final MethodCallInstruction instr,
			final IMethodSignature sig) {
		boolean isPrivate = Modifier.isPrivate(	sig.getSpecifiedMethodBinding().getModifiers());
		boolean isStatic  = Modifier.isStatic(sig.getSpecifiedMethodBinding().getModifiers());
		final Option<RcvrInstantiationPackage> ip = isStatic ? 
		    Option.<RcvrInstantiationPackage>none() :
			Option.some(new RcvrInstantiationPackage(this.itypeAnalysis, instr.getReceiverOperand()));
		final List<IMethodCaseInstance> cases = 
			sig.createPermissionsForCases(instr.isSuperCall() || isPrivate ? 
					MethodCheckingKind.METHOD_CALL_STATIC_DISPATCH : 
					MethodCheckingKind.METHOD_CALL_DYNAMIC_DISPATCH,
					false, instr.isSuperCall(), ip);
		
		assert ! cases.isEmpty();
		final int caseCount = cases.size();
		final IMethodCaseInstance singleCase;
		if(caseCount == 1)
			singleCase = cases.iterator().next();
		else
			singleCase = null;
		final boolean failFast = caseCount > 1 || ! ContextFactory.isSingleContext(le);
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public LinearContext context(TensorContext le) {
				if(singleCase != null) {
					return op.handleMethodCall(instr, 
							le, // need not copy the tuple
							singleCase, 
							failFast, 
							false);
				}
				else {
					Set<LinearContext> choices = new LinkedHashSet<LinearContext>(caseCount); 
					for(IMethodCaseInstance prePost : cases) {
						TensorContext context_copy = le.mutableCopy();
						TensorPluralTupleLE tuple = context_copy.getTuple();
						tuple.storeCurrentAliasingInfo(instr.getNode());
						choices.add(op.handleMethodCall(instr, 
								context_copy, 
								prePost,
								failFast, true));
					}
					return ContextFactory.choice(choices);
				}
			}
		});
		
		// stop True from poisoning the remainder of the method or the surrounding loop
		if(ContextFactory.isTrueContext(le))
			le = ContextFactory.falseContext(le.getParentChoiceID(), le.getChoiceID());
	}

	public void handleNewObject(			
			final NewObjectInstruction instr,
			final IConstructorSignature sig) {
		final RcvrInstantiationPackage ip = new RcvrInstantiationPackage(this.itypeAnalysis, instr.getTarget());
		final List<IConstructorCaseInstance> cases = 
			sig.createPermissionsForCases(MethodCheckingKind.CONSTRUCTOR_NEW, false, false, Option.some(ip));
		assert ! cases.isEmpty();
		final IConstructorCaseInstance singleCase;
		final int caseCount = cases.size();
		if(caseCount == 1)
			singleCase = cases.iterator().next();
		else
			singleCase = null;
		final boolean failFast = caseCount > 1 || ! ContextFactory.isSingleContext(le);
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public LinearContext context(TensorContext le) {
				if(singleCase != null) {
					return op.handleNewObject(instr,
							le, // need not copy the tuple
							singleCase, 
							failFast, false);
				}
				else {
					Set<LinearContext> choices = new LinkedHashSet<LinearContext>(caseCount); 
					for(IConstructorCaseInstance prePost : cases) {
						TensorContext context_copy = le.mutableCopy();
						TensorPluralTupleLE tuple = context_copy.getTuple();
						tuple.storeCurrentAliasingInfo(instr.getNode());
						choices.add(op.handleNewObject(instr,
								context_copy, 
								prePost, 
								failFast, true));
					}
					return ContextFactory.choice(choices);
				}
			}
		});

		// stop True from poisoning the remainder of the method or the surrounding loop
		if(ContextFactory.isTrueContext(le))
			le = ContextFactory.falseContext(le.getParentChoiceID(), le.getChoiceID());
	}

	public void handleConstructorCall(
			final ConstructorCallInstruction instr,
			final IConstructorSignature sig) {
		final List<IConstructorCaseInstance> cases = 
			sig.createPermissionsForCases(MethodCheckingKind.CONSTRUCTOR_SUPER_CALL, false, true, Option.<RcvrInstantiationPackage>none());
		assert ! cases.isEmpty();
		final IConstructorCaseInstance singleCase;
		final int caseCount = cases.size();
		if(caseCount == 1)
			singleCase = cases.iterator().next();
		else
			singleCase = null;
		final boolean failFast = caseCount > 1 || ! ContextFactory.isSingleContext(le);
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public LinearContext context(TensorContext le) {
				if(singleCase != null) {
					return op.handleConstructorCall(instr,
							le, // need not copy the tuple
							singleCase,
							failFast, false);
				}
				else {
					Set<LinearContext> choices = new LinkedHashSet<LinearContext>(cases.size()); 
					for(IConstructorCaseInstance prePost : cases) {
						TensorContext context_copy = le.mutableCopy();
						TensorPluralTupleLE tuple = context_copy.getTuple();
						tuple.storeCurrentAliasingInfo(instr.getNode());
						choices.add(op.handleConstructorCall(instr,
								context_copy, 
								prePost,
								failFast, true));
					}
					return ContextFactory.choice(choices);
				}
			}
		});

		// stop True from poisoning the remainder of the method or the surrounding loop
		if(ContextFactory.isTrueContext(le))
			le = ContextFactory.falseContext(le.getParentChoiceID(), le.getChoiceID());
	}

	public void prepareForFieldRead(final LoadFieldInstruction instr) {
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public LinearContext context(TensorContext le) {
				return op.handleFieldAccess(le, 
						null /* not an assignment */, instr);
			}
		});
	}

	public void prepareForFieldWrite(final StoreFieldInstruction instr) {
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public LinearContext context(TensorContext le) {
				return op.handleFieldAccess(le,
						instr.getSourceOperand(),
						instr);
			}
		});
	}

	/**
	 * @param instr
	 * @param x
	 * @param permissions
	 */
	public void put(final TACInstruction instr, final Variable x,
			final FractionalPermissions permissions) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				// slam the new permissions into every tuple
				tuple.put(instr, x, permissions);
				return true;
			}
		});
	}

	public void prepareForArrayWrite(final StoreArrayInstruction instr) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				// make the array's permission modifiable in every tuple
				FractionalPermissions permissions = tuple.get(instr, instr.getDestinationArray());
				// make sure that we can write to the array
				permissions = permissions.makeModifiable(permissions.getStateSpace().getRootState(),
						false /* virtual permission for array */); // TODO arrays as frames?
				tuple.put(instr, instr.getDestinationArray(), permissions);
				return true;
			}
		});
		
		
	}
	
	//
	// Error checking methods
	//

	/**
	 * Checks that the given variable is, in fact, in the states that are required
	 * of it, also given.
	 * @deprecated Use {@link #checkRegularCallPrecondition(ASTNode, Variable, List, PredicateChecker)}
	 * or {@link #checkSuperCallPrecondition(ASTNode, List, PredicateChecker)}.
	 */
	public String checkStates(final Variable x, final Set<Set<String>> requiredOptions) {
		if(requiredOptions == null || requiredOptions.isEmpty()) 
			return null;
		
		String error = le.dispatch(new ErrorReportingVisitor() {
			@Override
			public String checkTuple(TensorPluralTupleLE tuple) {
				FractionalPermissions perms = tuple.get(tuple.getLocations(x));
				Set<String> problems = new LinkedHashSet<String>();
				next_set:
				for(Set<String> required : requiredOptions) {
					if(required == null || required.isEmpty())
						// no requirement --> succeed
						return null;
					for(String needed : required) {
						if(perms.getUnpackedPermission() != null && 
								perms.getStateSpace().firstBiggerThanSecond(perms.getUnpackedPermission().getRootNode(), needed)) {
							if(log.isLoggable(Level.FINE))
								log.fine("Skipping state test on " + needed + " for unpacked permission: " + perms);
						}
						else if(perms.isInState(needed) == false) {
							if(x.isUnqualifiedThis()) {
								if(log.isLoggable(Level.FINE))
									log.fine("Ignoring unsatisfied receiver state " + needed);
							}
							else {	
								// only flag first unsatisfied state requirement
								problems.add("" + x.getSourceString() + " must be in state " + needed + " but is in " + perms.getStateInfo());
								continue next_set;
							}
						}
					}
					// required set fully satisfied --> succeed
					return null;
				}
				// no set fully satisfied --> fail
				assert ! problems.isEmpty();
				return ErrorReportingVisitor.errorString(problems, " OR ");
			}
		});
		
		return error;
	}

	/**
	 * Tests whether the constraints for the given variable are satisfiable.
	 * This method is no longer guaranteed to work properly because
	 * the garbage-collection logic may get rid of lattice information before
	 * you can test it.
	 * @param x
	 * @return <code>true</code> if the constraints are satisfiable, 
	 * <code>false</code> otherwise.
	 * @deprecated Use {@link #checkRegularCallPrecondition(ASTNode, Variable, List, PredicateChecker)}
	 * or {@link #checkSuperCallPrecondition(ASTNode, List, PredicateChecker)}.
	 */
	public boolean checkConstraintsSatisfiable(final Variable x) {
		return le.dispatch(new TestVisitor() {
			@Override
			public boolean testTuple(TensorPluralTupleLE tuple) {
				// TODO we may have to compact contexts to only
				// include fully satisfiable tuples.
				return ! tuple.get(tuple.getLocations(x)).isUnsatisfiable();
			}
		});
	}
	
	/**
	 * Checks the given pre-condition of a call with the given receiver and
	 * arguments.  Not to be used for calls on <code>super</code>.
	 * @param node
	 * @param receiver Receiver, if a instance method or constructor call.
	 * Must be <code>null</code> for static calls and <code>new</code>.
	 * @param arguments Call arguments.
	 * @param pre Pre-condition to check.
	 * @return String describing errors found or <code>null</code> if there
	 * were no errors.
	 * @see #checkSuperCallPrecondition(ASTNode, List, PredicateChecker) Checking calls on <code>super</code>.
	 */
	public String checkRegularCallPrecondition(final ASTNode node,
			final Variable receiver,
			final List<Variable> arguments, final PredicateChecker pre) {
		if(isBottom())
			return null;
		freeze(); // force-freeze before any checks
		return le.dispatch(new ErrorReportingVisitor() {
			@Override
			public String context(TensorContext le) {
				return op.checkCallPrecondition(
						node, le, receiver, arguments, pre);
			}

			@Override
			public String checkTuple(TensorPluralTupleLE tuple) {
				return Utilities.nyi("Shouldn't be called b/c we are overriding the TensorContext version.");
			}
		});
	}
	
	/**
	 * Like {@link #checkRegularCallPrecondition(ASTNode, Variable, List, PredicateChecker)}
	 * but for calls on <code>super</code>.  Need to treat this special because
	 * {@link ITACAnalysisContext#getSuperVariable()} is not available to the checker.
	 * @param node
	 * @param arguments Call arguments.
	 * @param pre Pre-condition to check.
	 * @return String describing errors found or <code>null</code> if there
	 * were no errors.
	 */
	public String checkSuperCallPrecondition(final ASTNode node,
			final List<Variable> arguments, final PredicateChecker pre) {
		if(isBottom())
			return null;
		freeze(); // force-freeze before any checks
		return le.dispatch(new ErrorReportingVisitor() {
			@Override
			public String context(TensorContext le) {
				return op.checkCallPrecondition(
						node, le, 
						op.getAnalysisContext().getSuperVariable(), // just stick it in 
						arguments, pre);
			}

			@Override
			public String checkTuple(TensorPluralTupleLE tuple) {
				return Utilities.nyi("I don't expect this to ever be called b/c I am overriding context.");
			}
		});
	}
	
	/**
	 * Checks the given post-condition with the given result variable
	 * and aliasing mapping for formal method parameters.
	 * @param node
	 * @param resultVar
	 * @param post
	 * @param parameterVars
	 * @param stateTests (possibly empty) map from return values to the 
	 * receiver state being tested.
	 * @return String describing errors found or <code>null</code> if there
	 * were no errors.
	 */
	public String checkPostCondition(
			final ASTNode node,
			final Variable resultVar,
			final PredicateChecker post,
			final SimpleMap<String, Aliasing> parameterVars,
			final Map<Boolean, String> stateTests) {
		if(isBottom())
			return null;
		freeze(); // force-freeze before any checks
		return le.dispatch(new ErrorReportingVisitor() {
			
			@Override
			public String context(TensorContext le) {
				Pair<String, String> trueTest = null;
				Pair<String, String> falseTest = null;
				if(stateTests.containsKey(true))
					trueTest = Pair.create("this!fr", stateTests.get(true));
				if(stateTests.containsKey(false))
					falseTest = Pair.create("this!fr", stateTests.get(false));
				return op.checkPostCondition(
						node, le, resultVar, post, parameterVars, trueTest, falseTest);
			}

			@Override
			public String checkTuple(TensorPluralTupleLE tuple) {
				return Utilities.nyi("I don't expect this to ever be called b/c I am overriding context");
			}
		});
	}

	//
	// Object methods
	//

	@Override
	public String toString() {
		if(isBottom())
			return "Bottom";
		return le.toString();
	}

	/**
	 * @param liveness
	 */
	public void killDeadVariables(final TACInstruction instr, final VariableLiveness liveness) {
		le.dispatch(new DescendingVisitor() {

			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.killDeadVariablesAfter(instr, liveness);
				return true;
			}
			
		});
	}

	/**
	 * Discards temporary state information for all permissions in every
	 * disjunct of this lattice if that permission <i>must</i> be
	 * {@code @Pure} or {@code @Share}.
	 */
	public PluralContext forgetShareAndPureStates() {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.forgetShareAndPureStateInformation();
				return true;
			}
		});
		return this;
	}
	
	/**
	 * Discards temporary state information for all permissions in every
	 * disjunct of this lattice if that permission must be pure or share,
	 * and that permission is not associated with one of the given variables.
	 * The current aliasing for each variable will be looked up first.
	 */
	public PluralContext forgetShareAndPureStatesNotInSet(
			final Set<Variable> vars_to_not_forget) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				tuple.forgetShareAndPureStateInformationNotInSet(vars_to_not_forget);
				return true;
			}
		});
		return this;
	}
	
	/**
	 * @param x
	 * @param perms
	 * @return
	 */
	public void splitOff(final Variable x,
			final PermissionSetFromAnnotations perms) {
		le.dispatch(new DescendingVisitor() {

			@Override
			public Boolean tuple(TensorPluralTupleLE tuple) {
				Aliasing a = tuple.getLocations(x);
				FractionalPermissions p = tuple.get(a);
				p.splitOff(perms);
				tuple.put(a, p);
				return true;
			}
			
		});
	}
}
