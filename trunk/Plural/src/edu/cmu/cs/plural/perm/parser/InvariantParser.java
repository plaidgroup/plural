package edu.cmu.cs.plural.perm.parser;

import java.util.Collections;

import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.pred.InvariantMergerChecker;
import edu.cmu.cs.plural.states.StateSpace;

/**
 * This class will create permissions from field invariants. It will both
 * parse the string and then populate the call back that you give it.
 */
public class InvariantParser extends AbstractParamVisitor implements
		AccessPredVisitor<Boolean>, InvariantMergerChecker {

	static InvariantParser createUnpackInvariantParser(SimpleMap<String, StateSpace> spaces) {
		return new InvariantParser(spaces, FractionCreation.NAMED_EXISTENTIAL);
	}
	
	static InvariantParser createPackInvariantParser(SimpleMap<String,StateSpace> spaces) {
		return new InvariantParser(spaces, FractionCreation.VARIABLE_EXISTENTIAL);
	}
	
	private InvariantParser(SimpleMap<String, StateSpace> spaces,
			FractionCreation namedFractions) {
		super(Collections.<String,PermissionSetFromAnnotations>emptyMap(), 
				spaces, false, false, namedFractions);
	}

	@Override
	protected AbstractParamVisitor createSubParser(FractionCreation namedFraction) {
		return new InvariantParser(getSpaces(), namedFraction);
	}

}
