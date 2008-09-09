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
package edu.cmu.cs.plural.fractions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Kevin Bierhoff
 *
 */
public class FractionAssignment {

	private boolean changed;
	private Map<FractionTerm, SortedSet<FractionTerm>> equivalenceClasses;
	private Set<FractionTerm> nonZero;
	
	public FractionAssignment() {
		equivalenceClasses = new HashMap<FractionTerm, SortedSet<FractionTerm>>();
		equivalenceClasses.put(Fraction.zero(), mutableSet(Fraction.zero()));
		equivalenceClasses.put(Fraction.one(), mutableSet(Fraction.one()));
		
		nonZero = new HashSet<FractionTerm>();
		nonZero.add(Fraction.one());
	}
	
	private static SortedSet<FractionTerm> mutableSet(FractionTerm... initialElements) {
		TreeSet<FractionTerm> result = new TreeSet<FractionTerm>();
		for(FractionTerm e : initialElements) {
			result.add(e);
		}
		return result;
	}

	public void resetChangedFlag() {
		changed = false;
	}

	public boolean isChanged() {
		return changed;
	}

	public void makeEquivalent(Iterable<FractionTerm> terms) {
		for(FractionTerm t1 : terms)
			for(FractionTerm t2 : terms) {
				union(t1, t2);
			}
	}

	public void makeEquivalent(FractionTerm... terms) {
		for(FractionTerm t1 : terms)
			for(FractionTerm t2 : terms) {
				union(t1, t2);
			}
	}

	private void union(FractionTerm t1, FractionTerm t2) {
		if(t1.equals(t2)) {
			if(equivalenceClasses.containsKey(t1) == false) {
				equivalenceClasses.put(t1, mutableSet(t1));
				changed = true;
			}
			return;
		}
		SortedSet<FractionTerm> eq1 = equivalenceClasses.get(t1);
		SortedSet<FractionTerm> eq2 = equivalenceClasses.get(t2);
		if(eq1 != null) {
			if(eq1 == eq2) return;
			if(eq2 != null) {
				eq1.addAll(eq2);
				for(FractionTerm t : eq2) {
					equivalenceClasses.put(t, eq1);
				}
			}
			else {
				eq1.add(t2);
				equivalenceClasses.put(t2, eq1);
			}
		}
		else {
			if(eq2 != null) {
				eq2.add(t1);
				equivalenceClasses.put(t1, eq2);
				
			}
			else {
				SortedSet<FractionTerm> newClass = mutableSet(t1, t2);
				equivalenceClasses.put(t1, newClass);
				equivalenceClasses.put(t2, newClass);
			}
		}
		changed = true;
	}

	public boolean isOne(FractionTerm f) {
		return equivalenceClasses.get(Fraction.one()).contains(f);
	}

	public boolean isZero(FractionTerm f) {
		return equivalenceClasses.get(Fraction.zero()).contains(f);
	}
	
	public boolean isNonZero(FractionTerm f) {
		return nonZero.contains(f);
	}
	
	public Fraction getConstant(FractionTerm f) {
		Set<FractionTerm> eq = equivalenceClasses.get(f);
		if(eq == null)
			return null;
		
		final AbstractFractionTermVisitor<Fraction> v = new AbstractFractionTermVisitor<Fraction>() {
			@Override public Fraction one(OneFraction fract) {
				return fract;
			}
			@Override public Fraction zero(ZeroFraction fract) {
				return fract;
			}
			@Override public Fraction named(NamedFraction fract) {
				return fract;
			}
		};
		for(FractionTerm t : eq) {
			Fraction result = t.dispatch(v);
			if(result != null)
				return result;
		}
		return null;
	}

	public Fraction getLiteral(FractionTerm f) {
		Set<FractionTerm> eq = equivalenceClasses.get(f);
		if(eq == null)
			return null;
		
		final AbstractFractionTermVisitor<Fraction> v = new AbstractFractionTermVisitor<Fraction>() {
			@Override public Fraction one(OneFraction fract) {
				return fract;
			}
			@Override public Fraction zero(ZeroFraction fract) {
				return fract;
			}
			@Override public Fraction named(NamedFraction fract) {
				return fract;
			}
			@Override public Fraction var(VariableFraction fract) {
				return fract;
			}
		};
		for(FractionTerm t : eq) {
			Fraction result = t.dispatch(v);
			if(result != null)
				return result;
		}
		return null;
	}
	
	/**
	 * 
	 * @param f
	 * @return A fraction representing the given fraction, possibly the given fraction itself, but never <code>null</code>.
	 */
	public Fraction getRepresentative(Fraction f) {
		Fraction lit = getLiteral(f);
		if(lit == null)
			return f;
		return lit;
	}

	public void makeZero(FractionTerm f) {
		union(f, Fraction.zero());
	}

	public void makeZero(List<FractionTerm> terms) {
		for(FractionTerm f : terms)
			makeZero(f);
	}

	public void makeOne(FractionTerm f) {
		union(f, Fraction.one());
	}

	public void makeNonZero(FractionTerm t) {
		if(nonZero.add(t))
			changed = true;
		Set<FractionTerm> eq = equivalenceClasses.get(t);
		if(eq != null)
			if(nonZero.addAll(eq))
				changed = true;
	}

	/**
	 * @param thisF
	 * @param otherF
	 * @return
	 */
	public boolean areEquivalent(FractionTerm t1, FractionTerm t2) {
		if(t1.equals(t2)) return true;
		SortedSet<FractionTerm> eq1 = equivalenceClasses.get(t1);
		SortedSet<FractionTerm> eq2 = equivalenceClasses.get(t2);
//		if(eq1 == null || eq2 == null)
//			throw new IllegalArgumentException("Term unknown: " + (eq1 == null ? t1 : t2));
		return eq1 != null && (eq1 == eq2 || eq1.contains(t2) || (eq2 != null && eq2.contains(t1)));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("[");
		for(FractionTerm t : equivalenceClasses.keySet()) {
			if(result.length() > 1) result.append(',');
			Fraction f = getLiteral(t);
			if(f == null)
				result.append(t + " in " + equivalenceClasses.get(t));
			else
				result.append(t + "=" + f);
		}
		result.append(']');
		return result.toString();
	}

	/**
	 * Checks if there is no more than one constant in every equivalence class.
	 * Constants are {@link ZeroFraction}, {@link OneFraction}, and {@link NamedFraction}.
	 * @return
	 */
	public boolean isConsistent() {
		for(Set<FractionTerm> eq  : equivalenceClasses.values()) {
			FractionTerm literal = null;
			for(FractionTerm t : eq) {
				if((t instanceof Fraction) == false) continue;
				if((t instanceof VariableFraction) == false) {
					if(literal != null)
						return false;
					literal = t; 
				}
			}
		}
		
		for(FractionTerm z : equivalenceClasses.get(Fraction.zero())) {
			if(nonZero.contains(z))
				return false;
		}
		
		return true;
	}

}
