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
package edu.cmu.cs.plural.concrete;

import java.util.List;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;


/**
 * 
 * @author Kevin Bierhoff
 * @since 7/28/2008
 */
public class GenericImplication implements Implication {
	
	private VariablePredicate ant;

	public GenericImplication(VariablePredicate ant) {
		this.ant = ant;
	}

	@Override
	public VariablePredicate getAntecedant() {
		return ant;
	}

	@Override
	public Implication createCopyWithNewAntecedant(Aliasing other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Implication createCopyWithOppositeAntecedant(Aliasing other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Implication createCopyWithoutTemporaryState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasTemporaryState() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ImplicationResult result() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean supportsMatch() {
		return false;
	}

	@Override
	public boolean match(VariablePredicate pred) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSatisfied(PluralTupleLatticeElement value) {
		final Aliasing anteVar = ant.getVariable();
		if(value.isKnownImplication(anteVar, this))
			return true;
		
		if(ant.isUnsatisfiable(value))
			// antecedent is false --> implication trivially holds
			return true;
		
		return true;
	}

}
