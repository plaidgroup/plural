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

package edu.cmu.cs.plural.polymorphic.internal;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.cmu.cs.crystal.AbstractCompilationUnitAnalysis;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.eclipse.EclipseTAC;
import edu.cmu.cs.crystal.tac.model.SourceVariable;
import edu.cmu.cs.crystal.tac.model.SuperVariable;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.crystal.util.VOID;
import edu.cmu.cs.plural.alias.AliasingLE;
import edu.cmu.cs.plural.alias.LocalAliasTransfer;
import edu.cmu.cs.plural.perm.parser.AccessPredVisitor;
import edu.cmu.cs.plural.perm.parser.BinaryExprAP;
import edu.cmu.cs.plural.perm.parser.Conjunction;
import edu.cmu.cs.plural.perm.parser.Disjunction;
import edu.cmu.cs.plural.perm.parser.EmptyPredicate;
import edu.cmu.cs.plural.perm.parser.EqualsExpr;
import edu.cmu.cs.plural.perm.parser.NotEqualsExpr;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.perm.parser.PermissionImplication;
import edu.cmu.cs.plural.perm.parser.StateOnly;
import edu.cmu.cs.plural.perm.parser.TempPermission;
import edu.cmu.cs.plural.perm.parser.Withing;
import edu.cmu.cs.plural.polymorphic.instantiation.ApplyAnnotationWrapper;
import edu.cmu.cs.plural.polymorphic.instantiation.GroundInstantiation;
import edu.cmu.cs.plural.polymorphic.instantiation.GroundParser;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedTypeAnalysis;
import edu.cmu.cs.plural.polymorphic.instantiation.RcvrInstantiationPackage;
import edu.cmu.cs.plural.polymorphic.instantiation.ResultApplyAnnotationWrapper;
import edu.cmu.cs.plural.states.IMethodCase;
import edu.cmu.cs.plural.states.IMethodCaseInstance;
import edu.cmu.cs.plural.states.IMethodSignature;
import edu.cmu.cs.plural.states.MethodCheckingKind;
import edu.cmu.cs.plural.states.StateSpaceRepository;
import edu.cmu.cs.plural.states.annowrappers.ClassStateDeclAnnotation;
import edu.cmu.cs.plural.states.annowrappers.StateDeclAnnotation;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * One half of Polymorphic Plural, checks that polymorphic variables are
 * used correctly internally (ie before instantiation). 
 * 
 * The internal checker tracks which polymorphic variables are in scope.
 * When those variables are used, this checker will ensure (symbolically,
 * ignoring fractions) that those polymorphic permissions are used according
 * to their bounds.  
 * 
 * @author Nels E. Beckman
 * @since Nov 10, 2009
 *
 */
public class PolyInternalChecker extends AbstractCompilationUnitAnalysis {

	@Override
	public void analyzeCompilationUnit(CompilationUnit d) {
		d.accept(new ASTVisitor(){
			@Override
			public void endVisit(TypeDeclaration node) {
				analyzeClassDeclaration(node);
			}			
		});
	}
	
	private static Option<PolyVar> lookup(String name, Map<String,PolyVar> classVars,
			Map<String,PolyVar> methodVars) {
		if( methodVars.containsKey(name) )
			return Option.some(methodVars.get(name));
		else if( classVars.containsKey(name) )
			return Option.some(classVars.get(name));
		else
			return Option.none();
	}
	
	private void analyzeClassDeclaration(TypeDeclaration clazz) {
		List<ICrystalAnnotation> annotations = this.getInput().getAnnoDB().getAnnosForType(clazz.resolveBinding());
		final Map<String, PolyVar> vars_in_scope = 
			Collections.unmodifiableMap(getPolyVarsInScope(annotations));
	
		checkPolyVarDeclAnnotations(clazz);
		checkApplyAnnotations(clazz, vars_in_scope);
		checkPolyVarsInInvariants(clazz, vars_in_scope);
		
		clazz.accept(new ASTVisitor(){
			@Override
			public void endVisit(MethodDeclaration node) {
				analyzeMethodDeclaration(node, vars_in_scope);
			}});
	}

