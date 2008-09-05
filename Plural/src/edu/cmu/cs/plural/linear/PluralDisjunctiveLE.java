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
package edu.cmu.cs.plural.linear;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.flow.LatticeElement;
import edu.cmu.cs.crystal.internal.Freezable;
import edu.cmu.cs.crystal.tac.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.SuperVariable;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.ThisVariable;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.concrete.ImplicationResult;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.states.IConstructorCaseInstance;
import edu.cmu.cs.plural.states.IConstructorSignature;
import edu.cmu.cs.plural.states.IMethodCaseInstance;
import edu.cmu.cs.plural.states.IMethodSignature;
import edu.cmu.cs.plural.states.StateSpaceRepository;
import edu.cmu.cs.plural.track.FractionAnalysisContext;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement.VariableLiveness;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * @author Kevin Bierhoff
 *
 */
public class PluralDisjunctiveLE implements LatticeElement<PluralDisjunctiveLE>, Freezable<PluralDisjunctiveLE> {
	
	private static final Logger log = Logger.getLogger(PluralDisjunctiveLE.class.getName());
	
	private static final PluralDisjunctiveLE BOTTOM = new PluralDisjunctiveLE();
	
	/**
	 * @param start
	 * @return
	 */
	public static PluralDisjunctiveLE tuple(TensorPluralTupleLE start,
			AnnotationDatabase annoDB, ITACAnalysisContext tacContext, ThisVariable thisVar,
			FractionAnalysisContext fractContext) {
		return new PluralDisjunctiveLE(ContextFactory.tensor(start),
				new LinearOperations(annoDB, tacContext, thisVar, fractContext));
	}
	
	/**
	 * Returns a special "bottom" lattice element.
	 * @return Bottom
	 */
	public static PluralDisjunctiveLE bottom() {
		return BOTTOM;
	}

	/**
	 * Linear proof contexts or <code>null</code> if this is bottom.
	 */
	private DisjunctiveLE le;
	private final LinearOperations op;
	
	private PluralDisjunctiveLE() {
		this.le = null;
		this.op = null;
	}
	
	/**
	 * @param le
	 * @param crystal 
	 * @param tacContext
	 * @param thisVar 
	 * @param fractContext
	 * @param reEntrant 
	 */
	private PluralDisjunctiveLE(DisjunctiveLE le, AnnotationDatabase annoDB, 
			ITACAnalysisContext tacContext, ThisVariable thisVar, 
			FractionAnalysisContext fractContext) {
		assert le != null;
		this.le = le;
		this.op = new LinearOperations(annoDB, tacContext, thisVar, fractContext);
	}

