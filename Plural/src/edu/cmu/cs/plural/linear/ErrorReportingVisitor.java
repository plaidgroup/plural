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
package edu.cmu.cs.plural.linear;

import java.util.LinkedHashSet;

import edu.cmu.cs.plural.contexts.ContextAllLE;
import edu.cmu.cs.plural.contexts.ContextChoiceLE;
import edu.cmu.cs.plural.contexts.DisjunctiveLE;
import edu.cmu.cs.plural.contexts.FailingPackContext;
import edu.cmu.cs.plural.contexts.LinearContextLE;
import edu.cmu.cs.plural.contexts.TensorPluralTupleLE;
import edu.cmu.cs.plural.contexts.TrueContext;

/**
 * Simple visitor to find errors according to the semantics of disjunctive
 * contexts and generate an appropriate error message where needed.  
 * Errors are discarded if a tuple with no errors is found
 * in a {@link ContextChoiceLE}.
 * @author Kevin Bierhoff
 * @since 4/24/2008
 */
public abstract class ErrorReportingVisitor extends DisjunctiveVisitor<String> {

	/**
	 * Formats a error string from the given error enumeration, by inserting
	 * the given separator between the individual errors.
	 * @param errors Errors to format, never <code>null</code>.
	 * @param separator String to insert between errors (including whitespace).
	 * @return a error string from the given error enumeration, empty if
	 * <code>errors</code> is empty.
	 */
	public static String errorString(Iterable<String> errors, String separator) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for(String s : errors) {
			if(first)
				first = false;
			else
				result.append(separator);
			result.append(s);
		}
		return result.toString();
	}

	/**
	 * Check an individual tuple with this method.
	 * @param tuple
	 * @return An error message or <code>null</code> if no errors were found.
	 */
	public abstract String checkTuple(TensorPluralTupleLE tuple);

	@Override
	public String context(LinearContextLE le) {
		return checkTuple(le.getTuple());
	}

	@Override
	public String all(ContextAllLE le) {
		LinkedHashSet<String> errors = new LinkedHashSet<String>();
		// succeed for empty (false) conjunction: false can prove anything
		for(DisjunctiveLE e : le.getElements()) {
			String error = e.dispatch(this);
			// more complete error message: find *all* failing contexts,
			// not just the first one
			if(error != null)
				errors.add(error); 
		}
		if(errors.isEmpty())
			return null;
		String result = errorString(errors, " AND ");
		if(errors.size() > 1)
			return "{ " + result + " }";
		else
			return result;
	}

	@Override
	public String choice(ContextChoiceLE le) {
		if(le.getElements().isEmpty())
			// fail for empty (true) choice: true cannot prove anything
			// TODO Suppress these warnings, because they result from previous failure?
			return "No available context--usually due to a previous failure or error during packing/unpacking";
		LinkedHashSet<String> errors = new LinkedHashSet<String>();
		for(DisjunctiveLE e : le.getElements()) {
			String error = e.dispatch(this);
			if(error == null)
				return null;
			errors.add(error);
		}
		assert ! errors.isEmpty() : "Shouldn't be able to get here without actual errors";
		String result = errorString(errors, " OR ");
		if(errors.size() > 1)
			return "[ " + result + " ]";
		else
			return result;
	}

	@Override
	public String trueContext(TrueContext trueContext) {
		// Is it horrible to have a instanceof check inside of a visitor?
		// I didn't want to have an extra method for every other visitor,
		// since none of them care about this context type.
		if( trueContext instanceof FailingPackContext ) {
			FailingPackContext fail = (FailingPackContext)trueContext;
			String state = fail.failingState();
			String inv = fail.failingInvariant();
			return "Previously attempted to pack to " + state + " but could not" +
					" because the following invariant was unsatisfiable: " +
					inv;
		}
		else {
			// fail for empty (true) choice: true cannot prove anything
			return "No available context--usually due to a previous failure or error during packing/unpacking";
		}		
	}
}