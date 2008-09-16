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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import edu.cmu.cs.crystal.BooleanLabel;
import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.ILabel;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.flow.LabeledSingleResult;
import edu.cmu.cs.crystal.flow.Lattice;
import edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.BinaryOperation;
import edu.cmu.cs.crystal.tac.BinaryOperator;
import edu.cmu.cs.crystal.tac.CastInstruction;
import edu.cmu.cs.crystal.tac.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.CopyInstruction;
import edu.cmu.cs.crystal.tac.DotClassInstruction;
import edu.cmu.cs.crystal.tac.InstanceofInstruction;
import edu.cmu.cs.crystal.tac.InvocationInstruction;
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
import edu.cmu.cs.crystal.tac.TACFieldAccess;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.ThisVariable;
import edu.cmu.cs.crystal.tac.UnaryOperation;
import edu.cmu.cs.crystal.tac.UnaryOperator;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.concrete.ImplicationResult;
import edu.cmu.cs.plural.fractions.AbstractFractionalPermission;
import edu.cmu.cs.plural.fractions.Fraction;
import edu.cmu.cs.plural.fractions.FractionAssignment;
import edu.cmu.cs.plural.fractions.FractionConstraints;
import edu.cmu.cs.plural.fractions.FractionalPermission;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.perm.ResultPermissionAnnotation;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.states.IInvocationSignature;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.util.ExtendedIterator;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * @author Kevin Bierhoff
 * @author Nels Beckman
 */
