/**
 * Copyright (C) 2007-2009 Carnegie Mellon University and others.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.alias.AliasAwareTupleLE;
import edu.cmu.cs.plural.concrete.DynamicStateLogic;
import edu.cmu.cs.plural.errors.ChoiceID;
import edu.cmu.cs.plural.errors.FailedPack;
import edu.cmu.cs.plural.errors.PackingResult;
import edu.cmu.cs.plural.fractions.FractionalPermission;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.pred.DefaultInvariantChecker;
import edu.cmu.cs.plural.pred.DefaultInvariantMerger;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.pred.PredicateChecker.SplitOffTuple;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.states.StateSpaceRepository;
import edu.cmu.cs.plural.states.annowrappers.ClassStateDeclAnnotation;
import edu.cmu.cs.plural.states.annowrappers.StateDeclAnnotation;
import edu.cmu.cs.plural.track.FractionAnalysisContext;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;

/**
 * Modifies PluralTupleLatticeElement to take advantage of support for multiple
 * contexts in DisjunctiveLE during packing and unpacking.
 * 
 * @author Kevin Bierhoff
 * @since 4/16/2008
 */
public class TensorPluralTupleLE extends PluralTupleLatticeElement {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(TensorPluralTupleLE.class.getName());

	/**
	 * Creates a TensorPluralTupleLE suitable for use by constructors.
	 * Constructors are different in that they are initialized to be unpacked.
	 * We have to get around the strange problem of assigning the 'unpackedVar'
	 * variable and getting aliasing information at the same time.
	 * @param bottom
	 * @param annotationDatabase
	 * @param repository
	 * @return
	 * @see PluralTupleLatticeElement#createConstructorLattice(FractionalPermissions, AnnotationDatabase, StateSpaceRepository, ThisVariable, MethodDeclaration)
	 */
	public static TensorPluralTupleLE createUnpackedLattice(
			FractionalPermissions fps,
			FractionAnalysisContext context,
			ThisVariable thisVar, 
			MethodDeclaration decl) {
		// This seems to violate the mostly-functional spirit of this class, but
		// I am not sure how else to do this.
		TensorPluralTupleLE tuple = new TensorPluralTupleLE(fps, context, thisVar, decl);
		return tuple;
	}

	/** Cached unsatisfiable flag; <code>null</code> if unknown. */
	private Boolean unsatisfiable;
	
	public TensorPluralTupleLE(FractionalPermissions b,
			FractionAnalysisContext context) {
		super(b, context);
	}

	protected TensorPluralTupleLE(AliasAwareTupleLE<FractionalPermissions> a,
			FractionAnalysisContext context,
			Variable unpackedVar, ASTNode nodeWhereUnpacked, DynamicStateLogic dsl) {
		super(a, context, unpackedVar, nodeWhereUnpacked, dsl);
	}

	protected TensorPluralTupleLE(FractionalPermissions b,
			FractionAnalysisContext context,
			Variable unpackedVar, ASTNode nodeWhereUnpacked) {
		super(b, context, unpackedVar, nodeWhereUnpacked);
	}

	@Override
	protected TensorPluralTupleLE create(
			AliasAwareTupleLE<FractionalPermissions> a, FractionAnalysisContext context, 
			Variable unpackedVar,
			ASTNode nodeWhereUnpacked, DynamicStateLogic dsl) {
		return new TensorPluralTupleLE(a, context, unpackedVar, nodeWhereUnpacked, dsl);
	}

	@Override
	public TensorPluralTupleLE mutableCopy() {
		// super.mutableCopy() calls create(), which we override
		return (TensorPluralTupleLE) super.mutableCopy();
	}

	public TensorPluralTupleLE join(TensorContext this_context, 
			TensorContext other_context,
			ASTNode node) {
		// super.join() calls create(), which we override
		return (TensorPluralTupleLE) super.join(this_context, other_context, node);
	}

	/**
	 * @deprecated Use {@link #fancyPackReceiverToBestGuess(ThisVariable, StateSpaceRepository, SimpleMap, String...)}.
	 */
	@Deprecated
	@Override
	public boolean packReceiverToBestGuess(Variable rcvrVar,
			StateSpaceRepository stateRepo, SimpleMap<Variable, Aliasing> locs,
			String... statesToTry) {
		return super.packReceiverToBestGuess(rcvrVar, stateRepo, locs, statesToTry);
	}

