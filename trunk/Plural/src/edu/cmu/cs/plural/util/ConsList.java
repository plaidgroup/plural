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

import edu.cmu.cs.plural.concrete.Implication;

/**
 * An immutable cons list. Because this list cannot be modified, many
 * lists can share the same elements. Many operations must traverse, 
 * and therefore run in linear time. Adding things to the front of the
 * list is done in constant time. Implements all methods of the {@code List}
 * interface, except those which are optional because they modify the
 * list in place.
 * 
 * @author Nels E. Beckman
 * @since Sep 8, 2008
 *
 * @param <T> The type of elements this list holds.
 */
public abstract class ConsList<T> implements List<T> {

	private final static Empty EMPTY_CONS_LIST = new Empty();
	
	/*
	 * ConsList methods 
	 */
	
	/**
	 * Create a new, empty list.
	 */
	public static <T> ConsList<T> empty() {
		return EMPTY_CONS_LIST;
	}
	
	/**
	 * Create a list with one element.
	 */
	public static <T> ConsList<T> singleton(T hd) {
		return new Nonempty<T>(hd, ConsList.<T>empty());
	}
	
	/**
	 * Create a new list from this list with {@code hd} at
	 * the front.
	 */
	public ConsList<T> cons(T hd) {
		return new Nonempty<T>(hd, this);
	}
	
	/**
	 * Get the first element of this list.
	 */
	public abstract T hd();
	
	/**
	 * Return this list without the first element.
	 */
	public abstract ConsList<T> tl();
	
	/**
	 * Removes every element in the list where 
	 * {@code hd().equals(t) == true}.
	 */
	public ConsList<T> removeElement(T t) {
		if( this.isEmpty() ) {
			return this;
		}
		else if( this.hd().equals(t) ) {
			return this.tl().removeElement(t);
		}
		else {
			return this.tl().removeElement(t).cons(hd());
		}
	}
	
	/**
	 * Removes the first element in the list where
	 * {@code hd().equals(t) == true}.
	 */
	public ConsList<T> removeElementOnce(T t) {
		if( this.isEmpty() ) {
			return this;
		}
		else if( this.hd().equals(t) ) {
			return this.tl();
		}
		else {
			return this.tl().removeElementOnce(t).cons(hd());
		}
	}
	
	/**
	 * Given a 'first-class function' that takes elements of type
	 * T and returns elements of type O, call that function on
	 * every element of {@code this} list, returning a new list
	 * of Os. 
	 */
	public <O> ConsList<O> map(Lambda<? super T, ? extends O> lam) {
		if( this.isEmpty() ) {
			return empty();
		}
		else {
			O new_hd = lam.call(this.hd());
			return this.tl().map(lam).cons(new_hd);
		}
	}
	
	/**
	 * Given a 'first-class function' that takes elements of type
	 * T and returns a boolean, call that function on every element
	 * of {@code this} list, returning a new list that only contains
	 * the elements for which the function call returned true.
	 */
	public ConsList<T> filter(Lambda<? super T,? extends Boolean> lam) {
		if( this.isEmpty() ) {
			return this;
		}
		else {
			T hd = this.hd();
			if( lam.call(hd) )
				return this.tl().filter(lam).cons(hd);
			else
				return this.tl().filter(lam);
		}
	}
	
	/**
	 * Fold over the elements of this list.
	 */
	public <O> O foldl(Lambda2<? super T,? super O,? extends O> lam, O o) {
		if( this.isEmpty() ) {
			return o;
		}
		else {
			T hd = this.hd();
			O new_o = lam.call(hd, o);
			return this.tl().foldl(lam, new_o);
		}
	}
	
	protected abstract int indexOfHelper(int cur_index, Object o);
	
	protected abstract int lastIndexOfHelper(boolean found, int cur_index, int cur_last, Object o);
	
	/*
	 * List methods
	 */
	
	public abstract int size();	
	
	public abstract boolean isEmpty();
	
	public abstract int indexOf(Object o);
	
	public abstract int lastIndexOf(Object o);
	
