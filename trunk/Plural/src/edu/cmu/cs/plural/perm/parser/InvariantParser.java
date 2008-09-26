package edu.cmu.cs.plural.perm.parser;

import java.util.Collections;
import java.util.Map;

import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.pred.InvariantMergerChecker;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * This class will create permissions from field invariants. It will both
 * parse the string and then populate the call back that you give it.
 */
public class InvariantParser extends AbstractParamVisitor implements
		AccessPredVisitor<Boolean>, InvariantMergerChecker {

	static InvariantParser createUnpackInvariantParser(SimpleMap<String, StateSpace> spaces) {
		return new InvariantParser(spaces, true);
	}
	
	static InvariantParser createPackInvariantParser(SimpleMap<String,StateSpace> spaces) {
		return new InvariantParser(spaces, false);
	}
	
	private InvariantParser(SimpleMap<String, StateSpace> spaces,
			boolean namedFractions) {
		super(Collections.<String,PermissionSetFromAnnotations>emptyMap(), 
				spaces, false, namedFractions);
	}

	@Override
	protected AbstractParamVisitor createSubParser(boolean namedFraction) {
		return new InvariantParser(getSpaces(), namedFraction);
	}

}
