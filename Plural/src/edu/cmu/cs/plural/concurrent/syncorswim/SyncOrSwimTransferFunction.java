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

package edu.cmu.cs.plural.concurrent.syncorswim;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.ILabel;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.tac.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.concurrent.ConcurrentTransferFunction;
import edu.cmu.cs.plural.contexts.PluralContext;
import edu.cmu.cs.plural.track.FractionAnalysisContext;

/**
 * Transfer function for SyncOrSwim. Tells the lattice to forget all share/pures
 * that may not be synchronized.
 * 
 * @author Nels E. Beckman
 * @since May 5, 2009
 * @see {@link SyncOrSwim}
 */
class SyncOrSwimTransferFunction extends ConcurrentTransferFunction {

	private final IsSynchronizedRefAnalysis refAnalysis = new IsSynchronizedRefAnalysis();
	private final IAnalysisInput analysisInput;
	
	public SyncOrSwimTransferFunction(IAnalysisInput input,
			FractionAnalysisContext context) {
		super(input, context);
		this.analysisInput = input;
	}

	@Override
	protected IResult<PluralContext> forgetIfNotProtected(ASTNode node,
			List<ILabel> labels, IResult<PluralContext> transfer_result) {
		// Get the set of variables that are synchronized at this point.
		Set<Variable> synced_vars = this.refAnalysis.refsSyncedAtNode(node, 
				this.analysisInput);
		
		return forgetGivenSyncedVars(labels, transfer_result, synced_vars);
	}

	/**
	 * Given a set of synchronized variables, forget the shares and pures that
	 * are not synchronized.
	 */
	private IResult<PluralContext> forgetGivenSyncedVars(
			List<ILabel> labels, IResult<PluralContext> transfer_result,
			Set<Variable> synced_vars) {
		// TODO: Is there a better default? Could we get the default from the old one?
		LabeledResult<PluralContext> result = LabeledResult.createResult(labels, null);
		for( ILabel label : labels ) {
			result.put(label, this.forgetSharedPermissions(transfer_result.get(label), synced_vars));
		}
		return result;
	}

	private PluralContext forgetSharedPermissions(PluralContext lattice, 
			Set<Variable> synced_vars) {
		return lattice.forgetShareAndPureStatesNotInSet(synced_vars);
	}

	@Override
	public IResult<PluralContext> transfer(MethodCallInstruction instr,
			List<ILabel> labels, PluralContext value) {
		IResult<PluralContext> result = super.transfer(instr, labels, value);
		
		// Is this a call to wait? If so, forget ALL shares and pures.
		if( isCallToWait(instr.resolveBinding()) ) {
			return forgetGivenSyncedVars(labels, result, Collections.<Variable>emptySet());
		} else {
			return result;
		}
	}

	/**
	 * Is this a method call to Object.wait() or any of its two variants?
	 */
	private boolean isCallToWait(IMethodBinding resolvedBinding) {
		if( "wait".equals(resolvedBinding.getName()) ) {
			// There are three types of 'wait' method:
			if( resolvedBinding.getParameterTypes().length == 0 ) {
				// First has no arguments
				return true;
			} 
			else if( resolvedBinding.getParameterTypes().length == 1 ) {
				// Second takes one long
				return "long".equals(resolvedBinding.getParameterTypes()[0].getName());
			}
			else if( resolvedBinding.getParameterTypes().length == 2 ) {
				// Third takes one long and one int
				boolean is_long = "long".equals(resolvedBinding.getParameterTypes()[0].getName());
				boolean is_int = "int".equals(resolvedBinding.getParameterTypes()[1].getName());
				return is_long && is_int;
			}
			else {
				return false;
			}
		} else {
			return false;	
		}
	}	
}