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
package edu.cmu.cs.plural.examples;

import java.util.Iterator;

import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Param;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;

/**
 * This iterator interface complements {@link PluralCollection}.
 * It represents a vanilla iterator specification that ensures that
 * {@link #remove()} is only called on modifying iterators.
 * @author Kevin Bierhoff
 * @since Oct 24, 2008
 *
 */
@Refine({
	@States(value = {PluralIterator.READONLY, PluralIterator.MODIFYING}, marker = true),
	@States(value = {PluralIterator.AVAILABLE, PluralIterator.END}, dim = PluralIterator.NEXT),
	@States(value = {PluralIterator.RETRIEVED, PluralIterator.REMOVED}, dim = PluralIterator.CURRENT)
})
@Param(name = "underlying", releasedFrom = "alive")
// parameterize iterators with the data source they iterate over
public interface PluralIterator<E> extends Iterator<E> {

	/** 
	 * Marker state for iterators that can modify the underlying data.
	 * @see PluralCollection#iterator()
	 */
	public static final String MODIFYING = "modifying";

	/** 
	 * Marker state for iterators that cannot the underlying data.
	 * This state is not needed for specifying this interface,
	 * but it may be useful documentation for iterators known to be
	 * read-only.
	 * @see PluralCollection#iterator()
	 */
	public static final String READONLY = "readonly";

	/** State dimension for {@link #AVAILABLE} and {@link #END}. */
	public static final String NEXT = "NEXT";
	
	/** Indicates that {@link #next()} can be called. */
	public static final String AVAILABLE = "available";
	
	/** Indicates that no more elements can be retrieved from this iterator. */
	public static final String END = "end";
	
	/** State dimension for {@link #RETRIEVED} and {@link #REMOVED}. */
	public static final String CURRENT = "CURRENT";

	/** Indicates that an element was retrieved but not (yet) removed. */
	public static final String RETRIEVED = "retrieved";
	
	/** Indicates that an element was retrieved and removed. */
	public static final String REMOVED = "removed";

	@Pure(NEXT)
	@TrueIndicates(AVAILABLE)
	boolean hasNext();
	
	@Full(requires = AVAILABLE, ensures = RETRIEVED)
	E next();
	
	@Full(guarantee = CURRENT, requires = RETRIEVED, ensures = REMOVED)
	@Pure(MODIFYING)
	void remove();
	
}
