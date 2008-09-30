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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.concrete.VariablePredicate;
import edu.cmu.cs.plural.fractions.FractionalPermissions;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.linear.PermissionPredicate;
import edu.cmu.cs.plural.pred.PredicateChecker.SplitOffTuple;
import edu.cmu.cs.plural.track.PluralTupleLatticeElement;

/**
 * Stores all the information that you find out about a param, meaning
 * a method argument or a field. It stores permissions, stateInfo,
 * and null-ness, true, and falseness. These will be created and
 * populated by the visitor of permission annotations.
 * 
 * @author Kevin Bierhoff
 * @since 8/04/2008
 */
public class ParamInfoHolder {
	
	private enum Primitive { 
		NULL, NONNULL, FALSE, TRUE, SEVERAL;
		
		public Primitive getOpposite() {
			switch(this) {
			case NULL: return NONNULL;
			case NONNULL: return NULL;
			case FALSE: return TRUE;
			case TRUE: return FALSE;
			default: return null;
			}
		}
	};
	
	private PermissionSetFromAnnotations perms;
	
	private Set<String> stateInfo = new LinkedHashSet<String>();
	
	private Primitive prim;
	
	/**
	 * @return the perms
	 */
	public PermissionSetFromAnnotations getPerms() {
		return perms;
	}

	/**
	 * @param perms the perms to set
	 */
	public void setPerms(PermissionSetFromAnnotations perms) {
		this.perms = perms;
	}

	public boolean hasStateInfo() {
		return stateInfo != null && !stateInfo.isEmpty();
	}

	/**
	 * @return the stateInfos
	 */
	public Set<String> getStateInfos() {
		return stateInfo;
	}

	/**
	 * @param pa
	 */
	public void addPerm(PermissionFromAnnotation pa) {
		if(perms == null)
			perms = PermissionSetFromAnnotations.createSingleton(pa);
		else
			perms = perms.combine(pa);
	}

	public void addNull(boolean nonnull) {
		addPrim(nonnull ? Primitive.NONNULL : Primitive.NULL);
	}

	public void addBoolean(boolean b) {
		addPrim(b ? Primitive.TRUE : Primitive.FALSE);
	}
	
	protected void addPrim(Primitive primitive) {
		if(prim == null)
			prim = primitive;
		else if(prim != primitive)
			prim = Primitive.SEVERAL;
		// else already set
	}

	public boolean hasNull() {
		return Primitive.NULL == prim;
	}

	public boolean hasNonNull() {
		return Primitive.NONNULL == prim;
	}

	public boolean hasTrue() {
		return Primitive.TRUE == prim;
	}

	public boolean hasFalse() {
		return Primitive.FALSE == prim;
	}

	ReleaseHolder createReleaseHolder(String releasedFromState) {
		return new ReleaseHolder(perms, releasedFromState);
	}
	
	public VariablePredicate createPredicate(String paramName, Aliasing var) {
		return createInfoPredicate(paramName, var);
	}
	
	InfoHolderPredicate createInfoPredicate(String paramName, Aliasing var) {
		return new InfoHolderPredicate(perms, prim, stateInfo, paramName, var);
	}
	
