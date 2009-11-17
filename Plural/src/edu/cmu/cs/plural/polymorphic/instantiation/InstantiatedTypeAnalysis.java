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

package edu.cmu.cs.plural.polymorphic.instantiation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.tac.eclipse.CompilationUnitTACs;
import edu.cmu.cs.crystal.tac.eclipse.EclipseTAC;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.annot.Apply;
import edu.cmu.cs.plural.annot.ResultApply;
import edu.cmu.cs.plural.polymorphic.internal.PolyVarDeclAnnotation;

/**
 * The instantiated type analysis will tell you the instantiated
 * type of any field or local variable. In the case of local variable,
 * it may end up performing an analysis (typechecking) to 
 * accomplish this goal, since we can only annotate variable declarations
 * and yet the method calls and constructor instances are what really matter.
 * 
 * @author Nels E. Beckman
 * @since Nov 12, 2009
 */
public final class InstantiatedTypeAnalysis {
	final private CompilationUnitTACs compilationUnit;
	final private AnnotationDatabase annoDB;
	
	private MethodDeclaration lastAnalyzed = null;
	private Map<Variable,List<String>> types = null;
	
	public InstantiatedTypeAnalysis(CompilationUnitTACs compilationUnit,
			AnnotationDatabase annoDB) {
		this.compilationUnit = compilationUnit;
		this.annoDB = annoDB;
	}
	
	public static class TypeError extends RuntimeException {
		private static final long serialVersionUID = 2922005566056611578L;
		final public String errorMsg;
		final public ASTNode errorNode;
		
		public TypeError(String errorMsg, ASTNode errorNode) {
			this.errorMsg = errorMsg;
			this.errorNode = errorNode;
		}
	}
	
	/** Perform static argument subsitution on original of the application arguments. The type information
	 *  tells us which elements of original are actually applicable for substitution. Error if the
	 *  length of applications is not the same as the number of params on type.  */
	public static List<String> substitute(List<String> applications, ITypeBinding type, List<String> original,
			AnnotationDatabase annoDB, ASTNode errorNode) {
		List<String> type_args = new LinkedList<String>();
		for( ICrystalAnnotation anno : annoDB.getAnnosForType(type) ) {
			if( anno instanceof PolyVarDeclAnnotation ) {
				type_args.add(((PolyVarDeclAnnotation)anno).getVariableName());
			}
		}
		if( applications.size() != type_args.size() ) {
			String error_msg = "Different number of static args.";
			throw new TypeError(error_msg, errorNode);
		}
		
		List<String> result = new LinkedList<String>();
		for( String arg : original ) {
			int index = type_args.lastIndexOf(arg);
			if( index != -1 ) {
				String new_arg = applications.get(index);
				result.add(new_arg);
			}
			else {
				result.add(arg);
			}
		}
		return result;
	}
	
	/** Performs no type analysis, just directly looks up the type of the
	 *  variable given from its annotations. */
	public List<String> findType(IVariableBinding binding) {
		for( ICrystalAnnotation anno_ : this.annoDB.getAnnosForVariable(binding) ) {
			if( anno_ instanceof ApplyAnnotationWrapper ) {
				return ((ApplyAnnotationWrapper)anno_).getValue();
			}
		}
		return Collections.emptyList();
	}
	
	/**
	 * Find the 'type' of this variable, where a type is really a list of
	 * instantiated permission arguments. The MethodDeclaration that must
	 * be given is the context where this variable arose. If the variable
	 * is actually a field, the method where the field was used should be
	 * given, ideally.
	 */
	public List<String> findType(Variable var, MethodDeclaration decl) {
		if( decl.equals(lastAnalyzed) ) {
			assert(this.types.containsKey(var));
			return this.types.get(var);
		}
		
		// Need to do some analysis...
		this.lastAnalyzed = decl;
		this.types = new HashMap<Variable,List<String>>();
		
		buildTypesForMethod(decl);
		assert(this.types.containsKey(var));
		return this.types.get(var);
	}

	private List<String> thisType(IMethodBinding binding) {
		ITypeBinding this_type_binding = binding.getDeclaringClass();
		for( ICrystalAnnotation anno_ : annoDB.getAnnosForType(this_type_binding) ) {
			if( anno_ instanceof PolyVarDeclAnnotation ) {
				PolyVarDeclAnnotation anno = (PolyVarDeclAnnotation)anno_;
				// TODO Right now, only one parameter can be declared at a time.
				return Collections.singletonList(anno.getVariableName());
			}
		}
		return Collections.emptyList();
	}
	
