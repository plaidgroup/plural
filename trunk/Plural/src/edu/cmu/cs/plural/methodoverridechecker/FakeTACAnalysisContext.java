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

package edu.cmu.cs.plural.methodoverridechecker;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.eclipse.CompilationUnitTACs;
import edu.cmu.cs.crystal.tac.eclipse.EclipseTAC;
import edu.cmu.cs.crystal.tac.model.SourceVariable;
import edu.cmu.cs.crystal.tac.model.SuperVariable;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;

/**
 * This is a TAC analysis context that I am calling "fake" because you can use it
 * even if you aren't doing a TAC dataflow analysis, which in this package, for
 * the overrider check, we are not! It stores one EclispeTAC for its lifetime,
 * so if you want variables to not be the same at some point, you should
 * create another one. 
 * 
 * @author Nels E. Beckman
 * @since Jul 29, 2009
 * @see OverrideChecker
 */
final class FakeTACAnalysisContext implements ITACAnalysisContext {

	private final MethodDeclaration analyzedMethod;
	private final EclipseTAC eclipseTAC;
	private final CompilationUnitTACs compUnitTAC;
	
	FakeTACAnalysisContext(MethodDeclaration decl) {
		this.analyzedMethod = decl;
		this.compUnitTAC = new CompilationUnitTACs();
		this.eclipseTAC = this.compUnitTAC.getMethodTAC(decl);
	}
	
	@Override
	public MethodDeclaration getAnalyzedMethod() {
		return this.analyzedMethod;
	}

	@Override
	public SourceVariable getSourceVariable(IVariableBinding varBinding) {
		return this.eclipseTAC.sourceVariable(varBinding);
	}

	@Override
	public SuperVariable getSuperVariable() {
		return this.eclipseTAC.superVariable(null);
	}

	@Override
	public ThisVariable getThisVariable() {
		return this.eclipseTAC.thisVariable();
	}

	@Override
	public Variable getVariable(ASTNode node) {
		return this.eclipseTAC.variable(node);
	}

	/**
	 * Returns the compUnitTAC.
	 * @return the compUnitTAC.
	 */
	public CompilationUnitTACs getCompUnitTAC() {
		return compUnitTAC;
	}	
}