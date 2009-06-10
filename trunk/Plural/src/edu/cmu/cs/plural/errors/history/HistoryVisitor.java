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

package edu.cmu.cs.plural.errors.history;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.util.Box;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.plural.contexts.ContextChoiceLE;
import edu.cmu.cs.plural.contexts.FalseContext;
import edu.cmu.cs.plural.contexts.LinearContext;
import edu.cmu.cs.plural.contexts.PluralContext;
import edu.cmu.cs.plural.contexts.TensorContext;
import edu.cmu.cs.plural.contexts.TrueContext;
import edu.cmu.cs.plural.errors.ChoiceID;
import edu.cmu.cs.plural.linear.DisjunctiveVisitor;

/**
 * A history visitor visits just about every potentially interesting
 * AST node in a method declaration, and then grabs the LinearContext
 * at each point. Then when that is done, it builds a 
 * {@link SingleCaseHistoryTree} that we can print out in the tree view.
 * 
 * @author Nels E. Beckman
 * @since Jun 2, 2009
 *
 */
class HistoryVisitor {

	/** Maps the id of that node to the node itself. */
	private final IDHistoryNodeMap idToNodeMap;
	private Option<HistoryNode> rootContext = Option.none();
	
	/**
	 * Restricted access. {@link HistoryVisitor#visitAndBuildTree(MethodDeclaration, TACFlowAnalysis)}
	 */
	private HistoryVisitor() {
		this.idToNodeMap = new IDHistoryNodeMap();
	}

	/**
	 * Given a method and an analysis, runs the analysis, building a
	 * tree along the way and returning it. It also returns the root
	 * linear context.
	 */
	public static Pair<HistoryNode, SingleCaseHistoryTree> visitAndBuildTree(
			final MethodDeclaration method_decl,
			final TACFlowAnalysis<PluralContext> analysis) {
		HistoryVisitor visitor = new HistoryVisitor();
		visitor.visit(method_decl, analysis);
		return visitor.buildTree();
	}

	private LinearContext extractSingleContext(TACFlowAnalysis<PluralContext> analysis, ASTNode node) {
		PluralContext ctx = analysis.getResultsBefore(node);
		LinearContext l_ctx = ctx.getLinearContext();
		
		// For now, to see what kind of contexts we can get, make
		// assert there was no choice.
		Boolean was_singleton =
		l_ctx.dispatch(new DisjunctiveVisitor<Boolean>(){
			@Override public Boolean falseContext(FalseContext falseContext) { return true; }
			@Override public Boolean trueContext(TrueContext trueContext) { return true; }
			@Override public Boolean choice(ContextChoiceLE le) { return false; }
			@Override public Boolean context(TensorContext le) { return true;	}				
		});
		assert(was_singleton);
		
		return l_ctx;
	}
	
	/**
	 * Populates a history tree, which must be done after
	 * the visitor has been called.
	 */
	private Pair<HistoryNode, SingleCaseHistoryTree> buildTree() {
		assert(this.rootContext.isSome());
		HistoryNode root_context = this.rootContext.unwrap();
		SingleCaseHistoryTree tree = SingleCaseHistoryTree.buildTree(this.idToNodeMap);
		return Pair.create(root_context, tree);
	}

	/**
	 * Does the visiting, creating a new visitor class which
	 * will populate {@link HistoryVisitor#idToNodeMap}.
	 */
	private void visit(MethodDeclaration method_decl,
			TACFlowAnalysis<PluralContext> analysis) {
		// So we expect there to be one choice only at the beginning node 
		LinearContext root_context = this.extractSingleContext(analysis, method_decl);
		
		
		HistoryNode root_node = new HistoryNode(root_context);
		this.rootContext = Option.some(root_node);
		this.idToNodeMap.put(root_context);
		
		method_decl.getBody().accept((new HelperVisitor(analysis)));
	}
	
	/** 
	 * The helper visitor does the hard work. It's a private
	 * member class so that it can 
	 */
	private class HelperVisitor extends ASTVisitor {
		private final TACFlowAnalysis<PluralContext> analysis;
		
		HelperVisitor(TACFlowAnalysis<PluralContext> analysis) {
			this.analysis = analysis;
		}

		// All of the following methods exist so that we will not descend into
		// nested classes.
		@Override public boolean visit(AnonymousClassDeclaration node) {return false;}
		@Override public boolean visit(TypeDeclaration node) {return false;}
		@Override public boolean visit(TypeDeclarationStatement node) {return false;}
		
