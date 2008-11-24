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
import java.util.List;

import edu.cmu.cs.plural.annot.Capture;
import edu.cmu.cs.plural.annot.Cases;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Param;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;

/**
 * This interface specifies {@link java.util.List} following the approach
 * chosen in {@link PluralCollection} to distinguish modifying operations
 * using a <i>marker state</i> {@link PluralCollection#MODIFIABLE}.  It also
 * illustrates how permissions can be used to specify {@link #subList(int, int)}
 * while still preventing concurrent modification exceptions in iterators,
 * which is tricky because it is possible to create sublists of sublists of lists.
 * @author Kevin Bierhoff
 * @since Nov 24, 2008
 * @see PluralIterator
 */
@Param(name = "underlying", releasedFrom = "alive")
// allow a list to be parameterized by another list
// this is for "views" on existing lists created with PluralList.subList()
public interface PluralList<E> extends PluralCollection<E>, List<E> {
    // Query Operations inherited from PluralCollection

    // Modification Operations mostly inherited from PluralCollection

	@Full(requires = MODIFIABLE)
    boolean addAll(int index, @Imm Collection<? extends E> c);

    // Comparison and hashing inherited from PluralCollection

    // Positional Access Operations

	@Pure
    E get(int index);

	@Full(requires = MODIFIABLE)
    E set(int index, E element);

	@Full(requires = MODIFIABLE)
    void add(int index, E element);

	@Full(requires = MODIFIABLE)
    E remove(int index);


    // Search Operations

	@Imm
    int indexOf(Object o);

	@Imm
    int lastIndexOf(Object o);


    // List Iterators

	@Cases({
		@Perm(requires = "immutable(this)", ensures = "unique(result) in readonly"),
		@Perm(requires = "full(this) in modifiable", ensures = "unique(result) in modifying")
	})
	@Capture(param = "underlying")
    // receiver permission is captured in result's "underlying" parameter
    PluralListIterator<E> listIterator();

	@Cases({
		@Perm(requires = "immutable(this)", ensures = "unique(result) in readonly"),
		@Perm(requires = "full(this) in modifiable", ensures = "unique(result) in modifying")
	})
	@Capture(param = "underlying")
    // receiver permission is captured in result's "underlying" parameter
    PluralListIterator<E> listIterator(int index);

    // View

    @Cases({
    	@Perm(requires = "immutable(this)", ensures = "unique(result) in unmodifiable"),
    	@Perm(requires = "full(this) in modifiable", ensures = "unique(result) in modifiable")
    })
    @Capture(param = "underlying")
    // receiver permission is captured in result's "underlying" parameter
    PluralList<E> subList(int fromIndex, int toIndex);

}
