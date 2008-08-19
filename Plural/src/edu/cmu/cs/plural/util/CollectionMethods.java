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
package edu.cmu.cs.plural.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionMethods {

	/**
	 * Interface used for the method in a map call.
	 */
	public interface Mapping<I,O> {
		public O eval(I elem);
	}

	/**
	 * Functional map, returns a new list.
	 */
	public static <I,O> List<O> map(List<? extends I> list, Mapping<I,O> fun) {
		List<O> result = new ArrayList<O>(list.size());
		for( I elem : list ) {
			result.add(fun.eval(elem));
		}
		return result;
	}

	/**
	 * Concatenates two lists. Returns a brand new list and does not modify the original
	 * lists.
	 */
	public static <T> List<T> concat(List<? extends T> l1, List<? extends T> l2) {
		List<T> result = new ArrayList<T>(l1.size() + l2.size());
		result.addAll(l1);
		result.addAll(l2);
		return result;
	}
	
	/**
	 * Return the union of two maps without modifying either one.
	 */
	public static <K, V> Map<K,V> union(Map<? extends K, ? extends V> m1,
			                            Map<? extends K, ? extends V> m2) {
		Map<K,V> result = new HashMap<K,V>();
		result.putAll(m1);
		result.putAll(m2);
		return result;
	}
	
	/**
	 * Add an element to a 'multi-map.' Modifies the map in place.
	 */
	public static <K, V> void addToMultiMap(K key, V val, 
			Map<K, List<V>> map) {
		if( map.containsKey(key) ) {
			map.get(key).add(val);
		}
		else {
			List<V> l = new LinkedList<V>();
			l.add(val);
			map.put(key, l);
		}
	}
	
	public static <T> Set<T> createSetWithoutElement(Set<T> s, T element) {
		if(! s.contains(element))
			return s;
		LinkedHashSet<T> result = new LinkedHashSet<T>(s);
		result.remove(element);
		return result;
	}

	/**
	 * Creates a set from an array of elements (i.e., duplicate elements will be dropped).
	 * @param <T>
	 * @param elements
	 * @return
	 */
	public static <T> Set<T> mutableSet(T... elements) {
		LinkedHashSet<T> elemSet = new LinkedHashSet<T>(elements.length);
		for(T e : elements) {
			elemSet.add(e);
		}
		return elemSet;
	}

}
