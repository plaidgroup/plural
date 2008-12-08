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
package edu.cmu.cs.plural.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import edu.cmu.cs.plural.annot.Cases;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.FalseIndicates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Refine;
import edu.cmu.cs.plural.annot.ResultImm;
import edu.cmu.cs.plural.annot.ResultUnique;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;

/**
 * An immutable cons list. Because this list cannot be modified, many
 * lists can share the same elements. Many operations must traverse, 
 * and therefore run in linear time. Adding things to the front of the
 * list is done in constant time. Implements all methods of the {@code List}
 * interface, except those which are optional because they modify the
 * list in place.<br>
 * <br>
 * 9/15/08<br>
 * Beginning the specification of this class.
 * 
 * @author Nels E. Beckman
 * @since Sep 8, 2008
 *
 * @param <T> The type of elements this list holds.
 */
@States(marker = true, value={"EMPTY", "NONEMPTY", "IMPOSSIBLE"})
public abstract class ConsList<T> implements List<T> {

	@SuppressWarnings("unchecked")
	@Imm("EMPTY")
	private final static Empty EMPTY_CONS_LIST = new Empty();
	
	/*
	 * ConsList methods 
	 */
	
	/**
	 * Create a new, empty list.
	 */
	@SuppressWarnings("unchecked")
	@Perm(ensures="immutable(result) in EMPTY")
	@ResultImm(ensures="EMPTY")
	public static <T> ConsList<T> empty() {
		return EMPTY_CONS_LIST;
	}
	
	/**
	 * Create a list with one element.
	 */
	@Perm(requires="immutable(#0)", ensures="immutable(result) in NONEMPTY")
	public static <T> ConsList<T> singleton(T hd) {
		return new Nonempty<T>(hd, ConsList.<T>empty());
	}
	
	/**
	 * Create a {@code ConsList} with the given elements.
	 */
	@Perm(ensures="immutable(result)")
	public static <T> ConsList<T> list(@Imm T... ts) {
		if( ts.length == 0 ) 
			return empty();
		else if( ts.length == 1 )
			return singleton(ts[0]); // NEB: We cannot specify arrays...
		else {
			ConsList<T> cur_list = empty();
			for( int i = ts.length - 1; i >= 0; i-- ) {
				cur_list = cons(ts[i], cur_list); // NEB: We cannot specify arrays...
			}
			return cur_list;
		}
	}
	
	/**
	 * Create a new {@code ConsList} with {@code hd} as the
	 * first element and {@code tl} as the rest of the list.
	 */
	@Perm(requires="immutable(#0) * immutable(#1)", 
		  ensures="immutable(result) in NONEMPTY")
	public static <T> ConsList<T> cons(T hd, ConsList<T> tl) {
		return new Nonempty<T>(hd, tl);
	}
	
	/**
	 * Concatenate the two given lists.
	 */
	@ResultImm
	public static <T> ConsList<T> concat(@Imm(returned=false) ConsList<T> front, 
			                             @Imm(returned=false) ConsList<T> back) {
		if( front.isEmpty() ) {
			return back;
		}
		else {
			return cons(front.hd(), concat(front.tl(), back));
		}
	}
	
	/**
	 * Get the first element of this list.
	 */
	@Pure(requires="NONEMPTY", ensures="NONEMPTY")
	@ResultImm
	public abstract T hd();
	
	/**
	 * Return this list without the first element.
	 */
	@Pure
	@ResultImm
	public abstract ConsList<T> tl();
	
	/**
	 * Removes every element in the list where 
	 * {@code hd().equals(t) == true}.
	 */
	@Imm(returned=false)
	@ResultImm
	public final ConsList<T> removeElement(@Pure T t) {
		if( this.isEmpty() ) {
			return this;
		}
		else if( this.hd().equals(t) ) {
			return this.tl().removeElement(t);
		}
		else {
			return cons(hd(), this.tl().removeElement(t));
		}
	}
	
