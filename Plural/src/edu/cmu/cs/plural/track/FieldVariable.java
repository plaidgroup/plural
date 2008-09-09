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
package edu.cmu.cs.plural.track;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import edu.cmu.cs.crystal.tac.IVariableVisitor;
import edu.cmu.cs.crystal.tac.Variable;

/**
 * You probably shouldn't be using this class... 
 * Used internally to track fields in the plural analysis.
 * 
 * @author Nels Beckman
 * @date Feb 26, 2008
 *
 */
public class FieldVariable extends Variable {

	final IVariableBinding fieldDecl;
	
	private FieldVariable() {
		super();
		fieldDecl = null;
	}
	
	public FieldVariable(IVariableBinding fieldDecl) {
		super();
		assert fieldDecl != null;
		this.fieldDecl = fieldDecl.getVariableDeclaration();
	}

	@Override
	public <T> T dispatch(IVariableVisitor<T> visitor) {
		throw new RuntimeException("This class should never be visited bc it should not " +
				"appear in an AST.");
	}
	
	/**
	 * Returns the declared name of the field.
	 * @return The declared name of the field.
	 */
	public String getFieldName() {
		return fieldDecl.getName();
	}
	
	@Override
	public String getSourceString() {
		return fieldDecl.getName();
	}

	@Override
	public String toString() {
		return fieldDecl.getName();
	}

	@Override
	public ITypeBinding resolveType() {
		return fieldDecl.getType();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fieldDecl == null) ? 0 : fieldDecl.hashCode());
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
		final FieldVariable other = (FieldVariable) obj;
		if (fieldDecl == null) {
			if (other.fieldDecl != null)
				return false;
		} else if (!fieldDecl.equals(other.fieldDecl))
			return false;
		return true;
	}

	
}
