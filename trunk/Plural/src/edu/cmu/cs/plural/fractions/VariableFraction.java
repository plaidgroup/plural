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
package edu.cmu.cs.plural.fractions;

import edu.cmu.cs.plural.fractions.elim.NormalizedFractionVisitor;

/**
 * @author Kevin Bierhoff
 *
 */
public class VariableFraction extends Fraction {
	
	private static long nextID = 0;
	private String varName;
	private Fraction value;
	
	public VariableFraction() {
		this.varName = "VAR" + (nextID++);
	}

	public String getVarName() {
		return varName;
	}
	
	public boolean isSolved() {
		return value != null;
	}

	public Fraction getValue() {
		return value;
	}

	public void setValue(Fraction value) {
		this.value = value;
	}

	@Override
	public boolean isVariable() {
		return true;
	}

	@Override
	public <T> T dispatch(FractionVisitor<T> visitor) {
		return visitor.var(this);
	}

	@Override
	public <T> T dispatch(NormalizedFractionVisitor<T> visitor) {
		return visitor.var(this);
	}

	@Override
	public String toString() {
		if(isSolved())
			return getVarName() + "[" + getValue() + "]";
		return getVarName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((varName == null) ? 0 : varName.hashCode());
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
		final VariableFraction other = (VariableFraction) obj;
		if (varName == null) {
			if (other.varName != null)
				return false;
		} else if (!varName.equals(other.varName))
			return false;
		return true;
	}

}
