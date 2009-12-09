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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.eclipse.CompilationUnitTACs;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.contexts.PluralContext;
import edu.cmu.cs.plural.polymorphic.instantiation.RcvrInstantiationPackage;
import edu.cmu.cs.plural.states.IInvocationCase;
import edu.cmu.cs.plural.states.IInvocationCaseInstance;
import edu.cmu.cs.plural.states.IInvocationSignature;
import edu.cmu.cs.plural.states.MethodCheckingKind;
import edu.cmu.cs.plural.states.StateSpaceRepository;
import edu.cmu.cs.plural.track.FractionAnalysisContext;
import edu.cmu.cs.plural.track.FractionalTransfer;

/**
 * In order to use Plural separately from FractionalAnalysis, which is
 * what we need to do for the PolyInternalTransfer in order to find the
 * receiver state, we need to give it several things, such as a 
 * FractionAnalysisContext. This wrapper class gives the analysis
 * everything it needs.
 * 
 * @author Nels E. Beckman
 * @since Dec 9, 2009
 *
 */
public final class PluralWrapper implements FractionAnalysisContext {

	
	private final ITACFlowAnalysis<PluralContext> fractionAnalysis;
	private final IAnalysisInput input;
	private final IInvocationCaseInstance case_instance;
	
	public PluralWrapper(MethodDeclaration mdecl, IAnalysisInput input) {
		FractionalTransfer transfer = new FractionalTransfer(input, this);
		this.fractionAnalysis = 
			new TACFlowAnalysis<PluralContext>(transfer,
					input.getComUnitTACs().unwrap());
		this.input = input;
		IInvocationSignature sig = this.getRepository().getSignature(mdecl.resolveBinding());
		
		if( sig.cases().size() > 1 )
			Utilities.nyi();
		
		IInvocationCase case_ = sig.cases().get(0);
		MethodCheckingKind mck = mdecl.isConstructor() ? 
				MethodCheckingKind.CONSTRUCTOR_IMPL_CUR_IS_VIRTUAL : MethodCheckingKind.METHOD_IMPL_CUR_IS_VIRTUAL;
		this.case_instance =			
			case_.createPermissions(mck, this.assumeVirtualFrame(), 
					false, Option.<RcvrInstantiationPackage>none());
	}
	
	@Override
	public boolean assumeVirtualFrame() {
		// True because when we use this it will be because we
		// need to know about packing/unpacking(?)
		return true;
	}

	@Override
	public IInvocationCaseInstance getAnalyzedCase() {
		return case_instance;
	}

	@Override
	public AnnotationDatabase getAnnoDB() {
		return input.getAnnoDB();
	}

	@Override
	public Option<CompilationUnitTACs> getComUnitTACs() {
		return input.getComUnitTACs();
	}

	@Override
	public Option<IProgressMonitor> getProgressMonitor() {
		return input.getProgressMonitor();
	}

	@Override
	public StateSpaceRepository getRepository() {
		return StateSpaceRepository.getInstance(this.getAnnoDB());
	}

	/** Return the actual fractional analysis, what is really PLURAL. */
	public ITACFlowAnalysis<PluralContext> getFractionalAnalysis() {
		return this.fractionAnalysis;
	}
}