	/**
	 * Note: For {@code ConsList<T>}, this method is less efficient
	 * than {@code iterator()} and should only be used if iterating
	 * in the reverse direction is really important. In particular,
	 * calling the {@code previous()} method is linear in the size
	 * of the list.
	 */
	public ListIterator<T> listIterator(final int index) {
		if( index < 0 )
			throw new IndexOutOfBoundsException();
		
		return new ListIterator<T>(){
			private ConsList<T> cur_element = ConsList.this.subListSameTail(index);
			private int cur_index = index;
			
			@Override
			public void add(T e) {
				impossible();
			}

			@Override
			public boolean hasNext() {
				return !cur_element.isEmpty();
			}

			@Override
			public boolean hasPrevious() {
				return cur_element != ConsList.this;
			}

			@Override
			public T next() {
				T hd = cur_element.hd();
				cur_element = cur_element.tl();
				cur_index++;
				return hd;
			}

			@Override
			public int nextIndex() {
				return cur_index + 1;
			}

			@Override
			public T previous() {
				if( this.cur_index == 0 )
					throw new NoSuchElementException();
				
				cur_element = ConsList.this.subListSameTail(cur_index - 1);
				cur_index--;
				
				return cur_element.hd();
			}

			@Override
			public int previousIndex() {
				return cur_index - 1;
			}

			@Override
			public void remove() {
				impossible();
			}

			@Override
			public void set(T e) {
				impossible();
			}};
	}
	
	/**
	 * Note: For {@code ConsList<T>}, this method is less efficient
	 * than {@code iterator()} and should only be used if iterating
	 * in the reverse direction is really important. In particular,
	 * calling the {@code previous()} method is linear in the size
	 * of the list.
	 */
	public ListIterator<T> listIterator() {
		return listIterator(0);
	}
	
	public T get(int index) {
		if( index == 0 ) {
			return hd();
		}
		else {
			return tl().get(index - 1);
		}
	}

	public Iterator<T> iterator() {
		return new Iterator<T>() {

			private ConsList<T> cur_list = ConsList.this;
			
			public boolean hasNext() {
				return !cur_list.isEmpty();
			}

			public T next() {
				T hd = cur_list.hd();
				cur_list = cur_list.tl();
				return hd;
			}

			public void remove() {
				impossible();
			}};
	}

	public boolean containsAll(Collection<?> c) {
		// Here's where I get lazy...
		return (new ArrayList<T>(this)).containsAll(c);
	}
	
	/**
	 * Will share the back of the list, if the sublist we are asking for
	 * only cuts off part of the front.
	 */
	private ConsList<T> subListSameTail(int fromIndex) {
		if( fromIndex == 0 ) {
			return this;
		}
		else {
			return subListSameTail(fromIndex - 1);
		}
	}
	
	public ConsList<T> subList(int fromIndex, int toIndex) {
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
			return this.tl().subList(0, toIndex - 1).cons(hd());
		}
	}
	
	public boolean contains(Object o) {
		if( hd().equals(o) ) {
			return true;
		}
		else if( this.isEmpty() ) {
			return false;
		}
		else {
			return tl().contains(o);
		}
	}

	public Object[] toArray() {
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

	public <S> S[] toArray(S[] a) {
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
	
	public void add(int index, T element) {
		impossible();
	}

	public boolean add(T e) {
		return impossible();
	}

	public boolean addAll(Collection<? extends T> c) {
		return impossible();
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		return impossible();
	}

	public void clear() {
		impossible();		
	}
	
	public T remove(int index) {
		return impossible();
	}

	public boolean remove(Object o) {
		return impossible();
	}

	public boolean removeAll(Collection<?> c) {
		return impossible();
	}

	public boolean retainAll(Collection<?> c) {
		return impossible();
	}

	public T set(int index, T element) {
		return impossible();
	}
}

final class Empty<T> extends ConsList<T> {

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
		return this;
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
}

final class Nonempty<T> extends ConsList<T> {

	private final T hd;
	private final ConsList<T> tl;
	private final int size;
	
	public Nonempty(T hd, ConsList<T> tl) {
		if( hd == null )
			throw new IllegalArgumentException("ConsList does not accept null elements.");
		
		this.hd = hd;
		this.tl = tl;
		this.size = tl.size() + 1;
	}

	@Override
	public T hd() {
		return hd;
	}

	@Override
	protected int indexOfHelper(int cur_index, Object o) {
		if( this.hd().equals(o) ) {
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
	public ConsList<T> tl() {
		return tl;
	}

	@Override
	protected int lastIndexOfHelper(boolean found, int cur_index, int cur_last, Object o) {
		if( this.hd().equals(o) ) {
			return this.tl().lastIndexOfHelper(true, cur_index + 1, cur_index, o);
		}
		else {
			return this.tl().lastIndexOfHelper(found, cur_index + 1, cur_last, o);
		}
	}

	@Override
	public String toString() {
		return this.hd().toString() + "::" + this.tl().toString();
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

	@Override
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
}
