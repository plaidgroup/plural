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
import java.util.Map;

import edu.cmu.cs.plural.fractions.Fraction;

/**
 * Represents an actual access permission, e.g.,<br>
 * <code>full(r, root, ff, ff) in alive</code><br>
 * This is a place-holder, that holds all the information we need to make a real
 * edu.cmu.cs.plural.track.Permission until we can actually create one.
 * 
 * @author Nels Beckman
 * @date Mar 26, 2008
 *
 */
public class TempPermission implements AccessPred {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	
	private final String type;
	private final RefExpr ref;
	private final String root;
	private final Map<String, Fraction> fractions;
//	private final String fractFunc;
//	private final String belowFunc;
	private final String[] stateInfo;

	public TempPermission(String type, RefExpr ref, String root, 
			          Map<String, Fraction> fractions, List<String> stateInfo) {
		this.type = type;
		this.ref = ref;		
		this.root = root == null ? TempPermission.getDeafaultRoot() : root;
		this.fractions = fractions == null ? Collections.<String, Fraction>emptyMap() : fractions;
//		this.fractFunc = fractFunc == null ? TempPermission.getDefaultFractFunc() : fractFunc;
//		this.belowFunc = belowFunc == null ? TempPermission.getDefaultBelowFunc() : belowFunc;
		if(stateInfo == null || stateInfo.size() == 0)
			this.stateInfo = new String[] { this.root };
		else
			// allocates an array of the right size
			this.stateInfo = stateInfo.toArray(EMPTY_STRING_ARRAY);
	}

	private static String getDefaultBelowFunc() {
		return "";
	}

	private static String getDefaultFractFunc() {
		return "";
	}

	private static String getDeafaultRoot() {
		return "alive";
	}

	public String getType() {
		return type;
	}

	public RefExpr getRef() {
		return ref;
	}

	public String getRoot() {
		return root;
	}
	
	public Map<String, Fraction> getFractions() {
		return fractions;
	}

//	public String getFractFunc() {
//		return fractFunc;
//	}
//
//	public String getBelowFunc() {
//		return belowFunc;
//	}
	
	public String[] getStateInfo() {
		return stateInfo;
	}

	@Override
	public <T> T accept(AccessPredVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return type + "(" + ref + ", " + root + ", " + fractions + 
				") in " + stateInfo;
	}
}