	/**
	 * Check to make sure that any polymorphic variables that this class
	 * declares are reasonable, and not share, pure, etc.
	 */
	private void checkPolyVarDeclAnnotations(TypeDeclaration clazz) {
		AnnotationDatabase annoDB = this.getInput().getAnnoDB();
		for( ICrystalAnnotation anno_ : annoDB.getAnnosForType(clazz.resolveBinding()) ) {
			if( anno_ instanceof PolyVarDeclAnnotation ) {
				PolyVarDeclAnnotation anno = (PolyVarDeclAnnotation)anno_;
				if( isPermLitteral(anno.getVariableName()) ) {
					String error_msg = "This class declares a polymoprhic permission " +
					 "with a reserved name; " + anno.getVariableName() + ".";
					this.getReporter().reportUserProblem(error_msg, clazz, getName());
				}
			}
		}
	}
	
	/** 
	 * Check that any poly vars used in invariants are well-known.
	 * If Poly Var is neither a ground permission nor a known poly var,
	 * it must be a programmer mistake.
	 * */
	private void checkPolyVarsInInvariants(final TypeDeclaration clazz,
			final Map<String,PolyVar> vars_in_scope) {
		List<ICrystalAnnotation> annos = getInput().getAnnoDB().getAnnosForType(clazz.resolveBinding());
		for( ICrystalAnnotation anno : annos ) {
			// Look for @ClassStates
			if( anno instanceof ClassStateDeclAnnotation ) {
				for( StateDeclAnnotation state_anno : ((ClassStateDeclAnnotation) anno).getStates() ) {
					String inv = state_anno.getInv();
					// Parse it & visit the result.
					PermParser.accept(inv, new AccessPredVisitor<VOID>(){

						@Override
						public VOID visit(TempPermission perm) {
							// THE ONLY INTERSTING CASE
							String type = perm.getType();
							if( !isPermLitteral(type) && !vars_in_scope.containsKey(type) ) {
								// This is bad. 
								String error = "Invariant permission " + type + " is unrecognized.";
								getReporter().reportUserProblem(error, clazz, getName());
							}
							return VOID.V();
						}

						@Override public VOID visit(Disjunction disj) {
							disj.getP1().accept(this);
							return disj.getP2().accept(this);
						}

						@Override
						public VOID visit(Conjunction conj) {
							conj.getP1().accept(this);
							return conj.getP2().accept(this);
						}

						@Override
						public VOID visit(Withing withing) {
							withing.getP1().accept(this);
							return withing.getP2().accept(this);
						}

						@Override
						public VOID visit(
								PermissionImplication permissionImplication) {
							return permissionImplication.cons().accept(this);
						}
						
						@Override public VOID visit(BinaryExprAP binaryExpr) {return VOID.V();}
						@Override public VOID visit(EqualsExpr equalsExpr) {return VOID.V();}
						@Override public VOID visit(NotEqualsExpr notEqualsExpr) {return VOID.V();}
						@Override public VOID visit(StateOnly stateOnly) {return VOID.V();}
						@Override public VOID visit(EmptyPredicate emptyPredicate) {return VOID.V();}
					});
				}
			}
		}
	}
	
