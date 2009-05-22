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

package edu.cmu.cs.plural.linear;

import static edu.cmu.cs.crystal.util.ConsList.cons;
import static edu.cmu.cs.crystal.util.ConsList.empty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.util.ConsList;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.contexts.TensorPluralTupleLE;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.fractions.VirtualFramePermissionSet;
import edu.cmu.cs.plural.pred.PredicateChecker.SplitOffTuple;

/**
 * Abstract base class for checking / splitting a predicate in
 * a {@link TensorPluralTupleLE} tuple.  It implements the callbacks
 * by calling the appropriate methods on the tuple.  
 * 
 * Accesses to
 * the (possibly unpacked) receiver frame are and logged in two fields.
 * Subclasses must implement {@link #finishSplit()} to pack and
 * check any states and split any predicates needed for the receiver frame.
 * Subclasses must also implement {@link #announceBorrowed(Set)} to take
 * borrowing into account where desired.
 * 
 * This implementation is <i>fail-fast</i>: the checker methods immediately
 * return <code>false</code> when any errors are detected.  They must be
 * overridden to change this behavior.  Care must be taken to ensure that
 * failed checks are caught eventually; also, permissions must still be split
 * (in {@link #splitOffInternal(Aliasing, String, TensorPluralTupleLE, PermissionSetFromAnnotations)})
 * even if splitting failures should not be detected.   
 *  
 * @author Kevin Bierhoff
 * @since Sep 15, 2008
 */
public abstract class AbstractPredicateChecker implements SplitOffTuple {
	
	// given
	protected final TensorPluralTupleLE value;
	protected final Aliasing this_loc;
	// constructed
	private ConsList<PermissionSetFromAnnotations> splitFromThis = empty();
	private final Set<String> neededReceiverStates = new HashSet<String>();
	private final boolean delayVirtualReceiverPermissions;
	
	public AbstractPredicateChecker(TensorPluralTupleLE value, Aliasing thisLoc) {
		this.value = value;
		this.this_loc = thisLoc;
		if(this_loc != null /* && ! this_loc.getLabels().isEmpty()*/)
			// this is a hack to determine whether we're treating virtual as frame permissions
			// we're just looking whether receiver permissions are tracked with special permission set implementation
			// TODO clean way of determining whether virtual receiver permissions need to be delayed
			this.delayVirtualReceiverPermissions = 
				(value.get(thisLoc) instanceof VirtualFramePermissionSet);
		else
			this.delayVirtualReceiverPermissions = false;
	}
	
	@Override
	public boolean checkStateInfo(Aliasing var, String var_name, Set<String> stateInfo, boolean inFrame) {
		if((delayVirtualReceiverPermissions || inFrame) && var.equals(this_loc)) {
			neededReceiverStates.addAll(stateInfo);
			return true;
		}
		else {
			return checkStateInfoInternal(var, stateInfo, inFrame);
		}
	}

	/**
	 * Override this method to manipulate checking the given object
	 * for the given states.
	 * @param var
	 * @param stateInfo
	 * @param inFrame
	 * @return
	 */
	protected boolean checkStateInfoInternal(Aliasing var,
			Set<String> stateInfo, boolean inFrame) {
		return value.get(var).isInStates(stateInfo, inFrame);
	}
	
	@Override
	public boolean checkImplication(Aliasing var, Implication impl) {
		
		if( value.isKnownImplication(var, impl) ) {
			// TODO: We only have to remove this if the implication is linear!
			// Need a new subclass of implcation, with isLinear method.
			value.removeImplication(var, impl);
			return true;
		}
		else if( impl.getAntecedant().isUnsatisfiable(value) ) {
			return true;
		}
		else {
			// Assuming the antecedant,
			impl.getAntecedant().putIntoLattice(value);
			if( impl.result().splitOffResult(this) ) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	@Override
	public boolean splitOffPermission(Aliasing var, String var_name,
			PermissionSetFromAnnotations perms) {
		if(this_loc != null && this_loc.equals(var)) {
			// defer...
			if(delayVirtualReceiverPermissions) {
				// remember the given permissions for later splitting
				splitFromThis = cons(perms, splitFromThis);
				neededReceiverStates.addAll(perms.getStateInfo(false));
				neededReceiverStates.addAll(perms.getStateInfo(true));
				// return since no splitting happens now
				return true;
			}
			else {
				// remember only frame permissions for later and split virtual permissions now
				Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> virtualAndFrame =
					PermissionSetFromAnnotations.splitPermissionSets(perms);
				// remember frame permissions for later splitting
				splitFromThis = cons(virtualAndFrame.snd(), splitFromThis);
				neededReceiverStates.addAll(virtualAndFrame.snd().getStateInfo(true));
				// split the virtual part below
				perms = virtualAndFrame.fst(); 
			}
		}
		// split virtual permissions
		return splitOffInternal(var, var_name, value, perms);
	}

	/**
	 * Override this method to manipulate how permissions are split from the given object.
	 * @param var
	 * @param var_name Formal parameter name for <code>var</code> or <code>null</code>
	 * if unknown
	 * @param value
	 * @param perms
	 * @return
	 */
	protected boolean splitOffInternal(Aliasing var, String var_name, TensorPluralTupleLE value, PermissionSetFromAnnotations perms) {
		if(!value.get(var).isInStates(perms.getStateInfoPair()))
			return false;
		
		// split off permission 
		value = LinearOperations.splitOff(var, value, perms);
		
		return ! value.get(var).isUnsatisfiable();
	}

	@Override
	public boolean checkFalse(Aliasing var, String var_name) {
		return value.isBooleanFalse(var);
	}

	@Override
	public boolean checkNonNull(Aliasing var, String var_name) {
		return value.isNonNull(var);
	}

	@Override
	public boolean checkNull(Aliasing var, String var_name) {
		return value.isNull(var);
	}

	@Override
	public boolean checkTrue(Aliasing var, String var_name) {
		return value.isBooleanTrue(var);
	}

	protected Set<String> getNeededReceiverStates() {
		return neededReceiverStates;
	}
	
	protected List<PermissionSetFromAnnotations> getSplitFromThis() {
		return splitFromThis;
	}
	
}