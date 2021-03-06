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
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.tac.model.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.model.BinaryOperation;
import edu.cmu.cs.crystal.tac.model.CastInstruction;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.DotClassInstruction;
import edu.cmu.cs.crystal.tac.model.InstanceofInstruction;
import edu.cmu.cs.crystal.tac.model.LoadArrayInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.LoadLiteralInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewArrayInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.SourceVariableDeclaration;
import edu.cmu.cs.crystal.tac.model.SourceVariableReadInstruction;
import edu.cmu.cs.crystal.tac.model.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.UnaryOperation;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.contexts.PluralContext;
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
 * 
 * @author Nels E. Beckman
 * @date Mar 4, 2008
 *
 */
public abstract class ConcurrentTransferFunction extends FractionalTransfer {

	public ConcurrentTransferFunction(IAnalysisInput input, FractionAnalysisContext context) {
		super(input, context);
	}
	
	/**
	 * This method to be implemented by subclasses depending on the appropriate
	 * protection scheme.
	 * @param node The AST node currently under analysis.
	 * @param labels The input transfer labels.
	 * @param result The result of transferring in the default analysis. 
	 * @return The new result-holding thingy.
	 */
	protected abstract IResult<PluralContext> forgetIfNotProtected(
			ASTNode node, List<ILabel> labels,
			IResult<PluralContext> result);
	
	
	private IResult<PluralContext> forgetIfNotProtectedAndNodeValid(
			ASTNode node, List<ILabel> labels,
			IResult<PluralContext> result) {
		
		// If a node is outside of a method decl, say because it's a field
		// initializer, we just kind of want to leave it alone.
		if( Utilities.getMethodDeclaration(node) == null ) {
			return result;
		}
		
		return forgetIfNotProtected(node, labels, result);
	}

	@Override
	public IResult<PluralContext> transfer(ArrayInitInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}

	@Override
	public IResult<PluralContext> transfer(BinaryOperation binop,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(binop, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(binop.getNode(), labels, result);
	}

	@Override
	public IResult<PluralContext> transfer(CastInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}

	@Override
	public IResult<PluralContext> transfer(
			ConstructorCallInstruction instr, List<ILabel> labels,
			PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(CopyInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(DotClassInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(InstanceofInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(LoadArrayInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(LoadFieldInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(LoadLiteralInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(MethodCallInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(NewArrayInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}
	
	@Override
	public IResult<PluralContext> transfer(NewObjectInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}

	@Override
	public IResult<PluralContext> transfer(
			SourceVariableDeclaration instr, List<ILabel> labels,
			PluralContext value) {
		// I specifically do not do anything here, since this is not
		// an expression.
		return super.transfer(instr, labels, value);
	}
	
	@Override
	public IResult<PluralContext> transfer(SourceVariableReadInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}

	@Override
	public IResult<PluralContext> transfer(StoreArrayInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}

	@Override
	public IResult<PluralContext> transfer(StoreFieldInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(instr.getNode(), labels, result);
	}

	@Override
	public IResult<PluralContext> transfer(UnaryOperation unop,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(unop, labels, value);
		
		return forgetIfNotProtectedAndNodeValid(unop.getNode(), labels, result);
	}
}