	/**
	 * Check that apply annotations are valid. One of the most important things
	 * that this method will do is to verify that only symmetric permissions
	 * are instantiated for @Symmetric annotations. 
	 */
	private void checkApplyAnnotations(TypeDeclaration clazz,
			final Map<String,PolyVar> class_vars_in_scope) {
		// Things we need to check:
		// 1 - All variable declarations...
		// 2 - Anything else?
		clazz.accept(new ASTVisitor() {
			@Override
			public void endVisit(VariableDeclarationFragment node) {
				check(node.resolveBinding(), node);
			}

			@Override
			public void endVisit(SingleVariableDeclaration node) {
				check(node.resolveBinding(), node);
			}
			
			private void check(IVariableBinding binding, ASTNode errorNode) {
				List<PolyVarKind> typeParameters = AnnotationUtilities.classParameterKinds(binding.getType(), getInput().getAnnoDB());
				List<String> applyAnnotations = AnnotationUtilities.applications(binding, getInput().getAnnoDB());
				// Make sure arg # matches param #
				if( typeParameters.size() != applyAnnotations.size() ) {
					String error = "Number of static arguments does not match" +
							" number of static parameters.";
					getReporter().reportUserProblem(error, errorNode, getName());
					return;
				}
				// Now, for all symmetric parameters...
				int param_num = 0;
				for( PolyVarKind kind : typeParameters ) {
					if( kind.equals(PolyVarKind.SYMMETRIC) ) {
						String applied = applyAnnotations.get(param_num);
						if( isPermLitteral(applied) ) {
							// Make sure it's a PURE/IMM/SHARE
							Option<GroundInstantiation> parse_ = GroundParser.parse(applied);
							PermissionKind p_kind = parse_.unwrap().getKind();
							if( p_kind.equals(PermissionKind.FULL) || p_kind.equals(PermissionKind.UNIQUE) ) {
								String error = "Static parameter must be symmetric but argument is " + p_kind;
								getReporter().reportUserProblem(error, errorNode, getName());
								return;
							}
						} 
						else {
							// Look up param, make sure IT'S a SYM
							if( !class_vars_in_scope.containsKey(applied) ) {
								String error = "Application contains out of scope variable: " + applied;
								getReporter().reportUserProblem(error, errorNode, getName());
								return;
							}
							PolyVarKind app_kind = class_vars_in_scope.get(applied).getKind();
							if( !app_kind.equals(PolyVarKind.SYMMETRIC) ) {
								String error = "Static parameter must be symmetric but argument " + applied + " is " + app_kind;
								getReporter().reportUserProblem(error, errorNode, getName());
								return;
							}
						}
					}
					param_num++;
				}
			}
			
		});
	}

	/**
	 * From the crystal annotations, find the polymorphic variable declarations and put them
	 * into a map where their name is the key.
	 */
	private Map<String, PolyVar> getPolyVarsInScope(List<ICrystalAnnotation> annotations) {
		Map<String,PolyVar> result = new HashMap<String,PolyVar>();
		for( ICrystalAnnotation anno : annotations ) {
			if( anno instanceof PolyVarDeclAnnotation ) {
				PolyVar var = polyVarFromAnnotation((PolyVarDeclAnnotation)anno);
				result.put(var.getName(), var);
			}
		}
		return result;
	}

	/**
	 * Take a PolyVarDeclAnnotation and uses it to create a new polyvar.
	 */
	private PolyVar polyVarFromAnnotation(final PolyVarDeclAnnotation anno) {
		return new PolyVar(){
			private String name = anno.getVariableName();
			private PolyVarKind kind = anno.getKind();
			@Override public PolyVarKind getKind() { return kind; }
			@Override public String getName() { return name;	}
		};
	}
	
	/**
	 * Performs the main analysis, which must be done on a method by method
	 * basis, after gathering the polymorphic variables that are in scope.
	 */
	private void analyzeMethodDeclaration(MethodDeclaration node, Map<String,PolyVar> class_scoped_vars) {
		AnnotationDatabase annodb = this.getInput().getAnnoDB();
		AnnotationSummary summary = annodb.getSummaryForMethod(node.resolveBinding());
		List<ICrystalAnnotation> annos = summary.getReturn();
		Map<String,PolyVar> method_vars = Collections.unmodifiableMap(getPolyVarsInScope(annos));
		ITACFlowAnalysis<AliasingLE> alias_analysis = createAliasAnalysis();
		// Get permission to check at the end of the method
		List<Pair<Aliasing,String>> params_to_check = AnnotationUtilities.findParamsToCheck(node, summary, alias_analysis);
		Option<String> return_to_check = AnnotationUtilities.findReturnValueToCheck(summary);
		Option<String> rcvr_to_check = AnnotationUtilities.findRcvrToCheck(summary);
		// Get permission to insert at the beginning of the method	
		List<Pair<Aliasing,Option<String>>> param_entry = AnnotationUtilities.findParamsForEntry(node, summary, alias_analysis);
		Option<String> rcvr_entry = AnnotationUtilities.findRcvrForEntry(summary);
		
		try {
			checkMethodSpecification(node, class_scoped_vars, method_vars, annodb);
			ErrorCheckingVisitor e_visitor = new ErrorCheckingVisitor(class_scoped_vars,
					method_vars, node, params_to_check, return_to_check, rcvr_to_check,
					param_entry, rcvr_entry, alias_analysis);
			node.accept(e_visitor);
		} catch(VarScope e) {
			String error = "Unknown variable " + e.varName + " mentioned in specification.";
			this.getReporter().reportUserProblem(error, e.node, this.getName());
		}
	}