	/**
	 * @deprecated Use {@link #fancyUnpackReceiver(Variable, ASTNode, StateSpaceRepository, SimpleMap, String, String)}.
	 */
	@Override
	@Deprecated
	public boolean unpackReceiver(Variable rcvrVar,
			ASTNode nodeWhereUnpacked, StateSpaceRepository stateRepo,
			SimpleMap<Variable, Aliasing> locs, String rcvrRoot, String assignedField) {
		return super.unpackReceiver(rcvrVar, nodeWhereUnpacked, stateRepo, locs, rcvrRoot, assignedField);
	}
	
	/**
	 * Implements the lattice manipulations for unpacking a variable at a particular
	 * node, i.e. it injects field permissions and marks the receiver as unpacked;
	 * {@link #fancyUnpackReceiver(Variable, ASTNode, StateSpaceRepository, SimpleMap, String, String)}
	 * does not call this method if there are no permissions available for unpacking.
	 * Trivially succeeds if the unpacked variable is associated with "bottom"
	 * or there are no frame permissions available for unpacking.  
	 * Returns <code>false</code> when the unpacked invariant contains void.
	 * Notice that this method does not fail if there are no (suitable) permissions 
	 * for unpacking available; failures would occur when fields are used,
	 * but in practice we don't even call this method in this case.
	 * If a field is assigned then the unpacked permission is forced to be modifiable
	 * and permissions for the old location assicated with the assigned field are not 
	 * overridden.
	 * @param rcvrVar
	 * @param nodeWhereUnpacked
	 * @param stateRepo
	 * @param locs
	 * @param rcvrRoot
	 * @param assignedField Assigned field or <code>null</code> if this unpack
	 * happens for reading a field.
	 * @return <code>false</code> if <b>void</b> was unpacked; 
	 * <code>true</code> otherwise.
	 */
	private boolean unpackReceiverInternal(Variable rcvrVar, ASTNode nodeWhereUnpacked, StateSpaceRepository stateRepo,
			final SimpleMap<Variable, Aliasing> locs, String rcvrRoot, final String assignedField) {

		if( isFrozen() )
			throw new IllegalStateException("Object is frozen.");

		if( !isRcvrPacked() )
			throw new IllegalStateException("Double unpack on the receiver. Not cool.");
		
		// 1.) Find out what state the receiver is in.
		Aliasing rcvrLoc = locs.get(rcvrVar);
		FractionalPermissions this_perms = this.get(rcvrLoc);
		if(this_perms.isBottom() || this_perms.getFramePermissions().isEmpty())
			// trivially succeed but no field permissions injected
			// (no point for bottom; will create problems when fields are used)
			// TODO enforce permission present to access fields: could fail if no frame perms 
			return true;
		this_perms = this_perms.unpack(rcvrRoot);
		if(assignedField != null)
			// assignment -> make sure we're unpacking a modifiable permission
			this_perms = this_perms.makeUnpackedPermissionModifiable();

		// 2.) Add resulting receiver permission.
		this.put(rcvrLoc, this_perms);
		this.setUnpackedVar(rcvrVar);
		this.setNodeWhereUnpacked(nodeWhereUnpacked);
		
		final ITypeBinding class_decl = rcvrVar.resolveType();
		final FractionalPermission unpacked_perm = this_perms.getUnpackedPermission();
		final SimpleMap<String,StateSpace> field_spaces = getFieldAndObjStateSpaces(class_decl, stateRepo);
		
		boolean result = true; // no void seen yet
		for( Pair<String,String> state_and_inv : getStatesAndInvs(class_decl, unpacked_perm, getAnnotationDB()) ) {
			// foreach invariant string:
			final String inv = state_and_inv.snd();
		
			// Parse the invariant string, creating a PredicateMerger object
			final Pair<PredicateMerger,?> parsed =
				PermParser.parseInvariant(inv, field_spaces);
			
			// Create a call-back for this state that will purify as necessary
			final boolean purify = this_perms.getUnpackedPermission().isReadOnly();
			final DefaultInvariantMerger callback = 
				new DefaultInvariantMerger(nodeWhereUnpacked, this, assignedField, purify); 
			
			final SimpleMap<String,Aliasing> locs_ =
				createFieldNameToAliasingMapping(locs, class_decl, rcvrLoc);
			
			// Call merge-in...
			parsed.fst().mergeInPredicate(locs_, callback);
			if(callback.isVoid()) {
				// got void from invariant
				result = false;
			}
		}
		
		return result;
	}
	
