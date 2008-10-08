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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;

/**
 * @author Kevin Bierhoff
 * @since Oct 7, 2008
 *
 */
public class EffectChecker extends AbstractCrystalMethodAnalysis {
	
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		if(isPure(d.resolveBinding()))
			d.accept(new PurityVisitor(d.resolveBinding().getDeclaringClass(),
					d.isConstructor() /* permit receiver assignment in constructor */));
	}
	
	private boolean isPure(IMethodBinding methodBinding) {
		boolean pure = analysisInput.getAnnoDB().getSummaryForMethod(methodBinding).
				getReturn("edu.cmu.cs.plural.annot.NoEffects") != null;
		return pure;
	}
	
	private class PurityVisitor extends ASTVisitor {
		
		private final ITypeBinding declaringType;
		private final boolean permitReceiverAssignment;
		
		public PurityVisitor(ITypeBinding declaringType) {
			this(declaringType, false);
		}
		public PurityVisitor(ITypeBinding declaringType, boolean permitReceiverAssignment) {
			this.declaringType = declaringType;
			this.permitReceiverAssignment = permitReceiverAssignment;
		}

		@Override
		public void endVisit(Assignment node) {
			checkAssignmentTarget(node.getLeftHandSide(), node);
			super.endVisit(node);
		}

		@Override
		public void endVisit(PostfixExpression node) {
			checkAssignmentTarget(node.getOperand(), node);
			super.endVisit(node);
		}

		@Override
		public void endVisit(PrefixExpression node) {
			checkAssignmentTarget(node.getOperand(), node);
			super.endVisit(node);
		}
		
		/**
		 * Checks whether the given assignment target represents a memory location
		 * and reports a warning if so.
		 * In other words, this method checks whether the given assignment is a store.
		 * @param target
		 * @param assignment
		 */
		private void checkAssignmentTarget(Expression target, ASTNode assignment) {
			boolean receiver = false;
			switch(target.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				IBinding b = ((Name) target).resolveBinding();
				if(b instanceof IVariableBinding) {
					IVariableBinding var = (IVariableBinding) b;
					if(var.isField()) {
						receiver = ! Modifier.isStatic(var.getModifiers()) &&
								isDefinedInDeclaringType(var);
						// error: assignment to field
						break;
					}
				}
				return;
			case ASTNode.QUALIFIED_NAME:
				b = ((Name) target).resolveBinding();
				if(b instanceof IVariableBinding) {
					IVariableBinding var = (IVariableBinding) b;
					if(var.isField()) {
						receiver = false; // cannot be this.f
						// error: assignment to field
						break;
					}
				}
				return;
			case ASTNode.FIELD_ACCESS:
				Expression obj = ((FieldAccess) target).getExpression();
				receiver = (obj.getNodeType() == ASTNode.THIS_EXPRESSION && ((ThisExpression) obj).getQualifier() == null);
				// error by definition
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				receiver = true; // receiver access by definition
				// error by definition
				break;
			case ASTNode.ARRAY_ACCESS:
				receiver = false; // receiver cannot be array
				// error by definition
				break;
			default:
				return;
			}
			
			if(!receiver || !permitReceiverAssignment) {
				EffectChecker.this.reporter.reportUserProblem(
						"Assignment in method with no effects forbidden", 
						assignment, 
						EffectChecker.this.getName());
			}
		}

		/**
		 * Walks {@link #declaringType} and its super-classes to
		 * find the given field declaration 
		 * @param var
		 * @return <code>true</code> if {@link #declaringType} or its
		 * superclasses declare the given field, <code>false</code> otherwise.
		 */
		private boolean isDefinedInDeclaringType(
				IVariableBinding var) {
			ITypeBinding type = declaringType;
			while(type != null) {
				for(IVariableBinding f : type.getDeclaredFields()) {
					if(f.equals(var))
						return true;
				}
				type = type.getSuperclass();
			}
			return false;
		}
		@Override
		public void endVisit(ClassInstanceCreation node) {
			checkInvocation(node.resolveConstructorBinding(), node);
			super.endVisit(node);
		}
		@Override
		public void endVisit(ConstructorInvocation node) {
			checkInvocation(node.resolveConstructorBinding(), node);
			super.endVisit(node);
		}
		@Override
		public void endVisit(MethodInvocation node) {
			checkInvocation(node.resolveMethodBinding(), node);
			super.endVisit(node);
		}
		@Override
		public void endVisit(SuperConstructorInvocation node) {
			checkInvocation(node.resolveConstructorBinding(), node);
			super.endVisit(node);
		}
		@Override
		public void endVisit(SuperMethodInvocation node) {
			checkInvocation(node.resolveMethodBinding(), node);
			super.endVisit(node);
		}
		
		private void checkInvocation(IMethodBinding methodBinding, ASTNode invocation) {
			if(!EffectChecker.this.isPure(methodBinding))
				EffectChecker.this.reporter.reportUserProblem(
						"Effectful invocation in method with no effects forbidden", 
						invocation, EffectChecker.this.getName());
		}
		
	}

}
