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
package edu.cmu.cs.fiddle.model;

import java.util.List;

/**
 * Model element representing a regular state, which holds several
 * dimensions.
 * 
 * @author Nels E. Beckman
 */
public interface IState extends IConnectable {

	/**
	 * Return the dimensions that make up this state machine. There is
	 * natural ordering for these dimensions.
	 * @return
	 */
	public List<IDimension> getDimensions();
	
	/**
	 * What is the on-screen location of this State? (X)
	 */
	public int getXPos();
	
	/**
	 * What is the on-screen location of this State? (Y)
	 */
	public int getYPos();

	/**
	 * Set a new X position for this state.
	 */
	public void setXPos(int x);

	/**
	 * Set a new Y position for this state.
	 */
	public void setYPos(int y);
}
