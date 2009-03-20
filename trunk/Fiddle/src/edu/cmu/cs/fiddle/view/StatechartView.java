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


import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.cs.fiddle.editpart.StateEditPartFactory;
import edu.cmu.cs.fiddle.model.StateMachine;


/**
 * The view that holds our state machine. This is better b/c
 * we don't want to have to take up the entire editor with our
 * state machine. Presumably the user will want to use their
 * Java editor when writing Java.
 * 
 * @author Nels Beckman
 * @date Nov 25, 2008
 *
 */
public class StatechartView extends ViewPart implements ISelectionListener {

	private EditDomain editDomain;

	private ScrollingGraphicalViewer graphicalViewer;

	private StateMachine stateMachine;

	private boolean pin = false;

	public StateMachine getStateMachine() {
		return stateMachine;
	}

	public void setStateMachine(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}

	protected void setPin(boolean b){
		this.pin = b;
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		setGraphicalViewer(new ScrollingGraphicalViewer());
		getGraphicalViewer().createControl(parent);
		getGraphicalViewer().setEditPartFactory(new StateEditPartFactory());

		setStateMachine(new StateMachine());
		getGraphicalViewer().setContents(getStateMachine());

		getGraphicalViewer().getControl().setBackground(
				ColorConstants.listBackground);

		// Add the menu that performs graph layout
		addGraphPinAction();

		getViewSite().getPage().addPostSelectionListener(this);
	}

	private void addGraphPinAction() {
		Action action = new PinAction(this);
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
	protected EditDomain getEditDomain() {
		return this.editDomain;
	}

	/**
	 * Returns the graphical viewer.
	 * 
	 * @return the graphical viewer
	 */
	private ScrollingGraphicalViewer getGraphicalViewer() {
		return this.graphicalViewer;
	}

	/**
	 * Sets the graphicalViewer for this EditorPart.
	 * 
	 * @param scrollingGraphicalViewer
	 *            the graphical viewer
	 */
	private void setGraphicalViewer(ScrollingGraphicalViewer scrollingGraphicalViewer) {
		this.graphicalViewer = scrollingGraphicalViewer;
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		getGraphicalViewer().getControl().setFocus();
	}
	
	private static <T> List<T> list(T... ts) {
		ArrayList<T> result = new ArrayList<T>(ts.length);
		for( T t : ts ) {
			result.add(t);
		}
		return result;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(!pin && selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (!ss.isEmpty()) {
				Object fe = ss.getFirstElement();
				if (fe instanceof IJavaElement) {
					IType type = null;
					IJavaElement ije = (IJavaElement) fe;

					if (ije instanceof CompilationUnit) {
						ICompilationUnit icu = (CompilationUnit) fe;
						type = icu.findPrimaryType();
					} else if (ije instanceof IMethod) {
						IMethod im = (IMethod) ije;
						type = im.getDeclaringType();
					} else if (ije instanceof IType) {
						type = (IType) ije;
					}
					if (type!=null){
						StateMachine machine = StateMachine.getStateMachineFromIType(type);
						setStateMachine(machine);
						getGraphicalViewer().setContents(getStateMachine());
//						getGraphicalViewer().getControl().setBackground(
//								ColorConstants.listBackground);
						getViewSite().getPage().addPostSelectionListener(this);
					}
				}
			}
		}
	}

}
