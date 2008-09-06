/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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
package edu.cmu.cs.plural.perm.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.Permission.PermissionKind;
import edu.cmu.cs.plural.util.CollectionMethods;

/**
 * This class is a temporary holding location for data that is being converted
 * from string annotations to PermissionFromAnnotation objects. 
 * 
 * @author Nels Beckman
 * @date Apr 2, 2008
 *
 */
class ParsedParameterSummary {

	private class InfoHolder {
		public InfoHolder(String rootNode2, String[] stateInfo, PermissionKind p_type, boolean isFramePermission) {
			this.rootNode = rootNode2;
			this.stateInfo = stateInfo;
			this.permType = p_type;
			this.isFramePermission = isFramePermission;
		}
		
		final String rootNode;
		final String stateInfo[];
		final PermissionKind permType;
		final boolean isFramePermission;
	}
	
	final private List<InfoHolder> receiverAnnotations;
	
	final private List<InfoHolder> resultAnnotations;
	
	final private Map<Integer, List<InfoHolder>> paramAnnotations;
	
	final private boolean impossible;
	
	public ParsedParameterSummary() {
		receiverAnnotations = new ArrayList<InfoHolder>();
		resultAnnotations = new ArrayList<InfoHolder>();
		paramAnnotations = new LinkedHashMap<Integer, List<InfoHolder>>();
		impossible = false;
	}
	
	private ParsedParameterSummary(List<InfoHolder> _rcvrAs,
			List<InfoHolder> _rsltAs, Map<Integer, List<InfoHolder>> _pAs) {
		this.receiverAnnotations = _rcvrAs;
		this.resultAnnotations = _rsltAs;
		this.paramAnnotations = _pAs;
		impossible = false;
	}
	
	public ParsedParameterSummary(boolean impossible) {
		this.impossible = impossible;
		receiverAnnotations = impossible ? null : new ArrayList<InfoHolder>();
		resultAnnotations = impossible ? null : new ArrayList<InfoHolder>();
		paramAnnotations = impossible ? null : new LinkedHashMap<Integer, List<InfoHolder>>();
	}
	/**
	 * Adds a new parameter permission. 
	 */
	public void addParam(Integer p_pos, String rootNode, String[] stateInfo,
			PermissionKind p_type) {
		CollectionMethods.addToMultiMap(p_pos, 
				new InfoHolder(rootNode, stateInfo, p_type, false),
				paramAnnotations);
	}
	
	/**
	 * Adds a new receiver permission.
	 */
	public void addRcvr(String rootNode, String stateInfo[], PermissionKind p_type, boolean isFramePermission) {
		this.receiverAnnotations.add(new InfoHolder(rootNode, stateInfo, p_type, isFramePermission));
	}
	
	/**
	 * Adds a new result permission.
	 * @param isFramePermission 
	 */
	public void addResult(String rootNode, String stateInfo[], PermissionKind p_type, boolean isFramePermission) {
		this.resultAnnotations.add(new InfoHolder(rootNode, stateInfo, p_type, isFramePermission));
	}
	
	/**
	 * Will be called by visitor at conjunction points.
	 */	
	public static ParsedParameterSummary union(ParsedParameterSummary s1,
			ParsedParameterSummary s2) {
		List<InfoHolder> rcAs = CollectionMethods.concat(s1.receiverAnnotations, s2.receiverAnnotations); 
		List<InfoHolder> rsAs = CollectionMethods.concat(s1.resultAnnotations, s2.resultAnnotations);
		Map<Integer, List<InfoHolder>> pAs = CollectionMethods.union(s1.paramAnnotations, s2.paramAnnotations);
		return new ParsedParameterSummary(rcAs, rsAs, pAs);
	}
	/**
	 * After collection, return all of the receiver permissions as
	 * PermissionFromAnnotation objects.
	 */
	public List<PermissionFromAnnotation> getReceiverPermissions(StateSpace space,
			boolean namedFractions) {
		List<PermissionFromAnnotation> result = 
			new ArrayList<PermissionFromAnnotation>(this.receiverAnnotations.size());
		for( InfoHolder perm_info : this.receiverAnnotations ) {
			PermissionFromAnnotation p = 
				PermissionFactory.INSTANCE.createOrphan(space, perm_info.rootNode,
						perm_info.permType, perm_info.isFramePermission, 
						perm_info.stateInfo, namedFractions);
			result.add(p);
		}
		return result;
	}
	
	/**
	 * After collection, return all of the <code>paramIndex</code> parameter
	 * permissions as PermissionFromAnnotation objects.
	 */
	public List<PermissionFromAnnotation> getParameterPermissions(
			StateSpace space, int paramIndex, boolean namedFractions) {
		List<PermissionFromAnnotation> result =
			new ArrayList<PermissionFromAnnotation>(this.paramAnnotations.size());
		

		Integer index = new Integer(paramIndex);
		if( !this.paramAnnotations.containsKey(index) ) return Collections.emptyList();
		
		for( InfoHolder perm_info : this.paramAnnotations.get(index) ) {
			PermissionFromAnnotation p =
				PermissionFactory.INSTANCE.createOrphan(space,
						perm_info.rootNode, perm_info.permType, perm_info.isFramePermission, perm_info.stateInfo, namedFractions);
			result.add(p);
		}
		return result;
	}
	
	/**
	 * After collection, return all of the result permissions as
	 * PermissionFromAnnotation objects.
	 */
	public List<PermissionFromAnnotation> getResultPermissions(StateSpace space,
			boolean namedFractions) {
		List<PermissionFromAnnotation> result = 
			new ArrayList<PermissionFromAnnotation>(this.resultAnnotations.size());
		for( InfoHolder perm_info : this.resultAnnotations ) {
			PermissionFromAnnotation p = 
				PermissionFactory.INSTANCE.createOrphan(space, perm_info.rootNode,
						perm_info.permType, perm_info.isFramePermission, perm_info.stateInfo, namedFractions);
			result.add(p);
		}
		return result;
	}
	
	public Set<String> getReceiverStates() {
		return getStateInfo(this.receiverAnnotations);
	}
	
	public Set<String> getParameterStates(int paramIndex) {
		if(this.paramAnnotations.containsKey(paramIndex) == false)
			return Collections.emptySet();
		return getStateInfo(this.paramAnnotations.get(paramIndex));
	}
	
	public Set<String> getResultStates() {
		return getStateInfo(this.resultAnnotations);
	}
	
	private Set<String> getStateInfo(Collection<InfoHolder> holders) {
		Set<String> result = new LinkedHashSet<String>(holders.size());
		for( InfoHolder perm_info : holders ) {
			for(String s : perm_info.stateInfo)
				result.add(s);
		}
		return result;
	}
}