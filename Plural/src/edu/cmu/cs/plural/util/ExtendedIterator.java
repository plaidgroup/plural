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

import java.util.Iterator;

/**
 * This interface extends regular iterators with the ability to replace 
 * the last element seen, using {@link #replace(Object)}.
 * @author Kevin Bierhoff
 *
 * @param <T>
 */
public interface ExtendedIterator<T> extends Iterator<T> {
	/**
	 * Replaces in the underlying collection the last element returned by the iterator
	 * with the given value. This method can be called only once per call to {@link 
	 * java.util.Iterator#next()}, and only if {@link java.util.Iterator#remove()}
	 * was not called since the last call to <code>next</code>.  Calling <code>remove</code>
	 * after calling this method is also not permitted.  The
	 * behavior of an iterator is unspecified if the underlying collection is modified 
	 * while the iteration is in progress in any way other than by calling this method
	 * or <code>remove</code>.
	 * @param newValue
	 * @throws IllegalStateException If the <code>next</code> method has not yet been 
	 * called, or <code>remove</code> or <code>replace</code> has already been called 
	 * after the last call to the <code>next</code> method.
	 * @see java.util.Iterator#remove()
	 */
	public void replace(T newValue);
}