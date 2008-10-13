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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.analysis.alias.ObjectLabel;
import edu.cmu.cs.crystal.flow.ITACFlowAnalysis;
import edu.cmu.cs.crystal.flow.LatticeElement;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACInstruction;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.crystal.util.CollectionMethods;
import edu.cmu.cs.crystal.util.ExtendedIterator;
import edu.cmu.cs.crystal.util.Freezable;
import edu.cmu.cs.plural.util.ReplacementGenerator;

/**
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
public class AliasAwareTupleLE<LE extends LatticeElement<LE>> implements
	Freezable<AliasAwareTupleLE<LE>>,	
	MutableAliasAwareTuple<LE>, Iterable<LE> {

	/**
	 * Creates an initial lattice element that will throw an exception if
	 * information about unknown variables is requested.
	 * @param b 
	 * @param adb 
	 * 
	 * @param <LE>
	 * @return
	 */
	public static <LE extends LatticeElement<LE>> AliasAwareTupleLE<LE> create(final IAnalysisInput input) {
		return new AliasAwareTupleLE<LE>(input, new TupleCallback<LE>() {
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
	public static <LE extends LatticeElement<LE>> AliasAwareTupleLE<LE> create(
			final IAnalysisInput input,
			final LE bottom) {
		return new AliasAwareTupleLE<LE>(input, new TupleCallback<LE>() {
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
	
	/**
	 * Filter callback interface for locations.
	 * @author Kevin Bierhoff
	 * @since 8/15/2008
	 * @see AliasAwareTupleLE#removeLocations(edu.cmu.cs.plural.alias.AliasAwareTupleLE.LabelFilter)
	 */
	public interface LabelFilter {

		/**
		 * Indicate whether the given label should be considered, in the context
		 * this filter is used.
		 * @param l
		 * @return <code>true</code> if the given label should be considered,
		 * <code>false</code> otherwise.
		 */
		boolean isConsidered(ObjectLabel l);
		
	}

	/** Local aliasing analysis information. */
	private final ITACFlowAnalysis<AliasingLE> aliasing;

	/** The actual analysis information this tuple keeps track of. */
	private final Map<ObjectLabel, LE> info;

	/** Callback interface to customize the tuple's behavior. */
	private final TupleCallback<LE> callback;

	/**
	 * Aliasing sets that have been checked to be at least as precise as their
	 * subsets. This set helps making the tuple implementation more efficient
	 * and, when in doubt, should be cleared whenever aliasing information is
	 * modified.
	 */
	private final Map<Aliasing, LE> derivedCache;

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
	 * @param adb 
	 * 
	 * @param callback
	 */
	public AliasAwareTupleLE(IAnalysisInput input, TupleCallback<LE> callback) {
		Map<IVariableBinding, Variable> receiverFields = new HashMap<IVariableBinding, Variable>();
		this.aliasing = new TACFlowAnalysis<AliasingLE>(
				// pass modifiable map into LocalAliasTransfer
				new LocalAliasTransfer(input.getAnnoDB(), receiverFields),
				input.getComUnitTACs().unwrap());
		this.info = new HashMap<ObjectLabel, LE>();
		this.derivedCache = new HashMap<Aliasing, LE>();
		this.callback = callback;
		// make this's field a unmodifiable view
		this.receiverFields = Collections.unmodifiableMap(receiverFields);
	}

	/**
	 * Internal constructor to create lattice elements based on an existing one.
	 * 
	 * @param aliasing
	 * @param info
	 * @param callback
	 * @param receiverFields 
	 */
	private AliasAwareTupleLE(
			ITACFlowAnalysis<AliasingLE> aliasing,
			Map<ObjectLabel, LE> info, TupleCallback<LE> callback,
			Map<IVariableBinding, Variable> receiverFields) {
		this.aliasing = aliasing;
		this.info = info;
		this.derivedCache = new HashMap<Aliasing, LE>();
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
		
		LE result = derivedCache.get(objects);
		if (result == null) {
			LE derived = deriveInfoFromLabels(objects);
			if (derived == null) {
				result = defaultResult(objects);
			}
			else {
				result = derived;
				derivedCache.put(objects, derived);
			}
		}
		assert result != null;
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
		return callback.defaultResult(a);
	}

	/**
	 * This method attempts to derive analysis information for a given aliasing
	 * set from information about its labels.
	 * 
	 * @param a
	 * @return Derived analysis information or <code>null</code> if no
	 *         information about any of the labels in the given aliasing set is available.
	 */
	private LE deriveInfoFromLabels(Aliasing a) {
		LE derived = null;
		Set<ObjectLabel> labels = a.getLabels();
		if(isBottom())
			return null;
		for (ObjectLabel l : labels) {
			LE labelLE = info.get(l);
			if (labelLE != null) {
				if (derived == null)
					derived = labelLE.copy();
				else
					derived = derived.copy()
							.join(labelLE.copy(), null);
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
	 * Calling {@link #getLocationsBefore(TACInstruction, Variable)} on the returned
	 * variables should return valid locations.
	 * @param instr
	 * @return Variables defined before the given instruction
	 */
	public Set<Variable> getVariablesBefore(TACInstruction instr) {
		return aliasing.getResultsBefore(instr.getNode()).getKeySet();
	}
	
	/**
	 * Returns the set of variables defined <i>before</i> the given AST node.  
	 * Calling {@link #getLocationsBefore(ASTNode, Variable)} on the returned
	 * variables should return valid locations.
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
	 * Returns an extended iterator over the analysis information currently
	 * available in the lattice.  In particular, the iterator allows
	 * removing or replacing entries.
	 * @return Extended iterator over the analysis information currently 
	 * available in the lattice.
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public ExtendedIterator<LE> iterator() {
		final Iterator<ObjectLabel> labelIt = new HashSet<ObjectLabel>(info.keySet()).iterator();
		return new ExtendedIterator<LE>() {
			private ObjectLabel cur = null;
			@Override public boolean hasNext() {
				return labelIt.hasNext();
			}

			@Override public LE next() {
				return AliasAwareTupleLE.this.info.get(cur = labelIt.next());
			}

			@Override public void remove() {
				if(cur == null)
					throw new IllegalStateException("Nothing to remove.");
				AliasAwareTupleLE.this.derivedCache.clear();
				AliasAwareTupleLE.this.info.remove(cur);
				cur = null;
			}
			
			@Override public void replace(LE newValue) {
				if(cur == null)
					throw new IllegalStateException("Nothing to replace.");
				AliasAwareTupleLE.this.derivedCache.clear();
				AliasAwareTupleLE.this.info.put(cur, newValue);
				cur = null;
			}
		};
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
	 * @param newInfo
	 * @return Previous analysis information for the given aliasing set.
	 */
	public void put(Aliasing a, LE newInfo) {
		if (frozen)
			throw new IllegalStateException(
					"Cannot change frozen tuple.  Get a mutable copy to do this.");
		assert a != null;
		assert newInfo != null;
		assert a.getLabels().isEmpty() == false : "This is probably a bug.  Are you 'putting' an undefined variable";
		
		// put should not happen on bottom because bottom is frozen, so dereferencing info is ok
		
		derivedCache.clear();
		if(a.getLabels().size() == 1) {
			// strong update
			ObjectLabel l = a.getLabels().iterator().next();
			info.put(l, newInfo.copy());
			derivedCache.put(a, newInfo);
		}
		else {
			for(ObjectLabel l : a.getLabels()) {
				LE previous = info.get(l);
				if(previous == null)
					info.put(l, newInfo.copy());
				else
					// weak update
					info.put(l, previous.copy().join(newInfo.copy(), null));
			}
		}
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
	 * Super duper hacks!
	 * @param n
	 * @return
	 */
	public AliasingLE getLocationsBefore(ASTNode n) {
		return aliasing.getResultsBefore(n);
	}
	
	/**
	 * Super duper hacks!
	 * @param n
	 * @return
	 */
	public AliasingLE getLocationsAfter(ASTNode n) {
		return aliasing.getResultsAfter(n);
	}
	
	/**
	 * Return the locations at the very beginning of the method.
	 * @param decl
	 * @return
	 */
	public AliasingLE getStartLocations(
			MethodDeclaration decl) {
		return aliasing.getStartResults(decl);
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
	 * Returns the aliasing set for a given variable at the <i>end</i>
	 * of the given method.
	 * @param var
	 * @param d
	 * @return
	 */
	public Aliasing getEndLocations(Variable var, MethodDeclaration d){
		return this.aliasing.getEndResults(d).get(var);
	}
	
	/**
	 * Returns the aliasing set for a given variable at the <i>start</i>
	 * of the given method.
	 * @param var
	 * @param d
	 * @return
	 */
	public Aliasing getStartLocations(Variable var, MethodDeclaration d){
		return this.aliasing.getStartResults(d).get(var);
	}

	/**
	 * Freezes the tuple, forbidding future changes to its analysis information
	 * 
	 * @return The receiver of this call, frozen.
	 */
	public AliasAwareTupleLE<LE> freeze() {
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
	public AliasAwareTupleLE<LE> mutableCopy() {
		if(isBottom())
			return createTuple(new HashMap<ObjectLabel, LE>());
		
		Map<ObjectLabel, LE> newInfo = new HashMap<ObjectLabel, LE>(info.size());
		for (Map.Entry<ObjectLabel, LE> e : info.entrySet()) {
			newInfo.put(e.getKey(), e.getValue().copy());
		}
		return createTuple(newInfo);
	}

	public boolean atLeastAsPrecise(AliasAwareTupleLE<LE> other, ASTNode node) {
		this.freeze();
		if (this == other)
			return true;
		if( other == null )
			return false;
		other.freeze();
		
		if( this.isBottom() )
			return true;
		if( other.isBottom() )
			return false;
		
		for (Map.Entry<ObjectLabel, LE> e : this.info.entrySet()) {
			ObjectLabel l = e.getKey();
			LE otherInfo = other.info.get(l);
			if(otherInfo == null)
				// Any information is more precise than no information.
				continue;
			if (e.getValue().atLeastAsPrecise(otherInfo, node) == false)
				return false;
		}
		for(Map.Entry<ObjectLabel, LE> e : other.info.entrySet()) {
			if(! this.info.containsKey(e.getKey()))
				// there is info in other that this doesn't have -> fail to get it
				return false;
		}
		return true;
	}

	public AliasAwareTupleLE<LE> copy() {
		// force-freeze when copies are requested
		return freeze();
	}

	public AliasAwareTupleLE<LE> join(AliasAwareTupleLE<LE> other, ASTNode node) {
		this.freeze();
		if (this == other || other == null)
			return this;
		other.freeze();

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
		
		Map<ObjectLabel, LE> newInfo = new HashMap<ObjectLabel, LE>(this.info.size());
		for (ObjectLabel l : this.info.keySet()) {
			LE thisInfo = this.info.get(l);		
			LE otherInfo = other.info.get(l);
			
			if(otherInfo == null) {
				// just preserve permission from this
				newInfo.put(l, thisInfo.copy());
			}
			else {
				// If the permission is in both, we join
				newInfo.put(l, thisInfo.copy().join(otherInfo.copy(), node));
			}
		}
		
		for (ObjectLabel l : other.info.keySet()) {
			if(this.info.containsKey(l)) {
				// already processed in first loop
				continue;
			}
			else {
				// just preserve permission from other
				LE old = newInfo.put(l, other.info.get(l).copy());
				assert old == null; // there shouldn't be anything for this key
			}
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
	private AliasAwareTupleLE<LE> createTuple(Map<ObjectLabel, LE> newInfo) {
		return new AliasAwareTupleLE<LE>(aliasing, newInfo, callback, this.receiverFields);
	}

	public AliasAwareTupleLE<LE> bottom() {
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
		return receiverFields;
	}

	/**
	 * Returns a set of tuples representing the cross product of
	 * alternatives generated for each lattice element in the tuple.
	 * @param replacementGenerator
	 * @return
	 */
	public Set<AliasAwareTupleLE<LE>> createAlternativesAsNeeded(
			ReplacementGenerator<LE> replacementGenerator) {
		Set<AliasAwareTupleLE<LE>> result = null;
		for(Map.Entry<ObjectLabel, LE> e : info.entrySet()) {
			Set<LE> alternatives = replacementGenerator.replacements(e.getValue());
			if(alternatives == null)
				continue;
			alternatives = CollectionMethods.createSetWithoutElement(alternatives, e.getValue());
			if(! alternatives.isEmpty()) {
				if(result == null) {
					// first time alternatives were requested--populate result with initial tuple
					result = new LinkedHashSet<AliasAwareTupleLE<LE>>();
					result.add(this.mutableCopy());
				}
				Set<AliasAwareTupleLE<LE>> newTuples = new LinkedHashSet<AliasAwareTupleLE<LE>>();
				ObjectLabel key = e.getKey();
				for(AliasAwareTupleLE<LE> t : result) {
					// for each existing tuple, create a new tuple for each alternative
					boolean first = true;
					for(LE alt : alternatives) {
						AliasAwareTupleLE<LE> newT;
						if(first) {
							// optimization: in-place modification for first alternative
							newT = t;
							first = false;
						}
						else
							newT = t.mutableCopy();
						newT.info.put(key, alt.copy());
						newTuples.add(newT);
					}
				}
				result = newTuples;
			}
		}
		if(result == null)
			// no alternatives...
			return Collections.singleton(this);
		return result;
	}

	/**
	 * Returns a set of locations in the tuple that is backed by the tuple.
	 * Thus, removing locations from the returned set will affect the tuple.
	 * Removing locations will result in an exception if the tuple is frozen.
	 * Adding locations to the set is not possible.
	 * @return a set of locations in the tuple that is backed by the tuple.
	 * @see Map#keySet()
	 */
	public Set<ObjectLabel> keySet() {
		if(frozen)
			// info remains modifiable...
			return Collections.unmodifiableSet(info.keySet());
		return info.keySet();
	}

	/**
	 * Removes the locations for which the given filter's {@link LabelFilter#isConsidered(ObjectLabel)}
	 * returns <code>true</code> from the tuple.
	 * @param filter
	 * @return <code>true</code> if any locations were removed, <code>false</code> otherwise.
	 */
	public boolean removeLocations(LabelFilter filter) {
		boolean result = false;
		for(Iterator<ObjectLabel> it = info.keySet().iterator(); it.hasNext(); ) {
			ObjectLabel l = it.next();
			if(filter.isConsidered(l)) {
				result = true;
				it.remove();
			}
		}
		return result;
	}

}
