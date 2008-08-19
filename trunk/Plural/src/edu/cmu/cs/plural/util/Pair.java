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
package edu.cmu.cs.plural.util;


/**
 * @author Kevin Bierhoff
 *
 */
public class Pair<A, B> {
	
	private A component1;
	private B component2;

	public Pair() {
		super();
	}
	
	public Pair(A component1, B component2) {
		this.component1 = component1;
		this.component2 = component2;
	}

	public A fst() {
		return component1;
	}

	public void setComponent1(A component1) {
		this.component1 = component1;
	}

	public B snd() {
		return component2;
	}

	public void setComponent2(B component2) {
		this.component2 = component2;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Pair<A, B>(component1, component2);
	}

	@Override
	public String toString() {
		return "<" + component1 + "," + component2 + ">";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((component1 == null) ? 0 : component1.hashCode());
		result = prime * result
				+ ((component2 == null) ? 0 : component2.hashCode());
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
		final Pair<?, ?> other = (Pair<?, ?>) obj;
		if (component1 == null) {
			if (other.component1 != null)
				return false;
		} else if (!component1.equals(other.component1))
			return false;
		if (component2 == null) {
			if (other.component2 != null)
				return false;
		} else if (!component2.equals(other.component2))
			return false;
		return true;
	}

	public static <A, B> Pair<A, B> create(A component1, B component2) {
		return new Pair<A, B>(component1, component2);
	}

}
