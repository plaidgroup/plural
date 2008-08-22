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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kevin Bierhoff
 * @since Aug 22, 2008
 *
 */
public class NonNullMap<K, V> extends HashMap<K, V> implements Map<K, V> {
	
	public interface Factory<V> {
		V create();
	}
	
	public static <K> NonNullMap<K, Integer> createIntMap() {
		return new NonNullMap<K, Integer>(new Factory<Integer>() {
			@Override
			public Integer create() {
				return 0;
			}
		});
	}
	
	public NonNullMap(Factory<V> factory) {
		this.factory = factory;
	}
	
	private static final long serialVersionUID = -3887331699490637158L;
	
	private Factory<V> factory;
	
	@Override
	public V get(Object key) {
		return safeGet((K) key);
	}

	public V safeGet(K key) {
		V result = super.get(key);
		if(result == null) {
			result = factory.create();
			put(key, result);
		}
		return result;
	}

}
