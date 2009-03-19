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
package edu.cmu.cs.plural.track;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;

import edu.cmu.cs.crystal.analysis.alias.AliasLE;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.analysis.alias.ObjectLabel;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.simple.LatticeElement;
import edu.cmu.cs.crystal.tac.SourceVariable;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.TempVariable;
import edu.cmu.cs.crystal.tac.ThisVariable;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.crystal.util.ExtendedIterator;
import edu.cmu.cs.crystal.util.Freezable;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.alias.AliasAwareTupleLE;
import edu.cmu.cs.plural.alias.AliasingLE;
import edu.cmu.cs.plural.alias.FrameLabel;
import edu.cmu.cs.plural.alias.ParamVariable;
import edu.cmu.cs.plural.alias.AliasAwareTupleLE.LabelFilter;
import edu.cmu.cs.plural.concrete.ConcreteAnnotationUtils;
import edu.cmu.cs.plural.concrete.DynamicStateLogic;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.concrete.ImplicationResult;
import edu.cmu.cs.plural.concrete.DynamicStateLogic.AliasingFilter;
import edu.cmu.cs.plural.fractions.AbstractFractionalPermission;
import edu.cmu.cs.plural.fractions.FractionConstraints;
import edu.cmu.cs.plural.fractions.FractionalPermission;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.perm.parser.ReleaseHolder;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.states.StateSpaceRepository;
import edu.cmu.cs.plural.states.annowrappers.ClassStateDeclAnnotation;
import edu.cmu.cs.plural.states.annowrappers.StateDeclAnnotation;


/**
 * Subclass of TupleLatticeElement that includes anything we need specifically
 * for the Plural protocol analysis.
 * 
 * Examples include facilities for keeping track of dynamic state tests.
 * 
 * @author Nels Beckman
 * @date Feb 13, 2008
 *
 * @param <K>
 * @param <LE>
 */
