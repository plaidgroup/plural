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

import edu.cmu.cs.crystal.simple.LatticeElement;

/**
 * @author Kevin Bierhoff
 *
 */
public class DisjointSets<K, I, AE extends AliasedLatticeElement<K, I, AE>> implements LatticeElement<DisjointSets<K, I, AE>> {
	
	private AE bottom;
	private AE aliasedInfo;
	private Map<K, K> representatives;

	public DisjointSets(AE bottom) {
		super();
		this.bottom = bottom;
		this.representatives = Collections.emptyMap();
		this.aliasedInfo = bottom.copy();
	}
	
	/**
	 * @param bot
	 * @param newMap
	 * @param ae
	 */
	protected DisjointSets(AE bottom, Map<K, K> representatives, AE setInfo) {
		super();
		this.bottom = bottom;
		this.representatives = Collections.unmodifiableMap(representatives);
		this.aliasedInfo = setInfo;
	}

	public DisjointSets<K, I, AE> singleton(K key, I initialInformation) {
		HashMap<K, K> newMap = new HashMap<K, K>(representatives);
		newMap.put(key, key);
		return createElement(newMap, aliasedInfo.makeSingleton(key, initialInformation));
	}
	
	public DisjointSets<K, I, AE> union(K key1, K key2, ASTNode node) {
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
		HashMap<K, K> newMap = new HashMap<K, K>(representatives);
		newMap.put(key2, rep1); // merge 2nd set into first
		return createElement(newMap, aliasedInfo.makeSecondAliasFirst(rep1, rep2, node));
	}

	/**
	 * @param newMap
	 * @param ae
	 * @return
	 */
	private DisjointSets<K, I, AE> createElement(Map<K, K> newMap, AE ae) {
		return new DisjointSets<K, I, AE>(bottom.copy(), newMap, ae);
	}

	public boolean sameSet(K key1, K key2) {
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
	
	/**
	 * @return
	 */
	public AE getAliasedInfo() {
		return aliasedInfo;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	public boolean atLeastAsPrecise(DisjointSets<K, I, AE> other, ASTNode node) {
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
		return aliasedInfo.atLeastAsPrecise(other, this.representatives);
	}

	/**
	 * @param key1
	 * @param key2
	 * @return
	 */
	private boolean sameSetInternal(K knownKey1, K knownKey2) {
		return getRepresentative(knownKey1).equals(getRepresentative(knownKey2));
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#copy()
	 */
	public DisjointSets<K, I, AE> copy() {
		return createElement(representatives, aliasedInfo.copy());
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.crystal.flow.LatticeElement#join(edu.cmu.cs.crystal.flow.LatticeElement)
	 */
	public DisjointSets<K, I, AE> join(DisjointSets<K, I, AE> other, ASTNode node) {
		HashMap<K, K> newMap = new HashMap<K, K>(this.representatives);
		AE newInfo = aliasedInfo;
		for(K key1 : other.representatives.keySet()) {
			for(K key2 : other.representatives.keySet()) {
				if(other.sameSetInternal(key1, key2)) {
					K rep1 = newMap.get(key1);
					K rep2 = newMap.get(key2);
					if(rep1 == null && rep2 == null) {
						newMap.put(key1, key1);
						newMap.put(key2, key1);
						// keys previously unknown--no need to tell aliasedInfo about them
					}
					else if(rep1 == null) {
						newMap.put(key1, rep2);
						//key1 previously unknown--no need to tell aliasedInfo about it
						//newInfo = newInfo.makeSingleton(key1, bottom); 
						//newInfo = newInfo.makeAliases(rep2, key1);
					}
					else if(rep2 == null) {
						newMap.put(key2, rep1);
						//key2 previously unknown--no need to tell aliasedInfo about it
						//newInfo = newInfo.makeSingleton(key2, bottom); 
						//newInfo = newInfo.makeAliases(rep1, key2);
					}
					else if(rep1.equals(rep2) == false) {
						newMap.put(key2, rep1);
						newInfo = newInfo.makeSecondAliasFirst(rep1, rep2, node);
					}
				}
			}
		}
		return createElement(newMap, 
				aliasedInfo.join(other, Collections.unmodifiableMap(newMap), node));
	}

}
