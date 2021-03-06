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
package edu.cmu.cs.plural.track;

import org.eclipse.core.runtime.IProgressMonitor;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.tac.eclipse.CompilationUnitTACs;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.plural.states.IInvocationCaseInstance;
import edu.cmu.cs.plural.states.StateSpaceRepository;

/**
 * For some reason, ECLIPSE's class loader really did not like when
 * this interface extended IAnalysisInput, a Crystal class. I don't
 * know what to make of that since Plural classes extend Crystal classes
 * all of the time with no problem, but since I need to get work done, I
 * have just copied the methods to this interface.
 * 
 * @author Kevin Bierhoff
 *
 */
public interface FractionAnalysisContext /* extends IAnalysisInput */ { 
	
	/**
	 * @return the AnnotationDatabase that was populated on all the compilation
	 * units which will be analyzed.
	 */
	public AnnotationDatabase getAnnoDB();
	
	/**
	 * @return A cache of the TACs for every method declaration, if it is available.
	 */
	public Option<CompilationUnitTACs> getComUnitTACs();

	/**
	 * @return A progress monitor for canceling the ongoing
	 * analysis, or {@link Option#none()} if it cannot be canceled.
	 * An analysis might wish to cancel the analysis if it hits an error
	 * which will cause all further results to be invalid.
	 */
	public Option<IProgressMonitor> getProgressMonitor();
	
	public StateSpaceRepository getRepository();
	
	public IInvocationCaseInstance getAnalyzedCase();

	/**
	 * @return
	 */
	public boolean assumeVirtualFrame();

}
