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

package edu.cmu.cs.plural.polymorphic.instantiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.AbstractingTransferFunction;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.crystal.tac.eclipse.CompilationUnitTACs;
import edu.cmu.cs.crystal.tac.eclipse.EclipseTAC;
import edu.cmu.cs.crystal.tac.model.CastInstruction;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.ReturnInstruction;
import edu.cmu.cs.crystal.tac.model.SourceVariableDeclaration;
import edu.cmu.cs.crystal.tac.model.SourceVariableReadInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Box;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.plural.annot.Apply;

/**
 * The instantiated type analysis will tell you the instantiated
 * type of any field or local variable. In the case of local variable,
 * it may end up performing an analysis (a very weak inference) to 
 * accomplish this goal, since we can only annotate variable declarations
 * and yet the method calls and constructor instances are what really matter.
 * 
 * @author Nels E. Beckman
 * @since Nov 12, 2009
 */
public final class InstantiatedTypeAnalysis {
	final private CompilationUnitTACs compilationUnit;
	
	public InstantiatedTypeAnalysis(CompilationUnitTACs compilationUnit) {
		this.compilationUnit = compilationUnit;
	}
}

final class InstantiatedTypeTransferFun extends 
AbstractingTransferFunction<TupleLatticeElement<Variable,InstTypeLE>> {

	private final TupleLatticeOperations<Variable,InstTypeLE> ops = 
		new TupleLatticeOperations<Variable,InstTypeLE>(new InstantiatedTypeLEOps(), InstTypeLE.TOP);
	
	private final EclipseTAC methodTAC; 
	private final AnnotationDatabase annoDB;
	
	public InstantiatedTypeTransferFun(EclipseTAC methodTAC, AnnotationDatabase annoDB) {
		this.methodTAC = methodTAC;
		this.annoDB = annoDB;
	}

	@Override
	public TupleLatticeElement<Variable,InstTypeLE> createEntryValue(MethodDeclaration method) {
		// Incoming lattice will have types for parameters. Types for 
		// fields will be loaded on demand, when a field is loaded. 
		TupleLatticeElement<Variable,InstTypeLE> result = ops.getDefault();
		AnnotationSummary summary = annoDB.getSummaryForMethod(method.resolveBinding());
		
		int param_num = 0;
		for( Object param_ : method.parameters() ) {
			SingleVariableDeclaration param = (SingleVariableDeclaration)param_;
			ApplyAnnotationWrapper anno = (ApplyAnnotationWrapper)summary.getParameter(param_num, Apply.class.getName());
			Variable param_var = this.methodTAC.sourceVariable(param.resolveBinding());

			InstTypeLE new_le = anno == null ? InstTypeLE.NONE : new InstTypeLE(anno.getValue());
			result.put(param_var, new_le);
			param_num++;
		}
		
		return result;
	}

	@Override
	public ILatticeOperations<TupleLatticeElement<Variable,InstTypeLE>> getLatticeOperations() {
		return ops;
	}

	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			CastInstruction instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		value.put(instr.getTarget(), value.get(instr.getOperand()));
		return super.transfer(instr, value);
	}

	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			CopyInstruction instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		value.put(instr.getTarget(), value.get(instr.getOperand()));
		return super.transfer(instr, value);
	}

	/** For a field look up the annotations and return the APPLY annotation if there
	 *  is one. */
	private Option<ApplyAnnotationWrapper> toApplyAnnotation(IVariableBinding binding) {
		for( ICrystalAnnotation anno : annoDB.getAnnosForVariable(binding) ) {
			if( anno instanceof ApplyAnnotationWrapper ) {
				ApplyAnnotationWrapper anno_ = (ApplyAnnotationWrapper)anno;
				return Option.some(anno_);
			}
		}
		return Option.none();
	}
	
	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			LoadFieldInstruction instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		// Look up type of field, from annotation...
		Option<ApplyAnnotationWrapper> anno_ = toApplyAnnotation(instr.resolveFieldBinding());
		if( anno_.isSome() ) {
			ApplyAnnotationWrapper anno = anno_.unwrap();
			InstTypeLE new_target_le = new InstTypeLE(anno.getValue());
			InstTypeLE new_source_le = new_target_le;
			value.put(instr.getSourceObject(), new_source_le);
			value.put(instr.getTarget(), new_target_le);
		}
		else {
			value.put(instr.getSourceObject(), InstTypeLE.NONE);
			value.put(instr.getTarget(), InstTypeLE.NONE);
		}
		return super.transfer(instr, value);
	}

	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			NewObjectInstruction instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		// Put in an empty list. We expect it to be modified when this
		// is assigned to a local variable.
		value.put(instr.getTarget(), new InstTypeLE(Collections.<String>emptyList()));
		return super.transfer(instr, value);
	}
	
	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			SourceVariableDeclaration instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		Option<ApplyAnnotationWrapper> anno_ = toApplyAnnotation(instr.resolveBinding());
		if( anno_.isSome() ) {
			ApplyAnnotationWrapper anno = anno_.unwrap();
			value.put(instr.getDeclaredVariable(), new InstTypeLE(anno.getValue()));
		}
		else {
			value.put(instr.getDeclaredVariable(), InstTypeLE.NONE);
		}
		return super.transfer(instr, value);
	}
	
	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			MethodCallInstruction instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, value);
	}

	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			ReturnInstruction instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, value);
	}

	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			SourceVariableReadInstruction instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, value);
	}

	@Override
	public TupleLatticeElement<Variable, InstTypeLE> transfer(
			StoreFieldInstruction instr,
			TupleLatticeElement<Variable, InstTypeLE> value) {
		// TODO Auto-generated method stub
		return super.transfer(instr, value);
	}
	
	
}

