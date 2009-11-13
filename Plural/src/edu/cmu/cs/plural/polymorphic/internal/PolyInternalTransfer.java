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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
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
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.alias.AliasingLE;

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
	
	public PolyInternalTransfer(ITACFlowAnalysis<AliasingLE> aliasAnalysis,
			SimpleMap<String,Option<PolyVar>> varLookup,
			List<Pair<Aliasing,String>> paramsForEntry, Option<String> rcvrEntryPerm) {
		this.aliasAnalysis = aliasAnalysis;
		this.varLookup = varLookup;
		this.paramsEntryPerm = paramsForEntry;
		this.rcvrEntryPerm = rcvrEntryPerm;
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

	@Override
	public IResult<TupleLatticeElement<Aliasing, PolyVarLE>> transfer(
			MethodCallInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Aliasing, PolyVarLE> value) {
		// Start with the method call. Based on the specification and
		// the current value of each parameter, modify the parameters.
		// Subtract spec permission from current available permission.
		IMethodBinding binding = instr.resolveBinding();
		
		int p_num = 0; // Which parameter are we analyzing?
		for( Variable param : instr.getArgOperands() ) {
			Aliasing ploc = this.aliasAnalysis.getResultsBefore(instr).get(param);
			PolyVarLE p_le = value.get(ploc);
			Option<PolyVarUseAnnotation> anno = AnnotationUtilities.ithParamToAnnotation(binding, p_num, null);
			PolyVarLE new_p_le = splitAndMerge(p_le, anno, instr.getNode()); 
			value.put(ploc, new_p_le);
			
			p_num++;
		}
		
		return super.transfer(instr, labels, value);
	}
	
	/**
	 * Given a lattice element for a location, and given the annotation of the
	 * parameter that is splitting off permission, return the resulting permission
	 * that would occur from splitting and then merging the specified permission. 
	 */
	private PolyVarLE splitAndMerge(PolyVarLE p_le,
			Option<PolyVarUseAnnotation> anno_, ASTNode error_node) {
		// If there's no annotation, we can just pretend nothing happens.
		if( anno_.isNone() )
			return p_le;
		
		// If we have top, then the result must be top as well.
		if( p_le.isTop() )
			return p_le;
		
		PolyVarUseAnnotation anno = anno_.unwrap();
		// If the permission is not the same, we produce bottom, just because
		// the checker will be signaling an error here. We don't want to make
		// even more errors. Same if we have no permission.
		if( p_le.name().isNone() || !p_le.name().unwrap().equals(anno.getVariableName()) )
			return PolyVarLE.BOTTOM;
		
		// So at THIS point, we know that the perm we need is the same
		// as the one we have.
		assert(p_le.name().unwrap().equals(anno.getVariableName()));
		
		// If returned is true, we always get the same perm back.
		if( anno.isReturned() )
			return p_le;
		
		// All we really need to do now is to look up the type of the poly.
		// If its EXACT or SIMILAR, we are left with nothing, but if its
		// SYMMETRIC, we are left with the same thing.
		Option<PolyVar> var_ = this.varLookup.get(anno.getVariableName());
		if( var_.isNone() ) {
			// Use of variable that is not in scope
			throw new PolyInternalChecker.VarScope(anno.getVariableName(), error_node);
		}
			
		// TODO Working here when you get instantiation figured out
		// if( this.varLookup.get(p_le.name().unwrap()) )
		return Utilities.nyi();
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