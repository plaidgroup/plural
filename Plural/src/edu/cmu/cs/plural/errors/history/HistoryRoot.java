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

package edu.cmu.cs.plural.errors.history;


/**
 * The root of a Plural analysis history. A history is one path from
 * root to leaf of the choices in an analysis. A root corresponds to 
 * a method specification case, and a 
 * {@code virtual == current / != current}
 * setting.
 * 
 * @author Nels E. Beckman
 * @since Jun 2, 2009
 *
 */
class HistoryRoot {

	protected final String methodCase;
	private final HistoryNode root; 
	
	/* Soon there will also be a root entry point. */
	
	private HistoryRoot(HistoryNode root, String methodCase) {
		this.methodCase = methodCase;
		this.root = root;
	}
	
	public HistoryNode getRoot() {
		return this.root;
	}
	
	/**
	 * A history root for checking cases where we do not need to
	 * separately check virtual == current/ != current.
	 */
	public static HistoryRoot noSeparateCaseRoot(HistoryNode root, String methodCase) {
		return new HistoryRoot(root, methodCase){

			@Override
			public String toString() {
				return "Case: " + methodCase;
			}
		};
	}
	
	/**
	 * A history root based on the given spec case when the virtual
	 * frame was checked as being the current frame.
	 */
	public static HistoryRoot virtualIsCurrent(HistoryNode root, String methodCase) {
		return new HistoryRoot(root, methodCase){

			@Override
			public String toString() {
				return "Case[virtual = current]: " + methodCase;
			}
		};		
	}
	
	/**
	 * A history root based on the given spec case when the virtual
	 * frame was checked as being different form the current frame.
	 */
	public static HistoryRoot virtualNotCurrent(HistoryNode root, String methodCase) {
		return new HistoryRoot(root, methodCase){

			@Override
			public String toString() {
				char not_eq = '\u2260';
				return "Case[virtual " + not_eq + " current]: " + methodCase;
			}
		};
	}
}