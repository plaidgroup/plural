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
package edu.cmu.cs.plural.perm.parser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.track.Permission.PermissionKind;
import edu.cmu.cs.plural.util.Pair;
import edu.cmu.cs.plural.util.SimpleMap;

/**
 * Common helper methods for parsing method parameter annotations
 * 
 * @author Kevin Bierhoff
 * @since 7/28/2008
 */
public abstract class AbstractParamVisitor implements AccessPredVisitor<Boolean> {

	private static final Logger log = Logger.getLogger(AbstractParamVisitor.class.getName());

	private final SimpleMap<String, StateSpace> spaces;

	private final boolean frameToVirtual;
	
	private final Map<String, ParamInfoHolder> params;
	
	private PermissionFactory pf = PermissionFactory.INSTANCE;

	private final boolean named;

	protected AbstractParamVisitor(Map<String, PermissionSetFromAnnotations> perms, 
			SimpleMap<String, StateSpace> spaces,
			boolean frameToVirtual, boolean namedFractions) {
		this.params = new LinkedHashMap<String, ParamInfoHolder>(perms.size());
		for(Map.Entry<String, PermissionSetFromAnnotations> p : perms.entrySet()) {
			ParamInfoHolder h = new ParamInfoHolder();
			h.setPerms(p.getValue());
			params.put(p.getKey(), h);
		}
		this.spaces = spaces;
		this.frameToVirtual = frameToVirtual;
		this.named = namedFractions;
	}

	@Override
	public Boolean visit(TempPermission perm) {
		RefExpr ref = perm.getRef();
		PermissionKind p_type = PermissionKind.valueOf(perm.getType().toUpperCase());
		StateSpace space = getStateSpace(ref);
		if(space != null) {
			Pair<String, Boolean> refPair = getRefPair(ref);
			boolean isFrame = refPair.snd();
			PermissionFromAnnotation pa = 
				pf.createOrphan(space, perm.getRoot(), p_type, isFrame, perm.getStateInfo(), named);
			addPerm(refPair.fst(), pa);
		}
		return null;
	}

	/**
	 * @param ref
	 * @return
	 */
	protected Pair<String, Boolean> getRefPair(RefExpr ref) {
		String param;
		boolean isFrame = false;
		if(ref instanceof Identifier) {
			param = ((Identifier) ref).getName();
			isFrame = !isFrameToVirtual() && ((Identifier) ref).isFrame();
		}
		else if(ref instanceof ParamReference) {
			param = ((ParamReference) ref).getParamString();
		}
		else
			throw new IllegalArgumentException("Unknown ref: " + ref);
		return Pair.create(param, isFrame);
	}

	/**
	 * @param ref
	 * @param pa
	 */
	protected void addPerm(String param, PermissionFromAnnotation pa) {
		ParamInfoHolder ps = getInfoHolder(param);
		ps.addPerm(pa);
	}

	/**
	 * @param param
	 * @return
	 */
	private ParamInfoHolder getInfoHolder(String param) {
		ParamInfoHolder ps = params.get(param);
		if(ps == null) {
			ps = new ParamInfoHolder();
			params.put(param, ps);
		}
		return ps;
	}

	@Override
	public Boolean visit(Disjunction disj) {
		log.warning("Ignore disjunction: " + disj);
		return null;
	}

	@Override
	public Boolean visit(Conjunction conj) {
		conj.getP1().accept(this);
		conj.getP2().accept(this);
		return null;
	}

	@Override
	public Boolean visit(Withing withing) {
		log.warning("Ignore with: " + withing);
		return null;
	}

	@Override
	public Boolean visit(BinaryExprAP binaryExpr) {
		log.warning("Ignore: " + binaryExpr);
		return null;
	}

	@Override
	public Boolean visit(EqualsExpr equalsExpr) {
		log.warning("Ignore: " + equalsExpr);
		return null;
	}

	@Override
	public Boolean visit(NotEqualsExpr notEqualsExpr) {
		log.warning("Ignore: " + notEqualsExpr);
		return null;
	}

	@Override
	public Boolean visit(StateOnly stateOnly) {
		String param = stateOnly.getStateInfo();
		getInfoHolder(param).getStateInfos().add(stateOnly.getStateInfo());
		return null;
	}
	
	/**
	 * @param ref
	 * @return
	 */
	protected StateSpace getStateSpace(RefExpr ref) {
		String n;
		if(ref instanceof Identifier) {
			Identifier id = (Identifier) ref;
			n = id.getName();
		}
		else if(ref instanceof ParamReference) {
			ParamReference pref = (ParamReference) ref;
			n = pref.getParamString();
		}
		else {
			log.warning("Unknown reference: " + ref);
			return null;
		}
		return spaces.get(n);
	}

	/**
	 * @return the pf
	 */
	protected PermissionFactory getPf() {
		return pf;
	}

	/**
	 * @param pf the pf to set
	 */
	public void setPf(PermissionFactory pf) {
		this.pf = pf;
	}

	/**
	 * @return the frameToVirtual
	 */
	protected boolean isFrameToVirtual() {
		return frameToVirtual;
	}

	/**
	 * @return the perms
	 */
	Map<String, ParamInfoHolder> getParams() {
		return params;
	}

	/**
	 * @return the spaces
	 */
	protected SimpleMap<String, StateSpace> getSpaces() {
		return spaces;
	}

}
