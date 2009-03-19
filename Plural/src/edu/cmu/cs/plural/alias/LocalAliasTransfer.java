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
package edu.cmu.cs.plural.alias;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;

import edu.cmu.cs.crystal.ILabel;
import edu.cmu.cs.crystal.analysis.alias.AliasLE;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.analysis.alias.DefaultObjectLabel;
import edu.cmu.cs.crystal.analysis.alias.MayAliasTransferFunction;
import edu.cmu.cs.crystal.analysis.alias.ObjectLabel;
import edu.cmu.cs.crystal.analysis.metrics.LoopCountingAnalysis;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledSingleResult;
import edu.cmu.cs.crystal.simple.LatticeElementOps;
import edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.AssignmentInstruction;
import edu.cmu.cs.crystal.tac.BinaryOperation;
import edu.cmu.cs.crystal.tac.CastInstruction;
import edu.cmu.cs.crystal.tac.ConstructorCallInstruction;
import edu.cmu.cs.crystal.tac.CopyInstruction;
import edu.cmu.cs.crystal.tac.DotClassInstruction;
import edu.cmu.cs.crystal.tac.EnhancedForConditionInstruction;
import edu.cmu.cs.crystal.tac.ITACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.InstanceofInstruction;
import edu.cmu.cs.crystal.tac.InvocationInstruction;
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
import edu.cmu.cs.crystal.tac.ThisVariable;
import edu.cmu.cs.crystal.tac.UnaryOperation;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.track.FieldVariable;

/**
 * @author Ciera Christopher
 * @author Kevin Bierhoff (refactorings from {@link MayAliasTransferFunction})
 *
 */
