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
package edu.cmu.cs.plural.contexts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.model.SourceVariable;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.errors.ChoiceID;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.fractions.VirtualFramePermissionSet;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.pred.PredicateMerger.MergeIntoTuple;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.FractionAnalysisContext;

/**
 * This class provides static methods for creating initial lattice elements.
 * <ul>
 * <li> {@link #createInitialMethodLE(PredicateMerger, ITACAnalysisContext, FractionAnalysisContext)}
 * for analyzing static and instance method bodies.
 * <li> {@link #createInitialConstructorLE(PredicateMerger, ITACAnalysisContext, FractionAnalysisContext)}
 * for analyzing constructor bodies.
 * </ul>
 * @author Kevin Bierhoff
 * @since Sep 16, 2008
 */
public abstract class InitialLECreator implements MergeIntoTuple {
	
	/**
	 * Creates an initial lattice element for analyzing a 
	 * static or instance method, but <b>not</b> a constructor.
	 * Additionally, the method creates a map from parameter names (including the receiver) 
	 * to their initial locations, which is useful for checking post-conditions for
	 * these locations.
	 * @param pre
	 * @param tacContext
	 * @param fractContext
	 * @return an initial lattice element for analyzing a 
	 * static or instance method plus
	 * a map from parameter names (including the receiver, if defined) 
	 * to their initial locations.
	 * @see #createInitialConstructorLE(PredicateMerger, ITACAnalysisContext, FractionAnalysisContext)
	 */
	public static Pair<LinearContext, SimpleMap<String, Aliasing>> createInitialMethodLE(
			PredicateMerger pre, 
			ITACAnalysisContext tacContext,
			FractionAnalysisContext fractContext) {
		// invoked directly for static and instance methods
		// invoked from createInitialConstructorLE if the constructor calls this(...)
		final TensorPluralTupleLE tuple =
			new TensorPluralTupleLE(
					FractionalPermissions.createEmpty(), // use top (no permissions) as default
					fractContext);
		tuple.storeInitialAliasingInfo(tacContext.getAnalyzedMethod());
		
		if(fractContext.assumeVirtualFrame()) {
			final ThisVariable receiverVar = tacContext.getThisVariable();
			// shouldn't be null, since we're asked to assume a virtual frame for "this"
			assert receiverVar.isUnqualifiedThis();
			Aliasing this_loc = tuple.getLocations(receiverVar);
			// virtual and frame permissions are the same for analyzing virtual receiver frame
			tuple.put(this_loc, VirtualFramePermissionSet.createEmpty());
		}
		else {
			// default takes care of this
		}
		
		InitialLECreator c = new InitialMethodLECreator(tuple);
		
		final SimpleMap<String, Aliasing> vars = createStartMap(tacContext, tuple);
		pre.mergeInPredicate(vars, c);
		if(c.isVoid())
			// void can prove anything: start with false
			// New choice ids for both the parent and the new context, when we are creating an LE
			return Pair.create(ContextFactory.falseContext(ChoiceID.choiceID(), ChoiceID.choiceID()), vars);
		
		if(!Modifier.isStatic(tacContext.getAnalyzedMethod().getModifiers())) {
			// issue #61: make the receiver non-null
			Aliasing t = vars.get("this");
			assert t != null;
			tuple.addNonNullVariable(t);
		}

		// New choice ids for both the parent and the new context, when we are creating an LE
		return Pair.create(ContextFactory.tensor(tuple, ChoiceID.choiceID(), ChoiceID.choiceID()), vars);
	}
	