final class InstTypeLE {
	// TODO: This will almost certainly change from String when we have 
	// non polyVar types that we can instantiate.
	private final Box<List<String>> instantiated;

	public static final InstTypeLE TOP = new InstTypeLE(Collections.<String>emptyList());
	public static final InstTypeLE BOTTOM = new InstTypeLE(Collections.<String>emptyList());
	public static final InstTypeLE NONE = new InstTypeLE(Collections.<String>emptyList());
	
	public InstTypeLE(List<String> instantiated) {
		this.instantiated = Box.<List<String>>box(new ArrayList<String>(instantiated));
	}

	public InstTypeLE(InstTypeLE original) {
		this.instantiated = original.instantiated;
	}

	/** Used to change the value of the instantiated permissions. This will have the
	 *  EFFECT of modifying all the istantiated types for variables that are equal
	 *  to this one. */
	public void changeValue(List<String> instantiated) {
		this.instantiated.setValue(new ArrayList<String>(instantiated));
	}
	
	/*
	 * I am pretty sure that it's okay if the equals and hashCode  methods have
	 * value semantics even though the object inside the box will be changed.
	 */
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((instantiated == null) ? 0 : instantiated.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstTypeLE other = (InstTypeLE) obj;
		if (instantiated == null) {
			if (other.instantiated != null)
				return false;
		} else if (!instantiated.equals(other.instantiated))
			return false;
		return true;
	}
}

final class InstantiatedTypeLEOps implements ILatticeOperations<InstTypeLE> {

	@Override
	public boolean atLeastAsPrecise(InstTypeLE info, InstTypeLE reference,
			ASTNode node) {
		if( info == InstTypeLE.BOTTOM )
			return true;
		else if( reference == InstTypeLE.TOP )
			return true;
		else if( info.equals(reference) )
			return true;
		else
			return false;
	}

	@Override public InstTypeLE bottom() {return InstTypeLE.BOTTOM;}

	@Override
	public InstTypeLE copy(InstTypeLE original) {
		if( original == InstTypeLE.BOTTOM || 
		    original == InstTypeLE.TOP || 
		    original == InstTypeLE.NONE ) {
			return original;
		}
		else
			return new InstTypeLE(original);
	}

	@Override
	public InstTypeLE join(InstTypeLE someInfo, InstTypeLE otherInfo,
			ASTNode node) {
		if( someInfo.equals(otherInfo) )
			return someInfo;
		else if( someInfo == InstTypeLE.TOP || otherInfo == InstTypeLE.TOP )
			return InstTypeLE.TOP;
		else if( someInfo == InstTypeLE.BOTTOM )
			return otherInfo;
		else if( otherInfo == InstTypeLE.BOTTOM )
			return someInfo;
		else
			return InstTypeLE.TOP;
	}
}