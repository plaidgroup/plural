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
package edu.cmu.cs.plural.test;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import edu.cmu.cs.plural.util.ConsList;
import static edu.cmu.cs.plural.util.ConsList.cons;
import static edu.cmu.cs.plural.util.ConsList.list;
import edu.cmu.cs.plural.util.Lambda2;

public class ConsListTest {

	@Test
	public void testEmpty() {
		
		ConsList<Integer> l = ConsList.empty();
		
		assertTrue(l.isEmpty());
		assertTrue(l.tl().isEmpty());
		assertEquals(l.size(), 0);
	}

	@Test
	public void testSingleton() {
		ConsList<Integer> l = ConsList.singleton(4);
		
		assertFalse(l.isEmpty());
		assertEquals(l.size(), 1);
		assertEquals(l.hd(), 4);
	}

	@Test
	public void testCons() {
		ConsList<Integer> l = ConsList.singleton(1);
		
		l = cons(2, l);
		l = cons(3, l);
		l = cons(4, l);
		
		assertEquals(l.size(), 4);
		assertFalse(l.isEmpty());
		assertEquals(l.hd(), 4);
	}

	@Test
	public void testTl() {
		ConsList<Integer> l = ConsList.singleton(1);
		ConsList<Integer> l2 = cons(2, l);
		
		assertEquals(l, l2.tl());
		assertEquals(l2.tl().size(), 1);
	}

	@Test
	public void testRemoveElement() {
		ConsList<Integer> l = ConsList.singleton(1);
		assertTrue(l.removeElement(1).isEmpty());
		assertEquals(l.removeElement(1).size(),0);
		
		l = cons(2, l);
		l = cons(3, l);
		l = cons(1, l);
		l = cons(1, l);
		l = l.removeElement(1);
		assertEquals(l.size(), 2);
	}

	@Test
	public void testIterator() {
		ConsList<Integer> l = ConsList.list(5,4,3,2,1);
		
		Iterator<Integer> iter = l.iterator();
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 5);
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 4);
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 3);
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 2);
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 1);
		
		assertFalse(iter.hasNext());
	}

	@Test
	public void testFoldl() {
		ConsList<Integer> l = list(5,4,3,2,1);
		
		Integer result =
		l.foldl(new Lambda2<Integer,Integer,Integer>(){
			@Override
			public Integer call(Integer i1, Integer i2) {
				return i1 + i2;
			}}, 0);
		assertEquals(result, 15);
	}
	
	@Test
	public void testList() {
		ConsList<Integer> l = list(1,2,3,4,5);
		
		Iterator<Integer> iter = l.iterator();
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 1);
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 2);
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 3);
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 4);
		
		assertTrue(iter.hasNext());
		assertEquals(iter.next(), 5);
		
		assertFalse(iter.hasNext());
	}
	
	public void testContains() {
		ConsList<Integer> l = list(4,5,6,7);
		
		assertTrue(l.contains(6));
		assertFalse(l.contains(9));
	}
}