	/**
	 * Check that each of the polymorphic variables mentioned in the specification
	 * of this method are actually 
	 */
	private static void checkMethodSpecification(MethodDeclaration node,
			Map<String, PolyVar> class_scoped_vars,
			Map<String, PolyVar> method_vars, AnnotationDatabase annoDB) {
		AnnotationSummary summary = annoDB.getSummaryForMethod(node.resolveBinding());
		// Parameters
		int param_num = 0;
		for( Object param_ : node.parameters() ) {
			SingleVariableDeclaration param = (SingleVariableDeclaration)param_;
			for( ICrystalAnnotation anno_ : summary.getParameter(param_num) ) {
				if( anno_ instanceof PolyVarUseAnnotation ) {
					PolyVarUseAnnotation anno = (PolyVarUseAnnotation)anno_;
					Option<?> var = lookup(anno.getVariableName(), class_scoped_vars, method_vars);
					if( var.isNone() )
						throw new VarScope(anno.getVariableName(), param);
				}
				else if( anno_ instanceof ApplyAnnotationWrapper ) {
					ApplyAnnotationWrapper anno = (ApplyAnnotationWrapper)anno_;
					for( String app_name : anno.getValue() ) {
						Option<?> var = lookup(app_name, class_scoped_vars, method_vars);
						if( var.isNone() && !isPermLitteral(app_name) )
							throw new VarScope(app_name, param);
					}
				}
			}
			param_num++;
		}
		// Returning & receiver annotations
		for( ICrystalAnnotation anno_ : summary.getReturn() ) {
			// Returning
			if( anno_ instanceof PolyVarReturnedAnnotation ) {
				PolyVarReturnedAnnotation anno = (PolyVarReturnedAnnotation)anno_;
				Option<?> var = lookup(anno.getVariableName(), class_scoped_vars, method_vars);
				if( var.isNone() )
					throw new VarScope(anno.getVariableName(), node);
			}
			else if( anno_ instanceof ResultApplyAnnotationWrapper ) {
				ResultApplyAnnotationWrapper anno = (ResultApplyAnnotationWrapper)anno_;
				for( String app_name : anno.getValue() ) {
					Option<?> var = lookup(app_name, class_scoped_vars, method_vars);
					if( var.isNone() && !isPermLitteral(app_name) )
						throw new VarScope(app_name, node);
				}
			}
			// Receiver
			else if( anno_ instanceof PolyVarUseAnnotation ) {
				PolyVarUseAnnotation anno = (PolyVarUseAnnotation)anno_;
				Option<?> var = lookup(anno.getVariableName(), class_scoped_vars, method_vars);
				if( var.isNone() )
					throw new VarScope(anno.getVariableName(), node);
			}
			else if( anno_ instanceof ApplyAnnotationWrapper ) {
				ApplyAnnotationWrapper anno = (ApplyAnnotationWrapper)anno_;
				for( String app_name : anno.getValue() ) {
					Option<?> var = lookup(app_name, class_scoped_vars, method_vars);
					if( var.isNone() && !isPermLitteral(app_name) )
						throw new VarScope(app_name, node);
				}
			}
		}
	}

	/**
	 * Create an alias analysis, using the fields of this analysis as input.
	 */
	private ITACFlowAnalysis<AliasingLE> createAliasAnalysis() {
		return new TACFlowAnalysis<AliasingLE>(
				new LocalAliasTransfer(this.getInput().getAnnoDB(),
		                               new HashMap<IVariableBinding,Variable>()),
		        this.getInput().getComUnitTACs().unwrap());
	}
	