	/**
	 * Does type-checking for the given method, 
	 */
	private void buildTypesForMethod(MethodDeclaration decl) {
		EclipseTAC tac = this.compilationUnit.getMethodTAC(decl);
		// First, build up types for all of the parameters.
		for( Object param_ : decl.parameters() ) {
			SingleVariableDeclaration param = (SingleVariableDeclaration)param_;
			IVariableBinding binding = param.resolveBinding();
			Variable tac_var = tac.sourceVariable(binding);
			List<String> type = this.findType(binding);
			this.types.put(tac_var, type);
		}
		// Then, add 'this' variable
		ThisVariable this_var = tac.thisVariable();
		List<String> type_type = thisType(decl.resolveBinding());
		this.types.put(this_var, type_type);
		// Then visit the body
		decl.getBody().accept(new StmtVisitor(tac));
	}
	
	/** What is the return type for the most recently analyzed method? */
	private List<String> returnTypeForCurMethod() {
		AnnotationSummary summary = this.annoDB.getSummaryForMethod(this.lastAnalyzed.resolveBinding());
		ICrystalAnnotation anno = summary.getReturn(ResultApply.class.getName());
		if( anno == null )
			return Collections.emptyList();
		else
			return ((ResultApplyAnnotationWrapper)anno).getValue();
	}
	
	// Stmt visitor traverses the tree of statements, 
	// and for the most part, just passes off its work to the ExprVisitor which
	// actually has a type, but for local variable declaration statements, it will
	// put the variable into the map.
	private class StmtVisitor extends ASTVisitor {
		private final EclipseTAC tac;
		
		public StmtVisitor(EclipseTAC tac) {
			this.tac = tac;
		}
		
		@Override
		public boolean visit(DoStatement node) {
			node.getBody().accept(this);
			node.getExpression().accept(new ExprVisitor(Collections.<String>emptyList()));
			return false;
		}

		@Override
		public boolean visit(ExpressionStatement node) {
			node.getExpression().accept(new ExprVisitor());
			return false;
		}

		@Override
		public boolean visit(ForStatement node) {
			for( Object init_ : node.initializers() ) {
				Expression init = (Expression)init_;
				init.accept(new ExprVisitor());
			}
			node.getExpression().accept(new ExprVisitor(Collections.<String>emptyList()));
			for(Object updater_ : node.updaters() ) {
				Expression updater = (Expression)updater_;
				updater.accept(new ExprVisitor());
			}
			return false;
		}

		@Override
		public boolean visit(IfStatement node) {
			node.getExpression().accept(new ExprVisitor(Collections.<String>emptyList()));
			node.getThenStatement().accept(this);
			node.getElseStatement().accept(this);
			return false;
		}

		@Override
		public boolean visit(ReturnStatement node) {
			if( node.getExpression() != null ) {
				// We DO have a downward type. It's the type of the
				// return value.
				List<String> return_type = returnTypeForCurMethod();
				node.getExpression().accept(new ExprVisitor(return_type));
			}
			return false;
		}

		@Override
		public boolean visit(SwitchCase node) {
			if( node.getExpression() != null ) {
				node.getExpression().accept(new ExprVisitor(Collections.<String>emptyList()));
			}
			return false;
		}

		@Override
		public boolean visit(SwitchStatement node) {
			node.getExpression().accept(new ExprVisitor(Collections.<String>emptyList()));
			for( Object stmt_ : node.statements() ) {
				Statement stmt = (Statement)stmt_;
				stmt.accept(this);
			}
			return false;
		}

		@Override
		public boolean visit(ThrowStatement node) {
			node.getExpression().accept(new ExprVisitor());
			return false;
		}

		@Override
		public boolean visit(TryStatement node) {
			node.getBody().accept(this);
			// Catch variable declarations.
			for( Object ketch_ : node.catchClauses() ) {
				CatchClause ketch = (CatchClause)ketch_;
				SingleVariableDeclaration decl = ketch.getException();
				Variable var = tac.sourceVariable(decl.resolveBinding());
				List<String> type = findType(decl.resolveBinding());
				types.put(var, type);
				ketch.getBody().accept(this);
			}
			node.getFinally().accept(this);
			return false;
		}

		@Override
		public boolean visit(TypeDeclarationStatement node) {
			// We just don't support these nested types, at least
			// not here.
			return false;
		}