	/**
	 * Creates an initial lattice element for analyzing a constructor.
	 * <b>In addition</b> to the given pre-condition, it inserts a
	 * unpacked unique frame permission for the receiver into the lattice.
	 * Additionally, the method creates a map from parameter names (including the receiver) 
	 * to their initial locations, which is useful for checking post-conditions for
	 * these locations.
	 * @param pre
	 * @param tacContext
	 * @param fractContext
	 * @param callsThisConstructor pass <code>true</code> if the constructor for which we're
	 * constructing initial lattice information calls another constructor in the same class using 
	 * <code>this(...)</code>; <code>false</code> if the constructor 
	 * (implicitly or explicitly) invokes a super-constructor. 
	 * @return an initial lattice element for analyzing a constructor plus
	 * a map from parameter names (including the receiver) to their initial locations.
	 * @see #createInitialMethodLE(PredicateMerger, ITACAnalysisContext, FractionAnalysisContext)
	 */
	public static Pair<LinearContext, SimpleMap<String, Aliasing>> createInitialConstructorLE(
			PredicateMerger pre, 
			ITACAnalysisContext tacContext,
			FractionAnalysisContext fractContext, 
			MethodDeclaration decl, boolean callsThisConstructor) {
		assert tacContext.getAnalyzedMethod().isConstructor();
		if(callsThisConstructor)
			// treat like a regular method if this constructor contains a this(...) call:
			// the receiver permission will come from the invoked constructor,
			// and we don't want to start out unpacked in this case
			return createInitialMethodLE(pre, tacContext, fractContext);
		
		final ThisVariable receiverVar = tacContext.getThisVariable(); 
		assert receiverVar.isUnqualifiedThis();
		final TensorPluralTupleLE tuple =
		TensorPluralTupleLE.createUnpackedLattice(
				FractionalPermissions.createEmpty(), // use top (no permissions) as default
				fractContext,
				receiverVar,
				decl);
		tuple.storeInitialAliasingInfo(tacContext.getAnalyzedMethod());
		Aliasing this_loc = tuple.getLocations(receiverVar);
		
		if(fractContext.assumeVirtualFrame()) {
			// virtual and frame permissions are the same for analyzing virtual receiver frame
			tuple.put(this_loc, VirtualFramePermissionSet.createEmpty());
		}
		else {
			// default takes care of this
		}
		
		InitialLECreator c = new InitialConstructorLECreator(tuple);
		final SimpleMap<String, Aliasing> vars = createStartMap(tacContext, tuple);
		pre.mergeInPredicate(vars, c);
		if(c.isVoid())
			// void can prove anything: start with false
			// New choice ids for both the parent and the new context, when we are creating an LE
			return Pair.create(ContextFactory.falseContext(ChoiceID.choiceID(), ChoiceID.choiceID()), vars);
		
		addUnpackedReceiver(tuple, this_loc,
				fractContext.getRepository().getStateSpace(receiverVar.resolveType()));
		// issue #61: make the receiver non-null
		tuple.addNonNullVariable(this_loc);
		
		// New choice ids for both the parent and the new context, when we are creating an LE
		return Pair.create(ContextFactory.tensor(tuple, ChoiceID.choiceID(), ChoiceID.choiceID()), vars);
	}
	
	private static void addUnpackedReceiver(TensorPluralTupleLE value,
			Aliasing receiverVar, StateSpace receiverSpace) {
		// add an unpacked unique frame permission for alive
		PermissionSetFromAnnotations thisFrame = PermissionSetFromAnnotations.createSingleton( 
				PermissionFactory.INSTANCE.createUniqueOrphan(receiverSpace, receiverSpace.getRootState(), 
						true /* frame permission */, receiverSpace.getRootState()),
				true /* universals are named */);
		
		FractionalPermissions ps = value.get(receiverVar);
//		if(ps.isEmpty()) {
//			ps = thisFrame.toLatticeElement();
//		}
//		else {
			// there shouldn't already be frame permissions
			assert ps.getFramePermissions().isEmpty() :
				"Specification error: a constructor cannot require frame permissions; instead, it starts out with an unpacked unique frame permission"; 
			ps = ps.mergeIn(thisFrame);
//		}
		ps = ps.unpack(receiverSpace.getRootState());
		value.put(receiverVar, ps);
	}

