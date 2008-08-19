/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.flow.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.linear.PluralDisjunctiveLE;
import edu.cmu.cs.plural.states.IConstructorSignature;
import edu.cmu.cs.plural.states.IInvocationCase;
import edu.cmu.cs.plural.states.IInvocationCaseInstance;
import edu.cmu.cs.plural.states.IInvocationSignature;
import edu.cmu.cs.plural.states.IMethodSignature;
import edu.cmu.cs.plural.states.StateSpaceRepository;

/**
 * @author Nels Beckman
 * @author Kevin Bierhoff
 */
public class FractionalAnalysis extends AbstractCrystalMethodAnalysis 
		implements FractionAnalysisContext {
	
	/** 
	 * Set this to <code>true</code> to enable array assignment checks. 
	 * @see #endVisit(ArrayAccess)
	 */
	public static boolean checkArrays = false;

	private ITACFlowAnalysis<PluralDisjunctiveLE> fa;
	private static Logger logger = Logger.getLogger(FractionalAnalysis.class.getName());

	private FractionalTransfer tf;

	private MethodDeclaration currentMethod;
	private IInvocationCaseInstance analyzedCase;
	
	/**
	 * Factory method to allow subclasses to return a different subclass of
	 * FractionalTransfer. This method should ALWAYS be used for initializing
	 * <code>this.tf</code>.
	 * @return
	 */
	protected FractionalTransfer createNewFractionalTransfer() {
		return new FractionalTransfer(crystal, this);
	}
	
	/**
	 * Another factory method. Allows subclasses to also extend FractionalChecker. 
	 * @return
	 */
	protected FractionalChecker createASTWalker() {
		return new FractionalChecker();
	}
	
	protected ITACFlowAnalysis<PluralDisjunctiveLE> getFa() {
		return fa;
	}

	protected FractionalTransfer getTf() {
		return tf;
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis#analyzeMethod(org.eclipse.jdt.core.dom.MethodDeclaration)
	 */
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		if(isAbstract(d)) {
			if(logger.isLoggable(Level.FINE))
				logger.fine("Skip abstract method " + d.getName());
		}
		else {
			// only analyze methods with code in them; skip abstract methods
			currentMethod = d;
			IInvocationSignature sig = getRepository().getSignature(d.resolveBinding());
			
			for(IInvocationCase c : sig.cases()) {
				analyzedCase = c.createPermissions(true, false);
				tf = createNewFractionalTransfer();
				fa = new TACFlowAnalysis<PluralDisjunctiveLE>(tf);
				
				FractionalChecker checker = createASTWalker();
				if(sig.cases().size() > 1)
					// make sure checker prints the case in which errors occurred 
					// (if more than one case)
					checker.setErrorContext(c.toString());
				if(logger.isLoggable(Level.FINE))
					logger.fine("Results for " + d.getName() + " case " + c);
				d.accept(checker);
			}
		}
	}	
	
	@Override
	public StateSpaceRepository getRepository() {
		return StateSpaceRepository.getInstance(crystal);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.track.FractionAnalysisContext#getAnalyzedCase()
	 */
	@Override
	public IInvocationCaseInstance getAnalyzedCase() {
		return analyzedCase;
	}

	protected class FractionalChecker extends ASTVisitor {
		
		protected String errorCtx = "";
		
		public void setErrorContext(String ctx) {
			errorCtx = " (" + ctx + ")";
		}

		@Override
		public void endVisit(MethodDeclaration node) {
			if(isAbstract(node) == false) {
				final Block block = node.getBody();
				// check whether post-conditions for parameters are validated
				PluralDisjunctiveLE exit = getFa().getResultsBefore(block);
				
				// Sometimes this method is called on unreachable statements :-(
				if( exit.isBottom() ) {
					if(logger.isLoggable(Level.FINEST))
						logger.finest("no implicit exit at the end of the method body");
					return;
				}
				
				reportIfError(
						exit.checkPostConditions(block, 
								getTf().getParameterPostConditions(), 
								null /* no result value */, 
								getTf().getResultPostCondition(), 
								Collections.<Boolean, String>emptyMap()), 
						block);

				//				exit = checkParameterPostCondition(exit, block);
				// check whether post-conditions for packing are validated
//				exit = checkRecvrFieldsPostCondition(exit, block);
				
				// debug
				if(logger.isLoggable(Level.FINEST))
					logger.finest("exit: " + exit);
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(ReturnStatement node) {
			// pull results first
			PluralDisjunctiveLE exit = getFa().getResultsBefore(node);

			if(exit.isBottom()) {
				logger.warning("Bottom encountered before return: " + node);
				super.endVisit(node);
				return;
			}
			
			Variable resultVar = node.getExpression() != null ? getFa().getVariable(node.getExpression()) : null;

			// check whether post-conditions for parameters are validated
			reportIfError(
					exit.checkPostConditions(node, 
							getTf().getParameterPostConditions(), 
							resultVar, 
							getTf().getResultPostCondition(), 
							getTf().getDynamicStateTest()), 
					node);

			
//			exit = checkParameterPostCondition(exit, node);
//			// check whether post-conditions for packing are validated
//			exit = checkRecvrFieldsPostCondition(exit, node);
//			// check accuracy of dynamic state test
//			exit = checkDynamicStateTest(exit, node);
//			// check whether post-condition for return value is validated
//			if( node.getExpression() != null &&
//				tf.getResultPostCondition() != null) {
//				reportIfError(exit.checkPermissionIfNotNull(
//						fa.getVariable(node.getExpression()), tf.getResultPostCondition()),
//						node.getExpression());
//				Aliasing return_value_loc = exit.getLocationsAfter(node, fa.getVariable(node.getExpression()));
//				// If the return value is not the value null, then it must fulfill its spec.
//				if( !exit.isNull(return_value_loc) ) {
//					FractionalPermissions resultPerms = exit.get(return_value_loc);
//					
//					for(String needed : tf.getResultPostCondition().getStateInfo()) {
//						if(resultPerms.isInState(needed) == false)
//							crystal.reportUserProblem("Return value must be in state " + 
//									needed + " but is in " + resultPerms.getStateInfo(),
//									node.getExpression(), LinearChecker.this);
//					}
//					
//					FractionalPermissions resultRemainder = resultPerms.splitOff(tf.getResultPostCondition());
//					if(resultRemainder.isUnsatisfiable())
//						crystal.reportUserProblem("Return value carries no suitable permission", node.getExpression(), LinearChecker.this);
//				}
//			}

			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest("" + fa.getResultsBefore(node));
				logger.finest("  " + node);
			}
		}

		@Override
		public void endVisit(ClassInstanceCreation node) {
			final PluralDisjunctiveLE before = fa.getResultsBefore(node);
			final PluralDisjunctiveLE after = fa.getResultsAfter(node);
			final IMethodBinding constructorBinding = node.resolveConstructorBinding();
			final IConstructorSignature sig = getRepository().getConstructorSignature(constructorBinding);
			
			if(FractionalAnalysis.isBottom(before, after, node)) {
				super.endVisit(node);
				return;
			}
			
			// arguments
			int argIndex = 0;
			for(Expression e : (List<Expression>) node.arguments()) {
				final Variable arg = fa.getVariable(e);
				
//				final FractionalPermissions perms = after.get(node, arg);
//				if(perms.isUnsatisfiable())
				if(! after.checkConstraintsSatisfiable(arg))
					// TODO better reporting
					crystal.reportUserProblem(
							"" + e + " yields no suitable permission for surrounding call" + errorCtx, 
							e, FractionalAnalysis.this);
				
//				checkState(before.get(node, arg), sig.getRequiredParameterStates(argIndex), arg, e);
				reportIfError(
						before.checkStates(arg, sig.getRequiredParameterStateOptions(argIndex)),
						e);
				
				++argIndex;
			}
			
			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
				logger.finest(after.toString());
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(MethodInvocation node) {
			final PluralDisjunctiveLE before = fa.getResultsBefore(node);
			final PluralDisjunctiveLE after = fa.getResultsAfter(node);
			final IMethodBinding methodBinding = node.resolveMethodBinding();
			final IMethodSignature sig = getRepository().getMethodSignature(methodBinding);
			
			if(FractionalAnalysis.isBottom(before, after, node)) {
				super.endVisit(node);
				return;
			}
			
			// receiver
			if(sig.hasReceiver()) {
				final Variable receiver;
				if(node.getExpression() == null)
					receiver = fa.getImplicitThisVariable(methodBinding);
				else
					receiver = fa.getVariable(node.getExpression());
				
//				final FractionalPermissions permsAfter = after.get(node, receiver);
//				if(permsAfter.isUnsatisfiable())
				if(! after.checkConstraintsSatisfiable(receiver))
					// TODO better reporting
					crystal.reportUserProblem(
							"" + receiver.getSourceString() + " carries no suitable permission" + errorCtx, 
							node, FractionalAnalysis.this);
				
//				checkState(before.get(node, receiver), sig.getRequiredReceiverStates(), receiver, node);
				reportIfError(
						before.checkStates(receiver, sig.getRequiredReceiverStateOptions()),
						node);
			}
			// TODO what to do with static methods?
			
			// arguments
			int argIdx = 0;
			for(Expression e : (List<Expression>) node.arguments()) {
				Variable arg = fa.getVariable(e);
//				FractionalPermissions perms = after.get(node, arg);
//				if(perms.isUnsatisfiable())
				if(! after.checkConstraintsSatisfiable(arg))
					// TODO better reporting
					crystal.reportUserProblem(
							"" + e + " yields no suitable permission for surrounding call" + errorCtx, 
							e, FractionalAnalysis.this);
				
//				checkState(before.get(node, arg), sig.getRequiredParameterStates(argIdx), arg, e);
				reportIfError(
						before.checkStates(arg, sig.getRequiredParameterStateOptions(argIdx)),
						e);
				++argIdx;
			}

			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
				logger.finest(after.toString());
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(SuperMethodInvocation node) {
			// TODO Auto-generated method stub
			super.endVisit(node);
		}

		@Override
		public void endVisit(ArrayAccess node) {
			// check stores into arrays: was array modifiable?
			if(checkArrays  && node.getParent() instanceof Assignment) {
				Assignment store = (Assignment) node.getParent();
				if(store.getLeftHandSide().equals(node)) {
					final PluralDisjunctiveLE before = fa.getResultsBefore(store);
					final PluralDisjunctiveLE after = fa.getResultsAfter(store);

					if(FractionalAnalysis.isBottom(before, after, node)) {
						super.endVisit(node);
						return;
					}
					
					if(! after.checkConstraintsSatisfiable(fa.getVariable(node.getArray())))
						// TODO better reporting
						crystal.reportUserProblem(
								"no suitable permission for assignment to " + node + errorCtx, 
								node, FractionalAnalysis.this);
				}
			}
			super.endVisit(node);
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			// do *not* visit anonymous inner classes
			// methods of inner classes should be checked with separate "analyzeMethod" calls to FractionalAnalysis
			return false;
		}

		@Override
		public boolean visit(TypeDeclarationStatement node) {
			// do *not* visit local type declarations
			// methods of local types should be checked with separate "analyzeMethod" calls to FractionalAnalysis
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.ConstructorInvocation)
		 */
		@Override
		public void endVisit(ConstructorInvocation node) {
			final PluralDisjunctiveLE before = fa.getResultsBefore(node);
			final PluralDisjunctiveLE after = fa.getResultsAfter(node);
			final IMethodBinding constructorBinding = node.resolveConstructorBinding();
			final IConstructorSignature sig = getRepository().getConstructorSignature(constructorBinding);
			
			if(FractionalAnalysis.isBottom(before, after, node)) {
				super.endVisit(node);
				return;
			}
			
			// receiver
			final Variable receiver = fa.getImplicitThisVariable(constructorBinding);
			
//				final FractionalPermissions permsAfter = after.get(node, receiver);
//				if(permsAfter.isUnsatisfiable())
			if(! after.checkConstraintsSatisfiable(receiver))
				// TODO better reporting
				crystal.reportUserProblem(
						"this carries no suitable permission for constructor call" + errorCtx, 
						node, FractionalAnalysis.this);
			
			reportIfError(
					before.checkStates(receiver, sig.getRequiredReceiverStateOptions()),
					node);
			
			// arguments
			int argIdx = 0;
			for(Expression e : (List<Expression>) node.arguments()) {
				Variable arg = fa.getVariable(e);
//				FractionalPermissions perms = after.get(node, arg);
//				if(perms.isUnsatisfiable())
				if(! after.checkConstraintsSatisfiable(arg))
					// TODO better reporting
					crystal.reportUserProblem(
							"" + e + " yields no suitable permission for constructor call" + errorCtx, 
							e, FractionalAnalysis.this);
				
//				checkState(before.get(node, arg), sig.getRequiredParameterStates(argIdx), arg, e);
				reportIfError(
						before.checkStates(arg, sig.getRequiredParameterStateOptions(argIdx)),
						e);
				++argIdx;
			}

			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
				logger.finest(after.toString());
			}
			super.endVisit(node);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.SuperConstructorInvocation)
		 */
		@Override
		public void endVisit(SuperConstructorInvocation node) {
			final PluralDisjunctiveLE before = fa.getResultsBefore(node);
			final PluralDisjunctiveLE after = fa.getResultsAfter(node);
			final IMethodBinding constructorBinding = node.resolveConstructorBinding();
			final IConstructorSignature sig = getRepository().getConstructorSignature(constructorBinding);
			
			if(FractionalAnalysis.isBottom(before, after, node)) {
				super.endVisit(node);
				return;
			}
			
			// receiver
			// TODO use "super" variable
			final Variable receiver = fa.getImplicitThisVariable(constructorBinding);
			
//				final FractionalPermissions permsAfter = after.get(node, receiver);
//				if(permsAfter.isUnsatisfiable())
			if(! after.checkConstraintsSatisfiable(receiver))
				// TODO better reporting
				crystal.reportUserProblem(
						"super carries no suitable permission for super-constructor call" + errorCtx, 
						node, FractionalAnalysis.this);
			
			reportIfError(
					before.checkStates(receiver, sig.getRequiredReceiverStateOptions()),
					node);
			
			// arguments
			int argIdx = 0;
			for(Expression e : (List<Expression>) node.arguments()) {
				Variable arg = fa.getVariable(e);
//				FractionalPermissions perms = after.get(node, arg);
//				if(perms.isUnsatisfiable())
				if(! after.checkConstraintsSatisfiable(arg))
					// TODO better reporting
					crystal.reportUserProblem(
							"" + e + " yields no suitable permission for super-constructor call" + errorCtx, 
							e, FractionalAnalysis.this);
				
//				checkState(before.get(node, arg), sig.getRequiredParameterStates(argIdx), arg, e);
				reportIfError(
						before.checkStates(arg, sig.getRequiredParameterStateOptions(argIdx)),
						e);
				++argIdx;
			}

			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
				logger.finest(after.toString());
			}
			super.endVisit(node);
		}

		/**
		 * Reports an error to the user if the given string is non-<code>null</code>.
		 * @param errorOrNull
		 * @param node
		 */
		protected void reportIfError(String errorOrNull, ASTNode node) {
			if(errorOrNull != null)
				crystal.reportUserProblem(errorOrNull + errorCtx, node, FractionalAnalysis.this);
		}

	} // END FractionalChecker
	
	private boolean isAbstract(MethodDeclaration node) {
		return node.getBody() == null;
	}

	private static boolean isBottom(
			PluralDisjunctiveLE before,
			PluralDisjunctiveLE after,
			ASTNode node) {
		if(before.isBottom())
			logger.warning("Encountered bottom before node: " + node);
		else if(after.isBottom())
			logger.warning("Encountered bottom after node: " + node);
		else 
			return false;
		return true;
	}

}
