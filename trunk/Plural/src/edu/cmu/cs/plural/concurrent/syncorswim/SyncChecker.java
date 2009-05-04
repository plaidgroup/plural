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

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SynchronizedStatement;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.util.Option;

/**
 * This analysis is meant to warn users of places where our analysis
 * doesn't actually work. For example, if you try to synchronize on
 * the result of a function call, or a non-final variable, we will
 * report a warning to the user.
 * 
 * @author Nels E. Beckman
 * @since May 1, 2009
 */
public class SyncChecker extends AbstractCrystalMethodAnalysis {

	private static final String ANALYSIS_NAME = "Sync or Swim";
	
	/**
	 * Checks to see if the given method declaration is both static
	 * and synchronized.
	 */
	private boolean isStaticSynced(MethodDeclaration d) {
		return
			Modifier.isStatic(d.getModifiers()) &&
			Modifier.isSynchronized(d.getModifiers());
	}
	
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		// We don't track syncs on statics, so report a warning
		if( isStaticSynced(d) ) {
			final String description = "This analysis does not track" +
				" synchronization on static methods.";
			this.reporter.reportUserProblem(description, d, ANALYSIS_NAME);
		}
		
		d.accept(new SyncCheckerVisitor());
	}

	private class SyncCheckerVisitor extends ASTVisitor {
		 
		
		@Override
		public void endVisit(SynchronizedStatement node) {
			Option<IVariableBinding> var_ =
				IsSynchronizedRefAnalysis.asVar(node.getExpression());
			boolean is_this = 
				IsSynchronizedRefAnalysis.isThis(node.getExpression());
			
			
			if( var_.isNone() && !is_this ) {
				// If you are synchronizing on neither this nor a variable,
				// we can't help you!
				final String description = "This analysis only supports synchronization" +
						" statements that synchronize on 'this,' a field or a local variable.";
				SyncChecker.this.reporter.reportUserProblem(description, 
						node.getExpression(), ANALYSIS_NAME);
			}
			
			if( var_.isSome() ) {
				IVariableBinding var = var_.unwrap();
				if( !Modifier.isFinal(var.getModifiers()) ) {
					// We only support synchronization on final variables
					final String description = "This analysis only supports synchronization" +
						    " on final variables and fields, as well as 'this.'";
					SyncChecker.this.reporter.reportUserProblem(description, 
							node.getExpression(), ANALYSIS_NAME);
				}
			}
		}		
	}
}