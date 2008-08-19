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
 * Represents P_1 & P_2, pronounced 'with.' I really need a better name for
 * this class.
 * 
 * @author Nels Beckman
 * @date Mar 26, 2008
 */
public class Withing implements AccessPred {

	private final AccessPred P_1;
	private final AccessPred P_2;
	
	public Withing(AccessPred P_1, AccessPred P_2) {
		this.P_1 = P_1;
		this.P_2 = P_2;
	}
	
	@Override
	public <T> T accept(AccessPredVisitor<T> visitor) {
		return visitor.visit(this);
	}

	public AccessPred getP1() {
		return P_1;
	}

	public AccessPred getP2() {
		return P_2;
	}
	
	@Override
	public String toString() {
		return P_1 + " & " + P_2;
	}
}
