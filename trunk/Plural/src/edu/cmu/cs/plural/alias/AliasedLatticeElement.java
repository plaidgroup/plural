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

/**
 * @author Kevin Bierhoff
 *
 */
public interface AliasedLatticeElement<K, I, AE extends AliasedLatticeElement<K, I, AE>> {
	
	/**
	 * Makes the given key a separate element, with the given initial information. 
	 * @param key
	 * @param initialInformation
	 * @return The modified lattice element.
	 */
	public AE makeSingleton(K key, I initialInformation);

	/**
	 * Renames occurrances of the second parameter into the first
	 * parameter, joining lattice information if necessary.
	 * @param representative New representative for <code>alias</code>
	 * @param alias Key to be renamed.
	 * @param node AST node on which the join occurs. 
	 * @return The modified lattice element.
	 */
	public AE makeSecondAliasFirst(K representative, K alias, ASTNode node);
	
	/**
	 * Compares this lattice information with another aliased lattice information,
	 * alpha-equivalent taking renaming to common representatives into account.
	 * @param other Lattice element to compare with.  
	 * @param representatives Maps all keys that may occur in both aliased
	 * lattice infos to common representatives. 
	 * @return <code>true</code> if this lattice element is more precise than
	 * the <code>other</code>, <code>false</code> otherwise.
	 * @see edu.cmu.cs.crystal.bridge.LatticeElement#atLeastAsPrecise(edu.cmu.cs.crystal.bridge.LatticeElement, ASTNode)
	 */
	public boolean atLeastAsPrecise(DisjointSets<K, I, AE> other, 
			Map<K, K> representatives);

	/**
	 * Copies this lattice element, if it is not immutable.
	 * @return Deep copy of this lattice element, or <code>this</code> if 
	 * it is immutable.
	 * @see edu.cmu.cs.crystal.bridge.LatticeElement#copy()
	 */
	public AE copy();
	
	/**
	 * Joins this lattice information with another aliased lattice information,
	 * possibly renaming to common representatives.
	 * @param other Lattice element to join with.  
	 * @param representatives Maps all keys that may occur in both aliased
	 * lattice infos to common representatives. 
	 * @param node AST node on which the join occurs. 
	 * @return Joined lattice information, using the given representatives.
	 * @see edu.cmu.cs.crystal.bridge.LatticeElement#join(edu.cmu.cs.crystal.bridge.LatticeElement)
	 */
	public AE join(DisjointSets<K, I, AE> other, 
			Map<K, K> representatives, ASTNode node);
}
