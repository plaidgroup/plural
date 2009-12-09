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

package edu.cmu.cs.plural.polymorphic.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.ReturnInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.alias.AliasingLE;
import edu.cmu.cs.plural.contexts.ContextChoiceLE;
import edu.cmu.cs.plural.contexts.FalseContext;
import edu.cmu.cs.plural.contexts.LinearContext;
import edu.cmu.cs.plural.contexts.PluralContext;
import edu.cmu.cs.plural.contexts.TensorContext;
import edu.cmu.cs.plural.contexts.TrueContext;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.linear.DisjunctiveVisitor;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedTypeAnalysis;
import edu.cmu.cs.plural.polymorphic.internal.PolyTupleLattice.PackedNess;

/**
 * Transfer function for the internal polymorphism checker.
 * 
 * @author Nels E. Beckman
 * @since Nov 11, 2009
 *
 */
public final class PolyInternalTransfer extends
	// Lol at the length of this superclass name...	
	AbstractTACBranchSensitiveTransferFunction<PolyTupleLattice> {

	private final PolyTupleLatticeOps ops = new PolyTupleLatticeOps();
	
	private final ITACFlowAnalysis<AliasingLE> aliasAnalysis;
	private final SimpleMap<String,Option<PolyVar>> varLookup;
	private final List<Pair<Aliasing,Option<String>>> paramsEntryPerm;
	private final Option<String> rcvrEntryPerm;
	private final AnnotationDatabase annoDB;
	private final IAnalysisInput input;
	
	/** This analysis depends on plural to determine what state 'this' is in. 
	 *  Value will change! Each time createEntryLattice is called. */
	private ITACFlowAnalysis<PluralContext> plural = null;
	
	/** Shows us which parameters have been applied to the types of each variable. */
	private final InstantiatedTypeAnalysis typeAnalysis;
	
	public PolyInternalTransfer(ITACFlowAnalysis<AliasingLE> aliasAnalysis,
			SimpleMap<String,Option<PolyVar>> varLookup,
			List<Pair<Aliasing,Option<String>>> paramsForEntry, Option<String> rcvrEntryPerm,
			IAnalysisInput input, InstantiatedTypeAnalysis typeAnalysis) {
		this.aliasAnalysis = aliasAnalysis;
		this.varLookup = varLookup;
		this.paramsEntryPerm = paramsForEntry;
		this.rcvrEntryPerm = rcvrEntryPerm;
		this.annoDB = input.getAnnoDB();
		this.input = input;
		this.typeAnalysis = typeAnalysis;
	}
	
	@Override
	public PolyTupleLattice createEntryValue(
			MethodDeclaration method) {
		PolyTupleLattice result = ops.getDefault();
		// Put in initial values for parameters and receiver as indicated
		// by the annotations.
		for( Pair<Aliasing,Option<String>> param : paramsEntryPerm ) {
			PolyVarLE le;
			if( param.snd().isSome() )
				le = PolyVarLE.HAVE_FACTORY.call(param.snd().unwrap());
			else
				le = PolyVarLE.NONE;
			result.put(param.fst(), le);
		}
		
		// Assign the plural analysis, since here we have the method decl
		this.plural = (new PluralWrapper(method, input)).getFractionalAnalysis();
		
		return result;
	}

	@Override
	public ILatticeOperations<PolyTupleLattice> getLatticeOperations() {
		return ops;
	}

	// The permission you have STAYS CONSTANT. Do no substitute it.
	// For arguments, All we need to do is substitute the applied(1) for the
	// static params(2) that come from the receiver, to the @PolyVar(3) permission.
	private PolyVarLE substituteSplitAndMerge(List<String> rcvr_app, ITypeBinding rcvr_type,
			PolyVarLE incoming, Option<PolyVarUseAnnotation> anno_, ASTNode error_node) {
		// If no permission is needed, we can just pretend nothing happens.
		if( anno_.isNone() )
			return incoming;
		
		PolyVarUseAnnotation anno = anno_.unwrap();
		
		// If permission is needed, we need to substitute based on the given params.
		List<String> needed_perms_pre_sub = Collections.singletonList(anno.getVariableName());
		List<String> needed_perms = InstantiatedTypeAnalysis.substitute(rcvr_app, rcvr_type, needed_perms_pre_sub, annoDB);
		assert(needed_perms.size() == 1);
		
		String needed_perm = needed_perms.get(0); 
		
		// The needed perm might be share, pure, unique, etc. in which case we ignore it.
		if( PolyInternalChecker.isPermLitteral(needed_perm) )
			return incoming;
		
		// If the permission is not the same, we'll just pretend we have the permission we are
		// supposed to have, since the checker SHOULD find this error. We'd like to only report
		// one error.
		if( incoming == PolyVarLE.TOP || incoming.name().isNone() || !incoming.name().unwrap().equals(needed_perm) )
			incoming = PolyVarLE.HAVE_FACTORY.call(needed_perm);
		
		// So at THIS point, we know that the perm we need is the same
		// as the one we have.
		assert(incoming.name().unwrap().equals(needed_perm));
		
		// If returned is true, we always get the same perm back.
		if( anno.isReturned() )
			return incoming;
		
		// All we really need to do now is to look up the type of the poly.
		Option<PolyVar> var_ = this.varLookup.get(needed_perm);
		if( var_.isNone() ) {
			// Use of variable that is not in scope
			throw new PolyInternalChecker.VarScope(needed_perm, error_node);
		}
		// If its EXACT or SIMILAR, we are left with nothing, but if its
		// SYMMETRIC, we are left with the same thing.
		switch( var_.unwrap().getKind() ) {
		case EXACT:
		case SIMILAR:
			return PolyVarLE.NONE;
		case SYMMETRIC:
			return incoming;
		default:
			throw new RuntimeException("Impossible");
		}
	}
	
	@Override
	public IResult<PolyTupleLattice> transfer(
			MethodCallInstruction instr, List<ILabel> labels,
			PolyTupleLattice value) {
		// First get the application type for the receiver.
		MethodDeclaration method = this.getAnalysisContext().getAnalyzedMethod();
		List<String> rcvr_type = typeAnalysis.findType(instr.getReceiverOperand());
		ITypeBinding rcvr_jtype = instr.getReceiverOperand().resolveType();
		
		// Now we can iterate through the arguments.
		int arg_num = 0;
		for( Variable arg : instr.getArgOperands() ) {
			Aliasing arg_loc = aliasAnalysis.getResultsBefore(instr).get(arg);
			PolyVarLE cur_le = value.get(arg_loc);
			Option<PolyVarUseAnnotation> anno_ = AnnotationUtilities.ithParamToAnnotation(instr.resolveBinding(), arg_num, annoDB);
			PolyVarLE new_le = this.substituteSplitAndMerge(rcvr_type, rcvr_jtype, cur_le, anno_, instr.getNode());
			value.put(arg_loc, new_le);
			arg_num++;
		}
		
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PolyTupleLattice> transfer(
			ConstructorCallInstruction instr, List<ILabel> labels,
			PolyTupleLattice value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PolyTupleLattice> transfer(
			CopyInstruction instr, List<ILabel> labels,
			PolyTupleLattice value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PolyTupleLattice> transfer(
			LoadFieldInstruction instr, List<ILabel> labels,
			PolyTupleLattice value) {
		ThisVariable this_var = getAnalysisContext().getThisVariable();
		
		if( instr.getSourceObject().equals(this_var) )
			value = unpackIfNeeded(instr, value, this_var);
		
		return super.transfer(instr, labels, value);
	}

	/**
	 * Unpack, based on the current state as determined by plural. Return
	 * the resulting lattice as 
	 */
	private PolyTupleLattice unpackIfNeeded(final TACInstruction instr,
			PolyTupleLattice value, final ThisVariable this_var) {
		if( value.getPackedness().equals(PackedNess.UNPACKED) )
			return value;
		
		PluralContext plural_ctx = plural.getResultsBefore(instr);
		
		// So, figure out what state the receiver is in...
		LinearContext ctx = plural_ctx.getLinearContext();
		Option<Set<String>> cur_rcvr_state =
			ctx.dispatch(new DisjunctiveVisitor<Option<Set<String>>>() {
				@Override public Option<Set<String>> falseContext(FalseContext falseContext) {return Option.none();}
				@Override public Option<Set<String>> trueContext(TrueContext trueContext) {return Option.none();}
				@Override public Option<Set<String>> choice(ContextChoiceLE le) {return Utilities.nyi();}
				@Override
				public Option<Set<String>> context(TensorContext le) {
					FractionalPermissions this_perms = le.getTuple().get(instr, this_var);
					List<String> packed_states = this_perms.getStateInfo(true);
					
					return Option.<Set<String>>some(new HashSet<String>(packed_states));
				}
			});
		
		if( cur_rcvr_state.isNone() )
			return value;
		
		ITypeBinding this_type = this_var.resolveType();
		Set<String> unpacked_states = cur_rcvr_state.unwrap();
		Map<Variable, PolyVar> fieldToInvMap = invariants(this_type, unpacked_states);
		
		AliasingLE locs = aliasAnalysis.getResultsBefore(instr);
		for( Map.Entry<Variable, PolyVar> entry : fieldToInvMap.entrySet() ) {
			Aliasing field_loc = locs.get(entry.getKey());
			PolyVar poly_var = entry.getValue();
			value.put(field_loc, PolyVarLE.HAVE_FACTORY.call(poly_var.getName()));
		}
		
		return value;
	}
	
	private Map<Variable, PolyVar> invariants(ITypeBinding this_type, Set<String> unpacked_states) {
		return PackingManager.invariants(this_type, unpacked_states, annoDB, varLookup);
	}
	
	@Override
	public IResult<PolyTupleLattice> transfer(
			NewObjectInstruction instr, List<ILabel> labels,
			PolyTupleLattice value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PolyTupleLattice> transfer(ReturnInstruction instr, 
			List<ILabel> labels, PolyTupleLattice value) {
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PolyTupleLattice> transfer(
			StoreFieldInstruction instr, List<ILabel> labels,
			PolyTupleLattice value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}	
}