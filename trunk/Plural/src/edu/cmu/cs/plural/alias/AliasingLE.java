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

import edu.cmu.cs.crystal.analysis.alias.AliasLE;
import edu.cmu.cs.crystal.analysis.alias.ObjectLabel;
import edu.cmu.cs.crystal.flow.LatticeElement;
import edu.cmu.cs.crystal.internal.Freezable;
import edu.cmu.cs.crystal.tac.Variable;

/**
 * @author Kevin Bierhoff
 *
 */
public class AliasingLE implements LatticeElement<AliasingLE>, Freezable<AliasingLE> {
	
	private Map<Variable, AliasLE> locs;
	private Map<ObjectLabel, Set<Variable>> refMap;
	
	/**
	 * Creates a bottom mapping.
	 * @return
	 */
	public static AliasingLE createBottom() {
		return new AliasingLE(null, null);
	}
	
	/**
	 * Creates an empty mapping.
	 * @return
	 */
	public static AliasingLE createEmpty() {
		return new AliasingLE();
	}
	
	/**
	 * Creates a new tuple with empty maps.
	 */
	private AliasingLE() {
		this(new HashMap<Variable, AliasLE>(), new HashMap<ObjectLabel, Set<Variable>>());
	}
	
	/**
	 * Creates a new tuple with the given maps.
	 * @param locs
	 * @param refCount
	 */
	private AliasingLE(Map<Variable, AliasLE> locs, HashMap<ObjectLabel, Set<Variable>> refCount) {
		this.locs = locs;
		this.refMap = refCount;
	}

	/**
	 * @param key
	 * @return
	 */
	public AliasLE get(Variable key) {
		if(isBottom())
			return AliasLE.bottom();
		AliasLE result = locs.get(key);
		if(result == null)
			return AliasLE.bottom();
		else
			return result;
	}
	
	public AliasLE put(Variable key, AliasLE le) {
		if(isBottom())
			throw new IllegalStateException();
		if(key == null || le == null)
			throw new NullPointerException();
		AliasLE result = locs.put(key, le);
		if(result != null)
			removeRef(result, key);
		addRef(le, key);
		return result;
	}
	
	public void kill(Variable key) {
		if(isBottom())
			return;
		AliasLE result = locs.remove(key);
		if(result != null)
			removeRef(result, key);
	}
	
	/**
	 * @param le
	 * @param key
	 */
	private void addRef(AliasLE le, Variable key) {
		for(ObjectLabel l : le.getLabels()) {
			Set<Variable> vars = refMap.get(l);
			if(vars == null) {
				vars = Collections.singleton(key);
			}
			else if(! vars.contains(key)) {
				vars = new HashSet<Variable>(vars);
				vars.add(key);
				vars = Collections.unmodifiableSet(vars);
			}
			else
				continue;
			refMap.put(l, vars);
		}
	}

	/**
	 * @param result
	 * @param key
	 */
	private void removeRef(AliasLE result, Variable key) {
		for(ObjectLabel l : result.getLabels()) {
			Set<Variable> vars = refMap.get(l);
			if(vars != null && vars.contains(key)) {
				if(vars.size() <= 1) {
					refMap.remove(l);
				}
				else {
					vars = new HashSet<Variable>(vars);
					vars.remove(key);
					vars = Collections.unmodifiableSet(vars);
					refMap.put(l, vars);
				}
			}
		}
	}

//	public boolean isLive(AliasLE le) {
//		if(isBottom())
//			return false;
//		if(le.getLabels().isEmpty())
//			return true;
//		for(ObjectLabel l : le.getLabels())
//			if(isLive(l))
//				return true;
//		return false;
//	}
	
	public Set<Variable> getVariables(ObjectLabel l) {
		if(isBottom())
			return Collections.emptySet();
		Set<Variable> result = refMap.get(l);
		if(result == null)
			return Collections.emptySet();
		return Collections.unmodifiableSet(result);
	}
	
//	/**
//	 * @param l
//	 * @return
//	 */
//	public boolean isLive(ObjectLabel l) {
//		if(isBottom())
//			return true;
//		Set<Variable> vars = refMap.get(l);
//		if(vars == null || vars.isEmpty())
//			return false;
//		return true;
//	}

	public boolean isBottom() {
		return locs == null;
	}
	
	public Set<Variable> getKeySet() {
		return Collections.unmodifiableSet(locs.keySet());
	}

	public boolean atLeastAsPrecise(AliasingLE other, ASTNode node) {
		this.freeze();

		if(this == other)
			return true;
		if(other == null)
			return this.isBottom();
		
		other.freeze();

		if(this.isBottom())
			return true;
		
		if(other.isBottom())
			return false;
		
		for(Map.Entry<Variable, AliasLE> thisE : this.locs.entrySet()) {
			AliasLE otherInfo = other.get(thisE.getKey());
			if(! thisE.getValue().atLeastAsPrecise(otherInfo, node))
				return false;
		}
		// additional elements in other don't matter : this.get returns bottom for them, which is more precise
		return true;
	}

	public AliasingLE copy() {
		return freeze();
	}

	public AliasingLE join(AliasingLE other, ASTNode node) {
		this.freeze();
		
		if(this == other || other == null)
			return this;
		
		other.freeze();
		
		if(other.isBottom())
			return this;
		if(this.isBottom())
			return other;
		
		AliasingLE result = new AliasingLE();
		
		for(Map.Entry<Variable, AliasLE> thisE : this.locs.entrySet()) {
			AliasLE otherInfo = other.get(thisE.getKey());
			result.put(thisE.getKey(), thisE.getValue().copy().join(otherInfo.copy(), node));
		}
		for(Map.Entry<Variable, AliasLE> otherE : other.locs.entrySet()) {
			if(! this.locs.containsKey(otherE.getKey()))
				result.put(otherE.getKey(), otherE.getValue().copy());
		}
		
		return result;
	}

	public AliasingLE freeze() {
		if(locs != null)
			locs = Collections.unmodifiableMap(locs);
		if(refMap != null)
			refMap = Collections.unmodifiableMap(refMap);
		return this;
	}

	public AliasingLE mutableCopy() {
		if(isBottom())
			return this;
		return new AliasingLE(new HashMap<Variable, AliasLE>(locs), new HashMap<ObjectLabel, Set<Variable>>(refMap));
	}
	
	@Override
	public String toString() {
		return locs == null ? "BOTTOM" : locs.toString();
	}

}
