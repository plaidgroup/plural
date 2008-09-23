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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;

import edu.cmu.cs.crystal.BooleanLabel;
import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.ILabel;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.flow.Lattice;
import edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.AssignmentInstruction;
import edu.cmu.cs.crystal.tac.BinaryOperation;
import edu.cmu.cs.crystal.tac.BinaryOperator;
import edu.cmu.cs.crystal.tac.CastInstruction;
import edu.cmu.cs.crystal.tac.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.CopyInstruction;
import edu.cmu.cs.crystal.tac.DotClassInstruction;
import edu.cmu.cs.crystal.tac.InstanceofInstruction;
import edu.cmu.cs.crystal.tac.LoadArrayInstruction;
import edu.cmu.cs.crystal.tac.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.LoadLiteralInstruction;
import edu.cmu.cs.crystal.tac.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.NewArrayInstruction;
import edu.cmu.cs.crystal.tac.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.SourceVariable;
import edu.cmu.cs.crystal.tac.SourceVariableDeclaration;
import edu.cmu.cs.crystal.tac.SourceVariableRead;
import edu.cmu.cs.crystal.tac.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.TempVariable;
import edu.cmu.cs.crystal.tac.UnaryOperation;
import edu.cmu.cs.crystal.tac.UnaryOperator;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.alias.LivenessProxy;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.linear.DisjunctiveLE;
import edu.cmu.cs.plural.linear.InitialLECreator;
import edu.cmu.cs.plural.linear.PluralDisjunctiveLE;
import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.states.IConstructorSignature;
import edu.cmu.cs.plural.states.IMethodSignature;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement.VariableLiveness;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * The transfer function for the PLURAL analysis.
 * 
 * @author Kevin Bierhoff
 * @author Nels Beckman
 */