public class LocalAliasTransfer extends
		AbstractTACBranchSensitiveTransferFunction<AliasingLE> implements
		ITACBranchSensitiveTransferFunction<AliasingLE> {
	
	private final LoopCountingAnalysis loopCounter;
//	private final LivenessProxy liveness;
	private Map<Variable, ObjectLabel> labelContext;

	/*
	 * This map contains fields of 'this' and the labels they are associated with.
	 * I know this is a hack, it is temporary.
	 */
	private final Map<IVariableBinding, Variable> receiverFields;
	private AnnotationDatabase annodb;
	
	public LocalAliasTransfer(AnnotationDatabase adb, Map<IVariableBinding, Variable> map) {
		this.annodb = adb;
		loopCounter = new LoopCountingAnalysis();
//		liveness = LivenessProxy.create();
		labelContext = new HashMap<Variable, ObjectLabel>();
		receiverFields = map;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.IFlowAnalysisDefinition#getLattice(org.eclipse.jdt.core.dom.MethodDeclaration)
	 */
	public ILatticeOperations<AliasingLE> createLatticeOperations(MethodDeclaration method) {
		return LatticeElementOps.create(AliasingLE.createBottom());
	}
	
	public AliasingLE createEntryValue(MethodDeclaration method) {
		labelContext = new HashMap<Variable, ObjectLabel>();
//		liveness.switchToMethod(methodDeclaration);
		AliasingLE entry = AliasingLE.createEmpty();
		
		// this
		Variable thisVar = getAnalysisContext().getThisVariable();
		if(thisVar != null) { // static methods
			ObjectLabel thisLabel = createThisLabel(thisVar);
			labelContext.put(thisVar, thisLabel);
			entry.put(thisVar, AliasLE.create(thisLabel));
		}
		
		// super
		Variable superVar = getAnalysisContext().getSuperVariable();
		if(superVar != null) { // static methods, java.lang.Object methods, interface methods
			ObjectLabel superLabel = createSuperLabel(superVar);
			labelContext.put(superVar, superLabel);
			entry.put(superVar, AliasLE.create(superLabel));
		}
		// TODO what about qualified this's and super's?
		
		/*
		 * Create brand new locations for each field of this.
		 */
		ITypeBinding this_type = method.resolveBinding().getDeclaringClass();
		while(this_type != null) {
			for( final IVariableBinding field : this_type.getDeclaredFields() ) {
				if((field.getModifiers() & Modifier.STATIC) != 0)
					// skip static fields
					continue;
				final FieldVariable field_var = new FieldVariable(field);
				receiverFields.put(field, field_var);
				/*
				 * Make brand-new label, like in SourceVarDecl
				 */
				ObjectLabel label = new ObjectLabel() {
					public ITypeBinding getType() {
						return field.getType();
					}
					public boolean isSummary() {
						return false;
					}
					@Override public String toString() {
						return "field." + field.getName();
					}
				};
				final AliasLE aliases = AliasLE.create(label);
				entry.put(field_var, aliases);
			}
			this_type = this_type.getSuperclass();
		}
		
		return entry;
	}

	private ObjectLabel createThisLabel(final Variable thisVar) {
		// special label for "this"
		return new ObjectLabel() {
			public ITypeBinding getType() {
				return thisVar.resolveType();
			}
			public boolean isSummary() {
				return false;
			}
			@Override
			public String toString() {
				return thisVar.getSourceString();
			}
		};
	}

	private ObjectLabel createSuperLabel(final Variable superVar) {
		// special label for "super"
		return new ObjectLabel() {
			public ITypeBinding getType() {
				return superVar.resolveType();
			}
			public boolean isSummary() {
				return false;
			}
			@Override
			public String toString() {
				return superVar.getSourceString();
			}
		};
	}
	
	private ObjectLabel getLabel(Variable associatedVar, ITypeBinding binding, TACInstruction declaringInstr) {
		//if we already produced an initial label for this variable, use it
		// TODO use special labels for literals and in particular null
		if (labelContext.get(associatedVar) != null) {
			return labelContext.get(associatedVar);
		}
		else {
			boolean isInLoop = loopCounter.isInLoop(declaringInstr.getNode());
			ObjectLabel label = new DefaultObjectLabel(binding, isInLoop);
			labelContext.put(associatedVar, label);
			return label;
		}
	}
	
	private AliasingLE 
	putSingletonLabel(AssignmentInstruction instr, AliasingLE value) {
		ObjectLabel label = getLabel(instr.getTarget(), instr.getTarget().resolveType(), instr);
		AliasLE aliases = AliasLE.create(label);
		value.put(instr.getTarget(), aliases);
		return value;
	}
	
	/**
	 * Create a label for the invocation result, with special treatment of
	 * @IsResult, @Capture, and @Lend annotations
	 * @param instr
	 * @param receiver Receiver variable, if this invocation has a receiver, 
	 * <code>null</code> otherwise.  Use <code>null</code> for static methods!
	 * @param value
	 * @return
	 */
	private AliasingLE 
	putSingletonLabel(InvocationInstruction instr, Variable receiver, AliasingLE value) {
		IMethodBinding binding = instr.resolveBinding();

		/*
		 * 1. See if one of the parameters is the result or lends the result
		 */
		if(receiver != null) {
			ICrystalAnnotation a = getAnnoDB().getSummaryForMethod(binding).getReturn(
					"edu.cmu.cs.plural.annot.IsResult");
			if(a != null) {
				value.put(instr.getTarget(), value.get(receiver));
				// have result point to the same locations as the receiver and quit
				return value;
			}
			
			ICrystalAnnotation l = getAnnoDB().getSummaryForMethod(binding).getReturn(
					"edu.cmu.cs.plural.annot.Lend");
			if(l != null) {
				String paramName = (String) l.getObject("param");
				Variable x = new ParamVariable(value.get(receiver), paramName, null /* not relevant for lookup */);
				if(! value.get(x).getLabels().isEmpty()) {
					value.put(instr.getTarget(), value.get(x));
					// have result point to the lent parameter and quit, if parameter location known
					return value;
				}
			}
		}
		for(int i = 0; i < binding.getParameterTypes().length; i++) {
			ICrystalAnnotation a = getAnnoDB().getSummaryForMethod(binding).getParameter(
					i, "edu.cmu.cs.plural.annot.IsResult");
			if(a != null) {
				value.put(instr.getTarget(), value.get(instr.getArgOperands().get(i)));
				// have result point to the same locations as the annotated parameter and quit
				return value;
			}

			ICrystalAnnotation l = getAnnoDB().getSummaryForMethod(binding).getParameter(
					i, "edu.cmu.cs.plural.annot.Lend");
			if(l != null) {
				String paramName = (String) l.getObject("param");
				Variable x = new ParamVariable(value.get(instr.getArgOperands().get(i)), 
						paramName, null /* not relevant for lookup */);
				if(! value.get(x).getLabels().isEmpty()) {
					value.put(instr.getTarget(), value.get(x));
					// have result point to the lent parameter and quit, if parameter location known
					return value;
				} // else make up new location
			}
			
		}
		
		/*
		 * 2. Create separate location for result
		 */
		ObjectLabel label = getLabel(instr.getTarget(), instr.getTarget().resolveType(), instr);
		AliasLE aliases = AliasLE.create(label);
		value.put(instr.getTarget(), aliases);
		
		/*
		 * 3. Create a ParamVariable for each parameter annotated with @Param
		 */
		if(receiver != null) {
			ICrystalAnnotation a = getAnnoDB().getSummaryForMethod(binding).getReturn(
					"edu.cmu.cs.plural.annot.Capture");
			if(a != null) {
				String paramName = (String) a.getObject("param");
				Variable x = new ParamVariable(aliases, paramName, binding.getDeclaringClass());
				value.put(x, value.get(receiver));
			}
		}
		for(int i = 0; i < binding.getParameterTypes().length; i++) {
			ICrystalAnnotation a = getAnnoDB().getSummaryForMethod(binding).getParameter(
					i, "edu.cmu.cs.plural.annot.Capture");
			if(a != null) {
				String paramName = (String) a.getObject("param");
				Variable x = new ParamVariable(aliases, paramName, binding.getParameterTypes()[i]);
				value.put(x, value.get(instr.getArgOperands().get(i)));
			}
		}
		
		return value;
	}
	
	private AnnotationDatabase getAnnoDB() {
		return annodb;
	}
	
	@Override
	public IResult<AliasingLE> transfer(
			ArrayInitInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, instr, instr.getInitOperands());
		return LabeledSingleResult.createResult(putSingletonLabel(instr, value), labels);
	}

	/**
	 * @param value 
	 * @param instr TODO
	 * @param initOperands
	 * @return the changed <code>value</code>.
	 */
	private AliasingLE killAllDead(AliasingLE value, TACInstruction instr, Collection<Variable> variables) {
		if(variables.isEmpty())
			return value;
		for(Variable x : variables) {
			if(isDead(instr, x))
				value.kill(x);
		}
		return value;
	}

	/**
	 * @param value 
	 * @param instr TODO
	 * @param initOperands
	 * @return the changed <code>value</code>.
	 */
	private AliasingLE killAllDead(AliasingLE value, TACInstruction instr, Variable... variables) {
		if(variables.length == 0)
			return value;
		for(Variable x : variables) {
			if(isDead(instr, x))
				value.kill(x);
		}
		return value;
	}

	/**
	 * @param instr TODO
	 * @param x
	 * @return
	 */
	private boolean isDead(TACInstruction instr, Variable x) {
		return false;
//		if(x instanceof SourceVariable && ((SourceVariable) x).getBinding().isParameter())
//			// keep parameters live for post-condition checks
//			return false;
//		if(x instanceof SourceVariable || x instanceof TempVariable) {
//			return liveness.isDeadAfter(instr.getNode(), x);
//		}
//		else
//			// keyword, type variables can't die
//			// TODO field variables?
//			return false;
	}

	@Override
	public IResult<AliasingLE> transfer(
			BinaryOperation binop, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, binop, binop.getOperand1(), binop.getOperand2());
		return LabeledSingleResult.createResult(putSingletonLabel(binop, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			CastInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		// TODO maybe one could use the type cased to to drop aliases of incompatible types?
		value = value.mutableCopy();
		value.put(instr.getTarget(), value.get(instr.getOperand()).copy());
		value = killAllDead(value, instr, instr.getOperand());
		return LabeledSingleResult.createResult(value, labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			DotClassInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		//TODO: consider how to handle literals
		value = value.mutableCopy();
		return LabeledSingleResult.createResult(putSingletonLabel(instr, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			CopyInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		//if value does not contain instr, then what? will it do the put automatically?
		value = value.mutableCopy();
		value.put(instr.getTarget(), value.get(instr.getOperand()).copy());
		value = killAllDead(value, instr, instr.getOperand());
		return LabeledSingleResult.createResult(value, labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			InstanceofInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		// result of instanceof is a fresh (boolean) label
		value = value.mutableCopy();
		value = killAllDead(value, instr, instr.getOperand());
		return LabeledSingleResult.createResult(putSingletonLabel(instr, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			LoadLiteralInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		//TODO: consider how to handle literals
		value = value.mutableCopy();
		return LabeledSingleResult.createResult(putSingletonLabel(instr, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			LoadArrayInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, instr, instr.getAccessedArrayOperand());
		return LabeledSingleResult.createResult(putSingletonLabel(instr, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			LoadFieldInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
		
		/*
		 * Is the field of 'this' ?
		 */
		ThisVariable receiver = getAnalysisContext().getThisVariable();
		if(receiver != null) {
			Aliasing this_loc = value.get(receiver);
			if( instr.getSourceObject().isUnqualifiedSuper() || value.get(instr.getSourceObject()).equals(this_loc) ) {
				final Variable field_var = new FieldVariable(instr.resolveFieldBinding());
				/*
				 * Just do a normal copy from field to temp.
				 */
				value.put(instr.getTarget(), value.get(field_var).copy());
				value = killAllDead(value, instr, instr.getAccessedObjectOperand());
				return LabeledSingleResult.createResult(value, labels);
			}
		}
		
		/*
		 * Just make up a new label. We aren't yet handling fields of other objects.
		 */
		value = killAllDead(value, instr, instr.getAccessedObjectOperand());
		return LabeledSingleResult.createResult(putSingletonLabel(instr, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			MethodCallInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, instr, instr.getReceiverOperand());
		value = killAllDead(value, instr, instr.getArgOperands());
		return LabeledSingleResult.createResult(putSingletonLabel(
				instr, instr.isStaticMethodCall() ? null : instr.getReceiverOperand(), value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			NewArrayInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
//		if(instr.isInitialized())
//			value = killAllDead(value, instr, instr.getInitOperand());
//		else
//			value = killAllDead(value, instr, instr.getDimensionOperands());
		return LabeledSingleResult.createResult(putSingletonLabel(instr, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			NewObjectInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, instr, instr.getArgOperands());
		return LabeledSingleResult.createResult(putSingletonLabel(instr, null, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			UnaryOperation unop, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, unop, unop.getOperand());
		return LabeledSingleResult.createResult(putSingletonLabel(unop, value), labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			final SourceVariableDeclaration instr, List<ILabel> labels,
			AliasingLE value) {
		// treat known-initialized variables special:
		if (instr.isFormalParameter() || 
				// Nels put in the extra condition || instr.isCaughtVariable()
				// so it could be totally busted.
				instr.isCaughtVariable() ||
				// Kevin put in extra condition for enhanced for parameter, not sure if it's right 
				instr.getNode().getParent() instanceof EnhancedForStatement) {
			value = value.mutableCopy();
			ObjectLabel label = labelContext.get(instr.getDeclaredVariable());
			if(label == null) {
				label = new ObjectLabel() {
					public ITypeBinding getType() {
						return instr.resolveBinding().getType();
					}
					public boolean isSummary() {
						return !instr.isFormalParameter() && loopCounter.isInLoop(instr.getNode());
					}
					@Override public String toString() {
						String prefix;
						if(instr.isCaughtVariable())
							prefix = "exn.";
						else if(instr.isFormalParameter())
							prefix = "param.";
						else
							prefix = "foreach.";
						return prefix + instr.getDeclaredVariable().getSourceString();
					}
				};
				labelContext.put(instr.getDeclaredVariable(), label);
			}
			AliasLE aliases = AliasLE.create(label);
			value.put(instr.getDeclaredVariable(), aliases);
		}
		// otherwise local --> will get locations through initializer / assignment
		return LabeledSingleResult.createResult(value, labels);
	}

	@Override
	public IResult<AliasingLE> transfer(
			StoreFieldInstruction instr, List<ILabel> labels,
			AliasingLE value) {
		value = value.mutableCopy();
		/*
		 * Is the field of 'this' ?
		 */
		ThisVariable receiver = getAnalysisContext().getThisVariable();
		if(receiver != null) {
			Aliasing this_loc = value.get(receiver);
			if( instr.getDestinationObject().isUnqualifiedSuper() || value.get(instr.getDestinationObject()).equals(this_loc) ) {
				final Variable field_var = new FieldVariable(instr.resolveFieldBinding());
				value.put(field_var, value.get(instr.getSourceOperand()).copy());
			}
		}
		// otherwise ignore
		value = killAllDead(value, instr, instr.getAccessedObjectOperand(), instr.getSourceOperand());
		return LabeledSingleResult.createResult(value, labels);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction#transfer(edu.cmu.cs.crystal.tac.ConstructorCallInstruction, java.util.List, edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	@Override
	public IResult<AliasingLE> transfer(ConstructorCallInstruction instr,
			List<ILabel> labels, AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, instr, instr.getArgOperands()); // construction object never dead
		return LabeledSingleResult.createResult(value, labels);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction#transfer(edu.cmu.cs.crystal.tac.EnhancedForConditionInstruction, java.util.List, edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	@Override
	public IResult<AliasingLE> transfer(EnhancedForConditionInstruction instr,
			List<ILabel> labels, AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, instr, instr.getIteratedOperand());
		return LabeledSingleResult.createResult(value, labels);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction#transfer(edu.cmu.cs.crystal.tac.SourceVariableRead, java.util.List, edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	@Override
	public IResult<AliasingLE> transfer(SourceVariableRead instr,
			List<ILabel> labels, AliasingLE value) {
		// no kills and no new locations
		return super.transfer(instr, labels, value);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction#transfer(edu.cmu.cs.crystal.tac.StoreArrayInstruction, java.util.List, edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	@Override
	public IResult<AliasingLE> transfer(StoreArrayInstruction instr,
			List<ILabel> labels, AliasingLE value) {
		value = value.mutableCopy();
		value = killAllDead(value, instr, instr.getAccessedArrayOperand(), instr.getSourceOperand());
		return LabeledSingleResult.createResult(value, labels);
	}
}
