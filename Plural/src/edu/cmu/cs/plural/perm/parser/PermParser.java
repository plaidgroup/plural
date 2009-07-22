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
package edu.cmu.cs.plural.perm.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.concrete.PluralParseError;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.parser.TopLevelPred.Impossible;
import edu.cmu.cs.plural.pred.MethodPostcondition;
import edu.cmu.cs.plural.pred.MethodPrecondition;
import edu.cmu.cs.plural.pred.PredicateChecker;
import edu.cmu.cs.plural.pred.PredicateMerger;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

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
	private static ThreadLocal<Option<Pair<String, TopLevelPred>>> cachedResult = new ThreadLocal<Option<Pair<String, TopLevelPred>>>() {
		private Option<Pair<String, TopLevelPred>> cachedResult = Option.none(); 
			
		@Override
		public Option<Pair<String, TopLevelPred>> get() {
			return this.cachedResult;
		}
		@Override
		public void set(Option<Pair<String, TopLevelPred>> value) {
			this.cachedResult = value;
		}
	};
	
	private static Option<TopLevelPred> parse(String str) {
		if( cachedResult.get().isSome() && 
			cachedResult.get().unwrap().fst().equals(str) ) {
			return Option.some(cachedResult.get().unwrap().snd());
		}
		else {
			AccessPredLexer lex = new AccessPredLexer(new ANTLRStringStream(str));
			CommonTokenStream tokens = new CommonTokenStream(lex);

			AccessPredParser parser = new AccessPredParser(tokens);

			TopLevelPred parsed_pred = null;
			try {
				parsed_pred = parser.start();
			} catch(RecognitionException re) {
				// As far as I can tell, this is never thrown.
				return Option.none();
			} catch(PluralParseError ppe) {
				return Option.none();
			}
			
			if( parsed_pred == null ) {
				return Option.none();
			}
			
			/*
			 * Update cache.
			 */			
			cachedResult.set(Option.some(Pair.create(str, parsed_pred)));
			return Option.some(parsed_pred);
		}
	}
	
	/**
	 * Does the given string parse?
	 */
	public static boolean willParse(String str) {
		return parse(str).isSome();
	}
	
	/**
	 * Returns a collection of strings that are the names of fields
	 * mentioned in the given invariant. This method will parse the
	 * invariant, search in every subexpression and find any field
	 * (not this and not super) that  it finds.
	 */
	public static Iterable<String>
	getFieldsMentionedInString(String inv) {
		Option<TopLevelPred> parsed = parse(inv);
		
		if( parsed.isNone() )
			return Collections.emptySet();
		
		TopLevelPred pred = parsed.unwrap();
		if( pred instanceof Impossible )
			return Collections.emptySet();
		
		AccessPred a_pred = (AccessPred)pred;
		
		return	a_pred.accept(new FieldFinderVisitor());
	}
	
	/**
	 * Attempts to parse the given string. If parsing succeeds,
	 * returns NONE. Otherwise, returns the parse error.
	 */
	public static Option<String> getParseError(String str) {
		try {
			// Just parse that string!
			AccessPredLexer lex = new AccessPredLexer(new ANTLRStringStream(str));
			CommonTokenStream tokens = new CommonTokenStream(lex);

			AccessPredParser parser = new AccessPredParser(tokens);
			parser.start();
		} catch(RecognitionException re) {
			// As far as I can tell, this is never thrown.
			return Option.some("\"" + str + "\" did not parse.");
		} catch(PluralParseError ppe) {
			return Option.some("\"" + str + "\" did not parse.");
		}
		
		return Option.none();
	}
	
	public static <T> T accept(String perm_string, AccessPredVisitor<T> visitor) {
		Option<TopLevelPred> parsed_pred = parse(perm_string);
		if(parsed_pred.isNone() || parsed_pred.unwrap() instanceof TopLevelPred.Impossible)
			return null;
		return ((AccessPred) parsed_pred.unwrap()).accept(visitor);
	}
	
	/**
	 * Given a string from a permission annotation, this method will return a
	 * list of fractional permissions. For the time-being, this list represents
	 * conjunction because we cannot handle any other type of permission.
	 * 
	 * @param perm_string
	 * @return A list of variable/permission pairs.
	 */
	public static List<Pair<String, PermissionFromAnnotation>> 
	parsePermissionsFromString(String perm_string, SimpleMap<String,StateSpace> stateInfos,
			boolean createNamedVariables) {

		Option<TopLevelPred> parsed_pred = parse(perm_string);
        return 
        	(parsed_pred.isNone() || parsed_pred.unwrap() instanceof TopLevelPred.Impossible ? 
        		Collections.<Pair<String, PermissionFromAnnotation>>emptyList() :
        		((AccessPred) parsed_pred.unwrap()).accept(new FieldFPVisitorConj(stateInfos, createNamedVariables)));
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
	parseStateInfoFromString(String perm_string) {
		
		Option<TopLevelPred> parsed_pred = parse(perm_string);
		return
			(parsed_pred.isNone() || parsed_pred.unwrap() instanceof TopLevelPred.Impossible ?
					Collections.<Pair<String,String>>emptyList() :
					((AccessPred) parsed_pred.unwrap()).accept(new StateInfoVisitorConj()));
	}
	
	/**
	 * This method tests whether the given string parses to {@link TopLevelPred.Impossible}.
	 * 
	 * @param perm_string The string coming from a permission
	 *                    annotation.
	 * @return <code>true</code> if the given string parses to {@link TopLevelPred.Impossible},
	 * <code>false</code> otherwise.
	 */
	public static boolean parseImpossibleFromString(String perm_string) {
		Option<TopLevelPred> parsed_pred = parse(perm_string);
		return parsed_pred.isNone() ? 
			false : 
			parsed_pred.unwrap() instanceof TopLevelPred.Impossible;
	}

	/**
	 * Get pre and post PermissionFromAnnotation objects for the receiver.
	 * Results are cached, so calling parse parameter methods multiple times
	 * on the same pre and post condition string should not be super slow.
	 * 
	 * @return A pair corresponding to pre and post conditions for 'this'.
	 */
	@Deprecated
	public static Pair<List<PermissionFromAnnotation>,
	List<PermissionFromAnnotation>> parseReceiverPermissions(String pre, String post,
			StateSpace rcvrSpace, boolean preIsNamed) {
		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper(pre, post);
		return Pair.create(summaries.fst().getReceiverPermissions(rcvrSpace, preIsNamed),
				summaries.snd().getReceiverPermissions(rcvrSpace, !preIsNamed));
	}
	
	/**
	 * Get pre and post PermissionFromAnnotation objects for the <code>paramIndex</code>th parameter.
	 * Results are cached, so calling parse parameter methods multiple times in a row on the same
	 * pre and post string should not be too slow.
	 * 
	 * @return A pair corresponding to pre and post conditions for the <code>paramIndex</code>th parameter.
	 */
	@Deprecated
	public static Pair<List<PermissionFromAnnotation>, List<PermissionFromAnnotation>> 
	parseParameterPermissions(String pre, String post, StateSpace space, 
			int paramIndex, boolean preIsNamed) {
		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper(pre, post);
		return Pair.create(summaries.fst().getParameterPermissions(space, paramIndex, preIsNamed),
				summaries.snd().getParameterPermissions(space, paramIndex, !preIsNamed));		
	}
	/**
	 * Get post PermissioNfromAnnotation objects for the result. Results are cached, so calling parse parameter methods multiple times in a row on the same
	 * pre and post string should not be too slow.
	 */
	@Deprecated
	public static List<PermissionFromAnnotation> parseResultPermissions(
			String perm_string, StateSpace space, boolean namedFractions) {
		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries =
			paramParseHelper("", perm_string);
		return summaries.snd().getResultPermissions(space, namedFractions);
	}
	
	/**
	 * This method will parse the invariant declaration for a state and
	 * return objects that can be used to insert those permissions and
	 * check those permissions.
	 * 
	 * @see {@link #parseSignature(Pair, boolean, boolean, SimpleMap, Map, String, Map, Map, Map, Set)}
	 */
	public static Pair<PredicateMerger, PredicateChecker>
	parseInvariant(String invariantString,
			SimpleMap<String, StateSpace> fieldMapping) {
		
		InvariantParser merger = InvariantParser.createUnpackInvariantParser(fieldMapping);
		InvariantParser checker = InvariantParser.createPackInvariantParser(fieldMapping);
		
		Option<TopLevelPred> inv_pred_ = parse(invariantString);
		
		// Parse error or FALSE
		if( inv_pred_.isNone() || inv_pred_.unwrap() instanceof Impossible ) {
			return Pair.<PredicateMerger, PredicateChecker>create(impossiblePre, impossiblePost);
		}
		
		// Visit these invariant trees for effect on the InvariantParser.
		((AccessPred) inv_pred_.unwrap()).accept(merger);
		((AccessPred) inv_pred_.unwrap()).accept(checker);
		
		return Pair.<PredicateMerger, PredicateChecker>create(merger, checker);
	}
	
	/**
	 * @tag todo.general -id="1969914" : remove need for manual coercion by keeping this and this!fr completely separate (affects AbstractParamVisitor.getRefPair as well)
	 */
	public static Pair<MethodPrecondition, MethodPostcondition> 
	parseSignature(
			Pair<String, String> preAndPostString,
			boolean forAnalyzingBody,
			boolean frameToVirtual, 
			boolean noReceiverPre, boolean noReceiverVirtual,
			SimpleMap<String, StateSpace> spaces,
			Map<String, String> capturedParams,
			String capturing, Map<String, String> released, 
			Map<String, PermissionSetFromAnnotations> prePerms,
			Map<String, PermissionSetFromAnnotations> postPerms,
			Set<String> notBorrowed) {
		assert !forAnalyzingBody || !frameToVirtual;
		
		/*
		 * 1. create pre-condition parser
		 */
		MethodPreconditionParser pre;
		if(forAnalyzingBody) {
			pre = MethodPreconditionParser.createPreconditionForAnalyzingBody(
					prePerms, spaces, notBorrowed);
		}
		else {
			pre = MethodPreconditionParser.createPreconditionForCallSite(
					prePerms, spaces, frameToVirtual, noReceiverVirtual, notBorrowed);
		}
		
		/*
		 * 2. parse pre-condition, if any
		 */
		Option<TopLevelPred> pre_pred = parse(preAndPostString == null ? "" : preAndPostString.fst());
		if(pre_pred.isSome()) {
			if(pre_pred.unwrap() instanceof Impossible) {
				return Pair.create(impossiblePre, impossiblePost);
			}
			else {
				((AccessPred) pre_pred.unwrap()).accept(pre);
			}
		} // else true

		/*
		 * 3. drop receiver pre-conditions if desired
		 */
		if(noReceiverPre) {
			pre.getParams().remove("this");
			pre.getParams().remove("this!fr");
		}
		
		/*
		 * 4. determine captured permissions
		 */
		Map<String, ReleaseHolder> captured;
		if(capturedParams.isEmpty()) {
			captured = null;
		}
		else {
			captured = new LinkedHashMap<String, ReleaseHolder>(capturedParams.size());
			for(Map.Entry<String, String> p : capturedParams.entrySet()) {
				String param = p.getKey();
				if("this!fr".equals(param) && frameToVirtual)
					// TODO remove this: coerce this!fr to this if needed
					param = "this";
				ParamInfoHolder h = pre.getParams().get(param);
				if(h != null)
					captured.put(p.getKey(), h.createReleaseHolder(p.getValue()));
			}
		}
		
		/*
		 * 5. create post-condition parser
		 */
		MethodPostconditionParser post;
		if(forAnalyzingBody)
			post = MethodPostconditionParser.createPostconditionForAnalyzingBody(
					postPerms, captured, capturing, released, spaces);
		else
			post = MethodPostconditionParser.createPostconditionForCallSite(
					postPerms, captured, capturing, released, spaces, 
					frameToVirtual, noReceiverVirtual);
		
		/*
		 * 6. parse post-condition
		 */
		Option<TopLevelPred> post_pred = parse(preAndPostString == null ? "" : preAndPostString.snd());
		if(post_pred.isSome()) {
			if(post_pred.unwrap() instanceof Impossible) {
				return Pair.<MethodPrecondition, MethodPostcondition>create(pre, impossiblePost);
			}
			else {
				((AccessPred) post_pred.unwrap()).accept(post);
			}
		} // else true
		
		/*
		 * 7. Remove parameters with additional permissions from consideration for borrowing
		 * This allows methods to "produce" permissions for parameters, which is in particular
		 * the case in constructors.  If they borrow and produce something we're less precise
		 * than we could, but that seems like a corner-case.
		 * TODO allow borrowing *plus* additional permissions in post-condition
		 */
		pre.addNotBorrowed(post.getNotBorrowed());
		
		return Pair.<MethodPrecondition, MethodPostcondition>create(pre, post);
	}
	
	private static MethodPostcondition impossiblePost = new MethodPostcondition() {
		@Override
		public void mergeInPredicate(SimpleMap<String, Aliasing> vars,
				MergeIntoTuple callback) {
			callback.addVoid();
		}

		@Override
		public boolean splitOffPredicate(SimpleMap<String, Aliasing> vars,
				SplitOffTuple callback) {
			// fail trivially, as void can't be proven
			return false;
		}
	};
	
	private static MethodPrecondition impossiblePre = new MethodPrecondition() {
		@Override
		public boolean splitOffPredicate(SimpleMap<String, Aliasing> vars,
				SplitOffTuple callback) {
			// fail trivially, as void can't be proven
			return false;
		}

		@Override
		public boolean isReadOnly() {
			return true;
		}

		@Override
		public void mergeInPredicate(SimpleMap<String, Aliasing> vars,
				MergeIntoTuple callback) {
			callback.addVoid();
		}
	};


	/**
	 * Caching fields used for parameters.
	 */
	private static Pair<String, ParsedParameterSummary> cachedPre = Pair.create("", new ParsedParameterSummary());
	private static Pair<String, ParsedParameterSummary> cachedPost = Pair.create("", new ParsedParameterSummary());
	
	private static Pair<ParsedParameterSummary, ParsedParameterSummary>
	paramParseHelper(String pre, String post) {
		ParsedParameterSummary pre_summary;
		ParsedParameterSummary post_summary;
		if( cachedPre.fst().equals(pre) ) {
			pre_summary = cachedPre.snd();
		}
		else {
			Option<TopLevelPred> pre_pred = parse(pre);
			pre_summary = pre_pred.isNone() ? 
					new ParsedParameterSummary() : 
					(pre_pred.unwrap() instanceof TopLevelPred.Impossible ?
							new ParsedParameterSummary(true) :
							((AccessPred) pre_pred.unwrap()).accept(new ParamFPVisitorConj()));
			cachedPre.setComponent1(pre);
			cachedPre.setComponent2(pre_summary);
		}
		if( cachedPost.fst().equals(post) ) {
			post_summary = cachedPost.snd();
		}
		else {
			Option<TopLevelPred> post_pred = parse(post);
			post_summary = post_pred.isNone() ? 
					new ParsedParameterSummary() : 
					(post_pred.unwrap() instanceof TopLevelPred.Impossible ?
							new ParsedParameterSummary(true) :
							((AccessPred) post_pred.unwrap()).accept(new ParamFPVisitorConj()));
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
	getParameterStateInfo(AnnotationSummary sum, int paramIndex) {
		Pair<String, String> preAndPostString = getPermAnnotationStrings(sum);

		if( preAndPostString == null ) {
			// no @Perm -> bail
			return Pair.create(Collections.<String>emptySet(), Collections.<String>emptySet());
		}
		
		return getParameterStateInfo(preAndPostString.fst(), preAndPostString.snd(), paramIndex);
	}
	
	public static Pair<Set<String>, Set<String>> 
	getParameterStateInfo(String pre, String post, int paramIndex) {
		
		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper(pre, post);
		
		return Pair.create(summaries.fst().getParameterStates(paramIndex), summaries.snd().getParameterStates(paramIndex));
	}
	
	/**
	 * For constructors, the pre-set is meaningless.
	 * @param sum
	 * @return
	 */
	public static Pair<Set<String>, Set<String>> 
	getReceiverStateInfo(AnnotationSummary sum) {
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
	 */
	public static Pair<Set<String>, Set<String>> 
	getReceiverStateInfo(String pre, String post) {

		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper(pre, post);
		
		return Pair.create(summaries.fst().getReceiverStates(), summaries.snd().getReceiverStates());
	}

	/**
	 * For constructors, the pre-set is meaningless.
	 * @param sum
	 * @return
	 */
	public static Set<String> 
	getResultStateInfo(AnnotationSummary sum) {
		Pair<String, String> preAndPostString = getPermAnnotationStrings(sum);
		
		if( preAndPostString == null ) {
			// no @Perm -> bail
			return Collections.<String>emptySet();
		}
		
		return getResultStateInfo(preAndPostString.snd());
	}
		
	public static Set<String> 
	getResultStateInfo(String post) {

		Pair<ParsedParameterSummary, ParsedParameterSummary> summaries = 
			paramParseHelper("", post);
		
		return summaries.snd().getReceiverStates();
	}
}
