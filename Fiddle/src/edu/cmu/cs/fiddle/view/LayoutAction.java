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

package edu.cmu.cs.fiddle.view;

import org.eclipse.gef.ui.actions.SelectionAction;

/**
 * An action that can be put in the tool bar of the state machine view
 * in order to layout the state machine diagram.
 * 
 * @author Nels Beckman
 * @date Nov 25, 2008
 *
 */
@SuppressWarnings("restriction")
public class LayoutAction extends SelectionAction { // NEB: As far as I know there
	                                                // is no reason we need to
	                                                // extend SelectionAction as
	                                                // opposed to the more basic
	                                                // actions.

	private StatechartView view;
	
	public LayoutAction(StatechartView view) {
		super(view);
		this.view = view;
		//this.setImageDescriptor(Helper.LAYOUT_ICON);
		this.setToolTipText("Layout the Diagram");
		this.setText("Layout Digram");
	}

	@Override
	protected boolean calculateEnabled() {
		return true;
	}

	@Override
	public void run() {
		//layoutStatechartPage();
    }
	
//	protected void layoutStatechartPage() {
//		try {
//			Shell shell = getWorkbenchPart().getSite().getWorkbenchWindow().getShell();
//			ProgressMonitorJobsDialog pmd = new ProgressMonitorJobsDialog(shell);
//
//			StateMachine gsm = view.getStateMachine();
//
//			if (gsm != null) {
//				pmd.setCancelable(true);
//				pmd.open();
//				Viewport viewport = view.getGraphicalViewer().getFigureCanvas().getViewport();
//				UpdateManager updateManager = viewport.getUpdateManager();
//				FastLayoutWrapper lp = new FastLayoutWrapper(gsm, updateManager);
//				pmd.run(true, true, lp);
//				Command layoutCommand = lp.getC();
//				pmd.close();
//				updateManager.addDirtyRegion(viewport, viewport.getBounds());
//				updateManager.performUpdate();
//
//				// Just execute the command. This means it cannot be undone,
//				// which is probably okay since we aren't saving the result.
//				layoutCommand.execute();				
//			}
//
//		} catch (Exception e) {
//			System.err.println("Layout error: "  + e.getMessage());
//			e.printStackTrace();
//		}
//	}
	
}
