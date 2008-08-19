/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.analysis.alias.ObjectLabel;
import edu.cmu.cs.crystal.flow.ITACFlowAnalysis;
import edu.cmu.cs.crystal.flow.LatticeElement;
import edu.cmu.cs.crystal.internal.Freezable;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.Variable;

/**
 * <b>Unfortunately, this implementation is broken.</b>  It turns out that
 * tracking lattice information separately for location sets is unsound
 * in the presence of unfortunate joins, as follows.
 * 
 * <pre>
 * x = ...;
 * y = ...;
 * if(*) {
 *     z = x;
 *     w = y;
 * } else {
 *     z = y;
 *     w = x;
 * }
 * z.close();
 * </pre>
 * 
 * In the previous code, z and w are associated with the same set of locations
 * after the if statement.  In this tuple lattice here, this would mean that 
 * the call on z updates the lattice information associated with z and with w,
 * making analyses erroneously believe that w is definitely closed where it 
 * should go to Top or something like that. 
 * 
 * This is an alias-aware tuple lattice implementation. It automatically
 * translates AST nodes and variables into sets of locations they may point to
 * and keeps aliasing information for overlapping sets consistent. Analysis
 * information can be retrieved with <code>get()</code> methods and updated
 * with <code>put()</code> methods. In case you need the aliasing information
 * yourself, the methods <code>getLocationsXxx()</code> let you find out what
 * locations a variable might point to before/after a given instruction.
 * 
 * This class implements a protocol to reduce copying overhead. When you call
 * {@link #freeze()}, or when it's copied or joined, the object freezes itself,
 * not allowing future changes. In order to change something, you have to call
 * {@link #mutableCopy()} and make the change on the copy. As a general rule, if
 * a transfer method wants to change the tuple it needs to call mutableCopy()
 * once, in the beginning, and make sure that that new object is used in the
 * result it returns. 
 * 
 * AliasAwareTupleLE is unfortunately not quite interface-compatible with
 * TupleLatticeElement: <code>get()</code> and <code>put()</code> take not
 * only the variable you're interested in, but also the TACInstruction for which
 * you're updating. This is so the thing can internally look up what the
 * aliasing analysis has to say about that instruction. It's a little
 * unpleasant, but that's the best way I could figure out so far how to do this.
 * Anyway, you're going to have to add the instruction to all these calls.
 * 
 * @author Kevin Bierhoff
 * @since Crystal 3.3.0
 * @see edu.cmu.cs.crystal.flow.TupleLatticeElement
 */
