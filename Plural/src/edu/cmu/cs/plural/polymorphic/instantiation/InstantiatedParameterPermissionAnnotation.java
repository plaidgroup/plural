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

package edu.cmu.cs.plural.polymorphic.instantiation;

import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.polymorphic.internal.PolyVarUseAnnotation;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * This class will be how we convert an instantiated polymorphic permission into a
 * ParameterPermissionAnnotation, the original parameter permission annotation used
 * in Plural. Basically it will take a polymorphic instantiation and a variable and
 * use that information to create an older style plural permission. Note that even
 * though this class indirectly extends ICrystalAnnotation, we don't plan on
 * registering it in the plugin manifesto.
 * 
 * @author Nels E. Beckman
 * @since Nov 19, 2009
 *
 * @see {@link ParameterPermissionAnnotation}
 * @see {@link ApplyAnnotationWrapper}
 */
public final class InstantiatedParameterPermissionAnnotation implements
		ParameterPermissionAnnotation {
	// What is the polymorphic permission for this parameter (which will presumably be substituted based
	// on the application of the receiver)?
	private final PolyVarUseAnnotation paramPermAnnotation;

	private final GroundInstantiation parsedSubstitutedPerm;
	
	public InstantiatedParameterPermissionAnnotation(PolyVarUseAnnotation paramPermAnnotation,
			GroundInstantiation parsedSubstitutedPerm) {
		this.paramPermAnnotation = paramPermAnnotation;
		this.parsedSubstitutedPerm = parsedSubstitutedPerm;
	}

	@Override
	public String[] getEnsures() {
		return this.parsedSubstitutedPerm.getStates();
	}

	@Override
	public PermissionKind getKind() {
		return this.parsedSubstitutedPerm.getKind();
	}

	@Override
	public String[] getRequires() {
		return this.parsedSubstitutedPerm.getStates();
	}

	@Override
	public String getRootNode() {
		return this.parsedSubstitutedPerm.getRoot();
	}

	@Override
	public boolean isFramePermission() {
		return this.parsedSubstitutedPerm.isFrame();
	}

	@Override
	public boolean isReturned() {
		return paramPermAnnotation.isReturned();
	}
	
	@Override
	public boolean isVirtualPermission() {
		return this.parsedSubstitutedPerm.isVirtual();
	}

	/**
	 * @return
	 * @see edu.cmu.cs.plural.polymorphic.instantiation.ApplyAnnotationWrapper#getName()
	 */
	@Override
	public String getName() {
		throw new UnsupportedOperationException("We don't expect this to be used as a Crystal annotation");
	}

	/**
	 * @param key
	 * @return
	 * @see edu.cmu.cs.plural.polymorphic.instantiation.ApplyAnnotationWrapper#getObject(java.lang.String)
	 */
	@Override
	public Object getObject(String key) {
		throw new UnsupportedOperationException("We don't expect this to be used as a Crystal annotation");
	}

	/**
	 * @param name
	 * @see edu.cmu.cs.plural.polymorphic.instantiation.ApplyAnnotationWrapper#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException("We don't expect this to be used as a Crystal annotation");
	}

	/**
	 * @param key
	 * @param value
	 * @see edu.cmu.cs.plural.polymorphic.instantiation.ApplyAnnotationWrapper#setObject(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setObject(String key, Object value) {
		throw new UnsupportedOperationException("We don't expect this to be used as a Crystal annotation");
	}
}