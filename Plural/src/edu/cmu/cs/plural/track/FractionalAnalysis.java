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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.eclipse.CompilationUnitTACs;
import edu.cmu.cs.crystal.tac.eclipse.EclipseTAC;
import edu.cmu.cs.crystal.tac.model.SourceVariable;
import edu.cmu.cs.crystal.tac.model.SuperVariable;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.contexts.PluralContext;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedTypeAnalysis;
import edu.cmu.cs.plural.polymorphic.instantiation.RcvrInstantiationPackage;
import edu.cmu.cs.plural.states.IConstructorSignature;
import edu.cmu.cs.plural.states.IInvocationCase;
import edu.cmu.cs.plural.states.IInvocationCaseInstance;
import edu.cmu.cs.plural.states.IInvocationSignature;
import edu.cmu.cs.plural.states.IMethodSignature;
import edu.cmu.cs.plural.states.MethodCheckingKind;
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

	private ITACFlowAnalysis<PluralContext> fa;
	private static Logger logger = Logger.getLogger(FractionalAnalysis.class.getName());

	private FractionalTransfer tf;

	private IInvocationCaseInstance analyzedCase;
	
	private boolean assumeVirtualFrame;

	public FractionalAnalysis() {
		super();
	}

	/**
	 * Factory method to allow subclasses to return a different subclass of
	 * FractionalTransfer. This method should ALWAYS be used for initializing
	 * <code>this.tf</code>.
	 * @return
	 */
	protected FractionalTransfer createNewFractionalTransfer() {
		return new FractionalTransfer(analysisInput, this);
	}
	
	/**
	 * Another factory method. Allows subclasses to also extend FractionalChecker. 
	 * @param d 
	 * @return
	 */
	protected FractionalChecker createASTWalker(MethodDeclaration d) {
		return new FractionalChecker(d);
	}
	
	protected ITACFlowAnalysis<PluralContext> getFa() {
		return fa;
	}

	protected FractionalTransfer getTf() {
		return tf;
	}
	
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		if(isAbstract(d)) {
			if(logger.isLoggable(Level.FINE))
				logger.fine("Skip abstract method " + d.getName());
		}
		else {
			// only analyze methods with code in them; skip abstract methods
			IInvocationSignature sig = getRepository().getSignature(d.resolveBinding());
			int classFlags = sig.getSpecifiedMethodBinding().getDeclaringClass().getModifiers();
			for(IInvocationCase c : sig.cases()) {
//				boolean requiresVirtualFrameCheck = c.isVirtualFrameSpecial();
				final boolean isFinalClass = Modifier.isFinal(classFlags);
				final boolean isAbstractClass = Modifier.isAbstract(classFlags);
				final boolean isStaticMethod = Modifier.isStatic(d.getModifiers());
				if(isStaticMethod || (!isFinalClass && !isAbstractClass && !c.isVirtualFrameSpecial()))
					// no separate checks for virtual frame needed
					// static methods are analyzed once b/c they don't have a receiver
					// !isFinalClass condition prevents spurious warning for current != virtual case
					// !isAbstractClass condition doesn't seem necessary (since that's the default case)
					// but will insert "assuming receiver is a subclass" into error msgs.
					analyzeCase(d, sig, c, null);
				else {
					if(!isFinalClass) 
						// can have subclasses: test assuming current != virtual frame
						analyzeCase(d, sig, c, false);
					if(!isAbstractClass) 
						// can have instances: test assuming current == virtual frame
						analyzeCase(d, sig, c, true);
				}
			}
		}
	}
	
	/**
	 * @param d
	 * @param sig
	 * @param c
	 * @param assumeVirtualFrame <code>null</code> if virtual frame doesn't need
	 * to be distinguished, <code>false</code> if analyzed != runtime type of the
	 * receiver, <code>true</code> if analyzed == runtime type of the receiver.
	 */
	private void analyzeCase(MethodDeclaration d, IInvocationSignature sig,
			IInvocationCase c, Boolean assumeVirtualFrame) {
		this.assumeVirtualFrame = assumeVirtualFrame != null && assumeVirtualFrame;
		MethodCheckingKind checkingKind = 
			MethodCheckingKind.methodCheckingKindImpl(d.isConstructor(), this.assumeVirtualFrame);
		
		analyzedCase = c.createPermissions(checkingKind, true, 
				this.assumeVirtualFrame, Option.<RcvrInstantiationPackage>none());
		tf = createNewFractionalTransfer();
		
		// need local to be able to set monitor
		TACFlowAnalysis<PluralContext> temp; 
		fa = temp = new TACFlowAnalysis<PluralContext>(getTf(),
				this.analysisInput.getComUnitTACs().unwrap());
		temp.setMonitor(analysisInput.getProgressMonitor());
		
		FractionalChecker checker = createASTWalker(d);
		if(sig.cases().size() > 1) {
			// make sure checker prints the case in which errors occurred 
			// (if more than one case)
			if(assumeVirtualFrame != null) {
				if(assumeVirtualFrame)
					checker.setErrorContext(c.toString() + " assuming receiver has analyzed type");
				else
					checker.setErrorContext(c.toString() + " assuming receiver is a subclass");
			}
			else
				checker.setErrorContext(c.toString());
		}
		else if(assumeVirtualFrame != null) {
			// distinguish frame assumptions
			if(assumeVirtualFrame)
				checker.setErrorContext("assuming receiver has analyzed type");
			else
				checker.setErrorContext("assuming receiver is a subclass");
		}
		if(logger.isLoggable(Level.FINE)) {
			if(assumeVirtualFrame != null)
				logger.fine("Results for " + d.getName() + (assumeVirtualFrame ? " (virtual frame) case " : " (non-virtual frame) case ") + c);
			else
				logger.fine("Results for " + d.getName() + " case " + c);
		}
		d.accept(checker);
	}	
	
	@Override
	public AnnotationDatabase getAnnoDB() {
		return analysisInput.getAnnoDB();
	}
	
	@Override
	public Option<CompilationUnitTACs> getComUnitTACs() {
		return analysisInput.getComUnitTACs();
	}
	
	@Override
	public Option<IProgressMonitor> getProgressMonitor() {
		return analysisInput.getProgressMonitor();
	}

	@Override
	public StateSpaceRepository getRepository() {
		return StateSpaceRepository.getInstance(analysisInput.getAnnoDB());
	}

	@Override
	public IInvocationCaseInstance getAnalyzedCase() {
		return analyzedCase;
	}
	
	@Override
	public boolean assumeVirtualFrame() {
		return assumeVirtualFrame;
	}

	protected class FractionalChecker extends ASTVisitor {
		
		protected String errorCtx = "";
		private final MethodDeclaration methodUnderAnalysis;
		
		public FractionalChecker(MethodDeclaration d) {
			this.methodUnderAnalysis = d;
		}

		public void setErrorContext(String ctx) {
			errorCtx = " (" + ctx + ")";
		}

		@Override
		public void endVisit(MethodDeclaration node) {
			// b/c of various problems...
			// we really only want this case to run if the method is void.
			ITypeBinding return_type = node.resolveBinding().getReturnType();
			
			if(isAbstract(node) == false && Utilities.isVoidType(return_type) ) {
				final Block block = node.getBody();
				// check whether post-conditions for parameters are validated
				PluralContext exit = getFa().getResultsBefore(block);
				
				// Sometimes this method is called on unreachable statements :-(
				if( exit.isBottom() ) {
					// TODO: Is this still necessary now that we check for void?
					if(logger.isLoggable(Level.FINEST))
						logger.finest("no implicit exit at the end of the method body");
					return;
				}
				
				reportIfError(
						exit.checkPostCondition(block, 
								null /* no result value */, 
								FractionalAnalysis.this.getAnalyzedCase().getPostconditionChecker(),
								getTf().getInitialLocations(), 
								Collections.<Boolean, String>emptyMap()), 
						block);

				// debug
				if(logger.isLoggable(Level.FINEST))
					logger.finest("exit: " + exit);
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(ReturnStatement node) {
			// pull results first
			PluralContext exit = getFa().getResultsBefore(node);

			if(exit.isBottom()) {
				logger.warning("Bottom encountered before return: " + node);
				super.endVisit(node);
				return;
			}
			
			Variable resultVar = node.getExpression() != null ? getFa().getVariable(node.getExpression()) : null;

			// check whether post-conditions for parameters are validated
			reportIfError(
					exit.checkPostCondition(node, 
							resultVar, 
							FractionalAnalysis.this.getAnalyzedCase().getPostconditionChecker(),
							getTf().getInitialLocations(), 
							getTf().getDynamicStateTest()), 
					node);

			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest("" + getFa().getResultsBefore(node));
				logger.finest("  " + node);
			}
		}

		@Override
		public void endVisit(ClassInstanceCreation node) {
			final PluralContext before = getFa().getResultsBefore(node);
			final IMethodBinding constructorBinding = node.resolveConstructorBinding();
			final IConstructorSignature sig = getRepository().getConstructorSignature(constructorBinding);
			
			if(FractionalAnalysis.isBottom(before, node)) {
				super.endVisit(node);
				return;
			}
		
			@SuppressWarnings("unchecked")
			List<Expression> arguments = (List<Expression>) node.arguments();
			checkCasesOfInvocation(node, before, sig, null /* no receiver yet */, 
					variables(arguments), false /* "new" */);
			
			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(MethodInvocation node) {
			final PluralContext before = getFa().getResultsBefore(node);
			final IMethodBinding methodBinding = node.resolveMethodBinding();
			final IMethodSignature sig = getRepository().getMethodSignature(methodBinding);
			
			if(FractionalAnalysis.isBottom(before, node)) {
				super.endVisit(node);
				return;
			}
			
			// receiver
			final Variable receiver;
			if(sig.hasReceiver()) {
				if(node.getExpression() == null)
					receiver = getFa().getImplicitThisVariable(methodBinding);
				else
					receiver = getFa().getVariable(node.getExpression());
				
			}
			else
				// static method
				receiver = null;
			
			boolean isPrivate = Modifier.isPrivate(sig.getSpecifiedMethodBinding().getModifiers());
			boolean isStatic = Modifier.isStatic(sig.getSpecifiedMethodBinding().getModifiers());
			
			@SuppressWarnings("unchecked")
			List<Expression> arguments = (List<Expression>) node.arguments();
			checkCasesOfInvocation(node, before, sig, receiver, 
					variables(arguments), isPrivate || isStatic);
			
			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(SuperMethodInvocation node) {
			final PluralContext before = getFa().getResultsBefore(node);
			final IMethodBinding methodBinding = node.resolveMethodBinding();
			final IMethodSignature sig = getRepository().getMethodSignature(methodBinding);
			
			if(FractionalAnalysis.isBottom(before, node)) {
				super.endVisit(node);
				return;
			}
			
			@SuppressWarnings("unchecked")
			List<Expression> arguments = (List<Expression>) node.arguments();
			checkCasesOfInvocation(node, before, sig, null /* lattice fills in super */, 
					variables(arguments), true /* static dispatch */);
			
			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
			}
			super.endVisit(node);
		}
		
		@Override
		public void endVisit(ArrayAccess node) {
			// check stores into arrays: was array modifiable?
			if(checkArrays  && node.getParent() instanceof Assignment) {
				Assignment store = (Assignment) node.getParent();
				if(store.getLeftHandSide().equals(node)) {
					final PluralContext before = getFa().getResultsBefore(store);
					final PluralContext after = getFa().getResultsAfter(store);

					if(FractionalAnalysis.isBottom(before, after, node)) {
						super.endVisit(node);
						return;
					}

					@SuppressWarnings("deprecation")
					boolean checkConstraintsSatisfiable = after.checkConstraintsSatisfiable(getFa().getVariable(node.getArray()));
					if(! checkConstraintsSatisfiable)
						// TODO better reporting
						reporter.reportUserProblem(
								"no suitable permission for assignment to " + node + errorCtx, 
								node, FractionalAnalysis.this.getName());
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

		@Override
		public void endVisit(ConstructorInvocation node) {
			final PluralContext before = getFa().getResultsBefore(node);
//			final PluralDisjunctiveLE after = getFa().getResultsAfter(node);
			final IMethodBinding constructorBinding = node.resolveConstructorBinding();
			final IConstructorSignature sig = getRepository().getConstructorSignature(constructorBinding);
			
			if(FractionalAnalysis.isBottom(before, node)) {
				super.endVisit(node);
				return;
			}
			
			// receiver is always "this"  (super constructor call is different node type)
			final Variable receiver = getFa().getImplicitThisVariable(constructorBinding);
			
			@SuppressWarnings("unchecked")
			List<Expression> arguments = (List<Expression>) node.arguments();
			checkCasesOfInvocation(node, before, sig, receiver, 
					variables(arguments), true);
			

			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
			}
			super.endVisit(node);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.SuperConstructorInvocation)
		 */
		@Override
		public void endVisit(SuperConstructorInvocation node) {
			final PluralContext before = getFa().getResultsBefore(node);
			final IMethodBinding constructorBinding = node.resolveConstructorBinding();
			final IConstructorSignature sig = getRepository().getConstructorSignature(constructorBinding);
			
			if(FractionalAnalysis.isBottom(before, node)) {
				super.endVisit(node);
				return;
			}
			
			@SuppressWarnings("unchecked")
			List<Expression> arguments = (List<Expression>) node.arguments();
			checkCasesOfInvocation(node, before, sig, null /* lattice fills in super variable */, 
					variables(arguments), true /* static dispatch */);

			// debug
			if(logger.isLoggable(Level.FINEST)) {
				logger.finest(before.toString());
				logger.finest("  " + node);
			}
			super.endVisit(node);
		}

		/**
		 * Given that we are checking a method invocation, returns the 
		 * MethodCheckingKind based on whether or not we are checking a 
		 * constructor and whether or not the receiver type is known
		 * statically.
		 * @param isConstructor Are we checking a constructor invocation?
		 * @param isStaticallyDispatched Is the receiver type known statically?
		 * @return The MethodCheckingKind given we are checking an invocation.
		 */
		private MethodCheckingKind methodCheckingKindInvoc(boolean isConstructor,
				boolean isStaticallyDispatched) {
			if( isConstructor ) {
				if( isStaticallyDispatched )
					return MethodCheckingKind.CONSTRUCTOR_SUPER_CALL;
				else
					return MethodCheckingKind.CONSTRUCTOR_NEW;
			}
			else {
				if( isStaticallyDispatched )
					return MethodCheckingKind.METHOD_CALL_STATIC_DISPATCH;
				else
					return MethodCheckingKind.METHOD_CALL_DYNAMIC_DISPATCH;
			}
		}
		
		private ITACAnalysisContext contextFromTAC() {
			final EclipseTAC tac = getComUnitTACs().unwrap().getMethodTAC(this.methodUnderAnalysis);
			final MethodDeclaration decl = this.methodUnderAnalysis;
			return new ITACAnalysisContext(){

				@Override
				public MethodDeclaration getAnalyzedMethod() {
					return decl;
				}

				@Override
				public SourceVariable getSourceVariable(IVariableBinding varBinding) {
					return tac.sourceVariable(varBinding);
				}

				@Override
				public SuperVariable getSuperVariable() {
					return tac.superVariable(null);
				}

				@Override
				public ThisVariable getThisVariable() {
					return tac.thisVariable();
				}

				@Override
				public Variable getVariable(ASTNode node) {
					return tac.variable(node);
				}};
		}
		
		/**
		 * @param node
		 * @param before
		 * @param sig
		 * @param receiver <code>null</code> for <code>super</code> and static methods.
		 * @param arguments
		 * @param receiverIsStaticallyBound for methods that have a receiver, <code>true</code>
		 * indicates that this is a statically dispatched call, ie. a call to <code>super</code>
		 * or a constructor invocation on <code>this</code>, but <b>not</b> a object instantiation
		 * with <code>new</code>.
		 */
		private void checkCasesOfInvocation(ASTNode node,
				final PluralContext before, final IInvocationSignature sig,
				final Variable receiver, List<Variable> arguments, boolean receiverIsStaticallyBound) {
			List<String> errors = new LinkedList<String>();
			MethodCheckingKind checkingKind = methodCheckingKindInvoc(sig.isConstructorSignature(),
					receiverIsStaticallyBound);
			
			InstantiatedTypeAnalysis ta = new InstantiatedTypeAnalysis(contextFromTAC(),getAnnoDB());
			Option<RcvrInstantiationPackage> ip = receiver == null ? Option.<RcvrInstantiationPackage>none() : 
				Option.some(new RcvrInstantiationPackage(ta, receiver));
			for(IInvocationCaseInstance c : 
				sig.createPermissionsForCases(checkingKind, false, receiverIsStaticallyBound, ip)) {
				String err;
				if(receiver == null && receiverIsStaticallyBound)
					err = before.checkSuperCallPrecondition(node, 
							arguments,
							c.getPreconditionChecker());
				else
					// any other call, including "new"
					err = before.checkRegularCallPrecondition(node, 
									receiver, arguments,
									c.getPreconditionChecker());
				if(err == null)
					return;
				errors.add(err);
			}
			if(errors.size() == 1)
				reportIfError(errors.iterator().next(), node);
			else if(errors.size() > 1)
				reportIfError("One of: " + errors, node);
		}

		private List<Variable> variables(List<? extends ASTNode> nodes) {
			if(nodes.isEmpty())
				return Collections.emptyList();
			ArrayList<Variable> result = new ArrayList<Variable>(nodes.size());
			for(ASTNode node: nodes) {
				result.add(getFa().getVariable(node));
			}
			return result;
		}

		/**
		 * Reports an error to the user if the given string is non-<code>null</code>.
		 * @param errorOrNull
		 * @param node
		 */
		protected void reportIfError(String errorOrNull, ASTNode node) {
			if(errorOrNull != null)
				reporter.reportUserProblem(errorOrNull + errorCtx, node, FractionalAnalysis.this.getName());
		}

	} // END FractionalChecker
	
	private boolean isAbstract(MethodDeclaration node) {
		return node.getBody() == null;
	}

	private static boolean isBottom(
			PluralContext before,
			PluralContext after,
			ASTNode node) {
		if(before.isBottom())
			logger.warning("Encountered bottom before node: " + node);
		else if(after.isBottom())
			logger.warning("Encountered bottom after node: " + node);
		else 
			return false;
		return true;
	}

	private static boolean isBottom(
			PluralContext before,
			ASTNode node) {
		if(before.isBottom())
			logger.warning("Encountered bottom before node: " + node);
		else 
			return false;
		return true;
	}

}
