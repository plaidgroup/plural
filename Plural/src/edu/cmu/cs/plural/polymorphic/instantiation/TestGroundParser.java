/**
 * Copyright (C) 2007-2009 Carnegie Mellon University and others.
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

package edu.cmu.cs.plural.polymorphic.instantiation;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * @author nbeckman
 * @since Nov 19, 2009
 *
 */
public class TestGroundParser {

	/**
	 * Test method for {@link edu.cmu.cs.plural.polymorphic.instantiation.GroundParser#parse(java.lang.String)}.
	 */
	@Test
	public void testParse() {
		Option<GroundInstantiation> parse = GroundParser.parse("unique");
		assertTrue(parse.isSome());
		assertTrue(parse.unwrap().isVirtual());
		assertEquals(parse.unwrap().getKind(), PermissionKind.UNIQUE);
	}

	@Test
	public void testParse2() {
		Option<GroundInstantiation> parse = GroundParser.parse("immutable in Open");
		assertTrue(parse.isSome());
		assertTrue(parse.unwrap().isVirtual());
		assertEquals(parse.unwrap().getStates().length, 1);
		assertEquals(parse.unwrap().getStates()[0], "Open");
		assertEquals(parse.unwrap().getKind(), PermissionKind.IMMUTABLE);
	}
	@Test
	public void testParse3() {
		Option<GroundInstantiation> parse = GroundParser.parse("full in Open,Closed");
		assertTrue(parse.isSome());
		assertTrue(parse.unwrap().isVirtual());
		assertEquals(parse.unwrap().getStates().length, 2);
		assertEquals(parse.unwrap().getStates()[0], "Open"); // note the LACK of a comma! ;-)
		assertEquals(parse.unwrap().getKind(), PermissionKind.FULL);
	}
	@Test
	public void testParse4() {
		Option<GroundInstantiation> parse = GroundParser.parse("share(alive, vr) in Closed");
		assertTrue(parse.isSome());
		assertEquals(parse.unwrap().getStates().length, 1);
		assertEquals(parse.unwrap().getRoot(), "alive");
		assertTrue(parse.unwrap().isVirtual());
		assertFalse(parse.unwrap().isFrame());
		assertEquals(parse.unwrap().getKind(), PermissionKind.SHARE);
	}
	@Test
	public void testParse5() {
		assertTrue(GroundParser.parse("share(alive) in Closed").isNone());
	}
	@Test
	public void testParse6() {
		assertTrue(GroundParser.parse("share(r,alive, vr) in Closed").isNone());
	}
}
