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

package edu.cmu.cs.plural.concurrent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;

import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.contexts.PluralContext;
import edu.cmu.cs.plural.track.FractionalAnalysis;

/**
 * Common super class for all (both) of the concurrent versions of
 * Plural. This started out as a refactoring from NIMBY to pull out
 * code that would be common to both it and SyncOrSwim.
 * 
 * @author Nels E. Beckman
 * @since May 4, 2009
 *
 */
public class ConcurrentChecker extends FractionalAnalysis {

	protected abstract class ConcurrentVisitor extends FractionalChecker {
		
		private final Set<ASTNode> previouslyReportedErrorNodes = new HashSet<ASTNode>();
		
		/**
		 * Does this receiver permission, as stored in the given lattice, require
		 * that it be protected? Can be overriden, but by default returns true for
		 * pure, full and share permission.
		 */
		protected boolean doesRcvrPermissionRequireProtection(PluralContext lattice) {
			// By default, pure/full/share
			return lattice.isRcvrFullSharePureInAnyDisjunct();
		}
		
		/**
		 * Return the mutual exclusion primitive walker associated with this object.
		 * This is specific to the subclass extending this class, so it is left
		 * abstract.
		 */
		protected abstract MutexWalker getMutexWalker();
		
		/**
		 * If the receiver should have been protected, but wasn't, what is the
		 * error message that should be returned?
		 */
		protected abstract String getUnpackedErrorMsg();
		
		/**
		 * Assert that nodes are protected and then ensure that they are protected by
		 * the same block.
		 * 
		 * Is the given {@code node} in the same mutex block as <i>every</i>
		 * node in the list, {@code nodes_where_unpacked}?
		 * 
		 * It must be true that {@code node} is inside an mutex block. All
		 * {@code nodes} must be inside mutex blocks as well.
		 * 
		 * TODO: Separate the results for, "is protected" and "is in same protection
		 * block" so that we can improve our error messages.
		 */
		protected boolean isProtectedBySameBlock(ASTNode node, List<ASTNode> nodes) {
			Option<? extends ASTNode> node_a_block = this.getMutexWalker().inWhichMutexBlockIsThisProtected(node, analysisInput);

			// False if this node is not inside an mutex block
			if( node_a_block.isNone() )
				return false;

			for( ASTNode other_node : nodes ) {
				Option<? extends ASTNode> other_node_m_block = 
					this.getMutexWalker().inWhichMutexBlockIsThisProtected(other_node, analysisInput);

				// False if any other node is not inside an mutex block
				if( other_node_m_block.isNone() ) 
					return false;

				// False if this node and that node are inside different mutex blocks
				if( !node_a_block.unwrap().equals(other_node_m_block.unwrap()) )
					return false;
			}

			return true;
		}
		
		// Template method pattern
		protected void assertProtectedIfTShared(ASTNode node) {
			// Are we unpacked after this statement?
			PluralContext lattice = ConcurrentChecker.this.getFa().getResultsAfter(node);

			// There are lots of 'expressions' and 'statements' that are not actually inside
			// of code... Also, there are real expressions that are just not inside of methods.
			if( lattice.isBottom() || Utilities.getMethodDeclaration(node) == null) return;

			if( lattice.isRcvrUnpackedInAnyDisjunct() ) {
				List<ASTNode> nodes_where_unpacked = lattice.whereWasRcvrUnpacked();

				// We require that this node be in the same atomic block as the
				// one where it was packed. inSameAtomic also asserts that nodes
				// are in any atomic block at all.
				if( !isProtectedBySameBlock(node, nodes_where_unpacked) &&
					!previouslyReportedErrorNodes.containsAll(nodes_where_unpacked) ) {

					// Is receiver permission Share/Pure/Full?
					if( doesRcvrPermissionRequireProtection(lattice) ) {

						previouslyReportedErrorNodes.add(node);
						previouslyReportedErrorNodes.addAll(nodes_where_unpacked);

						reporter.reportUserProblem(getUnpackedErrorMsg(), 
								node, 
								ConcurrentChecker.this.getName());
					}
				}

			}
		}
		
		/*
		 * TODO: We should override every important node.
		 */

		@Override
		public void endVisit(FieldAccess node) {
			super.endVisit(node);
			assertProtectedIfTShared(node);
		}

		@Override
		public void endVisit(SimpleName node) {
			super.endVisit(node);
			// SimpleName sucks because they are all over the place,
			// including in method parameter declarations. This is why
			// I first check to see if this is a field.
			
			if( node.resolveBinding() instanceof IVariableBinding && 
			  ((IVariableBinding)node.resolveBinding()).isField() ) {
				assertProtectedIfTShared(node);
			}
		}

		@Override
		public void endVisit(Assignment node) {
			super.endVisit(node);
			assertProtectedIfTShared(node);
		}

		@Override
		public void endVisit(ReturnStatement node) {
			super.endVisit(node);
			assertProtectedIfTShared(node);
		}
	}
}
