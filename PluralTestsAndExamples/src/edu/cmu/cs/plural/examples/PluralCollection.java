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

import java.util.Collection;

import edu.cmu.cs.plural.annot.Capture;
import edu.cmu.cs.plural.annot.Cases;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.States;

/**
 * This interface illustrates how the (optional) modifying methods in the
 * {@link java.util.Collection Java Collections API} can be expressed with
 * Plural marker states, aka, type qualifiers.  
 * We simply introduce a marker {@link PluralCollection#MODIFIABLE} 
 * that is required for all modifying methods. 
 * Marker states cannot be forgotten once established, but this
 * approach means that without explicit knowledge that a collection is
 * {@link http://java.sun.com/javase/6/docs/technotes/guides/collections/overview.html modifiable}, 
 * no modifying operations can be used.
 * 
 * Notice that the {@link PluralCollection#MODIFIABLE} marker also lets us
 * specify {@link #iterator()} better: a modifying iterator can only
 * be created on a modifiable collection.
 * @author Kevin Bierhoff
 * @since Oct 24, 2008
 *
 */
@States(value = {PluralCollection.MODIFIABLE, PluralCollection.UNMODIFIABLE}, marker = true)
public interface PluralCollection<E> extends Collection<E> {
	// State Names

	/** 
	 * Marker state for collections known to be
	 * <a href="http://java.sun.com/javase/6/docs/technotes/guides/collections/overview.html">modifiable</a>.
	 * It should be required by all modifying methods in this interface.
	 */
	public static final String MODIFIABLE = "modifiable";
	
	/** 
	 * Marker state for collections known to be 
	 * <a href="http://java.sun.com/javase/6/docs/technotes/guides/collections/overview.html"><i>un</i>modifiable</a>.
	 * This state is not needed to specify this interface, but it may be useful for
	 * methods that create known-unmodifiable collections.
	 */
	public static final String UNMODIFIABLE = "unmodifiable";
	
    // Query Operations

	@Pure
	int size();

	@Pure
    boolean isEmpty();

	@Imm
    boolean contains(Object o);

	@Cases({
		@Perm(requires = "immutable(this)", ensures = "unique(result) in readonly"),
		@Perm(requires = "full(this) in modifiable", ensures = "unique(result) in modifying")
	})
	@Capture(param = "underlying")
    PluralIterator<E> iterator();

	@Imm
    Object[] toArray();

	@Imm
    <T> T[] toArray(T[] a);

    // Modification Operations

	@Full(requires = MODIFIABLE)
    boolean add(E e);

	@Full(requires = MODIFIABLE)
    boolean remove(Object o);


    // Bulk Operations

	@Imm
    boolean containsAll(@Imm Collection<?> c);

	@Full(requires = MODIFIABLE)
    boolean addAll(@Imm Collection<? extends E> c);

	@Full(requires = MODIFIABLE)
    boolean removeAll(@Imm Collection<?> c);

	@Full(requires = MODIFIABLE)
    boolean retainAll(@Imm Collection<?> c);

	@Full(requires = MODIFIABLE)
    void clear();


    // Comparison and hashing

	@Imm
    boolean equals(@Imm Object o);

	@Perm(requires = "immutable(this)")
    int hashCode();
}