	/**
	 * Is the given perm share, pure, unique, full or immutable?
	 */
	public static boolean isPermLitteral(String perm) {
		return GroundParser.parse(perm).isSome();
	}

	static class VarScope extends RuntimeException {
		private static final long serialVersionUID = 2139857766082038184L;
		final String varName;
		final ASTNode node;
		
		public VarScope(String varName, ASTNode node) {
			this.varName = varName;
			this.node = node;
		}
	}
	
	/**
	 * This visitor corresponds to the visitor that exists in almost every
	 * Crystal analysis. It walks the tree, calling the dataflow analysis on-demand,
	 * and reports errors when it finds them. The main process is that we check if
	 * remaining symbolic permission is available at the places where it must be.
	 * Places where checking must occur: 
	 * 1 - End of the method OR explicit returns.
	 * 2 - Method calls AND constructor calls.
	 * 3 - Possible packing spots.
	 */
	private class ErrorCheckingVisitor extends ASTVisitor {
		final private Map<String,PolyVar> classVars;
		final private Map<String,PolyVar> methodVars;
		
		final private MethodDeclaration method;
		
		final private Option<String> returnToCheck;
		final private List<Pair<Aliasing,String>> paramsToCheck;
		
		final private ITACFlowAnalysis<AliasingLE> aliasAnalysis;
		final private ITACFlowAnalysis<PolyTupleLattice> polyAnalysis;
		
		final private InstantiatedTypeAnalysis typeAnalysis;
		
		ErrorCheckingVisitor(Map<String,PolyVar> class_vars, 
				Map<String,PolyVar> method_vars, MethodDeclaration node, 
				List<Pair<Aliasing,String>> paramsToCheck,
				Option<String> returnToCheck, Option<String> rcvrToCheck, 
				List<Pair<Aliasing, Option<String>>> param_entry, Option<String> rcvr_entry,
				ITACFlowAnalysis<AliasingLE> aliasAnalysis) {
			this.classVars = class_vars;
			this.methodVars = method_vars;
			this.method = node;
			this.returnToCheck = returnToCheck;
			this.paramsToCheck = paramsToCheck;
			
			EclipseTAC tac = getInput().getComUnitTACs().unwrap().getMethodTAC(node);
			this.aliasAnalysis = aliasAnalysis;
			this.typeAnalysis = new InstantiatedTypeAnalysis(contextFromTAC(tac, method), getInput().getAnnoDB());
			
			PolyInternalTransfer transferFunction = 
				new PolyInternalTransfer(aliasAnalysis, simpleLookupMap(), 
						param_entry, rcvr_entry, getInput(),typeAnalysis);
			this.polyAnalysis = new TACFlowAnalysis<PolyTupleLattice>(
					transferFunction, getInput().getComUnitTACs().unwrap());
		}
		
