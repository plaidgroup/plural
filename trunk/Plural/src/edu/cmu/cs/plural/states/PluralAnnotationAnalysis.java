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
package edu.cmu.cs.plural.states;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.cmu.cs.crystal.AbstractCompilationUnitAnalysis;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.perm.parser.AccessPredVisitor;
import edu.cmu.cs.plural.perm.parser.BinaryExprAP;
import edu.cmu.cs.plural.perm.parser.BoolLiteral;
import edu.cmu.cs.plural.perm.parser.Conjunction;
import edu.cmu.cs.plural.perm.parser.Disjunction;
import edu.cmu.cs.plural.perm.parser.EqualsExpr;
import edu.cmu.cs.plural.perm.parser.Identifier;
import edu.cmu.cs.plural.perm.parser.NotEqualsExpr;
import edu.cmu.cs.plural.perm.parser.Null;
import edu.cmu.cs.plural.perm.parser.ParamReference;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.perm.parser.PermissionImplication;
import edu.cmu.cs.plural.perm.parser.PrimaryExpr;
import edu.cmu.cs.plural.perm.parser.PrimaryExprVisitor;
import edu.cmu.cs.plural.perm.parser.StateOnly;
import edu.cmu.cs.plural.perm.parser.TempPermission;
import edu.cmu.cs.plural.perm.parser.Withing;
import edu.cmu.cs.plural.states.annowrappers.ClassStateDeclAnnotation;
import edu.cmu.cs.plural.states.annowrappers.StateDeclAnnotation;

/**
 * An analysis that examines user annotations to ensure that they have been
 * constructed correctly.
 * 
 * @author Kevin Bierhoff
 *
 */
public class PluralAnnotationAnalysis extends AbstractCompilationUnitAnalysis {
	
	private Set<String> permAnnos;
	private HashSet<String> resultAnnos;
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCompilationUnitAnalysis#analyzeCompilationUnit(org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	@Override
	public void analyzeCompilationUnit(CompilationUnit d) {
		d.accept(new AnnotationVisitor());
	}
	
	public StateSpaceRepository getRepository() {
		return StateSpaceRepository.getInstance(analysisInput.getAnnoDB());
	}
	
	private class AnnotationVisitor extends ASTVisitor {
		
		/** 
		 * <code>true</code> when Cases annotation seen on current method
		 * @see #checkCasesAndPermOnSameMethod(Annotation) 
		 */
		private boolean sawCases = false;
		
		/** 
		 * <code>true</code> when Perm annotation seen on current method
		 * @see #checkCasesAndPermOnSameMethod(Annotation) 
		 */
		private boolean sawPerm = false;

		//
		// Declaration checks
		// 

		@Override
		public void endVisit(AnonymousClassDeclaration node) {
			// TODO Auto-generated method stub
			super.endVisit(node);
		}

		@Override
		public void endVisit(CompilationUnit node) {
			// TODO Auto-generated method stub
			super.endVisit(node);
		}

