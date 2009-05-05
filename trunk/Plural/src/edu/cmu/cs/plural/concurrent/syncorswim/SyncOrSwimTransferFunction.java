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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.ILabel;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.plural.concurrent.ConcurrentTransferFunction;
import edu.cmu.cs.plural.linear.PluralDisjunctiveLE;
import edu.cmu.cs.plural.track.FractionAnalysisContext;

/**
 * Transfer function for SyncOrSwim.
 * 
 * @author Nels E. Beckman
 * @since May 5, 2009
 * @see {@link SyncOrSwim}
 */
class SyncOrSwimTransferFunction extends ConcurrentTransferFunction {

	public SyncOrSwimTransferFunction(IAnalysisInput input,
			FractionAnalysisContext context) {
		super(input, context);
	}

	@Override
	protected IResult<PluralDisjunctiveLE> forgetIfNotProtected(ASTNode node,
			List<ILabel> labels, IResult<PluralDisjunctiveLE> transfer_result) {
		// TODO: Is there a better default? Could we get the default from the old one?
		LabeledResult<PluralDisjunctiveLE> result = LabeledResult.createResult(labels, null);
		for( ILabel label : labels ) {
			result.put(label, this.forgetSharedPermissions(transfer_result.get(label), node));
		}
		return result;
	}

	private PluralDisjunctiveLE forgetSharedPermissions(PluralDisjunctiveLE lattice, ASTNode node) {
		return lattice.forgetShareAndPureStates();
	}
	
}