public class FractionalTransfer extends
		AbstractTACBranchSensitiveTransferFunction<PluralDisjunctiveLE> {
	
	private static final Logger log = Logger.getLogger(FractionalTransfer.class.getName());
	
	private PermissionFactory pf = PermissionFactory.INSTANCE;
	
	private FractionAnalysisContext context;

	/** Liveness information (another flow analysis...) */
	private final LivenessProxy liveness;
	
	/*
	 * Post-condition stuff.
	 */
//	private Map<Aliasing, PermissionSetFromAnnotations> paramPost;
	private Map<Boolean, String> dynamicStateTest;

	private SimpleMap<String, Aliasing> initialLocations;
	
//	private PermissionSetFromAnnotations resultPost;
	
//	private ThisVariable thisVar;
	
	public FractionalTransfer(IAnalysisInput input, FractionAnalysisContext context) {
		this.context = context;
		this.liveness = LivenessProxy.create(input);
	}

//	public Map<Aliasing, PermissionSetFromAnnotations> getParameterPostConditions() {
//		if(paramPost == null) throw new IllegalStateException("Must call getLattice() first--query any analysis results before calling this method");
//		return paramPost;
//	}
//	
//	/**
//	 * Returns the expected permissions for the method result, if any.
//	 * @return the expected permissions for the method result or <code>null</code> if there
//	 * is none (for constructors).
//	 */
//	public PermissionSetFromAnnotations getResultPostCondition() {
//		if(paramPost == null) throw new IllegalStateException("Must call getLattice() first--query any analysis results before calling this method");
//		return resultPost; 
//	}
	
	Map<Boolean, String> getDynamicStateTest() {
		if(dynamicStateTest == null) throw new IllegalStateException("Must call getLattice() first--query any analysis results before calling this method");	
		return dynamicStateTest;
	}
	
	public SimpleMap<String, Aliasing> getInitialLocations() {
		if(initialLocations == null) throw new IllegalStateException("Must call getLattice() first--query any analysis results before calling this method");	
		return initialLocations;
	}
	
	/* 
	 * As far as I can tell, this method does things that need to be set up at the beginning
	 * of a method analysis. 
	 * 
	 * For state invariants, I will insert information into the lattice about this object's
	 * fields based on what the precondition state implies.
	 */
	public Lattice<PluralDisjunctiveLE> getLattice(MethodDeclaration d) {
		if(initialLocations != null || dynamicStateTest != null)
			throw new IllegalStateException("getLattice() called twice--must create a new instance of this class for every method being analyzed");
		
//		final IMethodBinding methodBinding = d.resolveBinding();
		
//		if(paramPost != null || dynamicStateTest != null)
//			throw new IllegalStateException("getLattice() called twice--must create a new instance of this class for every method being analyzed");
//		paramPost = new HashMap<Aliasing, PermissionSetFromAnnotations>();

		liveness.switchToMethod(d);
		
		Pair<DisjunctiveLE, SimpleMap<String, Aliasing>> li = createLatticeInfo(d);
		DisjunctiveLE start = li.fst();
		// put location map in a field so it can be used for post-condition checking
		this.initialLocations = li.snd(); 
		
		dynamicStateTest = new HashMap<Boolean, String>(2);
		populateDynamicStateTest();
		
		//		TensorPluralTupleLE start;
//		boolean isConstructor = d.isConstructor();
//
//		// receiver permission--skip for static methods
//		if((d.getModifiers() & Modifier.STATIC) == 0) {
//			// instance method or constructor
//			thisVar = getAnalysisContext().getThisVariable();
//			// Get initial lattice, either constructor or regular method.
//			start = 
//				isConstructor ? 
//				TensorPluralTupleLE.createUnpackedLattice(
//					FractionalPermissions.createEmpty(), // use top (no permissions) as default
//					getAnnoDB(),
//					context.getRepository(),
//					thisVar)
//					:	
//				new TensorPluralTupleLE(
//					FractionalPermissions.createEmpty(), // use top (no permissions) as default
//					getAnnoDB(),
//					context.getRepository());
//			
//			start.storeInitialAliasingInfo(d);
//			Aliasing thisLocation =
//				start.getStartLocations(thisVar, d);
//			
//			// get the declared pre-condition
//			PermissionSetFromAnnotations this_pre = context.getAnalyzedCase().getRequiredReceiverPermissions();
//			// pre-condition
//			if(isConstructor) { // constructor
//				// add an unpacked unique frame permission for alive
//				StateSpace thisSpace = getStateSpace(methodBinding.getDeclaringClass());
//				PermissionFromAnnotation thisFrame = 
//					pf.createUniqueOrphan(thisSpace, thisSpace.getRootState(), 
//							true /* frame permission */, thisSpace.getRootState());
//				// there shouldn't already be frame permissions
//				assert this_pre.getFramePermissions().isEmpty() :
//					"Specification error: a constructor cannot require frame permissions; instead, it starts out with an unpacked unique frame permission"; 
//				this_pre = this_pre.combine(thisFrame); 
//
//				FractionalPermissions this_initial = this_pre.toLatticeElement();
//				this_initial = this_initial.unpack(thisSpace.getRootState());
//				start.put(thisLocation, this_initial);
//			}
//			else { 
//				// regular instance method: just use declared pre-condition
//				start.put(thisLocation, this_pre.toLatticeElement());
//			}
//			
//			// post-condition
//			PermissionSetFromAnnotations thisPost = context.getAnalyzedCase().getEnsuredReceiverPermissions();
//			paramPost.put(thisLocation, thisPost);
//		}
//		else {
//			// static method
//			thisVar = null;
//			start =	new TensorPluralTupleLE(FractionalPermissions.createEmpty(), // use top (no permissions) as default
//					getAnnoDB(), context.getRepository());
//			start.storeInitialAliasingInfo(d);
//		}
//		
//		/*
//		 * Fill this.dynamicStateTest
//		 */
//		populateDynamicStateTest();
//		
//		// parameter permissions
//		int paramCount = d.parameters().size();
//		for(int param = 0; param < paramCount; param++) {
//			SingleVariableDeclaration paramDecl = (SingleVariableDeclaration) d.parameters().get(param);
//			SourceVariable paramVar = getAnalysisContext().getSourceVariable(paramDecl.resolveBinding());
//			Aliasing paramLocation = start.getLocationsAfter(paramDecl, paramVar);
//			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> prePost = 
//				context.getAnalyzedCase().getParameterPermissions()[param];
////				parameterPermissions(methodBinding, param, true);
//			start.put(paramLocation, prePost.fst().toLatticeElement());
//			paramPost.put(paramLocation, prePost.snd());
//		}
//		
//		// populate expected result
//		if(isConstructor == false)
//			resultPost = context.getAnalyzedCase().getMethodCaseInstance().getEnsuredResultPermissions();
//		resultPost = resultPermissions(methodBinding, false);
		
		PluralDisjunctiveLE startLE = PluralDisjunctiveLE.createLE(start,
				getAnalysisContext(), context);
		
		if(d.isConstructor() && !hasConstructorInvocation(d)) {
			// TODO simulate default super constructor call
			if(log.isLoggable(Level.FINE))
				log.fine("Ignoring implicit super-constructor call");
		}
		
		return new Lattice<PluralDisjunctiveLE>(
				startLE, PluralDisjunctiveLE.bottom());
	}

	private Pair<DisjunctiveLE, SimpleMap<String, Aliasing>> createLatticeInfo(MethodDeclaration decl) {
		PredicateMerger pre = context.getAnalyzedCase().getPreconditionMerger();
		if(context.getAnalyzedCase().isConstructorCaseInstance())
			return InitialLECreator.createInitialConstructorLE(
					pre, getAnalysisContext(), context, decl);
		else
			return InitialLECreator.createInitialMethodLE(
					pre, getAnalysisContext(), context);
	}

	private void populateDynamicStateTest() {
		/*
		 * Set up the dynamic state test tracker, so we know what to test against
		 * at the end of the method.
		 */
		IndicatesAnnotation true_annot = 
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(getSpecificationBinding()), true);
		IndicatesAnnotation false_annot =
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(getSpecificationBinding()), false);
		
		if( true_annot != null ) {
			this.dynamicStateTest.put(Boolean.TRUE, true_annot.getIndicatedState());
		}
		if( false_annot != null ) {
			this.dynamicStateTest.put(Boolean.FALSE, false_annot.getIndicatedState());
		}
	}
	
	private IMethodBinding getSpecificationBinding() {
		return context.getAnalyzedCase().getInvocationCase().getSpecifiedMethodBinding();
	}

	private boolean hasConstructorInvocation(MethodDeclaration constructor) {
		// the call to statements() will trigger a NPE if constructor has no body
		// (which shouldn't ever happen if it's actually a constructor
		List<Statement> body = constructor.getBody().statements();
		if(body.isEmpty())
			return false;
		Statement firstStatement = body.get(0);
		return firstStatement != null && (firstStatement instanceof ConstructorInvocation ||
				firstStatement instanceof SuperConstructorInvocation);
	}

	/**
	 * @param value 
	 * @param instr TODO
	 * @param initOperands
	 * @return the changed <code>value</code>.
	 */
	private PluralDisjunctiveLE killAllDead(PluralDisjunctiveLE value, TACInstruction instr, Collection<Variable> variables) {
		if(variables.isEmpty())
			return value;
		for(Variable x : variables) {
			if(isDead(instr, x))
				/*value.kill(x)*/;
		}
		return value;
	}

	/**
	 * @param value 
	 * @param instr TODO
	 * @param initOperands
	 * @return the changed <code>value</code>.
	 */
	private PluralDisjunctiveLE killAllDead(PluralDisjunctiveLE value, TACInstruction instr, Variable... variables) {
		if(variables.length == 0)
			return value;
		for(Variable x : variables) {
			if(isDead(instr, x))
				/*value.kill(x)*/;
		}
		return value;
	}

	/**
	 * @param instr TODO
	 * @param x
	 * @return
	 */
	private boolean isDead(TACInstruction instr, Variable x) {
		if(x instanceof SourceVariable && ((SourceVariable) x).getBinding().isParameter())
			// keep parameters live for post-condition checks
			return false;
		if(x instanceof SourceVariable || x instanceof TempVariable) {
			return liveness.isDeadBefore(instr, x);
		}
		else
			// keyword, type variables can't die
			// TODO field variables?
			return false;
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			ArrayInitInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		return super.transfer(instr, labels, value);
	}

	/**
	 * @param instr
	 * @return
	 */
	private VariableLiveness createVariableLivenessBefore(
			final TACInstruction instr) {
		return new VariableLiveness() {

			@Override
			public boolean isLive(Variable x) {
				return liveness.isLiveBefore(instr, x);
			}
			
		};
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			BinaryOperation binop, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
		value.killDeadVariables(binop, createVariableLivenessBefore(binop));
		
		LabeledResult<PluralDisjunctiveLE> result = 
			LabeledResult.createResult(labels, value);
		
//		Aliasing op1_loc = value.getLocationsAfter(binop.getNode(), binop.getOperand1());
//		Aliasing op2_loc = value.getLocationsAfter(binop.getNode(), binop.getOperand2());
//		Aliasing target_loc = value.getLocationsAfter(binop.getNode(), binop.getTarget());
		Variable op1_loc = binop.getOperand1();
		Variable op2_loc = binop.getOperand2();
		Variable target_loc = binop.getTarget();
		
		if( labels.contains(BooleanLabel.getBooleanLabel(true)) ) {
			/*
			 * TRUE BRANCH
			 */
			PluralDisjunctiveLE branch_value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
			if( binop.getOperator().equals(BinaryOperator.BITWISE_AND) ) {
				/*
				 * When bitwise and is true, all of the operands involved are true.
				 */
				branch_value.addTrueVarPredicate(op1_loc);
				branch_value.addEquality(op1_loc, target_loc);
				branch_value.addEquality(op2_loc, target_loc);
				branch_value.addEquality(op1_loc, op2_loc);
			}
			else {
				// target is definitely true
				branch_value.addTrueVarPredicate(target_loc);
				if( binop.getOperator().equals(BinaryOperator.REL_EQ) ) {
					branch_value.addEquality(op1_loc, op2_loc);
				}
				else if( binop.getOperator().equals(BinaryOperator.REL_NEQ) ) {
					branch_value.addInequality(op1_loc, op2_loc);
				}
			}
			
			branch_value = addNewlyDeducedFacts(binop, branch_value,
					op1_loc, op2_loc, target_loc);
			result.put(BooleanLabel.getBooleanLabel(true), branch_value);
		}
		
		if( labels.contains(BooleanLabel.getBooleanLabel(false)) ) {
			/*
			 * FALSE BRANCH
			 */
			PluralDisjunctiveLE branch_value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
			if( binop.getOperator().equals(BinaryOperator.BITWISE_OR) ) {
				/*
				 * When bitwise or is false, all the operands involved are false.
				 */
				branch_value.addFalseVarPredicate(op1_loc);
				branch_value.addEquality(op1_loc, target_loc);
				branch_value.addEquality(op2_loc, target_loc);
				branch_value.addEquality(op1_loc, target_loc);		
			}
			else {
				// target is definitely false
				branch_value.addFalseVarPredicate(target_loc);
				if( binop.getOperator().equals(BinaryOperator.REL_EQ) ) {
					branch_value.addInequality(op1_loc, op2_loc);
				}
				else if( binop.getOperator().equals(BinaryOperator.REL_NEQ) ) {
					branch_value.addEquality(op1_loc, op2_loc);
				}
			}
			
			branch_value = addNewlyDeducedFacts(binop, branch_value,
					op1_loc, op2_loc, target_loc);
			result.put(BooleanLabel.getBooleanLabel(false), branch_value);
		}
		
		return result;
	}

	/**
	 * This convenience method calls solve, learnTempStateInfo, and then returns
	 * the resulting lattice.
	 * 
	 * @param value
	 * @param vs
	 * @return
	 */
	private static PluralDisjunctiveLE addNewlyDeducedFacts(
			// TODO not needed parameter
			TACInstruction instr, 
			PluralDisjunctiveLE value,
			Variable... vs) {
		value.addNewlyDeducedFacts(vs);
		
		// moved into lattice
//		List<ImplicationResult> new_facts = value.solveWithHints(vs);
//		for( ImplicationResult f : new_facts ) {
//			value = f.putResultIntoLattice(value); 					
//		}
				
		return value;
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			CastInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		// could be a boolean test if this instruction is y = (Boolean) x
		return handleBooleanTest(instr, labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			ConstructorCallInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		IMethodBinding binding = instr.resolveBinding();

		value.handleConstructorCall(instr, getConstructorSignature(binding));

		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			CopyInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		// equality between source and target?
		return handleBooleanTest(instr, labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			DotClassInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			InstanceofInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		return handleBooleanTest(instr, labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			LoadArrayInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		return handleBooleanTest(instr, labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			LoadFieldInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		if(instr.isStaticFieldAccess()) {
			PermissionSetFromAnnotations perms = getFieldPermissionsFromAnnotations(instr.resolveFieldBinding());
			if(perms != null) {
				value.put(instr, instr.getTarget(), perms.toLatticeElement());
			}
		}
		else if(inStaticMethod() == false) {
			value.prepareForFieldRead(instr);
		}
		else {
			if(log.isLoggable(Level.WARNING))
				log.warning("Unsupported field assignment to foreign object: " + instr.getNode());
		}
		return handleBooleanTest(instr, labels, value);
	}

	/**
	 * @param fieldBinding
	 * @return
	 */
	private PermissionSetFromAnnotations getFieldPermissionsFromAnnotations(
			IVariableBinding fieldBinding) {
		List<ParameterPermissionAnnotation> annos = CrystalPermissionAnnotation.fieldAnnotations(getAnnoDB(), fieldBinding);
		if(annos.isEmpty()) 
			return null;
		
		StateSpace space = getStateSpace(fieldBinding.getType());
		PermissionSetFromAnnotations perms = PermissionSetFromAnnotations.createEmpty(space);
		for(ParameterPermissionAnnotation a : annos) {
			perms = perms.combine(pf.createOrphan(space, a.getRootNode(), a.getKind(), new String[] { a.getRootNode() }, true));
		}
		return perms;
	}

	private boolean inStaticMethod() {
		return getAnalysisContext().getThisVariable() == null;
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			LoadLiteralInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		PluralDisjunctiveLE new_value = 
			value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		new_value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
			
		Variable target = instr.getTarget();
		if( instr.isNull() ) {
			new_value.addNullVariable(target);
		}
		else {
			if( instr.getLiteral() instanceof Boolean ) {

				boolean bool_value = ((Boolean)instr.getLiteral()).booleanValue();
				
				if( bool_value ) {
					new_value.addTrueVarPredicate(target);
				}
				else {
					new_value.addFalseVarPredicate(target);
				}
			}

			// immutable permission for primitive literals
//			PermissionSetFromAnnotations new_perms = PermissionSetFromAnnotations.createEmpty(StateSpace.SPACE_TOP);
//			new_perms = new_perms.combine(pf.createImmutableOrphan(
//					StateSpace.SPACE_TOP, 
//					StateSpace.STATE_ALIVE, 
//					false, 
//					new String[] { StateSpace.STATE_ALIVE }, 
//					true));
//			new_value.put(instr, instr.getTarget(), new_perms.toLatticeElement());
			new_value.put(instr, instr.getTarget(), FractionalPermissions.createEmpty());
		}
		return handleBooleanTest(instr, labels, new_value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			final MethodCallInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		IMethodBinding binding = instr.resolveBinding();

		value.handleMethodCall(instr, getMethodSignature(binding));
		
		// 4.0 Begin Nels' First Shot at State Tests
		return dynamicStateTestHelper(instr, labels, value);
	}

	private IMethodSignature getMethodSignature(
			IMethodBinding binding) {
		return context.getRepository().getMethodSignature(binding);
	}

	private IResult<PluralDisjunctiveLE> dynamicStateTestHelper(
			MethodCallInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		// Well we know afterwards that the receiver is not null!
		Variable rcvr_loc = instr.getReceiverOperand();
		value.addNonNullVariable(rcvr_loc);

		// find the method binding that actually carries the spec for the invoked method
		final IMethodBinding invokedBinding = context.getRepository().getSignature(instr.resolveBinding()).getSpecifiedMethodBinding();
	
		// find @TrueIndicates / @FalseIndicates annos
		IndicatesAnnotation true_result = 
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(invokedBinding),
					true);
		IndicatesAnnotation false_result = 
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(invokedBinding),
					false);
		addIndicatedImplications(instr, value, rcvr_loc, true_result,
				false_result);
		
		// state tests for arguments
		// TODO make indicator annotations available for method parameters; check if code below needs extension
		for(int arg = 0; arg < instr.getArgOperands().size(); arg++) {
			IndicatesAnnotation arg_true = 
				IndicatesAnnotation.getBooleanIndicatorIfAny(
						getAnnoDB().getSummaryForMethod(invokedBinding),
						arg, true);
			IndicatesAnnotation arg_false = 
				IndicatesAnnotation.getBooleanIndicatorIfAny(
						getAnnoDB().getSummaryForMethod(invokedBinding),
						arg, false);
//			Aliasing arg_loc = value.getLocationsAfter(instr.getNode(), (Variable) instr.getArgOperands().get(arg));
			Variable arg_loc = (Variable) instr.getArgOperands().get(arg);
			addIndicatedImplications(instr, value, arg_loc, arg_true,
					arg_false);
			
		}
		
		return handleBooleanTest(instr, labels, value);
	}

	/**
	 * Adds implications for any indicated states on the given location.
	 * @param instr
	 * @param value
	 * @param indicated_loc
	 * @param true_result
	 * @param false_result
	 * @return modified <code>value</code>.
	 */
	private PluralDisjunctiveLE addIndicatedImplications(MethodCallInstruction instr,
			PluralDisjunctiveLE value, Variable indicated_loc,
			IndicatesAnnotation true_result, IndicatesAnnotation false_result) {
		/*
		 * If either of the above results is non-null, we can add an implication to the
		 * normal path.
		 */
		if( true_result != null || false_result != null ) {
//			Aliasing target_loc = value.getLocationsAfter(instr.getNode(), instr.getTarget());
			Variable target_loc = instr.getTarget();
			if( true_result != null ) {
				value.addTrueImplication(target_loc, indicated_loc, true_result.getIndicatedState());
			}
			if( false_result != null ) {
				value.addFalseImplication(target_loc, indicated_loc, false_result.getIndicatedState());
			}
			//result.put(NormalLabel.getNormalLabel(), value, instr.getNode());
		}
		return value;
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			NewArrayInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		StateSpace arraySpace = getStateSpace(instr.getArrayType().resolveBinding());
		value.put(instr, instr.getTarget(), PermissionSetFromAnnotations.createSingleton(
				pf.createUniqueOrphan(arraySpace, arraySpace.getRootState(), 
						false /* not a frame perm */, arraySpace.getRootState())).toLatticeElement());
		// TODO consider arrays as frames?  may affect array assignment and checking
		
		// After instantiation, we know the target to not be null! 
		value.addNonNullVariable(instr.getTarget());
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			NewObjectInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		IMethodBinding binding = instr.resolveBinding();
		
		value.handleNewObject(instr, getConstructorSignature(binding));
		
		value.addNonNullVariable(instr.getTarget());
		
		// could be a boolean test if this instruction is new Boolean(...);
		return handleBooleanTest(instr, labels, value);
	}

	private IConstructorSignature getConstructorSignature(IMethodBinding binding) {
		return context.getRepository().getConstructorSignature(binding);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			SourceVariableDeclaration instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		// If this variable is the parameter to a catch block, we default
		// to the default permission. Catch blocks aren't really called...
		if( instr.isCaughtVariable() ) {
			FractionalPermissions perm = 
				getDefaultCatchParamPermission(instr.getDeclaredVariable());
			value.put(instr, instr.getDeclaredVariable(), perm);
			// TODO caught exceptions always non-null?
			value.addNonNullVariable(instr.getDeclaredVariable());
		}
		return super.transfer(instr, labels, value);
	}

	/**
	 * What is the default permission for parameters of catch blocks?
	 * e.g., In 
	 * <pre>
	 * catch(Exception v) { ... }
	 * </pre>
	 * There should be some permission associated with v.
	 * @return
	 */
	private FractionalPermissions getDefaultCatchParamPermission(Variable var) {
		StateSpace space =
			this.getStateSpace(var.resolveType());
		PermissionFromAnnotation p =
			PermissionFactory.INSTANCE.createImmutableOrphan(space, space.getRootState(), 
					false /* not a frame permission */, new String[] { space.getRootState() }, true);
		PermissionSetFromAnnotations result = 
			PermissionSetFromAnnotations.createSingleton(p);
		return result.toLatticeElement();
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			StoreArrayInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		if(FractionalAnalysis.checkArrays)
			value.prepareForArrayWrite(instr);

		// TODO check whether permission stored in array is compatible with array declaration
		return handleBooleanTest(instr, instr.getSourceOperand(), labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			StoreFieldInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		if(instr.isStaticFieldAccess()) {
			PermissionSetFromAnnotations perms = getFieldPermissionsFromAnnotations(instr.resolveFieldBinding());
			if(perms != null) {
				value.splitOff(instr.getSourceOperand(), perms);
			}
		}
		else if(inStaticMethod() == false) {
			value.prepareForFieldWrite(instr);
		}
		else {
			if(log.isLoggable(Level.WARNING))
				log.warning("Unsupported field assignment to foreign object: " + instr.getNode());
		}
		return handleBooleanTest(instr, instr.getSourceOperand(), labels, value);
	}

	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			UnaryOperation unop, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(unop.getNode());
		value.killDeadVariables(unop, createVariableLivenessBefore(unop));
		
		if( unop.getOperator().equals(UnaryOperator.BOOL_NOT) ) {
			/*
			 * Add an inequality.
			 */
			final Variable target = unop.getTarget();
			final Variable op = unop.getOperand();
			value.addInequality(target, op);
			value = addNewlyDeducedFacts(unop, value,
					op, target);
		}
		
		return handleBooleanTest(unop, labels, value);
	}
	
	@Override
	public IResult<PluralDisjunctiveLE> transfer(
			SourceVariableRead instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.killDeadVariables(instr, createVariableLivenessBefore(instr));
		
		return handleBooleanTest(instr, instr.getVariable(), labels, value);
	}

	private IResult<PluralDisjunctiveLE> handleBooleanTest(
			AssignmentInstruction instr, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		return handleBooleanTest(instr, instr.getTarget(), labels, value);
	}

	/**
	 * Helper method to handle boolean labels out of a given instruction.
	 * @param instr
	 * @param labels
	 * @param value
	 * @return
	 * @tag todo.general -id="6023976" : handle boolean test in all transfer cases
	 *
	 */
	private IResult<PluralDisjunctiveLE> handleBooleanTest(
			TACInstruction instr,
			final Variable testedVar, List<ILabel> labels,
			PluralDisjunctiveLE value) {
		/*
		 * If we read a variable, and we have boolean labels exiting that statement, it's
		 * because someone is testing the truth of that boolean. If we have state implications
		 * that depend on the truth or falsehood of that variable, we could learn new information.
		 */
		LabeledResult<PluralDisjunctiveLE> result = 
			LabeledResult.createResult(labels, value);

		if( labels.contains(BooleanLabel.getBooleanLabel(true)) ) {

			/*
			 * If the branch is true, does adding this predicate eliminate any
			 * implications?
			 */			
			PluralDisjunctiveLE br = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
			br.addTrueVarPredicate(testedVar);
			
			br = addNewlyDeducedFacts(instr, br, testedVar);
			result.put(BooleanLabel.getBooleanLabel(true), br);
		}

		if( labels.contains(BooleanLabel.getBooleanLabel(false)) ) {
			/*
			 * Same, but we know the variable is false.
			 */
			PluralDisjunctiveLE br = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
			br.addFalseVarPredicate(testedVar);
			
			br = addNewlyDeducedFacts(instr, br, testedVar);
			result.put(BooleanLabel.getBooleanLabel(false), br);
		}

		return result;
	}
	
	private AnnotationDatabase getAnnoDB() {
		return context.getAnnoDB();
	}

	private StateSpace getStateSpace(ITypeBinding type) {
		return context.getRepository().getStateSpace(type);
	}
}