	/**
	 * A predicate, suitable for insertion into DynamicStateLogic, that
	 * holds the information gathered about a variable from the permission
	 * visitor. It also has an Aliasing location, so we know which variable
	 * we are talking about. It is immutable, like other predicates. 
	 */
	static class InfoHolderPredicate extends PermissionPredicate 
			implements VariablePredicate {

		private final Set<String> stateInfo;
		private final Primitive prim;
		private String paramName;
		
		
		public InfoHolderPredicate(PermissionSetFromAnnotations perms,
				Primitive prim, Set<String> stateInfo, String paramName, Aliasing var) {
			super(var, perms);
			assert perms != null || prim != null || (stateInfo != null && !stateInfo.isEmpty()); 
			this.prim = prim;
			this.stateInfo = stateInfo == null ? 
					// force the field to be non-null
					Collections.<String>emptySet() : 
						Collections.unmodifiableSet(stateInfo);
			this.paramName = paramName;
		}

		@Override
		public InfoHolderPredicate createIdenticalPred(Aliasing other) {
			return new InfoHolderPredicate(getPerms(), prim, stateInfo, paramName, other);
		}

		@Override
		public InfoHolderPredicate createOppositePred(Aliasing other) {
			return new InfoHolderPredicate(null, prim.getOpposite(), null, paramName, other);
		}

		@Override
		public boolean denotesBooleanFalsehood() {
			return prim == Primitive.FALSE && getPerms() == null && stateInfo.isEmpty();
		}

		@Override
		public boolean denotesBooleanTruth() {
			return prim == Primitive.TRUE && getPerms() == null && stateInfo.isEmpty();
		}

		@Override
		public boolean denotesNonNullVariable() {
			return prim == Primitive.NONNULL && getPerms() == null && stateInfo.isEmpty();
		}

		@Override
		public boolean denotesNullVariable() {
			return prim == Primitive.NULL && getPerms() == null && stateInfo.isEmpty();
		}

		@Override
		public boolean isUnsatisfiable(PluralTupleLatticeElement value) {
			if(prim != null) {
				switch(prim) {
				case NULL: 
					if(value.isNonNull(getVariable())) 
						return true;
					break;
				case NONNULL: 
					if(value.isNull(getVariable())) 
						return true;
					break;
				case TRUE: 
					if(value.isBooleanFalse(getVariable())) 
						return true;
					break;
				case FALSE: 
					if(value.isBooleanTrue(getVariable())) 
						return true;
					break;
				default: 
					// nothing
				}
			}
			
			if(!stateInfo.isEmpty()) {
				// TODO unsatisfiable state info??
			}
			
			if(getPerms() != null) {
				return super.isUnsatisfiable(value);
			}
			return false;
		}

		@Override
		public PluralTupleLatticeElement putIntoLattice(
				PluralTupleLatticeElement value) {
			Aliasing var = getVariable();
			assert var != null;
			
			if(prim != null) {
				switch(prim) {
				case NULL:
					value.addNullVariable(var);
					// null implies bottom permissions -> do nothing else
					return value;
				case NONNULL:
					value.addNonNullVariable(var);
					break;
				case TRUE:
					value.addTrueVarPredicate(var);
					break;
				case FALSE:
					value.addFalseVarPredicate(var);
					break;
				default:
					// nothing
				}
			}
			
			FractionalPermissions ps = value.get(var);
			boolean changed = false;
			if(getPerms() != null) {
				changed = true;
				ps = ps.mergeIn(getPerms());
			}
			
			for(String s : stateInfo) {
				changed = true;
				// TODO learn all states at once
				ps = ps.learnTemporaryStateInfo(s, isFramePredicate());
			}
			
			if(changed)
				value.put(var, ps);
			
			return value;
		}
		
		@Override
		public boolean isSatisfied(PluralTupleLatticeElement value) {
			if(prim != null) {
				switch(prim) {
				case NULL: 
					if(! value.isNull(getVariable())) 
						return false;
					break;
				case NONNULL: 
					if(! value.isNonNull(getVariable())) 
						return false;
					break;
				case TRUE: 
					if(! value.isBooleanTrue(getVariable())) 
						return false;
					break;
				case FALSE: 
					if(! value.isBooleanFalse(getVariable())) 
						return false;
					break;
				default: 
					// can't satisfy multiple at once
					return false;
				}
			}
			
			if(!stateInfo.isEmpty()) {
				FractionalPermissions ps = value.get(getVariable());
				if(! ps.isInStates(stateInfo, isFramePredicate()))
					return false;
			}
			
			if(getPerms() != null) {
				return super.isSatisfied(value);
			}
			return true;
		}

		/**
		 * @see edu.cmu.cs.plural.concrete.Implication#hasTemporaryState()
		 * @return <code>true</code> if this predicate carries temporary state, 
		 * <code>false</code> otherwise.
		 */
		public boolean hasTemporaryState() {
			if(! stateInfo.isEmpty())
				// count state info as temporary for now (see TODO in createCopyWithoutTemporaryState)
				return true;
			
			if(getPerms() != null && getPerms().hasShareOrPurePermissions())
				return true;
			
			return false;
		}

		/**
		 * @see edu.cmu.cs.plural.concrete.Implication#createCopyWithoutTemporaryState()
		 * @tag todo.general -id="1969915" : keep state info protected by full/unique/imm permissions in newPs
		 *
		 */
		public InfoHolderPredicate createCopyWithoutTemporaryState() {
			PermissionSetFromAnnotations newPs;
			if(getPerms() != null && getPerms().hasShareOrPurePermissions())
				newPs = getPerms().forgetShareAndPureStates();
			else
				newPs = getPerms();
			
			if((newPs == null || newPs.isEmpty()) && prim == null)
				// ignore state info in this test 'cause it's dropped anyway
				return null; // nothing left in this predicate
			
			return new InfoHolderPredicate(newPs, prim, null /* drop state info */, paramName, getVariable());
		}
		
		public boolean splitOff(SplitOffTuple tuple) {
			if(prim != null) {
				switch(prim) {
				case NULL: 
					if(! tuple.checkNull(getVariable(), paramName)) 
						return false;
					break;
				case NONNULL: 
					if(! tuple.checkNonNull(getVariable(), paramName)) 
						return false;
					break;
				case TRUE: 
					if(! tuple.checkTrue(getVariable(), paramName)) 
						return false;
					break;
				case FALSE: 
					if(! tuple.checkFalse(getVariable(), paramName)) 
						return false;
					break;
				default: 
					// can't satisfy multiple at once
					return false;
				}
			}
			
			if(!stateInfo.isEmpty()) {
				boolean state_info_correct = tuple.checkStateInfo(getVariable(), paramName, 
						stateInfo, isFramePredicate());
				
				if( !state_info_correct )
					return false;
			}
			
			if(getPerms() != null) {
				boolean split_worked =
					tuple.splitOffPermission(getVariable(), paramName, getPerms());
				
				return split_worked;
			}
			return true;
		}

		private boolean isFramePredicate() {
			return "this!fr".equals(paramName);
		}

		@Override
		public String toString() {
			String result = null;
			if(prim != null)
				result = prim + "(" + getVariable() + ")";
			if(stateInfo != null && !stateInfo.isEmpty()) {
				if(result == null)
					result = getVariable() + " IN " + stateInfo;
				else
					result+= " && " + getVariable() + " IN " + stateInfo;
			}
			if(getPerms() != null) {
				if(result == null)
					result = getVariable() + " : " + getPerms();
				else
					result+= " && " + getVariable() + " : " + getPerms();
			}
			if(result == null)
				return "EMP"; // this shouldn't happen
			return result;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ ((paramName == null) ? 0 : paramName.hashCode());
			result = prime * result + ((prim == null) ? 0 : prim.hashCode());
			result = prime * result
					+ ((stateInfo == null) ? 0 : stateInfo.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			InfoHolderPredicate other = (InfoHolderPredicate) obj;
			if (paramName == null) {
				if (other.paramName != null)
					return false;
			} else if (!paramName.equals(other.paramName))
				return false;
			if (prim == null) {
				if (other.prim != null)
					return false;
			} else if (!prim.equals(other.prim))
				return false;
			if (stateInfo == null) {
				if (other.stateInfo != null)
					return false;
			} else if (!stateInfo.equals(other.stateInfo))
				return false;
			return true;
		}
		
	}

}