	/**
	 * @param le
	 */
	private PluralDisjunctiveLE(DisjunctiveLE le, LinearOperations op) {
		assert le != null;
		this.le = le;
		this.op = op;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.flow.LatticeElement, org.eclipse.jdt.core.dom.ASTNode)
	 */
	@Override
	public boolean atLeastAsPrecise(PluralDisjunctiveLE other, ASTNode node) {
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
	public PluralDisjunctiveLE copy() {
		freeze();
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#join(edu.cmu.cs.crystal.flow.LatticeElement, org.eclipse.jdt.core.dom.ASTNode)
	 */
	@Override
	public PluralDisjunctiveLE join(PluralDisjunctiveLE other, ASTNode node) {
		freeze();
		if(this == other)
			return this;
		other.freeze();
		if(other.isBottom())
			return this;
		if(this.isBottom())
			return other;
		assert this.op == other.op;
		return new PluralDisjunctiveLE(this.le.join(other.le, node).compact(node, true), op);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.internal.Freezable#freeze()
	 */
	@Override
	public PluralDisjunctiveLE freeze() {
		if(!isBottom())
			le = le.compact(null, true);
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.internal.Freezable#mutableCopy()
	 */
	@Override
	public PluralDisjunctiveLE mutableCopy() {
		return new PluralDisjunctiveLE(le.mutableCopy(), op);
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
	public PluralDisjunctiveLE storeCurrentAliasingInfo(final ASTNode node) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
				// slam the new permissions into every tuple
				tuple.addFalseImplication(tuple.getLocations(ant), 
						tuple.getLocations(indicatedVar), indicatedState);
				return true;
			}
		});
	}

	/**
	 * This convenience method calls solve, learnTempStateInfo, and then returns
	 * the resulting lattice.
	 * 
	 * @param vs
	 */
	public void addNewlyDeducedFacts(
			final Variable... vs) {
		// TODO can we keep dynamic state logic in PluralDisjunctiveLE
		// and just call f.putResultIntoLattice on every tuple?
		le.dispatch(new DescendingVisitor() {
			@Override
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
	 */
	public void learnTemporaryStateInfo(final TACInstruction instr, final Variable x,
			final String info) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public boolean tupleModification(TensorPluralTupleLE tuple) {
				// learn info in every tuple
				FractionalPermissions perms = tuple.get(instr, x);
				perms = perms.learnTemporaryStateInfo(info);
				tuple.put(instr, x, perms);
				return true;
			}
		});
	}

	/**
	 * TODO figure out if contexts can differ in receiver packed / unpacked
	 * @param rcvrVar
	 * @param stateRepo
	 * @param locs
	 * @param desiredRoot
	 * @param assignedField
	 */
	public void unpackReceiver(final ThisVariable rcvrVar,
			final StateSpaceRepository stateRepo,
			final SimpleMap<Variable, Aliasing> locs, final String desiredRoot,
			final String assignedField) {
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public DisjunctiveLE context(LinearContextLE le) {
				if(le.getTuple().isRcvrPacked()) {
					return le.getTuple().fancyUnpackReceiver(rcvrVar, stateRepo, locs, desiredRoot, assignedField);
				}
				// else ?
				return le;
			}
		});
	}

	public boolean packReceiver(final ThisVariable rcvrVar,
			final StateSpaceRepository stateRepo,
			final SimpleMap<Variable, Aliasing> locs,
			final Set<String> neededStates) {
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public DisjunctiveLE context(LinearContextLE le) {
				if(! le.getTuple().isRcvrPacked()) {
					if(le.getTuple().packReceiver(rcvrVar, stateRepo, locs, neededStates))
						return le;
					else
						return ContextFactory.falseContext();
				}
				else {
					if(le.getTuple().get(locs.get(rcvrVar)).isInStates(neededStates))
						return le;
					else
						return ContextFactory.falseContext();
				}
			}
		});
		return ! isImpossible();
	}

	public boolean packReceiverToBestGuess(final ThisVariable rcvrVar,
			final StateSpaceRepository stateRepo,
			final SimpleMap<Variable, Aliasing> locs, final String[] statesToTry) {
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public DisjunctiveLE context(LinearContextLE le) {
				if(! le.getTuple().isRcvrPacked()) {
					return le.getTuple().fancyPackReceiverToBestGuess(rcvrVar, stateRepo, locs, statesToTry);
				}
				// else ?
				return le;
			}
		});
		return ! isImpossible();
	}
	
	public void handleMethodCall(			
			final MethodCallInstruction instr,
			final IMethodSignature sig
//			final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> rcvrPrePost,
//			final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost,
//			final PermissionSetFromAnnotations resultPost
			) {
		final Variable rcvrInstance;
		if(instr.isStaticMethodCall())
			rcvrInstance = null;
		else
			rcvrInstance = instr.getReceiverOperand();
		final List<IMethodCaseInstance> cases = sig.createPermissionsForCases(false, instr.isSuperCall());
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
			public DisjunctiveLE context(LinearContextLE le) {
				if(singleCase != null) {
					return op.fancyHandleInvocation(instr, 
							sig.getSpecifiedMethodBinding(),
							le.getTuple(), // need not copy the tuple
							rcvrInstance, 
							rcvrInstance == null ? null : singleCase.getReceiverPermissions(), 
							singleCase.getParameterPermissions(), 
							instr.getTarget(),
							singleCase.getEnsuredResultPermissions(), failFast,
							singleCase.isReceiverBorrowed(), 
							singleCase.areArgumentsBorrowed());
				}
				else {
					Set<DisjunctiveLE> choices = new LinkedHashSet<DisjunctiveLE>(caseCount); 
					for(IMethodCaseInstance prePost : cases) {
						TensorPluralTupleLE context = le.getTuple().mutableCopy();
						context.storeCurrentAliasingInfo(instr.getNode());
						choices.add(op.fancyHandleInvocation(instr, 
								sig.getSpecifiedMethodBinding(),
								context, 
								rcvrInstance, 
								rcvrInstance == null ? null : prePost.getReceiverPermissions(), 
								prePost.getParameterPermissions(), 
								instr.getTarget(),
								prePost.getEnsuredResultPermissions(), 
								true /* fail fast */,
								prePost.isReceiverBorrowed(),
								prePost.areArgumentsBorrowed()));
					}
					return ContextFactory.choice(choices);
				}
			}
		});
	}

	public void handleNewObject(			
			final NewObjectInstruction instr,
			final IConstructorSignature sig
//			final PermissionSetFromAnnotations rcvrPost,
//			final Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost
			) {
		final List<IConstructorCaseInstance> cases = sig.createPermissionsForCases(false, false);
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
			public DisjunctiveLE context(LinearContextLE le) {
				if(singleCase != null) {
					return op.fancyHandleInvocation(instr,
							sig.getSpecifiedMethodBinding(),
							le.getTuple(), // need not copy the tuple
							null, null, // no receiver
							singleCase.getParameterPermissions(), 
							instr.getTarget(),
							singleCase.getEnsuredReceiverPermissions(), // treat receiver as target 
							failFast,
							false, // no receiver
							singleCase.areArgumentsBorrowed());
				}
				else {
					Set<DisjunctiveLE> choices = new LinkedHashSet<DisjunctiveLE>(caseCount); 
					for(IConstructorCaseInstance prePost : cases) {
						TensorPluralTupleLE context = le.getTuple().mutableCopy();
						context.storeCurrentAliasingInfo(instr.getNode());
						choices.add(op.fancyHandleInvocation(instr,
								sig.getSpecifiedMethodBinding(),
								context, 
								null, null, 
								prePost.getParameterPermissions(), 
								instr.getTarget(),
								prePost.getEnsuredReceiverPermissions(), // treat receiver as target 
								true /* fail fast */,
								false, // no receiver
								prePost.areArgumentsBorrowed()));
					}
					return ContextFactory.choice(choices);
				}
			}
		});
	}

	public void handleConstructorCall(
			final ConstructorCallInstruction instr,
			final IConstructorSignature sig) {
		final List<IConstructorCaseInstance> cases = sig.createPermissionsForCases(false, instr.isSuperCall());
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
			public DisjunctiveLE context(LinearContextLE le) {
				if(singleCase != null) {
					return op.fancyHandleInvocation(instr,
							sig.getSpecifiedMethodBinding(),
							le.getTuple(), // need not copy the tuple
							instr.getConstructionObject(), // this or super
							singleCase.getReceiverPermissions(),
							singleCase.getParameterPermissions(),
							null, null, // no target
							failFast,
							singleCase.isReceiverBorrowed(),
							singleCase.areArgumentsBorrowed());
				}
				else {
					Set<DisjunctiveLE> choices = new LinkedHashSet<DisjunctiveLE>(cases.size()); 
					for(IConstructorCaseInstance prePost : cases) {
						TensorPluralTupleLE context = le.getTuple().mutableCopy();
						context.storeCurrentAliasingInfo(instr.getNode());
						choices.add(op.fancyHandleInvocation(instr,
								sig.getSpecifiedMethodBinding(),
								context, 
								instr.getConstructionObject(), // this or super
								prePost.getReceiverPermissions(),
								prePost.getParameterPermissions(),
								null, null, // no target
								failFast,
								prePost.isReceiverBorrowed(),
								prePost.areArgumentsBorrowed()));
					}
					return ContextFactory.choice(choices);
				}
			}
		});
	}

	public void prepareForFieldRead(final LoadFieldInstruction instr) {
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public DisjunctiveLE context(LinearContextLE le) {
				if(le.getTuple().isRcvrPacked()) {
					return op.fancyUnpackForFieldAccess(le.getTuple(), 
							null /* not an assignment */, instr);
				}
				// else ?
				return le;
			}
		});
	}

	public void prepareForFieldWrite(final StoreFieldInstruction instr) {
		le = le.dispatch(new RewritingVisitor() {
			@Override
			public DisjunctiveLE context(LinearContextLE le) {
				if(le.getTuple().isRcvrPacked()) {
					return op.fancyUnpackForFieldAccess(le.getTuple(),
							instr.getSourceOperand(),
							instr);
				}
				// else ?
				return le;
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
				// slam the new permissions into every tuple
				tuple.put(instr, x, permissions);
				return true;
			}
		});
	}
	/*
	public void forgetTemporaryStateInfo() {
		le.dispatch(new DescendingVisitor() {
			@Override
			public boolean tupleModification(TensorPluralTupleLE tuple) {
				for(ExtendedIterator<FractionalPermissions> it = tuple.tupleInfoIterator(); it.hasNext(); ) {
					FractionalPermissions permissions = it.next();
					permissions = permissions.forgetTemporaryStateInfo();
					it.replace(permissions);
				}
				return true;
			}
		});
	}

	public void releaseParameter(final MethodCallInstruction instr,
			final Variable x, final String paramName) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public boolean tupleModification(TensorPluralTupleLE tuple) {
				tuple.releaseParameter(instr, x, paramName);
				return true;
			}
		});
	}
	*/

	public void prepareForArrayWrite(final StoreArrayInstruction instr) {
		le.dispatch(new DescendingVisitor() {
			@Override
			public boolean tupleModification(TensorPluralTupleLE tuple) {
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
							// only flag first unsatisfied state requirement
							problems.add("" + x.getSourceString() + " must be in state " + needed + " but is in " + perms.getStateInfo());
							continue next_set;
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
	 * @param x
	 * @return <code>true</code> if the constraints are satisfiable, 
	 * <code>false</code> otherwise.
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
	 * 
	 * @param node
	 * @param paramPost
	 * @param resultVar
	 * @param resultPost
	 * @param stateTests (possibly empty) map from return values to the 
	 * receiver state being tested.
	 * @return
	 */
	public String checkPostConditions(
			final ASTNode node,
			final Map<Aliasing, PermissionSetFromAnnotations> paramPost,
			final Variable resultVar,
			final PermissionSetFromAnnotations resultPost, 
			final Map<Boolean, String> stateTests) {
		if(isBottom())
			return null;
		return le.dispatch(new ErrorReportingVisitor() {
			@Override
			public String checkTuple(TensorPluralTupleLE tuple) {
				return op.fancyCheckPostConditions(
						node, tuple, paramPost, resultVar, resultPost, stateTests);
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
				tuple.killDeadVariables(instr, liveness);
				return true;
			}
			
		});
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
			public boolean tupleModification(TensorPluralTupleLE tuple) {
				Aliasing a = tuple.getLocations(x);
				FractionalPermissions p = tuple.get(a);
				p.splitOff(perms);
				tuple.put(a, p);
				return true;
			}
			
		});
	}

}
