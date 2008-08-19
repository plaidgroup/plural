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
package edu.cmu.cs.plural.perm.parser;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.tac.Variable;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * This code allows you to take a string and get back a list of fractional permissions.
 * 
 * @author Nels Beckman
 * @date Mar 26, 2008
 *
 */
public class PermParser {
	// Thread-local because static fields are bad for concurrency!
	// Plus it's just caching so there's no reason to be particularly worried.
	private static ThreadLocal<Pair<String, TopLevelPred>> cachedResult = new ThreadLocal<Pair<String, TopLevelPred>>() {
		private Pair<String, TopLevelPred> cachedResult = Pair.create("", null);
		@Override
		public Pair<String, TopLevelPred> get() {
			return this.cachedResult;
		}
		@Override
		public void set(Pair<String, TopLevelPred> value) {
			this.cachedResult = value;
		}
	};
	
	private static TopLevelPred parse(String str) throws RecognitionException {
		if( cachedResult.get().fst().equals(str) ) {
			return cachedResult.get().snd();
		}
		else {
			AccessPredLexer lex = new AccessPredLexer(new ANTLRStringStream(str));
			CommonTokenStream tokens = new CommonTokenStream(lex);

			AccessPredParser parser = new AccessPredParser(tokens);

			TopLevelPred parsed_pred = parser.start();
			/*
			 * Update cache.
			 */			
			cachedResult.set(Pair.create(str, parsed_pred));
			return parsed_pred;
		}
	}
	
	public static <T> T accept(String perm_string, AccessPredVisitor<T> visitor) throws RecognitionException {
		TopLevelPred parsed_pred = parse(perm_string);
		if(parsed_pred == null || parsed_pred instanceof TopLevelPred.Impossible)
			return null;
		return ((AccessPred) parsed_pred).accept(visitor);
	}
	
	/**
	 * Given a string from a permission annotation, this method will return a
	 * list of fractional permissions. For the time-being, this list represents
	 * conjunction because we cannot handle any other type of permission.
	 * 
	 * @param perm_string
	 * @return A list of variable/permission pairs.
	 * @throws RecognitionException on a parser error.
	 */
	public static List<Pair<String, PermissionFromAnnotation>> 
	parsePermissionsFromString(String perm_string, SimpleMap<String,StateSpace> stateInfos,
			boolean createNamedVariables) throws RecognitionException  {

		TopLevelPred parsed_pred = parse(perm_string);
        return 
        	(parsed_pred == null || parsed_pred instanceof TopLevelPred.Impossible ? 
        		Collections.<Pair<String, PermissionFromAnnotation>>emptyList() :
        		((AccessPred) parsed_pred).accept(new FieldFPVisitorConj(stateInfos, createNamedVariables)));
	}
	
	/**
	 * Given a string from a permission annotation, this method will return a list
	 * of variables that the string indicates must be null. Because of caching,
	 * repeated calls to this method on the same string do not re-parse, as long as
	 * the calls occur in a row. 
	 * 
	 * @param perm_string
	 * @param vars
	 * @return
	 */
	public static List<Variable>
	parseMustBeNullFromString(String perm_string, SimpleMap<String,Variable> vars) 
	throws RecognitionException {
		
		TopLevelPred parsed_pred = parse(perm_string);
		return 
			(parsed_pred == null || parsed_pred instanceof TopLevelPred.Impossible ?
					Collections.<Variable>emptyList() :
					((AccessPred) parsed_pred).accept(new NullVisitorConj(true, vars)));
	}
	
	/**
	 * Given a string from a permission annotation, this method will return a list
	 * of variables that the string indicates must NOT be null. Because of caching,
	 * repeated calls to this method on the same string do not re-parse, as long as
	 * the calls occur in a row.
	 * 
	 * @param perm_string
	 * @param vars
	 * @return
	 * @throws RecognitionException
	 */
	public static List<Variable>
	parseMustNotBeNullFromString(String perm_string, SimpleMap<String,Variable> vars) 
	throws RecognitionException {

		TopLevelPred parsed_pred = parse(perm_string);
		return 
			(parsed_pred == null || parsed_pred instanceof TopLevelPred.Impossible ?
					Collections.<Variable>emptyList() :
					((AccessPred) parsed_pred).accept(new NullVisitorConj(false, vars)));
	}
	
