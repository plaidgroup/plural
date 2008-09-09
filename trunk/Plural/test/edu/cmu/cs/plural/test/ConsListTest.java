package edu.cmu.cs.plural.test;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import edu.cmu.cs.plural.util.ConsList;
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
		
		l = l.cons(2);
		l = l.cons(3);
		l = l.cons(4);
		
		assertEquals(l.size(), 4);
		assertFalse(l.isEmpty());
		assertEquals(l.hd(), 4);
	}

	@Test
	public void testTl() {
		ConsList<Integer> l = ConsList.singleton(1);
		ConsList<Integer> l2 = l.cons(2);
		
		assertEquals(l, l2.tl());
		assertEquals(l2.tl().size(), 1);
	}

	@Test
	public void testRemoveElement() {
		ConsList<Integer> l = ConsList.singleton(1);
		assertTrue(l.removeElement(1).isEmpty());
		assertEquals(l.removeElement(1).size(),0);
		
		l = l.cons(2);
		l = l.cons(3);
		l = l.cons(1);
		l = l.cons(1);
		l = l.removeElement(1);
		assertEquals(l.size(), 2);
	}

	@Test
	public void testIterator() {
		ConsList<Integer> l = ConsList.<Integer>empty().cons(1).cons(2).cons(3).cons(4).cons(5);
		
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
		ConsList<Integer> l = ConsList.<Integer>empty().cons(1).cons(2).cons(3).cons(4).cons(5);
		
		Integer result =
		l.foldl(new Lambda2<Integer,Integer,Integer>(){
			@Override
			public Integer call(Integer i1, Integer i2) {
				return i1 + i2;
			}}, 0);
		assertEquals(result, 15);
	}
}
