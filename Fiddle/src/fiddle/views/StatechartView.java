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
package fiddle.views;


import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import com.evelopers.unimod.plugin.eclipse.model.GModel;
import com.evelopers.unimod.plugin.eclipse.model.GNormalState;
import com.evelopers.unimod.plugin.eclipse.model.GStateMachine;
import com.evelopers.unimod.plugin.eclipse.model.GTransition;
//import com.evelopers.unimod.plugin.eclipse.model.GTransition;
import com.evelopers.unimod.plugin.eclipse.ui.base.MyEditDomain;
import com.evelopers.unimod.plugin.eclipse.ui.base.MyScrollingGraphicalViewer;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class StatechartView extends ViewPart {

	private MyEditDomain editDomain;

	private MyScrollingGraphicalViewer graphicalViewer;
	
	private GStateMachine stateMachine;
	
	public GStateMachine getStateMachine() {
		return stateMachine;
	}

	public void setStateMachine(GStateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}

	// Just create some simple model, like we always do.
	private GStateMachine createModelForTestPurposes() {
		GModel m = new GModel();
		
		GStateMachine new_state_machine = new GStateMachine(m);
		new_state_machine.setName("Nels state machine #" +  
		    System.currentTimeMillis());
		m.addStateMachine(new_state_machine);
						
		GNormalState my_first_state = new GNormalState(new_state_machine);
		new_state_machine.getTop().addSubstate(my_first_state);
		
		GNormalState my_second_state = new GNormalState(new_state_machine);
		new_state_machine.getTop().addSubstate(my_second_state);
		
		GTransition my_transition = (GTransition) new_state_machine.createTransition(
				my_first_state, my_second_state, new_state_machine.createGuard("o1.x1"), new_state_machine.createEvent("e1"));
		
		return new_state_machine;
	}
	
	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		setEditDomain(new MyEditDomain(null)); // NEB: Normally takes an editor
		setGraphicalViewer(new MyScrollingGraphicalViewer());
		getGraphicalViewer().createControl(parent);
		getGraphicalViewer().setRootEditPart(
				new FreeformGraphicalRootEditPart());
		getGraphicalViewer().setEditPartFactory(new FStatechartPartFactory());
		
		setStateMachine(createModelForTestPurposes());
		getGraphicalViewer().setContents(getStateMachine().getTop());
		
		getGraphicalViewer().getControl().setBackground(
				ColorConstants.listBackground);
		
		// Add the menu that performs graph layout
		addGraphLayoutAction();
	}

	private void addGraphLayoutAction() {
		Action action = new LayoutAction(this);
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager dropDownMenu = actionBars.getMenuManager();
		IToolBarManager toolBar = actionBars.getToolBarManager();
		dropDownMenu.add(action);
		toolBar.add(action);		
	}

	/**
	 * Get the EditDomain for this ViewPart.
	 * 
	 * @return the EditDomain for this ViewPart.
	 */
	protected MyEditDomain getEditDomain() {
		return this.editDomain;
	}

	/**
	 * Returns the graphical viewer.
	 * 
	 * @return the graphical viewer
	 */
	protected MyScrollingGraphicalViewer getGraphicalViewer() {
		return this.graphicalViewer;
	}

	/**
	 * Sets the EditDomain for this ViewPart.
	 * 
	 * @param anEditDomain
	 *            the EditDomain for this ViewPart.
	 */
	protected void setEditDomain(MyEditDomain anEditDomain) {
		this.editDomain = anEditDomain;
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		getGraphicalViewer().getControl().setFocus();
	}

	/**
	 * Sets the graphicalViewer for this EditorPart.
	 * 
	 * @param viewer
	 *            the graphical viewer
	 */
	protected void setGraphicalViewer(MyScrollingGraphicalViewer viewer) {
		getEditDomain().addViewer(viewer);
		this.graphicalViewer = viewer;
	}

}