	/**
	 * Giving a string from a permission annotation, this method
	 * will return all of the 'more specific state information'
	 * that we find. In other words, when 'var in OPEN' is
	 * found, this tells us that more specific information for
	 * var has been found, refining another permission.
	 * 
	 * @param perm_string The string coming from a permission
	 *                    annotation.
	 * @return A list of pairs mapping variable names to states.
	 * @throws RecognitionException If there is a parsing error.
	 */
	public static List<Pair<String, String>>
	parseStateInfoFromString(String perm_string)
	throws RecognitionException {
		
		TopLevelPred parsed_pred = parse(perm_string);
		return
			(parsed_pred == null || parsed_pred instanceof TopLevelPred.Impossible ?
					Collections.<Pair<String,String>>emptyList() :
					((AccessPred) parsed_pred).accept(new StateInfoVisitorConj()));
	}
	
	/**
	 * This method tests whether the given string parses to {@link TopLevelPred.Impossible}.
	 * 
	 * @param perm_string The string coming from a permission
	 *                    annotation.
	 * @return <code>true</code> if the given string parses to {@link TopLevelPred.Impossible},
	 * <code>false</code> otherwise.
	 * @throws RecognitionException If there is a parsing error.
	 */
	public static boolean parseImpossibleFromString(String perm_string) 
	throws RecognitionException {
		TopLevelPred parsed_pred = parse(perm_string);
		return parsed_pred == null ? false : parsed_pred instanceof TopLevelPred.Impossible;
	}

	/**
	 * Get pre and post PermissionFromAnnotation objects for the receiver.
	 * Results are cached, so calling parse parameter methods multiple times
	 * on the same pre and post condition string should not be super slow.
	 * 
	 * @return A pair corresponding to pre and post conditions for 'this'.
	 */
	public static Pair<List<PermissionFromAnnotation>,
	List<PermissionFromAnnotation>> parseReceiverPermissions(String pre, String post,
			StateSpace rcvrSpace, boolean preIsNamed, boolean frameAsVirtual)
	throws RecognitionException
	{
		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper(pre, post);
		return Pair.create(summaries.fst().getReceiverPermissions(rcvrSpace, preIsNamed, frameAsVirtual),
				summaries.snd().getReceiverPermissions(rcvrSpace, !preIsNamed, frameAsVirtual));
	}
	
	/**
	 * Get pre and post PermissionFromAnnotation objects for the <code>paramIndex</code>th parameter.
	 * Results are cached, so calling parse parameter methods multiple times in a row on the same
	 * pre and post string should not be too slow.
	 * 
	 * @return A pair corresponding to pre and post conditions for the <code>paramIndex</code>th parameter.
	 */
	public static Pair<List<PermissionFromAnnotation>, List<PermissionFromAnnotation>> 
	parseParameterPermissions(String pre, String post, StateSpace space, 
			int paramIndex, boolean preIsNamed) 
	throws RecognitionException		
	{
		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper(pre, post);
		return Pair.create(summaries.fst().getParameterPermissions(space, paramIndex, preIsNamed),
				summaries.snd().getParameterPermissions(space, paramIndex, !preIsNamed));		
	}
	/**
	 * Get post PermissioNfromAnnotation objects for the result. Results are cached, so calling parse parameter methods multiple times in a row on the same
	 * pre and post string should not be too slow.
	 */
	public static List<PermissionFromAnnotation> parseResultPermissions(
			String perm_string, StateSpace space, boolean namedFractions) 
	throws RecognitionException {
		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries =
			paramParseHelper("", perm_string);
		return summaries.snd().getResultPermissions(space, namedFractions);
	}
	
