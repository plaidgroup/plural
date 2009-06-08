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

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.bridge.LatticeElementOps;
import edu.cmu.cs.crystal.flow.BooleanLabel;
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.flow.NormalLabel;
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
import edu.cmu.cs.crystal.tac.SourceVariableDeclaration;
import edu.cmu.cs.crystal.tac.SourceVariableRead;
import edu.cmu.cs.crystal.tac.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.UnaryOperation;
import edu.cmu.cs.crystal.tac.UnaryOperator;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.alias.LivenessProxy;
import edu.cmu.cs.plural.contexts.LinearContext;
import edu.cmu.cs.plural.contexts.InitialLECreator;
import edu.cmu.cs.plural.contexts.PluralContext;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.states.IConstructorSignature;
import edu.cmu.cs.plural.states.IMethodSignature;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement.VariableLiveness;

/**
 * The transfer function for the PLURAL analysis.
 * 
 * @author Kevin Bierhoff
 * @author Nels Beckman
 */
public class FractionalTransfer extends
		AbstractTACBranchSensitiveTransferFunction<PluralContext> {
	
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
	
	//
	//
	// LATTICE CREATION and information for following checker
	//
	//

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
	public ILatticeOperations<PluralContext> createLatticeOperations(MethodDeclaration d) {
		return LatticeElementOps.create(PluralContext.bottom());
	}
	
	public PluralContext createEntryValue(MethodDeclaration d) {
		if(initialLocations != null || dynamicStateTest != null)
			throw new IllegalStateException("getLattice() called twice--must create a new instance of this class for every method being analyzed");
		
		liveness.switchToMethod(d);
		
		Pair<LinearContext, SimpleMap<String, Aliasing>> li = createLatticeInfo(d);
		LinearContext start = li.fst();
		// put location map in a field so it can be used for post-condition checking
		this.initialLocations = li.snd(); 
		
		dynamicStateTest = new HashMap<Boolean, String>(2);
		populateDynamicStateTest();
		
		PluralContext startLE = PluralContext.createLE(start,
				getAnalysisContext(), context);
		
		return startLE;
	}

	private Pair<LinearContext, SimpleMap<String, Aliasing>> createLatticeInfo(MethodDeclaration decl) {
		PredicateMerger pre = context.getAnalyzedCase().getPreconditionMerger();
		if(decl.isConstructor()) {
			boolean callsThisConstructor = false;
			switch(findConstructorInvocation(decl)) {
			case NONE:
				// TODO simulate default super constructor call
				if(log.isLoggable(Level.FINE))
					log.fine("Ignoring implicit super-constructor call");
				break;
			case THIS:
				callsThisConstructor = true;
				break;
			case SUPER:
				break;
			}
			return InitialLECreator.createInitialConstructorLE(
					pre, getAnalysisContext(), context, decl, callsThisConstructor);
		}
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

	/**
	 * Return type for {@link FractionalTransfer#findConstructorInvocation(MethodDeclaration)}
	 * indicating what constructor a given constructor body invokes.
	 * @author Kevin Bierhoff
	 * @since Apr 2, 2009
	 */
	private enum InvokedConstructor {
		NONE, SUPER, THIS
	}
	
	private static InvokedConstructor findConstructorInvocation(MethodDeclaration constructor) {
		// the call to statements() will trigger a NPE if constructor has no body
		// (which shouldn't ever happen if it's actually a constructor
		List<Statement> body = constructor.getBody().statements();
		if(body.isEmpty())
			return InvokedConstructor.NONE;
		Statement firstStatement = body.get(0);
		if(firstStatement == null)
			return InvokedConstructor.NONE;
		else if(firstStatement instanceof ConstructorInvocation)
			return InvokedConstructor.THIS;
		else if(firstStatement instanceof SuperConstructorInvocation)
			return InvokedConstructor.SUPER;
		else
			return InvokedConstructor.NONE;
	}
	
	//
	//
	// TRANSFER METHODS
	//
	//

	@Override
	public IResult<PluralContext> transfer(
			ArrayInitInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		
		// killing dead variables is the last thing we do
		value.killDeadVariables(instr, createVariableLivenessAfter(instr, NormalLabel.getNormalLabel()));
		value.freeze(); // done with this lattice element--freeze it
		return super.transfer(instr, labels, value);
	}

	/**
	 * @param instr
	 * @return
	 */
	private VariableLiveness createVariableLivenessAfter(
			final TACInstruction instr, final ILabel label) {
		return new VariableLiveness() {
			@Override
			public boolean isLive(Variable x) {
				return liveness.isLiveAfter(instr, x, label);
			}
		};
	}

	@Override
	public IResult<PluralContext> transfer(
			BinaryOperation binop, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
		
		// this code is highly suspicious for going wrong if one of the operands is the target
		Variable op1_loc = binop.getOperand1();
		Variable op2_loc = binop.getOperand2();
		Variable target_loc = binop.getTarget();
		
		PluralContext true_value = null;
		if( labels.contains(BooleanLabel.getBooleanLabel(true)) ) {
			/*
			 * TRUE BRANCH
			 */
			true_value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
			if( binop.getOperator().equals(BinaryOperator.BITWISE_AND) ) {
				/*
				 * When bitwise and is true, all of the operands involved are true.
				 */
				true_value.addTrueVarPredicate(op1_loc);
				true_value.addEquality(op1_loc, target_loc);
				true_value.addEquality(op2_loc, target_loc);
				true_value.addEquality(op1_loc, op2_loc);
			}
			else {
				// target is definitely true
				true_value.addTrueVarPredicate(target_loc);
				if( binop.getOperator().equals(BinaryOperator.REL_EQ) ) {
					true_value.addEquality(op1_loc, op2_loc);
				}
				else if( binop.getOperator().equals(BinaryOperator.REL_NEQ) ) {
					true_value.addInequality(op1_loc, op2_loc);
				}
			}
			
			true_value = addNewlyDeducedFacts(binop, true_value,
					op1_loc, op2_loc, target_loc);

			// killing dead variables is the last thing we do
			true_value.killDeadVariables(binop, 
					createVariableLivenessAfter(binop, BooleanLabel.getBooleanLabel(true)));
			true_value.freeze(); // done with this lattice element--freeze it
		}
		
		PluralContext false_value = null;
		if( labels.contains(BooleanLabel.getBooleanLabel(false)) ) {
			/*
			 * FALSE BRANCH
			 */
			false_value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
			if( binop.getOperator().equals(BinaryOperator.BITWISE_OR) ) {
				/*
				 * When bitwise or is false, all the operands involved are false.
				 */
				false_value.addFalseVarPredicate(op1_loc);
				false_value.addEquality(op1_loc, target_loc);
				false_value.addEquality(op2_loc, target_loc);
				false_value.addEquality(op1_loc, target_loc);		
			}
			else {
				// target is definitely false
				false_value.addFalseVarPredicate(target_loc);
				if( binop.getOperator().equals(BinaryOperator.REL_EQ) ) {
					false_value.addInequality(op1_loc, op2_loc);
				}
				else if( binop.getOperator().equals(BinaryOperator.REL_NEQ) ) {
					false_value.addEquality(op1_loc, op2_loc);
				}
			}
			
			false_value = addNewlyDeducedFacts(binop, false_value,
					op1_loc, op2_loc, target_loc);

			// killing dead variables is the last thing we do
			false_value.killDeadVariables(binop, 
					createVariableLivenessAfter(binop, BooleanLabel.getBooleanLabel(false)));
			false_value.freeze(); // done with this lattice element--freeze it
		}
		
		// killing dead variables is the last thing we do
		value.killDeadVariables(binop, createVariableLivenessAfter(binop, 
				NormalLabel.getNormalLabel()));
		value.freeze(); // done with this lattice element--freeze it
		
		LabeledResult<PluralContext> result = 
			LabeledResult.createResult(labels, value);
		if(true_value != null)
			result.put(BooleanLabel.getBooleanLabel(true), true_value);
		if(false_value != null)
			result.put(BooleanLabel.getBooleanLabel(false), false_value);
		
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
	private static PluralContext addNewlyDeducedFacts(
			// TODO not needed parameter
			TACInstruction instr, 
			PluralContext value,
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
	public IResult<PluralContext> transfer(
			CastInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		// could be a boolean test if this instruction is y = (Boolean) x
		return finishTransfer(instr, labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			ConstructorCallInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		IMethodBinding binding = instr.resolveBinding();

		value.handleConstructorCall(instr, getConstructorSignature(binding));

		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			CopyInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		// equality between source and target?
		return finishTransfer(instr, labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			DotClassInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			InstanceofInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		return finishTransfer(instr, labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			LoadArrayInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		return finishTransfer(instr, labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			LoadFieldInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		if(instr.isStaticFieldAccess()) {
			PermissionSetFromAnnotations perms = getFieldPermissionsFromAnnotations(instr.resolveFieldBinding());
			if(perms != null) {
				value.put(instr, instr.getTarget(), perms.toLatticeElement());
			}
		}
		else /*if(inStaticMethod() == false)*/ {
			value.prepareForFieldRead(instr);
		}
//		else {
//			if(log.isLoggable(Level.WARNING))
//				log.warning("Unsupported field load from foreign object: " + instr.getNode());
//		}
		return finishTransfer(instr, labels, value);
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
			perms = perms.combine(
					pf.createOrphan(space, a.getRootNode(), a.getKind(), new String[] { a.getRootNode() }, true /* named fractions */),
					false /* named fractions are existentials */);
		}
		return perms;
	}

	private boolean inStaticMethod() {
		return getAnalysisContext().getThisVariable() == null;
	}

	@Override
	public IResult<PluralContext> transfer(
			LoadLiteralInstruction instr, List<ILabel> labels,
			PluralContext initial_value) {
		PluralContext new_value = 
			initial_value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		new_value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
			
		Variable target = instr.getTarget();
		if( instr.isNull() ) {
			new_value.addNullVariable(target);
		}
		else {
			// Ugh! I'm just doing this for one example... NEB
			// TODO: We need the ability to arbitrarily map a field to a
			// dimension if it's not mentioned in any invariant.
			new_value.addNonNullVariable(target); 
			
			if( instr.getLiteral() instanceof Boolean ) {
				boolean bool_value = ((Boolean)instr.getLiteral()).booleanValue();
				
				if( bool_value ) {
					new_value.addTrueVarPredicate(target);
				}
				else {
					new_value.addFalseVarPredicate(target);
				}
			}
			// override any previous permission info
			new_value.put(instr, instr.getTarget(), FractionalPermissions.createEmpty());
		}
		return finishTransfer(instr, labels, new_value);
	}

	@Override
	public IResult<PluralContext> transfer(
			final MethodCallInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));

		// find the signature and the method binding that carries the spec for the invoked method
		// binding can be different because of spec inheritance
		IMethodSignature sig = getMethodSignature(instr.resolveBinding());
		final IMethodBinding specBinding = sig.getSpecifiedMethodBinding();
	
		value.handleMethodCall(instr, sig);
		
		/*
		 * "Nels' first shot at dynamic state tests" [ highly reworked :) --KB ]
		 */
		
		// Well we know afterwards that the receiver is not null!
		Variable rcvr_loc = instr.getReceiverOperand();
		value.addNonNullVariable(rcvr_loc);

		// find @TrueIndicates / @FalseIndicates annos
		IndicatesAnnotation true_result = 
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(specBinding),
					true);
		IndicatesAnnotation false_result = 
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(specBinding),
					false);
		addIndicatedImplications(instr, value, rcvr_loc, true_result,
				false_result);
		
		// state tests for arguments
		// TODO make indicator annotations available for method parameters; check if code below needs extension
		for(int arg = 0; arg < instr.getArgOperands().size(); arg++) {
			IndicatesAnnotation arg_true = 
				IndicatesAnnotation.getBooleanIndicatorIfAny(
						getAnnoDB().getSummaryForMethod(specBinding),
						arg, true);
			IndicatesAnnotation arg_false = 
				IndicatesAnnotation.getBooleanIndicatorIfAny(
						getAnnoDB().getSummaryForMethod(specBinding),
						arg, false);
			Variable arg_loc = (Variable) instr.getArgOperands().get(arg);
			addIndicatedImplications(instr, value, arg_loc, arg_true,
					arg_false);
			
		}
		
		return finishTransfer(instr, labels, value);
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
	private PluralContext addIndicatedImplications(MethodCallInstruction instr,
			PluralContext value, Variable indicated_loc,
			IndicatesAnnotation true_result, IndicatesAnnotation false_result) {
		/*
		 * Add implications for any state indicator annotations
		 */
		if( true_result != null || false_result != null ) {
			Variable target_loc = instr.getTarget();
			if( true_result != null ) {
				value.addTrueImplication(target_loc, indicated_loc, true_result.getIndicatedState());
			}
			if( false_result != null ) {
				value.addFalseImplication(target_loc, indicated_loc, false_result.getIndicatedState());
			}
		}
		return value;
	}

	@Override
	public IResult<PluralContext> transfer(
			NewArrayInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		StateSpace arraySpace = getStateSpace(instr.getArrayType().resolveBinding());
		value.put(instr, instr.getTarget(), PermissionSetFromAnnotations.createSingleton(
				pf.createUniqueOrphan(arraySpace, arraySpace.getRootState(), 
						false /* not a frame perm */, arraySpace.getRootState()), 
				false /* named is existential */).toLatticeElement());
		// TODO consider arrays as frames?  may affect array assignment and checking
		
		// After instantiation, we know the target to not be null! 
		value.addNonNullVariable(instr.getTarget());
		return finishTransfer(instr, labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			NewObjectInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		IMethodBinding binding = instr.resolveBinding();
		
		value.handleNewObject(instr, getConstructorSignature(binding));
		
		value.addNonNullVariable(instr.getTarget());
		
		// could be a boolean test if this instruction is new Boolean(...);
		return finishTransfer(instr, labels, value);
	}

	private IConstructorSignature getConstructorSignature(IMethodBinding binding) {
		return context.getRepository().getConstructorSignature(binding);
	}

	@Override
	public IResult<PluralContext> transfer(
			SourceVariableDeclaration instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		
		// If this variable is the parameter to a catch block, we default
		// to the default permission. Catch blocks aren't really called...
		if( instr.isCaughtVariable() ) {
			FractionalPermissions perm = 
				getDefaultCatchParamPermission(instr.getDeclaredVariable());
			value.put(instr, instr.getDeclaredVariable(), perm);
			// TODO caught exceptions always non-null?
			value.addNonNullVariable(instr.getDeclaredVariable());
		}
		
		// killing dead variables is the last thing we do
		value.killDeadVariables(instr, createVariableLivenessAfter(instr, NormalLabel.getNormalLabel()));
		value.freeze(); // done with this lattice element--freeze it
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
			PermissionSetFromAnnotations.createSingleton(p, false /* named is existential */);
		return result.toLatticeElement();
	}

	@Override
	public IResult<PluralContext> transfer(
			StoreArrayInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		if(FractionalAnalysis.checkArrays)
			value.prepareForArrayWrite(instr);

		// TODO check whether permission stored in array is compatible with array declaration
		return finishTransfer(instr, instr.getSourceOperand(), labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			StoreFieldInstruction instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
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
		return finishTransfer(instr, instr.getSourceOperand(), labels, value);
	}

	@Override
	public IResult<PluralContext> transfer(
			UnaryOperation unop, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(unop.getNode());
//		value.killDeadVariables(unop, createVariableLivenessAfter(unop));
		
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
		
		return finishTransfer(unop, labels, value);
	}
	
	@Override
	public IResult<PluralContext> transfer(
			SourceVariableRead instr, List<ILabel> labels,
			PluralContext value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
//		value.killDeadVariables(instr, createVariableLivenessAfter(instr));
		
		return finishTransfer(instr, instr.getVariable(), labels, value);
	}

	/**
	 * Helper method to handle boolean labels out of a given assignment, and to kill
	 * variables that are dead after the instruction.
	 * If we read a variable, and we have boolean labels exiting that statement, it's
	 * because someone is testing the truth of that boolean. If we have state implications
	 * that depend on the truth or falsehood of that variable, we could learn new information.
	 * @param instr Current assignment instruction.
	 * @param labels Labels out of the instruction <b>as given to the transfer function</b>. 
	 * @param value Analysis information.
	 * @return Transfer result with frozen analysis information for return to framework.
	 * @see #finishTransfer(TACInstruction, Variable, List, PluralContext)
	 */
	private IResult<PluralContext> finishTransfer(
			AssignmentInstruction instr, List<ILabel> labels,
			PluralContext value) {
		return finishTransfer(instr, instr.getTarget(), labels, value);
	}

	/**
	 * Helper method to handle boolean labels out of a given instruction, and to kill
	 * variables that are dead after the instruction.
	 * If we read a variable, and we have boolean labels exiting that statement, it's
	 * because someone is testing the truth of that boolean. If we have state implications
	 * that depend on the truth or falsehood of that variable, we could learn new information.
	 * @param instr Current instruction.
	 * @param testedVar Variable being tested in the instruction.
	 * @param labels Labels out of the instruction <b>as given to the transfer function</b>. 
	 * @param value Analysis information.
	 * @return Transfer result with frozen analysis information for return to framework.
	 */
	private IResult<PluralContext> finishTransfer(
			TACInstruction instr,
			final Variable testedVar, List<ILabel> labels,
			PluralContext value) {
		PluralContext true_value = null;
		if( labels.contains(BooleanLabel.getBooleanLabel(true)) ) {

			/*
			 * If the branch is true, does adding this predicate eliminate any
			 * implications?
			 */			
			true_value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
			true_value.addTrueVarPredicate(testedVar);

			true_value = addNewlyDeducedFacts(instr, true_value, testedVar);

			// killing dead variables is the last thing we do
			true_value.killDeadVariables(instr, 
					createVariableLivenessAfter(instr, BooleanLabel.getBooleanLabel(true)));
			true_value.freeze(); // done with this lattice element--freeze it
		}

		PluralContext false_value = null;
		if( labels.contains(BooleanLabel.getBooleanLabel(false)) ) {
			/*
			 * Same, but we know the variable is false.
			 */
			false_value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
			false_value.addFalseVarPredicate(testedVar);
			
			false_value = addNewlyDeducedFacts(instr, false_value, testedVar);
			
			// killing dead variables is the last thing we do
			false_value.killDeadVariables(instr, 
					createVariableLivenessAfter(instr, BooleanLabel.getBooleanLabel(false)));
			false_value.freeze(); // done with this lattice element--freeze it
		}

		// killing dead variables is the last thing we do
		value.killDeadVariables(instr, 
				createVariableLivenessAfter(instr, NormalLabel.getNormalLabel()));
		value.freeze(); // done with this lattice element--freeze it

		LabeledResult<PluralContext> result = 
			LabeledResult.createResult(labels, value);
		if(true_value != null)
			result.put(BooleanLabel.getBooleanLabel(true), true_value);
		if(false_value != null)
			result.put(BooleanLabel.getBooleanLabel(false), false_value);

		return result;
	}
	
	private IMethodSignature getMethodSignature(
			IMethodBinding binding) {
		return context.getRepository().getMethodSignature(binding);
	}

	private AnnotationDatabase getAnnoDB() {
		return context.getAnnoDB();
	}

	private StateSpace getStateSpace(ITypeBinding type) {
		return context.getRepository().getStateSpace(type);
	}
}
