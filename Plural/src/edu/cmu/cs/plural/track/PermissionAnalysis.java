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
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.bridge.LatticeElementOps;
import edu.cmu.cs.crystal.flow.BooleanLabel;
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.flow.LabeledSingleResult;
import edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.model.AssignmentInstruction;
import edu.cmu.cs.crystal.tac.model.CastInstruction;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.DotClassInstruction;
import edu.cmu.cs.crystal.tac.model.InstanceofInstruction;
import edu.cmu.cs.crystal.tac.model.LoadArrayInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.LoadLiteralInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewArrayInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.SourceVariable;
import edu.cmu.cs.crystal.tac.model.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.TypeVariable;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.plural.alias.DisjointSetTuple;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * @author Kevin Bierhoff
 *
 */
public class PermissionAnalysis extends AbstractCrystalMethodAnalysis {

	private ITACFlowAnalysis<DisjointSetTuple<Variable, Permissions>> fa;

	private HashMap<TACInstruction, String> problems;

	/**
	 * 
	 */
	public PermissionAnalysis() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis#analyzeMethod(com.surelogic.ast.java.operator.IMethodDeclarationNode)
	 */
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		// create a transfer function object and pass it to a new FlowAnalysis
		PermTransferFunction tf = new PermTransferFunction(d);
		fa = new TACFlowAnalysis<DisjointSetTuple<Variable, Permissions>>(tf, 
				this.analysisInput.getComUnitTACs().unwrap());
		