	/**
	 * Removes the first element in the list where
	 * {@code hd().equals(t) == true}.
	 */
	@Imm(returned=false)
	@ResultImm
	public final ConsList<T> removeElementOnce(@Pure T t) {
		if( this.isEmpty() ) {
			return this;
		}
		else if( this.hd().equals(t) ) {
			return this.tl();
		}
		else {
			return cons(hd(), this.tl().removeElementOnce(t));
		}
	}
	
	/**
	 * Given a 'first-class function' that takes elements of type
	 * T and returns elements of type O, call that function on
	 * every element of {@code this} list, returning a new list
	 * of Os. 
	 */
	@ResultImm
	@Pure(returned=false)
	public final <O> ConsList<O> map(@Pure Lambda<? super T, ? extends O> lam) {
		if( this.isEmpty() ) {
			return empty();
		}
		else {
			O new_hd = lam.call(this.hd());
			return cons(new_hd, this.tl().map(lam));
		}
	}
	
	/**
	 * Given a 'first-class function' that takes elements of type
	 * T and returns a boolean, call that function on every element
	 * of {@code this} list, returning a new list that only contains
	 * the elements for which the function call returned true.
	 */
	@Imm(returned=false)
	@ResultImm
	public final ConsList<T> filter(@Pure Lambda<? super T,? extends Boolean> lam) {
		if( this.isEmpty() ) {
			return this;
		}
		else {
			T hd = this.hd();
			if( lam.call(hd) )
				return cons(hd, this.tl().filter(lam));
			else
				return this.tl().filter(lam);
		}
	}
	
	/**
	 * Fold over the elements of this list.
	 */
	@Imm
	@ResultImm
	public final <O> O foldl(@Pure Lambda2<? super T,? super O,? extends O> lam, @Imm(returned=false) O o) {
		if( this.isEmpty() ) {
			return o;
		}
		else {
			T hd = this.hd();
			O new_o = lam.call(hd, o);
			return this.tl().foldl(lam, new_o);
		}
	}
	
	@Pure
	protected abstract int indexOfHelper(int cur_index, Object o);
	
	@Pure
	protected abstract int lastIndexOfHelper(boolean found, int cur_index, int cur_last, Object o);
	
	/*
	 * List methods
	 */
	@Pure public abstract int size();	
	
	@Pure(fieldAccess=true)
	@TrueIndicates("EMPTY")
	@FalseIndicates("NONEMPTY")
	public abstract boolean isEmpty();
	
	@Pure public abstract int indexOf(Object o);
	
	@Pure public abstract int lastIndexOf(Object o);
		
	/**
	 * Note: For {@code ConsList<T>}, this method is less efficient
	 * than {@code iterator()} and should only be used if iterating
	 * in the reverse direction is really important. In particular,
	 * calling the {@code previous()} method is linear in the size
	 * of the list.
	 */
	@Imm(returned=false)
	@ResultImm
	public final ListIterator<T> listIterator() {
		return listIterator(0);
	}
	
	@Imm // NEB: The right specification here is index < size, and then we need an implication.
	@ResultImm
	public final T get(int index) {
		if( index == 0 ) {
			return hd();
		}
		else {
			return tl().get(index - 1);
		}
	}

