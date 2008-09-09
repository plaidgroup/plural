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
package edu.cmu.cs.plural.atomic;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.ILabel;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.tac.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.BinaryOperation;
import edu.cmu.cs.crystal.tac.CastInstruction;
import edu.cmu.cs.crystal.tac.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.CopyInstruction;
import edu.cmu.cs.crystal.tac.DotClassInstruction;
import edu.cmu.cs.crystal.tac.InstanceofInstruction;
import edu.cmu.cs.crystal.tac.LoadArrayInstruction;
import edu.cmu.cs.crystal.tac.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.LoadLiteralInstruction;
import edu.cmu.cs.crystal.tac.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.NewArrayInstruction;
import edu.cmu.cs.crystal.tac.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.SourceVariableDeclaration;
import edu.cmu.cs.crystal.tac.SourceVariableRead;
import edu.cmu.cs.crystal.tac.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.UnaryOperation;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.track.FractionAnalysisContext;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;
import edu.cmu.cs.plural.track.SingleTruthFractionalTransfer;
import edu.cmu.cs.plural.util.ExtendedIterator;

/**
 * This class is the transfer function for my analysis of type-state in the
 * context of an <code>atomic</code> primitive. In reality, the functionality
 * of this class is fairly limited. It just acts as a wrapper for Kevin's
 * FractionalTransfer function, but forgets information in strategic places.
 * I wanted to delegate, but instead I had to extend...
 * 
 * @author Nels Beckman
 * @date Mar 4, 2008
 *
 */
public class NIMBYTransferFunction extends SingleTruthFractionalTransfer {

	private IsInAtomicAnalysis isInAtomicAnalysis = new IsInAtomicAnalysis();

	public NIMBYTransferFunction(AnnotationDatabase annoDB, FractionAnalysisContext context) {
		super(annoDB, context);
	}

	/**
	 * 'Forgets' state information (rootState? stateInfo? Unknown at this point...) for
	 * permissions in the given lattice and returns the resulting lattice without modifying
	 * the original lattice.
	 * 
	 * @param lattice Source lattice, from which permissions should be forgotten.
	 * @param isFullShared Does FULL count as a shared permission? Necessary for weak atomicity.
	 * @return A copy of the given lattice without state information for thread-shared permissions.
	 */
	private PluralTupleLatticeElement forgetSharedPermissions(PluralTupleLatticeElement lattice, ASTNode n) {
		PluralTupleLatticeElement result = lattice.mutableCopy();

		/*
		 * Loop through all variables that should be reset if they are shared.
		 */
		for( ExtendedIterator<FractionalPermissions> it = result.tupleInfoIterator(); it.hasNext(); ) {
			/*
			 * For each permission for this var, see what type it is.
			 */
			FractionalPermissions var_perms = it.next();
			/*
			 * FOR THE TIME-BEING, WE ARE JUST FORGETTING THE STATE-INFO, BUT IF
			 * STATE GUARANTEES ARE GUARANTEED WITH PURE OR SHARE SOMEHOW, THEY
			 * SHOULD BE FORGOTTEN TOO.
			 */
			var_perms = var_perms.forgetTemporaryStateInfo();
			it.replace(var_perms);
			
//			for( FractionalPermission var_perm : var_perms.getPermissions() ) {
//				/*
//				 * Forget if thread-shared.
//				 */
//				if( var_perm.isShare() || 
//						var_perm.isPure() ) {
//					final FractionalPermission new_var_perm = var_perm.forgetStateInfo();
//					var_perms = var_perms.splitOff(var_perm);
//					var_perms = var_perms.mergeIn(new_var_perm);
//
//					result.put(var, var_perms);
//				}
//			}
		}

		return result.storeCurrentAliasingInfo(n);
	}

	/**
	 * A more efficient version of forgetSharedPermissions if you already know which variables
	 * could have changed since the last 'forgetting.' See
	 * {@link NIMBYTransferFunction#forgetSharedPermissions(PluralTupleLatticeElement, boolean)} 
	 * @param lattice
	 * @param candidateVars
	 * @return
	 */
	private PluralTupleLatticeElement forgetSharedPermissions(
			PluralTupleLatticeElement lattice,
			TACInstruction instr,
			Variable... candidateVars) {
		PluralTupleLatticeElement result = lattice.mutableCopy();

		/*
		 * Loop through all variables that should be reset if they are shared.
		 */
		for( Variable var : candidateVars ) {
			/*
			 * For each permission for this var, see what type it is.
			 */
			FractionalPermissions var_perms = result.get(instr, var);
			/*
			 * FOR THE TIME-BEING, WE ARE JUST FORGETTING THE STATE-INFO, BUT IF
			 * STATE GUARANTEES ARE GUARANTEED WITH PURE OR SHARE SOMEHOW, THEY
			 * SHOULD BE FORGOTTEN TOO.
			 */
			var_perms = var_perms.forgetTemporaryStateInfo();
			result.put(instr, var, var_perms);
			
//			for( FractionalPermission var_perm : var_perms.getPermissions() ) {
//				/*
//				 * Forget if thread-shared.
//				 */
//				if( var_perm.isShare() || 
//						var_perm.isPure() ) {
//					final FractionalPermission new_var_perm = var_perm.forgetStateInfo();
//					var_perms = var_perms.splitOff(var_perm);
//					var_perms = var_perms.mergeIn(new_var_perm);
//
//					result.put(var, var_perms);
//				}
//			}
		}

		return result;
	}

	/**
	 * Helper method, gets aliasing locations when given an array of variables.
	 */
	private Aliasing[] getVariableLocations(PluralTupleLatticeElement lattice,
			ASTNode curNode, Variable... vars) {
		Aliasing[] result = new Aliasing[vars.length];

		for( int i = 0; i<vars.length; ) {
			result[i] = lattice.getLocationsAfter(curNode, vars[i]);
		}
		return result;
	}

	/**
	 * Another helper method.
	 */
	private IResult<PluralTupleLatticeElement> forgetSharedPermissions(
			IResult<PluralTupleLatticeElement> transfer_result, List<ILabel> labels, ASTNode n) {
		/*
		 * I have no real default... How do I do this?
		 */
		LabeledResult<PluralTupleLatticeElement> result = LabeledResult.createResult(labels, null);
		for( ILabel label : labels ) {
			result.put(label, this.forgetSharedPermissions(transfer_result.get(label), n));
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			ArrayInitInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(BinaryOperation binop,
			List<ILabel> labels, PluralTupleLatticeElement value) {


		IResult<PluralTupleLatticeElement> result = super.transfer(binop, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(binop.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, binop.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(CastInstruction instr,
			List<ILabel> labels, PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			ConstructorCallInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(CopyInstruction instr,
			List<ILabel> labels, PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			DotClassInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			InstanceofInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			LoadArrayInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			LoadFieldInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			LoadLiteralInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			MethodCallInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			NewArrayInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		IResult<PluralTupleLatticeElement> result = super.transfer(instr, labels, value);

		if( !this.isInAtomicAnalysis.isInAtomicBlock(instr.getNode()) ) {
			result = this.forgetSharedPermissions(result, labels, instr.getNode());
		}
		return result;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			NewObjectInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			SourceVariableDeclaration instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			SourceVariableRead instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			StoreArrayInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			StoreFieldInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(UnaryOperation unop,
			List<ILabel> labels, PluralTupleLatticeElement value) {
		return super.transfer(unop, labels, value);
	}


}
