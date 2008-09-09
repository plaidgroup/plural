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
package edu.cmu.cs.plural.states.annowrappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.crystal.annotations.CrystalAnnotation;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;

final public class ClassStateDeclAnnotation implements ICrystalAnnotation {

	/*
	 * Delegate object. Does all the hard call-back work.
	 */
	private final CrystalAnnotation crystalAnnotation;
	
	public ClassStateDeclAnnotation() {
		crystalAnnotation = new CrystalAnnotation();
	}

	public ClassStateDeclAnnotation(String s) {
		crystalAnnotation = new CrystalAnnotation(s);
	}
	
	public String getName() {
		return crystalAnnotation.getName();
	}

	public Object getObject(String key) {
		return crystalAnnotation.getObject(key);
	}

	public void setName(String name) {
		crystalAnnotation.setName(name);
	}

	public void setObject(String key, Object value) {
		crystalAnnotation.setObject(key, value);
	}
	
	List<StateDeclAnnotation> statesCache = null;
	public List<StateDeclAnnotation> getStates() {
		if ( statesCache != null ) return statesCache;
		
		Object thing = crystalAnnotation.getObject("value");
		if( thing instanceof Object[] ) {
			Object[] os = (Object[])thing;
			List<StateDeclAnnotation> result = new ArrayList<StateDeclAnnotation>(os.length);
			
			for(Object o : os) {
				result.add((StateDeclAnnotation)o);
			}
			return ( statesCache = Collections.unmodifiableList(result) );
		}
		else {
			return ( statesCache = Collections.singletonList((StateDeclAnnotation)thing) );
		}

	}
}
