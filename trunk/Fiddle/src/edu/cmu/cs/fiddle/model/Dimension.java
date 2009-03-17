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
package edu.cmu.cs.fiddle.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A model element that represents a dimension of a state.
 * 
 * @author Nels E. Beckman
 */
public class Dimension implements IDimension, IHasProperties {

	private String name;
	private final Set<IState> states;
	private final Set<IConnection> outgoingConnections;
	private final Set<IConnection> incomingConnections;
	
	private final PropertyChangeSupport listeners;
	
	public Dimension(String name, Collection<? extends IState> states) {
		this.name = name;
		this.states = new HashSet<IState>(states);
		this.outgoingConnections = new HashSet<IConnection>();
		this.incomingConnections = new HashSet<IConnection>();
		this.listeners = new PropertyChangeSupport(this);
	}
	
	@Override
	public void addState(IState state) {
		this.states.add(state);
		this.firePropertyChange(PropertyType.CHILDREN, null, state);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<IState> getStates() {
		return Collections.unmodifiableSet(states);
	}

	@Override
	public void addIncomingConnection(IConnection conn) {
		assert(conn.getTarget().equals(conn));
		this.incomingConnections.add(conn);
		this.firePropertyChange(PropertyType.CONNECTIONS, null, conn);
	}

	@Override
	public void addOutgoingConnection(IConnection conn) {
		assert(conn.getSource().equals(this));
		this.outgoingConnections.add(conn);
		this.firePropertyChange(PropertyType.CONNECTIONS, null, conn);
	}

	@Override
	public Set<IConnection> getIncomingConnections() {
		return Collections.unmodifiableSet(this.incomingConnections);
	}

	@Override
	public Set<IConnection> getOutgoingConnections() {
		return Collections.unmodifiableSet(this.outgoingConnections);
	}

	private void firePropertyChange(PropertyType type, Object oldValue, Object newValue) {
		this.listeners.firePropertyChange(type.toString(), oldValue, newValue);
	}
	
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.listeners.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.listeners.removePropertyChangeListener(listener);
	}

	@Override
	public boolean isParentOf(IConnectable child) {
		// Breadth-first
		for( IConnectable dim : this.getStates() ) {
			if( dim.equals(child) ) {
				return true;
			}
		}
		for( IConnectable dim : this.getStates() ) {
			if( dim.isParentOf(child) ) {
				return true;
			}
		}
		return false;
	}
}