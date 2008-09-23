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
package edu.cmu.cs.plural.perm.parser;

public class ParamReference implements RefExpr, PrimaryExpr {

	private final Integer paramPosition;
	private final String paramString;
	
	/**
	 * Creates a new parameter reference with the given string
	 * to specify a parameter.  The given string will be 
	 * converted to an integer; if the conversion fails then 
	 * {@link #getParamPosition()} returns <code>null</code>.
	 * @param paramString Non-<code>null</code> parameter string
	 * that will be converted to an integer.
	 * @see #getParamString()
	 */
	ParamReference(String paramString) {
		Integer pp;
		try {
			pp = Integer.valueOf(paramString);
		}
		catch(NumberFormatException nfe) {
			pp = null;
		}
		this.paramString = paramString;
		this.paramPosition = pp;
	}

	/**
	 * Returns the 0-based parameter position or <code>null</code> if
	 * the provided parameter string could not be parsed as an integer.
	 * @return the 0-based parameter position or <code>null</code>.
	 */
	Integer getParamPosition() {
		return paramPosition;
	}
	
	/**
	 * Returns the original string used to specify a parameter.
	 * This can be useful for error messages when the provided string
	 * could not be parsed as an integer.
	 * @return the original string used to specify a parameter.
	 */
	String getParamString() {
		return "#" + paramString;
	}

	@Override
	public <T> T dispatch(PrimaryExprVisitor<T> visitor) {
		return visitor.visitParam(this);
	}
}
