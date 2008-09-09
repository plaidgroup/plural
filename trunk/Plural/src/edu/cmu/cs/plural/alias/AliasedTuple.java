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

import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.flow.LatticeElement;
import edu.cmu.cs.crystal.flow.TupleLatticeElement;

/**
 * @author Kevin Bierhoff
 *
 */
public class AliasedTuple<K, LE extends LatticeElement<LE>> 
		implements AliasedLatticeElement<K, LE, AliasedTuple<K, LE>> {
	
	private TupleLatticeElement<K, LE> tuple;
	
	public AliasedTuple(LE bottom) {
		super();
		this.tuple = new TupleLatticeElement<K, LE>(bottom, bottom);
	}

	/**
	 * @param name
	 */
	private AliasedTuple(TupleLatticeElement<K, LE> tuple) {
		super();
		this.tuple = tuple;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.alias.AliasedLatticeElement#atLeastAsPrecise(edu.cmu.cs.plural.alias.AliasedLatticeElement, java.util.Map)
	 */
	public boolean atLeastAsPrecise(DisjointSets<K, LE, AliasedTuple<K, LE>> other,
			Map<K, K> representatives) {
		for(K key : tuple.getKeySet()) {
			K otherRep = other.getRepresentative(key);
			if(otherRep == null) continue;
			if(tuple.get(key).atLeastAsPrecise(other.getAliasedInfo().tuple.get(otherRep), null) == false)
				return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.alias.AliasedLatticeElement#copy()
	 */
	public AliasedTuple<K, LE> copy() {
		return new AliasedTuple<K, LE>(tuple.copy());
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.alias.AliasedLatticeElement#join(edu.cmu.cs.plural.alias.AliasedLatticeElement, java.util.Map)
	 */
	public AliasedTuple<K, LE> join(DisjointSets<K, LE, AliasedTuple<K, LE>> other,
			Map<K, K> representatives, ASTNode node) {
		TupleLatticeElement<K, LE> newTuple = tuple.copy();
		for(K key : representatives.keySet()) {
			K otherRep = other.getRepresentative(key);
			if(otherRep == null) continue;
			LE thisLE = newTuple.get(key);
			LE otherLE = other.getAliasedInfo().tuple.get(otherRep);
			newTuple.put(key, thisLE.join(otherLE, node));
		}
		return new AliasedTuple<K, LE>(newTuple);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.alias.AliasedLatticeElement#makeSecondAliasFirst(java.lang.Object, java.lang.Object)
	 */
	public AliasedTuple<K, LE>  makeSecondAliasFirst(
			K representative, K alias, ASTNode node) {
		LE rep, al;
		rep = tuple.get(representative);
		al = tuple.get(alias);
		// take alias out
		tuple.remove(alias);
		tuple.put(representative, rep.join(al, node));
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.alias.AliasedLatticeElement#makeSingleton(java.lang.Object, java.lang.Object)
	 */
	public AliasedTuple<K, LE> makeSingleton(K key,
			LE initialInformation) {
		tuple.put(key, initialInformation);
		return this;
	}

}
