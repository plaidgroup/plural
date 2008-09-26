package edu.cmu.cs.plural.concrete;

import java.util.Set;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;

/**
 * Next in a series of rather limited implications is a implication
 * that produces permissions when it is eliminated.
 * 
 * @author Nels E. Beckman
 * @since Sep 24, 2008
 */
public class PermissionImplication implements Implication {

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
	public VariablePredicate getAntecedant() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasTemporaryState() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean match(VariablePredicate pred) {
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSatisfied(PluralTupleLatticeElement value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<Aliasing> getConsequenceVariables() {
		// TODO Auto-generated method stub
		return null;
	}

}