		@Override
		public boolean visit(VariableDeclarationStatement node) {
			// Probably the most interesting of all of the stmt
			// nodes, this one takes the type for the declaration
			// and jams it into the initializing expression, if
			// there is one.
			for( Object decl_ : node.fragments() ) {
				VariableDeclarationFragment decl = (VariableDeclarationFragment)decl_;
				IVariableBinding binding = decl.resolveBinding();
				Variable var = tac.sourceVariable(binding);
				List<String> type = findType(binding);
				types.put(var, type);
				
				if( decl.getInitializer() != null )
					decl.getInitializer().accept(new ExprVisitor(type));
			}
			return false;
		}

		@Override
		public boolean visit(WhileStatement node) {
			node.getExpression().accept(new ExprVisitor(Collections.<String>emptyList()));
			node.getBody().accept(this);
			return false;
		}
		
		// WHOA doubly-nested class!
		// Don't forget, some expressions can declare variables, namely the
		// VariableDeclarationExpression.
		private class ExprVisitor extends ASTVisitor {
			// The type of this expression, as given by
			// a super-expression or statement context.
			// NONE means unconstrained while SOME means
			// has constraints... even if those constraints
			// are an empty list, which means we KNOW the
			// type has no application of arguments.
			private final Option<List<String>> downwardType;
			private List<String> resultType;
			
			public ExprVisitor(List<String> downwardType) {
				// Just try to save on allocation I guess.
				this.downwardType = 
					downwardType.isEmpty() ? NOTHING : Option.some(downwardType);
			}
			
			public ExprVisitor() {
				this.downwardType = Option.none();
			}
			
			private ExprVisitor(Option<List<String>> type) {
				this.downwardType = type;
			}
			
			private List<String> check(Expression expr) {
				expr.accept(this);
				return resultType;
			}
			
			private void assertEqualIfNecessary(Option<List<String>> expected, List<String> actual, ASTNode en) {
				if( expected.isSome() )
					assertEqual(expected.unwrap(), actual, en);
			}
			
			private void assertEqual(List<String> expected, List<String> actual, ASTNode errorNode) {
				assert(expected != null && actual != null);
				if( !expected.equals(actual) ) {
					String error_msg = "Polymorphic parameter mismatch: Expected " + argString(expected) + 
					  " but is actually " + argString(actual);
					throw new TypeError(error_msg, errorNode);
				}
			}
			
			/** Arguments to string. */
			private String argString(List<String> strings) {
				StringBuilder result_ = new StringBuilder('<');
				for( String arg : strings ) {
					result_.append(arg);
					result_.append(',');
				}
				result_.deleteCharAt(result_.length()-1);
				result_.append('>');
				return result_.toString();
			}

			/** Store the given type in the types map for the variable corresponding
			 *  to this Expression. */
			private void storeTypeForExpr(List<String> type, Expression expr) {
				Variable var = tac.variable(expr);
				assert(var != null);
				types.put(var, type);
			}

//			/** Should only be called if variable is already known to
//			 *  be in the types map. */
//			private List<String> getTypeForExpr(Expression expr) {
//				Variable var = tac.variable(expr);
//				assert(  var != null );
//				assert( types.containsKey(var) );
//				return types.get(tac.variable(expr));
//			}
			
			@Override
			public boolean visit(Assignment node) {
				List<String> lhs_type = (new ExprVisitor(this.downwardType)).check(node.getLeftHandSide());
				List<String> rhs_typ = (new ExprVisitor(this.downwardType)).check(node.getRightHandSide());
				assertEqual(lhs_type, rhs_typ, node);
				storeTypeForExpr(lhs_type, node);
				this.resultType = lhs_type;
				return false;
			}

