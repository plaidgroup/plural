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
package edu.cmu.cs.plural.track;

import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * @author Kevin
 *
 */
public class SimplePermissionAnnotation {
	
	private String variable;
	private Permission requires;
	private Permission ensures;
	private boolean returned;

	/**
	 * @param stateSpace
	 */
	public SimplePermissionAnnotation(
			String variable, 
			StateSpace stateSpace,
			String rootNode,
			PermissionKind kind,
			boolean returned,
			String requires,
			String ensures) {
		this.variable = variable;
		this.returned = returned;
		this.requires = new Permission(stateSpace, rootNode, requires, kind);
		if(returned)
			this.ensures = new Permission(stateSpace, rootNode, ensures, kind);
		else
			this.ensures = null;
	}

	/**
	 * @return the returned
	 */
	public boolean isReturned() {
		return returned;
	}

	/**
	 * @return the variable
	 */
	public String getVariable() {
		return variable;
	}

	/**
	 * @return the ensures
	 */
	public Permission getEnsures() {
		// TODO Does it make sense to still have ensured state information, even if permission is captured?
		if(! isReturned())
			throw new IllegalStateException("Captured permission does not ensure anything");
		return ensures;
	}

	/**
	 * @return the requires
	 */
	public Permission getRequires() {
		return requires;
	}

	/**
	 * Set the returned flag.
	 * @param returned flag.
	 */
	public void setReturned(boolean returned) {
		this.returned = returned;
		if(returned && ensures == null)
			throw new IllegalStateException("Cannot start returning something out of nowhere.");
		if(returned == false)
			ensures = null;
	}

	/**
	 * @param defaultName
	 * @param a
	 * @param stateSpace 
	 * @return
	 */
	public static SimplePermissionAnnotation createPermissionIfPossible(
			String defaultName, ICrystalAnnotation a, StateSpace stateSpace) {
		PermissionKind k;
		if(a.getName().equals("edu.cmu.cs.plural.annot.Unique")) {
			k = PermissionKind.UNIQUE;
		}
		else if(a.getName().equals("edu.cmu.cs.plural.annot.Full")) {
			k = PermissionKind.FULL;
		}
		else if(a.getName().equals("edu.cmu.cs.plural.annot.Share")) {
			k = PermissionKind.SHARE;
		}
		else if(a.getName().equals("edu.cmu.cs.plural.annot.Imm")) {
			k = PermissionKind.IMMUTABLE;
		}
		else if(a.getName().equals("edu.cmu.cs.plural.annot.Pure")) {
			k = PermissionKind.PURE;
		}
		else
			// not a permission anno
			return null;
		
		// default values
		String varName = defaultName;
		String value = StateSpace.STATE_ALIVE;
		String requires = null;
		String ensures = null;
		boolean returned = true;
		
		String str;
		Object[] arr;
		
		str = (String) a.getObject("value");
		if(str.length() > 0) value = str;
		
		returned = (Boolean) a.getObject("returned");
		
		str = (String) a.getObject("var");
		if(str.length() > 0) 
			varName = str;
		
		arr = (Object[]) a.getObject("requires");
		assert arr.length <= 1;
		if(arr.length > 0) 
			requires = (String) arr[0];
		
		arr = (Object[]) a.getObject("ensures");
		assert arr.length <= 1;
		if(arr.length > 0) 
			ensures = (String) arr[0];
		
		// TODO fraction function annotations, field mappings

		// use root state as default pre- and post-condition if not explicitly provided
		if(requires == null) requires = value;
		if(ensures == null) ensures = value;
		
		return new SimplePermissionAnnotation(
				varName, 
				stateSpace, value, k, 
				returned,
				requires,
				ensures);
	}
	
}
