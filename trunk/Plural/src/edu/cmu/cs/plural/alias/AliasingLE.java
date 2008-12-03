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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import edu.cmu.cs.crystal.analysis.alias.AliasLE;
import edu.cmu.cs.crystal.analysis.alias.ObjectLabel;
import edu.cmu.cs.crystal.flow.LatticeElement;
import edu.cmu.cs.crystal.tac.KeywordVariable;
import edu.cmu.cs.crystal.tac.SourceVariable;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.crystal.util.Freezable;

/**
 * @author Kevin Bierhoff
 *
 */
public final class AliasingLE implements LatticeElement<AliasingLE>, Freezable<AliasingLE> {
	
	private Map<Variable, AliasLE> locs;
	private Map<ObjectLabel, Set<Variable>> refMap;
	
	/** Is this lattice element frozen? */
	private boolean frozen = false;
	
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
		if(result != null)
			return result;
		if(key instanceof KeywordVariable) {
			KeywordVariable outer = (KeywordVariable) key;
			if(outer.isQualified()) {
				return outerLocation(outer);
			}
		}
		if(key instanceof SourceVariable) {
			// variable from outer scope?
			SourceVariable fromouter = (SourceVariable) key;
			if(fromouter.isCapturedFromOuterScope()) {
				return capturedLocation(fromouter);
			}
		}
		return AliasLE.bottom();
	}
	
	/**
	 * Create location for captured variable.
	 * @param fromouter a variable captured from an outer scope
	 * @return location for given captured variable.
	 */
	private AliasLE capturedLocation(SourceVariable fromouter) {
		assert fromouter.isCapturedFromOuterScope();
		return AliasLE.create(new CapturedVariableLabel(fromouter.getBinding()));
	}

	/**
	 * Create location for outer this or super.
	 * @param outer qualified this or super variable.
	 * @return location for given outer this or super.
	 */
	private AliasLE outerLocation(final KeywordVariable outer) {
		assert outer.isQualified();
		return AliasLE.create(new OuterLabel(outer.resolveType()));
	}

	public AliasLE put(Variable key, AliasLE le) {
		if( this.frozen )
			throw new IllegalStateException("This lattice element is frozen!");
		
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
		if( this.frozen )
			throw new IllegalStateException("This lattice element is frozen!");
		
		AliasLE result = locs.remove(key);
		if(result != null)
			removeRef(result, key);
	}
	
	/**
	 * @param le
	 * @param key
	 */
	private void addRef(AliasLE le, Variable key) {
		if( this.frozen )
			throw new IllegalStateException("This lattice element is frozen!");
		
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
		if( this.frozen )
			throw new IllegalStateException("This lattice element is frozen!");
		
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

	public Set<Variable> getVariables(ObjectLabel l) {
		if(isBottom())
			return Collections.emptySet();
		Set<Variable> result = refMap.get(l);
		if(result == null)
			return Collections.emptySet();
		return Collections.unmodifiableSet(result);
	}
	
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
			result.put(thisE.getKey(), smartJoin(thisE.getKey(), thisE.getValue(), other, node));
			//result.put(thisE.getKey(), thisE.getValue().copy().join(otherInfo.copy(), node));
		}
		for(Map.Entry<Variable, AliasLE> otherE : other.locs.entrySet()) {
			if(! this.locs.containsKey(otherE.getKey()))
				result.put(otherE.getKey(), otherE.getValue().copy());
		}
		
		return result;
	}

	/**
	 * @param key
	 * @param value
	 * @param other
	 * @param node
	 * @return
	 */
	private AliasLE smartJoin(Variable x, AliasLE thisInfo, AliasingLE other, ASTNode node) {
		AliasLE otherInfo = other.get(x);
		if(thisInfo == otherInfo)
			return thisInfo;
		
		if(! thisInfo.hasAnyLabels(otherInfo.getLabels()) && ! otherInfo.hasAnyLabels(thisInfo.getLabels())) {
			// disjoint location sets
			Set<Variable> thisV = this.getAllReferencingVariables(thisInfo);
			Set<Variable> otherV = other.getAllReferencingVariables(otherInfo);
			
			if(thisV.size() == 1 && otherV.size() == 1 &&
					thisV.contains(x) && otherV.contains(x) 
					/* thisV and otherV only contain x */) {
				return thisInfo;
			}
		}
		// normal join
		return thisInfo.copy().join(otherInfo.copy(), node);
	}

	/**
	 * @param otherInfo
	 * @return
	 */
	private Set<Variable> getAllReferencingVariables(AliasLE locs) {
		Set<Variable> result = new HashSet<Variable>();
		Set<ObjectLabel> allLabels = new HashSet<ObjectLabel>(locs.getLabels());
		Set<ObjectLabel> newLabels = locs.getLabels();
		while(!newLabels.isEmpty()) {
			Set<ObjectLabel> iterLabels = newLabels;
			// avoiding concurrent modification...
			newLabels = new HashSet<ObjectLabel>();
			for(ObjectLabel l : iterLabels) {
				Set<Variable> l_vars = getVariables(l);
				if(result.addAll(l_vars)) {
					for(Variable x : l_vars) {
						if(allLabels.addAll(get(x).getLabels()))
							// some label not seen before
							newLabels.addAll(get(x).getLabels());
					}
				}
			}
		}
		return result;
	}

	public AliasingLE freeze() {
		if( !frozen ) {
			if(locs != null)
				locs = Collections.unmodifiableMap(locs);
			if(refMap != null)
				refMap = Collections.unmodifiableMap(refMap);
		}
		
		frozen = true;
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

	/**
	 * Labels for qualified this and super variables.
	 * @author Kevin Bierhoff
	 * @since Oct 30, 2008
	 *
	 */
	private static class OuterLabel implements ObjectLabel {
		private final ITypeBinding outerType;
		
		public OuterLabel(ITypeBinding outerType) {
			this.outerType = outerType;
		}

		@Override 
		public ITypeBinding getType() { 
			return outerType;
		}

		@Override 
		public boolean isSummary() {
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((outerType == null) ? 0 : outerType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OuterLabel other = (OuterLabel) obj;
			if (outerType == null) {
				if (other.outerType != null)
					return false;
			} else if (!outerType.equals(other.outerType))
				return false;
			return true;
		}
		
	}
	
	private static class CapturedVariableLabel implements ObjectLabel {
		
		private final IVariableBinding var;
		
		public CapturedVariableLabel(IVariableBinding var) {
			this.var = var;
		}

		@Override
		public ITypeBinding getType() {
			return var.getType();
		}

		@Override
		public boolean isSummary() {
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((var == null) ? 0 : var.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CapturedVariableLabel other = (CapturedVariableLabel) obj;
			if (var == null) {
				if (other.var != null)
					return false;
			} else if (!var.equals(other.var))
				return false;
			return true;
		}
		
	}
}
