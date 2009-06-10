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

import edu.cmu.cs.crystal.tac.model.TACInstruction;

/**
 * A three address code location where something occurred. Its
 * main method available is the toString method for now.
 * 
 * @author Nels E. Beckman
 * @since Jun 9, 2009
 *
 */
public interface ITACLocation {
	
	/**
	 * All ITAC locations must override.
	 */
	public String toString();
}

class MethodIncomingLocation implements ITACLocation {

	static MethodIncomingLocation INSTANCE = new MethodIncomingLocation();
	
	@Override
	public String toString() {
		return "At method start";
	}
}

class AfterTACInstruction implements ITACLocation {
	private final TACInstruction instruction;
	
	public AfterTACInstruction(TACInstruction instruction) {
		if( instruction == null )
			throw new NullPointerException("This class does not accept null instructions.");
			
		this.instruction = instruction;
	}
	
	@Override
	public String toString() {
		return "After 3AC instruction: " + instruction.toString();
	}
}