		@Override
		public void endVisit(TypeDeclaration node) {
			// Errors in declared state dimensions
			Map<ICrystalAnnotation, Set<String>> problems = 
				PluralAnnotationAnalysis.this.getRepository().checkStateSpace(node);
			for(Map.Entry<ICrystalAnnotation, Set<String>> p : problems.entrySet()) {
				// TODO localize annotation that causes the problem
				for(String desc : p.getValue()) {
					reporter.reportUserProblem(desc, node, PluralAnnotationAnalysis.this.getName());
				}
			}
				
			/*
			 * Get all errors of ClassStates annotations, and print them out.
			 */
			final List<Pair<ASTNode, String>> errors = 
				checkClassStatesAnnot(analysisInput.getAnnoDB(), node);
			for( Pair<ASTNode, String> error : errors ) {
				reporter.reportUserProblem(error.snd(), error.fst(),
						PluralAnnotationAnalysis.this.getName());
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(TypeDeclarationStatement node) {
			// TODO Auto-generated method stub
			super.endVisit(node);
		}

		@Override
		public void endVisit(MarkerAnnotation node) {
			checkCasesAndPermOnSameMethod(node);

			// check that @Cases is not empty, i.e., prevent @Cases({ })
			if(isCasesAnno(node))
				reporter.reportUserProblem("Must have cases in @Cases", node, PluralAnnotationAnalysis.this.getName());
			// check that @ResultXxx is not on constructor or void method
			else if(isPermAnnotation(node)) {
				ASTNode annotated = getAnnotatedElement(node);
				if(annotated != null && annotated instanceof MethodDeclaration) {
					if(isResultAnnotation(node)) { 
						if(isVoid(((MethodDeclaration) annotated).resolveBinding().getReturnType())) 
							reportUserProblem("@ResultXxx annotations must be on non-void method", node);
					}
					else {
						if(Modifier.isStatic(((MethodDeclaration) annotated).getModifiers())) {
							PluralAnnotationAnalysis.this.reportUserProblem(
									"Receiver annotation must be on non-static method", node);
						}
					}
				}
				else if(isResultAnnotation(node)) {
					reportUserProblem("@ResultXxx annotations must be on non-void method", node);
				}
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(NormalAnnotation node) {
			checkCasesAndPermOnSameMethod(node);

			// check that @Cases is not empty, i.e., prevent @Cases({ })
			if(isCasesAnno(node)) {
				if(! checkValueArrayNonEmpty(node.resolveAnnotationBinding()))
					reporter.reportUserProblem("Must have cases in @Cases", node, PluralAnnotationAnalysis.this.getName());
			}
			// Do the user's annotations actually parse correctly?
			else if( "edu.cmu.cs.plural.annot.State".equals(node.resolveTypeBinding().getQualifiedName()) ) {
				Option<Object> perm_ = getAnnotationParam(node.resolveAnnotationBinding(), "inv");
				if( perm_.isSome() ) {
					String perm = (String)perm_.unwrap();
					Option<String> parse_error = PermParser.getParseError(perm);
					if( parse_error.isSome() ) {
						reporter.reportUserProblem("Parse error in annotation string: " + parse_error.unwrap(), 
								node, PluralAnnotationAnalysis.this.getName());
					}
					// TODO check invariant string from here??
				}
				
			}
			else if( "edu.cmu.cs.plural.annot.Perm".equals(node.resolveTypeBinding().getQualifiedName()) ) {
				MethodDeclaration m = getSurroundingMethod(node);
				if(m == null) {
					reporter.reportUserProblem("@Perm annotation must be for a method", 
							node, PluralAnnotationAnalysis.this.getName());
				}

				Option<Object> req_ = getAnnotationParam(node.resolveAnnotationBinding(), "requires");
				Option<Object> ens_ = getAnnotationParam(node.resolveAnnotationBinding(), "ensures");
				
				if( req_.isSome() ) {
					String perm = (String)req_.unwrap();
					Option<String> parse_error = PermParser.getParseError(perm);
					if( parse_error.isSome() ) {
						reporter.reportUserProblem("Parse error in annotation string: " + parse_error.unwrap(), 
								node, PluralAnnotationAnalysis.this.getName());
					}
					else if(!"".equals(perm)) {
						Option<String> problem = 
						PermParser.accept(perm, new PreconditionParamVisitor(m.resolveBinding()));
						if(problem.isSome())
							reporter.reportUserProblem(problem.unwrap(), 
									node, PluralAnnotationAnalysis.this.getName());
					}
				}
				if( ens_.isSome() ) {
					String perm = (String)ens_.unwrap();
					Option<String> parse_error = PermParser.getParseError(perm);
					if( parse_error.isSome() ) {
						reporter.reportUserProblem("Parse error in annotation string: " + parse_error.unwrap(), 
								node, PluralAnnotationAnalysis.this.getName());
					}
					else if(!"".equals(perm)) {
						Option<String> problem = 
							PermParser.accept(perm, new PostconditionParamVisitor(m.resolveBinding()));
							if(problem.isSome())
								reporter.reportUserProblem(problem.unwrap(), 
										node, PluralAnnotationAnalysis.this.getName());
					}
				}
			}
			else if(isPermAnnotation(node)) {
				// TODO refactor location restrictions for annotations in some principled way
				ASTNode annotated = getAnnotatedElement(node);
				ITypeBinding type;
				if(annotated == null) {
					type = null;
				}
				else if(annotated instanceof MethodDeclaration) {
					MethodDeclaration m = (MethodDeclaration) annotated;
					if(isResultAnnotation(node)) {
						type = m.resolveBinding().getReturnType();
						if(isVoid(type)) {
							reportUserProblem("@ResultXxx annotations must be on non-void methods", node);
							type = null;
						}
					}
					else { // receiver annotation
						if(Modifier.isStatic(m.getModifiers())) {
							PluralAnnotationAnalysis.this.reportUserProblem(
									"Receiver annotation must be on non-static method", node);
							type = null;
						}
						else 
							type = m.resolveBinding().getDeclaringClass();
					}
				}
				else if(annotated instanceof SingleVariableDeclaration) {
					// should be a parameter...
					SingleVariableDeclaration d = (SingleVariableDeclaration) annotated;
					type = d.resolveBinding().getType();
				}
				else if(annotated instanceof VariableDeclarationFragment && 
						annotated.getParent() instanceof FieldDeclaration) {
					// should be a field...
					VariableDeclarationFragment frag = (VariableDeclarationFragment) annotated;
					FieldDeclaration d = (FieldDeclaration) annotated.getParent();
					if(!Modifier.isStatic(d.getModifiers())) {
						PluralAnnotationAnalysis.this.reportUserProblem(
								"Permission annotations must be on non-static method, static field, or method parameter", node);
						type = null;
					}
					// TODO check requires to only contain marker states / ensures to be empty
					type = frag.resolveBinding().getType();
				}
				else {
					PluralAnnotationAnalysis.this.reportUserProblem(
							"Permission annotations must be on non-static method, static field, or method parameter", node);
					type = null;
				}
				
				// check that required / ensured states are inside declared root
				if(type != null) {
					Option<Object> guarantee = getDeclaredAttribute(node, "guarantee");
					Option<Object> value = getDeclaredAttribute(node, "value");
					if(guarantee.isSome() && value.isSome()) {
						PluralAnnotationAnalysis.this.reportUserProblem(
								"Only use one of the equivalent attributes: guarantee and value", 
								node);
					}
					else {
						if(guarantee.isNone()) guarantee = value;
						if(guarantee.isSome()) {
							Object root = guarantee.unwrap();
							if(root instanceof String) {
								Object[] req = getArrayAttribute(node, "requires");
								for(Object o : req) {
									if(o instanceof String) {
										Option<String> msg = checkPermissionStates(type, (String) root, (String) o);
										if(msg.isSome())
											PluralAnnotationAnalysis.this.reportUserProblem(msg.unwrap(), node);
									}
								}
								Object[] ens = getArrayAttribute(node, "ensures");
								for(Object o : ens) {
									if(o instanceof String) {
										Option<String> msg = checkPermissionStates(type, (String) root, (String) o);
										if(msg.isSome())
											PluralAnnotationAnalysis.this.reportUserProblem(msg.unwrap(), node);
									}
								}
							}
							else {
								// this shouldn't happen
								PluralAnnotationAnalysis.this.reportUserProblem(
										"Root attribute must be a string", 
										node);
							}
						} // else nothing to check...
					}
				}
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(SingleMemberAnnotation node) {
			checkCasesAndPermOnSameMethod(node);

			// check that @Cases is not empty, i.e., prevent @Cases({ })
			if(isCasesAnno(node)) {
				if(! checkValueArrayNonEmpty(node.resolveAnnotationBinding()))
					reporter.reportUserProblem("Must have cases in @Cases", node, PluralAnnotationAnalysis.this.getName());
			}
			// check that @ResultXxx is not on constructor or void method
			else if(isPermAnnotation(node)) {
				ASTNode annotated = getAnnotatedElement(node);
				if(annotated != null && annotated instanceof MethodDeclaration) {
					if(isResultAnnotation(node)) { 
						if(isVoid(((MethodDeclaration) annotated).resolveBinding().getReturnType())) 
							reportUserProblem("@ResultXxx annotations must be on non-void method", node);
					}
					else {
						if(Modifier.isStatic(((MethodDeclaration) annotated).getModifiers())) {
							PluralAnnotationAnalysis.this.reportUserProblem(
									"Receiver annotation must be on non-static method", node);
						}
					}
				}
				else if(isResultAnnotation(node)) {
					reportUserProblem("@ResultXxx annotations must be on non-void method", node);
				}
			}

			super.endVisit(node);
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			// reset cases and perm flags
			sawCases = sawPerm = false;
			return super.visit(node);
		}
		
		@Override
		public void endVisit(MethodDeclaration node) {
			String ambiguity = checkAmbiguousSpecification(node.resolveBinding());
			if(ambiguity != null)
				reporter.reportUserProblem(ambiguity, node, PluralAnnotationAnalysis.this.getName());
			
			super.endVisit(node);
		}

		//
		// use checks
		//
		
		@Override
		public void endVisit(MethodInvocation node) {
			// check at call sites in case the ambiguity happens in a method not currently checked 
			String ambiguity = checkAmbiguousSpecification(node.resolveMethodBinding());
			if(ambiguity != null)
				reporter.reportUserProblem(ambiguity + ".  Provide specification in " + 
						node.resolveMethodBinding().getDeclaringClass().getName(), 
						node, PluralAnnotationAnalysis.this.getName());
			super.endVisit(node);
		}

		@Override
		public void endVisit(SuperMethodInvocation node) {
			// check at call sites in case the ambiguity happens in a method not currently checked 
			String ambiguity = checkAmbiguousSpecification(node.resolveMethodBinding());
			if(ambiguity != null)
				reporter.reportUserProblem(ambiguity + ".  Provide specification in " + 
						node.resolveMethodBinding().getDeclaringClass().getName(), 
						node, PluralAnnotationAnalysis.this.getName());
			super.endVisit(node);
		}

		/**
		 * Call this method for every annotation!!!
		 * Check whether we saw both @Cases and @Perm when processing the given node
		 * @param node
		 */
		private void checkCasesAndPermOnSameMethod(Annotation node) {
			if(sawCases && sawPerm)
				// already reported error about this
				return;
			
			if(isCasesAnno(node)) 
				sawCases = true;
			
			if("edu.cmu.cs.plural.annot.Perm".equals(node.resolveTypeBinding().getQualifiedName())) {
				// only count @Perm annotations outside @Cases
				ASTNode n = node.getParent();
				while(n != null && ! (n instanceof MethodDeclaration)) {
					if(n instanceof Annotation && isCasesAnno((Annotation) n))
						return;
					n = n.getParent();
				}
				sawPerm = true;
			}
			
			// check if there is both a @Cases and a @Perm on this method
			if(sawCases && sawPerm) {
				reporter.reportUserProblem("@Perm next to @Cases is ignored!",
						node, PluralAnnotationAnalysis.this.getName());
			}
		}

		/**
		 * Finds the nearest method declaration around the given node, if any.
		 * @param node
		 * @return the nearest method declaration around the given node or <code>null</code>.
		 */
		private MethodDeclaration getSurroundingMethod(ASTNode node) {
			return Utilities.getMethodDeclaration(node);
		}
		
		private ASTNode getAnnotatedElement(Annotation node) {
			// FIXME this doesn't work b/c of the intermediate value nodes between annotation nodes
			ASTNode result = node;
			while(result != null && result instanceof Annotation)
				result = result.getParent();
			return result;
		}

	}
	
	private static boolean isVoid(ITypeBinding t) {
		if(!t.isPrimitive())
			return false;
		return "void".equals(t.getName());
	}

	/**
	 * @param node
	 * @return
	 */
	private boolean isCasesAnno(Annotation node) {
		return "edu.cmu.cs.plural.annot.Cases".equals(node.resolveTypeBinding().getQualifiedName());
	}

	/**
	 * @param node
	 * @return
	 */
	private boolean isPermAnnotation(Annotation node) {
		if(permAnnos == null) {
			permAnnos = new HashSet<String>();
			permAnnos.add("edu.cmu.cs.plural.annot.Unique");
			permAnnos.add("edu.cmu.cs.plural.annot.Full");
			permAnnos.add("edu.cmu.cs.plural.annot.Share");
			permAnnos.add("edu.cmu.cs.plural.annot.Imm");
			permAnnos.add("edu.cmu.cs.plural.annot.Pure");
		}
		return permAnnos.contains(node.resolveTypeBinding().getQualifiedName()) ||
				isResultAnnotation(node);
	}

	/**
	 * @param node
	 * @return
	 */
	private boolean isResultAnnotation(Annotation node) {
		if(resultAnnos == null) {
			resultAnnos = new HashSet<String>();
			resultAnnos.add("edu.cmu.cs.plural.annot.ResultUnique");
			resultAnnos.add("edu.cmu.cs.plural.annot.ResultFull");
			resultAnnos.add("edu.cmu.cs.plural.annot.ResultShare");
			resultAnnos.add("edu.cmu.cs.plural.annot.ResultImm");
			resultAnnos.add("edu.cmu.cs.plural.annot.ResultPure");
		}
		return resultAnnos.contains(node.resolveTypeBinding().getQualifiedName());
	}

	/**
	 * @param string
	 * @param node
	 */
	public void reportUserProblem(String problemDescription, ASTNode node) {
		reporter.reportUserProblem(problemDescription, node, getName());
	}

	/**
	 * Given a reference, make sure it's a field and that it exists
	 * in the given set of field_names.
	 */
	private static Option<String> checkField(PrimaryExpr expr, final Set<String> field_names) {
		return
		expr.dispatch(new PrimaryExprVisitor<Option<String>>() {

			@Override
			public Option<String> visitBool(BoolLiteral bool) {
				return Option.none();
			}

			@Override
			public Option<String> visitId(Identifier id) {
				if( !field_names.contains(id.getName()) )
					return Option.some(id.getName());
				else
					return Option.none();
			}

			@Override
			public Option<String> visitNull(Null nul) {
				return Option.none();
			}

			@Override
			public Option<String> visitParam(ParamReference paramReference) {
				return Option.some(paramReference.getParamString());
			}});
	}
	
	/**
	 * This method checks the well-formedness of a ClassStates annotation. It does so
	 * given a type declaration, so it is entirely possible that no annotations of this
	 * type will even exist.
	 * 
	 * Returned list is a list of error-full AST nodes and the error messages that should
	 * be printed with them.
	 */
	private List<Pair<ASTNode, String>> 
	checkClassStatesAnnot(AnnotationDatabase annotationDatabase,
			TypeDeclaration node) {
		
		/*
		 * No matter what, we need a set of the types in the class.
		 */
		final Map<String, ITypeBinding> field_names = new HashMap<String, ITypeBinding>();
		for( IVariableBinding field : node.resolveBinding().getDeclaredFields() ) {
			field_names.put(field.getName(), field.getType());
		}
		field_names.put("this", node.resolveBinding());
		field_names.put("this!fr", node.resolveBinding());
		if(node.resolveBinding().getSuperclass() != null)
			// accept superclass references
			field_names.put("super", node.resolveBinding().getSuperclass());
		
		/*
		 * First check to see that problems even exist, which they may not.
		 */
		final List<ICrystalAnnotation> annos = 
			annotationDatabase.getAnnosForType(node.resolveBinding());
		boolean is_problem = false;
		String problem_field = "";
		
		outer_loop:
		for( ICrystalAnnotation anno : annos ) {
			if( anno instanceof ClassStateDeclAnnotation ) {
				for( StateDeclAnnotation sda : ((ClassStateDeclAnnotation)anno).getStates() ) {
					// Get invariant string
					String inv = sda.getInv();
					Option<String> problem =
					PermParser.accept(inv, new InvariantFieldVisitor(field_names));
						
						if( problem == null ) {
							continue;
						}
						else if( problem.isSome() ) {
							is_problem = true;
							problem_field = problem.unwrap();
							break outer_loop;
						}
				}
			}
		}
		
		if( !is_problem ) 
			return Collections.emptyList();
		
		/*
		 * There is a problem, we need to find the appropriate annotation to flag.
		 */
		for( Object obj : node.modifiers() ) {
			if( obj instanceof SingleMemberAnnotation ) {
				final boolean this_one = 
					((SingleMemberAnnotation)obj).getTypeName().getFullyQualifiedName().endsWith("ClassStates");
					
				if( this_one ) {
					// FIXME error message could talk about a state now....
					// TODO find the right @State annotation and make it work for NormalAnnotation
					return Collections.singletonList(new Pair<ASTNode, String>((SingleMemberAnnotation)obj,
					"This annotation refers to field " + problem_field + " of class " +
					node.getName() + " which cannot be found. Possible misspelling or use of " +
					"superclass field."));
				}
			}
		}
		
		throw new IllegalStateException("Probable bug");
	}

	/**
	 * Returns the value of the annotation parameter with the given name, or
	 * NONE if it is not in the given annotation.
	 */
	private Option<Object> getAnnotationParam(IAnnotationBinding anno, String p_name) {
		for(IMemberValuePairBinding p : anno.getAllMemberValuePairs()) {
			if(p_name.equals(p.getName())) {
				return Option.some(p.getValue());
			}
		}		
		return Option.none();
	}
	
	/**
	 * Returns the value of the annotation parameter with the given name, or
	 * NONE if it is not <b>declared</b>in the given annotation.
	 */
	private static Option<Object> getDeclaredAttribute(Annotation anno, String p_name) {
		for(IMemberValuePairBinding p : anno.resolveAnnotationBinding().getDeclaredMemberValuePairs()) {
			if(p_name.equals(p.getName())) {
				return Option.some(p.getValue());
			}
		}
		return Option.none();
	}
	
	/**
	 * Returns the given annotation attribute as an array, which will
	 * be empty if the attribute is not <b>defined</b>.
	 */
	private static Object[] getArrayAttribute(Annotation anno, String p_name) {
		for(IMemberValuePairBinding p : anno.resolveAnnotationBinding().getAllMemberValuePairs()) {
			if(p_name.equals(p.getName())) {
				Object result = p.getValue();
				if(result instanceof Object[])
					return (Object[]) result;
				return new Object[] { result };
			}
		}
		return new Object[0];
	}
	
	/**
	 * Returns the value of the parameter named "value" for the given
	 * annotation binding, or NONE if there is no key called "value."
	 */
	private Option<Object> getAnnotationValue(IAnnotationBinding anno) {
		return getAnnotationParam(anno, "value");
	}
	
	/**
	 * Checks that the given annotation has a non-empty "value" array parameter.
	 * @param casesAnnotation
	 * @return <code>true</code> if the "value" array parameter is non-empty, 
	 * <code>false</code> otherwise
	 */
	private boolean checkValueArrayNonEmpty(
			IAnnotationBinding casesAnnotation) {
		
		Option<Object> value_ = getAnnotationValue(casesAnnotation);
		
		if( value_.isNone() ) return false;
		
		Object value = value_.unwrap();
		
		if( value instanceof Object[] ) {
			return ((Object[]) value).length > 0;
		}
		else {
			return true;
		}
	}

	/**
	 * Checks whether the effective specification for the given method is ambiguous
	 * because specs are inherited from multiple places.
	 * @param binding
	 * @return An error message if the specification for the given method is ambiguous,
	 * <code>null</code> otherwise (i.e., <code>null</code> means everything is ok).
	 */
	private String checkAmbiguousSpecification(
			IMethodBinding binding) {
		Set<IMethodBinding> specSources = getRepository().findAllMethodsWithSpecification(binding);
		if(specSources.size() > 1) {
			StringBuffer error = new StringBuffer();
			error.append("Ambiguous protocol annotations inherited from types ");
			boolean first = true;
			for(IMethodBinding m : specSources) {
				if(first)
					first = false;
				else
					error.append(", ");
				error.append(m.getDeclaringClass().getName());
			}
			return error.toString();
		}
		return null;
	}
	
	abstract class ReferenceVisitor implements AccessPredVisitor<Option<String>> {
		
		protected abstract Option<String> checkPrimary(PrimaryExpr expr);
		
		@Override
		public Option<String> visit(Disjunction disj) {
			Option<String> c_1 = disj.getP1().accept(this);
			Option<String> c_2 = disj.getP2().accept(this);

			if( c_1.isSome() )
				return c_1;
			if( c_2.isSome() )
				return c_2;

			return Option.none();
		}

		@Override
		public Option<String> visit(Conjunction conj) {
			Option<String> c_1 = conj.getP1().accept(this);
			Option<String> c_2 = conj.getP2().accept(this);

			if( c_1.isSome() )
				return c_1;
			if( c_2.isSome() )
				return c_2;

			return Option.none();
		}

		@Override
		public Option<String> visit(Withing withing) {
			Option<String> c_1 = withing.getP1().accept(this);
			Option<String> c_2 = withing.getP2().accept(this);

			if( c_1.isSome() )
				return c_1;
			if( c_2.isSome() )
				return c_2;

			return Option.none();
		}

		@Override
		public Option<String> visit(PermissionImplication permissionImplication) {
			Option<String> c_1 = permissionImplication.ant().accept(this);
			Option<String> c_2 = permissionImplication.cons().accept(this);

			if( c_1.isSome() )
				return c_1;
			if( c_2.isSome() )
				return c_2;

			return Option.none();
		}

		@Override
		public Option<String> visit(BinaryExprAP binaryExpr) {
			Option<String> c_1 = checkPrimary(binaryExpr.getBinExpr().getE1());
			Option<String> c_2 = checkPrimary(binaryExpr.getBinExpr().getE2());
			if( c_1.isSome() )
				return c_1;
			if( c_2.isSome() )
				return c_2;

			return Option.none();
		}

		@Override
		public Option<String> visit(EqualsExpr equalsExpr) {
			Option<String> c_1 = checkPrimary(equalsExpr.getE1());
			Option<String> c_2 = checkPrimary(equalsExpr.getE2());
			if( c_1.isSome() )
				return c_1;
			if( c_2.isSome() )
				return c_2;

			return Option.none();
		}

		@Override
		public Option<String> visit(NotEqualsExpr notEqualsExpr) {
			Option<String> c_1 = checkPrimary(notEqualsExpr.getE1());
			Option<String> c_2 = checkPrimary(notEqualsExpr.getE2());
			if( c_1.isSome() )
				return c_1;
			if( c_2.isSome() )
				return c_2;

			return Option.none();
		}

	}
	
	/**
	 * A visitor to check that the fields actually exist.
	 */
	class InvariantFieldVisitor extends ReferenceVisitor {
		private final Map<String, ITypeBinding> field_names; 

		InvariantFieldVisitor(Map<String, ITypeBinding> field_names) {
			this.field_names = field_names;
		}

		@Override
		protected Option<String> checkPrimary(PrimaryExpr expr) {
			return checkField(expr, field_names.keySet());
		}

		@Override
		public Option<String> visit(TempPermission perm) {
			if( perm.getRef() instanceof Identifier ) {
//				return checkField(perm.getRef(), field_names);
				String f = ((Identifier) perm.getRef()).getName();
				if(!field_names.containsKey(f))
					return Option.some(f);
				ITypeBinding t = field_names.get(f);
				return PluralAnnotationAnalysis.this.checkPermissionStates(t, perm.getRoot(), perm.getStateInfo());
			}
			else if( perm.getRef() instanceof ParamReference )
				return Option.some(((ParamReference) perm.getRef()).getParamString());
//				return checkField(perm.getRef(), field_names);
			else
				return Utilities.nyi();
		}

		@Override
		public Option<String> visit(StateOnly stateOnly) {
			if(stateOnly.getVar() instanceof Identifier)
				// TODO check state somehow?
				return checkField((Identifier) stateOnly.getVar(), field_names.keySet());
			else if(stateOnly.getVar() instanceof ParamReference)
				return Option.some(((ParamReference) stateOnly.getVar()).getParamString());
			else
				return Utilities.nyi();
		}

	}

	/**
	 * A visitor to check that the fields actually exist.
	 */
	class PreconditionParamVisitor extends ReferenceVisitor {
		
		protected final IMethodBinding meth;

		PreconditionParamVisitor(IMethodBinding meth) {
			this.meth = meth;
		}

		@Override
		protected Option<String> checkPrimary(PrimaryExpr expr) {
			return expr.dispatch(new PrimaryExprVisitor<Option<String>>() {

				@Override
				public Option<String> visitBool(BoolLiteral bool) {
					return Option.none();
				}

				@Override
				public Option<String> visitId(Identifier id) {
					if(PreconditionParamVisitor.this.identifierType(id) == null)
						return Option.some("Not a valid identifier: " + id.getName());
					else
						return Option.none();
				}

				@Override
				public Option<String> visitNull(Null nul) {
					return Option.none();
				}

				@Override
				public Option<String> visitParam(ParamReference paramReference) {
					if(PreconditionParamVisitor.this.paramType(paramReference) == null)
						return Option.some("Not a valid parameter: " + paramReference.getParamString() + " (parameters are referenced with # + their 0-based index)");
					else
						return Option.none();
				}
				
			});
		}

		/**
		 * @param paramReference
		 * @return
		 */
		protected ITypeBinding paramType(ParamReference paramReference) {
			Integer p = paramReference.getParamPosition();
			if(p == null)
				// not a number
				return null;
			else if(p < 0 || p >= meth.getParameterTypes().length)
				// not a parameter number for this method
				return null;
			else
				// look up parameter type
				return meth.getParameterTypes()[p];
		}

		/**
		 * Returns the type of the given identifier, 
		 * ignores <i>result</i> in the pre-condition.
		 * @param id
		 * @return
		 */
		protected ITypeBinding identifierType(Identifier id) {
			String name = id.getName();
			if(Modifier.isStatic(meth.getModifiers()))
				// static methods cannot refer to any names in their pre-condition
				return null;
			else if("this".equals(name) || "this!fr".equals(name))
				// receiver
				return meth.getDeclaringClass();
			else 
				// other names not allowed
				return null;
		}
		

		@Override
		public Option<String> visit(TempPermission perm) {
			ITypeBinding t;
			if( perm.getRef() instanceof Identifier ) {
				t = identifierType((Identifier) perm.getRef());
				if(t == null)
					return Option.some("Not a valid identifier: " + ((Identifier) perm.getRef()).getName());
			}
			else if( perm.getRef() instanceof ParamReference ) {
				t = paramType((ParamReference) perm.getRef());
				if(t == null)
					return Option.some("Not a valid parameter identifier: " + ((ParamReference) perm.getRef()).getParamString() + " (parameters are identified with # + their 0-based index)");
			}
			else
				return Utilities.nyi();
			assert t != null;
			return PluralAnnotationAnalysis.this.checkPermissionStates(t, perm.getRoot(), perm.getStateInfo());
		}

		@Override
		public Option<String> visit(StateOnly stateOnly) {
			if(stateOnly.getVar() instanceof Identifier)
				return checkPrimary((Identifier) stateOnly.getVar());
			else if(stateOnly.getVar() instanceof ParamReference)
				return checkPrimary((ParamReference) stateOnly.getVar());
			else
				return Utilities.nyi();
		}

	}
	
	class PostconditionParamVisitor extends PreconditionParamVisitor {

		/**
		 * @param meth
		 */
		PostconditionParamVisitor(IMethodBinding meth) {
			super(meth);
		}

		/**
		 * Returns the type of the given identifier,
		 * overriding inherited method to <i>not</i> ignore <i>result</i>
		 * @param id
		 * @return
		 */
		@Override
		protected ITypeBinding identifierType(Identifier id) {
			ITypeBinding result = super.identifierType(id);
			if(result != null)
				return result;
			else if("result".equals(id.getName()) && 
					!PluralAnnotationAnalysis.isVoid(meth.getReturnType()))
				return meth.getReturnType();
			else 
				// other names not allowed
				return null;
		}
		
	}

	/**
	 * @param t
	 * @param root
	 * @param stateInfo
	 */
	public Option<String> checkPermissionStates(ITypeBinding t, String root,
			String... stateInfo) {
		StateSpace space = getRepository().getStateSpace(t);
		List<String> wrong = new LinkedList<String>();
		for(String s : stateInfo) {
			// TODO check whether state is "known"?  Not really helpful since we take every state as known
			if(!space.firstBiggerThanSecond(root, s))
				wrong.add(s);
		}
		
		if(wrong.isEmpty())
			// this handles a empty stateInfo: wrong cannot possibly be non-empty
			return Option.none();
		if(wrong.size() == 1)
			return Option.some("Inconsistent state space references: " + wrong.get(0) + " not inside of " + root);
		return Option.some("Inconsistent state space references: " + wrong.toString() + " not inside of " + root);
	}
}
