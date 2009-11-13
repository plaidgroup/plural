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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.plural.alias.AliasingLE;

/**
 * @author Nels E. Beckman
 * @since Nov 12, 2009
 *
 */
public final class AnnotationUtilities {

	private AnnotationUtilities() {
		// This class consists entirely of static methods.
		// You should not be able to instantiate this class.
	}
	
	/**
	 * When we check a method return, any method parameter that takes polymorphic
	 * permission may need to be checked to ensure that the right amount of permission
	 * is left.
	 * @param alias_analysis A list of locations that will need to be checked when
	 *   the method returns, paired with the name of the polymorphic permission 
	 *   that they must have available at that time.
	 */
	public static List<Pair<Aliasing, String>> findParamsToCheck(MethodDeclaration method, 
			AnnotationSummary summary, ITACFlowAnalysis<AliasingLE> alias_analysis) {
		return findParams(method, summary, alias_analysis, true);
	}

	/** Find the parameters that are available at method entry. */
	public static List<Pair<Aliasing,String>> findParamsForEntry(MethodDeclaration method,
			AnnotationSummary summary, ITACFlowAnalysis<AliasingLE> alias_analysis) {
		return findParams(method, summary, alias_analysis, false);
	}
	
	private static List<Pair<Aliasing, String>> findParams(MethodDeclaration method, 
			AnnotationSummary summary, ITACFlowAnalysis<AliasingLE> alias_analysis,
			boolean ignore_not_returned) {
		AliasingLE start_locs = alias_analysis.getStartResults(method);
		List<Pair<Aliasing,String>> result = new LinkedList<Pair<Aliasing,String>>();
		
		// For every parameter of this method, see if its specification uses a polyvar,
		// and if so, store the location of that parameter and the name of is polyvar.
		for( Object param_ : method.parameters() ) {
			SingleVariableDeclaration param = (SingleVariableDeclaration)param_;
			Aliasing loc = start_locs.get(alias_analysis.getSourceVariable(param.resolveBinding()));
			String param_name = param.getName().getFullyQualifiedName();
			String anno_name = edu.cmu.cs.plural.annot.PolyVar.class.getName();
			PolyVarUseAnnotation use = (PolyVarUseAnnotation)summary.getParameter(param_name, 
					anno_name);
			// We only need to check borrowed permissions!
			if( use != null && (use.isReturned() || ignore_not_returned == false) )
				result.add(Pair.create(loc, use.getVariableName()));
		}
		return result;
	}
	
	/** The name of the polyvar REQ as a permission for the ith parameter
	 *  of the given method, or NONE if it does not use a polyvar. */
	public static Option<PolyVarUseAnnotation> ithParamToAnnotation(IMethodBinding method, int i,
			AnnotationDatabase annoDB) {
		AnnotationSummary summary = annoDB.getSummaryForMethod(method);
		for( ICrystalAnnotation anno : summary.getParameter(i) ) {
			if( anno instanceof PolyVarUseAnnotation ) {
				return Option.some((PolyVarUseAnnotation)anno);
			}
		}
		return Option.none();
	}
	
	/** By looking at the method annotations, find out how much permission should
	 *  be available for the return value. */
	public static Option<String> findReturnValueToCheck(AnnotationSummary summary) {
		for( ICrystalAnnotation anno : summary.getReturn() ) {
			if( anno instanceof PolyVarReturnedAnnotation ) {
				PolyVarReturnedAnnotation anno_ = (PolyVarReturnedAnnotation)anno;
				return Option.some(anno_.getVariableName());
			}
		}
		return Option.none();
	}
	
	/**
	 * Find the polymorphic permission that is required of the receiver upon
	 * return, if any.
	 */
	public static Option<String> findRcvrToCheck(AnnotationSummary summary) {
		return findRcvr(summary, true);
	}
	
	/**
	 * Find the polymorphic permission for the receiver if any.
	 */
	public static Option<String> findRcvrForEntry(AnnotationSummary summary) {
		return findRcvr(summary, false);
	}
	
	private static Option<String> findRcvr(AnnotationSummary summary, 
			boolean ignore_not_returned) {
		for( ICrystalAnnotation anno : summary.getReturn() ) {
			if( anno instanceof PolyVarUseAnnotation ) {
				PolyVarUseAnnotation anno_ = (PolyVarUseAnnotation)anno;
				
				return (anno_.isReturned() || !ignore_not_returned) ? 
				  Option.<String>some(anno_.getVariableName()) : Option.<String>none();
			}
		}
		return Option.none();
	}
}