	@Override
	public PackingResult packReceiver(TensorContext curContext,
			Variable rcvrVar, StateSpaceRepository stateRepo,
			SimpleMap<Variable, Aliasing> locs, Set<String> desiredState) {
		
//		if( curContext.getTuple() != this )
//			throw new IllegalArgumentException("Attached TensorContext must be the context associated with this.");
		
		if( isFrozen() || isRcvrPacked() )
			throw new IllegalStateException("Object cannot be packed or is frozen.");
		
		final Aliasing rcvrLoc = locs.get(rcvrVar);		
		final ITypeBinding rcvr_type = rcvrVar.resolveType();
		
		// Create the new receiver permission.
		FractionalPermissions rcvr_perms = this.get(rcvrLoc);
		FractionalPermission new_rcvr_perm = 
			rcvr_perms.getUnpackedPermission().copyNewState(desiredState);

		// Get state spaces for field types
		final SimpleMap<String,StateSpace> field_spaces = getFieldAndObjStateSpaces(rcvr_type, stateRepo);
		
		for( Pair<String,String> state_and_inv : getStatesAndInvs(rcvr_type, new_rcvr_perm, getAnnotationDB()) ) {
			// foreach invariant string
			final String inv = state_and_inv.snd();
		
			// Parse the invariant string, creating a PredicateMerger object
			final Pair<?,PredicateChecker> parsed =
				PermParser.parseInvariant(inv, field_spaces);

			// Create a call-back for this state that will purify as necessary
			final boolean purify = new_rcvr_perm.isReadOnly();
			
			final SimpleMap<String,Aliasing> locs_ =
				createFieldNameToAliasingMapping(locs, rcvr_type, rcvrLoc);
			
			final SplitOffTuple callback =
				new DefaultInvariantChecker(curContext, rcvrLoc, purify);
			
			// Call splitOff to see if we have enough permission
			boolean split_worked = 
				parsed.snd().splitOffPredicate(locs_, callback);
			
			if( !split_worked ) {
				// If the pack didn't work, we poison the lattice
				put(rcvrLoc, rcvr_perms.invalidPack());
				setUnpackedVar(null);
				setNodeWhereUnpacked(null);
				
				String state = state_and_inv.fst();
				return FailedPack.fail(state, inv);	
			}
		}
		
		// 3. pack the receiver permissions object, put new results back in.
		rcvr_perms = rcvr_perms.pack(desiredState);
		put(rcvrLoc, rcvr_perms);
		setUnpackedVar(null);
		setNodeWhereUnpacked(null);
		
		return PackingResult.success;
	}
	
	/**
	 * Given a class, create a mapping from field name to state space.
	 * Includes 'this.' The state space comes from the type of each field.
	 */
	private static SimpleMap<String, StateSpace> getFieldAndObjStateSpaces(
			final ITypeBinding class_decl, final StateSpaceRepository stateRepo) {
		// Build field mapping.
		final Map<String, IVariableBinding> fields = createFieldNameToBindingMapping(class_decl);
//		final StateSpace this_space = stateRepo.getStateSpace(class_decl);
		
		return new SimpleMap<String,StateSpace>() {
			@Override public StateSpace get(String key) {
				if( "this".equals(key) || "this!fr".equals(key) )
					return stateRepo.getStateSpace(class_decl);
				else if ("super".equals(key))
					return stateRepo.getStateSpace(class_decl.getSuperclass());
				else
					return stateRepo.getStateSpace(fields.get(key).getType());
			}};
	}

