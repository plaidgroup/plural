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

import edu.cmu.cs.plural.states.StateSpace;

/**
 * @author Kevin
 *
 */
public class Permission extends Object implements IPermission<Permission> {
	
	private StateSpace stateSpace;
	private String rootNode;
	private PermissionKind kind;
	private String stateInfo;
	
	public Permission(StateSpace stateSpace) {
		this(stateSpace, stateSpace.getRootState());
	}

	public Permission(StateSpace stateSpace, String rootNode) {
		this(stateSpace, rootNode, rootNode);
	}
	
	public Permission(StateSpace stateSpace, String rootNode, String stateInfo) {
		this(stateSpace, rootNode, stateInfo, PermissionKind.UNIQUE);
	}

	public Permission(StateSpace stateSpace, String rootNode, PermissionKind kind) {
		this(stateSpace, rootNode, rootNode, kind);
	}
	
	public Permission(StateSpace stateSpace, String rootNode, String stateInfo, PermissionKind kind) {
		super();
		this.stateSpace = stateSpace;
		this.rootNode = rootNode;
		this.kind = kind;
		this.stateInfo = stateInfo;
	}
	
	public boolean contains(Permission other) {
		if(this.getRootNode().equals(other.getRootNode())) {
			if(this.getKind().isStrongerThan(other.getKind()) &&
					this.getStateSpace().firstBiggerThanSecond(other.getStateInfo(), this.getStateInfo()))
				return true;
			else
				return false;
		}
		// else we would have to break down permission for bigger state
		// that's only possible with unique / full permissions
		if(! (this.isUnique() || this.isFull()))
			return false;	
		if(other.getStateSpace().firstBiggerThanSecond(this.getRootNode(), other.getRootNode()) &&
				this.getStateSpace().firstBiggerThanSecond(other.getStateInfo(), this.getStateInfo()) &&
				this.getStateSpace().firstBiggerThanSecond(other.getRootNode(), this.getStateInfo()))
			return true;
		else
			return false;
	}
	
