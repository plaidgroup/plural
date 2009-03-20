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

/**
 * A connection for connecting States/Dimensions.
 * 
 * @author Nels E. Beckman
 *
 */
public class Connection implements IConnection, IHasProperties {

	private IConnectable source;
	private IConnectable target;
	
	private String name;

	private PropertyChangeSupport listeners;
	
	/**
	 * Creates a new IConnection and connects it to the source and target, 
	 * additionally calling the add methods on both source and target.
	 */
	public static IConnection connectTwoIConnectables(IConnectable source, IConnectable target) {
		IConnection result = new Connection(source, target);
		source.addOutgoingConnection(result);
		target.addIncomingConnection(result);
		return result;
	}
	
	public Connection(IConnectable source, IConnectable target) {
		super();
		this.source = source;
		this.target = target;
		this.listeners = new PropertyChangeSupport(this);
	}

	@Override
	public IConnectable getSource() {
		return source;
	}

	@Override
	public IConnectable getTarget() {
		return target;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@SuppressWarnings("unused")
	private void firePropertyChange(PropertyType prop, Object oldValue, Object newValue) {
		this.listeners.firePropertyChange(prop.toString(), oldValue, newValue);
	}
	
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}
}