public class PluralTupleLatticeElement  
implements LatticeElement<PluralTupleLatticeElement>,
Freezable<PluralTupleLatticeElement>, PluralLatticeElement {

	/**
	 * @author Kevin Bierhoff
	 * @since 8/13/2008
	 */
	public interface VariableLiveness {

		/**
		 * @param x
		 * @return
		 */
		boolean isLive(Variable x);

	}

	final private DynamicStateLogic dynamicStateLogic;
	final private AliasAwareTupleLE<FractionalPermissions> tupleLatticeElement;
	final private FractionAnalysisContext context;
	
	private static final Logger log = Logger.getLogger(FractionalTransfer.class.getName());
	
	/**
	 * Really only needs to be checked by local functionality, since both 
	 * DynamicStateLogic and AliasAwareTupleLE are freezable.
	 */
	private boolean isFrozen;
	// Holds the most recent aliasing state of the world.
	private AliasingLE mostRecentAliasInfo = null;
	
	/** Call for no unpacked var. */
	public PluralTupleLatticeElement(FractionalPermissions b, 
			FractionAnalysisContext context) {
		this(b, context, null, null);
	}
	
	/** Call for an unpacked var, and a new DynamicStateLogic. */
	protected PluralTupleLatticeElement(FractionalPermissions b, 
			FractionAnalysisContext context,
			Variable unpackedVar, ASTNode nodeWhereUnpacked) {
		this.tupleLatticeElement = AliasAwareTupleLE.create(context, b);
		this.dynamicStateLogic = new DynamicStateLogic();
		this.context = context;
		this.isFrozen = false;
		this.unpackedVar = unpackedVar;
		this.nodeWhereUnpacked = nodeWhereUnpacked;
	}

	/** Call to specify everything. */
	protected PluralTupleLatticeElement(AliasAwareTupleLE<FractionalPermissions> a, 
			FractionAnalysisContext context, Variable unpackedVar,
			ASTNode nodeWhereUnpacked, DynamicStateLogic dsl) {
		this.tupleLatticeElement = a;
		this.dynamicStateLogic = dsl;
		this.context = context;
		this.isFrozen = false;
		this.unpackedVar = unpackedVar;
		this.nodeWhereUnpacked = nodeWhereUnpacked;
	}
	
	/**
	 * Creates a PluralTupleLatticeElement suitable for use by constructors.
	 * Constructors are different in that they are initialized to be unpacked.
	 * We have to get around the strange problem of assigning the 'unpackedVar'
	 * variable and getting aliasing information at the same time.
	 * @param bottom
	 * @param annotationDatabase
	 * @param repository
	 * @return
	 */
	public static PluralTupleLatticeElement createConstructorLattice(
			FractionalPermissions fps,
			FractionAnalysisContext context,
			ThisVariable thisVar, MethodDeclaration decl) {
		// This seems to violate the mostly-functional spirit of this class, but
		// I am not sure how else to do this.
		PluralTupleLatticeElement tuple = new PluralTupleLatticeElement(
				fps, context, thisVar, decl);
		return tuple;
	}
	
	protected PluralTupleLatticeElement create(
			AliasAwareTupleLE<FractionalPermissions> a, FractionAnalysisContext context,
			Variable unpackedVar, ASTNode nodeWhereUnpacked,
			DynamicStateLogic dsl) {
		return new PluralTupleLatticeElement(
				a, context, unpackedVar, nodeWhereUnpacked, dsl);
	}

	public boolean isNull(Aliasing loc) {
		return dynamicStateLogic.isNull(loc);
	}
	
	public boolean isNonNull(Aliasing loc) {
		return dynamicStateLogic.isNonNull(loc);
	}
	
	public boolean isBooleanTrue(Aliasing var) {
		return dynamicStateLogic.isBooleanTrue(var);
	}
	
	public boolean isBooleanFalse(Aliasing var) {
		return dynamicStateLogic.isBooleanFalse(var);
	}
	
	public void addFalseImplication(Aliasing ant, Aliasing object, String state) {
		dynamicStateLogic.addFalseImplication(ant, object, state);
	}

	public void addFalseVarPredicate(Aliasing v) {
		dynamicStateLogic.addFalseVarPredicate(v);
	}

	public void addNonNullVariable(Aliasing var) {
		dynamicStateLogic.addNonNullVariable(var);
	}
	
	public void addNullVariable(Aliasing var) {
		dynamicStateLogic.addNullVariable(var);
		tupleLatticeElement.put(var, FractionalPermissions.bottom());
	}
	
	public void addNullImplication(Aliasing v_1, boolean is_v1_true,
			Aliasing v_2, boolean is_v2_null) {
		dynamicStateLogic.addNullImplication(v_1, is_v1_true, v_2, is_v2_null);
	}

	public void addTrueImplication(Aliasing ant, Aliasing object, String state) {
		dynamicStateLogic.addTrueImplication(ant, object, state);
	}

	public void addTrueVarPredicate(Aliasing v) {
		dynamicStateLogic.addTrueVarPredicate(v);
	}

	public void addEquality(Aliasing v_1, Aliasing v_2) {
		dynamicStateLogic.addEquality(v_1, v_2);
	}

	public void addInequality(Aliasing v_1, Aliasing v_2) {
		dynamicStateLogic.addInequality(v_1, v_2);
	}

	public List<ImplicationResult> solve() {
		return dynamicStateLogic.solve(this);
	}

	public List<ImplicationResult> solveWithHint(Aliasing v) {
		return dynamicStateLogic.solveWithHint(this, v);
	}

	public List<ImplicationResult> solveWithHints(Aliasing... vs) {
		return dynamicStateLogic.solveWithHints(this, vs);
	}
	
	public boolean isKnownImplication(Aliasing v,
			Implication implication) {
		return dynamicStateLogic.isKnownImplication(v, implication);
	}
	
	public void addImplication(Aliasing var, Implication impl) {
		if(impl.getAntecedant().isAlwaysTrue()) {
			// antecedent is "true" -> apply implication right away
			impl.result().putResultIntoLattice(this);
		}
		else {
			dynamicStateLogic.addImplication(var, impl);
		}
	}
	
	public void removeImplication(Aliasing var, Implication impl) {
		dynamicStateLogic.removeImplication(var, impl);
	}

	/**
	 * @param loc
	 * @return
	 */
	public List<ReleaseHolder> findImpliedParameter(Aliasing anteLoc, Aliasing paramLoc) {
		return dynamicStateLogic.findImpliedParameter(anteLoc, paramLoc);
	}

	/**
	 * Calling this method signals to the object that it is a good time
	 * to record the current aliasing information. The object will store
	 * that information. Note that this method, for convenience only, returns
	 * 'this.' This is the mutated object.
	 * 
	 * @param ASTNode The current location where we are in the AST, needed
	 *                to find the 'current' aliasing information.
	 * @return <code>this</code>, the mutated object, for convenience.
	 */
	public PluralTupleLatticeElement storeCurrentAliasingInfo(ASTNode n) {
		this.mostRecentAliasInfo = this.tupleLatticeElement.getLocationsAfter(n);
		return this;
	}
	/**
	 * Store the locations at the very beginning.
	 * 
	 * @param decl
	 * @return
	 * 
	 * @see edu.cmu.cs.plural.track.PluralTupleLatticeElement.storeCurrentAliasingInfo
	 */
	public PluralTupleLatticeElement storeInitialAliasingInfo(MethodDeclaration decl) {
		this.mostRecentAliasInfo = this.tupleLatticeElement.getStartLocations(decl);
		return this;
	}
	
	/**
	 * Use this with care!  Copies aliasing info from orig to this.  Only
	 * use this if you know that the aliasing info is the same.  
	 * @param orig
	 */
	protected void storeIdenticalAliasInfo(final PluralTupleLatticeElement orig) {
		this.mostRecentAliasInfo = orig.mostRecentAliasInfo;
	}

	@Override
	public PluralTupleLatticeElement freeze() {
		assert(this.mostRecentAliasInfo != null);
		dynamicStateLogic.freeze();
		tupleLatticeElement.freeze();
		this.isFrozen = true;
		return this;
	}

	@Override
	public PluralTupleLatticeElement copy() {
		return freeze();
	}

	/**
	 * Joins use & for unpackedVar, because we want to stay packed only if both lattices
	 * say we must be packed. Will converge to unpacked.
	 * 
	 * @param other
	 * @param node Can be null.
	 */
	public PluralTupleLatticeElement join(PluralTupleLatticeElement other, ASTNode node) {
		assert(this.mostRecentAliasInfo != null && other.mostRecentAliasInfo != null);
		
		this.freeze();
		if(this == other)
			return this;
		other.freeze();
		
		PluralTupleLatticeElement this_copy = this.mutableCopy();
		// This is getting uglier and uglier...
		this_copy.mostRecentAliasInfo = this.mostRecentAliasInfo;
		
		// we need not pack if either
		// (a) both this_copy and other are packed or
		// (b) both this_copy and other are unpacked to the same permission
		// the following line tests (a)
		boolean needNotPack = this_copy.isRcvrPacked() && other.isRcvrPacked();
		
		if(! this_copy.isRcvrPacked() && ! other.isRcvrPacked() ) {
			// bad: may need to pack both
			final Variable this_var = this_copy.getUnpackedVar();
			final Variable other_var = other.getUnpackedVar();
			final FractionalPermissions this_perms = this_copy.get(this_copy.mostRecentAliasInfo.get(this_var));
			final FractionalPermissions other_perms = other.get(other.mostRecentAliasInfo.get(other_var));
			final FractionalPermission this_unpacked = this_perms.getUnpackedPermission();
			final FractionalPermission other_unpacked = other_perms.getUnpackedPermission();

			// test condition (b) for not having to pack
			// need to pack if the two unpacked permissions are not equal
			final FractionConstraints concatConstraints = this_perms.getConstraints().concat(other_perms.getConstraints());
			needNotPack = this_unpacked.atLeastAsPrecise(
						other_unpacked, 
						node, 
						concatConstraints) &&
					other_unpacked.atLeastAsPrecise(
							this_unpacked, 
							node, 
							concatConstraints);
			if(! needNotPack) {
				final String this_root = this_unpacked.getRootNode();
				final String other_root = other_unpacked.getRootNode();
				
				if(this_perms.getStateSpace().areOrthogonal(this_root, other_root)) {
					// pack like when only one incoming tuple is unpacked:
					// use state info from one tuple to guide packing of the other
					
					// do nothing here:
					// let the code for one unpacked permission handle this case
				}
				else {
					// now we're really in trouble
					// one unpacked root is the parent of the other
					// we have to guess what states to pack to for the "smaller" unpacked permission
					final PluralTupleLatticeElement first;
					boolean first_is_this = this_perms.getStateSpace().firstBiggerThanSecond(other_root, this_root);
					if(first_is_this) {
						// We need a mutable version b/c we have to pack, but we want it to keep the same aliasing.
						first = this_copy.mutableCopy();  // Nels: do we really copy again???
						first.mostRecentAliasInfo = this_copy.mostRecentAliasInfo;
						this_copy = first; // replace original with copy
					}
					else {
						// We need a mutable version b/c we have to pack, but we want it to keep the same aliasing.
						first = other.mutableCopy();
						first.mostRecentAliasInfo = other.mostRecentAliasInfo;
						other = first; // replace original with copy
					}
					final AliasingLE first_locs = first.mostRecentAliasInfo;

					if(first.packReceiverToBestGuess(first.getUnpackedVar(), first.getStateRepo(), 
							new SimpleMap<Variable, Aliasing>() {
						@Override
						public Aliasing get(Variable key) {
							return first_locs.get(key);
						}} /* have no states to try */)) {
						// pack second according to states that first was packed to
						
						// do nothing here:
						// let code for unpacked lattice handle the rest
					}
					else {
						// first couldn't be packed--try packing second anyway and move on
						final PluralTupleLatticeElement second;
						if(first_is_this) {
							second = other.mutableCopy();
							second.mostRecentAliasInfo = other.mostRecentAliasInfo;
							other = second; // replace original with copy
						}
						else {
							second = this_copy.mutableCopy();  // Nels: do we really copy again???
							second.mostRecentAliasInfo = this_copy.mostRecentAliasInfo;
							this_copy = second; // replace original with copy
						}
						final AliasingLE second_locs = second.mostRecentAliasInfo;
						
						second.packReceiverToBestGuess(second.getUnpackedVar(), second.getStateRepo(), 
								new SimpleMap<Variable, Aliasing>() {
							@Override
							public Aliasing get(Variable key) {
								return second_locs.get(key);
							}} /* have no states to try */);
					}
				}
			}
		}
		
		if( ! needNotPack ) {
			// must pack before we try to join.
			// invariant at this point: either
			// (a) only one of this_copy and other are unpacked or
			// (b) both this_copy and other are unpacked, and their unpacked permissions are orthogonal 
			// this is important because we use the state info from one permission to pack the other
			
			if( ! other.isRcvrPacked() ) {
				Variable unpacked_var = other.unpackedVar;
				Aliasing unpacked_loc = other.mostRecentAliasInfo.get(unpacked_var);
				Aliasing unpacked_loc_this = this.mostRecentAliasInfo.get(unpacked_var);

				FractionalPermissions packed_perms = this_copy.get(unpacked_loc_this);
				String unpacked_root = other.get(unpacked_loc).getUnpackedPermission().getRootNode();
				// test invariant
				assert packed_perms.getUnpackedPermission() == null || 
						packed_perms.getStateSpace().areOrthogonal(packed_perms.getUnpackedPermission().getRootNode(), unpacked_root);
				Set<String> states_to_pack_to = AbstractFractionalPermission.filterStateInfo(
						packed_perms.getStateSpace(), packed_perms.getStateInfo(true), unpacked_root);
				
				// We need a mutable version b/c we have to pack, but we want it to keep the same aliasing.
				final AliasingLE locs = other.mostRecentAliasInfo;
				other = other.mutableCopy();
				other.mostRecentAliasInfo = locs;
				
				// create map for variable locations.
				other.packReceiver(unpacked_var, other.getStateRepo(), 
						new SimpleMap<Variable,Aliasing>() {
					@Override
					public Aliasing get(Variable key) {
						return locs.get(key);
					}},
					states_to_pack_to);
			}
			
			if( ! this_copy.isRcvrPacked() ){
				Variable unpacked_var = this_copy.unpackedVar;
				Aliasing unpacked_loc = this_copy.mostRecentAliasInfo.get(unpacked_var);
				Aliasing unpacked_loc_other = other.mostRecentAliasInfo.get(unpacked_var);

				FractionalPermissions packed_perms = other.get(unpacked_loc_other);
				String unpacked_root = this_copy.get(unpacked_loc).getUnpackedPermission().getRootNode();
				// test invariant
				assert packed_perms.getUnpackedPermission() == null || 
						packed_perms.getStateSpace().areOrthogonal(packed_perms.getUnpackedPermission().getRootNode(), unpacked_root);
				Set<String> states_to_pack_to = AbstractFractionalPermission.filterStateInfo(
						packed_perms.getStateSpace(), packed_perms.getStateInfo(true), unpacked_root);
				
				// We need a mutable version b/c we have to pack, but we want it to keep the same aliasing.
				final AliasingLE locs = this.mostRecentAliasInfo;
				this_copy = this_copy.mutableCopy();   // Nels: do we really copy again???
				
				// create map for variable locations
				this_copy.packReceiver(unpacked_var, this_copy.getStateRepo(),
						new SimpleMap<Variable, Aliasing>() {
					@Override
					public Aliasing get(Variable key) {
						return locs.get(key);
					}},
					states_to_pack_to);
			}
		}
		
		// The two unpacked variables must be the same, whether that's null or something else.
		if( (this_copy.unpackedVar == null && other.unpackedVar != null) || 
			(this_copy.unpackedVar != null && !this_copy.unpackedVar.equals(other.unpackedVar)) ) 
			throw new IllegalStateException("Unhandled case");
						
		PluralTupleLatticeElement copy = 
			create(this_copy.tupleLatticeElement.join(other.tupleLatticeElement, node), this_copy.context, 
					this_copy.unpackedVar, this.nodeWhereUnpacked,
				this_copy.dynamicStateLogic.join(other.dynamicStateLogic, node));
		// Needed because of our invariant that mostRecentAliasInfo is never null at joins/freezes.
		copy.mostRecentAliasInfo = this.mostRecentAliasInfo.join(other.mostRecentAliasInfo, node);
		
		return copy;
	}

	/*
	 * For unpackedVar, I claim that both lattices must say the same thing.
	 */
	public boolean atLeastAsPrecise(PluralTupleLatticeElement other, ASTNode node) {
		return this.tupleLatticeElement.atLeastAsPrecise(other.tupleLatticeElement, node) && 
		       this.dynamicStateLogic.atLeastAsPrecise(other.dynamicStateLogic) &&
		       this.unpackedVar == other.unpackedVar;
	}
	

	public void put(TACInstruction instr, Variable x, FractionalPermissions l) {
		tupleLatticeElement.put(instr, x, l);
	}

	@Override
	public String toString() {
		return tupleLatticeElement.toString();
	}

	public FractionalPermissions get(Aliasing objects) {
		return tupleLatticeElement.get(objects);
	}


	public FractionalPermissions get(TACInstruction instr, Variable x) {
		return tupleLatticeElement.get(instr, x);
	}

	public void put(Aliasing a, FractionalPermissions l) {
		tupleLatticeElement.put(a, l);
	}

	public FractionalPermissions get(ASTNode n, Variable x) {
		return tupleLatticeElement.get(n, x);
	}

	public void put(ASTNode n, Variable x, FractionalPermissions l) {
		tupleLatticeElement.put(n, x, l);
	}

	public PluralTupleLatticeElement bottom() {
		return create(this.tupleLatticeElement.bottom(), this.context, null, null, this.dynamicStateLogic.bottom());
	}

	public ExtendedIterator<FractionalPermissions> tupleInfoIterator() {
		return tupleLatticeElement.iterator();
	}
	
	public boolean isBottom() {
		return this.tupleLatticeElement.isBottom() && this.dynamicStateLogic.isBottom();
	}
	
	public PluralTupleLatticeElement mutableCopy() {
		AliasAwareTupleLE<FractionalPermissions> aatle = tupleLatticeElement.mutableCopy();
		DynamicStateLogic dsl = this.dynamicStateLogic.mutableCopy();
		return create(aatle, this.context, this.unpackedVar, 
				this.nodeWhereUnpacked,	dsl);
	}

	public Aliasing getLocationsAfter(ASTNode n, Variable x) {
		return tupleLatticeElement.getLocationsAfter(n, x);
	}

	public Aliasing getLocationsAfter(TACInstruction instr, Variable x) {
		return tupleLatticeElement.getLocationsAfter(instr, x);
	}

	public Aliasing getLocations(Variable x) {
		return mostRecentAliasInfo.get(x);
	}

	public Aliasing[] getLocationsAfter(final Variable[] vs) {
		final Aliasing[] result = new Aliasing[vs.length];
		for(int i = 0; i < vs.length; i++) {
			result[i] = mostRecentAliasInfo.get(vs[i]);
		}
		return result;
	}
	
	public Aliasing getLocationsBefore(ASTNode n, Variable x) {
		return tupleLatticeElement.getLocationsBefore(n, x);
	}

	public Aliasing getLocationsBefore(TACInstruction instr, Variable x) {
		return tupleLatticeElement.getLocationsBefore(instr, x);
	}

	/*
	 * BEGIN PACKING MANAGER
	 */
	private Variable unpackedVar = null;
	private ASTNode nodeWhereUnpacked = null;

	/**
	 * Is the object receiver ('this') currently packed?
	 */
	public boolean isRcvrPacked() {
		return this.unpackedVar == null;
	}

	/**
	 * At which node was the receiver unpacked?
	 */
	public ASTNode getNodeWhereUnpacked() {
		if( nodeWhereUnpacked == null ) {
			throw new IllegalStateException("Can't call this method unless unpacked.");
		}
		return nodeWhereUnpacked;
	}
	
	/**
	 * Pack the receiver to the given state with suitable defaults for the
	 * rest of the permission details.
	 * Does nothing if receiver is already packed.
	 * @param stateInfo
	 * @return Did we successfully pack?
	 * @tag todo.general -id="6023977" : fix problems with locations in receiver
	 */
	public boolean packReceiver(Variable rcvrVar, StateSpaceRepository stateRepo,
			SimpleMap<Variable, Aliasing> locs, Set<String> desiredState) {
		
		throw new IllegalStateException("This method should no longer be called.");
	}
	
	/**
	 * This method will attempt to pack the receiver to a suitably 'good' permission/state,
	 * for some definition of good.<br>
	 * <br>
	 * We will try to do the following:<br>
	 * 1.) Pack to suggested states (statesToTry)<br>
	 * 2.) Pack to the state that was unpacked<br>
	 * 3.) Pack to the root<br>
	 * 5.) ??<br>
	 * 6.) Fail<br>
	 * 7.) Profit<br>
	 * 
	 * @param thisVariable
	 * @param repository
	 * @param node
	 * @param statesToTry States worth trying to pack to (good ideas are pre and post conditions)
	 * @return Did we successfully pack?
	 */
	public boolean packReceiverToBestGuess(Variable rcvrVar,
			StateSpaceRepository stateRepo, SimpleMap<Variable, Aliasing> locs,
			String... statesToTry) {

		if( isFrozen )
			throw new IllegalStateException("Object is frozen.");

		if( isRcvrPacked() )
			throw new IllegalStateException("Double pack on the receiver. Not cool.");

		final Aliasing rcvrLoc = locs.get(rcvrVar);	
		
		Set<String> statesWorthTrying = new LinkedHashSet<String>(2+statesToTry.length);

		FractionalPermission unpacked_permission =
			this.get(rcvrLoc).getUnpackedPermission();


		// Add all the states we want to try.		
		statesWorthTrying.addAll(Arrays.asList(statesToTry));
		statesWorthTrying.addAll(unpacked_permission.getStateInfo());
		statesWorthTrying.add(unpacked_permission.getRootNode());

		// Iterate through states we want to try, take first that is SAT.
		Map<Aliasing, FractionalPermissions> repl_perms_4_state_fields = 
			new HashMap<Aliasing,FractionalPermissions>();

		boolean purify = unpacked_permission.isReadOnly();
		boolean found_good_state = false;
		String good_state = "";

		next_state:
		for( String state : statesWorthTrying ) {
			// skip states that are not underneath the unpacked permission's root
			if(!unpacked_permission.getStateSpace().firstBiggerThanSecond(unpacked_permission.getRootNode(), state))
				continue;
			
			// At this point will be empty or full of perms from unsat state
			repl_perms_4_state_fields.clear();

			FractionalPermission new_rcvr_perm = unpacked_permission.copyNewState(state);
			List<Pair<Aliasing, PermissionFromAnnotation>> inv_perms =
				this.getInvariantPermissions(rcvrVar.resolveType(),
						new_rcvr_perm, locs,rcvrLoc, false, stateRepo);
			
			if(inv_perms == null)
				// FALSE invariant --> try next state
				continue next_state;

			// Remove fields that must be null from permission checking.
//			inv_perms = this.filterOutNullPermissions(inv_perms, locs, rcvrVar.resolveType(),
//					new_rcvr_perm, stateRepo);
			
			// split them off, see if SAT
			for( Pair<Aliasing, PermissionFromAnnotation> inv_perm : inv_perms ) {
				Aliasing field_var = inv_perm.fst();

				// If we already found this variable once, split from same permission
				FractionalPermissions cur_fperms = 
					repl_perms_4_state_fields.containsKey(field_var) ?
							repl_perms_4_state_fields.get(field_var) :
								this.get(field_var);

				// get the permission and purify, if necessary
				PermissionFromAnnotation p = inv_perm.snd();
				if(purify) p = p.purify();

				// First, check to see if the field is in the right state.
				if( !cur_fperms.isInStates(p.getStateInfo(), p.isFramePermission()) ) 
					continue next_state;

				// Then, check the permission to make sure there is enough.
				FractionalPermissions new_fperms = 
					cur_fperms.splitOff(p);
				if( new_fperms.isUnsatisfiable() ) {
					// Well, we'll have to pack to some other state!
					continue next_state;
				}
				else {
					repl_perms_4_state_fields.put(field_var, new_fperms);
				}
			}
			
			// After we've checked each of the fields for permissions,
			// make sure concrete field invariants hold (including null/non-null).
			SimpleMap<String, Aliasing> vars = 
				createFieldNameToAliasingMapping(locs, rcvrVar.resolveType(), rcvrLoc);
			if(ConcreteAnnotationUtils.checkConcreteFieldInvariants(
					this, rcvrVar.resolveType(), new_rcvr_perm, vars, getAnnotationDB()) != null) {
				// Sad path exit. A concrete invariant was violated
				continue next_state;
			}
//				List<Variable> null_fields = 
//					ConcreteAnnotationUtils.getFieldsThatMustBeNull(rcvrVar.resolveType(), new_rcvr_perm, stateRepo, annotationDB);
//				for( Variable null_field : null_fields ) {
//					Aliasing loc = locs.get(null_field);
//					if( !this.dynamicStateLogic.isNull(loc) ) {
//						// Sad path exit. A field that needed to be null could not
//						// be guaranteed null.
//						continue next_state;
//					}
//				}
			
			// If we haven't continued, then we found the state!
			// Break out of the 'try each state' loop.
			found_good_state = true;
			good_state = state;
			break;
		}
	
		if( found_good_state ) {
			// If we found a good state, we re-insert the unpacked permission,
			// and the remainder field permissions.
			for( Map.Entry<Aliasing, FractionalPermissions> fperm : repl_perms_4_state_fields.entrySet()) {
				this.put(fperm.getKey(), fperm.getValue());
			}
			this.put(rcvrLoc, this.get(rcvrLoc).pack(Collections.singleton(good_state)));
			this.unpackedVar = null;
			return true;
		}
		else {
			// If we didn't find an acceptable state, FAIL.
			// Make all permissions impossible.
			this.put(rcvrLoc, this.get(rcvrLoc).invalidPack());
			this.unpackedVar = null;
			return false;
		}
	}

	/** 
	 * Unpacks the receiver, removing any permission to 'this,' and replacing
	 * it with permission for the fields that are implied by the current state
	 * invariant. Takes no action if receiver is already unpacked. Needs a
	 * state space repository so that it can create field permissions with
	 * legitimate state spaces.  If the receiver has no permissions, this
	 * method won't actually unpack but still return <code>true</code>
	 * 
	 * @param ThisVariable The receiver variable.
	 * @param StateSpaceRepository Gives us the possible states for a field type.
	 * @param ASTNode Node that will be used for BEFORE aliasing results.
	 * @return <code>false</code> if we tried to unpack an impossible state, <code>true</code>
	 * otherwise. 
	 */
	public boolean unpackReceiver(Variable rcvrVar, ASTNode nodeWhereUnpacked,
					final StateSpaceRepository stateRepo, final SimpleMap<Variable, Aliasing> locs, String rcvrRoot, String assignedField){
		
		throw new RuntimeException("I no longer expect this method to be called ever.");
	}

	protected void setNodeWhereUnpacked(ASTNode node) {
		if( isFrozen )
			throw new IllegalStateException("Object is frozen.");
		
		this.nodeWhereUnpacked = node;
	}

	protected void setUnpackedVar(Variable rcvrVar) {
		if( isFrozen )
			throw new IllegalStateException("Object is frozen.");
		
		this.unpackedVar = rcvrVar;
	}
	
	/**
	 * @param locs
	 * @param class_decl
	 * @return
	 */
	public static SimpleMap<String, Aliasing> createFieldNameToAliasingMapping(
			final SimpleMap<Variable, Aliasing> locs,
			final ITypeBinding rcvr_type,
			final Aliasing rcvr_loc) {
		final SimpleMap<String, Variable> fields = createFieldNameToVariableMapping(rcvr_type);
				
		SimpleMap<String, Aliasing> vars = new SimpleMap<String, Aliasing>() {
			@Override
			public Aliasing get(String key) {
				if( "this".equals(key) || "this!fr".equals(key) ) {
					return rcvr_loc;
				}
				else if(key.equals("super")) {
					if(rcvr_type.getSuperclass() != null)
						return getFrameAliasing(rcvr_type.getSuperclass());
					else
						throw new IllegalArgumentException("No superclass available for: " + rcvr_type);
				}
				return locs.get(fields.get(key));
			}
		};
		return vars;
	}

	/**
	 * @param class_decl
	 * @return
	 */
	public static SimpleMap<String, Variable> createFieldNameToVariableMapping(
			final ITypeBinding class_decl) {
		/*
		 * Build field mapping.
		 */
		final Map<String, IVariableBinding> fields = createFieldNameToBindingMapping(class_decl);
		return new SimpleMap<String, Variable>() {
			@Override
			public Variable get(String key) {
				IVariableBinding b = fields.get(key);
				if(b == null)
					return null;
				return new FieldVariable(b);
			}
		};
	}
	
	public static Map<String, IVariableBinding> createFieldNameToBindingMapping(ITypeBinding clazz) {
		Map<String, IVariableBinding> result = new HashMap<String, IVariableBinding>();
		for( IVariableBinding var : clazz.getDeclaredFields() ) {
			if((var.getModifiers() & Modifier.STATIC) == 0 && !result.containsKey(var.getName()))
				// skip shadowed fields
				result.put(var.getName(), var);
		}
		return Collections.unmodifiableMap(result);
	}
	
	public static Set<FieldVariable> getFieldVariables(ITypeBinding clazz) {
		Set<FieldVariable> result = new LinkedHashSet<FieldVariable>();
		for( IVariableBinding var : clazz.getDeclaredFields() ) {
			if((var.getModifiers() & Modifier.STATIC) == 0)
				// skip static fields
				result.add(new FieldVariable(var));
		}
		return Collections.unmodifiableSet(result);
	}
	
	/**
	 * This method retrieves the class state invariant annotations from a class and
	 * returns permissions for fields that are implied by the receiver's state.
	 * 
	 * This method is very much incomplete and will need to be made to work with
	 * state hierarchies and other objects that are not the receiver.
	 *   
	 * @param d The method we are currently in.
	 * @param thisPerm The state the receiver is currently in.
	 * @param isPreCondition
	 * @return <code>null</code> if the state invariant is <i>false</i>,
	 * a list of aliasing-permission pairs otherwise that represent the invariant
	 * of <code>thisPerm</code>.
	 * @tag todo.general -id="4902437" : support "false" state invariants\
	 *
	 */
	private List<Pair<Aliasing, PermissionFromAnnotation>>
	getInvariantPermissions(final ITypeBinding class_decl, FractionalPermission thisPerm,
			SimpleMap<Variable, Aliasing> locs, Aliasing rcvrLoc, 
			boolean isPreCondition, final StateSpaceRepository stateRepo) {

		/*
		 * We have this's state. Now, assume an unpack at this location. Does this state
		 * imply any state invariants?
		 */
		
		/*
		 * Build field mapping.
		 */
		final Map<String, IVariableBinding> fields = createFieldNameToBindingMapping(class_decl);

		/*
		 * Get all state invariant information from the annotation database.
		 */
		List<Pair<String, PermissionFromAnnotation>> name_perms = 
			new ArrayList<Pair<String, PermissionFromAnnotation>>();
		List<Pair<String, String>> state_infos =
			new ArrayList<Pair<String, String>>();
		
		for( ICrystalAnnotation csda : getAnnotationDB().getAnnosForType(class_decl)) {
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
						thisPerm.impliesState(decl.getStateName()) &&
								(thisPerm.coversNode(decl.getStateName()) || 
										thisPerm.getStateSpace().firstBiggerThanSecond(decl.getStateName(), thisPerm.getRootNode()));
					// TODO we can include permissions from nodes above thisPerm.getRootNode()
					// as well as long as we "purify" them
					
					if( applies ) {
						String perm_str = decl.getInv();
						if(PermParser.parseImpossibleFromString(perm_str))
							// FALSE invariant found --> quit
							return null;

						SimpleMap<String, StateSpace> smap = new SimpleMap<String,StateSpace>() {
							@Override
							public StateSpace get(String key) {
								if(key.equals("super"))
									return stateRepo.getStateSpace(class_decl.getSuperclass());
								return stateRepo.getStateSpace(fields.get(key).getType());
							}
						};

						name_perms.addAll(
								PermParser.parsePermissionsFromString(perm_str, smap, isPreCondition));
						state_infos.addAll(PermParser.parseStateInfoFromString(perm_str));

					}
				}
			}
		}
		
		/*
		 * For default field permissions, keep track of which fields have been seen.
		 */