		private ITACAnalysisContext contextFromTAC(final EclipseTAC tac, final MethodDeclaration dec) {
			return new ITACAnalysisContext() {
				@Override
				public MethodDeclaration getAnalyzedMethod() {
					return dec;
				}

				@Override
				public SourceVariable getSourceVariable(
						IVariableBinding varBinding) {
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
		
		private SimpleMap<String,Option<PolyVar>> simpleLookupMap() {
			return new SimpleMap<String,Option<PolyVar>>(){
				@Override public Option<PolyVar> get(String key) {
					return lookup(key);
				}};
		}
		
		private Option<PolyVar> lookup(String name) {
			return PolyInternalChecker.lookup(name,classVars,methodVars);
		}

		/** Split off the given permission (in string form, from the 
		 *  annotation) from the given lattice element. Return what
		 *  would be the remaining permission. */
		private PolyVarLE split(PolyVarLE has, String to_split_off, ASTNode error_node) {
			Option<PolyVar> split_off_ = lookup(to_split_off);
			if( split_off_.isNone() ) {
				throw new VarScope("Variable " + to_split_off + " not in scope.", error_node);
			}
			else {
				PolyVar split_off = split_off_.unwrap();
				switch( split_off.getKind() ) {
				case EXACT:
				case SIMILAR:
					return PolyVarLE.NONE;
				case SYMMETRIC:
					return has;
				default:
					throw new RuntimeException("Impossible"); 
				}
			}
		}
		
		@Override
		public void endVisit(MethodDeclaration node) {
			// Here's where we do the check for an implicit
			// return...
			PolyTupleLattice lattice = this.polyAnalysis.getResultsBefore(node.getBody());
			EclipseTAC tac = getInput().getComUnitTACs().unwrap().getMethodTAC(method);
			
			checkParameterReturns(lattice, tac, node);
			//checkPackToPost(lattice, tac, node);
		}

		@Override
		public void endVisit(ReturnStatement node) {
			// Return statement: Make sure any permissions that were
			// borrowed are available, and same for result permissions.
			ASTNode node_of_interest = node.getExpression() == null ? node : node.getExpression();
			PolyTupleLattice lattice = this.polyAnalysis.getResultsAfter(node_of_interest);
			EclipseTAC tac = getInput().getComUnitTACs().unwrap().getMethodTAC(method);
			
			if( this.returnToCheck.isSome() ) {
				// Check result
				Variable return_var = tac.variable(node.getExpression());
				Aliasing return_loc = aliasAnalysis.getResultsBefore(node.getExpression()).get(return_var);
				PolyVarLE le = lattice.get(return_loc);
				
				if( le.isBottom() || le.isTop() || 
				    le.name().isNone() || !le.name().unwrap().equals(returnToCheck.unwrap()) ) {
					// ERROR!
					String error_msg = "Return value must have permission " + returnToCheck.unwrap() +
					                   " but instead has " + le + ".";
					getReporter().reportUserProblem(error_msg, node, getName());
				}
				else {
					// Split off returned permission
					PolyVarLE new_le = split(le, returnToCheck.unwrap(), node);
					lattice.put(return_loc, new_le);
				}
			}
			
			checkParameterReturns(lattice, tac, node);
			//checkPackToPost(lattice, tac, node.getExpression());
		}
		
		@Override
		public void endVisit(MethodInvocation node) {
			// We have to check that there is enough permission to each
			// parameter, otherwise we signal an error.
			PolyTupleLattice lattice = this.polyAnalysis.getResultsBefore(node);
			AliasingLE locs = this.aliasAnalysis.getResultsBefore(node);
			EclipseTAC tac = getInput().getComUnitTACs().unwrap().getMethodTAC(method);
			IMethodBinding binding = node.resolveMethodBinding();
			
			// If we have a non static method call, we may need to substitute.
			boolean isStatic = Modifier.isStatic(binding.getModifiers());
			Variable rcvrVar = isStatic ? null : 
				(node.getExpression() == null ? tac.thisVariable() : tac.variable(node.getExpression()));
			List<String> rcvrType = isStatic ? Collections.<String>emptyList() : 
					this.typeAnalysis.findType(rcvrVar);
			ITypeBinding rcvrJType = isStatic ? null : rcvrVar.resolveType();
			
			// Check arguments
			int arg_num = 0;
			for( Object arg_ : node.arguments() ) {
				Expression arg = (Expression)arg_;
				Variable arg_var = tac.variable(arg);
				Aliasing arg_loc = locs.get(arg_var);
				Option<PolyVarUseAnnotation> anno_ = AnnotationUtilities.ithParamToAnnotation(binding, arg_num, getInput().getAnnoDB());
				
				if( anno_.isNone() ) continue; // Nothing required...
				String needed_perm = anno_.unwrap().getVariableName();
				
				// Must perform substitution in order to see if we have
				// enough permission at this point.
				if( !isStatic ) {
					List<String> needed_perm_ = Collections.singletonList(needed_perm);
					List<String> new_type = InstantiatedTypeAnalysis.substitute(rcvrType, rcvrJType, needed_perm_, getInput().getAnnoDB());
					assert(new_type.size() == 1);
					needed_perm = new_type.get(0);
				}
				
				if(  isPermLitteral(needed_perm) ) continue; // Checked in the instantiated checker
				
				PolyVarLE cur_perm = lattice.get(arg_loc);
				if( cur_perm.isTop() || cur_perm.name().isNone() || !needed_perm.equals(cur_perm.name().unwrap()) ) {
					// ERROR
					String error_msg = "Argument needs permission " + needed_perm + " but " +
						"has only " + cur_perm + ".";
					getReporter().reportUserProblem(error_msg, arg, getName());
				}
				else {
					// SPLIT
					PolyVarLE remainder = split(cur_perm, needed_perm, node);
					lattice.put(arg_loc, remainder);
				}
				
				arg_num++;
			}
		}

		/** Check that each parameter has enough permission to return. */
		private PolyTupleLattice checkParameterReturns(PolyTupleLattice lattice,
				EclipseTAC tac, ASTNode error_node) {
			for( Pair<Aliasing,String> param_to_check : this.paramsToCheck ) {
				Aliasing param_loc = param_to_check.fst();
				String perm_needed = param_to_check.snd();
				PolyVarLE param_le = lattice.get(param_loc);

				if( param_le.isBottom()  ) {
					// We HOPE this indicates this point is not reachable, and
					// the results are being checked at an actual return.
					assert(error_node instanceof MethodDeclaration);
					continue;
				}
				else if( param_le.isTop() || param_le.name().isNone() || 
						  !param_le.name().unwrap().equals(perm_needed) ) {
					// ERROR!
					String error_msg = "On return, parameter " + param_loc +
					" must have permission " + perm_needed +
					" but instead has " + param_le + ".";
					getReporter().reportUserProblem(error_msg, error_node, getName());
				}
				else {
					// Split off returned permission
					PolyVarLE new_le = split(param_le, perm_needed, error_node);
					lattice.put(param_loc, new_le);
				}
			}
			return lattice;
		}
		
		/** Make sure the receiver is in the proper post-condition
		 * state, which may include packing it to the post-condition
		 * state. If that cannot be done, errors would be issued. */
		private void checkPackToPost(PolyTupleLattice lattice, 
				EclipseTAC tac, final ASTNode error_node) {
			Set<String> rcvr_post = getRcvrPostState();
			AnnotationDatabase annoDB = analysisInput.getAnnoDB();
			ThisVariable this_var = tac.thisVariable();
			
			SimpleMap<Variable,Aliasing> locs = new SimpleMap<Variable,Aliasing>() {
				@Override
				public Aliasing get(Variable key) {
					return aliasAnalysis.getResultsAfter(error_node).get(key);
				}};
			SimpleMap<String,Option<PolyVar>> varLookup = new SimpleMap<String,Option<PolyVar>>() {
				@Override
				public Option<PolyVar> get(String key) {
					return lookup(key);
				}};
			Option<PolyTupleLattice> new_lattice =
				PackingManager.packRcvr(lattice, rcvr_post, annoDB, 
					varLookup, locs, this_var.resolveType());
			
			if( new_lattice.isNone() ) {
				// ERROR!
				String error_msg = "On return, could not pack to required " +
						"receiver post-condition states: " + rcvr_post;
				getReporter().reportUserProblem(error_msg, error_node, getName());
			}
		}

		/**
		 * @return
		 */
		private Set<String> getRcvrPostState() {
			AnnotationDatabase annoDB = analysisInput.getAnnoDB();
			IMethodBinding resolveBinding = method.resolveBinding();
			IMethodSignature sig =
				StateSpaceRepository.getInstance(annoDB).getMethodSignature(resolveBinding);
			
			if( sig.cases().size() > 1 )
				Utilities.nyi();
				
			IMethodCase case_ = sig.cases().get(0);
			IMethodCaseInstance case_instance =
				case_.createPermissions(MethodCheckingKind.METHOD_IMPL_CUR_IS_VIRTUAL, true, false, Option.<RcvrInstantiationPackage>none());
			return case_instance.getEnsuredReceiverPermissions().getStateInfo(true);
		}
	}
}