public class SingleTruthFractionalTransfer extends
		AbstractTACBranchSensitiveTransferFunction<PluralTupleLatticeElement> {
	
	private static final Logger log = Logger.getLogger(SingleTruthFractionalTransfer.class.getName());
	
	private final IAnalysisInput input;
	private PermissionFactory pf = PermissionFactory.INSTANCE;
	
	/*
	 * Post-condition stuff.
	 */
	private Map<Aliasing, PermissionSetFromAnnotations> paramPost;
	private Map<Boolean, String> dynamicStateTest;
	
	private PermissionSetFromAnnotations resultPost;
	private FractionAnalysisContext context;
	
	private ThisVariable thisVar; 
	
	public SingleTruthFractionalTransfer(IAnalysisInput input, FractionAnalysisContext context) {
		this.input = input;
		this.context = context;
	}

	public Map<Aliasing, PermissionSetFromAnnotations> getParameterPostConditions() {
		if(paramPost == null) throw new IllegalStateException("Must call getLattice() first--query any analysis results before calling this method");
		return paramPost;
	}
	
	public PermissionSetFromAnnotations getResultPostCondition() {
		if(paramPost == null) throw new IllegalStateException("Must call getLattice() first--query any analysis results before calling this method");
		return resultPost; 
	}
	
	Map<Boolean, String> getDynamicStateTest() {
		if(dynamicStateTest == null) throw new IllegalStateException("Must call getLattice() first--query any analysis results before calling this method");	
		return dynamicStateTest;
	}
	
	/* 
	 * As far as I can tell, this method does things that need to be set up at the beginning
	 * of a method analysis. 
	 * 
	 * For state invariants, I will insert information into the lattice about this object's
	 * fields based on what the precondition state implies.
	 */
	public Lattice<PluralTupleLatticeElement> getLattice(MethodDeclaration d) {
		final IMethodBinding methodBinding = d.resolveBinding();
		
		if(paramPost != null || dynamicStateTest != null)
			throw new IllegalStateException("getLattice() called twice--must create a new instance of this class for every method being analyzed");
		paramPost = new HashMap<Aliasing, PermissionSetFromAnnotations>();
		dynamicStateTest = new HashMap<Boolean, String>(2);
		
		PluralTupleLatticeElement start;

		// receiver permission--skip for static methods
		if((d.getModifiers() & Modifier.STATIC) == 0) {
			thisVar = getAnalysisContext().getThisVariable();
			boolean isConstructor = d.isConstructor();
			// Get initial lattice, either constructor or regular method.
			start = isConstructor ? 
				PluralTupleLatticeElement.createConstructorLattice(
					FractionalPermissions.bottom(),
					input,
					context.getRepository(),
					thisVar,
					d)
					:	
				new PluralTupleLatticeElement(
					FractionalPermissions.bottom(),
					input,
					context.getRepository());
			
			start.storeInitialAliasingInfo(d);
			Aliasing thisLocation =
				start.getStartLocations(thisVar, d);
			
			if(isConstructor) { // constructor
				// start with an unpacked unique permission for alive
				StateSpace thisSpace = getStateSpace(methodBinding.getDeclaringClass());
				PermissionFromAnnotation thisPre = 
					pf.createUniqueOrphan(thisSpace, thisSpace.getRootState(), 
							true /* frame permission */, thisSpace.getRootState());
				FractionalPermissions this_initial = 
					PermissionSetFromAnnotations.createSingleton(thisPre).toLatticeElement();
				this_initial = this_initial.unpack(thisSpace.getRootState());
				start.put(thisLocation, this_initial);

				// post-condition
				Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> prePost = 
					// this is a hack to get VariableFractions in this's post-condition
					// we will ignore the generated pre-condition
					receiverPermissions(methodBinding, false, false);
				paramPost.put(thisLocation, prePost.snd());
			}
			else { 
				// regular instance method
				Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> prePost = 
					receiverPermissions(methodBinding, true, false);
				start.put(thisLocation, prePost.fst().toLatticeElement());
				paramPost.put(thisLocation, prePost.snd());
			}
		}
		else {
			// static method
			thisVar = null;
			start =	new PluralTupleLatticeElement(FractionalPermissions.bottom(),
					input, context.getRepository());
			start.storeInitialAliasingInfo(d);
		}
		
		/*
		 * Fill this.dynamicStateTest
		 */
		populateDynamicStateTest(d);
		
		// parameter permissions
		int paramCount = d.parameters().size();
		for(int param = 0; param < paramCount; param++) {
			SingleVariableDeclaration paramDecl = (SingleVariableDeclaration) d.parameters().get(param);
			SourceVariable paramVar = getAnalysisContext().getSourceVariable(paramDecl.resolveBinding());
			Aliasing paramLocation = start.getLocationsAfter(paramDecl, paramVar);
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> prePost = 
				parameterPermissions(methodBinding, param, true);
			start.put(paramLocation, prePost.fst().toLatticeElement());
			paramPost.put(paramLocation, prePost.snd());
		}
		
		// populate expected result
		resultPost = resultPermissions(methodBinding, false);
		
		return new Lattice<PluralTupleLatticeElement>(
				start, start.bottom());
	}

	private void populateDynamicStateTest(MethodDeclaration d) {
		/*
		 * Set up the dynamic state test tracker, so we know what to test against
		 * at the end of the method.
		 */
		IndicatesAnnotation true_annot = 
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(d.resolveBinding()), true);
		IndicatesAnnotation false_annot =
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(d.resolveBinding()), false);
		
		if( true_annot != null ) {
			this.dynamicStateTest.put(Boolean.TRUE, true_annot.getIndicatedState());
		}
		if( false_annot != null ) {
			this.dynamicStateTest.put(Boolean.FALSE, false_annot.getIndicatedState());
		}
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			ArrayInitInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			BinaryOperation binop, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
		LabeledResult<PluralTupleLatticeElement> result = 
			LabeledResult.createResult(labels, value);
		
		Aliasing op1_loc = value.getLocationsAfter(binop.getNode(), binop.getOperand1());
		Aliasing op2_loc = value.getLocationsAfter(binop.getNode(), binop.getOperand2());
		Aliasing target_loc = value.getLocationsAfter(binop.getNode(), binop.getTarget());
		
		if( labels.contains(BooleanLabel.getBooleanLabel(true)) ) {
			/*
			 * TRUE BRANCH
			 */
			PluralTupleLatticeElement branch_value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
			if( binop.getOperator().equals(BinaryOperator.BITWISE_AND) ) {
				/*
				 * When bitwise and is true, all of the operands involved are true.
				 */
				branch_value.addTrueVarPredicate(op1_loc);
				branch_value.addEquality(op1_loc, target_loc);
				branch_value.addEquality(op2_loc, target_loc);
				branch_value.addEquality(op1_loc, op2_loc);
			}
			else if( binop.getOperator().equals(BinaryOperator.REL_EQ) ) {
				branch_value.addEquality(op1_loc, op2_loc);
			}
			else if( binop.getOperator().equals(BinaryOperator.REL_NEQ) ) {
				branch_value.addInequality(op1_loc, op2_loc);
			}
			
			branch_value = SingleTruthFractionalTransfer.addNewlyDeducedFacts(binop, branch_value,
					op1_loc, op2_loc, target_loc);
			result.put(BooleanLabel.getBooleanLabel(true), branch_value);
		}
		
		if( labels.contains(BooleanLabel.getBooleanLabel(false)) ) {
			/*
			 * FALSE BRANCH
			 */
			PluralTupleLatticeElement branch_value = value.mutableCopy().storeCurrentAliasingInfo(binop.getNode());
			if( binop.getOperator().equals(BinaryOperator.BITWISE_OR) ) {
				/*
				 * When bitwise or is false, all the operands involved are false.
				 */
				branch_value.addFalseVarPredicate(op1_loc);
				branch_value.addEquality(op1_loc, target_loc);
				branch_value.addEquality(op2_loc, target_loc);
				branch_value.addEquality(op1_loc, target_loc);		
			}
			else if( binop.getOperator().equals(BinaryOperator.REL_EQ) ) {
				branch_value.addInequality(op1_loc, op2_loc);
			}
			else if( binop.getOperator().equals(BinaryOperator.REL_NEQ) ) {
				branch_value.addEquality(op1_loc, op2_loc);
			}
			
			branch_value = SingleTruthFractionalTransfer.addNewlyDeducedFacts(binop, branch_value,
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
	private static PluralTupleLatticeElement addNewlyDeducedFacts(
			TACInstruction instr,
			PluralTupleLatticeElement value,
			Aliasing... vs) {
		
		List<ImplicationResult> new_facts = value.solveWithHints(vs);
		for( ImplicationResult f : new_facts ) {
			value = f.putResultIntoLattice(value); 					
		}
				
		return value;
	}
	
	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			CastInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			ConstructorCallInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			CopyInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			DotClassInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			InstanceofInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			LoadArrayInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		// TODO make sure permission is non-zero
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			LoadFieldInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		
		if(instr.isStaticFieldAccess()) {
			// TODO what do we do with static fields?
		}
		else if(inStaticMethod() == false) {
			/*
			 * We may need to unpack...
			 * Check if field is from the receiver.
			 */
			Aliasing loc = value.getLocationsBefore(instr.getNode(), instr.getSourceObject());
			Aliasing this_loc = value.getLocationsBefore(instr.getNode(), thisVar);
			if( loc.equals(this_loc) && value.isRcvrPacked() ) {
				value = unpackReceiver(false, instr, value);
			}
		}
		else {
			if(log.isLoggable(Level.WARNING))
				log.warning("Unsupported field assignment to foreign object: " + instr.getNode());
		}
		return LabeledResult.createResult(labels, value);
	}

	private boolean inStaticMethod() {
		return thisVar == null;
	}

	private PluralTupleLatticeElement unpackReceiver(boolean isAssignment,
			final TACFieldAccess instr, final PluralTupleLatticeElement value) {
		/*
		 * Better see if fields have been unpacked...
		 */
		if( thisVar != null && value.isRcvrPacked() ) {
			StateSpace this_space = getStateSpace(thisVar.resolveType());
			String unpacking_root = 
				this_space.getFieldRootNode(instr.resolveFieldBinding());
			
			for(FractionalPermission p : value.get(instr.getNode(), thisVar).getPermissions()) {
				if(p.getRootNode().equals(unpacking_root))
					// permission with the right root available
					break;
				if(p.getStateSpace().firstBiggerThanSecond(p.getRootNode(), unpacking_root)) {
					// permission with bigger root available 
					FractionAssignment a = value.get(instr.getNode(), thisVar).getConstraints().simplify();
					if(! a.isOne(p.getFractions().get(unpacking_root)))
						// not a full permission -> do not try to move its root down
						unpacking_root = p.getRootNode();
					break;
				}
				if(p.getStateSpace().firstBiggerThanSecond(unpacking_root, p.getRootNode())) {
					// permission with smaller root available
					FractionAssignment a = value.get(instr.getNode(), thisVar).getConstraints().simplify();
					if(! a.isOne(p.getFractions().get(unpacking_root)))
						// not a unique permission -> do not try to move its root up
						unpacking_root = p.getRootNode();
					break;
				}
			}
			
//			unpacking_root = StateSpace.STATE_ALIVE;
			value.unpackReceiver(thisVar, instr.getNode(),
					context.getRepository(),
					new SimpleMap<Variable,Aliasing>() {
						@Override
						public Aliasing get(Variable key) {
							return value.getLocationsAfter(instr.getNode(), key);
						}}, 
					unpacking_root, isAssignment ? instr.getFieldName() : null);
		}
		return value;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			LoadLiteralInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		
		Aliasing target_loc = value.getLocationsAfter(instr.getNode(), instr.getTarget());
		if( instr.getLiteral() instanceof Boolean ) {

			boolean bool_value = ((Boolean)instr.getLiteral()).booleanValue();
			
			PluralTupleLatticeElement new_value = 
				value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
			
			FractionalPermissions perms = value.get(instr, instr.getTarget());	
			if( bool_value ) {
				new_value.addTrueVarPredicate(target_loc);
			}
			else {
				new_value.addFalseVarPredicate(target_loc);
			}

			new_value.put(instr, instr.getTarget(), perms);
			
			return LabeledResult.createResult(labels, new_value);
		}
		else if( instr.isNull() ) {
			PluralTupleLatticeElement new_value = 
				value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
				
			new_value.addNullVariable(target_loc);
			return LabeledResult.createResult(labels, new_value);
		}
		else {
			PluralTupleLatticeElement new_value = 
				value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
			
			// immutable permission for string or number literal
			PermissionSetFromAnnotations new_perms = PermissionSetFromAnnotations.createEmpty(StateSpace.SPACE_TOP);
			new_perms = new_perms.combine(pf.createImmutableOrphan(
					StateSpace.SPACE_TOP, 
					StateSpace.STATE_ALIVE, 
					false, // not a frame permission
					new String[] { StateSpace.STATE_ALIVE }, 
					true));
			new_value.put(instr, instr.getTarget(), new_perms.toLatticeElement());
			return LabeledSingleResult.createResult(new_value, labels);
		}
		
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			MethodCallInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		
		int argCount = instr.getArgOperands().size();
		IMethodBinding binding = instr.resolveBinding();

		// acquire permission sets to be used for pre- and post-conditions
		Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> thisPrePost = 
			instr.isStaticMethodCall() ? null : receiverPermissions(binding);
		Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost = new Pair[argCount];
		for(int arg = 0; arg < argCount; arg++) {
			argPrePost[arg] = parameterPermissions(binding, arg);
		}
		
		// Is 'this' a param or the receiver of the method call?
		List<Pair<Variable,PermissionSetFromAnnotations>> thisPermsToSplit = 
			new ArrayList<Pair<Variable,PermissionSetFromAnnotations>>();		
		
		// 0. make unpacked receiver modifiable and pack, if necessary

		boolean modifiesAnalyzedMethodReceiverField = false;
		Set<String> neededAnalyzedMethodReceiverState = new LinkedHashSet<String>();  // remembers needed state, if receiver is involved in this call
		// TODO should we assume methods modify by default?
		// the null tests enforce that we only assume modification if there's an annotation to that effect 
		if(thisPrePost != null) {
			modifiesAnalyzedMethodReceiverField = modifiesAnalyzedMethodReceiverField || 
					(!thisPrePost.fst().isReadOnly() && value.maybeFieldAccess(instr, instr.getReceiverOperand()));
			if(mayBeAnalyzedMethodReceiver(value, instr, instr.getReceiverOperand())) {
				neededAnalyzedMethodReceiverState.addAll(thisPrePost.fst().getStateInfo());
			}
		}
		for(int arg = 0; arg < argCount; arg++) {
			PermissionSetFromAnnotations pre = argPrePost[arg].fst();
			Variable x = (Variable) instr.getArgOperands().get(arg);
			modifiesAnalyzedMethodReceiverField = modifiesAnalyzedMethodReceiverField || 
					(!pre.isReadOnly() && value.maybeFieldAccess(instr, x));
			if(mayBeAnalyzedMethodReceiver(value, instr, x)) {
				neededAnalyzedMethodReceiverState.addAll(pre.getStateInfo());
			}
		}
		
		// 1. precondition
		
		// 1.1 receiver
		Variable methodRcvrVar = null;
		if(instr.isStaticMethodCall() == false) {
			methodRcvrVar = instr.getReceiverOperand();
			
			Aliasing rcvr_loc = value.getLocationsAfter(instr.getNode(), methodRcvrVar);
			Aliasing this_loc = value.getLocationsAfter(instr.getNode(), thisVar);
			
			if( !rcvr_loc.equals(this_loc) ) {
				// split off permission for method receiver
				value = splitOff(instr, value, methodRcvrVar, thisPrePost.fst());
			}
			else {
				thisPermsToSplit.add(Pair.create(methodRcvrVar, thisPrePost.fst()));
			}
		}
		// TODO handle permissions for "receiver" (TypeVariable) of static method calls 
		
		// 1.2 arguments
		for(int arg = 0; arg < argCount; arg++) {
			Variable x = (Variable) instr.getArgOperands().get(arg);
			PermissionSetFromAnnotations pre = argPrePost[arg].fst();

			Aliasing x_loc = value.getLocationsAfter(instr.getNode(), x);
			Aliasing this_loc = value.getLocationsAfter(instr.getNode(), thisVar);
			if( !x_loc.equals(this_loc) ) {
				// split off permission for argument
				value = splitOff(instr, value, x, pre);
			}
			else {
				thisPermsToSplit.add(Pair.create(x, pre));
			}
		}
				
		// 1.3 Now we pack. If 'this' is the method call receiver or an arg,
		//     we also split it off.
		value = prepareAnalyzedMethodReceiverForCall(instr, value,
				modifiesAnalyzedMethodReceiverField,
				neededAnalyzedMethodReceiverState.toArray(new String[neededAnalyzedMethodReceiverState.size()]));
		for( Pair<Variable, PermissionSetFromAnnotations> pair : thisPermsToSplit ) {
			value = splitOff(instr,value,pair.fst(),pair.snd());
		}
		
		// 2. forget state information for remaining permissions
		if(isEffectFree(binding, thisPrePost, argPrePost) == false) {
			value = forgetStateInfo(value);
			value = forgetFieldPermissionsIfNeeded(value, thisVar, instr);
		}
		
		
		// 3. postcondition
		
		// 3.1 receiver
		if(methodRcvrVar != null && thisPrePost != null)
			value = mergeIn(instr, value, methodRcvrVar, thisPrePost.snd());
		
		// 3.2 arguments
		for(int arg = 0; arg < argCount; arg++) {
			Variable x = (Variable) instr.getArgOperands().get(arg);
			value = mergeIn(instr, value, x, argPrePost[arg].snd());
		}
		
		// 3.3 released parameters
//		release:
//		if(methodRcvrVar != null)	{
//			AnnotationSummary m = crystal.getAnnotationDatabase().getSummaryForMethod(binding);
//			if(m == null) 
//				break release;
//			
//			ICrystalAnnotation anno = m.getReturn("edu.cmu.cs.plural.annot.Release");
//			if(anno == null)
//				break release;
//			
//			String releasedParam = (String) anno.getObject("value");
//			Aliasing loc = value.get(instr, methodRcvrVar).getParameter(releasedParam);
//			if(loc == null)
//				// parameter not instantiated --> throw away released permission
//				break release;
//			
//			PermissionSetFromAnnotations paramPerms = value.get(instr, methodRcvrVar).getParameterPermission(releasedParam);
//			if(paramPerms == null || paramPerms.isEmpty())
//				// no permission associated with parameter --> skip
//				break release;
//			
//			value.put(loc, value.get(loc).mergeIn(paramPerms));
//		}
		
		// 3.4 result
		value = addVariable(instr, value, instr.getTarget(), resultPermissions(binding));
			
		// 4.0 Begin Nels' First Shot at State Tests
		IResult<PluralTupleLatticeElement> result =	
			dynamicStateTestHelper(instr, labels, value);
		return result;
	}

	/**
	 * Attempts, among other things, to pack the current method receiver before a call
	 * occurs inside the method.
	 * @param instr
	 * @param value
	 * @param modifiesAnalyzedMethodReceiverField
	 * @param neededAnalyzedMethodReceiverState
	 */
	private PluralTupleLatticeElement prepareAnalyzedMethodReceiverForCall(
			final InvocationInstruction instr,
			final PluralTupleLatticeElement value,
			boolean modifiesAnalyzedMethodReceiverField,
			String... neededAnalyzedMethodReceiverState) {
		// make unpacked permission modifiable, if necessary
		if(modifiesAnalyzedMethodReceiverField) {
			if(value.isRcvrPacked()) {
				// TODO figure out what to do here
				log.warning("Can't force receiver permission to be modifiable at call site that requires modifying field permission.");
			}
			else {
				ThisVariable rcvr = getAnalysisContext().getThisVariable();
				FractionalPermissions rcvrPerms = value.get(instr, rcvr);
				value.put(instr, rcvr, rcvrPerms.makeUnpackedPermissionModifiable());
			}
		}
		
		if(neededAnalyzedMethodReceiverState.length == 0) {
			/*
			 * The receiver is not a
			 * parameter to the method and we have to search for a state that is
			 * suitable to pack to.
			 */
			if( !value.isRcvrPacked() ) {
				/*
				 * Try the post-condition and the pre-condition as better guesses.
				 */
				IMethodBinding this_method = this.getAnalysisContext().getAnalyzedMethod().resolveBinding();
				IInvocationSignature sig = context.getRepository().getSignature(this_method);
				
				List<String> states_to_try = new LinkedList<String>();
				// try post-condition states
				for(Set<String> rcvr_state_options: sig.getEnsuredReceiverStateOptions()) {
					states_to_try.addAll(rcvr_state_options);
				}
				// throw in the pre-condition, for good measure
				if(! sig.isConstructorSignature())
					for(Set<String> rcvr_state_options: sig.getMethodSignature().getRequiredReceiverStateOptions()) {
						states_to_try.addAll(rcvr_state_options);
					}
				
				boolean pack_worked = 
					value.packReceiverToBestGuess(this.getAnalysisContext().getThisVariable(),
							context.getRepository(), 
							new SimpleMap<Variable,Aliasing>() {
								@Override
								public Aliasing get(Variable key) {
									return value.getLocationsAfter(instr.getNode(), key);
								}},
							states_to_try.toArray(new String[states_to_try.size()])
							);
				if( !pack_worked ) {
					if(log.isLoggable(Level.WARNING))
						log.warning("Pack before method call, where we tried to pack to any state failed. " +
								"\nLattice:\n" + value.toString() + 
								"" + "\nMethod:"+instr.resolveBinding().getName());
				}
			}
			// do nothing if receiver already packed
		}
		else {
			// try to pack receiver to needed state
			if(value.isRcvrPacked()) {
				// do nothing for now--this may result in a pre-condition violation
				// TODO try unpack and re-pack if receiver is in wrong state...
			}
			else {
				StateSpace thisSpace = getStateSpace(thisVar.resolveType());
				Set<String> cleanedNeededState = AbstractFractionalPermission.cleanStateInfo(
						thisSpace, 
						value.get(instr, thisVar).getUnpackedPermission().getRootNode(), 
						Arrays.asList(neededAnalyzedMethodReceiverState), true);
				boolean pack_worked = 
					value.packReceiver(this.getAnalysisContext().getThisVariable(), 
						context.getRepository(),
						new SimpleMap<Variable, Aliasing>() {
							@Override
							public Aliasing get(Variable key) {
								return value.getLocationsAfter(instr.getNode(), key);
							}},
						cleanedNeededState);
				if( !pack_worked ) {
					if(log.isLoggable(Level.WARNING))
						log.warning("Pack before method call, where we knew what state we had to pack (" +
								neededAnalyzedMethodReceiverState + 
								")to failed. " +
								"\nLattice:\n" + value.toString() + 
								"" + "\nMethod:"+instr.resolveBinding().getName());
				}
			}
		}
		
		return value;
	}

	private boolean mayBeAnalyzedMethodReceiver(PluralTupleLatticeElement value, 
			TACInstruction instr,
			Variable x) {
		if(inStaticMethod())
			return false;
		Aliasing receiver_loc = value.getLocationsAfter(instr.getNode(), thisVar);
		Aliasing x_loc = value.getLocationsAfter(instr.getNode(), x);
		return receiver_loc.hasAnyLabels(x_loc.getLabels());
	}

	/**
	 * At certain points, we should no longer be able to think about permissions to
	 * fields because they may have been reassigned. If a method or constructor call
	 * does not retain FULL/UNIQUE/IMMUTABLE permission to the receiver, then must forget
	 * anything we know about fields of that receiver. However, because fields can be
	 * mapped to states, we do this on a dimension by dimension basis. 
	 */
	private static PluralTupleLatticeElement forgetFieldPermissionsIfNeeded(
			PluralTupleLatticeElement value, Variable thisVar,
			InvocationInstruction instr) {

		if( thisVar == null ) return value;
		
		FractionalPermissions this_perms = value.get(instr, thisVar);
		FractionAssignment fa = this_perms.getConstraints().simplify();
		
		Set<IVariableBinding> dont_forget_fields = new HashSet<IVariableBinding>();
		
		// For each field...
		for( IVariableBinding field : thisVar.resolveType().getDeclaredFields() ) {
			// Find that field's mapped node
			String mapped_node = this_perms.getStateSpace().getFieldRootNode(field);
			
			// For each permission of this...
			for( FractionalPermission this_perm : this_perms.getPermissions() ) {
				boolean dont_forget = false;
				Fraction belowF = this_perm.getFractions().getBelowFraction();
				Fraction const_ = fa.getConstant(belowF);
				
				// If perm root is >= mapped node
				if( this_perm.getStateSpace().firstBiggerThanSecond(this_perm.getRootNode(),
						mapped_node) ) {
					 
					// If One of two cases apply:
					// 1.) The permission type is easy to discover.			
					dont_forget |= fa.isOne(belowF); // UNIQUE/FULL
					dont_forget |= (this_perm.isReadOnly() && const_!=null && const_.isNamed()); // IMM
					
					// 2.) The permission takes more work to figure out.
					//     For now we will (soundly) ignore these
				}
				// if perm root < mapped node
				else if( this_perm.getStateSpace().firstBiggerThanSecond(mapped_node, this_perm.getRootNode()) &&
						 !mapped_node.equals(this_perm.getRootNode()) ) {
					// One of two cases also must apply, since 
					// we still need to make sure this permission is not NOTHING
					// 1.) The permission type is easy to discover.					
					dont_forget = (const_!=null && const_.isNamed()) | fa.isOne(belowF);
					
					// 2.) The permission takes more work to figure out.
					//     For now we will (soundly) ignore these					
				}
				// add to don't forget
				if( dont_forget ) dont_forget_fields.add(field);
			}
		}
		
		// Forget [all fields] \ [don't forget fields]
		for( IVariableBinding field : thisVar.resolveType().getDeclaredFields() ) {
			if( !dont_forget_fields.contains(field) ) {
				value.put(instr, new FieldVariable(field),
						FractionalPermissions.createEmpty());
			}
		}
		return value;
	}

	private LabeledResult<PluralTupleLatticeElement> dynamicStateTestHelper(
			MethodCallInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {

		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		
		// Well we know afterwards that the receiver is not null!
		Aliasing rcvr_loc = value.getLocationsAfter(instr.getNode(), instr.getReceiverOperand());
		value.addNonNullVariable(rcvr_loc);
		
		IndicatesAnnotation true_result = 
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(instr.resolveBinding()),
					true);
		IndicatesAnnotation false_result = 
			IndicatesAnnotation.getBooleanIndicatorOnReceiverIfAny(
					getAnnoDB().getSummaryForMethod(instr.resolveBinding()),
					false);
		addIndicatedImplications(instr, value, rcvr_loc, true_result,
				false_result);
		
		// state tests for arguments
		// TODO make indicator annotations available for method parameters; check if code below needs extension
		for(int arg = 0; arg < instr.getArgOperands().size(); arg++) {
			IndicatesAnnotation arg_true = 
				IndicatesAnnotation.getBooleanIndicatorIfAny(
						getAnnoDB().getSummaryForMethod(instr.resolveBinding()),
						arg, true);
			IndicatesAnnotation arg_false = 
				IndicatesAnnotation.getBooleanIndicatorIfAny(
						getAnnoDB().getSummaryForMethod(instr.resolveBinding()),
						arg, false);
			Aliasing arg_loc = value.getLocationsAfter(instr.getNode(), (Variable) instr.getArgOperands().get(arg));
			addIndicatedImplications(instr, value, arg_loc, arg_true,
					arg_false);
			
		}
		
		LabeledResult<PluralTupleLatticeElement> result = 
			LabeledResult.createResult(labels, value);
		//result.put(NormalLabel.getNormalLabel(), value, instr.getNode());
		
		/*
		 * Um, add TRUE/FALSE predicate for target. Normally not needed, but can't hurt.
		 * Only maybe needed for weird field stuff.
		 */
		if( labels.contains(BooleanLabel.getBooleanLabel(true)) ) {
			
		}
		
		/*
		 * Additionally, if we have boolean labels, we can just inject the new state
		 * right now.
		 */
		if( true_result != null && labels.contains(BooleanLabel.getBooleanLabel(true)) ) {
			PluralTupleLatticeElement br = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode()); // don't remove
			FractionalPermissions perms = br.get(instr, instr.getReceiverOperand());
			perms = perms.learnTemporaryStateInfo(true_result.getIndicatedState());
			br.put(instr, instr.getReceiverOperand(), perms);
			result.put(BooleanLabel.getBooleanLabel(true), br);
		}
		
		if( false_result != null && labels.contains(BooleanLabel.getBooleanLabel(false))) {
			PluralTupleLatticeElement br = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode()); // don't remove
			FractionalPermissions perms = br.get(instr, instr.getReceiverOperand());
			perms = perms.learnTemporaryStateInfo(false_result.getIndicatedState());
			br.put(instr, instr.getReceiverOperand(), perms);
			result.put(BooleanLabel.getBooleanLabel(false), br);			
		}
		return result;
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
	private PluralTupleLatticeElement addIndicatedImplications(MethodCallInstruction instr,
			PluralTupleLatticeElement value, Aliasing indicated_loc,
			IndicatesAnnotation true_result, IndicatesAnnotation false_result) {
		/*
		 * If either of the above results is non-null, we can add an implication to the
		 * normal path.
		 */
		if( true_result != null || false_result != null ) {
			Aliasing target_loc = value.getLocationsAfter(instr.getNode(), instr.getTarget());
			
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

	private boolean isEffectFree(IMethodBinding method, 
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> thisPrePost, 
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost) {
		for(int i = 0; i < method.getParameterTypes().length; i++) {
			if(argPrePost[i] != null && argPrePost[i].fst().isReadOnly() == false)
				return false;
		}
		if(method.isConstructor())
			// for constructors, receiver is a "result", so we don't check receiver pre-condition
			return true;
		return thisPrePost != null && thisPrePost.fst().isReadOnly();
	}

	private PluralTupleLatticeElement forgetStateInfo(
			PluralTupleLatticeElement value) {
		for(ExtendedIterator<FractionalPermissions> it = value.tupleInfoIterator(); it.hasNext(); ) {
			FractionalPermissions permissions = it.next();
			permissions = permissions.forgetShareAndPureStates();
			it.replace(permissions);
		}
		return value;
	}

	private PluralTupleLatticeElement addVariable(
			TACInstruction instr,
			PluralTupleLatticeElement value,
			Variable x, PermissionSetFromAnnotations anno) {
		return addVariable(instr, value, x, anno, Collections.<String, Aliasing>emptyMap(), 
				Collections.<Aliasing, PermissionSetFromAnnotations>emptyMap());
	}

	private PluralTupleLatticeElement addVariable(
			TACInstruction instr,
			PluralTupleLatticeElement value,
			Variable x, PermissionSetFromAnnotations newPerms, 
			Map<String, Aliasing> parameters, 
			Map<Aliasing, PermissionSetFromAnnotations> parameterPermissions) {
		if(x == null) throw new NullPointerException("Null variable provided");
		value.put(instr, x, newPerms.toLatticeElement(/*parameters, parameterPermissions*/));
		return value;
	}

	private PluralTupleLatticeElement mergeIn(
			TACInstruction instr,
			PluralTupleLatticeElement value,
			Variable x, PermissionSetFromAnnotations toMerge) {
		if(x == null) throw new NullPointerException("Null variable provided");
		if(toMerge == null || toMerge.isEmpty())
			return value;
		
		FractionalPermissions permissions = value.get(instr, x);
		permissions = permissions.mergeIn(toMerge);
		value.put(instr, x, permissions);
		return value;
	}
	
	private PluralTupleLatticeElement splitOff(
			TACInstruction instr,
			PluralTupleLatticeElement value,
			Variable x, PermissionSetFromAnnotations toSplit) {
		if(x == null) throw new NullPointerException("Null variable provided");
		if(toSplit == null || toSplit.isEmpty()) 
			return value;

		FractionalPermissions permissions = value.get(instr, x);
		permissions = permissions.splitOff(toSplit);
		value.put(instr, x, permissions);
		return value;
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			NewArrayInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		StateSpace arraySpace = getStateSpace(instr.getArrayType().resolveBinding());
		FractionConstraints newConstraints = new FractionConstraints();
		
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		value.put(instr, instr.getTarget(), PermissionSetFromAnnotations.createSingleton(
				pf.createUniqueOrphan(arraySpace, arraySpace.getRootState(), 
						false /* not a frame permission */, 
						arraySpace.getRootState())).toLatticeElement());
		
		// After insantiation, we know the target to not be null! 
		value.addNonNullVariable(value.getLocationsAfter(instr.getNode(), instr.getTarget()));
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			NewObjectInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());

		int argCount = instr.getArgOperands().size();
		IMethodBinding binding = instr.resolveBinding();
		
		// acquire permission sets to be used for argument pre- and post-conditions
		Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations>[] argPrePost = 
			new Pair[argCount];
		for(int arg = 0; arg < argCount; arg++) {
			argPrePost[arg] = parameterPermissions(binding, arg);
		}

		// Is 'this' a param or the receiver of the method call?
		List<Pair<Variable,PermissionSetFromAnnotations>> thisPermsToSplit = 
			new ArrayList<Pair<Variable,PermissionSetFromAnnotations>>();	
		
		// 0. make unpacked permission modifiable, if necessary

		boolean modifiesAnalyzedMethodReceiverField = false;
		// remembers needed state, if receiver is involved in this call
		Set<String> neededAnalyzedMethodReceiverState = new LinkedHashSet<String>();  
		// TODO should we assume methods modify by default?
		// the null tests enforce that we only assume modification if there's an annotation to that effect 
		for(int arg = 0; arg < argCount; arg++) {
			PermissionSetFromAnnotations pre = argPrePost[arg].fst();
			Variable x = (Variable) instr.getArgOperands().get(arg);
			modifiesAnalyzedMethodReceiverField = modifiesAnalyzedMethodReceiverField || 
					(!pre.isReadOnly() && value.maybeFieldAccess(instr, x));
			if(mayBeAnalyzedMethodReceiver(value, instr, x)) {
				neededAnalyzedMethodReceiverState.addAll(pre.getStateInfo());
			}
		}
		
		// 1. argument preconditions 
		Map<String, Aliasing> instantiatedParameters = new HashMap<String, Aliasing>();
		Map<Aliasing, PermissionSetFromAnnotations> parameterPermissions = new HashMap<Aliasing, PermissionSetFromAnnotations>();
		for(int arg = 0; arg < argCount; arg++) {
			Variable x = (Variable) instr.getArgOperands().get(arg);
			PermissionSetFromAnnotations pre = argPrePost[arg].fst();

			Aliasing x_loc = value.getLocationsAfter(instr.getNode(), x);
			Aliasing this_loc = value.getLocationsAfter(instr.getNode(), thisVar);
			if( !x_loc.equals(this_loc) ) {
				// split off argument permission
				value = splitOff(instr, value, x, pre);
			}
			else {
				thisPermsToSplit.add(Pair.create(x, pre));
			}

			// instantiate parameters (it's ok to do this even if x points to this_loc, since
			// pre is unaffected by packing the receiver below
			for(ParameterPermissionAnnotation a : CrystalPermissionAnnotation.parameterAnnotations(getAnnoDB(), binding, arg)) {
				// this is supposed to test whether this constructor argument instantiates a parameter
				if(! "".equals(a.getParameter())) {
					Aliasing loc = value.getLocationsBefore(instr.getNode(), x);
					instantiatedParameters.put(a.getParameter(), loc);
					if(pre != null)
						parameterPermissions.put(loc, pre);
				}
			}
		}
		
		// 1.1  -Pack 'this' before what is effectively a method call.
		//      -Split off 'this' permissions that we were waiting for.
		value = prepareAnalyzedMethodReceiverForCall(instr, value, 
				modifiesAnalyzedMethodReceiverField, 
				neededAnalyzedMethodReceiverState.toArray(new String[neededAnalyzedMethodReceiverState.size()]));
		for( Pair<Variable, PermissionSetFromAnnotations> pair : thisPermsToSplit ) {
			value = splitOff(instr,value,pair.fst(),pair.snd());
		}
		
		// 2. forget state information for remaining permissions
		if(isEffectFree(instr.resolveBinding(), null /* no receiver */, argPrePost) == false) {
			value = forgetStateInfo(value);
			value = forgetFieldPermissionsIfNeeded(value, thisVar, instr);
		}
		
		// 3. post-condition
		
		// 3.1 arguments
		for(int arg = 0; arg < argCount; arg++) {
			Variable x = (Variable) instr.getArgOperands().get(arg);
			value = mergeIn(instr, value, x, argPrePost[arg].snd());
		}
		
		// 3.2 result / new object
		value = addVariable(instr, value, instr.getTarget(), receiverPermissions(instr.resolveBinding()).snd(),
				instantiatedParameters, parameterPermissions);
		
		// 3.3 We know that the target operand is not null!
		value.addNonNullVariable(value.getLocationsAfter(instr.getNode(), instr.getTarget()));
		
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			SourceVariableDeclaration instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		// If this variable is the parameter to a catch block, we default
		// to the default permission. Catch blocks aren't really called...
		if( instr.isCaughtVariable() ) {
			FractionalPermissions perm = 
				getDefaultCatchParamPermission(instr.getDeclaredVariable());
			value.put(instr, instr.getDeclaredVariable(), perm);
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
	public IResult<PluralTupleLatticeElement> transfer(
			StoreArrayInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		FractionalPermissions permissions = value.get(instr, instr.getDestinationArray());
		// make sure that we can write to the array
		permissions = permissions.makeModifiable(permissions.getStateSpace().getRootState(), 
				false /* use virtual permissions */);
		value.put(instr, instr.getDestinationArray(), permissions);
		// TODO check whether permission stored in array is compatible with array declaration
		return super.transfer(instr, labels, value);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			StoreFieldInstruction instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		
		if(instr.isStaticFieldAccess()) {
			// TODO what do we do with static fields?
		}
		else if(inStaticMethod() == false) {
			/*
			 * We may need to unpack...
			 * Check if field is from the receiver.
			 */
			Aliasing loc = value.getLocationsBefore(instr.getNode(), instr.getDestinationObject());
			Aliasing this_loc = value.getLocationsBefore(instr.getNode(), thisVar);
			if( loc.equals(this_loc) ) {
				/*
				 * 1. Save permission that's assigned to field. 
				 */
				FractionalPermissions new_field_perms = value.get(
						value.getLocationsBefore(instr.getNode(), instr.getSourceOperand()));
	
				/*
				 * 2. Unpack receiver if needed.
				 * This may generate permissions for the field, and because of aliasing,
				 * override the permission for the source operand
				 * TODO Maybe we can avoid affecting the source operand during unpacking?
				 */
				if( value.isRcvrPacked() )
					value = unpackReceiver(true, instr, value);
				
				/*
				 * 3. Force unpacked permission to be modifiable, to allow assignment.
				 */
				FractionalPermissions rcvrPerms = value.get(loc);
				rcvrPerms = rcvrPerms.makeUnpackedPermissionModifiable();
				value.put(loc, rcvrPerms);
				
				/*
				 * 4. Override potential permission for assigned field from unpacking
				 * with saved permission being assigned.  This will fix the permission
				 * associated with the source operand as well, because of aliasing. 
				 */
				value.put(instr, new FieldVariable(instr.resolveFieldBinding()), new_field_perms);
			}
			else {
				if(log.isLoggable(Level.WARNING))
					log.warning("Unsupported field assignment to foreign object: " + instr.getNode());
			}
		}
		else {
			if(log.isLoggable(Level.WARNING))
				log.warning("Unsupported field assignment to foreign object: " + instr.getNode());
		}
		return LabeledSingleResult.createResult(value, labels);
	}

	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			UnaryOperation unop, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		
		value = value.mutableCopy().storeCurrentAliasingInfo(unop.getNode());
		
		final Aliasing target_loc = value.getLocationsAfter(unop.getNode(), unop.getTarget());
		final Aliasing op_loc = value.getLocationsAfter(unop.getNode(), unop.getOperand());
		
		if( unop.getOperator().equals(UnaryOperator.BOOL_NOT) ) {
			/*
			 * Add an inequality.
			 */
			value.addInequality(target_loc, op_loc);
			value = SingleTruthFractionalTransfer.addNewlyDeducedFacts(unop, value,
					op_loc, target_loc);
		}
		
		LabeledResult<PluralTupleLatticeElement> result = 
			LabeledResult.createResult(labels, value);
		if( labels.contains(BooleanLabel.getBooleanLabel(true)) ) {
			/*
			 * TRUE branch...
			 */
			if( unop.getOperator().equals(UnaryOperator.BOOL_NOT) ) {
				/*
				 * We can add negative facts.
				 */
				PluralTupleLatticeElement br = value.mutableCopy().storeCurrentAliasingInfo(unop.getNode());
				br.addTrueVarPredicate(target_loc);
				//br.addInequality(unop.getTarget(), unop.getOperand());
				br = SingleTruthFractionalTransfer.addNewlyDeducedFacts(unop, br,
						op_loc, target_loc);
				result.put(BooleanLabel.getBooleanLabel(true), br);
			}
		}
		if( labels.contains(BooleanLabel.getBooleanLabel(false))) {
			/*
			 * FALSE branch
			 */
			if( unop.getOperator().equals(UnaryOperator.BOOL_NOT) ) {
				PluralTupleLatticeElement br = value.mutableCopy().storeCurrentAliasingInfo(unop.getNode());
				br.addFalseVarPredicate(target_loc);
				//br.addInequality(unop.getTarget(), unop.getOperand());
				br = SingleTruthFractionalTransfer.addNewlyDeducedFacts(unop, br,
						op_loc, target_loc);
				result.put(BooleanLabel.getBooleanLabel(false), br);
			}
		}
		
		return result;
	}
	
	@Override
	public IResult<PluralTupleLatticeElement> transfer(
			SourceVariableRead instr, List<ILabel> labels,
			PluralTupleLatticeElement value) {
		value = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
		/*
		 * If we read a variable, and we have boolean labels exiting that statement, it's
		 * because someone is testing the truth of that boolean. If we have state implications
		 * that depend on the truth or falsehood of that variable, we could learn new information.
		 */
		LabeledResult<PluralTupleLatticeElement> result = 
			LabeledResult.createResult(labels, value);

		final Aliasing var_loc = value.getLocationsAfter(instr.getNode(), instr.getVariable());
		
		if( labels.contains(BooleanLabel.getBooleanLabel(true)) ) {

			/*
			 * If the branch is true, does adding this predicate eliminate any
			 * implications?
			 */			
			PluralTupleLatticeElement br = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
			br.addTrueVarPredicate(var_loc);
			
			br = SingleTruthFractionalTransfer.addNewlyDeducedFacts(instr, br, var_loc);
			result.put(BooleanLabel.getBooleanLabel(true), br);
		}

		if( labels.contains(BooleanLabel.getBooleanLabel(false)) ) {
			/*
			 * Same, but we know the variable is false.
			 */
			PluralTupleLatticeElement br = value.mutableCopy().storeCurrentAliasingInfo(instr.getNode());
			br.addFalseVarPredicate(var_loc);
			
			br = SingleTruthFractionalTransfer.addNewlyDeducedFacts(instr, br, var_loc);
			result.put(BooleanLabel.getBooleanLabel(false), br);
		}

		return result;
	}
	
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
	receiverPermissions(IMethodBinding binding) {
		return this.receiverPermissions(binding, false, true);
	}

	/**
	 * Returns the pre and post condition permissions for the receiver of the
	 * method call, given the method binding.
	 * @param frameAsVirtual 
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
	receiverPermissions(IMethodBinding binding, boolean namedFractions, boolean frameAsVirtual) {
		if(isStaticMethod(binding))
			return null;
		StateSpace space = getStateSpace(binding.getDeclaringClass());
		Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> result = 
			prePostFromAnnotations(space, 
					CrystalPermissionAnnotation.receiverAnnotations(getAnnoDB(), binding), namedFractions, frameAsVirtual);
		
		result = mergeWithParsedRcvrPermissions(result, binding, space, namedFractions);
		if(binding.isConstructor())
			result = Pair.create(null, result.snd());
		return result;
	}

	private boolean isStaticMethod(IMethodBinding binding) {
		return (binding.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
	}

	/**
	 * Returns the pre and post condition permissions for the paramIndex-th 
	 * parameter of the binding method.
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
	parameterPermissions(IMethodBinding binding, int paramIndex) {
		return this.parameterPermissions(binding, paramIndex, false);
	}
	
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
	parameterPermissions(IMethodBinding binding, int paramIndex, boolean namedFractions) {
		
		StateSpace space = getStateSpace(binding.getParameterTypes()[paramIndex]);
		Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> result = prePostFromAnnotations(space, 
				CrystalPermissionAnnotation.parameterAnnotations(getAnnoDB(), binding, paramIndex), namedFractions, false);
		
		result = mergeWithParsedParamPermissions(result, binding, space, paramIndex, namedFractions);
		return result;
	}

	/**
	 * @param space
	 * @param annos
	 * @param namedFractions
	 * @param frameAsVirtual 
	 * @return
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> prePostFromAnnotations(
			StateSpace space, List<ParameterPermissionAnnotation> annos, boolean namedFractions, boolean frameAsVirtual) {
		PermissionSetFromAnnotations pre = PermissionSetFromAnnotations.createEmpty(space);
		PermissionSetFromAnnotations post = PermissionSetFromAnnotations.createEmpty(space);
		for(ParameterPermissionAnnotation a : annos) {
			PermissionFromAnnotation p = pf.createOrphan(space, a.getRootNode(), a.getKind(), !frameAsVirtual && a.isFramePermission(), a.getRequires(), namedFractions);
			pre = pre.combine(p);
			if(a.isReturned())
				post = post.combine(p.copyNewState(a.getEnsures()));
		}
		return Pair.create(pre, post);
	}

	/**
	 * This method takes receiver permissions that came from the old-style
	 * permission annotations on a method and combines them with permissions from
	 * the new-style annotations.
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> mergeWithParsedRcvrPermissions(
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> preAndPost,
			IMethodBinding binding, StateSpace space, boolean namedFractions) {
		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		
		if( preAndPostString == null ) {
			return preAndPost;
		}
			
		PermissionSetFromAnnotations prePerm = preAndPost.fst();
		PermissionSetFromAnnotations postPerm = preAndPost.snd();
		
		Pair<List<PermissionFromAnnotation>,
		List<PermissionFromAnnotation>> prePostPerms = 
			PermParser.parseReceiverPermissions(preAndPostString.fst(), preAndPostString.snd(),
					space, namedFractions);
		for( PermissionFromAnnotation pre_p : prePostPerms.fst() ) {
			prePerm = prePerm.combine(pre_p);
		}
		for( PermissionFromAnnotation post_p : prePostPerms.snd() ) {
			postPerm = postPerm.combine(post_p);
		}
		return Pair.create(prePerm, postPerm);
	}
	/**
	 * This method merges takes the pre and post permission for a parameter from the old style annotations and
	 * merges in the permissions from the new-style annotations.
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> mergeWithParsedParamPermissions(
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> preAndPost,
			IMethodBinding binding, StateSpace space, int paramIndex, boolean namedFractions) {
		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		
		if( preAndPostString == null ) {
			return preAndPost;
		}
			
		PermissionSetFromAnnotations prePerm = preAndPost.fst();
		PermissionSetFromAnnotations postPerm = preAndPost.snd();
		
		Pair<List<PermissionFromAnnotation>,
		List<PermissionFromAnnotation>> prePostPerms = 
			PermParser.parseParameterPermissions(preAndPostString.fst(), preAndPostString.snd(),
					space, paramIndex, namedFractions);
		for( PermissionFromAnnotation pre_p : prePostPerms.fst() ) {
			prePerm = prePerm.combine(pre_p);
		}
		for( PermissionFromAnnotation post_p : prePostPerms.snd() ) {
			postPerm = postPerm.combine(post_p);
		}

		return Pair.create(prePerm, postPerm);
	}
	/**
	 * This method takes the result permission for a parameter from the old style
	 * annotations and merges in the permissions from the new-style annotations.
	 */
	private PermissionSetFromAnnotations mergeWithParsedResultPermissions(
			PermissionSetFromAnnotations result, IMethodBinding binding,
			StateSpace space, boolean namedFractions) {
		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		
		if( preAndPostString == null ) {
			return result;
		}
			
		List<PermissionFromAnnotation> postPerms = 
			PermParser.parseResultPermissions(preAndPostString.snd(),
					space, namedFractions);
		for( PermissionFromAnnotation pre_p : postPerms ) {
			result = result.combine(pre_p);
		}

		return result;
	}
	
	private PermissionSetFromAnnotations 
	resultPermissions(IMethodBinding binding) {
		PermissionSetFromAnnotations result = resultPermissions(binding, true);
		return result;
	}
	
	private PermissionSetFromAnnotations 
	resultPermissions(IMethodBinding binding, boolean namedFractions) {
		StateSpace space = getStateSpace(binding.getReturnType());
		PermissionSetFromAnnotations result = PermissionSetFromAnnotations.createEmpty(space);
		for(ResultPermissionAnnotation a : CrystalPermissionAnnotation.resultAnnotations(getAnnoDB(), binding)) {
			PermissionFromAnnotation p = pf.createOrphan(space, a.getRootNode(), a.getKind(), a.getEnsures(), namedFractions);
			result = result.combine(p);
		}
		result = mergeWithParsedResultPermissions(result, binding, space, namedFractions);
		return result;
	}

	private AnnotationDatabase getAnnoDB() {
		return input.getAnnoDB();
	}

	private StateSpace getStateSpace(ITypeBinding type) {
		return context.getRepository().getStateSpace(type);
	}
}
