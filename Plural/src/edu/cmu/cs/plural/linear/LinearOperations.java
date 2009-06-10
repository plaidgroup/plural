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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import edu.cmu.cs.crystal.analysis.alias.AliasLE;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.SuperVariable;
import edu.cmu.cs.crystal.tac.model.TACFieldAccess;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.TACInvocation;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.alias.FrameLabel;
import edu.cmu.cs.plural.concrete.StateImplication;
import edu.cmu.cs.plural.contexts.ContextFactory;
import edu.cmu.cs.plural.contexts.LinearContext;
import edu.cmu.cs.plural.contexts.TensorContext;
import edu.cmu.cs.plural.contexts.TensorPluralTupleLE;
import edu.cmu.cs.plural.errors.PackingResult;
import edu.cmu.cs.plural.fractions.AbstractFractionalPermission;
import edu.cmu.cs.plural.fractions.Fraction;
import edu.cmu.cs.plural.fractions.FractionAssignment;
import edu.cmu.cs.plural.fractions.FractionalPermission;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.ReleaseHolder;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.pred.PredicateChecker.SplitOffTuple;
import edu.cmu.cs.plural.pred.PredicateMerger.MergeIntoTuple;
import edu.cmu.cs.plural.states.IConstructorCaseInstance;
import edu.cmu.cs.plural.states.IInvocationCaseInstance;
import edu.cmu.cs.plural.states.IMethodCaseInstance;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.states.StateSpaceRepository;
import edu.cmu.cs.plural.track.FieldVariable;
import edu.cmu.cs.plural.track.FractionAnalysisContext;
import edu.cmu.cs.plural.track.Permission.PermissionKind;
import edu.cmu.cs.plural.util.TACAnalysisHelper;

/**
 * This class encapsulates major operations on individual tuple lattices.
 * @author Kevin Bierhoff
 * @since 4/15/2008
 */
public class LinearOperations extends TACAnalysisHelper {
	
	private static final Logger log = Logger.getLogger(LinearOperations.class.getName());
	
	private final FractionAnalysisContext fractContext;
	
	private final boolean packBeforeCall;
	
	public LinearOperations(ITACAnalysisContext ctx, FractionAnalysisContext fractContext) {
		super(fractContext.getAnnoDB(), ctx);
		this.fractContext = fractContext;
		this.packBeforeCall = fractContext.getAnalyzedCase().getInvocationCase().isReentrant();
	}
	
	//
	// invocations (method calls, new)
	//
	
	/**
	 * Applies the given case of the given method call to the given tuple.
	 * @param instr
	 * @param value Must be mutable.
	 * @param sigCase Must be an instance for checking the call.
	 * @param failFast When <code>true</code>, unsatisfiable predicates are detected
	 * and lead to {@link ContextFactory#trueContext() false}.  Otherwise, predicates
	 * are not checked and permissions simply split.
	 * @return The resulting context, possibly containing multiple tuples or
	 * {@link ContextFactory#trueContext() false}.
	 */
	public LinearContext handleMethodCall(
			final MethodCallInstruction instr,
			final TensorContext current_context,
			final IMethodCaseInstance sigCase,
			boolean failFast) {
		// create parameter map suitable for method call
		final TensorPluralTupleLE value = current_context.getTuple();
		final SimpleMap<String, Aliasing> paramMap = createParameterMap(
				instr, value, instr.getReceiverOperand(), instr.getTarget());
		if(instr.isSuperCall() && value.isRcvrPacked()) {
			// may have to unpack to get super-frame permissions
			LinearContext unpacked = value.fancyUnpackReceiver(
					getThisVar(), instr.getNode(), getRepository(),
					new SimpleMap<Variable,Aliasing>() {
						@Override
						public Aliasing get(Variable key) {
							return value.getLocations(key);
						}},
					StateSpace.STATE_ALIVE /* super always mapped to root node */, 
					null /* not an assignment */,
					true /* also keep the packed version */, 
					current_context.getParentChoiceID(), 
					current_context.getChoiceID());
			
			final boolean failFaster = failFast || ! ContextFactory.isSingleContext(unpacked);
			return unpacked.dispatch(new RewritingVisitor() {
				@Override
				public LinearContext context(TensorContext le) {
					return handleInvocation(instr, le, 
							sigCase, failFaster, paramMap);
				}
			});
		}
		else
			// not a super call: business as usual
			// super call but already unpacked: 
			//   will just fail if super-frame permission missing
			//   super is mapped to alive, so any unpacked node will do
			//   no need to check like in handleFieldAccess
			return handleInvocation(instr, current_context, sigCase, failFast, paramMap);
	}
	
	/**
	 * Applies the given case of the given <code>new</code> instruction to the given tuple.
	 * @param instr
	 * @param value Must be mutable.
	 * @param sigCase Must be an instance for checking the call.
	 * @param failFast When <code>true</code>, unsatisfiable predicates are detected
	 * and lead to {@link ContextFactory#trueContext() false}.  Otherwise, predicates
	 * are not checked and permissions simply split.
	 * @return The resulting context, possibly containing multiple tuples or
	 * {@link ContextFactory#trueContext() false}.
	 */
	public LinearContext handleNewObject(
			NewObjectInstruction instr,
			TensorContext cur_context,
			IConstructorCaseInstance sigCase,
			boolean failFast) {
		// create parameter map suitable for "new"
		TensorPluralTupleLE value = cur_context.getTuple();
		final Aliasing target_loc = value.getLocationsAfter(instr, instr.getTarget());
		final SimpleMap<String, Aliasing> paramMap = createParameterMap(
				instr, value, 
				target_loc, // target is receiver
				target_loc, // target is receiver
				null);	// no result
		return handleInvocation(instr, cur_context, sigCase, failFast, paramMap);
	}

	/**
	 * Applies the given case of the given constructor call to the given tuple.
	 * @param instr
	 * @param value Must be mutable.
	 * @param sigCase Must be an instance for checking the call.
	 * @param failFast When <code>true</code>, unsatisfiable predicates are detected
	 * and lead to {@link ContextFactory#trueContext() false}.  Otherwise, predicates
	 * are not checked and permissions simply split.
	 * @return The resulting context, possibly containing multiple tuples or
	 * {@link ContextFactory#trueContext() false}.
	 */
	public LinearContext handleConstructorCall(
			ConstructorCallInstruction instr,
			TensorContext cur_context,
			IConstructorCaseInstance sigCase,
			boolean failFast) {
		// create parameter map suitable for constructor invocation
		final SimpleMap<String, Aliasing> paramMap = createParameterMap(
				instr, cur_context.getTuple(), 
				instr.getConstructionObject(),	 
				null); // no result
		return handleInvocation(instr, cur_context, sigCase, failFast, paramMap);
	}

	/**
	 * Applies the given case of the given invocation to the given tuple.
	 * @param instr
	 * @param value
	 * @param sigCase
	 * @param failFast
	 * @param paramMap
	 * @return The resulting context, possibly containing multiple tuples or
	 * {@link ContextFactory#trueContext() false}.
	 */
	private LinearContext handleInvocation(final TACInvocation instr,
			final TensorContext cur_context, final IInvocationCaseInstance sigCase,
			boolean failFast, final SimpleMap<String, Aliasing> paramMap) {
		
		TensorPluralTupleLE value = cur_context.getTuple();
		
		// 1. split off pre-condition
		PredicateChecker pre = sigCase.getPreconditionChecker();
		Aliasing this_loc = value.getLocationsBefore(instr, getThisVar());
		
		CallHandlerContinuation cont = new CallHandlerContinuation() {

			@Override
			public LinearContext mergePostIntoTuple(TensorContext tensor,
					Map<Aliasing, FractionalPermissions> borrowedPerms) {
				TensorPluralTupleLE tuple = tensor.getTuple();
				// 2. forget state information for remaining permissions
				if(sigCase.isEffectFree() == false) {
					tuple = forgetStateInfo(tuple);
					tuple = forgetFieldPermissionsIfNeeded(tuple, getThisVar(), instr);
				}
				
				// 3. merge in post-condition
				PredicateMerger post = sigCase.getPostconditionMerger();
				CallPostPredicateMerger merger = new CallPostPredicateMerger(
						instr.getNode(), tuple, borrowedPerms);
				post.mergeInPredicate(paramMap, merger);
				
				if(merger.isVoid())
					return ContextFactory.falseContext(tensor.getParentChoiceID(), tensor.getChoiceID());
				else
					return ContextFactory.tensor(tuple, tensor.getParentChoiceID(), tensor.getChoiceID());
			}
			
		};
		
		CallPreconditionHandler splitter = createSplitter(
				instr, cur_context, this_loc, cont, failFast);
		
		// the splitter will also merge in the post-condition
		if(! pre.splitOffPredicate(
				paramMap, 
				splitter))
			return ContextFactory.trueContext(cur_context.getParentChoiceID(),
					cur_context.getChoiceID());
		
		return splitter.getResult();
	}

