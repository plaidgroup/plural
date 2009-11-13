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
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.cmu.cs.crystal.AbstractCompilationUnitAnalysis;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.plural.alias.AliasingLE;
import edu.cmu.cs.plural.alias.LocalAliasTransfer;

/**
 * One half of Polymorphic Plural, checks that polymorphic variables are
 * use correct internally. 
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
				  method_vars, params_to_check, return_to_check, rcvr_to_check,
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
	private static class ErrorCheckingVisitor extends ASTVisitor {
		final private Map<String,PolyVar> classVars;
		final private Map<String,PolyVar> methodVars;
		final private List<Pair<Aliasing,String>> paramsToCheck; 
		final private Option<String> returnToCheck;
		final private Option<String> rcvrToCheck;
		// Permission available upon entry.
		final private List<Pair<Aliasing,String>> paramsForEntry;
		final private Option<String> rcvrForEntry;
		// Alias analysis
		final private ITACFlowAnalysis<AliasingLE> aliasAnalysis;
		
		ErrorCheckingVisitor(Map<String,PolyVar> class_vars, 
				Map<String,PolyVar> method_vars, List<Pair<Aliasing,String>> paramsToCheck,
				Option<String> returnToCheck, Option<String> rcvrToCheck, 
				List<Pair<Aliasing, String>> param_entry, Option<String> rcvr_entry,
				ITACFlowAnalysis<AliasingLE> aliasAnalysis) {
			this.classVars = class_vars;
			this.methodVars = method_vars;
			this.paramsToCheck = paramsToCheck;
			this.returnToCheck = returnToCheck;
			this.rcvrToCheck = rcvrToCheck;
			this.paramsForEntry = param_entry;
			this.rcvrForEntry = rcvr_entry;
			this.aliasAnalysis = aliasAnalysis;
		}
		
		private Option<PolyVar> lookup(String name) {
			if( methodVars.containsKey(name) )
				return Option.some(methodVars.get(name));
			else if( classVars.containsKey(name) )
				return Option.some(classVars.get(name));
			else
				return Option.none();
		}
	}
}