//		Set<String> specifiedFields = new HashSet<String>();
		
		List<Pair<Aliasing, PermissionFromAnnotation>> result = 
			new ArrayList<Pair<Aliasing, PermissionFromAnnotation>>(name_perms.size());
		for( Pair<String,PermissionFromAnnotation> pair : name_perms ) {
//			specifiedFields.add(pair.fst());
			
			/*
			 * For the given permission, we have to go through the stateInfo refinements
			 * and determine if they apply. If so, change the permission. It seems like
			 * we should be returning a map rather than a list from the StateOnlyVisitor.
			 * Unfortunately, because many substate invariants could refine the same
			 * permission, that map would have to be a multi-map, and little work would
			 * be saved.
			 */
			for( Pair<String, String> state_info : state_infos ) {
				final String var = state_info.fst();
				final String s_i = state_info.snd();
				if( pair.fst().equals(var) ) {
					if( pair.snd().impliesState(s_i) == false ) {
						/*
						 * NOTE: modification of pair!!
						 */
						pair.setComponent2(pair.snd().addStateInfo(s_i));
					}
				}
			}
			
			SimpleMap<String, Aliasing> fieldLocs = createFieldNameToAliasingMapping(locs, class_decl, rcvrLoc);
			Aliasing a = fieldLocs.get(pair.fst());
			result.add(Pair.create(a, pair.snd()));
		}
		
