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
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.contexts.TensorContext;
import edu.cmu.cs.plural.errors.PackingResult;
import edu.cmu.cs.plural.states.StateSpaceRepository;

/**
 * @author Kevin Bierhoff
 * @since 4/15/2008
 */
public interface PluralLatticeElement {

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

	public boolean isBottom();

	/**
	 * Pack the receiver to the given state with suitable defaults for the
	 * rest of the permission details.
	 * Does nothing if receiver is already packed.
	 * @param curContext TODO
	 * @param stateInfo
	 * @return A result indicating whether or not the pack was succesful.
	 */
	public PackingResult packReceiver(TensorContext curContext,
			Variable rcvrVar, StateSpaceRepository stateRepo,
			SimpleMap<Variable, Aliasing> locs, Set<String> desiredState);

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
}