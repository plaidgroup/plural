package edu.cmu.cs.plural.perm.parser;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.concrete.ImplicationResult;
import edu.cmu.cs.plural.concrete.VariablePredicate;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.ParamInfoHolder.InfoHolderPredicate;
import edu.cmu.cs.plural.pred.PredicateChecker.SplitOffTuple;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;

/**
 * I think this old comment below is actually out-dated. This implication appears
 * to allow any form of implication we want, including things that imply
 * permissions. This was not true before.<br>
 * <br>
 * This class allows us to have implications in the post-conditions of methods.
 * It is the implication itself, and was created instead of using the
 * existing implications in the plural.concrete package. 
 */
class ImplicationOfAPermission implements Implication, ImplicationResult {
	
	private InfoHolderPredicate ant;
	private List<InfoHolderPredicate> cons;

	/**
	 * Creates an implication with the given antecedent and consequence.
	 * @param ant
	 * @param cons
	 */
	public ImplicationOfAPermission(InfoHolderPredicate ant,
			List<InfoHolderPredicate> cons) {
		super();
		this.ant = ant;
		this.cons = Collections.unmodifiableList(cons);
	}

	@Override
	public ImplicationOfAPermission createCopyWithNewAntecedant(Aliasing other) {
		return new ImplicationOfAPermission(ant.createIdenticalPred(other), cons);
	}

	@Override
	public ImplicationOfAPermission createCopyWithOppositeAntecedant(Aliasing other) {
		return new ImplicationOfAPermission(ant.createOppositePred(other), cons);
	}

	@Override
	public ImplicationOfAPermission createCopyWithoutTemporaryState() {
		List<InfoHolderPredicate> newPs = new LinkedList<InfoHolderPredicate>();
		for(InfoHolderPredicate p : cons) {
			p = p.createCopyWithoutTemporaryState();
			if(p != null)
				newPs.add(p);
		}
		if(newPs.isEmpty())
			return null; // all dropped...
		return new ImplicationOfAPermission(ant, newPs);
	}

	@Override
	public VariablePredicate getAntecedant() {
		return ant;
	}

	@Override
	public boolean hasTemporaryState() {
		for(InfoHolderPredicate p : cons) {
			if(p.hasTemporaryState())
				return true;
		}
		return false;
	}

	@Override
	public ImplicationResult result() {
		return this;
	}

	@Override
	public boolean isSatisfied(PluralTupleLatticeElement value) {
		final Aliasing anteVar = ant.getVariable();
		if(value.isKnownImplication(anteVar, this))
			return true;
		
		if(ant.isUnsatisfiable(value))
			// antecedent is false --> implication trivially holds
			return true;
		
		for(InfoHolderPredicate p : cons) {
			if(! p.isSatisfied(value))
				return false;
		}
		return true;
	}

	@Override
	public PluralTupleLatticeElement putResultIntoLattice(
			PluralTupleLatticeElement value) {
		ant.removeFromLattice(value);
		value.removeImplication(ant.getVariable(), this);
		for(InfoHolderPredicate p : cons) {
			p.putIntoLattice(value);
		}
		return value;
	}
	
	@Override
	public String toString() {
		return ant + " implies TENS " + cons;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ant == null) ? 0 : ant.hashCode());
		result = prime * result + ((cons == null) ? 0 : cons.hashCode());
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
		ImplicationOfAPermission other = (ImplicationOfAPermission) obj;
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

	@Override
	public Set<Aliasing> getConsequenceVariables() {
		HashSet<Aliasing> result = new HashSet<Aliasing>();
		for(InfoHolderPredicate p : cons) {
			result.add(p.getVariable());
		}
		return result;
	}

	@Override
	public boolean splitOffResult(SplitOffTuple tuple) {
		for( InfoHolderPredicate info_holder : this.cons ) {
			if( !info_holder.splitOff(tuple) )
				return false;
		}
		return true;
	}

	@Override
	public boolean isImpliedBy(Implication impl) {
		// if one can be split off by the other...
		if( impl.getAntecedant().equals(this.getAntecedant()) ) {
			if( impl instanceof ImplicationOfAPermission ) {
				ImplicationOfAPermission impl_ = (ImplicationOfAPermission)impl;
				
				Map<Aliasing, FractionalPermissions> var_to_our_perms =
					new LinkedHashMap<Aliasing, FractionalPermissions>();

				// From our set of permissions, build of map of var to perm
				for( InfoHolderPredicate ifp : this.cons ) {
					if( var_to_our_perms.containsKey(ifp.getVariable()) ) {
						PermissionSetFromAnnotations p_set = ifp.getPerms();
						var_to_our_perms.get(ifp.getVariable()).mergeIn(p_set);
					}
					else {
						PermissionSetFromAnnotations p_set = ifp.getPerms();
						var_to_our_perms.put(ifp.getVariable(), p_set.toLatticeElement());
					}
				}
				
				// Now, for each var, see if we can split the other impl
				// off of what we have
				for( InfoHolderPredicate ifp : impl_.cons ) {
					FractionalPermissions perm = var_to_our_perms.get(ifp.getVariable());
					FractionalPermissions result = perm.splitOff(ifp.getPerms());
					var_to_our_perms.put(ifp.getVariable(), result);
				}
				
				// Go every permission left and make sure it's still sat
				for( FractionalPermissions perm : var_to_our_perms.values() ) {
					if( perm.isUnsatisfiable() )
						return false;
				}
				return true;
			}
		}
		return false;
	}	
}