			@Override
			public boolean visit(BooleanLiteral node) {
				this.resultType = Collections.emptyList();
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(CastExpression node) {
				// Type on the right becomes the type on the left.
				// No parameter casting is allowed.
				List<String> e_type = (new ExprVisitor(this.downwardType)).check(node.getExpression());
				this.resultType = e_type;
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(CharacterLiteral node) {
				this.resultType = Collections.emptyList();
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(ClassInstanceCreation node) {
				// This is the new expression. We can safely ignore
				// the expression on the left-hand side, for now.
				// Type needs to come from the downwards direction!
				this.resultType = downwardType.unwrap();
				storeTypeForExpr(resultType, node);
				
				// BUT, we should also check the argument expressions have
				// the right type too. Need to substitute application arguments
				// given by downward type for applicable parameter arguments.
				// We need the return type, so we need to check the receiver type, 
				// check the arguments match the substituted type of the formal params,
				// and then return the substituted return type.
				
				// Receiver:
				List<String> xtor_type = this.resultType;
				ITypeBinding xtor_jtype = node.resolveTypeBinding();
				
				// Check arguments based on substitution of rcvr_type
				int arg_num = 0;
				for( Object arg_ : node.arguments() ) {
					Expression arg = (Expression)arg_;
					// Find out the type from annotations
					List<String> pre_sub_arg_type = ithArgApplyType(arg_num, node.resolveConstructorBinding(), annoDB);
					List<String> post_sub_arg_type = substitute(xtor_type, xtor_jtype, pre_sub_arg_type, annoDB, node);
					// Now check the argument
					(new ExprVisitor(post_sub_arg_type)).check(arg);
					arg_num++;
				}

				return false;
			}

			@Override
			public boolean visit(ConditionalExpression node) {
				node.getExpression().accept(new ExprVisitor(Collections.<String>emptyList()));
				List<String> then_type = (new ExprVisitor(this.downwardType)).check(node.getThenExpression());
				List<String> else_type = (new ExprVisitor(this.downwardType)).check(node.getElseExpression());
				assertEqual(then_type, else_type, node);
				this.resultType = then_type;
				storeTypeForExpr(then_type, node);
				return false;
			}

			@Override
			public boolean visit(FieldAccess node) {
				List<String> rcvr_type = (new ExprVisitor()).check(node.getExpression());
				ITypeBinding rcvr_type_binding = node.getExpression().resolveTypeBinding();
				List<String> field_type = findType(node.resolveFieldBinding());
				List<String> new_field_type = substitute(rcvr_type, rcvr_type_binding, field_type, annoDB, node);
				
				this.resultType = new_field_type;
				assertEqualIfNecessary(this.downwardType, new_field_type, node);
				storeTypeForExpr(new_field_type, node);
				return false;
			}

			@Override
			public boolean visit(InfixExpression node) {
				// Can only be ints, bools, etc
				this.resultType = Collections.emptyList();
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(InstanceofExpression node) {
				this.resultType = Collections.emptyList();
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			/** Returns the specified Apply type for the ith parameter of this method, without
			 *  taking into account the type of the receiver. */
			private List<String> ithArgApplyType(int i, IMethodBinding meth, AnnotationDatabase annoDB) {
				AnnotationSummary summary = annoDB.getSummaryForMethod(meth);
				ICrystalAnnotation anno = summary.getParameter(i, Apply.class.getName());
				
				if( anno == null )
					return Collections.emptyList();
				else
					return ((ApplyAnnotationWrapper)anno).getValue();
			}
			
			/**  Returns the specified Apply type for the return value of this method,
			 *   without taking into account the type of the receiver (ie substitutions) */
			private List<String> resultApplyType(IMethodBinding meth, AnnotationDatabase annoDB) {
				AnnotationSummary summary = annoDB.getSummaryForMethod(meth);
				List<ICrystalAnnotation> annos = summary.getReturn();
				
				for( ICrystalAnnotation anno_ : annos ) {
					if( anno_ instanceof ResultApplyAnnotationWrapper ) {
						return ((ResultApplyAnnotationWrapper)anno_).getValue();
					}
				}
				return Collections.emptyList();
			}
			
			/** Gets the type for the receiver of the given method invocation. This
			 *  method is necessary because in the AST the receiver could be
			 *  implicit, in which case the expression node of the method invocation
			 *  will be null. If the method is static, this will return an empty
			 *  list. */
			private List<String> findTypeForMethodReceiver(MethodInvocation node) {
				if( node.getExpression() != null )
					return (new ExprVisitor()).check(node.getExpression());
				
				IMethodBinding binding = node.resolveMethodBinding();
				boolean is_static = Modifier.isStatic(binding.getModifiers());
				
				if( is_static )
					return Collections.emptyList();
				else {
					// Assume that is must be THIS, and return this class' type
					// parameters.
					return thisType(binding);
				}
			}
			
			private Option<ITypeBinding> findJTypeForMethodReceiver(MethodInvocation node) {
				if( node.getExpression() != null )
					return Option.some(node.getExpression().resolveTypeBinding());
				
				IMethodBinding binding = node.resolveMethodBinding();
				boolean is_static = Modifier.isStatic(binding.getModifiers());
				
				if( is_static )
					return Option.none();
				else {
					// Assume that is must be THIS, and return this class' type
					// parameters.
					return Option.some(binding.getDeclaringClass());
				}	
			}
			
			@Override
			public boolean visit(MethodInvocation node) {
				// We need the return type, so we need to check the receiver type, 
				// check the arguments match the substituted type of the formal params,
				// and then return the substituted return type.
				
				// Receiver:
				Expression rcvr_expr = node.getExpression();
				List<String> rcvr_type = findTypeForMethodReceiver(node);
				Option<ITypeBinding> rcvr_jtype =  findJTypeForMethodReceiver(node);
				
				// Check arguments based on substitution of rcvr_type
				int arg_num = 0;
				for( Object arg_ : node.arguments() ) {
					Expression arg = (Expression)arg_;
					// Find out the type from annotations
					List<String> pre_sub_arg_type = ithArgApplyType(arg_num, node.resolveMethodBinding(), annoDB);
					List<String> post_sub_arg_type = rcvr_jtype.isNone() ? pre_sub_arg_type :
						substitute(rcvr_type, rcvr_jtype.unwrap(), pre_sub_arg_type, annoDB, node);
					// Now check the argument
					(new ExprVisitor(post_sub_arg_type)).check(arg);
					arg_num++;
				}
				
				// Now substitute return type.
				List<String> pre_sub_result_type = resultApplyType(node.resolveMethodBinding(), annoDB);
				List<String> post_sub_result_type = rcvr_jtype.isNone() ? pre_sub_result_type :
					substitute(rcvr_type, rcvr_jtype.unwrap(), pre_sub_result_type, annoDB, node);
				
				// Set the type for this expression
				this.resultType = post_sub_result_type;
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				
				return false;
			}

			@Override
			public boolean visit(NumberLiteral node) {
				this.resultType = Collections.emptyList();
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(ParenthesizedExpression node) {
				List<String> expr_type = (new ExprVisitor(this.downwardType)).check(node.getExpression());
				this.resultType = expr_type;
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(PostfixExpression node) {
				this.resultType = Collections.emptyList();
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(PrefixExpression node) {
				this.resultType = Collections.emptyList();
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(SimpleName node) {
				IBinding binding_ = node.resolveBinding();
				if( binding_ instanceof IVariableBinding ) {
					IVariableBinding binding = (IVariableBinding)binding_;
					List<String> type = findType(binding);
					this.resultType = type;
					assertEqualIfNecessary(downwardType, resultType, node);
					storeTypeForExpr(resultType, node);
				}
				else {
					Utilities.nyi("Is this possible if we have an expression?");
				}
				return false;
			}

			@Override
			public boolean visit(StringLiteral node) {
				this.resultType = Collections.emptyList();
				assertEqualIfNecessary(downwardType, resultType, node);
				storeTypeForExpr(resultType, node);
				return false;
			}

			@Override
			public boolean visit(SuperFieldAccess node) {
				return Utilities.nyi("Totally not ready for this.");
			}

			@Override
			public boolean visit(SuperMethodInvocation node) {
				return Utilities.nyi("Totally not ready for this.");
			}
			
			@Override
			public boolean visit(ThisExpression node) {
				// AS in Java Generics, the type of the this node is
				// implicitly instantiated with the parameters that are
				// in scope.
				this.resultType = thisType(lastAnalyzed.resolveBinding());

				storeTypeForExpr(resultType, node);
				assertEqualIfNecessary(downwardType, resultType, node);
				return false;
			}

			@Override
			public boolean visit(TypeLiteral node) {
				this.resultType = Collections.emptyList();
				storeTypeForExpr(resultType, node);
				assertEqualIfNecessary(downwardType, resultType, node);
				return false;
			}

			@Override
			public boolean visit(VariableDeclarationExpression node) {
				for( Object decl_ : node.fragments() ) {
					VariableDeclarationFragment decl = (VariableDeclarationFragment)decl_;
					IVariableBinding binding = decl.resolveBinding();
					List<String> type = findType(binding);
					Variable var = tac.sourceVariable(binding);
					types.put(var, type);
					
					decl.getInitializer().accept(new ExprVisitor(type));
				}
				
				this.resultType = Collections.emptyList();
				storeTypeForExpr(resultType, node);
				assertEqualIfNecessary(downwardType, resultType, node);
				return false;
			}
		}
	}
	
	private final static Option<List<String>> NOTHING = Option.some(Collections.<String>emptyList());	
}