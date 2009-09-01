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

package edu.cmu.cs.plural.states.annowrappers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.crystal.annotations.CrystalAnnotation;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;

/**
 * Crystal annotation wrapper for the ForcePack annotation.
 * 
 * @author Nels E. Beckman
 * @since Aug 31, 2009
 *
 */
public class ForcePackAnnotation implements ICrystalAnnotation {

	/*
	 * Delegate object. Does all the hard call-back work.
	 */
	private final CrystalAnnotation crystalAnnotation = new CrystalAnnotation();
	
	@Override
	public String getName() {
		return this.crystalAnnotation.getName();
	}

	@Override
	public Object getObject(String key) {
		return this.crystalAnnotation.getObject(key);
	}

	@Override
	public void setName(String name) {
		this.crystalAnnotation.setName(name);
	}

	@Override
	public void setObject(String key, Object value) {
		this.crystalAnnotation.setObject(key, value);
	}

	/**
	 * Get the nodes that this annotation says we should pack to.
	 */
	public Set<String> getNodes() {
		Object thing = crystalAnnotation.getObject("value");
		
		if( thing instanceof Object[] ) {
			Object[] os = (Object[])thing;
			Set<String> result = new HashSet<String>(os.length);
			
			for(Object o : os) {
				result.add((String)o);
			}
			return Collections.unmodifiableSet(result);
		}
		else {
			return Collections.singleton((String)thing);
		}

	}
}
