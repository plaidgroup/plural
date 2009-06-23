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
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.bridge.LatticeElementOps;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.AbstractingTransferFunction;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.model.AssignmentInstruction;
import edu.cmu.cs.crystal.tac.model.CastInstruction;
import edu.cmu.cs.crystal.tac.model.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.DotClassInstruction;
import edu.cmu.cs.crystal.tac.model.InstanceofInstruction;
import edu.cmu.cs.crystal.tac.model.InvocationInstruction;
import edu.cmu.cs.crystal.tac.model.LoadArrayInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.LoadLiteralInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewArrayInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.SourceVariable;
import edu.cmu.cs.crystal.tac.model.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.StoreInstruction;
import edu.cmu.cs.crystal.tac.model.TACInstruction;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;

/**
 * @author Kevin Bierhoff
 * @deprecated Use {@link PermissionAnalysis} instead to get take dynamic tests into account.
 */
public class BranchInsensitivePermissionAnalysis extends AbstractCrystalMethodAnalysis {

	private TACFlowAnalysis<TupleLatticeElement<Variable, Permissions>> fa;

	private HashMap<TACInstruction, String> problems;

	/**
	 * 
	 */
	public BranchInsensitivePermissionAnalysis() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis#analyzeMethod(com.surelogic.ast.java.operator.IMethodDeclarationNode)
	 */
	@Override
	public void analyzeMethod(MethodDeclaration d) {
		// create a transfer function object and pass it to a new FlowAnalysis
		PermTransferFunction tf = new PermTransferFunction(d);
		fa = new TACFlowAnalysis<TupleLatticeElement<Variable, Permissions>>(tf,
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
	}

	/**
	 * Reports problems to the user
	 * @param instr Instruction with problem
	 * @param problem Problem description
	 */
	private void report(TACInstruction instr, String problem) {
		reporter.reportUserProblem(problem, instr.getNode(), this.getName());
	}
	
	private class PermTransferFunction extends AbstractingTransferFunction<TupleLatticeElement<Variable, Permissions>> {
		
		private MethodDeclaration analyzedMethod;
		
		private TupleLatticeOperations<Variable, Permissions> ops =
			new TupleLatticeOperations<Variable, Permissions>(LatticeElementOps.create(Permissions.BOTTOM), Permissions.BOTTOM);

		public PermTransferFunction(MethodDeclaration analyzedMethod) {
			this.analyzedMethod = analyzedMethod;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.ITransferFunction#getLattice(com.surelogic.ast.java.operator.IMethodDeclarationNode)
		 */
		public ILatticeOperations<TupleLatticeElement<Variable, Permissions>> getLatticeOperations() {
			return ops;
		}
		
		public TupleLatticeElement<Variable, Permissions> createEntryValue(MethodDeclaration d) {
			// TODO initialize receiver and parameters
			return ops.getDefault();
		}
		
		public Permissions get(Variable x, TupleLatticeElement<Variable, Permissions> value) {
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
					if(((ThisVariable) x).isQualified() == false) {
						SimplePermissionAnnotation thisanno = receiverPermission(analyzedMethod.resolveBinding());
						if(thisanno != null)
							return Permissions.createSingleton(thisanno.getRequires());
					}
					// TODO qualified this...
				}
				// TODO default annotation?
				return Permissions.BOTTOM;
			}
			else
				return result;
		}
		
		public void put(Variable x, TupleLatticeElement<Variable, Permissions> value, Permissions newElem) {
			value.put(x, newElem);
		}

		public void put(Variable x, TupleLatticeElement<Variable, Permissions> value, Permission singletonElem) {
			put(x, value, Permissions.createSingleton(singletonElem));
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.ArrayInitInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(ArrayInitInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			put(instr.getTarget(), value, Permission.createUnique("alive"));
			return value;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.ArrayLengthInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 *
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(ArrayLengthInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			Permissions ps = get(instr.getOperand(), value);
			ps = Permissions.borrowPermissionWithStateUntouched(ps, Permission.createPure("alive"));
			put(instr.getOperand(), value, ps);
			put(instr.getTarget(), value, Permission.createImmutable("alive"));
			return value;
		}*/

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.AssignmentInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(AssignmentInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// permission for primitive values (ints, booleans, etc.)
			put(instr.getTarget(), value, Permission.createImmutable("alive"));
			return value;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.CastInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(CastInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			// do we support this?
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.ClassInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(DotClassInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// immutable permission to Class object
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.ConstructorCallInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(ConstructorCallInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.CopyInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(CopyInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			if(instr.getTarget() instanceof SourceVariable &&  
					((SourceVariable) instr.getTarget()).getBinding().isParameter()) {
				problems.put(instr, "Assignments to method parameters not supported");
			}
			else {
				problems.put(instr, "Copying not supported");
			}
			return value;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.InstanceOfInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(InstanceofInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// immutable permission to boolean result
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.InvocationInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(InvocationInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			// anything common to new and method calls?
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.LiteralInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(LoadLiteralInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.LoadArrayInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(LoadArrayInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.LoadFieldInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(LoadFieldInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.MethodCallInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(MethodCallInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// receiver pre-condition
			SimplePermissionAnnotation thisanno = receiverPermission(instr.resolveBinding());
			Permissions thispre = get(instr.getReceiverOperand(), value);
			checkPermissions(instr, instr.getReceiverOperand(), thispre, thisanno);
			put(instr.getReceiverOperand(), value, Permissions.subtractPermissions(thispre, thisanno));
			
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
				put(x, value, Permissions.subtractPermissions(p, a));
			}
			
			// put borrowed permissions for arguments back in, if any
			for(int i = argannos.size() - 1; i >= 0; i--) {
				put(args.get(i), value, Permissions.filterPermissions(argpre.get(i), argannos.get(i)));
			}
			
			// put borrowed permissions for receiver back in, if any
			put(instr.getReceiverOperand(), value, Permissions.filterPermissions(thispre, thisanno));
			
			// add permissions for result
			SimplePermissionAnnotation resultanno = resultPermission(instr.resolveBinding());
			if(resultanno != null) {
				if(resultanno.isReturned())
					put(instr.getTarget(), value, Permissions.createSingleton(resultanno.getEnsures()));
				else {
					problems.put(instr, "result annotation sets return flag to false");
					put(instr.getTarget(), value, Permissions.BOTTOM);
				}
				
			}
			else
				put(instr.getTarget(), value, Permissions.BOTTOM);
			return value;
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
				problems.put(instr, "Need " + anno.getRequires() + " but have " + perms);
				//Crystal.getInstance().reportUserProblem("Need " + anno.getPermission() + " but have " + perms, instr.getNode(), PermissionAnalysis.this);
			return result;
		}

		/**
		 * Find "result" annotation.
		 * @param binding
		 * @return
		 */
		private SimplePermissionAnnotation resultPermission(IMethodBinding binding) {
			try {
				for(IAnnotationBinding a : binding.getAnnotations()) {
					SimplePermissionAnnotation anno = Permission.createPermissionIfPossible("this", a);
					// by default, annotations on method itself are for receiver; 
					// permission for result must explicitly set 'var' parameter to "result"
					if(anno != null && anno.getVariable().equals("result"))
						return anno;
					// else not a permission annotation or probably receiver permission...
				}
				// no annotation: return null value
			}
			catch(NullPointerException npe) {
				// no binding: return null value
			}
			return null;
		}

		/**
		 * For constructors, this is the "result"
		 * @param binding
		 * @return
		 */
		private SimplePermissionAnnotation receiverPermission(IMethodBinding binding) {
			try {
				for(IAnnotationBinding a : binding.getAnnotations()) {
					SimplePermissionAnnotation anno = Permission.createPermissionIfPossible("this", a);
					if(anno != null && anno.getVariable().equals("this"))
						return anno;
					// else not a permission annotation or permission for result...
				}
				// no annotation: return null value
			}
			catch(NullPointerException npe) {
				// no binding: return null value
			}
			return null;
		}

		/**
		 * @param binding
		 * @param argumentCount 
		 * @return
		 */
		private List<SimplePermissionAnnotation> argumentPermissions(IMethodBinding binding, int argumentCount) {
			ArrayList<SimplePermissionAnnotation> result = new ArrayList<SimplePermissionAnnotation>(argumentCount);
			for(int i = 0; i < argumentCount; i++) {
				// TODO do we need the real formal parameter name?
				String paramName = "arg" + i;
				SimplePermissionAnnotation anno = null;
				for(IAnnotationBinding a : binding.getParameterAnnotations(i)) {
					anno = Permission.createPermissionIfPossible(paramName, a);
					if(anno != null) 
						break;
				}
				result.add(anno);
			}
			return result;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.NewArrayInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(NewArrayInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			put(instr.getTarget(), value, Permission.createUnique("alive"));
			return value;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.NewObjectInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(NewObjectInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			List<SimplePermissionAnnotation> argannos = argumentPermissions(instr.resolveBinding(), instr.getArgOperands().size());
			List<Variable> args = instr.getArgOperands();
			for(int i = 0; i < argannos.size(); i++) {
				Permissions p = get(args.get(i), value);
				SimplePermissionAnnotation a = argannos.get(i);
				checkPermissions(instr, args.get(i), p, a);
				p = Permissions.filterPermissions(p, a);
				put(args.get(i), value, p);
			}
			SimplePermissionAnnotation resultanno = receiverPermission(instr.resolveBinding());
			if(resultanno != null) {
				if(resultanno.isReturned())
					put(instr.getTarget(), value, Permissions.createSingleton(resultanno.getEnsures()));
				else {
					problems.put(instr, "constructor receiver annotation sets return flag to false");
					put(instr.getTarget(), value, Permissions.BOTTOM);
				}
			}
			else
				put(instr.getTarget(), value, Permissions.BOTTOM);
			return value;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.StoreArrayInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(StoreArrayInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.StoreFieldInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(StoreFieldInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			return super.transfer(instr, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.crystal.tac.AbstractingTransferFunction#transfer(edu.cmu.cs.crystal.tac.StoreInstruction, edu.cmu.cs.crystal.flow.LatticeElement)
		 */
		@Override
		public TupleLatticeElement<Variable, Permissions> transfer(StoreInstruction instr, TupleLatticeElement<Variable, Permissions> value) {
			// TODO Auto-generated method stub
			return super.transfer(instr, value);
		}

	}
	
}