	/**
	 * The returned map uses the given tuple's {@link TensorPluralTupleLE#getLocations(edu.cmu.cs.crystal.tac.Variable)}
	 * method to map variables to locations.  Thus, the map will only represent the
	 * parameter locations at the beginning of the method if the tuple 
	 * {@link TensorPluralTupleLE#storeInitialAliasingInfo(org.eclipse.jdt.core.dom.MethodDeclaration)
	 * stores the initial aliasing information}.
	 * @param tacContext
	 * @param tuple
	 * @return
	 */
	private static SimpleMap<String, Aliasing> createStartMap(
			ITACAnalysisContext tacContext, TensorPluralTupleLE tuple) {
		// cache parameter locations in a map
		final List<SingleVariableDeclaration> params = tacContext.getAnalyzedMethod().parameters();
		final Map<String, Aliasing> paramMap = 
			new HashMap<String, Aliasing>(params.size());
		for(int i = 0; i < params.size(); i++) {
			SingleVariableDeclaration paramDecl = params.get(i);
			SourceVariable paramVar = tacContext.getSourceVariable(paramDecl.resolveBinding());
			// alias analysis has locations for parameters only after transferring over their declarations
			Aliasing param_loc = tuple.getLocationsAfter(paramDecl, paramVar);
			paramMap.put("#" + i, param_loc);
		}
		
		// receiver locations, if not static method
		if(!Modifier.isStatic(tacContext.getAnalyzedMethod().getModifiers())) {
			Aliasing this_loc = tuple.getLocations(tacContext.getThisVariable());
			paramMap.put("this", this_loc);
			paramMap.put("this!fr", this_loc);
		}
		
		return new SimpleMap<String, Aliasing>() {
			@Override
			public Aliasing get(String key) {
				Aliasing result = paramMap.get(key);
				assert result != null : "Parameter unknown: " + key;
				return result;
			}
		};		
	}
	
	protected static class InitialMethodLECreator extends InitialLECreator {
		InitialMethodLECreator(TensorPluralTupleLE value) {
			super(value);
		}

		@Override
		public void finishMerge() {
			// nothing further
		}
	}
	
	protected static class InitialConstructorLECreator extends InitialLECreator {
		InitialConstructorLECreator(TensorPluralTupleLE value) {
			super(value);
		}

		@Override
		public void finishMerge() {
			// nothing further
		}
	}
	
	protected TensorPluralTupleLE value;
	private boolean isVoid;
	
	protected InitialLECreator(TensorPluralTupleLE value) {
		this.value = value;
	}
	
	public boolean isVoid() {
		return isVoid;
	}

	@Override
	public void addFalse(Aliasing var, String var_name) {
		value.addFalseVarPredicate(var);
	}

	@Override
	public void addImplication(Aliasing var, Implication implication) {
		value.addImplication(var, implication);
	}

	@Override
	public void addNonNull(Aliasing var, String var_name) {
		value.addNonNullVariable(var);
	}

	@Override
	public void addNull(Aliasing var, String var_name) {
		value.addNullVariable(var);
	}

	@Override
	public void addStateInfo(Aliasing var, String var_name, Set<String> stateInfos,
			boolean inFrame) {
		FractionalPermissions ps = value.get(var);
		for(String s : stateInfos)
			ps = ps.learnTemporaryStateInfo(s, inFrame);
		value.put(var, ps);
	}

	@Override
	public void addTrue(Aliasing var, String var_name) {
		value.addTrueVarPredicate(var);
	}

	@Override
	public void addVoid() {
		isVoid = true;
	}

	@Override
	public void mergeInPermission(Aliasing var, String var_name,
			PermissionSetFromAnnotations perms) {
		FractionalPermissions ps = value.get(var);
		if(ps.isEmpty())
			ps = ps.createReplacement(perms);
		else
			ps = ps.mergeIn(perms);
		value.put(var, ps);
	}

	@Override
	public void releaseParameter(Aliasing var, String param) {
		throw new IllegalStateException("Shouldn't be used for a pre-condition");
	}


}