	/**
	 * Given a class and the permission that we are unpacking, return
	 * a list of pairs of state names (that apply) and the invariant
	 * strings those states define.
	 */
	private static Iterable<? extends Pair<String,String>> 
	getStatesAndInvs(ITypeBinding class_decl,
			FractionalPermission unpacked_perm, AnnotationDatabase annoDB) {
		List<Pair<String,String>> result = new LinkedList<Pair<String,String>>();
		
		for( ICrystalAnnotation csda : annoDB.getAnnosForType(class_decl)) {
			if( csda instanceof ClassStateDeclAnnotation ) {
				final List<StateDeclAnnotation> decls = 
					((ClassStateDeclAnnotation)csda).getStates();

				for( StateDeclAnnotation decl : decls ) {
					/*
					 * Is this declaration applicable to the current recvr state?
					 * We want the annotation state to be between the this state and
					 * the this root state, inclusive.
					 */
					final boolean applies =
						unpacked_perm.impliesState(decl.getStateName()) &&
								(unpacked_perm.coversNode(decl.getStateName()) || 
										unpacked_perm.getStateSpace().firstBiggerThanSecond(decl.getStateName(), unpacked_perm.getRootNode()));
					
					if( applies ) {
						String state = decl.getStateName();
						String inv = decl.getInv();
						result.add(Pair.create(state, inv));
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * This is the disjunctive version of 
	 * {@link PluralTupleLatticeElement#packReceiverToBestGuess(ThisVariable, StateSpaceRepository, SimpleMap, String...)}.
	 * Tries all given states and returns the successful packs.
	 * @param rcvrVar
	 * @param stateRepo
	 * @param locs
	 * @param statesToTry
	 * @return
	 */
	public LinearContext fancyPackReceiverToBestGuess(TensorContext curContext, ThisVariable rcvrVar,
			StateSpaceRepository stateRepo, SimpleMap<Variable, Aliasing> locs,
			ChoiceID parentID, String... statesToTry) {
		// need not be frozen because we create mutable copies...
		
//		if( curContext.getTuple() != this )
//			throw new IllegalArgumentException("TensorContext must be context associated with this.");
		
		if( isRcvrPacked() )
			throw new IllegalStateException("Double pack on the receiver. Not cool.");

		final Aliasing rcvrLoc = locs.get(rcvrVar);	
		FractionalPermission unpacked_permission =
			this.get(rcvrLoc).getUnpackedPermission();

		// Add all the states we want to try.		
		Set<String> statesWorthTrying = new LinkedHashSet<String>(2+statesToTry.length);
		for(String n : statesToTry) {
			if(unpacked_permission.coversNode(n))
				statesWorthTrying.add(n);
			// otherwise don't bother trying
		}
		statesWorthTrying.addAll(unpacked_permission.getStateInfo());
		// add in all nodes we could possibly pack to
		// TODO we could completely ignore statesToTry
		for(String n : unpacked_permission.getStateSpace().getAllNodes()) {
			if(unpacked_permission.coversNode(n))
				statesWorthTrying.add(n);
		}

		// try packing to each state; discard unsuccessful attempts
		// TODO try state combinations for states from different dimensions
		LinkedHashSet<LinearContext> resultElems = new LinkedHashSet<LinearContext>();
		for(String n : statesWorthTrying) {
			TensorContext elem_ctx = curContext.mutableCopy();
			TensorPluralTupleLE elem = elem_ctx.getTuple();
			elem.storeIdenticalAliasInfo(this);
			PackingResult pack_result = elem.packReceiver(elem_ctx, rcvrVar, stateRepo, locs, Collections.singleton(n));
			if(pack_result.worked()) {
				// could check for satisfiability here
				// New choice ID for each packed direction.
				resultElems.add(TensorContext.tensor(elem, parentID, 
						ChoiceID.choiceID("Choosing to pack to " + n)));
			}
		}
		return ContextChoiceLE.choice(resultElems);
	}

	/**
	 * This is the disjunctive version of 
	 * {@link PluralTupleLatticeElement#unpackReceiver(Variable, ASTNode, StateSpaceRepository, SimpleMap, String, String)}.
	 * If this is an assignment, then receiver is forced to have a modifiable permission.
	 * @tag usage.restriction: no unpacking is necessary for unmapped nodes, so this shouldn't be called in that case. 
	 * @param rcvrVar
	 * @param nodeWhereUnpacked Node where this unpack happens
	 * @param stateRepo
	 * @param locs
	 * @param rcvrRoot
	 * @param assignedField Name of the field being assigned, 
	 * or <code>null</code> if the unpack happens for a field read
	 * @param includeOriginal set to <code>true</code> to include
	 * the receiver tuple as-is in the result.
	 * @param parentChoiceID The id of the parent of this choice.
	 * @param choiceID The id of this choice.
	 * @return The resulting lattice value, possibly a disjunction of different
	 * states we tried to unpack from.
	 */
	public LinearContext fancyUnpackReceiver(Variable rcvrVar,
			ASTNode nodeWhereUnpacked, StateSpaceRepository stateRepo,
			SimpleMap<Variable, Aliasing> locs, String rcvrRoot, 
			String assignedField, boolean includeOriginal, 
			ChoiceID parentChoiceID, ChoiceID choiceID) {
		// this need not be unfrozen because we create mutable copies...

		LinkedHashSet<LinearContext> resultElems = new LinkedHashSet<LinearContext>();
		if(includeOriginal) {
			// Every choice in the tuple starts a new choice path
			resultElems.add(ContextFactory.tensor(this, choiceID, 
					ChoiceID.choiceID("Choosing to not unpack.")));
		}
		
		StateSpace rcvr_space = stateRepo.getStateSpace(rcvrVar.resolveType());
		Aliasing rcvr_loc = locs.get(rcvrVar);
		assert rcvr_loc != null;

		FractionalPermissions rcvrPerms = get(rcvr_loc);
		if(rcvrPerms.isBottom()) {
			// receiver is bottom
			// trivially succeed, but no injected field permissions
			return ContextFactory.tensor(this, parentChoiceID, choiceID);
		}
		else if(rcvrPerms.getFramePermissions().isEmpty()) {
			// fail: must have permission for unpacking
			return includeOriginal ?
					ContextFactory.tensor(this, parentChoiceID, choiceID) :
					ContextFactory.trueContext(parentChoiceID, choiceID);
		}
		// the above should just be an optimization for below
		else {
			// try all states implied by receiver's state info inside the requested root
			Set<String> tried_nodes = new HashSet<String>();
//			FractionAssignment a = rcvrPerms.getConstraints().simplify();
			
			List<String> infoToTry = new LinkedList<String>();
			infoToTry.add(rcvrRoot); // try root node (and bigger) first
			if(assignedField == null)
				// try smaller unpacking roots *if this is not an assignment*
				// (otherwise only try rcvrRoot and bigger)
				infoToTry.addAll(rcvrPerms.getStateInfo(true));
			for(String n : infoToTry) {
				if(rcvr_space.firstBiggerThanSecond(rcvrRoot, n) == false)
					continue;
				state_iter:
				for(Iterator<String> it = rcvr_space.stateIterator(n); it.hasNext(); ) {
					String try_node = it.next();
					if(tried_nodes.add(try_node) == false)
						// node already tried --> skip
						continue state_iter;
					
					TensorPluralTupleLE elem = this.mutableCopy();
					elem.storeIdenticalAliasInfo(this);
					if(!elem.unpackReceiverInternal(rcvrVar, nodeWhereUnpacked, stateRepo, locs, try_node, assignedField))
						// tried to unpack to a state with VOID invariant
						// anything is possible after that... 
						return ContextFactory.falseContext(parentChoiceID, choiceID);
					if(elem.get(rcvr_loc).getConstraints().seemsConsistent()) {
						// could check for satisfiability here
						// A new choice path starts here!
						resultElems.add(TensorContext.tensor(elem, choiceID, 
								ChoiceID.choiceID("Choosing to unpack to root " + try_node)));
					}
				}
			}
			return ContextChoiceLE.choice(resultElems);
		}
	}
	
	/**
	 * Tests whether there is an element in the tuple that is unsatisfiable.
	 * @return <code>true</code> if there is an unsatisfiable element in the
	 * tuple; <code>false</code> otherwise.
	 * @see FractionalPermissions#isUnsatisfiable()
	 */
	public boolean isUnsatisfiable() {
		if(unsatisfiable != null)
			return unsatisfiable;
		if(isFrozen()) {
			unsatisfiable = isUnsatInternal();
			return unsatisfiable;
		}
		else
			return isUnsatInternal();
	}
	
	private boolean isUnsatInternal() {
		for(FractionalPermissions p : getTupleLatticeElement()) {
			if(p.isUnsatisfiable())
				return true;
		}
		return false;
	}
}