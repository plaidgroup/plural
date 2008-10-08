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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
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
		boolean pure = analysisInput.getAnnoDB().getSummaryForMethod(d.resolveBinding()).
				getReturn("edu.cmu.cs.plural.annot.NoEffects") != null;
		if(pure)
			d.accept(new PurityVisitor());
	}
	
	private class PurityVisitor extends ASTVisitor {
		
		private ITypeBinding declaringType;

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
						// TODO make sure this is an implicit access to unqualified "this"
						receiver = declaringType.isSubTypeCompatible(var.getDeclaringClass());
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
			default:
				return;
			}
			EffectChecker.this.reporter.reportUserProblem(
					"Assignment in method with no effects forbidden", assignment, EffectChecker.this.getName());
		}

	}

}
