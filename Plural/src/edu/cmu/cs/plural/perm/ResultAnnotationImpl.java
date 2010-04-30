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
package edu.cmu.cs.plural.perm;

import edu.cmu.cs.crystal.annotations.CrystalAnnotation;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

public class ResultAnnotationImpl extends CrystalAnnotation implements ResultPermissionAnnotation {

	/**
	 * @param name
	 */
	public ResultAnnotationImpl(String name) {
		super(name);
	}

	/**
	 * 
	 */
	public ResultAnnotationImpl() {
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.track.ResultPermissionAnnotation#getKind()
	 */
	@Override
	public PermissionKind getKind() {
		return deriveKindFromName(getName());
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.track.ResultPermissionAnnotation#getRootNode()
	 */
	@Override
	public String getRootNode() {
		return (String) getObject("guarantee");
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.track.ResultPermissionAnnotation#getEnsures()
	 */
	@Override
	public String[] getEnsures() {
		Object[] ensures = (Object[]) getObject("ensures");
		if(ensures.length == 0)
			return new String[] { getRootNode() };
		String[] result = new String[ensures.length];
		System.arraycopy(ensures, 0, result, 0, ensures.length);
		return result;
//		return Arrays.copyOf(result, result.length, String[].class);
	}
	
	private static PermissionKind deriveKindFromName(String name) {
		PermissionKind k;
		if(name.equals("edu.cmu.cs.plural.annot.ResultUnique")) {
			k = PermissionKind.UNIQUE;
		}
		else if(name.equals("edu.cmu.cs.plural.annot.ResultFull")) {
			k = PermissionKind.FULL;
		}
		else if(name.equals("edu.cmu.cs.plural.annot.ResultShare")) {
			k = PermissionKind.SHARE;
		}
		else if(name.equals("edu.cmu.cs.plural.annot.ResultImm")) {
			k = PermissionKind.IMMUTABLE;
		}
		else if(name.equals("edu.cmu.cs.plural.annot.ResultPure")) {
			k = PermissionKind.PURE;
		}
		else
			// not a result permission anno
			throw new IllegalArgumentException("Unknown result annotation: " + name);
		return k;
	}

}
