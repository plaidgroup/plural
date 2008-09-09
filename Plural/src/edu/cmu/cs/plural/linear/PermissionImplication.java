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
package edu.cmu.cs.plural.linear;

import java.util.LinkedList;
import java.util.List;

import javax.jws.soap.SOAPBinding.ParameterStyle;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.concrete.ImplicationResult;
import edu.cmu.cs.plural.concrete.VariablePredicate;
import edu.cmu.cs.plural.perm.parser.ParamInfoHolder;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;
import edu.cmu.cs.plural.util.Pair;

/**
 * @author Kevin Bierhoff
 * @since 8/04/2008
 */
public class PermissionImplication implements Implication {
	
	private final PermissionPredicate ant;
	private final List<Pair<Aliasing, ParamInfoHolder>> cons;
	
	public PermissionImplication(PermissionPredicate ant, List<Pair<Aliasing, ParamInfoHolder>> cons) {
		this.ant = ant;
		this.cons = cons;
	}

	@Override
	public Implication createCopyWithNewAntecedant(Aliasing other) {
		return new PermissionImplication((PermissionPredicate) ant.createIdenticalPred(other), cons);
	}

	@Override
	public Implication createCopyWithOppositeAntecedant(Aliasing other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public VariablePredicate getAntecedant() {
		return ant;
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
	public ImplicationResult result() {
		return new ImplicationResult() {

			@Override
			public PluralTupleLatticeElement putResultIntoLattice(
					PluralTupleLatticeElement value) {
				ant.removeFromLattice(value);
				value.removeImplication(ant.getVariable(), PermissionImplication.this);
				for(Pair<Aliasing, ParamInfoHolder> c : cons) {
					c.snd().putIntoLattice(value, c.fst());
				}
				return value;
			}
			
		};
	}

	@Override
	public boolean isSatisfied(PluralTupleLatticeElement value) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ant == null) ? 0 : ant.hashCode());
		result = prime * result + ((cons == null) ? 0 : cons.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PermissionImplication other = (PermissionImplication) obj;
		if (ant == null) {
			if (other.ant != null)
				return false;
		} else if (!ant.equals(other.ant))
			return false;
		if (cons == null) {
			if (other.cons != null)
				return false;
		} else if (!cons.equals(other.cons))
			return false;
		return true;
	}

	/**
	 * @param paramLoc
	 * @return
	 */
	public List<ParamInfoHolder> findImpliedParameter(Aliasing paramLoc) {
		List<ParamInfoHolder> result = new LinkedList<ParamInfoHolder>();
		for(Pair<Aliasing, ParamInfoHolder> c : cons) {
			if(c.fst().equals(paramLoc))
				result.add(c.snd());
		}
		return result;
	}

	@Override
	public Implication createCopyWithoutTemporaryState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasTemporaryState() {
		for(Pair<Aliasing, ParamInfoHolder> param : cons) {
			if(! param.snd().getStateInfos().isEmpty())
				return true;
		}
		return false;
	}

}