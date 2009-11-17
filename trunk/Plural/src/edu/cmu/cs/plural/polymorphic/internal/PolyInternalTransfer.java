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
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.ReturnInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.alias.AliasingLE;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedTypeAnalysis;

/**
 * Transfer function for the internal polymorphism checker.
 * 
 * @author Nels E. Beckman
 * @since Nov 11, 2009
 *
 */
public final class PolyInternalTransfer extends
	// Lol at the length of this superclass name...	
	AbstractTACBranchSensitiveTransferFunction<TupleLatticeElement<Aliasing, PolyVarLE>> {

	private final TupleLatticeOperations<Aliasing,PolyVarLE> ops =
		new TupleLatticeOperations<Aliasing,PolyVarLE>(new PolyInternalLatticeOps(), PolyVarLE.TOP);
	
	private final ITACFlowAnalysis<AliasingLE> aliasAnalysis;
	private final SimpleMap<String,Option<PolyVar>> varLookup;
	private final List<Pair<Aliasing,String>> paramsEntryPerm;
	private final Option<String> rcvrEntryPerm;
	private final AnnotationDatabase annoDB;
	
	/** Shows us which parameters have been applied to the types of each variable. */
	private final InstantiatedTypeAnalysis typeAnalysis;
	
	public PolyInternalTransfer(ITACFlowAnalysis<AliasingLE> aliasAnalysis,
			SimpleMap<String,Option<PolyVar>> varLookup,
			List<Pair<Aliasing,String>> paramsForEntry, Option<String> rcvrEntryPerm,
			AnnotationDatabase annoDB, InstantiatedTypeAnalysis typeAnalysis) {
		this.aliasAnalysis = aliasAnalysis;
		this.varLookup = varLookup;
		this.paramsEntryPerm = paramsForEntry;
		this.rcvrEntryPerm = rcvrEntryPerm;
		this.annoDB = annoDB;
		this.typeAnalysis = typeAnalysis;
	}
	
	@Override
	public TupleLatticeElement<Aliasing, PolyVarLE> createEntryValue(
			MethodDeclaration method) {
		TupleLatticeElement<Aliasing, PolyVarLE> result = ops.getDefault();
		// Put in initial values for parameters and receiver as indicated
		// by the annotations.
		for( Pair<Aliasing,String> param : paramsEntryPerm ) {
			PolyVarLE le = PolyVarLE.HAVE_FACTORY.call(param.snd());
			result.put(param.fst(), le);
		}
		return result;
	}

	@Override
	public ILatticeOperations<TupleLatticeElement<Aliasing, PolyVarLE>> getLatticeOperations() {
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
		List<String> needed_perms = InstantiatedTypeAnalysis.substitute(rcvr_app, rcvr_type, needed_perms_pre_sub, annoDB, error_node);
		assert(needed_perms.size() == 1);
		
		String needed_perm = needed_perms.get(0); 
		
		// The needed perm might be share, pure, unique, etc. in which case we ignore it.
		if( isPermLitteral(needed_perm) )
			return incoming;
		
		// If the permission is not the same, we produce bottom, just because
		// the checker will be signaling an error here. We don't want to make
		// even more errors. Same if we have no permission.
		if( incoming == PolyVarLE.TOP || incoming.name().isNone() || !incoming.name().unwrap().equals(needed_perm) )
			return PolyVarLE.BOTTOM;
		
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
	
	/**
	 * Is the given perm share, pure, unique, full or immutable?
	 */
	private boolean isPermLitteral(String perm) {
		String lc_perm = perm.toLowerCase();
		return lc_perm.equals("pure") ||
			lc_perm.equals("share") ||
			lc_perm.equals("full") ||
			lc_perm.equals("unique") ||
			lc_perm.equals("immutable");
	}

	@Override
	public IResult<TupleLatticeElement<Aliasing, PolyVarLE>> transfer(
			MethodCallInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Aliasing, PolyVarLE> value) {
		// First get the application type for the receiver.
		MethodDeclaration method = this.getAnalysisContext().getAnalyzedMethod();
		List<String> rcvr_type = typeAnalysis.findType(instr.getReceiverOperand(), method);
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
	public IResult<TupleLatticeElement<Aliasing, PolyVarLE>> transfer(
			ConstructorCallInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Aliasing, PolyVarLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<TupleLatticeElement<Aliasing, PolyVarLE>> transfer(
			CopyInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Aliasing, PolyVarLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<TupleLatticeElement<Aliasing, PolyVarLE>> transfer(
			LoadFieldInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Aliasing, PolyVarLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<TupleLatticeElement<Aliasing, PolyVarLE>> transfer(
			NewObjectInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Aliasing, PolyVarLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<TupleLatticeElement<Aliasing, PolyVarLE>> transfer(
			ReturnInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Aliasing, PolyVarLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<TupleLatticeElement<Aliasing, PolyVarLE>> transfer(
			StoreFieldInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Aliasing, PolyVarLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, labels, value);
	}	
}