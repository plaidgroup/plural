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
package edu.cmu.cs.plural.concurrent.nimby;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.concurrent.MutexWalker;

/**
 * This analysis is a wrapper for a visitor that records whether or not a
 * particular node in the AST is statically known to be inside an atomic block.
 * The design of this class is patterned after Ciera's Loop Counter analysis,
 * except mine is even simpler because there is only one stmt I care about.
 * This analysis is meant to be run on-demand, and hopefully will have a low
 * overhead.
 * 
 * @author Nels Beckman
 * @date Mar 4, 2008
 *
 */
public class IsInAtomicAnalysis extends AbstractCrystalMethodAnalysis 
	implements	MutexWalker {

	/*
	 * Should be a set. There is no generic weak hash set. Weakness is important
	 * because we don't want to hold references to the AST forever, although likely
	 * the map will be reset on a regular basis.
	 */
	private Map<ASTNode, Option<LabeledStatement>> nodesInsideAtomic = 
		new WeakHashMap<ASTNode, Option<LabeledStatement>>(); 
	/*
	 * Methods we've previously analyzed. Again, a map posing as a set.
	 */
	private Map<MethodDeclaration, Object> analyzedMethods = new WeakHashMap<MethodDeclaration, Object>();
	
	public IsInAtomicAnalysis() {
		
	}

	/**
	 * Is the given node statically nested inside of an atomic block?<br>
	 * e.g.)<br>
	 * False!
	 * <pre>
	 *   foo() {
	 *   	node
	 *   }
	 * </pre>
	 * True!
	 * <pre>
	 *   foo() {
	 *     atomic: {
	 *       node
	 *     }
	 *   }
	 * </pre>
	 * @param node
	 * @return
	 */
	public boolean isInAtomicBlock(ASTNode node) {
		return inWhichAtomicBlock(node).isSome();
	}
	
	/**
	 * Which (if any) atomic block is the given node lexical contained
	 * within?
	 * @return {@code SOME(labeled_stmt)} if {@code node} is inside of
	 * an atomic block. Otherwise {@code NONE}.
	 */
	public Option<LabeledStatement> inWhichAtomicBlock(ASTNode node) {
		if( node == null ) {
			return Option.none();
		}
		else {
			final MethodDeclaration methodDecl = Utilities.getMethodDeclaration(node);
			this.analyzeMethod(methodDecl);
			
			if( this.nodesInsideAtomic.containsKey(node) )
				return this.nodesInsideAtomic.get(node);
			else
				return Option.none();
		}
	}
	
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		assert(d != null);
		
		if( !analyzedMethods.containsKey(d) ) {
			d.accept(new AtomicCheckVisitor());
			analyzedMethods.put(d, null);
		}
	}

	static private boolean isAtomicBlock(LabeledStatement labeledStmt) {
		return labeledStmt.getLabel().getIdentifier().equals("atomic");
	}
	
	/**
	 * Code that will visit an ast recording which node is in which
	 * atomic block. This code is designed based on the assumption
	 * that atomic blocks cannot be lexically nested, which for the
	 * time-being they cannot since Java labels cannot be lexically
	 * nested.
	 */
	private class AtomicCheckVisitor extends ASTVisitor {
		private Option<LabeledStatement> atomicBlock = Option.none();
		
		@Override
		public boolean visit(LabeledStatement node) {
			if( isAtomicBlock(node) ) {
				atomicBlock = Option.some(node);
			}
			return true;
		}

		@Override
		public void endVisit(LabeledStatement node) {
			if( isAtomicBlock(node) ) {
				atomicBlock = Option.none();
			}
		}

		@Override
		public void preVisit(ASTNode node) {
			/*
			 * You are an atomic node if we are inside at least one atomic
			 * block. No entry for nodes that are not inside.
			 */
			if( this.atomicBlock.isSome() ) {
				IsInAtomicAnalysis.this.nodesInsideAtomic.put(node, this.atomicBlock);
			}
			super.preVisit(node);
		}	
	}

	@Override
	public Option<? extends ASTNode> inWhichMutexBlock(ASTNode node) {
		return this.inWhichAtomicBlock(node);
	}	
}