		// must call getResultsAfter at least once on this method,
		// or the analysis won't be run on this method
		fa.getResultsAfter(d);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis#afterAllMethods(org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	@Override
	public void afterAllMethods(ICompilationUnit compUnit, CompilationUnit rootNode) {
		// report any problems found
		for(TACInstruction i : problems.keySet())
			report(i, problems.get(i));
		// clear problems
		problems = null;

		super.afterAllMethods(compUnit, rootNode);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis#beforeAllMethods(org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	@Override
	public void beforeAllMethods(ICompilationUnit compUnit, CompilationUnit rootNode) {
		super.beforeAllMethods(compUnit, rootNode);
		problems = new HashMap<TACInstruction, String>();
		// TODO use annotation database
	}

	/**
	 * Reports problems to the user
	 * @param instr Instruction with problem
	 * @param problem Problem description
	 */
	private void report(TACInstruction instr, String problem) {
		reporter.reportUserProblem(problem, instr.getNode(), this.getName());
	}
	
	/**
	 * @param list
	 * @return
	 */
	private static <T> List<T> emptyForNull(List<T> list) {
		if(list == null)
			return Collections.emptyList();
		return list;
	}

	private class PermTransferFunction 
			extends AbstractTACBranchSensitiveTransferFunction<DisjointSetTuple<Variable, Permissions>> {
		
		private MethodDeclaration analyzedMethod;

		public PermTransferFunction(MethodDeclaration analyzedMethod) {
			this.analyzedMethod = analyzedMethod;
		}

		public ILatticeOperations<DisjointSetTuple<Variable, Permissions>> createLatticeOperations(MethodDeclaration d) {
			return LatticeElementOps.create(
					new DisjointSetTuple<Variable, Permissions>(Permissions.BOTTOM));
		}
		
		public DisjointSetTuple<Variable, Permissions> createEntryValue(MethodDeclaration d) {
			return new DisjointSetTuple<Variable, Permissions>(Permissions.BOTTOM);
		}
		
		public Permissions get(Variable x, DisjointSetTuple<Variable, Permissions> value) {
			Permissions result = value.get(x);
			if(result.equals(Permissions.BOTTOM)) {
				if(x instanceof SourceVariable) {
					if(((SourceVariable) x).getBinding().isParameter()) {
						for(IAnnotationBinding a : ((SourceVariable) x).getBinding().getAnnotations()) {
							// use declared pre-condition
							Permission p = Permission.createPermissionIfPossible(x.getSourceString(), a).getRequires();
							if(p != null)
								return Permissions.createSingleton(p);
						}
					}
				}
				if(x instanceof ThisVariable) {
					if(analyzedMethod.isConstructor())
						// use "initial" state for newly created objects 
						return Permissions.createSingleton(Permission.createUnique(StateSpace.STATE_ALIVE));
					if(((ThisVariable) x).isQualified() == false) {
						SimplePermissionAnnotation thisanno = receiverPermission(analyzedMethod.resolveBinding());
						if(thisanno != null)
							return Permissions.createSingleton(thisanno.getRequires());
					}
					// TODO qualified this...
				}
				if(x instanceof TypeVariable) {
					// assume type variables are shared
					return Permissions.createSingleton(Permission.createShare(StateSpace.STATE_ALIVE));
				}
				// TODO default annotation?
				return Permissions.BOTTOM;
			}
			else
				return result;
		}
		
		private DisjointSetTuple<Variable, Permissions> put(Variable x, DisjointSetTuple<Variable, Permissions> value, Permissions newElem) {
			return value.put(x, newElem);
		}

		private DisjointSetTuple<Variable, Permissions> put(Variable x, DisjointSetTuple<Variable, Permissions> value, Permission singletonElem) {
			return put(x, value, Permissions.createSingleton(singletonElem));
		}

		private DisjointSetTuple<Variable, Permissions> singleton(Variable x, DisjointSetTuple<Variable, Permissions> value, Permissions newElem) {
			return value.singleton(x, newElem);
		}

		private DisjointSetTuple<Variable, Permissions> singleton(Variable x, DisjointSetTuple<Variable, Permissions> value, Permission singletonElem) {
			return singleton(x, value, Permissions.createSingleton(singletonElem));
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(ArrayInitInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			value = put(instr.getTarget(), value, Permission.createUnique("alive"));
			return LabeledSingleResult.createResult(value, labels);
		}

		private IResult<DisjointSetTuple<Variable, Permissions>> addImmutableTarget(AssignmentInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			// permission for primitive values (ints, booleans, etc.)
			value = singleton(instr.getTarget(), value, Permission.createImmutable("alive"));
			return LabeledSingleResult.createResult(value, labels);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(CastInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			// do we support this?
			return super.transfer(instr, labels, value);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(DotClassInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			// immutable permission to Class object
			return addImmutableTarget(instr, labels, value);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(ConstructorCallInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			return super.transfer(instr, labels, value);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(CopyInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			if(instr.getTarget().resolveType().isPrimitive())
				// immutable permission to primitive value
				return addImmutableTarget(instr, labels, value);
			
			// TODO Variable copying
			if(instr.getTarget() instanceof SourceVariable &&  
					((SourceVariable) instr.getTarget()).getBinding().isParameter()) {
				problems.put(instr, "Assignments to method parameters not supported");
			}
			else {
				value = value.addKeyToSet(instr.getTarget(), instr.getOperand());
			}
			return LabeledSingleResult.createResult(value, labels);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(InstanceofInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			// immutable permission to boolean result
			return addImmutableTarget(instr, labels, value);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(LoadLiteralInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			if(instr.isNull())
				// no permission to null value
				return super.transfer(instr, labels, value);
			// immutable permission for primitive or string literal
			return addImmutableTarget(instr, labels, value);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(LoadArrayInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			// TODO permissions for elements of source array
			return super.transfer(instr, labels, value);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(LoadFieldInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			// TODO make sure there is a permission for accessed object
			if(instr.resolveFieldBinding().getType().isPrimitive()) {
				return addImmutableTarget(instr, labels, value);
			}
			Variable origin = instr.getSourceObject();
//			if((origin instanceof ThisVariable && ((ThisVariable) origin).isQualified() == false) ||
//					origin instanceof TypeVariable) {
				// read from method receiver or static field
				SimplePermissionAnnotation fieldanno = fieldAnnotation(instr.resolveFieldBinding());
				if(fieldanno != null) {
					// TODO better treatment of fields than with requires/ensures?
					// TODO receiver state-dependent field permissions
					Variable target = instr.getTarget();
					Permissions originPerm = get(origin, value);
					if(originPerm == null)
						; // no permission for loaded field --> will be impossible to call annotated method
					else if(fieldanno.getRequires().isUnique())
						problems.put(instr, "Cannot soundly handle access to @Unique field: " + instr.getFieldName());
					else if(fieldanno.getRequires().isFull()) {
						problems.put(instr, "Can only soundly use @Full field as @Pure: " + instr.getFieldName());
						value = singleton(target, value, fieldanno.getRequires().copyNewKind(PermissionKind.PURE));
					}
					else if(Permissions.isReadOnlyAccess(originPerm))
						// share becomes pure
						value = singleton(target, value, fieldanno.getRequires().purify());
					else
						// use share, imm, pure as is for modifying this-permission
						value = singleton(target, value, fieldanno.getRequires());
				}
				// TODO what if field is not annotated?
//			}
//			else
//				problems.put(instr, "No support for access to foreign object: " + origin.getSourceString());
			return LabeledSingleResult.createResult(value, labels);
		}

		/**
		 * @param binding
		 * @return
		 */
		private SimplePermissionAnnotation fieldAnnotation(IVariableBinding field) {
			SimplePermissionAnnotation result = null;
			for(ICrystalAnnotation a : 
				emptyForNull(getAnnoDB().getAnnosForField(field))) {
				result = SimplePermissionAnnotation.createPermissionIfPossible(field.getName(), a, StateSpace.SPACE_TOP);
				if(result != null) {
					// simulate capturing of permission during field assignment
					result.setReturned(false);
					return result;
				}
			}
			return null;
		}

		private AnnotationDatabase getAnnoDB() {
			return analysisInput.getAnnoDB();
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(MethodCallInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			// receiver pre-condition
			SimplePermissionAnnotation thisanno = receiverPermission(instr.resolveBinding());
			Permissions thispre = get(instr.getReceiverOperand(), value);
			checkPermissions(instr, instr.getReceiverOperand(), thispre, thisanno);
			value = put(instr.getReceiverOperand(), value, Permissions.subtractPermissions(thispre, thisanno));
			
			// argument pre-conditions
			List<SimplePermissionAnnotation> argannos = argumentPermissions(instr.resolveBinding(), instr.getArgOperands().size());
			List<Variable> args = instr.getArgOperands();
			ArrayList<Permissions> argpre = new ArrayList<Permissions>(args.size());
			for(int i = 0; i < argannos.size(); i++) {
				Variable x = args.get(i);
				Permissions p = get(x, value);
				argpre.add(p);
				SimplePermissionAnnotation a = argannos.get(i);
				checkPermissions(instr, x, p, a);
				value = put(x, value, Permissions.subtractPermissions(p, a));
			}
			
			// put borrowed permissions for arguments back in, if any
			for(int i = argannos.size() - 1; i >= 0; i--) {
				value = put(args.get(i), value, Permissions.filterPermissions(argpre.get(i), argannos.get(i)));
			}
			
			// put borrowed permissions for receiver back in, if any
			value = put(instr.getReceiverOperand(), value, Permissions.filterPermissions(thispre, thisanno));
			
			// add permissions for result
			SimplePermissionAnnotation resultanno = resultPermission(instr.resolveBinding());
			if(resultanno != null) {
				// use singleton because we want to treat result separate from other variables
				if(resultanno.isReturned())
					value = singleton(instr.getTarget(), value, Permissions.createSingleton(resultanno.getEnsures()));
				else {
					problems.put(instr, "result annotation sets return flag to false");
					value = singleton(instr.getTarget(), value, Permissions.BOTTOM);
					return LabeledSingleResult.createResult(value, labels);
				}
			}
			else if(instr.getTarget().resolveType().isPrimitive())
				// immutable permission for primitive result
				value = singleton(instr.getTarget(), value, Permission.createImmutable("alive"));
			else
				value = singleton(instr.getTarget(), value, Permissions.BOTTOM);
			
			// process boolean state tests
			LabeledResult<DisjointSetTuple<Variable, Permissions>> result = 
				LabeledResult.createResult(labels, value);
			for(ILabel label : labels) {
				if(label instanceof BooleanLabel) {
					boolean outcome = ((BooleanLabel) label).getBranchValue();
					IndicatesAnnotation indicator = booleanIndicator(instr.resolveBinding(), outcome);
					if(indicator != null) {
						DisjointSetTuple<Variable, Permissions> branchResult = value.copy();
						Permissions indicatedPerms = branchResult.get(instr.getReceiverOperand());
						String newState = indicator.getIndicatedState();
						//if(Permissions.isConsistentState(indicatedPerms, newState))
							branchResult = put(instr.getReceiverOperand(), branchResult, 
									Permissions.morePreciseState(indicatedPerms, newState));
						//else
						//	problems.put(instr, "Tested state " + newState + " inconsistent with permission for " + 
						//			instr.getReceiverOperand().getSourceString() + ": " + indicatedPerms);
						result.put(label, branchResult);
					}
				}
				else {
					result.put(label, value);
				}
			}
			return result;
		}

		/**
		 * @param binding
		 * @param outcome
		 * @return
		 */
		private IndicatesAnnotation booleanIndicator(IMethodBinding binding, boolean outcome) {
			return StateTestAnnotation.createBooleanIndicatorIfPossible(binding, outcome);
		}

		/**
		 * @param instr
		 * @param receiverObject
		 * @param thisperm
		 * @param thisanno
		 */
		private boolean checkPermissions(TACInstruction instr, Variable x, Permissions perms, SimplePermissionAnnotation anno) {
			boolean result = Permissions.checkPermissions(perms, anno);
			if(result == false)
				problems.put(instr, "Need " + anno.getRequires() + " but have " + perms + " for " + x.getSourceString());
				//Crystal.getInstance().reportUserProblem("Need " + anno.getPermission() + " but have " + perms, instr.getNode(), PermissionAnalysis.this);
			return result;
		}

		/**
		 * Find "result" annotation.
		 * @param binding
		 * @return
		 */
		private SimplePermissionAnnotation resultPermission(IMethodBinding binding) {
			if(getAnnoDB().getSummaryForMethod(binding) == null)
				return null;
			for(ICrystalAnnotation a : 
				emptyForNull(getAnnoDB().getSummaryForMethod(binding).getReturn())) {
				// by default, annotations on method itself are for receiver; 
				SimplePermissionAnnotation anno = SimplePermissionAnnotation.createPermissionIfPossible("this", a, StateSpace.SPACE_TOP);
				// permission for result must explicitly set 'var' parameter to "result"
				if(anno != null && anno.getVariable().equals("result"))
					return anno;
				// else not a permission annotation or probably receiver permission...
			}
			// no annotation: return null value
			return null;
		}

		/**
		 * For constructors, this is the "result"
		 * @param binding
		 * @return
		 */
		private SimplePermissionAnnotation receiverPermission(IMethodBinding binding) {
			if(getAnnoDB().getSummaryForMethod(binding) == null)
				return null;
			for(ICrystalAnnotation a : 
				emptyForNull(getAnnoDB().getSummaryForMethod(binding).getReturn())) {
				SimplePermissionAnnotation anno = SimplePermissionAnnotation.createPermissionIfPossible("this", a, StateSpace.SPACE_TOP);
				if(anno != null && anno.getVariable().equals("this"))
					return anno;
				// else not a permission annotation or permission for result...
			}
			// no annotation: return null value
			return null;
		}

		/**
		 * @param binding
		 * @param argumentCount 
		 * @return
		 */
		private List<SimplePermissionAnnotation> argumentPermissions(IMethodBinding binding, int argumentCount) {
			ArrayList<SimplePermissionAnnotation> result = new ArrayList<SimplePermissionAnnotation>(argumentCount);
			if(getAnnoDB().getSummaryForMethod(binding) == null) {
				for(int i = 0; i < argumentCount; i++)
					result.add(null);
				return result;
			}
			for(int i = 0; i < argumentCount; i++) {
				String paramName = getAnnoDB().getSummaryForMethod(binding).getParameterName(i);
				SimplePermissionAnnotation anno = null;
				for(ICrystalAnnotation a : 
					emptyForNull(getAnnoDB().getSummaryForMethod(binding).getParameter(i))) {
					anno = SimplePermissionAnnotation.createPermissionIfPossible(paramName, a, StateSpace.SPACE_TOP);
					// TODO do we need to validate that it's the real parameter name?
					if(anno != null) 
						break;
				}
				// add annotation or null, if parameter not annotated
				result.add(anno);
			}
			return result;
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(NewArrayInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			value = singleton(instr.getTarget(), value, Permission.createUnique("alive"));
			return LabeledSingleResult.createResult(value, labels);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(NewObjectInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			List<SimplePermissionAnnotation> argannos = argumentPermissions(instr.resolveBinding(), instr.getArgOperands().size());
			List<Variable> args = instr.getArgOperands();
			for(int i = 0; i < argannos.size(); i++) {
				Permissions p = get(args.get(i), value);
				SimplePermissionAnnotation a = argannos.get(i);
				checkPermissions(instr, args.get(i), p, a);
				p = Permissions.filterPermissions(p, a);
				value = put(args.get(i), value, p);
			}
			SimplePermissionAnnotation resultanno = receiverPermission(instr.resolveBinding());
			if(resultanno != null) {
				if(resultanno.isReturned())
					value = singleton(instr.getTarget(), value, Permissions.createSingleton(resultanno.getEnsures()));
				else {
					problems.put(instr, "constructor receiver annotation sets return flag to false");
					value = singleton(instr.getTarget(), value, Permissions.BOTTOM);
				}
			}
			else
				value = singleton(instr.getTarget(), value, Permissions.BOTTOM);
			return LabeledSingleResult.createResult(value, labels);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(StoreArrayInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			Permissions arrayperms = get(instr.getDestinationArray(), value);
			if(Permissions.isReadOnlyAccess(arrayperms)) {
				problems.put(instr, "Need modifying permission for storing into array " + instr.getDestinationArray().getSourceString() + " but have: " + arrayperms);
			}
			// TODO check if permission for stored object is suitable for destination array
			// TODO take stored permission away from source variable
			return super.transfer(instr, labels, value);
		}

		@Override
		public IResult<DisjointSetTuple<Variable, Permissions>> transfer(StoreFieldInstruction instr, List<ILabel> labels, DisjointSetTuple<Variable, Permissions> value) {
			Variable dest = instr.getDestinationObject();

			if(! Permissions.checkPermissions(get(dest, value), new Permission(StateSpace.SPACE_TOP, StateSpace.STATE_ALIVE, PermissionKind.SHARE)))
				problems.put(instr, "Modifying permission needed to assign to field of: " + dest.getSourceString());
			
//			if((dest instanceof ThisVariable && ((ThisVariable) dest).isQualified() == false) ||
//					dest instanceof TypeVariable) {
				// assign to method receiver or static field
				SimplePermissionAnnotation fieldanno = fieldAnnotation(instr.resolveFieldBinding());
				if(fieldanno != null) {
					// TODO better treatment of fields than with requires/ensures?
					// TODO receiver state-dependent field permissions
					Variable src = instr.getSourceOperand();
					checkPermissions(instr, src, get(src, value), fieldanno);
					// capture needed permission in field
					value = put(src, value, Permissions.subtractPermissions(get(src, value), fieldanno));
				}
				// TODO what if field is not annotated?
//			}
//			else
//				problems.put(instr, "No support for access to foreign object: " + dest.getSourceString());
			return LabeledSingleResult.createResult(value, labels);
		}

	}
	
}
