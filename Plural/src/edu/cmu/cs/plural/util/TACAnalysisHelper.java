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

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.tac.ITACAnalysisContext;
import edu.cmu.cs.crystal.tac.ThisVariable;

/**
 * @author Kevin Bierhoff
 * @since 4/21/2008
 */
public class TACAnalysisHelper {

	private final ITACAnalysisContext context;
	private final AnnotationDatabase annoDB;
	private final ThisVariable thisVar;
	
	/**
	 * Use this constructor for methods without <code>this</code>, i.e., static methods.
	 * @param crystal
	 * @param ctx
	 */
	public TACAnalysisHelper(AnnotationDatabase annoDB, ITACAnalysisContext ctx) {
		this.annoDB = annoDB;
		this.context = ctx;
		this.thisVar = null;
	}

	/**
	 * Use this constructor for methods <code>this</code>, i.e., instance methods and constructors.
	 * @param crystal
	 * @param ctx
	 * @param thisVar
	 */
	public TACAnalysisHelper(AnnotationDatabase annoDB, ITACAnalysisContext ctx, ThisVariable thisVar) {
		this.annoDB = annoDB;
		this.context = ctx;
		this.thisVar = thisVar;
	}

	public ITACAnalysisContext getAnalysisContext() {
		return context;
	}
	
	public ThisVariable getThisVar() {
		return thisVar;
	}
	
	public boolean inStaticMethod() {
		return thisVar == null;
	}

	public AnnotationDatabase getAnnoDB() {
		return annoDB;
	}

}
