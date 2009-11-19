/**
 * Copyright (C) 2007-2009 Carnegie Mellon University and others.
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

package edu.cmu.cs.plural.polymorphic.instantiation;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Triple;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * Takes ground permissions, parses them, and returns them in the form of a
 * {@link GroundInstantiation}. (Or, they may fail to parse!) This may eventually
 * be implemented as an ANTLR parser, but for the time being, I don't see any good
 * reason why I can just make a series of regular expressions.<br>
 * <br>
 * A permission looks like:<br>
 * <code>share(alive,vrfr) in Open,HasNext</code><br>
 * Where the middle part, or both the middle and end part can be left off, and
 * defaults substituted. For the virtual/frame section, legal choices are:<br>
 * vr, fr, or vrfr<br>
 * <br>
 * @author Nels E. Beckman
 * @since Nov 19, 2009
 * @see {@link GroundInstantiation}
 */
public final class GroundParser {

	private final static Pattern firstStatePattern = 
		Pattern.compile(".+in\\s+(\\w+)\\s*,?");
	
	private final static Pattern remainingStatesPattern =
		Pattern.compile("\\s*(\\w+)\\s*.?");
	
	private final static Pattern statesSanityCheck = 
		Pattern.compile(".*\\sin\\s.*");
	
	private final static Pattern middlePattern = 
		Pattern.compile(".+\\(\\s*(\\w+)\\s*,\\s*(\\w+)\\s*\\).+");
	
	private final static Pattern middleSanityCheck = 
		Pattern.compile(".*\\(.*\\).*");
	
	private static class ParseException extends Exception {
		private static final long serialVersionUID = -4984411706947693775L;
	}
	
	public static Option<GroundInstantiation> parse(String str) {
		try {
			PermissionKind kind = parseFront(str);
			Option<String[]> states_ = parseEnd(str);
			Option<Triple<String,Boolean,Boolean>> root_and_type_ = parseMiddle(str);
			GroundInstantiation result;
		
			if( root_and_type_.isSome() ) {
				Triple<String,Boolean,Boolean> root_and_type = root_and_type_.unwrap();
				result = new GroundInstantiation(kind, root_and_type.thrd(), root_and_type.snd(), 
						root_and_type.fst(), states_.unwrap());
				
			}
			else if( states_.isSome() ) {
				result = new GroundInstantiation(kind, states_.unwrap());
			}
			else {
				result = new GroundInstantiation(kind);
			}
			return Option.some(result);
			
		} catch (ParseException e) {
			return Option.none();
		}
	}
	
	private static PermissionKind parseFront(String wholeString) throws ParseException {
		// We don't even need a regular expression for this one:
		String[] split = wholeString.split("\\(|\\s");
		if( split.length < 1 ) throw new ParseException();
		
		String perm_str = split[0];
		split = perm_str.split("in");
		if( split.length < 1 ) throw new ParseException();
		
		perm_str = split[0].toLowerCase();
		
		if( perm_str.equals("share") )
			return PermissionKind.SHARE;
		else if( perm_str.equals("pure") )
			return PermissionKind.PURE;
		else if( perm_str.equals("unique") )
			return PermissionKind.UNIQUE;
		else if( perm_str.equals("full") ) 
			return PermissionKind.FULL;
		else if( perm_str.equals("immutable") || perm_str.equals("imm") )
			return PermissionKind.IMMUTABLE;
		else 
			throw new ParseException();
	}
	
	private static Option<Triple<String, Boolean, Boolean>> parseMiddle(String wholeString) throws ParseException {
		if( !middleSanityCheck.matcher(wholeString).matches() )
			return Option.none();
		
		Matcher matcher = middlePattern.matcher(wholeString);
		if( !matcher.matches() )
			throw new ParseException();
		
		// Otherwise, get root and perm type
		String root = matcher.group(1);
		String perm_type = matcher.group(2).toLowerCase();
		Boolean vr;
		Boolean fr;
		
		if( perm_type.equals("vr") ) {
			vr = Boolean.TRUE;
			fr = Boolean.FALSE;
		}
		else if( perm_type.equals("fr") ) {
			vr = Boolean.FALSE;
			fr = Boolean.TRUE;
		}
		else if( perm_type.equals("vrfr") ) {
			vr = Boolean.TRUE;
			fr = Boolean.TRUE;
		}
		else {
			throw new ParseException();
		}
		Triple<String,Boolean,Boolean> result_ = Triple.createTriple(root, vr, fr);
		return Option.some(result_);
	}
	
	private static Option<String[]> parseEnd(String wholeString) throws ParseException {
		if( !statesSanityCheck.matcher(wholeString).matches() )
			return Option.none();
			
		Matcher matcher = firstStatePattern.matcher(wholeString);
		if( !matcher.find() )
			throw new ParseException();
		
		// We have at least one state
		List<String> result_ = new LinkedList<String>();
		result_.add(matcher.group(1));
		
		// Now, start searching the rest of it.
		int where_to_start = matcher.end();
		Matcher remaining_matcher = remainingStatesPattern.matcher(wholeString);
		
		if( remaining_matcher.find(where_to_start) ) {
			do {
				result_.add(remaining_matcher.group(1));
			} while( remaining_matcher.find() );
		}
		
		String[] result__ = result_.toArray(new String[]{});
		return Option.some(result__);
	}	
}