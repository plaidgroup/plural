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
package edu.cmu.cs.plural.linear;

/**
 * @author Kevin Bierhoff
 *
 */
abstract class SearchVisitor<T> extends DisjunctiveVisitor<T> {
	
	private T result;

	/**
	 * Override this method to carry out the search for a given tuple.
	 * By default (but see {@link #stopSearch(Object)}) we use a
	 * <code>null</code> result to indicate that nothing was found in the
	 * given tuple.
	 * @param tuple
	 * @return The result of searching the given tuple; this value
	 * in general can be <code>null</code>.
	 */
	public abstract T tupleAccess(TensorPluralTupleLE tuple);

	/**
	 * Override this method to decide whether the search should stop
	 * given the current result.  By default, the search stops when
	 * the current result is non-<code>null</code>.
	 * @param currentResult
	 * @return
	 */
	protected boolean stopSearch(T currentResult) {
		return currentResult != null;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.linear.DisjunctiveVisitor#all(edu.cmu.cs.plural.linear.ContextAllLE)
	 */
	@Override
	public T all(ContextAllLE le) {
		for(DisjunctiveLE e : le.getElements()) {
			if(stopSearch(e.dispatch(this)))
				break;
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.linear.DisjunctiveVisitor#choice(edu.cmu.cs.plural.linear.ContextChoiceLE)
	 */
	@Override
	public T choice(ContextChoiceLE le) {
		for(DisjunctiveLE e : le.getElements()) {
			if(stopSearch(e.dispatch(this)))
				break;
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.linear.DisjunctiveVisitor#context(edu.cmu.cs.plural.linear.LinearContextLE)
	 */
	@Override
	public T context(LinearContextLE le) {
		result = tupleAccess(le.getTuple());
		return result;
	}

	/**
	 * @return the result
	 */
	public T getResult() {
		return result;
	}

}