public class BrokenAliasAwareTupleLE<LE extends LatticeElement<LE>> implements
	Freezable<BrokenAliasAwareTupleLE<LE>>, MutableAliasAwareTuple<LE> {

	/**
	 * Creates an initial lattice element that will throw an exception if
	 * information about unknown variables is requested.
	 * 
	 * @param <LE>
	 * @return
	 */
	public static <LE extends LatticeElement<LE>> BrokenAliasAwareTupleLE<LE> create() {
		return new BrokenAliasAwareTupleLE<LE>(new TupleCallback<LE>() {
			public LE defaultResult(Aliasing a) {
				throw new IllegalStateException(
						"Information for unknown alias set requested.");
			}
		});
	}

	/**
	 * Creates an initial lattice element that will return the given bottom
	 * value if information about unknown variables is requested.
	 * 
	 * @param <LE>
	 * @return
	 */
	public static <LE extends LatticeElement<LE>> BrokenAliasAwareTupleLE<LE> create(
			final LE bottom) {
		return new BrokenAliasAwareTupleLE<LE>(new TupleCallback<LE>() {
			public LE defaultResult(Aliasing a) {
				return bottom;
			}
		});
	}

	/**
	 * Callback interface to customize AliasAwareTupleLE's behavior in case of
	 * missing values. This method will be called for aliasing sets for which no
	 * analysis information is available or derivable.
	 * 
	 * @author Kevin Bierhoff
	 * 
	 * @param <LE>
	 */
	public interface TupleCallback<LE extends LatticeElement<LE>> {

		/**
		 * This method should return a valid analysis information object about
		 * the given aliasing set.
		 * 
		 * @param a
		 * @return A valid non-<code>null</code> analysis information object
		 *         about the given aliasing set.
		 */
		LE defaultResult(Aliasing a);

	}

	/** Local aliasing analysis information. */
	private final ITACFlowAnalysis<AliasingLE> aliasing;

	/** The actual analysis information this tuple keeps track of. */
	private final Map<Aliasing, LE> info;

	/** Callback interface to customize the tuple's behavior. */
	private final TupleCallback<LE> callback;

	/**
	 * Aliasing sets that have been checked to be at least as precise as their
	 * subsets. This set helps making the tuple implementation more efficient
	 * and, when in doubt, should be cleared whenever aliasing information is
	 * modified.
	 */
	private final Set<Aliasing> checked;

	/**
	 * Flag indicating whether the tuple was frozen. Once frozen, a tuple must
	 * not be modified ever again.
	 */
	private boolean frozen;

	/**
	 * This map contains fields of 'this' and the labels they are associated with.
	 * This map is actually built up by the this.aliasing flow analysis, and we
	 * just hold reference to it so that we will have the mappings at a later point.
	 */
	private final Map<IVariableBinding, Variable> receiverFields;
	
	/**
	 * Creates an initial lattice element that will invoke the given callback
	 * interface where needed.
	 * 
	 * @param callback
	 */
	public BrokenAliasAwareTupleLE(TupleCallback<LE> callback) {
		Map<IVariableBinding, Variable> receiverFields = new HashMap<IVariableBinding, Variable>();
		this.aliasing = new TACFlowAnalysis<AliasingLE>(
				// pass modifiable map into LocalAliasTransfer
				new LocalAliasTransfer(null, receiverFields));
		this.info = new HashMap<Aliasing, LE>();
		this.checked = new HashSet<Aliasing>();
		this.callback = callback;
		// make this's field a unmodifiable view
		this.receiverFields = receiverFields;
	}

	/**
	 * Internal constructor to create lattice elements based on an existing one.
	 * 
	 * @param aliasing
	 * @param info
	 * @param callback
	 * @param receiverFields 
	 */
	private BrokenAliasAwareTupleLE(
			ITACFlowAnalysis<AliasingLE> aliasing,
			Map<Aliasing, LE> info, TupleCallback<LE> callback,
			Map<IVariableBinding, Variable> receiverFields) {
		this.aliasing = aliasing;
		this.info = info;
		this.checked = new HashSet<Aliasing>();
		this.callback = callback;
		this.receiverFields = receiverFields; 
	}

	/**
	 * Returns analysis information for the given variable using aliasing
	 * information after the given node was evaluated.
	 * 
	 * @param n
	 * @param x
	 * @return Analysis information for the given node after the node was
	 *         evaluated.
	 */
	public LE get(ASTNode n, Variable x) {
		return get(getLocations(n, x));
	}

	/**
	 * Returns analysis information for the given variable using
	 * aliasing information after the given instruction was evaluated.
	 * 
	 * @param instr
	 * @param x
	 * @return Analysis information for the given variable before the given
	 *         instruction was evaluated.
	 */
	public LE get(TACInstruction instr, Variable x) {
		return get(getLocations(instr, x));
	}

	/**
	 * Returns analysis information for the given aliasing set.
	 * 
	 * @param objects
	 * @return Analysis information for the given aliasing set.
	 */
	public LE get(Aliasing objects) {
		if( this.isBottom() )
			return defaultResult(objects);
		
		LE result = info.get(objects);
		if (result == null)
			result = defaultResult(objects);
		else {
			LE derived = deriveInfoFromSubsets(objects);
			if (derived != null && derived.atLeastAsPrecise(result, null)) {
				result = derived;
				info.put(objects, derived);
			}
		}
		assert result != null;
		checked.add(objects);
		return result;
	}

	/**
	 * This method is called internally to determine initial lattice information
	 * for a given aliasing set. If no information can be derived from other
	 * aliasing sets then the callback is invoked. Derived information is stored
	 * in the lattice while information coming from a callback information is
	 * not.
	 * 
	 * @param a
	 * @return Initial lattice information for a given aliasing set
	 */
	private LE defaultResult(Aliasing a) {
		if( this.isBottom() )
			return callback.defaultResult(a);
			
		LE derived = deriveInfoFromSubsets(a);
		if (derived == null)
			return callback.defaultResult(a);
		return derived;
	}

	/**
	 * This method attempts to derive analysis information for a given aliasing
	 * set from information about its subsets.
	 * 
	 * @param a
	 * @return Derived analysis information or <code>null</code> if no
	 *         information about subsets is available.
	 */
	private LE deriveInfoFromSubsets(Aliasing a) {
		LE derived = null;
		Set<ObjectLabel> labels = a.getLabels();
		if (labels.size() > 1) {
			for (Aliasing ot : info.keySet()) {
				if (labels.containsAll(ot.getLabels())) {
					if (derived == null)
						derived = info.get(ot).copy();
					else
						derived = derived.copy()
								.join(info.get(ot).copy(), null);
				}
			}
		}
		return derived;
	}

	/**
	 * Returns the set of variables defined at the given instruction.  Calling 
	 * {@link #get(TACInstruction, Variable)} on the returned
	 * variables should return valid analysis information.
	 * @param instr
	 * @return Variables defined at the given instruction
	 */
	public Set<Variable> getVariables(TACInstruction instr) {
		return getVariablesAfter(instr);
	}
	
	/**
	 * Returns the set of variables defined at the given AST node.  Calling 
	 * {@link #get(ASTNode, Variable)} on the returned
	 * variables should return valid analysis information.
	 * @param node
	 * @return Variables defined at the given AST node.
	 */
	public Set<Variable> getVariables(ASTNode node) {
		return getVariablesAfter(node);
	}
	
	/**
	 * Returns the set of variables defined <i>before</i> the given instruction.  
	 * Calling {@link #get(TACInstruction, Variable)} on the returned
	 * variables should return valid analysis information.
	 * @param instr
	 * @return Variables defined before the given instruction
	 */
	public Set<Variable> getVariablesBefore(TACInstruction instr) {
		return aliasing.getResultsBefore(instr.getNode()).getKeySet();
	}
	
	/**
	 * Returns the set of variables defined <i>before</i> the given AST node.  
	 * Calling {@link #get(ASTNode, Variable)} on the returned
	 * variables should return valid analysis information.
	 * @param node
	 * @return Variables defined before the given AST node.
	 */
	public Set<Variable> getVariablesBefore(ASTNode node) {
		return aliasing.getResultsBefore(node).getKeySet();
	}
	
	/**
	 * Returns the set of variables defined <i>after</i> the given instruction.  
	 * Calling {@link #get(TACInstruction, Variable)} on the returned
	 * variables should return valid analysis information.
	 * @param instr
	 * @return Variables defined after the given instruction
	 */
	public Set<Variable> getVariablesAfter(TACInstruction instr) {
		return aliasing.getResultsAfter(instr.getNode()).getKeySet();
	}
	
	/**
	 * Returns the set of variables defined <i>after</i> the given AST node.  
	 * Calling {@link #get(ASTNode, Variable)} on the returned
	 * variables should return valid analysis information.
	 * @param node
	 * @return Variables defined after the given AST node.
	 */
	public Set<Variable> getVariablesAfter(ASTNode node) {
		return aliasing.getResultsAfter(node).getKeySet();
	}

	/**
	 * Sets the analysis information for the given variable to the given value
	 * based on the variable's aliasing set <i>after</i> the given node was
	 * evaluated.
	 * This may update analysis information for variables whose aliasing sets
	 * overlap with the aliasing set for the given node.
	 * 
	 * @param n
	 * @param x
	 * @param l
	 * @return Previous analysis information for the given variable.
	 */
	public void put(ASTNode n, Variable x, LE l) {
		put(getLocations(n, x), l);
	}

	/**
	 * Sets the analysis information for the given variable to the given value
	 * based on the variable's aliasing set <i>after</i> the given instruction
	 * was evaluated. This may update analysis information for variables whose
	 * aliasing sets overlap with the aliasing set for the given variable.
	 * 
	 * @param instr
	 * @param x
	 * @param l
	 * @return Previous analysis information for the given variable.
	 */
	public void put(TACInstruction instr, Variable x, LE l) {
		put(getLocations(instr, x), l);
	}

	/**
	 * Sets the analysis information for the given aliasing set to the given
	 * value. This may update analysis information for overlapping aliasing
	 * sets.
	 * 
	 * @param a
	 * @param l
	 * @return Previous analysis information for the given aliasing set.
	 */
	public void put(Aliasing a, LE l) {
		if (frozen)
			throw new IllegalStateException(
					"Cannot change frozen tuple.  Get a mutable copy to do this.");
		assert a != null;
		assert l != null;
		assert a.getLabels().isEmpty() == false : "This is probably a bug. Are you 'putting' bottom?";
		
		// this should not happen on bottom because bottom is frozen
		LE result = info.put(a, l);
		if (l.atLeastAsPrecise(result, null))
			// new info won't affect overlapping aliasing sets
			// also don't have to re-check against results for subsets
			return /*result*/;
		checked.clear();
		Set<ObjectLabel> labels = a.getLabels();
		for (Aliasing ot : info.keySet()) {
			if (ot.equals(a))
				continue;
			if (ot.hasAnyLabels(labels))
				info.put(ot, info.get(ot).copy().join(l.copy(), null));
		}
		return /*result*/;
	}
	
	/**
	 * Indicates whether the given variable may alias with a field.
	 * @param instr
	 * @param x
	 * @return <code>true</code> if the given variable may alias with a field,
	 * <code>false</code> otherwise.
	 */
	public boolean maybeFieldAccess(TACInstruction instr, Variable x) {
		Aliasing x_loc = getLocations(instr, x);
		for( Variable field_var : getReceiverFields().values() ) {
			if(getLocations(instr, field_var).hasAnyLabels(x_loc.getLabels()))
				return true;
		}
		return false;
		
	}

	/**
	 * Returns the aliasing set for the given variable <i>after</i> the 
	 * given node was evaluated.
	 * 
	 * @param n
	 * @param x
	 * @return the aliasing set for the given node <i>after</i> the node was
	 *         evaluated.
	 */
	public Aliasing getLocations(ASTNode n, Variable x) {
		return getLocationsAfter(n, x);
	}

	/**
	 * Returns the aliasing set for the given variable <i>after</i> the given
	 * instruction was evaluated.
	 * 
	 * @param instr
	 * @param x
	 * @return the aliasing set for the given variable <i>after</i> the given
	 *         instruction was evaluated.
	 */
	public Aliasing getLocations(TACInstruction instr, Variable x) {
		return getLocationsAfter(instr, x);
	}

	/**
	 * Returns the aliasing set for the given variable <i>before</i> the 
	 * given node was evaluated.
	 * 
	 * @param n
	 * @param x
	 * @return the aliasing set for the given node <i>after</i> the node was
	 *         evaluated.
	 */
	public Aliasing getLocationsBefore(ASTNode n, Variable x) {
		// TODO account for missing values, e.g. keyword variables
		return aliasing.getResultsBefore(n).get(x);
	}

	/**
	 * Returns the aliasing set for the given variable <i>before</i> the given
	 * instruction was evaluated.
	 * 
	 * @param instr
	 * @param x
	 * @return the aliasing set for the given variable <i>before</i> the given
	 *         instruction was evaluated.
	 */
	public Aliasing getLocationsBefore(TACInstruction instr, Variable x) {
		return getLocationsBefore(instr.getNode(), x);
	}

	/**
	 * Returns the aliasing set for the given variable <i>after</i> the 
	 * given node was evaluated.
	 * 
	 * @param n
	 * @param x
	 * @return the aliasing set for the given node <i>after</i> the node was
	 *         evaluated.
	 */
	public Aliasing getLocationsAfter(ASTNode n, Variable x) {
		// TODO account for missing values, e.g. keyword variables
		return aliasing.getResultsAfter(n).get(x);
	}

	/**
	 * Returns the aliasing set for the given variable <i>after</i> the given
	 * instruction was evaluated.
	 * 
	 * @param instr
	 * @param x
	 * @return the aliasing set for the given variable <i>after</i> the given
	 *         instruction was evaluated.
	 */
	public Aliasing getLocationsAfter(TACInstruction instr, Variable x) {
		return getLocationsAfter(instr.getNode(), x);
	}

	/**
	 * Freezes the tuple, forbidding future changes to its analysis information
	 * 
	 * @return The receiver of this call, frozen.
	 */
	public BrokenAliasAwareTupleLE<LE> freeze() {
		frozen = true;
		// do *not* make info immutable because defaultResult may add derived
		// info
		return this;
	}

	/**
	 * Creates a mutable copy of the receiving tuple. The returned tuple may be
	 * changed without affecting the original tuple. If the original tuple was
	 * frozen it remains frozen after this call. The returned tuple is never
	 * frozen.
	 * 
	 * @return A mutable copy of the receiving tuple.
	 */
	public BrokenAliasAwareTupleLE<LE> mutableCopy() {
		if(isBottom())
			return createTuple(new HashMap<Aliasing, LE>());
		
		Map<Aliasing, LE> newInfo = new HashMap<Aliasing, LE>(info.size());
		for (Aliasing a : info.keySet()) {
			newInfo.put(a, info.get(a).copy());
		}
		return createTuple(newInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.flow.LatticeElement, 
	 * 		org.eclipse.jdt.core.dom.ASTNode)
	 */
	public boolean atLeastAsPrecise(BrokenAliasAwareTupleLE<LE> other, ASTNode node) {
		if (this == other)
			return true;
		if( this.isBottom() )
			return true;
		if( other.isBottom() )
			return false;
		
		for (Aliasing a : other.info.keySet()) {
			if (this.get(a).atLeastAsPrecise(other.get(a), node) == false)
				return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#copy()
	 */
	public BrokenAliasAwareTupleLE<LE> copy() {
		// force-freeze when copies are requested
		return freeze();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#join(edu.cmu.cs.crystal.flow.LatticeElement,
	 *      org.eclipse.jdt.core.dom.ASTNode)
	 */
	public BrokenAliasAwareTupleLE<LE> join(BrokenAliasAwareTupleLE<LE> other, ASTNode node) {
		if (this == other)
			// force-freeze
			return freeze();

		if (this.aliasing != other.aliasing)
			// this is likely an error inside this class as the alias analysis
			// should never leave it
			throw new IllegalStateException(
					"Cannot join tuples based on different alias analyses");

		// Check to see if either tuple is the bottom tuple. If so, return the other
		// one.
		if( other.isBottom() )
			return this;
		if( this.isBottom() )
			return other;
		
		// force-freeze when joining with another LE
		this.frozen = true;
		other.frozen = true;

		
		HashSet<Aliasing> keys = new HashSet<Aliasing>(this.info.keySet());
		keys.addAll(other.info.keySet());
		Map<Aliasing, LE> newInfo = new HashMap<Aliasing, LE>(this.info.size());
		for (Aliasing a : keys) {
			newInfo.put(a, this.get(a).copy().join(other.get(a).copy(), node));
		}
		return createTuple(newInfo);
	}

	/**
	 * Internal method to create a tuple based on the receiving one but with new
	 * analysis information.
	 * 
	 * @param newInfo
	 *            New analysis information about aliasing sets.
	 * @return A new tuple with the same aliasing information and callback as
	 *         the receiving one but with the given (new) analysis information.
	 */
	private BrokenAliasAwareTupleLE<LE> createTuple(Map<Aliasing, LE> newInfo) {
		return new BrokenAliasAwareTupleLE<LE>(aliasing, newInfo, callback, this.receiverFields);
	}

	public BrokenAliasAwareTupleLE<LE> bottom() {
		return createTuple(null).freeze();
	}
	
	/**
	 * Does this lattice represent bottom?
	 */
	public boolean isBottom() {
		return this.info == null;
	}

	@Override
	public String toString() {
		return info == null ? "BOTTOM" : info.toString();
	}

	public Map<IVariableBinding, Variable> getReceiverFields() {
		return Collections.unmodifiableMap(receiverFields);
	}
	
	public Aliasing getEndResult(Variable var, MethodDeclaration d){
		return this.aliasing.getEndResults(d).get(var);
	}
	
	public Aliasing getStartResult(Variable var, MethodDeclaration d){
		return this.aliasing.getStartResults(d).get(var);
	}
}
