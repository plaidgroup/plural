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
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.bridge.LatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;

/**
 * This class stores separate lattice information for disjoint sets of keys.
 * When lattice elements of this class are joined then overlapping sets are
 * merged and their lattice information is joined.
 * This lattice is useful for tracking local aliasing between variables.
 * 
 * <b>This is an <i>immutable</i> lattice</b>; therefore, all methods that change the
 * lattice return a whole-new lattice.
 * @author Kevin Bierhoff
 * @see TupleLatticeElement
 */
public class DisjointSetTuple<K, LE extends LatticeElement<LE>> 
		implements LatticeElement<DisjointSetTuple<K, LE>> {
	
	private Map<K, K> representatives;
	private TupleLatticeElement<K, LE> tuple;

	/**
	 * Creates a new tuple using the given object as bottom for individual sets.
	 * @param elementBottom
	 */
	public DisjointSetTuple(LE elementBottom) {
		super();
		this.representatives = Collections.emptyMap();
		this.tuple = new TupleLatticeElement<K, LE>(elementBottom, elementBottom);
	}
	
	/**
	 * Internal constructor to create a new tuple from the given structures.
	 * @param representatives
	 * @param tuple
	 */
	protected DisjointSetTuple(Map<K, K> representatives, TupleLatticeElement<K, LE> tuple) {
		super();
		this.representatives = Collections.unmodifiableMap(representatives);
		this.tuple = tuple;
	}

	/**
	 * Creates a new set with only the given key in it and maps it to the given
	 * lattice information.  This operation will change an existing set to use a
	 * new representative if the given key was previously used as the set's representative.
	 * @param key
	 * @param initialInformation
	 * @return A new object with key mapping to the given information.
	 */
	public DisjointSetTuple<K, LE> singleton(K key, LE initialInformation) {
		// new representative map
		HashMap<K, K> newMap;
		K newRep = null; // marker if key was representative for other keys but itself
		if(key.equals(representatives.get(key))) {
			// big problem: key was previously representative
			newMap = new HashMap<K, K>();
			for(K k : representatives.keySet()) {
				if(k.equals(key)) continue;
				if(key.equals(representatives.get(k))) {
					if(newRep == null) newRep = k;
					newMap.put(k, newRep);
				}
			}
		}
		else {
			newMap = new HashMap<K, K>(representatives);
		}			
		newMap.put(key, key);
		
		// new tuple
		TupleLatticeElement<K, LE> newTuple = tuple.copy();
		LE oldKeyInfo = newTuple.put(key, initialInformation);
		if(newRep != null)
			newTuple.put(newRep, oldKeyInfo);

		// create the new object
		return createElement(newMap, newTuple);
	}
	
	/**
	 * Adds the first parameter to the set containing the second parameter.
	 * The lattice information for the grown set is unchanged.
	 * If the second parameter is unknown then a new set with the two keys
	 * mapping to the "bottom" parameter passed upon creation of the lattice
	 * (with {@link #DisjointSetTuple(LatticeElement)}) will be created.
	 * @param key Key to add to the set containing the second parameter.
	 * @param setKey Key that should be part of an existing set.
	 * @return A new object with key added to setKey's set.
	 */
	public DisjointSetTuple<K, LE> addKeyToSet(K key, K setKey) {
		K rep = getRepresentative(setKey);
		if(rep == null) {
			K newRep = null; // marker if key was representative for other keys but itself
			
			// new representative map and tuple
			HashMap<K, K> newMap;
			TupleLatticeElement<K, LE> newTuple = tuple.copy();
			if(key.equals(representatives.get(key))) {
				// big problem: key was previously representative
				// this can only happen if key and setKey are not equal
				newMap = new HashMap<K, K>();
				for(K k : representatives.keySet()) {
					if(k.equals(key)) continue;
					if(key.equals(representatives.get(k))) {
						if(newRep == null) newRep = k;
						newMap.put(k, newRep);
					}
				}

				// take old info out
				LE oldKeyInfo = newTuple.remove(key);
				if(newRep != null) 
					// map old info for key to new representative for key's old set
					newTuple.put(newRep, oldKeyInfo);
			}
			else {
				newMap = new HashMap<K, K>(representatives);
				newTuple = tuple;
			}
			newMap.put(key, setKey);    // removes key from old set, if any
			newMap.put(setKey, setKey); // setKey not previously in set, unless equal to key
			
			// new tuple
			newTuple.put(setKey, tuple.get(setKey)); // should be bottom
			
			// create the new object
			return createElement(newMap, newTuple);
		}
		else {
			if(rep.equals(getRepresentative(key))) 
				// key and setKey are already in the same set (or even the same)
				return this;
			// else: key and setKey in different sets or key not in any set
			
			K newRep = null; // marker if key was representative for other keys but itself
			
			// new representative map and tuple
			HashMap<K, K> newMap;
			TupleLatticeElement<K, LE> newTuple;
			if(key.equals(representatives.get(key))) {
				// big problem: key was previously representative
				newMap = new HashMap<K, K>();
				for(K k : representatives.keySet()) {
					if(k.equals(key)) continue;
					if(key.equals(representatives.get(k))) {
						if(newRep == null) newRep = k;
						newMap.put(k, newRep);
					}
				}

				newTuple = tuple.copy();
				// take old info out
				LE oldKeyInfo = newTuple.remove(key);
				if(newRep != null) 
					// map old info for key to new representative for key's old set
					newTuple.put(newRep, oldKeyInfo);
			}
			else {
				newMap = new HashMap<K, K>(representatives);
				newTuple = tuple; // no copy needed: no change
			}
			newMap.put(key, rep); // removes key from previous set, if any
			
			// create the new object
			return createElement(newMap, newTuple);
		}
	}

	/**
	 * Merges the sets containing the two given keys and joins their lattice
	 * information.
	 * @param key1
	 * @param key2
	 * @param node AST node at which the union was triggered (for joining 
	 * underlying lattice elements.
	 * @return
	 */
	protected DisjointSetTuple<K, LE> union(K key1, K key2, ASTNode node) {
		if(key1.equals(key2)) return this;
		K rep1, rep2;
		rep1 = getRepresentative(key1);
		rep2 = getRepresentative(key2);
		if(rep1 == null)
			throw new NullPointerException("Unknown key: " + key1);
		if(rep2 == null)
			throw new NullPointerException("Unknown key: " + key2);
		if(rep1.equals(rep2))
			// already aliased--no changes
			return this;
		// new representative map
		HashMap<K, K> newMap = new HashMap<K, K>(representatives);
		for(K k : representatives.keySet()) {
			// merge 2nd set into first
			if(key2.equals(representatives.get(k)))
				newMap.put(k, rep1);
		}
		// new tuple
		TupleLatticeElement<K, LE> newTuple = tuple.copy();
		newTuple.put(rep1, newTuple.get(rep1).join(newTuple.remove(rep2), node));
		// create the new object
		return createElement(newMap, newTuple);
	}

	/**
	 * Factory method to create a new tuple from the given structures.
	 * @param newRepresentatives
	 * @param newTuple
	 * @return
	 */
	protected DisjointSetTuple<K, LE> createElement(Map<K, K> newRepresentatives, 
			TupleLatticeElement<K, LE> newTuple) {
		return new DisjointSetTuple<K, LE>(newRepresentatives, newTuple);
	}

	/**
	 * Tests whether the given keys are in the same set.
	 * @param key1
	 * @param key2
	 * @return <code>True</code> if the two parameters are in the same set, <code>false</code>
	 * otherwise.
	 */
	public boolean inSameSet(K key1, K key2) {
		K rep1, rep2;
		rep1 = getRepresentative(key1);
		rep2 = getRepresentative(key2);
		if(rep1 == null)
			throw new NullPointerException("Unknown key: " + key1);
		if(rep2 == null)
			throw new NullPointerException("Unknown key: " + key2);
		return rep1.equals(rep2);
	}
	
	/**
	 * Returns the lattice information for the set containing the given key.
	 * If the given key is unknown then the "bottom" parameter passed upon 
	 * creation of the lattice
	 * (with {@link #DisjointSetTuple(LatticeElement)}) will be returned. 
	 * @param key
	 * @return The lattice information for the set containing the given key
	 * or "bottom" if the key is unknown.
	 */
	public LE get(K key) {
		K rep = getRepresentative(key);
		// rep is null if key unknown
		return tuple.get(rep); // returns bottom if rep null 
	}
	
	/**
	 * Updates the lattice information for the set containing the given key.
	 * If the key is not previously known then it is added as a new singleton
	 * set with the given lattice information.
	 * @param key
	 * @param latticeInfo
	 * @return New object mapping the set containing the given key to the given lattice
	 * information.
	 */
	public DisjointSetTuple<K, LE> put(K key, LE latticeInfo) {
		K rep = getRepresentative(key);
		if(rep == null)
			return singleton(key, latticeInfo);
		TupleLatticeElement<K, LE> newTuple = tuple.copy();
		newTuple.put(rep, latticeInfo);
		return createElement(representatives, newTuple);
	}

	/**
	 * @param key
	 * @return
	 */
	public K getRepresentative(K key) {
		return representatives.get(key);
	}
	
	/**
	 * @return
	 */
	public Set<K> getKnownKeys() {
		return Collections.unmodifiableSet(representatives.keySet());
	}
	
	/**
	 * @return
	 */
	public Set<K> getSetRepresentatives() {
		return new HashSet<K>(representatives.values());
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	public boolean atLeastAsPrecise(DisjointSetTuple<K, LE> other, ASTNode node) {
		// 1. this must know at least the keys known in other
		if(this.representatives.keySet().containsAll(other.representatives.keySet()) == false)
			return false;
		// 2. this must consider all keys distinct that other considers distinct
		for(K key1 : other.representatives.keySet()) {
			for(K key2 : other.representatives.keySet()) {
				if(other.sameSetInternal(key1, key2) == false &&
						this.sameSetInternal(key1, key2))
					return false;
			}
		}
		// 3. compare tuples
		for(K key : tuple.getKeySet()) {
			K otherRep = other.getRepresentative(key);
			if(otherRep == null) continue;
			// TODO use this.get and other.get
			if(tuple.get(key).atLeastAsPrecise(other.tuple.get(otherRep), null) == false)
				return false;
		}
		return true;
	}

	/**
	 * 
	 * @param knownKey1
	 * @param knownKey2
	 * @return
	 */
	private boolean sameSetInternal(K knownKey1, K knownKey2) {
		return getRepresentative(knownKey1).equals(getRepresentative(knownKey2));
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#copy()
	 */
	public DisjointSetTuple<K, LE> copy() {
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#join(edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	public DisjointSetTuple<K, LE> join(DisjointSetTuple<K, LE> other, ASTNode node) {
		if(other == this) return this;
		// 0. copy data structures
		HashMap<K, K> newMap = new HashMap<K, K>(this.representatives);
		TupleLatticeElement<K, LE> newTuple = tuple.copy();
		// 1. union in sets from other
		for(K key1 : other.representatives.keySet()) {
			for(K key2 : other.representatives.keySet()) {
				// key1 and key2 can alias
				if(other.sameSetInternal(key1, key2)) {
					// key1 and key2 are in one set in other
					K rep1 = newMap.get(key1);
					K rep2 = newMap.get(key2);
					if(rep1 == null && rep2 == null) {
						// both unknown: new set with both keys
						newMap.put(key1, key1);
						newMap.put(key2, key1);
					}
					else if(rep1 == null) {
						// first unknown: union into second
						newMap.put(key1, rep2);
					}
					else if(rep2 == null) {
						// second unknown: union into first
						newMap.put(key2, rep1);
					}
					else if(rep1.equals(rep2) == false) {
						// not in one set in this: merge sets in newTuple
						newMap.put(key2, rep1);
						newTuple.put(rep1, newTuple.get(rep1).join(newTuple.remove(rep2), node));
					}
					// else already in one set
				}
			}
		}
		// 2. merge newTuple with other.tuple
		for(K key : newMap.keySet()) {
			// this is inefficient: only need to go over this's representative keys
			// problem: newTuple doesn't contain all reps if both were unknown before
			K rep = newMap.get(key);
			newTuple.put(rep, newTuple.get(rep).join(other.get(key), node));
		}
		// 3. create new object
		return createElement(newMap, newTuple);
	}

}
