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

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.ThisVariable;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.crystal.util.ExtendedIterator;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.states.StateSpaceRepository;

/**
 * @author Kevin Bierhoff
 * @since 4/15/2008
 */
public interface PluralLatticeElement {

//	public boolean isNull(Aliasing loc);
//
//	public boolean isNonNull(Aliasing loc);
//
//	public boolean isBooleanTrue(Aliasing var);
//
//	public boolean isBooleanFalse(Aliasing var);
//
//	public void addFalseImplication(Aliasing ant, Aliasing object, String state);
//
//	public void addFalseVarPredicate(Aliasing v);
//
//	public void addNonNullVariable(Aliasing var);
//
//	public void addNullVariable(Aliasing var);
//
//	public void addNullImplication(Aliasing v_1, boolean is_v1_true,
//			Aliasing v_2, boolean is_v2_null);
//
//	public void addTrueImplication(Aliasing ant, Aliasing object, String state);
//
//	public void addTrueVarPredicate(Aliasing v);
//
//	public void addEquality(Aliasing v_1, Aliasing v_2);
//
//	public void addInequality(Aliasing v_1, Aliasing v_2);
//
//	public List<ImplicationResult> solve();
//
//	public List<ImplicationResult> solveWithHint(Aliasing v);
//
//	public List<ImplicationResult> solveWithHints(Aliasing... vs);

	/**
	 * Calling this method signals to the object that it is a good time
	 * to record the current aliasing information. The object will store
	 * that information. Note that this method, for convenience only, returns
	 * 'this.' This is the mutated object.
	 * 
	 * @param ASTNode The current location where we are in the AST, needed
	 *                to find the 'current' aliasing information.
	 * @return <code>this</code>, the mutated object, for convenience.
	 */
	public PluralTupleLatticeElement storeCurrentAliasingInfo(ASTNode n);

	/**
	 * Store the locations at the very beginning.
	 * 
	 * @param decl
	 * @return
	 * 
	 * @see edu.cmu.cs.plural.track.PluralTupleLatticeElement.storeCurrentAliasingInfo
	 */
	public PluralTupleLatticeElement storeInitialAliasingInfo(
			MethodDeclaration decl);

//	public void put(TACInstruction<?> instr, Variable x, FractionalPermissions l);
//
//	public FractionalPermissions get(Aliasing objects);
//
//	public FractionalPermissions get(TACInstruction<?> instr, Variable x);
//
//	public void put(Aliasing a, FractionalPermissions l);
//
//	public FractionalPermissions get(ASTNode n, Variable x);
//
//	public void put(ASTNode n, Variable x, FractionalPermissions l);
//
//	public PluralTupleLatticeElement bottom();
//
//	public ExtendedIterator<FractionalPermissions> tupleInfoIterator();

	public boolean isBottom();

//	public Aliasing getLocationsAfter(ASTNode n, Variable x);
//
//	public Aliasing getLocationsBefore(ASTNode n, Variable x);

//	public boolean isRcvrPacked();

	/**
	 * Pack the receiver to the given state with suitable defaults for the
	 * rest of the permission details.
	 * Does nothing if receiver is already packed.
	 * @param stateInfo
	 * @return Did we successfully pack?
	 */
	public boolean packReceiver(Variable rcvrVar,
			StateSpaceRepository stateRepo, SimpleMap<Variable, Aliasing> locs,
			Set<String> desiredState);

	/**
	 * This method will attempt to pack the receiver to a suitably 'good' permission/state,
	 * for some definition of good.<br>
	 * <br>
	 * We will try to do the following:<br>
	 * 1.) Pack to suggested states (statesToTry)<br>
	 * 2.) Pack to the state that was unpacked<br>
	 * 3.) Pack to the root<br>
	 * 5.) ??<br>
	 * 6.) Fail<br>
	 * 7.) Profit<br>
	 * 
	 * @param thisVariable
	 * @param repository
	 * @param node
	 * @param statesToTry States worth trying to pack to (good ideas are pre and post conditions)
	 * @return Did we successfully pack?
	 */
	public boolean packReceiverToBestGuess(Variable rcvrVar,
			StateSpaceRepository stateRepo, SimpleMap<Variable, Aliasing> locs,
			String... statesToTry);

	/** 
	 * Unpacks the receiver, removing any permission to 'this,' and replacing
	 * it with permission for the fields that are implied by the current state
	 * invariant. Takes no action if receiver is already unpacked. Needs a
	 * state space repository so that it can create field permissions with
	 * legitimate state spaces.
	 * @param nodeWhereUnpacked TODO
	 * @param ThisVariable The receiver variable.
	 * @param StateSpaceRepository Gives us the possible states for a field type.
	 * @param ASTNode Node that will be used for BEFORE aliasing results. 
	 */
	public boolean unpackReceiver(Variable rcvrVar,
			ASTNode nodeWhereUnpacked, StateSpaceRepository stateRepo,
			SimpleMap<Variable, Aliasing> locs, String rcvrRoot, String assignedField);

//	public boolean maybeFieldAccess(TACInstruction<?> instr, Variable x);
//
//	public Aliasing getEndLocations(Variable var, MethodDeclaration d);
//
//	public Aliasing getStartLocations(Variable var, MethodDeclaration d);

}