	/**
	 * Creates a aliasing map for a invocation with the given receiver and
	 * result variables for use in TAC-based transfer functions.
	 * @param instr
	 * @param value
	 * @param receiver May be <code>null</code>.
	 * @param result May be <code>null</code>.
	 * @return a aliasing map for a invocation with the given receiver and
	 * result variables.
	 * @see #createNodeArgumentMap(ASTNode, TensorPluralTupleLE, Variable, List)
	 * creating AST-based maps for checkers
	 * @see #handleMethodCall(MethodCallInstruction, TensorContext, IMethodCaseInstance, boolean)
	 * @see #handleConstructorCall(ConstructorCallInstruction, TensorPluralTupleLE, IConstructorCaseInstance, boolean)
	 */
	private SimpleMap<String, Aliasing> createParameterMap(
			final TACInvocation instr,
			TensorPluralTupleLE value,
			Variable receiver, Variable result) {
		Aliasing virt_loc = null;
		Aliasing frame_loc = null;
		Aliasing result_loc = null;
		if(receiver != null) {
			if(receiver.isUnqualifiedSuper()) {
				virt_loc = value.getLocationsBefore(instr, getThisVar());
				frame_loc = getFrameAliasing(receiver);
			}
			else {
				virt_loc = value.getLocationsBefore(instr, receiver);
				frame_loc = value.getLocationsBefore(instr, receiver);
			}
		}
		if(result != null) {
			result_loc = value.getLocationsAfter(instr, result);
		}
		return createParameterMap(instr, value, virt_loc, frame_loc, result_loc);
	}

	/**
	 * Creates a aliasing map for a invocation with the given locations for
	 * the receiver and result.
	 * @param instr
	 * @param value
	 * @param rcvrVirtual May be <code>null</code>.
	 * @param rcvrFrame May be <code>null</code>.
	 * @param result May be <code>null</code>.
	 * @return a aliasing map for a invocation with the given locations for
	 * the receiver and result.
	 * @see #handleNewObject(NewObjectInstruction, TensorPluralTupleLE, IConstructorCaseInstance, boolean)
	 * @see #createParameterMap(TACInvocation, TensorPluralTupleLE, Variable, Variable)
	 */
	public static SimpleMap<String, Aliasing> createParameterMap(
			final TACInvocation instr,
			TensorPluralTupleLE value,
			Aliasing rcvrVirtual, Aliasing rcvrFrame, Aliasing result) {
		final Map<String, Aliasing> known = new HashMap<String, Aliasing>();
		if(rcvrVirtual != null) {
			known.put("this", rcvrVirtual);
		}
		if(rcvrFrame != null) {
			known.put("this!fr", rcvrFrame);
		}
		if(result != null) {
			known.put("result", result);
		}
		int arg = 0;
		for(Variable x : instr.getArgOperands()) {
			Aliasing loc = value.getLocationsBefore(instr, x);
			known.put("#" + arg++, loc);
		}
		
		return new SimpleMap<String, Aliasing>() {

			@Override
			public Aliasing get(String key) {
				Aliasing result = known.get(key);
				if(result != null)
					return result;
				throw new IllegalArgumentException("Unknown parameter: " + key + " for call " + instr);
			}
			
		};
	}

	/**
	 * Creates a suitable pre-condition handler depending on <code>failFast</code>
	 * @param instr
	 * @param value
	 * @param this_loc
	 * @param cont 
	 * @param failFast
	 * @return A {@link CallPreconditionHandler} instance if <code>failFast</code> is
	 * <code>true</code>; a {@link LazyPreconditionHandler} otherwise.
	 * @see #handleInvocation(TACInvocation, TensorPluralTupleLE, IMethodCaseInstance, boolean, SimpleMap)  
	 */
	private CallPreconditionHandler createSplitter(
			final TACInvocation instr, TensorContext cur_context,
			final Aliasing this_loc, CallHandlerContinuation cont, boolean failFast) {
		
		return failFast ? new CallPreconditionHandler(instr, cur_context, this_loc, cont) :
					new LazyPreconditionHandler(instr, cur_context, this_loc, cont);
	}
	
	/**
	 * Continuation called by {@link CallPreconditionHandler#finishSplit()} to
	 * move on to post-condition processing.  Convenient to define this as a
	 * continuation in order to keep post-condition concerns out of the
	 * pre-condition checker.
	 * @author Kevin Bierhoff
	 * @since Sep 29, 2008
	 * @see CallPreconditionHandler
	 * @see LinearOperations#handleInvocation(TACInvocation, TensorPluralTupleLE, IInvocationCaseInstance, boolean, SimpleMap)
	 */
	interface CallHandlerContinuation {

		/**
		 * Call this method to merge the post-condition of an invocation
		 * into the given tuple, assuming the given borrowed permissions.
		 * @param tensor
		 * @param borrowedPerms Map from borrowed locations to their original
		 * permissions.
		 * @return The resulting context(s).
		 */
		LinearContext mergePostIntoTuple(TensorContext tensor,
				Map<Aliasing, FractionalPermissions> borrowedPerms);
		
	}

	/**
	 * Callbacks for checking and splitting a call pre-condition off a tuple as part of a
	 * transfer function.  
	 * Afterwards, it calls a continuation to merge in the call post-condition.  
	 * The checker methods return <code>false</code> when a check fails.  
	 * 
	 * In order to treat borrowed objects special, this splitter delays permission splits
	 * on borrowed objects until after packing.
	 * It then remembers the current permissions of borrowed objects, allowing the post-
	 * condition merger to put those back into the tuple (instead of performing an explicit
	 * merge).
	 * It is important to do this after packing in case packing affects objects borrowed
	 * in the call (e.g., in a method call on a field).
	 * In order to allow this special treatment of borrowed objects in the post-condition,
	 * this class invokes a continuation into which it passes the tuple after pre-condition
	 * checking as well as borrowed permissions.
	 * 
	 * Defined as inner class because it uses instance methods of {@link LinearOperations}.
	 * @author Kevin Bierhoff
	 * @since Sep 15, 2008
	 * @see LazyPreconditionHandler
	 * @see LinearOperations#handleInvocation(TACInvocation, TensorPluralTupleLE, IInvocationCaseInstance, boolean, SimpleMap)
	 */
	class CallPreconditionHandler extends AbstractPredicateChecker implements SplitOffTuple {
		
		// given
		/** Node being processed, for looking up aliasing information etc. */
		private final ASTNode node;
		/** Post-condition processing. */
		private final CallHandlerContinuation cont;

		// populated
		/** Final lattice after pre- and post-condition processing, from {@link #finishSplit()} */
		private LinearContext result = null;
		/** Borrowed locations, populated in {@link #announceBorrowed(Set)}. */
		private Set<Aliasing> borrowed = null;
		/** Permission splits delayed until after packing (internal). */
		private final List<Pair<Aliasing, PermissionSetFromAnnotations>> delayed =
			new LinkedList<Pair<Aliasing, PermissionSetFromAnnotations>>();
		
		/**
		 * Creates a call pre-condition handler for the given invocation instruction.
		 * @param instr Invocation being processed
		 * @param value Initial tuple
		 * @param thisLoc Location of the (possibly unpacked) surrounding receiver
		 * @param cont Continuation to perform post-condition processing
		 * @see #CallPreconditionHandler(ASTNode, TensorPluralTupleLE, Aliasing, CallHandlerContinuation)
		 */
		public CallPreconditionHandler(TACInvocation instr,
				TensorContext cur_context, Aliasing thisLoc,
				CallHandlerContinuation cont) {
			super(cur_context, thisLoc);
			this.node = instr.getNode();
			this.cont = cont;
		}

		/**
		 * Creates a call pre-condition handler for the given AST node.
		 * @param node Node being processed
		 * @param value Initial tuple
		 * @param thisLoc Location of the (possibly unpacked) surrounding receiver
		 * @param cont Continuation to perform post-condition processing
		 * @see #CallPreconditionHandler(TACInvocation, TensorPluralTupleLE, Aliasing, CallHandlerContinuation)
		 */
		protected CallPreconditionHandler(ASTNode node,
				TensorContext cur_context, Aliasing thisLoc,
				CallHandlerContinuation cont) {
			super(cur_context, thisLoc);
			this.node = node;
			this.cont = cont;
		}

		/**
		 * The result of performing pre-condition checks <b>and</b> invoking
		 * the continuation for post-condition processing.
		 * @return
		 */
		public LinearContext getResult() {
			return result;
		}

		@Override
		protected final boolean splitOffInternal(Aliasing var,
				String var_name, TensorPluralTupleLE value, PermissionSetFromAnnotations perms) {
			if(borrowed != null && borrowed.contains(var)) {
				// no permissions for the *current* frame should get here
				// (super-frame permissions are ok...)
				assert !var.equals(this_loc) || perms.getFramePermissions().isEmpty();
				
				if(!checkStateInfoInternal(var, perms.getStateInfo(false), false))
					// check state info right away so we can fail early if desired
					return false;
				delayed.add(Pair.create(var, perms));
				return true;
			}
			else
				return doSplitOffInternal(var, value, perms, false);
		}

		/**
		 * Replaces {@link #splitOffInternal(Aliasing, String, TensorPluralTupleLE, PermissionSetFromAnnotations)}
		 * in subclasses so we can delay permission splitting.  Takes an additional parameter
		 * to force state and constraint checks in {@link LazyPreconditionHandler}.
		 * @param var
		 * @param value
		 * @param perms
		 * @param forceFail 
		 * @return <code>true</code> to continue checking, <code>false</code> otherwise
		 * @see #splitOffInternal(Aliasing, String, TensorPluralTupleLE, PermissionSetFromAnnotations)
		 */
		protected boolean doSplitOffInternal(Aliasing var,
				TensorPluralTupleLE value, PermissionSetFromAnnotations perms, 
				boolean forceFail) {
			return super.splitOffInternal(var, null /* doesn't matter */, value, perms);
		}

		@Override
		public void announceBorrowed(Set<Aliasing> borrowedVars) {
			assert this.borrowed == null;
			this.borrowed = borrowedVars;
		}

