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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.cmu.cs.crystal.AbstractCompilationUnitAnalysis;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.eclipse.EclipseTAC;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.alias.AliasingLE;
import edu.cmu.cs.plural.alias.LocalAliasTransfer;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedTypeAnalysis;

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
	
	private void analyzeClassDeclaration(TypeDeclaration clazz) {
		List<ICrystalAnnotation> annotations = this.getInput().getAnnoDB().getAnnosForType(clazz.resolveBinding());
		final Map<String, PolyVar> vars_in_scope = 
			Collections.unmodifiableMap(getPolyVarsInScope(annotations));
		
		clazz.accept(new ASTVisitor(){
			@Override
			public void endVisit(MethodDeclaration node) {
				analyzeMethodDeclaration(node, vars_in_scope);
			}});
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
		List<Pair<Aliasing,String>> param_entry = AnnotationUtilities.findParamsForEntry(node, summary, alias_analysis);
		Option<String> rcvr_entry = AnnotationUtilities.findRcvrForEntry(summary);
		
		try {
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
	 * Create an alias analysis, using the fields of this analysis as input.
	 */
	private ITACFlowAnalysis<AliasingLE> createAliasAnalysis() {
		return new TACFlowAnalysis<AliasingLE>(
				new LocalAliasTransfer(this.getInput().getAnnoDB(),
		                               new HashMap<IVariableBinding,Variable>()),
		        this.getInput().getComUnitTACs().unwrap());
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
		final private ITACFlowAnalysis<TupleLatticeElement<Aliasing, PolyVarLE>> polyAnalysis;
		
		ErrorCheckingVisitor(Map<String,PolyVar> class_vars, 
				Map<String,PolyVar> method_vars, MethodDeclaration node, 
				List<Pair<Aliasing,String>> paramsToCheck,
				Option<String> returnToCheck, Option<String> rcvrToCheck, 
				List<Pair<Aliasing, String>> param_entry, Option<String> rcvr_entry,
				ITACFlowAnalysis<AliasingLE> aliasAnalysis) {
			this.classVars = class_vars;
			this.methodVars = method_vars;
			this.method = node;
			this.returnToCheck = returnToCheck;
			this.paramsToCheck = paramsToCheck;
			
			this.aliasAnalysis = aliasAnalysis;
			InstantiatedTypeAnalysis typeAnalysis = 
				new InstantiatedTypeAnalysis(getInput().getComUnitTACs().unwrap(), getInput().getAnnoDB());
			
			PolyInternalTransfer transferFunction = 
				new PolyInternalTransfer(aliasAnalysis, simpleLookupMap(), 
						param_entry, rcvr_entry, getInput().getAnnoDB(), typeAnalysis);
			this.polyAnalysis = new TACFlowAnalysis<TupleLatticeElement<Aliasing,PolyVarLE>>(
					transferFunction, getInput().getComUnitTACs().unwrap());
		}
		
		private SimpleMap<String,Option<PolyVar>> simpleLookupMap() {
			return new SimpleMap<String,Option<PolyVar>>(){
				@Override public Option<PolyVar> get(String key) {
					return lookup(key);
				}};
		}
		
		private Option<PolyVar> lookup(String name) {
			if( methodVars.containsKey(name) )
				return Option.some(methodVars.get(name));
			else if( classVars.containsKey(name) )
				return Option.some(classVars.get(name));
			else
				return Option.none();
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
		public void endVisit(ReturnStatement node) {
			// Return statement: Make sure any permissions that were
			// borrowed are available, and same for result permissions.
			ASTNode node_of_interest = node.getExpression() == null ? node : node.getExpression();
			TupleLatticeElement<Aliasing,PolyVarLE> lattice = this.polyAnalysis.getResultsAfter(node_of_interest);
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
		}
		
		private void checkParameterReturns(TupleLatticeElement<Aliasing,PolyVarLE> lattice,
				EclipseTAC tac, ASTNode error_node) {
			for( Pair<Aliasing,String> param_to_check : this.paramsToCheck ) {
				Aliasing param_loc = param_to_check.fst();
				String perm_needed = param_to_check.snd();
				PolyVarLE param_le = lattice.get(param_loc);

				if( param_le.isBottom() || param_le.isTop() || 
						param_le.name().isNone() || !param_le.name().unwrap().equals(perm_needed) ) {
					// ERROR!
					String error_msg = "On return, parameter " + param_loc +
					" must have permission " + perm_needed +
					" but instead has " + param_le + ".";
					getReporter().reportUserProblem(error_msg, error_node, getName());
				}
				else {
					// Split off returned permission
					PolyVarLE new_le = split(param_le, returnToCheck.unwrap(), error_node);
					lattice.put(param_loc, new_le);
				}
			}
		}
	}
}