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
package edu.cmu.cs.plural.util;

import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;

/**
 * A 'lambda,' or first-class function, that takes two arguments and
 * returns one argument.
 *  
 * @author Nels E. Beckman
 * @since Sep 9, 2008
 *
 * @param <I1> The type of the first argument.
 * @param <I2> The type of the second argument.
 * @param <O> The type of the output.
 */
public interface Lambda2<I1, I2, O> {

	@Perm(requires="pure(this) * immutable(#0) * immutable(#1)", 
		  ensures="pure(this) * immutable(result)")
	public O call(@Pure(returned=false) I1 i1, @Pure(returned=false) I2 i2);
	
}
