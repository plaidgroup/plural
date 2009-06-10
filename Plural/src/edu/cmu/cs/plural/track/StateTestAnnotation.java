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

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;

import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.CrystalAnnotation;

/**
 * @author Kevin
 * @deprecated This class is no longer needed since we can use Crystal's multi-annotation
 * feature to group indicator annotations.
 * @see edu.cmu.cs.crystal.annotations.MultiAnnotation
 */
public class StateTestAnnotation extends CrystalAnnotation {

	/**
	 * @deprecated Not used for now.
	 */
	public StateTestAnnotation() {
		super(StateTestAnnotation.class.getName());
	}
	
	public IndicatesAnnotation[] getCases() {
		Object result = getObject("value");
		if(result instanceof Object[])
			return (IndicatesAnnotation[]) result;
		return new IndicatesAnnotation[] { (IndicatesAnnotation) result };
	}
	
	public static boolean isDynamicStateTest(IMethodBinding method) {
		for(IAnnotationBinding a : method.getAnnotations()) {
			if("StateTest".equals(a.getName()))
				return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param method
	 * @param value
	 * @return
	 * @deprecated Use {@link IndicatesAnnotation#getBooleanIndicatorOnReceiverIfAny(AnnotationSummary, boolean) instead.
	 */
	public static IndicatesAnnotation createBooleanIndicatorIfPossible(IMethodBinding method, boolean value) {
		for(IAnnotationBinding a : method.getAnnotations()) {
			if("StateTest".equals(a.getName())) {
				for(IMemberValuePairBinding p : a.getAllMemberValuePairs()) {
					if("value".equals(p.getName())) {
						Object indicatorAnnos = p.getValue();
						if(indicatorAnnos instanceof Object[]) {
							for(Object o : (Object[]) indicatorAnnos) {
								IndicatesAnnotation result = 
									IndicatesAnnotation.createBooleanIndicatorIfPossible(
											(IAnnotationBinding) o, value);
								if(result != null) 
									return result;
							}
						}
						else
							return IndicatesAnnotation.createBooleanIndicatorIfPossible(
									(IAnnotationBinding) indicatorAnnos, value);
					}
				}
			}
		}
		return null;
	}

}