		@Override
		public boolean finishSplit() {
			result = prepareAnalyzedMethodReceiverForCall(node, incomingContext,
					getNeededReceiverStates());
			
			final boolean forceFail = !ContextFactory.isSingleContext(result);
			
			result = result.dispatch(new RewritingVisitor() {

				@Override
				public LinearContext context(TensorContext le) {
					TensorPluralTupleLE tuple = le.getTuple();
					
					/*
					 * Finish splitting
					 */
					Map<Aliasing, FractionalPermissions> borrowedPerms;
					if(borrowed != null) {
						borrowedPerms = new HashMap<Aliasing, FractionalPermissions>(
								borrowed.size());

						// populate map of borrowed permissions
						for(Aliasing var : borrowed) {
							borrowedPerms.put(var, tuple.get(var));
						}
	
						// split off delayed permissions
						for(Pair<Aliasing, PermissionSetFromAnnotations> d : delayed) {
							if(! doSplitOffInternal(d.fst(), tuple, d.snd(), forceFail))
								return ContextFactory.trueContext(le.getParentChoiceID(),
										le.getChoiceID());
						}
						
						// split off permissions for packed frame
						for(PermissionSetFromAnnotations pa : getSplitFromThis()) {
							if(! doSplitOffInternal(this_loc, tuple, pa, forceFail))
								return ContextFactory.trueContext(le.getParentChoiceID(),
										le.getChoiceID());
						}
					}
					else
						borrowedPerms = Collections.emptyMap();

					/*
					 * Call continuation to forget state info and merge
					 */
					return cont.mergePostIntoTuple(le, borrowedPerms);
				}
				
			});
			
			return ! ContextFactory.isTrueContext(result);
		}
		
	}
	
	/**
	 * Callbacks for splitting permissions in a call pre-condition off a tuple as part of a
	 * transfer function.  
	 * Unlike its superclass, the checker methods in this class do not 
	 * actually check anything.
	 * The exception is {@link #doSplitOffInternal(Aliasing, TensorPluralTupleLE, PermissionSetFromAnnotations, boolean)}
	 * which can be forced to perform checks.
	 * Defined as inner class because it uses instance methods of {@link LinearOperations}.
	 * @author Kevin Bierhoff
	 * @since Sep 15, 2008
	 * @see CallPreconditionHandler
	 */
	class LazyPreconditionHandler extends CallPreconditionHandler implements SplitOffTuple {
		
		/**
		 * @see CallPreconditionHandler#CallPreconditionHandler(TACInvocation, TensorPluralTupleLE, Aliasing, CallHandlerContinuation)
		 */
		public LazyPreconditionHandler(TACInvocation instr,
				TensorContext cur_context, Aliasing thisLoc,
				CallHandlerContinuation cont) {
			super(instr, cur_context, thisLoc, cont);
		}

		/**
		 * @see CallPreconditionHandler#CallPreconditionHandler(ASTNode, TensorPluralTupleLE, Aliasing, CallHandlerContinuation)
		 */
		protected LazyPreconditionHandler(ASTNode node,
				TensorContext cur_lattice, Aliasing thisLoc,
				CallHandlerContinuation cont) {
			super(node, cur_lattice, thisLoc, cont);
		}
		
		@Override
		protected boolean doSplitOffInternal(
				Aliasing var, TensorPluralTupleLE value, 
				PermissionSetFromAnnotations perms, boolean forceFail) {
			if(forceFail)
				return super.doSplitOffInternal(var, value, perms, forceFail);
			
			// split off permission 
			value = LinearOperations.splitOff(var, value, perms);
			return true; // ignore checks
		}

		@Override
		protected boolean checkStateInfoInternal(Aliasing var,
				Set<String> stateInfo, boolean inFrame) {
			return true;  // ignore this check
		}

		@Override
		public boolean checkFalse(Aliasing var, String var_name) {
			return true;  // ignore this check
		}

		@Override
		public boolean checkNonNull(Aliasing var, String var_name) {
			return true;  // ignore this check
		}

		@Override
		public boolean checkNull(Aliasing var, String var_name) {
			return true;  // ignore this check
		}

		@Override
		public boolean checkTrue(Aliasing var, String var_name) {
			return true;  // ignore this check
		}
		
	}
	
	/**
	 * Call post-condition merger with special treatment of borrowed objects.
	 * {@link #finishMerge()} puts permissions of borrowed objects back into the lattice,
	 * with updated state information, overriding any constraints collected for them
	 * for the processed call.
	 * This is ok as long as we catch constraint failures either right away or,
	 * for {@link LazyPreconditionHandler lazy processing}, in the checker.
	 * Nonetheless, the current approach isn't perfect because it's conceivable
	 * that permissions in addition to ones borrowed in the call are merged from the
	 * post-condition.
	 * These permissions would currently be dropped.
	 * @author Kevin Bierhoff
	 * @since Sep 29, 2008
	 *
	 */
	class CallPostPredicateMerger extends DefaultPredicateMerger implements MergeIntoTuple {

		// given
		/** Permissions for borrowed objects. */
		private Map<Aliasing, FractionalPermissions> borrowed;

		/**
		 * Creates a merger with the given borrowed permissions.
		 * @param astNode Node being processed.
		 * @param value Initial tuple.
		 * @param borrowed Permissions for borrowed objects.
		 */
		public CallPostPredicateMerger(ASTNode astNode,
				TensorPluralTupleLE value, Map<Aliasing, FractionalPermissions> borrowed) {
			super(astNode, value);
			this.borrowed = borrowed;
		}

		@Override
		public void finishMerge() {
			if(borrowed != null) {
				// simply replace the current (after split and merge) permissions
				// with the given permissions for borrowed locations
				// (assuming the given locations are what we had before splitting and merging)
				// but use the new state information after forgetting and merging
				for(Map.Entry<Aliasing, FractionalPermissions> b : borrowed.entrySet()) {
					Aliasing var = b.getKey();
					FractionalPermissions p = value.get(var);
					List<String> virtStates = p.getStateInfo(false);
					List<String> frameStates = p.getStateInfo(true);
					p = b.getValue().withoutStateInfo();
					for(String s : virtStates)
						p = p.learnTemporaryStateInfo(s, false);
					for(String s : frameStates)
						p = p.learnTemporaryStateInfo(s, true);
					value.put(var, p);
				}
			}
			super.finishMerge();
		}
		
	}
	
	/**
	 * Attempts to pack the current method receiver before a call occurs inside the method
	 * being checked (if needed).
	 * May not pack if receiver has unique or immutable unpacked or {@link #packBeforeCall}
	 * is <code>false</code>.
	 * @param node
	 * @param value Tuple before packing; <i>do not use after this method returns</i>.
	 * @param neededAnalyzedMethodReceiverState Receiver <i>frame</i> states needed in the 
	 * called method's pre-condition.  Guaranteed to pack if this set is non-empty.
	 * @return Context(s) after preparing the receiver
	 * @see #wrangleIntoPackedStates(ASTNode, TensorPluralTupleLE, Set, boolean)  
	 */
	private LinearContext prepareAnalyzedMethodReceiverForCall(
			final ASTNode node,
			final TensorContext cur_context,
			Set<String> neededAnalyzedMethodReceiverState) {
		return wrangleIntoPackedStates(node, cur_context, neededAnalyzedMethodReceiverState,
				!packBeforeCall);
	}

	/**
	 * Determine if the unpacked permission of the given variable is unique / immutable.
	 * This is interesting because we don't need to pack these guys.
	 * @param value
	 * @param node
	 * @param thisVar
	 * @return <code>true</code> if the unpacked permission is guaranteed unique or immutable, 
	 * <code>false</code> otherwise.
	 */
	private boolean isUniqueOrImmutableUnpacked(TensorPluralTupleLE value,
			ASTNode node, Variable thisVar) {
		FractionalPermission unp = value.get(node, thisVar).getUnpackedPermission();
		if(unp == null) 
			return false;
		PermissionKind up_kind = unp.getKind(value.get(node, thisVar).getConstraints());
		return PermissionKind.UNIQUE == up_kind || PermissionKind.IMMUTABLE == up_kind;
	}

	private StateSpaceRepository getRepository() {
		return fractContext.getRepository();
	}

	private StateSpace getStateSpace(ITypeBinding type) {
		return fractContext.getRepository().getStateSpace(type);
	}

	private boolean mayBeAnalyzedMethodReceiver(TensorPluralTupleLE value, 
			TACInstruction instr,
			Variable x) {
		if(inStaticMethod())
			return false;
		Aliasing receiver_loc = value.getLocationsAfter(instr.getNode(), getThisVar());
		Aliasing x_loc = value.getLocationsAfter(instr.getNode(), x);
		return receiver_loc.hasAnyLabels(x_loc.getLabels());
	}

