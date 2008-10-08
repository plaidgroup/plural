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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
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
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;

/**
 * This is a simple Crystal checker to make sure that methods and constructors
 * that carry the @NoEffects annotation have in fact no effects.  Any store into memory
 * counts as an effect, except stores to receiver fields in a constructor.
 * @author Kevin Bierhoff
 * @since Oct 7, 2008
 */
public class EffectChecker extends AbstractCrystalMethodAnalysis {
	
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		IMethodBinding binding = d.resolveBinding();
		if(isPure(binding) && ! Modifier.isAbstract(binding.getModifiers()))
			// needs checked
			d.accept(new PurityVisitor(binding.getDeclaringClass(),
					d.isConstructor() /* permit receiver assignment in constructor */));
		// else method can do whatever it wants
	}
	
	/**
	 * Helper method to report a problem for the given node to the user.
	 * @param problemDescription
	 * @param node
	 */
	private void reportUserProblem(String problemDescription, ASTNode node) {
		reporter.reportUserProblem(problemDescription, node, this.getClass().getName());
	}

	/**
	 * Determines whether the given method is declared to have no effects.
	 * @param methodBinding
	 * @return <code>true</code> if the given method should have no effects,
	 * <code>false</code> otherwise.
	 */
	private boolean isPure(IMethodBinding methodBinding) {
		if(analysisInput.getAnnoDB().getSummaryForMethod(methodBinding).
				getReturn("edu.cmu.cs.plural.annot.NoEffects") != null)
			return true;
		
		if(methodBinding.getDeclaringClass().getSuperclass() == null)
			// method in java.lang.Object
			return false;
		
		if(methodBinding.isDefaultConstructor()) {
			// for compiler-generated default constructors, find the invoked super-constructor
			// this does not work for constructors found in class files b/c those are not
			// known to be compiler-generated according to isDefaultConstructor javadoc
			for(IMethodBinding m : methodBinding.getDeclaringClass().getSuperclass().getDeclaredMethods()) {
				if(m.isConstructor() && m.getParameterTypes().length == 0)
					return isPure(m);
			}
		}
		
		if(methodBinding.isConstructor() || Modifier.isStatic(methodBinding.getModifiers()))
			// constructors and static methods cannot inherit spec's
			return false;

		// walk super-types to see if the method is declared to be pure somewhere
		Set<ITypeBinding> seen = new HashSet<ITypeBinding>();
		seen.add(methodBinding.getDeclaringClass());
		if(isPure(methodBinding.getDeclaringClass().getSuperclass(), methodBinding, seen))
			return true;
		for(ITypeBinding itf : methodBinding.getDeclaringClass().getInterfaces()) {
			if(isPure(itf, methodBinding, seen))
				return true;
		}
		return false;
	}
	
	/**
	 * Helper method for walking supertypes looking for @NoEffects annotations.
	 * @param type
	 * @param methodBinding
	 * @param seen Set of types that were already visited and need not be checked again.
	 * @return <code>true</code> if a method in the given type that the given 
	 * method overrides is declared to have no effects, <code>false</code> if
	 * the given type was <code>null</code>, contained in seen, or did not contain
	 * any methods declared to have no effects that the given one overrides.
	 */
	private boolean isPure(ITypeBinding type, IMethodBinding methodBinding,
			Set<ITypeBinding> seen) {
		if(type == null || seen.contains(type))
			return false;
		seen.add(type);
		for(IMethodBinding m : type.getDeclaredMethods()) {
			if(methodBinding.isSubsignature(m)) {
				if(analysisInput.getAnnoDB().getSummaryForMethod(m).
						getReturn("edu.cmu.cs.plural.annot.NoEffects") != null)
					return true;
			}
		}
		if(isPure(type.getSuperclass(), methodBinding, seen))
			return true;
		for(ITypeBinding itf : type.getInterfaces()) {
			if(isPure(itf, methodBinding, seen))
				return true;
		}
		return false;
	}

	/**
	 * This visitor finds stores into memory and impure invocations in the visited 
	 * AST.  It is intended to be called separately for each method body.  
	 * This visitor does not descend into anonymous inner and local classes,
	 * but it would descend into top-level and inner classes.    
	 * @author Kevin Bierhoff
	 * @since Oct 7, 2008
	 */
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
				EffectChecker.this.reportUserProblem(
						"Assignment in method with no effects forbidden", 
						assignment);
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
				EffectChecker.this.reportUserProblem(
						"Effectful invocation in method with no effects forbidden", 
						invocation);
		}
		
		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			// do *not* visit anonymous inner classes
			// methods of inner classes should be visited separately
			return false;
		}

		@Override
		public boolean visit(TypeDeclarationStatement node) {
			// do *not* visit local type declarations
			// methods of inner classes should be visited separately
			return false;
		}

	}

}
