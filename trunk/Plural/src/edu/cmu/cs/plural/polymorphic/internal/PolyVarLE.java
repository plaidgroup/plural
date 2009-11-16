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

package edu.cmu.cs.plural.polymorphic.internal;

import edu.cmu.cs.crystal.util.Lambda;
import edu.cmu.cs.crystal.util.Option;

/**
 * The lattice element for variables associated with polymorphic 
 * permission. There are three possible values, TOP, HAVE_NOT
 * which is signified by the name method returning NONE, and
 * HAVE, which is signified by the name method returning SOME.
 * If isTop returns true, other methods will throw an exception.
 * 
 * @author Nels E. Beckman
 * @since Nov 11, 2009
 *
 */
public interface PolyVarLE {
	/** The static TOP lattice element. */
	public static final PolyVarLE TOP = new PolyVarLE(){
		@Override public boolean isTop() { return true;	}
		@Override
		public Option<String> name() {
			throw new UnsupportedOperationException("Invalid for TOP.");
		}
		@Override public boolean isBottom() {return false;}
		@Override public String toString() {return "TOP";}
	};
	
	/** The static BOTTOM lattice element. */
	public static final PolyVarLE BOTTOM = new PolyVarLE(){
		@Override public boolean isBottom() {return true;}
		@Override public boolean isTop() {return false;}
		@Override
		public Option<String> name() {
			throw new UnsupportedOperationException("Invalid for BOTTOM.");
		}
		@Override public String toString() {return "BOTTOM";}
	};
		
	/** The static NONE lattice element. */
	public static final PolyVarLE NONE = new PolyVarLE(){
		@Override public boolean isBottom() {return false;}
		@Override public boolean isTop() {return false;}
		@Override public Option<String> name() {return Option.none();}
		@Override public String toString() {return "NONE";}
	};
	
	/** Factory for making the HAVE element; given a name of a polymorphic
	 *  permission, it will return a lattice element for that permission. */
	public static final Lambda<String,PolyVarLE> HAVE_FACTORY = new Lambda<String,PolyVarLE>(){
		@Override
		public PolyVarLE call(final String i) {
			return new PolyVarLE(){
				@Override public boolean isBottom() {return false;}
				@Override public boolean isTop() {return false;}
				@Override public Option<String> name() {return Option.some(i);}
				@Override
				public int hashCode() {
					final int prime = 31;
					int result = 1;
					result = prime * result + ((i == null) ? 0 : i.hashCode());
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
					PolyVarLE other = (PolyVarLE) obj;
					if( other.name().isNone() )
						return false;
					return other.name().unwrap().equals(i);
				}
				@Override public String toString() {return i;}
			};
		}};
	
	/** Is this value TOP? */
	boolean isTop();
	
	/** Is this the value BOTTOM? */
	boolean isBottom();
	
	/** If returns NONE, then there is no permission at all. If SOME, then
	 *  the permission is the permission given by NAME. */
	Option<String> name();
}