//		/*
//		 * Add default field permissions.
//		 */
//		for( IVariableBinding field : class_decl.getDeclaredFields() ) {
//			if( !specifiedFields.contains(field.getName()) ) {
//				/*
//				 * If we haven't seen it, then we can introduce a default
//				 * permission.
//				 */
//				PermissionFromAnnotation defaultFieldPerm =
//					PermissionFactory.INSTANCE.createOrphan(stateRepo.getStateSpace(field.getType()),
//							StateSpace.STATE_ALIVE, DEFAULT_FIELD_PERMISSION,
//							StateSpace.STATE_ALIVE, isPreCondition);
//			}
//		}
		
		return result;
	}

	public boolean maybeFieldAccess(TACInstruction instr, Variable x) {
		return tupleLatticeElement.maybeFieldAccess(instr, x);
	}

	public Aliasing getEndLocations(Variable var, MethodDeclaration d) {
		return tupleLatticeElement.getEndLocations(var, d);
	}

	public Aliasing getStartLocations(Variable var, MethodDeclaration d) {
		return tupleLatticeElement.getStartLocations(var, d);
	}
	
	protected StateSpace getStateSpace(ITypeBinding type) {
		return getStateRepo().getStateSpace(type);
	}

	protected DynamicStateLogic getDynamicStateLogic() {
		return dynamicStateLogic;
	}

	protected AliasAwareTupleLE<FractionalPermissions> getTupleLatticeElement() {
		return tupleLatticeElement;
	}

	protected AnnotationDatabase getAnnotationDB() {
		return context.getAnnoDB();
	}

	protected StateSpaceRepository getStateRepo() {
		return context.getRepository();
	}
	
	protected AliasingLE getMostRecentAliasInfo() {
		return mostRecentAliasInfo;
	}

	protected Variable getUnpackedVar() {
		return unpackedVar;
	}

	protected boolean isFrozen() {
		return isFrozen;
	}

	/**
	 * @param frameType
	 * @return
	 */
	private static AliasLE getFrameAliasing(final ITypeBinding frameType) {
		return AliasLE.create(new FrameLabel(frameType));
	}
	
	/**
	 * Kills dead variables with the locations <i>after</i> the given instruction.
	 * @param instr
	 * @param liveness Liveness information after the given instruction.
	 */
	public void killDeadVariablesAfter(TACInstruction instr, final VariableLiveness liveness) {
		// variables that "also" need to stay alive
		// this is to avoid removing a variable that's the target of an implication
		// declare them here so then can be captured in the filter
		final Set<Aliasing> liveInImpl = new HashSet<Aliasing>();
		final Set<ObjectLabel> liveLocInImpl = new HashSet<ObjectLabel>();
		
		final AliasingLE aliasing = tupleLatticeElement.getLocationsAfter(instr.getNode());
		
		class Filter implements AliasingFilter, LabelFilter {
			
			/**
			 * Returns <code>true</code> if all locations in the given aliasing set
			 * are pointed to by dead variables, false otherwise.
			 * @param var
			 * @return
			 */
			@Override
			public boolean isConsidered(Aliasing var) {
				if(var.getLabels().isEmpty())
					return false;
				if(liveInImpl.contains(var))
					// staying alive in implication
					return false;
				
				for(ObjectLabel l : var.getLabels()) {
					if(isLive(l))
						return false;
				}
				return true;
			}

			/**
			 * @param l
			 */
			private boolean isLive(ObjectLabel l) {
				if(liveLocInImpl.contains(l))
					// staying alive in implication
					return true;
				Set<Variable> vars = aliasing.getVariables(l);
				if(vars == null || vars.isEmpty())
					// this is for parameters whose "declare" instructions have not yet been processed
					// TODO remove this and initialize parameter labels in LocalAliasTransfer.getLattice()
					return true;
				for(Variable x : vars) {
					if(x instanceof SourceVariable && ((SourceVariable) x).getBinding().isParameter()) {
						// keep parameters live for post-condition checks
						return true;
					}
					else if(x instanceof SourceVariable || x instanceof TempVariable) {
						if(liveness.isLive(x))
							return true;
					}
					else if(x instanceof ParamVariable) {
						if(! isConsidered(((ParamVariable) x).getOwner()))
							return true;
					}
					else
						// keyword, type variables can't die
						// TODO field variables?
						return true;
				}
				return false;
			}

			@Override
			public boolean isConsidered(ObjectLabel l) {
				return ! isLive(l);
			}
		};
		
		Filter filter = new Filter();

		// in order to find implications enabled by previously solved implications,
		// we iterate until nothing more happens
		// would be better to not consider variables kept alive by implications
		// when looking for applicable implications, but that currently results in
		// either implications being dropped too early or non-linear implications
		// staying around until the loop terminates after 10 iterations
		// TODO apply implications without regard to variables live in implications
		// TODO revisit only locations and implications revealed by previous iteration
		boolean changed;
		int count = 0;
		do {
			changed = false;

			// trace variables that "also" need to stay alive
			// this is to avoid removing a variable that's the target of an implication
			// re-compute on every turn since linear implications may have been removed
			liveInImpl.clear();
			liveInImpl.addAll(dynamicStateLogic.getLiveInImplVariables());
			liveLocInImpl.clear();
			for(Aliasing a : liveInImpl) {
				liveLocInImpl.addAll(a.getLabels());
			}
			
			/*
			 * 1. apply implications of dead variables
			 */
			List<ImplicationResult> results = dynamicStateLogic.solveFilteredVariables(this, filter);
			for(ImplicationResult r : results) {
				changed = true;
				r.putResultIntoLattice(this);
			}
			
			/*
			 * 2. remove predicates and implications of dead variables
			 */
			if(dynamicStateLogic.removeVariables(filter))
				changed = true;
			
			/*
			 * 3. remove permissions for dead variables
			 */
			if(tupleLatticeElement.removeLocations(filter))
				changed = true;
		}
		while(changed && /* avoid infinite loop */ ++count < 10);
		if(changed)
			log.warning("Exceeded dead variable kill iteration limit: " + count);
	}

	/**
	 * Discard non-guaranteed state information for permissions of every type.
	 * (Does this really forget for every permission type? Doesn't look like it.)
	 */
	public void forgetTemporaryStateInformation() {
		for(ExtendedIterator<FractionalPermissions> it = tupleInfoIterator(); it.hasNext(); ) {
			FractionalPermissions permissions = it.next();
			permissions = permissions.forgetShareAndPureStates();
			it.replace(permissions);
		}
		
		dynamicStateLogic.forgetTemporaryStateInImplications();
	}

	/**
	 * Discard state information for share and pure permissions.
	 */
	public void forgetShareAndPureStateInformation() {
		for(ExtendedIterator<FractionalPermissions> it = tupleInfoIterator(); it.hasNext(); ) {
			FractionalPermissions permissions = it.next();
			permissions = permissions.forgetShareAndPureStates();
			it.replace(permissions);
		}
		
		// TODO: Shall I make a share/pure version of this call?
		// dynamicStateLogic.forgetTemporaryStateInImplications();
	}
	
}