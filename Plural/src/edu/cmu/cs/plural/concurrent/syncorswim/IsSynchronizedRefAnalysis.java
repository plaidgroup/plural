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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.util.Option;

/**
 * An analysis that will determine statically which references are
 * protected by synchronized blocks and which are not at a given
 * location. This analysis will be later queried by the
 * SyncOrSwim analysis to determine which references can be
 * remembered and which can be forgotten. This analysis only works
 * on final variables, including 'this.'
 * 
 * @author Nels E. Beckman
 * @since Apr 10, 2009
 */
public final class IsSynchronizedRefAnalysis extends
		AbstractCrystalMethodAnalysis {

	
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		// What are the synchronized variables at a given node?
		Map<ASTNode, NodeTree> syncedVarsAtNode = 
			new HashMap<ASTNode, NodeTree>();
		NodeTree initialTree;
		
		// First, look at flags to determine if 'this'
		// is synchronized.
		if(  Modifier.isSynchronized(d.getModifiers()) && 
			!Modifier.isStatic(d.getModifiers())) {
			// Add 'this' to the list of synched things.
			initialTree = new ExtensionTree(EMPTY_TREE_INSTANCE, 
					new SynchronizedThis());
		}
		else {
			initialTree = EMPTY_TREE_INSTANCE;
		}
		
		// Modifies our map in place!
		d.accept(new IsSynchronizedVisitor(syncedVarsAtNode, initialTree));
	}
	
	// Is the given expression, 'this'?
	static boolean isThis(Expression expr) {
		if( expr instanceof ThisExpression ) {
			return true;
		}
		return false;
	}
	
	/**
	 * If this is a field or variable, return the variable biding, else NONE.
	 */
	static Option<IVariableBinding> asVar(Expression expr) {
		if( expr instanceof FieldAccess ) {
			FieldAccess field = (FieldAccess)expr;
			return Option.some(field.resolveFieldBinding());
		} else if( expr instanceof SimpleName ) {
			SimpleName name = (SimpleName)expr;
			if( name.resolveBinding() instanceof IVariableBinding ) {
				IVariableBinding binding = (IVariableBinding)name.resolveBinding();
				return Option.some(binding);
			}
		} else if( expr instanceof Name ) {
			Name name = (Name)expr;
			if( name.resolveBinding() instanceof IVariableBinding ) {
				IVariableBinding binding = (IVariableBinding)name.resolveBinding();
				return Option.some(binding);
			}
		} else if( expr instanceof QualifiedName ) {
			QualifiedName qname = (QualifiedName)expr;
			if( qname.resolveBinding() instanceof IVariableBinding ) {
				IVariableBinding binding = (IVariableBinding)qname.resolveBinding();
				return Option.some(binding);
			}
		}
		
		// If we get down to here, it wasn't a field...
		return Option.none();
	}
	
	/**
	 * Visitor for synchronized blocks. Goes through each node in
	 * the ast, when encountering a synchronized block it records
	 * the fact that it has encountered one.
	 */
	private static class IsSynchronizedVisitor extends ASTVisitor {

		// Map from nodes to NodeTrees that gets updated in place
		private final Map<ASTNode, NodeTree> syncedVarsAtNode;
		private final NodeTree nodeTree;
		
		/**
		 * @param syncedVarsAtNode
		 * @param initialTree
		 */
		public IsSynchronizedVisitor(Map<ASTNode, NodeTree> syncedVarsAtNode,
				NodeTree initialTree) {
			this.syncedVarsAtNode = syncedVarsAtNode;
			this.nodeTree = initialTree;
		}

		@Override
		public void preVisit(ASTNode node) {
			// This method is called right before visit at every node.
			syncedVarsAtNode.put(node, nodeTree);
		}

		@Override
		public boolean visit(SynchronizedStatement node) {
			// Only works if sync expr is a variable or this.
			// All others will be reported as a warning.
			Expression sync_expr = node.getExpression();
			Option<IVariableBinding> var_ = asVar(sync_expr);
			
			if( var_.isSome() ) {
				// recur with var added to the node tree
				SynchronizedVar var = new SynchronizedFieldLocal(var_.unwrap());
				NodeTree extesion = new ExtensionTree(this.nodeTree, var);
				IsSynchronizedVisitor visitor = 
					new IsSynchronizedVisitor(this.syncedVarsAtNode, extesion);
				
				node.getExpression().accept(visitor);
				node.getBody().accept(visitor);
				return false;
			} else if( isThis(sync_expr) ) {
				// recur with var added to the node tree
				SynchronizedVar thiz = new SynchronizedThis();
				NodeTree extension= new ExtensionTree(this.nodeTree, thiz);
				IsSynchronizedVisitor visitor = 
					new IsSynchronizedVisitor(this.syncedVarsAtNode, extension);
				
				node.getExpression().accept(visitor);
				node.getBody().accept(visitor);
				return false;
			} else {
				return true;
			}
		}
		
	}
		
	/**
	 * NodeTree is a memory-efficient way of storing which references have
	 * been synchronized at each point. It works a lot like a cons list. Because
	 * most nodes will be synchronized on exactly the same references as their
	 * parents, or the same references plus one additional reference, NodeTrees
	 * are designed to be shared by many different nodes.
	 */
	private abstract static class NodeTree {
		abstract public boolean isSynced(SynchronizedVar v);
	}
	// I am feeling really happy!
	private static final EmptyTree EMPTY_TREE_INSTANCE = new EmptyTree();
	
	private static class EmptyTree extends NodeTree {
		@Override public boolean isSynced(SynchronizedVar v) { return false; }
	}
	
	private static class ExtensionTree extends NodeTree {
		private final NodeTree parent;
		private final SynchronizedVar syncedVar;
		
		public ExtensionTree(NodeTree parent, SynchronizedVar syncedVar) {
			this.parent = parent;
			this.syncedVar = syncedVar;
		}

		@Override
		public boolean isSynced(SynchronizedVar v) {
			if( v.equals(this.syncedVar) )
				return true;
			else
				return this.parent.isSynced(v);
		}
	}
}