	@Cases({
		@Perm(requires="immutable(this) in EMPTY", ensures="unique(result) in END"),
		@Perm(requires="immutable(this) in NONEMPTY", ensures="unique(result) in HASNEXT"),
		@Perm(requires="immutable(this)", ensures="unique(result)")
	})
	public final Iterator<T> iterator() {
		
		@ClassStates({@State(name="alive", inv="immutable(curList)"),
			          @State(name="HASNEXT", inv="curList in NONEMPTY"),
			          @State(name="END", inv="curList in EMPTY")})
		class ConsListIterator<S> implements Iterator<S> {

			private ConsList<S> curList;
			
			@Cases({
				@Perm(requires="immutable(#0) in EMPTY", ensures="unique(this!fr) in END"),
				@Perm(requires="immutable(#0) in NONEMPTY", ensures="unique(this!fr) in HASNEXT"),
				@Perm(requires="immutable(#0)", ensures="unique(this!fr)")
			})
			public ConsListIterator(ConsList<S> curList) {
				this.curList = curList;
			}
			
			@Override
			@Pure(fieldAccess=true)
			@TrueIndicates("HASNEXT")
			@FalseIndicates("END")
			public boolean hasNext() {
				if( curList.isEmpty() )
					return false;
				else
					return true;
			}

			@Override
			@Full(fieldAccess=true, requires="HASNEXT")
			@ResultImm
			public S next() {
				S hd = curList.hd();
				curList = curList.tl();
				return hd;
			}

			@Override
			@Full(requires="IMPOSSIBLE", ensures="IMPOSSIBLE")
			public void remove() {
				impossible();
			}
			
		}
		
		return new ConsListIterator<T>(this);
	}

	@Imm
	public abstract boolean containsAll(Collection<?> c);
	
	/**
	 * Will share the back of the list, if the sublist we are asking for
	 * only cuts off part of the front.
	 */
	@Imm(returned=false)
	@ResultImm
	private ConsList<T> subListSameTail(int fromIndex) {
		if( fromIndex == 0 ) {
			return this;
		}
		else {
			return subListSameTail(fromIndex - 1);
		}
	}
	
	@Imm(returned=false)
	@ResultImm
	public final ConsList<T> subList(int fromIndex, int toIndex) {
		if( fromIndex < 0 || fromIndex > toIndex )
			throw new IndexOutOfBoundsException();
		
		if( toIndex == this.size() ) {
			return subListSameTail(fromIndex);
		}
		else if( fromIndex == 0 && toIndex == 0 ) {
			return empty();
		}
		else if( fromIndex > 0 ) {
			return this.tl().subList(fromIndex - 1, toIndex - 1);
		}
		else {
			return cons(hd(), this.tl().subList(0, toIndex - 1)); // NEB: We would need a pre-condition saying, "toIndex <= this.size()".
		}
	}
	
	@Pure
	public final boolean contains(@Pure Object o) {
		if( this.isEmpty() ) {
			return false;
		}
		else if( hd().equals(o) ) {
			return true;
		}
		else {
			return tl().contains(o);
		}
	}

	@ResultUnique
	@Imm(returned=false)
	public final Object[] toArray() {
		Object[] result = new Object[this.size()];
		
		Iterator<T> iter = this.iterator();
		int i = 0;
		
		while( iter.hasNext() ) {
			T t = iter.next();
			result[i] = t;
			i++;
		}
		
		if( i != this.size() )
			throw new RuntimeException("Invariant violated.");
		
		return result;
	}

	@SuppressWarnings("unchecked")
	@ResultImm
	@Imm(returned=false)
	public final <S> S[] toArray(@Full S[] a) {
        if (a.length < this.size())
            a = (S[])java.lang.reflect.Array.newInstance(
                                a.getClass().getComponentType(), size());
        
        Object[] result = a;
		Iterator<T> iter = this.iterator();
		int i = 0;
		
		while( iter.hasNext() ) {
			T t = iter.next();
			result[i] = t;
			i++;
		}
		
		if( i != this.size() )
			throw new RuntimeException("Invariant violated.");

        return a;
	}
	
	/*
	 * Below are mutating operations.
	 */
	private static <R> R impossible() {
		throw new 
		UnsupportedOperationException(
				"ConsList is immutable and does not support this operation.");
	}
	
	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final void add(int index, T element) {
		impossible();
	}

	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final boolean add(T e) {
		return impossible();
	}

	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final boolean addAll(Collection<? extends T> c) {
		return impossible();
	}

	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final boolean addAll(int index, Collection<? extends T> c) {
		return impossible();
	}

	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final void clear() {
		impossible();		
	}
	
	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final T remove(int index) {
		return impossible();
	}

	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final boolean remove(Object o) {
		return impossible();
	}

	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final boolean removeAll(Collection<?> c) {
		return impossible();
	}

	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final boolean retainAll(Collection<?> c) {
		return impossible();
	}

