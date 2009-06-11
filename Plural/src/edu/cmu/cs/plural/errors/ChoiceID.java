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

package edu.cmu.cs.plural.errors;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A choice id uniquely identifies a node in the choice tree. Choices
 * (as in &) come from things like method cases and inference of
 * which state to pack to. In order to track the tree of choices
 * over time, we give them ids. A choice ID has an age, so we can
 * say that one choice id is newer or older than another.
 * 
 * @author Nels E. Beckman
 * @since May 29, 2009
 */
public class ChoiceID implements Comparable<ChoiceID> {

	private static final AtomicLong ID_GENERATOR = new AtomicLong(0L);
	
	private final long id;
		
	private ChoiceID() {
		id = ID_GENERATOR.incrementAndGet();
	}
	
	public static ChoiceID choiceID() {
		return new ChoiceID();
	}
	
	public static ChoiceID choiceID(final String purpose) {
		return new ChoiceID() {
			@Override
			public String toString() {
				return purpose;
			}
		};
	}
	
	@Override
	public int compareTo(ChoiceID arg0) {
		// I didn't just subtract because the return value of
		// this method is an int and I didn't want to have to
		// worry about annoying overflow garbage.
		return 
			this.id < arg0.id ? -1 :
				(this.id == arg0.id ? 0 : 1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChoiceID other = (ChoiceID) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	/**
	 * Returns the 'younger' of the two given choice ids, or the first
	 * one if they are the same.
	 */
	public static ChoiceID younger(ChoiceID first, ChoiceID second) {
		int compareTo = first.compareTo(second);
		if( compareTo < 0 )
			return first;
		else if( compareTo == 0 )
			return first;
		else
			return second;
			
	}

	@Override
	public String toString() {
		return "Choice ID: " + this.id;
	}	
}