	public Permission join(Permission other) {
		if(this.equals(other)) return this;
		
		if(!this.getRootNode().equals(other.getRootNode()) || !this.getKind().equals(other.getKind()))
			return null;
		String stateApprox = this.getRootNode();
		if(getStateSpace().firstBiggerThanSecond(this.getStateInfo(), other.getStateInfo()))
			stateApprox = this.getStateInfo();
		else if(getStateSpace().firstBiggerThanSecond(other.getStateInfo(), this.getStateInfo()))
			stateApprox = other.getStateInfo();
		
		return copyNewState(stateApprox);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((kind == null) ? 0 : kind.hashCode());
		result = PRIME * result + ((rootNode == null) ? 0 : rootNode.hashCode());
		result = PRIME * result + ((stateInfo == null) ? 0 : stateInfo.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Permission other = (Permission) obj;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		if (rootNode == null) {
			if (other.rootNode != null)
				return false;
		} else if (!rootNode.equals(other.rootNode))
			return false;
		if (stateInfo == null) {
			if (other.stateInfo != null)
				return false;
		} else if (!stateInfo.equals(other.stateInfo))
			return false;
		return true;
	}

	/*	
	public boolean equals(Object other) {
		if(super.equals(other)) return true;
		if(other instanceof Permission) {
			Permission ot = (Permission) other;
			return this.getRootNode().equals(ot.getRootNode())
				&& this.getKind().equals(ot.getKind())
				&& this.getStateInfo().equals(ot.getKind());
		}
		return false;
	}
*/
	public boolean isUnique() {
		return getKind().equals(PermissionKind.UNIQUE);
	}

	public boolean isFull() {
		return getKind().equals(PermissionKind.FULL);
	}

	public boolean isShare() {
		return getKind().equals(PermissionKind.SHARE);
	}

	public boolean isImmutable() {
		return getKind().equals(PermissionKind.IMMUTABLE);
	}

	public boolean isPure() {
		return getKind().equals(PermissionKind.PURE);
	}

	/**
	 * @return
	 */
	public boolean isReadOnly() {
		return isPure() || isImmutable();
	}
	
	/**
	 * @return the permission kind
	 */
	protected PermissionKind getKind() {
		return kind;
	}

	/**
	 * @return the rootNode
	 */
	public String getRootNode() {
		return rootNode;
	}

	/**
	 * @return the stateSpace
	 */
	public StateSpace getStateSpace() {
		return stateSpace;
	}

	/**
	 * @return the stateInfo
	 */
	public String getStateInfo() {
		return stateInfo;
	}

	/**
	 * @param permission
	 * @return
	 */
	public Permission splitOff(Permission permission) {
		PermissionKind k = getKind().remainderWhenSplit(permission.getKind());
		if(k == null)
			return null;
		return new Permission(this.getStateSpace(), permission.getRootNode(), k);
	}
	
	/**
	 * @param ensuredState
	 * @return
	 */
	public Permission copyNewState(String newState) {
		if(newState == null) newState = getRootNode();
		return new Permission(getStateSpace(), getRootNode(), newState, getKind());
	}

	/**
	 * @return purified permission
	 */
	public Permission purify() {
		return new Permission(getStateSpace(), getRootNode(), getStateInfo(), getKind().purify());
	}

	/**
	 * @return permission with different kind.
	 */
	public Permission copyNewKind(PermissionKind newKind) {
		return new Permission(getStateSpace(), getRootNode(), getStateInfo(), newKind);
	}

	@Override
	public String toString() {
		return getKind().name() + "(" + getRootNode() + ") in " + getStateInfo(); 
	}

	public enum PermissionKind {
		UNIQUE {
			public boolean isStrongerThan(PermissionKind other) {
				return true;
			}
			public PermissionKind remainderWhenSplit(PermissionKind splitOff) {
				if(splitOff.equals(UNIQUE)) return null;
				if(splitOff.equals(FULL)) return PURE;
				if(splitOff.equals(SHARE)) return SHARE;
				if(splitOff.equals(IMMUTABLE)) return IMMUTABLE;
				return FULL;
			}
			public boolean isReadOnly() {
				return false;
			}
			public PermissionKind purify() {
				return IMMUTABLE;
			}
		},
		FULL {
			public boolean isStrongerThan(PermissionKind other) {
				if(other.equals(UNIQUE))
					return false;
				return true;
			}
			public PermissionKind remainderWhenSplit(PermissionKind splitOff) {
				if(splitOff.equals(UNIQUE)) return null;
				if(splitOff.equals(FULL)) return PURE;
				if(splitOff.equals(SHARE)) return SHARE;
				if(splitOff.equals(IMMUTABLE)) return IMMUTABLE;
				return FULL;
			}
			public boolean isReadOnly() {
				return false;
			}
			public PermissionKind purify() {
				return IMMUTABLE;
			}
		},
		SHARE {
			public boolean isStrongerThan(PermissionKind other) {
				if(other.equals(UNIQUE) || other.equals(FULL) || other.equals(IMMUTABLE))
					return false;
				return true;
			}
			public PermissionKind remainderWhenSplit(PermissionKind splitOff) {
				if(splitOff.equals(UNIQUE)) return null;
				if(splitOff.equals(FULL)) return null;
				if(splitOff.equals(SHARE)) return SHARE;
				if(splitOff.equals(IMMUTABLE)) return null;
				return SHARE;
			}
			public boolean isReadOnly() {
				return false;
			}
			public PermissionKind purify() {
				return PURE;
			}
		},
		IMMUTABLE {
			public boolean isStrongerThan(PermissionKind other) {
				if(other.equals(UNIQUE) || other.equals(FULL) || other.equals(SHARE))
					return false;
				return true;
			}
			public PermissionKind remainderWhenSplit(PermissionKind splitOff) {
				if(splitOff.equals(UNIQUE)) return null;
				if(splitOff.equals(FULL)) return null;
				if(splitOff.equals(SHARE)) return null;
				if(splitOff.equals(IMMUTABLE)) return IMMUTABLE;
				return IMMUTABLE;
			}
			public boolean isReadOnly() {
				return true;
			}
			public PermissionKind purify() {
				return IMMUTABLE;
			}
		},
		PURE {
			public boolean isStrongerThan(PermissionKind other) {
				if(other.equals(PURE))
					return true;
				return false;
			}
			public PermissionKind remainderWhenSplit(PermissionKind splitOff) {
				if(splitOff.equals(UNIQUE)) return null;
				if(splitOff.equals(FULL)) return null;
				if(splitOff.equals(SHARE)) return null;
				if(splitOff.equals(IMMUTABLE)) return null;
				return PURE;
			}
			public boolean isReadOnly() {
				return true;
			}
			public PermissionKind purify() {
				return PURE;
			}
		};
		
		public abstract boolean isStrongerThan(PermissionKind other);
		public abstract PermissionKind remainderWhenSplit(PermissionKind splitOff);
		public abstract boolean isReadOnly();
		public abstract PermissionKind purify();
	}

}