	@Imm(requires="IMPOSSIBLE",ensures="IMPOSSIBLE")
	public final T set(int index, T element) {
		return impossible();
	}
	
	/**
	 * Note: For {@code ConsList<T>}, this method is less efficient
	 * than {@code iterator()} and should only be used if iterating
	 * in the reverse direction is really important. In particular,
	 * calling the {@code previous()} method is linear in the size
	 * of the list.
	 */
	@ResultUnique
	@Cases({
		@Perm(requires="immutable(this) in EMPTY", ensures="unique(result) in END,FRONT"),
		@Perm(requires="immutable(this) in NONEMPTY", ensures="unique(result) in HASNEXT,FRONT"),
		@Perm(requires="immutable(this)", ensures="unique(result)")
	})
	public final ListIterator<T> listIterator(final int index) {
		if( index < 0 )
			throw new IndexOutOfBoundsException();

		@Refine({
			@States(dim="NEXT", value={"HASNEXT","END"}),
			@States(dim="PREV", value={"HASPREV", "FRONT"})
		})
		@ClassStates({@State(name="alive", 
				             inv="immutable(curElement) * immutable(startingPoint)"),
				      @State(name="HASNEXT",
				    		 inv="curElement in NONEMPTY"),
				      @State(name="END",
				    		 inv="curElement in EMPTY")
				      // These invariants do not work, unfortunately
				      //@State(name="FRONT", inv="curElement==startingPoint")
				      //@State(name="HASPREV", inv="curElement != startingPoint")
		})
		class ConsListIterator<S> implements ListIterator<S> {

			private ConsList<S> curElement;
			private final ConsList<S> startingPoint;
			private int curIndex = index;

			@Cases({
				@Perm(requires="immutable(#0) in EMPTY", ensures="unique(this!fr) in END,FRONT"),
				@Perm(requires="immutable(#0) in NONEMPTY", ensures="unique(this!fr) in HASNEXT,FRONT"),
				@Perm(requires="immutable(#0)", ensures="unique(this!fr)")
			})
			public ConsListIterator(ConsList<S> curElement) {
				this.curElement = curElement;
				this.startingPoint = curElement;
			}
			
			@Override
			@Pure(fieldAccess=true)
			@TrueIndicates("HASNEXT")
			@FalseIndicates("END")
			public boolean hasNext() {
				if( curElement.isEmpty() )
					return false;
				else
					return true;
			}

			@Override
			@TrueIndicates("HASPREV")
			@FalseIndicates("FRONT")
			@Pure(fieldAccess=true)
			public boolean hasPrevious() {
				return curElement != startingPoint;
			}

			@Override
			@Full(fieldAccess=true, requires="HASNEXT")
			@ResultImm
			public S next() {
				S hd = curElement.hd();
				curElement = curElement.tl();
				curIndex++;
				return hd;
			}

			@Override
			@Pure
			public int nextIndex() {
				return curIndex + 1;
			}

			@Override
			@Full(fieldAccess=true, requires="HASPREV")
			@ResultImm
			public S previous() {
				if( this.curIndex == 0 )
					throw new NoSuchElementException();
				
				curElement = startingPoint.subListSameTail(curIndex - 1);
				curIndex--;

				// NEB: Fails because of an invariant that I cannot specify:
				// If we have a previous element, then 
				// subListSameTail(curIndex - 1) must be non-empty.
				return curElement.hd();
			}

			@Override
			@Pure
			public int previousIndex() {
				return curIndex - 1;
			}

			@Override
			@Full(requires="IMPOSSIBLE", ensures="IMPOSSIBLE")
			public void remove() {
				impossible();
			}

			@Override
			@Full(requires="IMPOSSIBLE", ensures="IMPOSSIBLE")
			public void set(S e) {
				impossible();
			}
			
			@Override
			@Full(requires="IMPOSSIBLE", ensures="IMPOSSIBLE")
			public void add(S e) {
				impossible();
			}
		}
		
		return new ConsListIterator<T>(this.subListSameTail(index));
	}
}