	/**
	 * At certain points, we should no longer be able to think about permissions to
	 * fields because they may have been reassigned. If a method or constructor call
	 * does not retain FULL/UNIQUE/IMMUTABLE permission to the receiver, then must forget
	 * anything we know about fields of that receiver. However, because fields can be
	 * mapped to states, we do this on a dimension by dimension basis. 
	 * 
	 * If we're non-reentrant ({@link #packBeforeCall} is <code>false</code>)
	 * then fields cannot be re-assigned during a method call, and so we don't forget
	 * anything about fields in this case.
	 */
	private TensorPluralTupleLE forgetFieldPermissionsIfNeeded(
			TensorPluralTupleLE value, Variable thisVar,
			TACInvocation instr) {

		if( thisVar == null ) return value;
		
		FractionalPermissions this_perms = value.get(instr, thisVar);
		FractionalPermission this_unp = this_perms.getUnpackedPermission();

		if( this_unp != null && ( ! packBeforeCall || isUniqueOrImmutableUnpacked(value, instr.getNode(), thisVar) ))
			// thisVar is unpacked and either we're not packing or unique is unpacked
			// --> no need to forget at all because fields are fully protected
			return value;
		
		FractionAssignment fa = this_perms.getConstraints().simplify();
		
		Set<IVariableBinding> dont_forget_fields = new HashSet<IVariableBinding>();
		
		// For each field...
		next_field:
		for( IVariableBinding field : thisVar.resolveType().getDeclaredFields() ) {
			if((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
				continue;
			
			// Find that field's mapped node
			String mapped_node = this_perms.getStateSpace().getFieldRootNode(field);
			
			if(mapped_node == null)
				// field is not mapped, so we know nothing about it -> no protection
				continue next_field;
			
			// For each permission of this...
			for( FractionalPermission this_perm : this_perms.getFramePermissions() ) {
				// test if this_perm "protects" the field's mapped node so we don't
				// have to forget it
				boolean dont_forget = permissionProtectsFieldsInNode(fa,
						mapped_node, this_perm);
				// add to don't forget
				if( dont_forget ) {
					dont_forget_fields.add(field);
					continue next_field; // next field
				}
			}
			
			if ( this_unp != null && permissionProtectsFieldsInNode(fa, mapped_node, this_unp)) {
				// test if unpacked permission protects the field (not currently needed)
				dont_forget_fields.add(field);
				continue next_field;
			}
		}
		
		// Forget [all fields] \ [don't forget fields]
		for( IVariableBinding field : thisVar.resolveType().getDeclaredFields() ) {
			if((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
				continue;
			if( !dont_forget_fields.contains(field) ) {
				value.put(instr, new FieldVariable(field),
						FractionalPermissions.createEmpty());
			}
		}
		return value;
	}
	
	private Aliasing getFrameAliasing(Variable x) {
		assert x.isUnqualifiedSuper();
		return AliasLE.create(new FrameLabel(((SuperVariable) x).resolveType()));
	}

	/**
	 * @param fa
	 * @param node
	 * @param perm
	 * @return
	 */
	private static boolean permissionProtectsFieldsInNode(
			FractionAssignment fa, String node, FractionalPermission perm) {
		boolean dont_forget = false;
		Fraction belowF = perm.getFractions().getBelowFraction();
		Fraction const_ = fa.getConstant(belowF);
		
		// If perm root is >= mapped node
		if( perm.getStateSpace().firstBiggerThanSecond(perm.getRootNode(),
				node) ) {
			 
			// If One of two cases apply:
			// 1.) The permission type is easy to discover.			
			dont_forget |= fa.isOne(belowF); // UNIQUE/FULL
			dont_forget |= (perm.isReadOnly() && const_!=null && const_.isNamed()); // IMM
			
			// 2.) The permission takes more work to figure out.
			//     For now we will (soundly) ignore these
		}
		// if perm root < mapped node
		else if( perm.getStateSpace().firstBiggerThanSecond(node, perm.getRootNode()) &&
				 !node.equals(perm.getRootNode()) ) {
			// We still need to make sure this permission is not NOTHING
			// One of two cases also must apply
			
			// 1.) We can guarantee easily that the permission is not NOTHING
			// Get the fraction for the mapped node
			Fraction mappedF = perm.getFractions().get(node);
			Fraction mappedC = fa.getConstant(mappedF); // its constant, if any
			// make sure that fraction is non-zero
			dont_forget |= fa.isOne(mappedF);
			dont_forget |= (mappedC != null && mappedC.isNamed());
			
			// 2.) The permission takes more work to figure out.
			//     For now we will (soundly) ignore these					
		}
		return dont_forget;
	}

	private static boolean isEffectFree(IMethodBinding method, 
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> thisPrePost, 
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost) {
		for(int i = 0; i < method.getParameterTypes().length; i++) {
			if(argPrePost[i] != null && argPrePost[i].fst().isReadOnly() == false)
				return false;
		}
		if(method.isConstructor())
			// for constructors, receiver is a "result", so we don't check receiver pre-condition
			return true;
		return thisPrePost != null && thisPrePost.fst().isReadOnly();
	}

	private static TensorPluralTupleLE forgetStateInfo(
			TensorPluralTupleLE value) {
		value.forgetTemporaryStateInformation();
		return value;
	}

	private static TensorPluralTupleLE addVariable(
			TACInstruction instr,
			TensorPluralTupleLE value,
			Variable x, PermissionSetFromAnnotations newPerms,
			List<Pair<Aliasing, ReleaseHolder>> paramInfo) {
		if(x == null) throw new NullPointerException("Null variable provided");
		Aliasing var = value.getLocationsAfter(instr, x);
		value.put(var, newPerms.toLatticeElement());
		if(paramInfo != null && ! paramInfo.isEmpty())
			value.addImplication(var, new ReleasePermissionImplication(new PermissionPredicate(var, newPerms), paramInfo));
		return value;
	}

	private static TensorPluralTupleLE mergeIn(
			TACInstruction instr,
			TensorPluralTupleLE value,
			Variable x, PermissionSetFromAnnotations toMerge) {
		if(x == null) throw new NullPointerException("Null variable provided");
		return mergeIn(value.getLocationsAfter(instr.getNode(), x), value, toMerge);
	}
	
	static TensorPluralTupleLE mergeIn(
			Aliasing a,
			TensorPluralTupleLE value,
			PermissionSetFromAnnotations toMerge) {
		if(a.getLabels().isEmpty()) 
			throw new IllegalArgumentException("Empty aliasing provided");
//		if(toMerge == null || toMerge.isEmpty())
//			return value;
		
		FractionalPermissions permissions = value.get(a);
		permissions = permissions.mergeIn(toMerge);
		value.put(a, permissions);
		return value;
	}
	
	private static TensorPluralTupleLE splitOff(
			TACInstruction instr,
			TensorPluralTupleLE value,
			Variable x, PermissionSetFromAnnotations toSplit) {
		if(x == null) throw new NullPointerException("Null variable provided");
		if(toSplit == null || toSplit.isEmpty()) 
			return value;

		FractionalPermissions permissions = value.get(instr, x);
		permissions = permissions.splitOff(toSplit);
		value.put(instr, x, permissions);
		return value;
	}
	
	static TensorPluralTupleLE splitOff(
			Aliasing a,
			TensorPluralTupleLE value,
			PermissionSetFromAnnotations toSplit) {
		if(a.getLabels().isEmpty()) 
			throw new IllegalArgumentException("Empty aliasing provided");
		if(toSplit == null || toSplit.isEmpty()) 
			return value;

		FractionalPermissions permissions = value.get(a);
		permissions = permissions.splitOff(toSplit);
		value.put(a, permissions);
		return value;
	}
	
	//
	// field access
	//

	/**
	 * Unpacks the given lattice for a field access, if not already unpacked.
	 * @param value Initial lattice information
	 * @param assignmentSource Variable used as the source of an assignment or
	 * <code>null</code> for field reads.
	 * @param instr
	 * @return Possibly a disjunction of new lattice information.
	 */
	public LinearContext handleFieldAccess(
			TensorContext cur_context,
			final Variable assignmentSource,
			final TACFieldAccess instr) {
		
		TensorPluralTupleLE value = cur_context.getTuple();
		
		if("length".equals(instr.getFieldName()) &&
				instr.getAccessedObjectOperand().resolveType().isArray()) {
			// reading the array length
			// do nothing--that's always ok
			assert assignmentSource == null;
			
			// could enforce that there must be a permission for the accessed array:
//			FractionalPermissions array_perms = value.get(loc);
//			array_perms = array_perms.makeNonZero(array_perms.getStateSpace().getRootState());
//			value.put(loc, array_perms);
		}
		else if(getThisVar() != null) {
			/*
			 * We may need to unpack...
			 * Check if field is from the receiver.
			 */
			final Aliasing loc = value.getLocationsBefore(instr.getNode(), instr.getAccessedObjectOperand());
			Aliasing this_loc = value.getLocationsBefore(instr.getNode(), getThisVar());
			if( loc.equals(this_loc) ) {
				boolean isAssignment = assignmentSource != null;

				/*
				 * 1. Save permission that's assigned to field. 
				 */
//				final FractionalPermissions new_field_perms;
//				if(isAssignment)
//					new_field_perms = value.get(
//						value.getLocationsBefore(instr.getNode(), assignmentSource));
//				else
//					new_field_perms = null;

				/*
				 * 2. Unpack receiver if needed.
				 * This may generate permissions for the field, and because of aliasing,
				 * override the permission for the source operand
				 * TODO Maybe we can avoid affecting the source operand during unpacking?
				 */
				LinearContext result = internalHandleFieldAccess(cur_context, 
						isAssignment,
						instr);
				
				
				return result;
			}
			else {
				// not a receiver field
				if(log.isLoggable(Level.WARNING))
					log.warning("Unsupported field access: " + instr.getNode());
			}
		}
		else {
			// static method--cannot be a receiver field access
			if(log.isLoggable(Level.WARNING))
				log.warning("Unsupported static field access: " + instr.getNode());
		}
		return ContextFactory.tensor(value, cur_context.getParentChoiceID(), cur_context.getChoiceID());
	}
	
	private LinearContext internalHandleFieldAccess(
			TensorContext cur_context,
			final boolean isAssignment, 
			final TACFieldAccess instr) {
		final TensorPluralTupleLE value = cur_context.getTuple();
		/*
		 * Better see if fields have been unpacked...
		 */
		Variable thisVar = instr.getAccessedObjectOperand();
		StateSpace this_space = getStateSpace(thisVar.resolveType());
		String unpacking_root = 
			this_space.getFieldRootNode(instr.resolveFieldBinding());
		
		if(unpacking_root != null) {
			
			if(value.isRcvrPacked()) {
				return value.fancyUnpackReceiver(thisVar, instr.getNode(), 
					getRepository(),
					new SimpleMap<Variable,Aliasing>() {
						@Override
						public Aliasing get(Variable key) {
							return value.getLocations(key);
						}},
					unpacking_root, 
					isAssignment ? instr.getFieldName() : null,
					false /* do not keep packed tuple */, 
					cur_context.getParentChoiceID(), 
					cur_context.getChoiceID());
			}
			else { // already unpacked...
				final Aliasing loc = 
					value.getLocationsBefore(instr.getNode(), thisVar);
				FractionalPermissions rcvrPerms = value.get(loc);
				if(isAssignment) {
					/*
					 * need unpacked root to be at least as big as 
					 * the node the field is mapped to 
					 */
					if(!this_space.firstBiggerThanSecond(
							rcvrPerms.getUnpackedPermission().getRootNode(), 
							unpacking_root)) {
						return ContextFactory.trueContext(cur_context.getParentChoiceID(),
								cur_context.getChoiceID());
					}
					/*
					 * Force unpacked permission to be modifiable, to allow assignment.
					 */
					rcvrPerms = rcvrPerms.makeUnpackedPermissionModifiable();
					value.put(loc, rcvrPerms);
				}
				else /* field read */ {
					/* 
					 * unpacked permission must overlap with 
					 * the node the field is mapped to 
					 */
					if(this_space.areOrthogonal(
							rcvrPerms.getUnpackedPermission().getRootNode(), 
							unpacking_root)) {
						return ContextFactory.trueContext(cur_context.getParentChoiceID(),
								cur_context.getChoiceID());
					}
				}
				return ContextFactory.tensor(value, cur_context.getParentChoiceID(), cur_context.getChoiceID());
			}
		}
		else {
			// field is not mapped -> no unpack, no modifiable permission for assignment needed
			return ContextFactory.tensor(value, cur_context.getParentChoiceID(), cur_context.getChoiceID());
		}
	}
	
	//
	// Post-condition checks
	//
	
	/**
	 * Applies the given post-condition to the given tuple, assuming the given
	 * variable as the method return value, and returns a string describing 
	 * failing conditions, if any.  This method is meant to be called in a checker
	 * routing following a flow analysis.
	 * @param node
	 * @param curLattice Readonly or mutable tuple, will not be changed.
	 * @param resultVar Method return value or <code>null</code> for <code>void</code>
	 * methods.
	 * @param post The post-condition to be checked.
	 * @param parameterVars Map containing the original method parameter locations at method entry.
	 * This <i>cannot</i> be inferred from the given tuple because parameter variables
	 * can be re-assigned in the method body.
	 * @param trueTest Pair of parameter and state indicated for that parameter by a 
	 * <code>true</code> return value, or <code>null</code> if the checked
	 * method is no such test.
	 * @param falseTest Same as <code>trueTest</code> but for <code>false</code> return value
	 * @return a string describing failing conditions or <code>null</code> if the
	 * check was successful.
	 */
	public String checkPostCondition(ASTNode node, 
			TensorContext cur_context,
			Variable resultVar,
			PredicateChecker post,
			SimpleMap<String, Aliasing> parameterVars,
			Pair<String, String> trueTest,
			Pair<String, String> falseTest) {
		// 0. determine receiver and result locations
		Aliasing this_loc = inStaticMethod() ? null : parameterVars.get("this!fr");
		TensorPluralTupleLE curLattice = cur_context.getTuple();
		Aliasing res_loc;
		
		if(resultVar == null) {
			res_loc = null;
			assert trueTest == null && falseTest == null; // can't have state tests without result
		}
		else {
			res_loc = curLattice.getLocationsAfter(node, resultVar);
			assert res_loc != null; // make sure result location exists
		}
		final SimpleMap<String, Aliasing> vars = createPostMap(parameterVars, res_loc);
		
		// 1. Check without state tests
		{
			PredicateErrorReporter checks = new PredicateErrorReporter(true, // return checker 
					node, cur_context, this_loc);
			post.splitOffPredicate(vars, checks);
			if(checks.hasErrors())
				return checks.getErrors().iterator().next();
		}

		// 2. Check true state test
		if(trueTest != null && ! curLattice.isBooleanFalse(res_loc)) {
			// checker makes a copy of the lattice
			PredicateErrorReporter checks = new PredicateErrorReporter(true, // return checker 
					node, cur_context, this_loc);
			
			// TODO hack: slam antecedent in
			checks.incomingContext.getTuple().addTrueVarPredicate(res_loc);
			
			// check whether state is indicated
			Aliasing tested_loc = parameterVars.get(trueTest.fst());
			checks.checkStateInfo(tested_loc, trueTest.fst(),
					Collections.singleton(trueTest.snd()), // tested state
					"this!fr".equals(trueTest.fst()));     // frame test?
			// ignore always-true return value: errors will appear in the list

			post.splitOffPredicate(vars, checks);
			if(checks.hasErrors())
				return "For indicating " + trueTest.fst() + " in " + trueTest.snd() + ": " + 
						checks.getErrors().iterator().next();
		}
		
		// 3. Check false state test
		if(falseTest != null && ! curLattice.isBooleanTrue(res_loc)) {
			// checker makes a copy of the lattice
			PredicateErrorReporter checks = new PredicateErrorReporter(true, // return checker 
					node, cur_context, this_loc);
			
			// TODO hack: slam antecedent in
			checks.incomingContext.getTuple().addFalseVarPredicate(res_loc);
			
			// check whether state is indicated
			Aliasing tested_loc = parameterVars.get(falseTest.fst());
			checks.checkStateInfo(tested_loc, falseTest.fst(),
					Collections.singleton(falseTest.snd()), // tested state
					"this!fr".equals(falseTest.fst()));     // frame test?
			// ignore always-true return value: errors will appear in the list

			post.splitOffPredicate(vars, checks);
			if(checks.hasErrors())
				return "For indicating " + falseTest.fst() + " in " + falseTest.snd() + ": " +
						checks.getErrors().iterator().next();
		}
		
		return null;
	}
	
	/**
	 * Creates a aliasing map for checking method post-conditions based on the
	 * aliasing map for the method parameters and the given result value location.
	 * @param parameterVars
	 * @param resultLoc May be <code>null</code> for <code>void</code> methods.
	 * @return a aliasing map for checking method post-conditions.
	 * @see #checkPostCondition(ASTNode, TensorPluralTupleLE, Variable, PredicateChecker, SimpleMap, Map)
	 */
	private SimpleMap<String, Aliasing> createPostMap(
			final SimpleMap<String, Aliasing> parameterVars, final Aliasing resultLoc) {
		return new SimpleMap<String, Aliasing>() {
			@Override
			public Aliasing get(String key) {
				if("result".equals(key)) {
					return resultLoc;
				}
				return parameterVars.get(key);
			}
		};
	}
	
	/**
	 * Applies the given pre-condition to the given tuple with the given
	 * receiver and argument variables and returns an error string describing
	 * failing conditions, if any.  This method is meant to be called in a checker
	 * routing following a flow analysis.
	 * @param node
	 * @param value Readonly or mutable tuple, will not be changed.
	 * @param receiver
	 * @param arguments
	 * @param pre The pre-condition to be checked.
	 * @return a string describing failing conditions or <code>null</code> if the
	 * check was successful.
	 */
	public String checkCallPrecondition(final ASTNode node,
			TensorContext cur_context,
			Variable receiver,
			List<Variable> arguments, final PredicateChecker pre) {
		final TensorPluralTupleLE value = cur_context.getTuple();
		final SimpleMap<String, Aliasing> argLocs = createNodeArgumentMap(node, value, receiver, arguments);
		final Aliasing this_loc = value.getLocationsBefore(node, getThisVar());
		
		if(node != null && (node instanceof SuperMethodInvocation) && value.isRcvrPacked()) {
			// super-method call....
			// may have to unpack to get super-frame permissions
			LinearContext unpacked = value.fancyUnpackReceiver(
					getThisVar(), node, getRepository(),
					new SimpleMap<Variable,Aliasing>() {
						@Override
						public Aliasing get(Variable key) {
							return value.getLocations(key);
						}},
					StateSpace.STATE_ALIVE /* super always mapped to root node */, 
					null /* not an assignment */,
					true /* also keep the packed version */, 
					cur_context.getParentChoiceID(), 
					cur_context.getChoiceID());
			// keeping the original means there is at least one tuple in unpacked
			// that's good because we'll visit at least one tuple below and 
			// get error messages from it
			
			return unpacked.dispatch(new ErrorReportingVisitor() {
				
				@Override
				public String context(TensorContext le) {
					return checkCallPreconditionInternal(node, le, pre, 
							argLocs, this_loc);
				}

				@Override
				public String checkTuple(TensorPluralTupleLE tuple) {
					return Utilities.nyi("Should be prevented by overriding context");
				}
			});
		}
		else 
			// standard case
			// just do the pre-condition check outright
			return checkCallPreconditionInternal(node, cur_context, pre, argLocs,
					this_loc);
	}

	/**
	 * @param node
	 * @param value
	 * @param pre
	 * @param argLocs
	 * @param this_loc
	 * @return
	 */
	private String checkCallPreconditionInternal(ASTNode node,
			TensorContext cur_context, PredicateChecker pre,
			SimpleMap<String, Aliasing> argLocs, Aliasing this_loc) {
		PredicateErrorReporter checker = new PredicateErrorReporter(
				false /* pre-checker */,
				node, cur_context, this_loc);
		pre.splitOffPredicate(argLocs, checker);
		if(checker.hasErrors())
			return ErrorReportingVisitor.errorString(checker.getErrors(), " and ");
		else
			return null;
	}
	
	/**
	 * Creates a aliasing map for checking call pre-conditions based on the
	 * given receiver and argument variables.  
	 * This method is similar to {@link #createParameterMap(TACInvocation, TensorPluralTupleLE, Variable, Variable)}
	 * but uses a AST node for looking up locations in the tuple, making it suitable
	 * for use in AST-based checker routines.
	 * @param node
	 * @param value
	 * @param receiver The receiver variable or <code>null</code> for static methods.
	 * @param arguments Actual method argument variables.
	 * @return a aliasing map for checking method pre-conditions.
	 * @see #checkCallPrecondition(ASTNode, TensorPluralTupleLE, Variable, List, PredicateChecker)
	 */
	private SimpleMap<String, Aliasing> createNodeArgumentMap(
			ASTNode node, TensorPluralTupleLE value,
			Variable receiver, List<Variable> arguments) {
		Aliasing virt_loc = null;
		Aliasing frame_loc = null;
		if(receiver != null) {
			if(receiver.isUnqualifiedSuper()) {
				virt_loc = value.getLocationsBefore(node, getThisVar());
				frame_loc = getFrameAliasing(receiver);
			}
			else {
				virt_loc = value.getLocationsBefore(node, receiver);
				frame_loc = value.getLocationsBefore(node, receiver);
			}
		}
		return createNodeArgumentMap(node, value, virt_loc, frame_loc, arguments);
	}

	/**
	 * Creates a aliasing map for checking call pre-conditions based on the
	 * given receiver locations and argument variables.
	 * Used internally by {@link #createNodeArgumentMap(ASTNode, TensorPluralTupleLE, Variable, List)}.  
	 * This method is similar to {@link #createParameterMap(TACInvocation, TensorPluralTupleLE, Aliasing, Aliasing, Aliasing)}
	 * but uses a AST node for looking up locations in the tuple, making it suitable
	 * for use in AST-based checker routines.
	 * @param node
	 * @param value
	 * @param thisVirt May be <code>null</code>
	 * @param thisFrame May be <code>null</code>
	 * @param arguments
	 * @return a aliasing map for checking method pre-conditions.
	 */
	private SimpleMap<String, Aliasing> createNodeArgumentMap(final ASTNode node,
			TensorPluralTupleLE value, Aliasing thisVirt, Aliasing thisFrame,
			List<Variable> arguments) {
		final Map<String, Aliasing> argMap = new HashMap<String, Aliasing>();
		if(thisVirt != null)
			argMap.put("this", thisVirt);
		if(thisFrame != null)
			argMap.put("this!fr", thisFrame);
		for(int i = 0; i < arguments.size(); i++) {
			argMap.put("#" + i, value.getLocationsBefore(node, arguments.get(i)));
		}
		return new SimpleMap<String, Aliasing>() {
			@Override
			public Aliasing get(String key) {
				Aliasing result = argMap.get(key);
				assert result != null : 
					// tolerate null receiver aliasing for "new" pre-condition checking 
					"Parameter unknown: " + key + " in " + node;
				return result;
			}
		};
	}

	/**
	 * Callbacks for generating human-readable error messages for violated predicates
	 * as part of a code checker following a flow analysis.
	 * This class can be used for checking call pre-conditions as well as
	 * method body post-conditions.  Depending on which is checked,
	 * different packing strategies are applied and the error messages are 
	 * slightly different.
	 * Defined as inner class because it uses instance methods
	 * of {@link LinearOperations}.
	 * @author Kevin Bierhoff
	 * @since Sep 19, 2008
	 * @see LinearOperations#checkCallPrecondition(ASTNode, TensorPluralTupleLE, Variable, List, PredicateChecker)
	 * @see LinearOperations#checkPostCondition(ASTNode, TensorPluralTupleLE, Variable, PredicateChecker, SimpleMap, Map)
	 */
	class PredicateErrorReporter extends AbstractPredicateChecker {
		
		private final Set<String> errors = new LinkedHashSet<String>();
		private final boolean isReturnCheck;
		private final ASTNode node;
		
		/**
		 * Creates a mutable copy of the given lattice as to not interfere
		 * with existing results.
		 * @param isReturnCheck <code>true</code> will check the predicate
		 * as a post-condition of a method body; <code>false</code> will check
		 * the predicate as a pre-condition of a method call.  
		 * @param node
		 * @param value
		 * @param thisLoc
		 */
		public PredicateErrorReporter(boolean isReturnCheck, ASTNode node,
				TensorContext cur_context, Aliasing thisLoc) {
			super(cur_context.mutableCopy(), thisLoc);
			this.isReturnCheck = isReturnCheck;
			this.node = node;
		}
		
		/**
		 * Indicates whether errors were found.
		 * @return <code>true</code> if errors were found, <code>false</code> otherwise.
		 */
		public boolean hasErrors() {
			return errors.isEmpty() == false;
		}
		
		/**
		 * Returns the list of errors found.
		 * @return the list of errors found.
		 */
		public Set<String> getErrors() {
			return Collections.unmodifiableSet(errors);
		}

		/**
		 * Override this method to return a string describing the given location 
		 * in error messages to the user.
		 * @param var
		 * @param var_name Formal parameter name, if known, <code>null</code> otherwise.
		 * @return a string describing the given location in error messages to the user.
		 */
		protected String getSourceString(Aliasing var, String var_name) {
			if(var_name != null) {
				if(var_name.equals("result") || var_name.equals("this") || var_name.equals("this!fr"))
					// don't use "parameter" for receiver and result return checks
					// shouldn't normally report errors for result if isReturnCheck false
					return isReturnCheck ? var_name : "argument " + var_name;
				return (isReturnCheck ? "formal parameter " : "argument ") + var_name;
			}
			return var == null ? "NULL location" : var.toString();
		}

		@Override
		public boolean finishSplit() {
			if(this_loc == null) {
				// no receiver
				assert getNeededReceiverStates().isEmpty();
				return hasErrors();
			}
			
			LinearContext packed_lattice = isReturnCheck ?
					// TODO combine wrangle and prepare
					prepareAnalyzedMethodReceiverForReturn(node, this.incomingContext, getNeededReceiverStates()) :
						prepareAnalyzedMethodReceiverForCall(node, this.incomingContext, 
								getNeededReceiverStates());
			
			// fail right away if wrangling was unsuccessful
			if( !hasErrors() /* only check this if previously no error */ && 
					ContextFactory.isTrueContext(packed_lattice) ) {
				if(getNeededReceiverStates().isEmpty())
					errors.add("Could not pack receiver to any state " +
							"due to insufficient field permissions " + (isReturnCheck ? "for return" : "for call"));					
				else
					errors.add("Could not pack receiver to states " +
							getNeededReceiverStates() + " due to insufficient field permissions " +
							(isReturnCheck ? "for return" : "for call"));
				return false;
			}

			// check post-condition for each possible outcome
			String error = packed_lattice.dispatch(new ErrorReportingVisitor() {

				@Override
				public String checkTuple(TensorPluralTupleLE tuple) {
					for(PermissionSetFromAnnotations pa : getSplitFromThis()) {
						if(pa.isEmpty())
							continue;
						if(!tuple.get(this_loc).isInStates(pa.getStateInfo(false), false)) {
							return "After packing, receiver must be in state " + 
									pa.getStateInfo(false) + " but is in " + 
									tuple.get(this_loc).getStateInfo(false);
						}
						else if(!tuple.get(this_loc).isInStates(pa.getStateInfo(true), true)) {
							return "After packing, receiver frame must be in state " + 
									pa.getStateInfo(true) + " but is in " + 
									tuple.get(this_loc).getStateInfo(true);
						}
						// keep going anyway....

						// split off permission 
						FractionalPermissions beforeSplit = tuple.get(this_loc);
						tuple = LinearOperations.splitOff(this_loc, tuple, pa);
						
						if(tuple.get(this_loc).isUnsatisfiable()) {
							// constraint failure
							return "After packing, receiver must " + 
								(isReturnCheck ? "return" : "have") + " permissions " + pa.getUserString() + 
								" but it has " + beforeSplit.getUserString();
						}
					}
					return null;
				}
				
			});
			if(error != null)
				errors.add(error);
			return hasErrors();
		}
		
		@Override
		public boolean splitOffPermission(Aliasing var, String var_name,
				PermissionSetFromAnnotations perms) {
			super.splitOffPermission(var, var_name, perms);
			// error reporting happens in splitOffInternal
			return true; // keep going to collect more errors
		}

		@Override
		public boolean checkFalse(Aliasing var, String var_name) {
			boolean result = super.checkFalse(var, var_name);
			if(! result)
				errors.add("Must " + (isReturnCheck ? "return" : "be") + 
						" false: " + getSourceString(var, var_name));
			return true; // keep going to collect more errors
		}

		@Override
		public boolean checkNonNull(Aliasing var, String var_name) {
			boolean result = super.checkNonNull(var, var_name);
			if(! result)
				errors.add("Must not " + (isReturnCheck ? "return" : "be") + 
						" null: " + getSourceString(var, var_name));
			return true; // keep going to collect more errors
		}

		@Override
		public boolean checkNull(Aliasing var, String var_name) {
			boolean result = super.checkNull(var, var_name);
			if(! result)
				errors.add("Must " + (isReturnCheck ? "return" : "be") + 
						" null: " + getSourceString(var, var_name));
			return true; // keep going to collect more errors
		}

		@Override
		public boolean checkStateInfo(Aliasing var, String var_name, 
				Set<String> stateInfo, boolean inFrame) {
			boolean result = super.checkStateInfo(var, var_name, stateInfo, inFrame);
			
			final TensorPluralTupleLE value = this.incomingContext.getTuple();
			if(! result) {
				List<String> actual = value.get(var).getStateInfo(inFrame);
				if(inFrame)
					errors.add(getSourceString(var, var_name) + " frame must " + (isReturnCheck ? "return" : "be") + 
						" in state(s) " + stateInfo + " but is in " + actual);
				else
					errors.add(getSourceString(var, var_name) + " must " + (isReturnCheck ? "return" : "be") + 
						" in state(s) " + stateInfo + " but is in " + actual);
			}
			return true; // keep going to collect more errors
		}

		@Override
		public boolean checkTrue(Aliasing var, String var_name) {
			boolean result = super.checkTrue(var, var_name);
			if(! result)
				errors.add("Must " + (isReturnCheck ? "return" : "be") + 
						" true: " + getSourceString(var, var_name));
			return true; // keep going to collect more errors
		}
		
		@Override
		protected boolean splitOffInternal(Aliasing var, 
				String var_name,
				TensorPluralTupleLE value, PermissionSetFromAnnotations perms) {
			if((var == null || var.getLabels().isEmpty()) && perms.isEmpty())
				// this should be an empty permission set for results
				// this happens when checking the post-condition of VOID methods
				// or when checking the pre-condition of a "new" call (with the created object)
				return true;
			
			if(!value.get(var).isInStates(perms.getStateInfo(false), false)) {
				errors.add(getSourceString(var, var_name) + " must " + (isReturnCheck ? "return" : "be") + 
						" in state " + perms.getStateInfo(false) + 
						" but is in " + value.get(var).getStateInfo(false));
			}
			else if(!value.get(var).isInStates(perms.getStateInfo(true), true)) {
				errors.add(getSourceString(var, var_name) + " frame must " + (isReturnCheck ? "return" : "be") + 
						" in state " + perms.getStateInfo(true) + 
						" but is in " + value.get(var).getStateInfo(true));
			}
			// keep going anyway....
			
			// split off permission 
			FractionalPermissions beforeSplit = value.get(var);
			value = LinearOperations.splitOff(var, value, perms);
			
			if(value.get(var).isUnsatisfiable()) {
				// constraint failure
				errors.add(getSourceString(var, var_name) + " must " + (isReturnCheck ? "return" : "have") + 
						" permissions " + perms.getUserString() + 
						" but it has " + beforeSplit.getUserString());
			}
			return true; // keep going to collect more errors
		}

		@Override
		public void announceBorrowed(Set<Aliasing> borrowedVars) {
			// ignore
		}

	}

	/**
	 * @param node
	 * @param curLattice
	 * @param paramPost
	 * @param resultLoc
	 * @param resultPost
	 * @param indicatedStates
	 * @return
	 */
	private String checkPostConditionOption(final ASTNode node,
			final TensorContext cur_context,
			final Map<Aliasing, PermissionSetFromAnnotations> paramPost,
			final Aliasing resultLoc,
			final PermissionSetFromAnnotations resultPost,
			final Map<Aliasing, StateImplication> indicatedStates) {
		TensorPluralTupleLE curLattice = cur_context.getTuple();
		
		curLattice = curLattice.mutableCopy();
		
		if(indicatedStates.isEmpty() == false) {
			// add antecedents for state implications
			Set<Aliasing> vars = new HashSet<Aliasing>();
			for(StateImplication test : indicatedStates.values()) {
				assert ! test.getAntecedant().isUnsatisfiable(curLattice) :
					"Unsatisfiable state indicator: " + test;
				test.getAntecedant().putIntoLattice(curLattice);
				vars.add(test.getAntecedant().getVariable());
			}
			// solve to make sure we derive any available facts
			curLattice.solveWithHints(vars.toArray(new Aliasing[0]));
		}
		
		final LinearContext packed_lattice;
		if(getThisVar() != null) {
			final Aliasing this_loc = curLattice.getLocationsAfter(node, getThisVar());
			final PermissionSetFromAnnotations this_post_perm = paramPost.get(this_loc);
			
			final Set<String> needed_rcvr_states;
			if(this_post_perm == null)
				needed_rcvr_states = Collections.emptySet();
			else {
				// fail early if receiver is packed and unsat
				if(curLattice.isRcvrPacked() && curLattice.get(this_loc).isUnsatisfiable())
					return this_loc + " returns no suitable permissions for post-condition";

				if(indicatedStates.containsKey(this_loc)) {
					StateImplication test = indicatedStates.get(this_loc);
					
					// enrich post-condition states with indicated state
					Set<String> state_union = new LinkedHashSet<String>(this_post_perm.getStateInfo(true));
					state_union.add(test.getVarState());
					needed_rcvr_states = AbstractFractionalPermission.cleanStateInfo(
							this_post_perm.getStateSpace(), 
							state_union, 
							true /* expect problems with tested states overlapping with others */);
				}
				else {
					needed_rcvr_states = this_post_perm.getStateInfo(true);
				}
				
			}
			
			// pack receiver, if necessary
			packed_lattice =
				this.prepareAnalyzedMethodReceiverForReturn(node, cur_context, needed_rcvr_states);
			
			// fail right away if wrangling was unsuccessful
			if( ContextFactory.isImpossible(packed_lattice) ) {
				if(needed_rcvr_states.isEmpty())
					return "Could not pack receiver to any state " +
							"due to insufficient field permissions";					
				else
					return "Could not bring receiver into post-condition states " +
							needed_rcvr_states + " due to insufficient field permissions";	
			}

			// check post-condition for each possible outcome
			DisjunctiveVisitor<String> testTuples = new ErrorReportingVisitor() {
				@Override
				public String checkTuple(TensorPluralTupleLE tuple) {
					return checkPostConditionAfterPacking(node, tuple, 
							paramPost, resultLoc, resultPost, indicatedStates);
				}
			};
			return packed_lattice.dispatch(testTuples);
		}
		else
			return checkPostConditionAfterPacking(node, curLattice, 
					paramPost, resultLoc, resultPost, indicatedStates);
		
	}

	
	/**
	 * Attempts to pack the current method receiver for a return from the method being analyzed.
	 * May not pack if receiver has unique or immutable unpacked.
	 * @param node
	 * @param neededStates States needed for the receiver <i>frame</i>; wrangling cannot
	 * be used to get virtual permissions into desired states.s
	 * @param value Tuple before packing; <i>do not use after this call returns</i>.
	 * @return Possible contexts after packing, including {@link ContextFactory#trueContext()}
	 * if packing was unsuccessful, but the resulting permissions need not be in the right state.
	 * {@link #wrangleIntoPackedStates(ASTNode, TensorPluralTupleLE, Set, boolean)} 
	 */
	private LinearContext prepareAnalyzedMethodReceiverForReturn(
			final ASTNode node, 
			final TensorContext cur_context,
			final Set<String> neededStates) {
		return wrangleIntoPackedStates(node, cur_context, neededStates, false);
	}
	/**
	 * When the receiver is in some dubious state, this method packs/unpacks the
	 * lattice and tries to wrangle it into the given set of states.
	 * @param node
	 * @param neededStates States needed for the receiver <i>frame</i>; wrangling cannot
	 * be used to get virtual permissions into desired states.s
	 * @param value
	 * @param needNotPack If <code>true</code>, this method only tries to pack if
	 * <code>neededStates</code> is non-empty.  This is useful for checking calls
	 * inside non-reentrant methods. 
	 * @return Possible contexts after packing, including {@link ContextFactory#trueContext()}
	 * if packing was unsuccessful, but the resulting permissions need not be in the right state.
	 * @see #prepareAnalyzedMethodReceiverForCall(ASTNode, TensorPluralTupleLE, Set)
	 * @see #prepareAnalyzedMethodReceiverForReturn(ASTNode, TensorPluralTupleLE, Set) 
	 */
	private LinearContext wrangleIntoPackedStates(
			final ASTNode node, 
			TensorContext cur_context,
			final Set<String> neededStates, boolean needNotPack) {
		
		final TensorPluralTupleLE value = cur_context.getTuple();
		
		final SimpleMap<Variable, Aliasing> loc_map = new SimpleMap<Variable, Aliasing>() {
			@Override
			public Aliasing get(Variable key) {
				return value.getLocationsBefore(node, key);
			}};
		
		if(neededStates.isEmpty()) {
			/*
			 * No post-condition for the receiver.
			 * Make sure we can pack to SOMETHING. Go home.
			 */
			if( value.isRcvrPacked() )
				// already packed -> done
				return ContextFactory.tensor(value, cur_context.getParentChoiceID(), cur_context.getChoiceID());
			else if(needNotPack || isUniqueOrImmutableUnpacked(value, node, getThisVar()))
				// don't have to pack if needNotPack true or unique / immutable
				return ContextFactory.tensor(value, cur_context.getParentChoiceID(), cur_context.getChoiceID());
			else
				// try packing to any state
				return value.fancyPackReceiverToBestGuess(cur_context, getThisVar(),
						getRepository(), loc_map, cur_context.getChoiceID()) ;
		}

		/*
		 * we have needed states.
		 * must pack no matter what (ignoring needNotPack).
		 */
		final ThisVariable this_var = getThisVar();
		final Aliasing this_loc = loc_map.get(this_var);
		final StateSpace rcvr_state_space = getRepository().getStateSpace(this_var.resolveType());
		Set<String> states = AbstractFractionalPermission.cleanStateInfo(
				rcvr_state_space, 
				neededStates, true);
		
		if( ! value.isRcvrPacked() ) {
			/*
			 * Pack to the subset of states under the currently unpacked permission
			 */
			String root = value.get(this_loc).getUnpackedPermission().getRootNode();
			
			// find the states we can pack to under the currently unpacked permission
			final Set<String> packToStates = AbstractFractionalPermission.filterStateInfo(
					rcvr_state_space,
					states,
					root);

			PackingResult packed = 
				value.packReceiver(cur_context,
					getThisVar(), getRepository(), loc_map, packToStates);

			if( !packed.worked() ) {
				// couldn't pack to needed states -> fail
				String state = packed.failedState().unwrap();
				String inv = packed.failedInvariant().unwrap();
				return ContextFactory.failedPack(state, inv, 
						cur_context.getParentChoiceID(), cur_context.getChoiceID());
			}
			// else see if all states are satisfied below
		}
		// we're definitely packed here
		
		/*
		 * See if needed states are satisfied, un- and re-pack if not 
		 */
		FractionalPermissions this_perms = value.get(value.getLocationsBefore(node, this_var));
		Set<String> unsatStates = filterFrameUnsatisfied(this_perms, states);
		if( unsatStates.isEmpty() ) {
			// home free: all needed states there
			return ContextFactory.tensor(value, cur_context.getParentChoiceID(), cur_context.getChoiceID());
		}
		else {
			// there are states we need to get to that we're not already in
			String unpack_state = null;
			for(String s : unsatStates) {
				for(FractionalPermission p : this_perms.getFramePermissions()) {
					if(p.getStateSpace().firstBiggerThanSecond(p.getRootNode(), s)) {
						if(unpack_state == null)
							unpack_state = p.getRootNode();
						else if(! unpack_state.equals(p.getRootNode())) {
							// TODO unpack to tightest root around needed states
							// if we would have to unpack multiple permissions,
							// just try to unpack alive for now
							unpack_state = StateSpace.STATE_ALIVE;
						}
						break;
					}
					if(p.getStateSpace().firstBiggerThanSecond(s, p.getRootNode())) {
						// can't happen: isInState(state) should be true in this case
						log.warning("Object not in state " + s + " even though " +
								"more precise permission is available: " + this_perms);
					}
				}
			}
			if(unpack_state == null)
				// no permission could be unpacked to reach desired state -> fail
				return ContextFactory.trueContext(cur_context.getParentChoiceID(),
						cur_context.getChoiceID());
			
			/*
			 * We have to try to unpack and pack to the correct states.
			 */
			LinearContext unpacked = 
			value.fancyUnpackReceiver(this_var, node, 
					getRepository(),
					loc_map, unpack_state, 
					null /* no assigned field */, 
					false /* do not keep packed version */, 
					cur_context.getParentChoiceID(),
					cur_context.getChoiceID());

			// find the states we need to re-pack to
			final Set<String> packToStates = AbstractFractionalPermission.cleanStateInfo(
					rcvr_state_space,
					unpack_state,
					neededStates,
					true,
					true);

			unpacked = unpacked.dispatch(new RewritingVisitor() {
				@Override
				public LinearContext context(TensorContext le) {
					if(le.getTuple().isRcvrPacked())
						// unpack doesn't always unpack
						return le;
					
					FractionalPermission unpacked_perm = le.getTuple().get(this_loc).getUnpackedPermission();
					// find the subset of packToStates that are below the unpacked root
					Set<String> filteredPackToStates = new LinkedHashSet<String>();
					for(String n : packToStates) {
						if(unpacked_perm.coversNode(n))
							filteredPackToStates.add(n);
						else if(! le.getTuple().get(this_loc).isInState(n, true))
							// cannot pack to n and rcvr is not currently in n --> fail
							return ContextFactory.trueContext(le.getParentChoiceID(),
									le.getChoiceID());
						// else n is outside unpacked_perm and rcvr is currently in n --> ignore
					}
					
					PackingResult pack_result = le.getTuple().packReceiver(
							le, 
							this_var, 
							getRepository(), 
							loc_map, filteredPackToStates 
							);

					if(pack_result.worked())
						return le;
					else {
						String state = pack_result.failedState().unwrap();
						String inv = pack_result.failedInvariant().unwrap();
						return ContextFactory.failedPack(state, inv,
								le.getParentChoiceID(), le.getChoiceID());
					}
				}
			});
			return unpacked.compact(node, false);
		}
	}

	/**
	 * This is a helper method for {@link #wrangleIntoPackedStates(Set, TensorPluralTupleLE)}
	 * that returns the subset of states not satisfied by the given permissions.
	 * @param perms
	 * @param states
	 * @return the subset of <code>states</code> not satisfied by the given permissions.
	 */
	private static Set<String> filterFrameUnsatisfied(FractionalPermissions perms, Set<String> states) {
		LinkedHashSet<String> result = new LinkedHashSet<String>();
		for(String s : states) {
			if(! perms.isInState(s, true))
				result.add(s);
		}
		return result;
	}

	/**
	 * Tests the post-condition for a set of parameters (including
	 * the receiver) and the method result.
	 * More abstractly, this checks whether the permissions for a given set of objects
	 * as well as a given variable are satisfiable.  If a permission for
	 * an unpacked object is to be satisfied it helps to pack before
	 * this method is called.	 
	 * @param paramPost
	 * @param resultLoc
	 * @param resultPost
	 * @param tuple
	 * @param indicatedStates (possibly empty) map from locations to additional states
	 * these locations should be in (for dynamic state test implementations).
	 * @return
	 */
	private String checkPostConditionAfterPacking(
			final ASTNode node,
			TensorPluralTupleLE tuple,
			final Map<Aliasing, PermissionSetFromAnnotations> paramPost,
			final Aliasing resultLoc,
			final PermissionSetFromAnnotations resultPost, 
			Map<Aliasing, StateImplication> indicatedStates) {
		// 1. check parameter post-conditions (incl. receiver)
		for(Aliasing x : paramPost.keySet()) {
			
			FractionalPermissions paramPerms = tuple.get(x);

			for(String needed : paramPost.get(x).getStateInfo(false)) {
				if(paramPerms.isInState(needed, false) == false)
					return x + " must return in state " + needed + " but is in " + paramPerms.getStateInfo(false);
			}
			
			for(String needed : paramPost.get(x).getStateInfo(true)) {
				if(paramPerms.isInState(needed, true) == false)
					return x + "'s frame must return in state " + needed + " but is in " + paramPerms.getStateInfo(true);
			}
			
			if(indicatedStates.containsKey(x)) {
				// could use StateImplication.isSatisfied, 
				// but seems pointless because we previously assert the antecedent
				String additional = indicatedStates.get(x).getVarState();
				if(paramPerms.isInState(additional) == false)
					return "Return value indicates " + x + " to be in state " + additional + " but it is in " + paramPerms.getStateInfo();
			}

			FractionalPermissions paramRemainder = paramPerms.splitOff(paramPost.get(x));

			if(paramRemainder.isUnsatisfiable())
				return x + " returns no suitable permissions for post-condition";
			
			/*
			 * Either way, the rest goes back into the lattice.
			 */
			tuple.put(x, paramRemainder);
		}
		
		// 2. check result post-condition
		return checkResultPermissionIfNotNull(node, tuple, resultLoc, resultPost);
	}

	/**
	 * Tests a given permission, if the variable is non-<code>null</code>.
	 * @param node
	 * @param value
	 * @param resultLoc Must be non-<code>null</code> if <code>permissionSet</code> is.
	 * The variable is assumed to represent a result (for error messages).
	 * @param permissionSet
	 * @return An error message or <code>null</code> if no errors were found.
	 */
	private String checkResultPermissionIfNotNull(
			final ASTNode node,
			final TensorPluralTupleLE value,
			final Aliasing resultLoc, 
			final PermissionSetFromAnnotations permissionSet) {
		if(permissionSet == null || permissionSet.isEmpty())
			return null;
		if(resultLoc == null)
			throw new NullPointerException();
		
		// If the return value is not the value null, then it must fulfill its spec.
		if( !value.isNull(resultLoc) ) {
			FractionalPermissions resultPerms = value.get(resultLoc);
			
			for(String needed : permissionSet.getStateInfo(false)) {
				if(resultPerms.isInState(needed, false) == false)
					return "Return value must be in state " + 
							needed + " but is in " + resultPerms.getStateInfo(false);
			}
			
			for(String needed : permissionSet.getStateInfo(true)) {
				if(resultPerms.isInState(needed, true) == false)
					return "Return value frame must be in state " + 
							needed + " but is in " + resultPerms.getStateInfo(true);
			}
			
			FractionalPermissions resultRemainder = resultPerms.splitOff(permissionSet);
			if(resultRemainder.isUnsatisfiable())
				return "Return value carries no suitable permission";
		}
		return null; // no error
	}

}