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

import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;

/**
 * @author Kevin
 *
 */
public class IndicatesAnnotation implements ICrystalAnnotation {
	
	/**
	 * Returns the boolean indicator annotation on a method receiver
	 * for the given boolean value, if any.
	 * @param method
	 * @param value
	 * @return The boolean indicator annotation for the given boolean <code>value</code>
	 * or <code>null</code> if no such annotation exists.
	 */
	public static IndicatesAnnotation getBooleanIndicatorOnReceiverIfAny(AnnotationSummary method, boolean value) {
		String indicatorAnnoName = value ? "edu.cmu.cs.plural.annot.TrueIndicates" : "edu.cmu.cs.plural.annot.FalseIndicates";
		return (IndicatesAnnotation) method.getReturn(indicatorAnnoName);
	}

	/**
	 * Returns the boolean indicator annotation on a method parameter 
	 * for the given boolean value, if any.
	 * @param method
	 * @param paramIndex The 0-based index of the parameter to be examined.
	 * @param value
	 * @return The boolean indicator annotation for the given boolean <code>value</code>
	 * or <code>null</code> if no such annotation exists.
	 */
	public static IndicatesAnnotation getBooleanIndicatorIfAny(AnnotationSummary method, 
			int paramIndex, boolean value) {
		String indicatorAnnoName = value ? "edu.cmu.cs.plural.annot.TrueIndicates" : "edu.cmu.cs.plural.annot.FalseIndicates";
		return (IndicatesAnnotation) method.getParameter(paramIndex, indicatorAnnoName);
	}

	private Boolean boolResult;
	private String indicatedState;
	private String name;
	
	/**
	 * Public default constructor so annotation database can instantiate instances.
	 */
	public IndicatesAnnotation() {
		super();
	}

	/**
	 * Legacy constructor for objects created from raw annotation bindings.
	 * @param boolResult
	 * @param indicatedState
	 * @deprecated Do not directly instantiate; use annotation database instead
	 */
	IndicatesAnnotation(boolean boolResult, String indicatedState) {
		super();
		this.boolResult = boolResult;
		this.indicatedState = indicatedState;
	}

	public boolean getBoolResult() {
		if(boolResult == null) {
			if("edu.cmu.cs.plural.annot.TrueIndicates".equals(name))
				boolResult = true;
			else if("edu.cmu.cs.plural.annot.FalseIndicates".equals(name))
				boolResult = false;
			if(boolResult == null)
				throw new IllegalStateException("This is not a boolean indicator: " + name);
			return boolResult;
		}
		else
			return boolResult;
	}
	
	public String getIndicatedState() {
		return indicatedState;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getObject(String key) {
		if("value".equals(key))
			return indicatedState;
		return null;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setObject(String key, Object value) {
		// TODO bool and state are for legacy purposes
		// (@Indicates used to be used for boolean tests)
		if("value".equals(key) || "state".equals(key))
			indicatedState = (String) value;
		else if("bool".equals(key))
			boolResult = (Boolean) value;
		else
			throw new IllegalArgumentException("Unknown indicator parameter: " + key);
	}

	/**
	 * @param anno
	 * @return
	 * @deprecated Use {@link IndicatesAnnotation#getBooleanIndicatorOnReceiverIfAny(AnnotationSummary, boolean) instead.
	 */
	public static IndicatesAnnotation createBooleanIndicatorIfPossible(IAnnotationBinding anno) {
		boolean bool = false;
		String state = "alive";
		if("Indicates".equals(anno.getName()) == false)
			return null;
		
		for(IMemberValuePairBinding p : anno.getAllMemberValuePairs()) {
			if("bool".equals(p.getName())) {
				bool = (Boolean) p.getValue();
			}
			if("state".equals(p.getName())) {
				state = (String) p.getValue();
			}
		}
		return new IndicatesAnnotation(bool, state);
	}

	/**
	 * @param anno
	 * @param value
	 * @return
	 * @deprecated Use {@link IndicatesAnnotation#getBooleanIndicatorOnReceiverIfAny(AnnotationSummary, boolean) instead.
	 */
	public static IndicatesAnnotation createBooleanIndicatorIfPossible(IAnnotationBinding anno, boolean value) {
		IndicatesAnnotation indicator = createBooleanIndicatorIfPossible(anno);
		if(indicator == null)
			return null;
		if(indicator.getBoolResult() == value)
			return indicator;
		return null;
	}

	/**
	 * @param annos
	 * @param value
	 * @return
	 * @deprecated Use {@link IndicatesAnnotation#getBooleanIndicatorOnReceiverIfAny(AnnotationSummary, boolean) instead.
	 */
	public static IndicatesAnnotation createBooleanIndicatorIfPossible(IAnnotationBinding[] annos, boolean value) {
		for(IAnnotationBinding a : annos) {
			IndicatesAnnotation indicator = createBooleanIndicatorIfPossible(a);
			if(indicator == null)
				continue;
			if(indicator.getBoolResult() == value)
				return indicator;
		}
		return null;
	}
}