	/**
	 * Caching fields used for parameters.
	 */
	private static Pair<String, ParsedParameterSummary> cachedPre = Pair.create("", new ParsedParameterSummary());
	private static Pair<String, ParsedParameterSummary> cachedPost = Pair.create("", new ParsedParameterSummary());
	private static Pair<ParsedParameterSummary, ParsedParameterSummary>
	paramParseHelper(String pre, String post)
			throws RecognitionException {
		ParsedParameterSummary pre_summary;
		ParsedParameterSummary post_summary;
		if( cachedPre.fst().equals(pre) ) {
			pre_summary = cachedPre.snd();
		}
		else {
			TopLevelPred pre_pred = parse(pre);
			pre_summary = pre_pred == null ? 
					new ParsedParameterSummary() : 
					(pre_pred instanceof TopLevelPred.Impossible ?
							new ParsedParameterSummary(true) :
							((AccessPred) pre_pred).accept(new ParamFPVisitorConj()));
			cachedPre.setComponent1(pre);
			cachedPre.setComponent2(pre_summary);
		}
		if( cachedPost.fst().equals(post) ) {
			post_summary = cachedPost.snd();
		}
		else {
			TopLevelPred post_pred = parse(post);
			post_summary = post_pred == null ? 
					new ParsedParameterSummary() : 
					(post_pred instanceof TopLevelPred.Impossible ?
							new ParsedParameterSummary(true) :
							((AccessPred) post_pred).accept(new ParamFPVisitorConj()));
		}
		return Pair.create(pre_summary, post_summary);
	}

	/**
	 * Returns null if cannot be found.
	 */
	public static Pair<String, String> getPermAnnotationStrings(AnnotationSummary sum) {
		for( ICrystalAnnotation anno : sum.getReturn() ) {
			if( anno instanceof PermAnnotation ) {
				PermAnnotation panno = (PermAnnotation)anno;
				/*
				 * Can't be there twice... we know that.
				 */
				return Pair.create(panno.getRequires(), panno.getEnsures());
			}
		}
		return null;
	}
	
	public static Pair<Set<String>, Set<String>> 
	getParameterStateInfo(AnnotationSummary sum, int paramIndex) throws RecognitionException {
		Pair<String, String> preAndPostString = getPermAnnotationStrings(sum);

		if( preAndPostString == null ) {
			// no @Perm -> bail
			return Pair.create(Collections.<String>emptySet(), Collections.<String>emptySet());
		}
		
		return getParameterStateInfo(preAndPostString.fst(), preAndPostString.snd(), paramIndex);
	}
	
	public static Pair<Set<String>, Set<String>> 
	getParameterStateInfo(String pre, String post, int paramIndex) throws RecognitionException {
		
		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper(pre, post);
		
		return Pair.create(summaries.fst().getParameterStates(paramIndex), summaries.snd().getParameterStates(paramIndex));
	}
	
	/**
	 * For constructors, the pre-set is meaningless.
	 * @param sum
	 * @return
	 * @throws RecognitionException
	 */
	public static Pair<Set<String>, Set<String>> 
	getReceiverStateInfo(AnnotationSummary sum) throws RecognitionException {
		Pair<String, String> preAndPostString = getPermAnnotationStrings(sum);
		
		if( preAndPostString == null ) {
			// no @Perm -> bail
			return Pair.create(Collections.<String>emptySet(), Collections.<String>emptySet());
		}
	
		return getReceiverStateInfo(preAndPostString.fst(), preAndPostString.snd());
	}
		
	/**
	 * For constructors, the pre-set is meaningless.
	 * @param sum
	 * @return
	 * @throws RecognitionException
	 */
	public static Pair<Set<String>, Set<String>> 
	getReceiverStateInfo(String pre, String post) throws RecognitionException {

		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper(pre, post);
		
		return Pair.create(summaries.fst().getReceiverStates(), summaries.snd().getReceiverStates());
	}

	/**
	 * For constructors, the pre-set is meaningless.
	 * @param sum
	 * @return
	 * @throws RecognitionException
	 */
	public static Set<String> 
	getResultStateInfo(AnnotationSummary sum) throws RecognitionException {
		Pair<String, String> preAndPostString = getPermAnnotationStrings(sum);
		
		if( preAndPostString == null ) {
			// no @Perm -> bail
			return Collections.<String>emptySet();
		}
		
		return getResultStateInfo(preAndPostString.snd());
	}
		
	public static Set<String> 
	getResultStateInfo(String post) throws RecognitionException {

		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper("", post);
		
		return summaries.snd().getReceiverStates();
	}
}
