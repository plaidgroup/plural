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
package edu.cmu.cs.plural.perm.parser;

/**
 * Represents a boolean literal in a state invariant. Use static methods to
 * return the singleton objects.
 * 
 * @author Nels Beckman
 * @date Apr 22, 2008
 *
 */
public class BoolLiteral implements PrimaryExpr {

	private final boolean value;
	
	private static final BoolLiteral TRUE_INSTANCE = new BoolLiteral(true);
	private static final BoolLiteral FALSE_INSTANCE = new BoolLiteral(false);
	
	private BoolLiteral(boolean v) {
		value = v;
	}

	public static PrimaryExpr getTrueInstance() {
		return TRUE_INSTANCE;
	}

	public static PrimaryExpr getFalseInstance() {
		return FALSE_INSTANCE;
	}

	public boolean hasValue(boolean other) {
		return value == other;
	}

	@Override
	public <T> T dispatch(PrimaryExprVisitor<T> visitor) {
		return visitor.visitBool(this);
	}
}
