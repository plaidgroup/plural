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

package fiddle.parts;

import java.beans.PropertyChangeEvent;

import com.evelopers.unimod.core.stateworks.State;
import com.evelopers.unimod.plugin.eclipse.editpart.NormalStateEditPart;
import com.evelopers.unimod.plugin.eclipse.model.GState;

public class FDimensionEditPart extends NormalStateEditPart {
    @Override
	protected void sendProblems() {}
    
    /* 
	 * We want to get rid of the calls to remove on the breakpoint manager.
	 * This is a hack, and we need a better general solution for things
	 * to do when an Editor is required. Maybe storing one in StatechartView
	 * is the right way to go...
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String property_name = evt.getPropertyName();
		
		if(property_name.equals(GState.NAME_PROPERTY) || 
		   property_name.equals(GState.SIZE_PROPERTY) ||
		   property_name.equals(GState.LOCATION_PROPERTY)) {
			// just delegate
			super.propertyChange(evt);
		}
		else if(property_name.equals(equals(State.SUBSTATES_PROPERTY)) ) {
			this.refreshChildren();
		}
		else if(property_name.equals(State.OUTGOING_TRANSITIONS_PROPERTY) ) {
			this.refreshSourceConnections();
		}
		else if(property_name.equals(State.INCOMING_TRANSITIONS_PROPERTY) ) {
			this.refreshTargetConnections();
		}
		else {
			// In case of any future developments...
			super.propertyChange(evt);
		}
	}
	
}
