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
package edu.cmu.cs.plural.alias;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.ILabel;
import edu.cmu.cs.crystal.analysis.live.LiveVariableLE;
import edu.cmu.cs.crystal.analysis.live.LiveVariableTransferFunction;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.ITACFlowAnalysis;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.Variable;

/**
 * @author Kevin Bierhoff
 * @since 7/30/2008
 */
public class LivenessProxy {
	
	private final ITACFlowAnalysis<TupleLatticeElement<Variable, LiveVariableLE>> livenessAnalysis;
	
	public static LivenessProxy create(final IAnalysisInput input) {
		return new LivenessProxy(input);
	}
	
	private LivenessProxy(final IAnalysisInput input) {		
		livenessAnalysis = 
			new TACFlowAnalysis<TupleLatticeElement<Variable, LiveVariableLE>>(new LiveVariableTransferFunction(), input.getComUnitTACs().unwrap());
	}
	
	public void switchToMethod(MethodDeclaration d) {
		livenessAnalysis.getEndResults(d);
	}
	
	public boolean isDeadBefore(TACInstruction instr, Variable x) {
		// no point in using labeled results since dead variable is branch insensitive
		return livenessAnalysis.getResultsBefore(instr).get(x) == LiveVariableLE.DEAD;
	}

	public boolean isDeadAfter(TACInstruction instr, Variable x, ILabel label) {
		// distinguish incoming results on different labels
		return getAfterResult(instr, x, label) == LiveVariableLE.DEAD;
	}

	public boolean isLiveBefore(TACInstruction instr, Variable x) {
		// no point in using labeled results since dead variable is branch insensitive
		return livenessAnalysis.getResultsBefore(instr).get(x) == LiveVariableLE.LIVE;
	}

	public boolean isLiveAfter(TACInstruction instr, Variable x, ILabel label) {
		// distinguish incoming results on different labels
		LiveVariableLE info = getAfterResult(instr, x, label);
		return info == LiveVariableLE.LIVE;
	}

	/**
	 * Internally used to look up the after-liveness information for the given variable on
	 * edges of the given label, or the merged after-result, if there are
	 * no edges with that label.
	 * @param instr
	 * @param x
	 * @param label
	 * @return the after-liveness information for the given variable on
	 * edges of the given label, or the merged after-result, if there are
	 * no edges with that label.
	 */
	private LiveVariableLE getAfterResult(TACInstruction instr, Variable x,
			ILabel label) {
		IResult<TupleLatticeElement<Variable, LiveVariableLE>> after = 
			livenessAnalysis.getLabeledResultsAfter(instr);
		LiveVariableLE result;
		if(after.keySet().contains(label)) {
			result = after.get(label).get(x);
		} 
		else
			// label unknown--use merged result
			result = livenessAnalysis.getResultsAfter(instr).get(x);
		return result;
	}

}