final class Empty<T> extends ConsList<T> {

	@Perm(ensures="unique(this!fr) in EMPTY")
	public Empty() {
		
	}
	
	@Override
	public T hd() {
		throw new IndexOutOfBoundsException();
	}

	@Override
	public int indexOf(Object o) {
		return -1;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public int lastIndexOf(Object o) {
		return -1;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public ConsList<T> tl() {
		return empty();
	}

	@Override
	protected int indexOfHelper(int cur_index, Object o) {
		return -1;
	}

	@Override
	protected int lastIndexOfHelper(boolean found, int cur_index, int cur_last, Object o) {
		if( found )
			return cur_last;
		else
			return -1;
	}
	
	@Override
	public String toString() {
		return "Nil";
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return c.isEmpty();
	}
}

@ClassStates(@State(name="alive", inv="immutable(hd) * immutable(tl)"))
final class Nonempty<T> extends ConsList<T> {

	private final T hd;
	private final ConsList<T> tl;
	private final int size;
	
	@Perm(requires="immutable(#0) * immutable(#1)",
		  ensures="unique(this!fr) in NONEMPTY")
	public Nonempty(T hd, ConsList<T> tl) {
		if( hd == null )
			throw new IllegalArgumentException("ConsList does not accept null elements.");
		
		this.hd = hd;
		this.tl = tl;
		this.size = tl.size() + 1;
	}

	@Override
	@Pure(fieldAccess=true, requires="NONEMPTY", ensures="NONEMPTY")
	@ResultImm
	public T hd() {
		return hd;
	}

	@Override
	@Pure
	protected int indexOfHelper(int cur_index, Object o) {
		if( this.hd().equals(o) ) { // NEB: We need to somehow know this subclass is always in NONEMPTY
			return cur_index;
		}
		else {
			return this.tl().indexOfHelper(cur_index + 1, o);
		}
	}
	
	@Override
	public int indexOf(Object o) {
		return indexOfHelper(0, o);
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public int lastIndexOf(Object o) {
		return lastIndexOfHelper(false, 0, 0, o);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	@Pure(fieldAccess = true)
	@ResultImm
	public ConsList<T> tl() {
		return tl;
	}

	@Override
	protected int lastIndexOfHelper(boolean found, int cur_index, int cur_last, Object o) {
		if( this.hd().equals(o) ) { // NEB: We need to somehow know this subclass is always in NONEMPTY 
			return this.tl().lastIndexOfHelper(true, cur_index + 1, cur_index, o);
		}
		else {
			return this.tl().lastIndexOfHelper(found, cur_index + 1, cur_last, o);
		}
	}

	@Override
	@Pure
	public String toString() {
		return "(" + this.hd().toString() + ")::" + this.tl().toString(); // NEB: We need to somehow know this subclass is always in NONEMPTY
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hd == null) ? 0 : hd.hashCode());
		result = prime * result + size;
		result = prime * result + ((tl == null) ? 0 : tl.hashCode());
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Pure
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Nonempty other = (Nonempty) obj;
		if (hd == null) {
			if (other.hd != null)
				return false;
		} else if (!hd.equals(other.hd))
			return false;
		if (size != other.size)
			return false;
		if (tl == null) {
			if (other.tl != null)
				return false;
		} else if (!tl.equals(other.tl))
			return false;
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// Here's where I get lazy...
		return (new ArrayList<T>(this)).containsAll(c);
	}
}
