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
package edu.cmu.cs.plural.concurrent;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.ILabel;
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
import edu.cmu.cs.crystal.tac.UnaryOperation;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.concurrent.nimby.IsInAtomicAnalysis;
import edu.cmu.cs.plural.linear.PluralDisjunctiveLE;
import edu.cmu.cs.plural.track.FractionAnalysisContext;
import edu.cmu.cs.plural.track.FractionalTransfer;

/**
 * The superclass for all concurrent transfer functions, this class forgets
 * permissions unless they are protected in a manner that is to be defined by
 * subclasses. In reality, the functionality
 * of this class is fairly limited. It just acts as a wrapper for Kevin's
 * FractionalTransfer function, but forgets information in strategic places.
 * I wanted to delegate, but instead I had to extend...
 * 
 * This class is the transfer function for my analysis of type-state in the
 * context of an <code>atomic</code> primitive.
 * 
 * @author Nels E. Beckman
 * @date Mar 4, 2008
 *
 */
public class ConcurrentTransferFunction extends FractionalTransfer {

	private IsInAtomicAnalysis isInAtomicAnalysis = new IsInAtomicAnalysis();

	public ConcurrentTransferFunction(IAnalysisInput input, FractionAnalysisContext context) {
		super(input, context);
	}
	
	private IResult<PluralDisjunctiveLE> forgetSharedPermissions(
			IResult<PluralDisjunctiveLE> transfer_result, List<ILabel> labels,
			PluralDisjunctiveLE value, ASTNode node) {
		// Is there a better default? Could we get the default from the old one?
		LabeledResult<PluralDisjunctiveLE> result = LabeledResult.createResult(labels, null);
		for( ILabel label : labels ) {
			result.put(label, this.forgetSharedPermissions(transfer_result.get(label), node));
		}
		return result;
	}

	private IResult<PluralDisjunctiveLE> forgetIfNotInAtomic(
			ASTNode node, List<ILabel> labels,
			PluralDisjunctiveLE value, IResult<PluralDisjunctiveLE> result) {
		
		// If a node is outside of a method decl, say because it's a field
		// initializer, we just kind of want to leave it alone.
		if( Utilities.getMethodDeclaration(node) == null ) {
			return result;
		}
		
		if( !this.isInAtomicAnalysis.isInAtomicBlock(node) ) {
			result = this.forgetSharedPermissions(result, labels, value, node);
		}
		return result;
	}
	
	private PluralDisjunctiveLE forgetSharedPermissions(PluralDisjunctiveLE lattice, ASTNode node) {
		return lattice.forgetShareAndPureStates();
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(ArrayInitInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(BinaryOperation binop,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(binop, labels, value);
		
		return forgetIfNotInAtomic(binop.getNode(), labels, value, result);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(CastInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			ConstructorCallInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(CopyInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(DotClassInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(InstanceofInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(LoadArrayInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(LoadFieldInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(LoadLiteralInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(MethodCallInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(NewArrayInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(NewObjectInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			SourceVariableDeclaration instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		// I specifically do not do anything here, since this is not
		// an expression.
		return super.transfer(instr, labels, value);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(SourceVariableRead instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(StoreArrayInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(StoreFieldInstruction instr,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(instr, labels, value);
		
		return forgetIfNotInAtomic(instr.getNode(), labels, value, result);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(UnaryOperation unop,
			List<ILabel> labels, PluralDisjunctiveLE value) {
		IResult<PluralDisjunctiveLE> result = super.transfer(unop, labels, value);
		
		return forgetIfNotInAtomic(unop.getNode(), labels, value, result);
	}
}