		/** 
		 * This method pulls out a mapping from Choice IDs to history nodes from
		 * the linear context BEFORE the given ast node. Additionally, it looks
		 * for linear contexts that have the same choice id as the current
		 * {@code HistoryVisitor#rootContext}, and if it finds them, returns
		 * it.
		 */
		private Pair<IDHistoryNodeMap, Option<HistoryNode>> 
		extractAllChoicesAfterNode(ASTNode node) {
			PluralContext ctx = analysis.getResultsAfter(node);
			LinearContext l_ctx = ctx.getLinearContext();
						
			// Dispatch on linear context, adding only singleton contexts, and not
			// choice contexts, to the result map.
			final Option<HistoryNode> cur_root_node = HistoryVisitor.this.rootContext;
			final Option<ChoiceID> root_id = cur_root_node.isSome() ? 
					Option.some(cur_root_node.unwrap().getChoiceID()) :
					Option.<ChoiceID>none();
			final Box<Option<HistoryNode>> result_root_node = 
				Box.box(Option.<HistoryNode>none());
			IDHistoryNodeMap result =
				l_ctx.dispatch(new DisjunctiveVisitor<IDHistoryNodeMap>() {
					private IDHistoryNodeMap singleton(LinearContext c) {
						// We need to continue to build up the root node if 
						// we encounter nodes that have the same id.
						if( root_id.isSome() && root_id.unwrap().equals(c.getChoiceID()) ) {
							if( result_root_node.getValue().isNone() ) {
								result_root_node.setValue(Option.some(new HistoryNode(c)));
							}
							else {
								HistoryNode prev_node = result_root_node.getValue().unwrap();
								prev_node.append(c);
							}
						}
							
						return IDHistoryNodeMap.singleton(c);
					}
					
					@Override
					public IDHistoryNodeMap falseContext(FalseContext falseContext) {
						return singleton(falseContext);
					}

					@Override
					public IDHistoryNodeMap trueContext(TrueContext trueContext) {
						return singleton(trueContext);
					}

					@Override
					public IDHistoryNodeMap context(TensorContext le) {
						return singleton(le);
					}
					
					@Override
					public IDHistoryNodeMap choice(	ContextChoiceLE le) {
						IDHistoryNodeMap result = new IDHistoryNodeMap();
						for(  LinearContext ctx : le.getElements() ) {
							IDHistoryNodeMap choice = ctx.dispatch(this);
							result.merge(choice);
						}
						return result;
					}
				});
			
			return Pair.create(result, result_root_node.getValue());
		}
		
		@Override
		public void endVisit(ConstructorInvocation node) {
			
			Pair<IDHistoryNodeMap, Option<HistoryNode>> ctxs = extractAllChoicesAfterNode(node);
			HistoryVisitor.this.idToNodeMap.merge(ctxs.fst());
			
			if( ctxs.snd().isSome() ) {
				HistoryNode additional_root = ctxs.snd().unwrap();
				HistoryVisitor.this.rootContext.unwrap().concat(additional_root);
			}
		}

		@Override
		public void endVisit(FieldAccess node) {
			Pair<IDHistoryNodeMap, Option<HistoryNode>> ctxs = extractAllChoicesAfterNode(node);
			HistoryVisitor.this.idToNodeMap.merge(ctxs.fst());
			
			if( ctxs.snd().isSome() ) {
				HistoryNode additional_root = ctxs.snd().unwrap();
				HistoryVisitor.this.rootContext.unwrap().concat(additional_root);
			}
		}

		@Override
		public void endVisit(MethodInvocation node) {
			Pair<IDHistoryNodeMap, Option<HistoryNode>> ctxs = extractAllChoicesAfterNode(node);
			HistoryVisitor.this.idToNodeMap.merge(ctxs.fst());
			
			if( ctxs.snd().isSome() ) {
				HistoryNode additional_root = ctxs.snd().unwrap();
				HistoryVisitor.this.rootContext.unwrap().concat(additional_root);
			}
		}

		@Override
		public void endVisit(ReturnStatement node) {
			Pair<IDHistoryNodeMap, Option<HistoryNode>> ctxs = extractAllChoicesAfterNode(node);
			HistoryVisitor.this.idToNodeMap.merge(ctxs.fst());
			
			if( ctxs.snd().isSome() ) {
				HistoryNode additional_root = ctxs.snd().unwrap();
				HistoryVisitor.this.rootContext.unwrap().concat(additional_root);
			}
		}

		@Override
		public void endVisit(SimpleName node) {
			// This is just to catch any other field accesses.
			if( !(node.resolveBinding() instanceof IVariableBinding) )
				return;
			
			IVariableBinding binding = (IVariableBinding)node.resolveBinding();
			if( !binding.isField() )
				return;
			
			Pair<IDHistoryNodeMap, Option<HistoryNode>> ctxs = extractAllChoicesAfterNode(node);
			HistoryVisitor.this.idToNodeMap.merge(ctxs.fst());
			
			if( ctxs.snd().isSome() ) {
				HistoryNode additional_root = ctxs.snd().unwrap();
				HistoryVisitor.this.rootContext.unwrap().concat(additional_root);
			}
		}
	}
}