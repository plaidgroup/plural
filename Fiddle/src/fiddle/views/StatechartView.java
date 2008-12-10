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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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

import com.evelopers.unimod.core.stateworks.State;
import com.evelopers.unimod.core.stateworks.StateMachine;
import com.evelopers.unimod.plugin.eclipse.model.GModel;
import com.evelopers.unimod.plugin.eclipse.model.GNormalState;
import com.evelopers.unimod.plugin.eclipse.model.GStateMachine;
import com.evelopers.unimod.plugin.eclipse.ui.base.MyEditDomain;
import com.evelopers.unimod.plugin.eclipse.ui.base.MyScrollingGraphicalViewer;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.internal.AbstractCrystalPlugin;
import edu.cmu.cs.crystal.internal.WorkspaceUtilities;
import edu.cmu.cs.plural.states.IInvocationSignature;
import edu.cmu.cs.plural.states.IMethodSignature;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.states.StateSpaceRepository;
import fiddle.parts.FStatechartPartFactory;


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

@SuppressWarnings("restriction")
public class StatechartView extends ViewPart implements ISelectionListener{

	private MyEditDomain editDomain;

	private MyScrollingGraphicalViewer graphicalViewer;
	
	private GStateMachine stateMachine;
	
	private boolean pin = false;
	
	private LayoutAction lact;
	
	public GStateMachine getStateMachine() {
		return stateMachine;
	}

	public void setStateMachine(GStateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	
	protected void setPin(boolean b){
		this.pin = b;
		System.err.println("pin = " + pin);
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
		
		/*GTransition my_transition = (GTransition) */new_state_machine.createTransition(
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
		addGraphPinAction();
		
		getViewSite().getPage().addPostSelectionListener(this);
	}

	private void addGraphLayoutAction() {
		Action action = new LayoutAction(this);
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager dropDownMenu = actionBars.getMenuManager();
		IToolBarManager toolBar = actionBars.getToolBarManager();
		dropDownMenu.add(action);
		toolBar.add(action);
		lact = (LayoutAction)action;
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
	
	private StateSpaceRepository getSpaceRepo() {
		Crystal crystal = AbstractCrystalPlugin.getCrystalInstance();
		final AnnotationDatabase annoDB = new AnnotationDatabase();
		crystal.registerAnnotationsWithDatabase(annoDB);
		StateSpaceRepository ssr = StateSpaceRepository.getInstance(annoDB);
		
		return ssr;
	}
	
	private GStateMachine getStateMachineFromIType(IType type) {
		StateSpaceRepository ssr = getSpaceRepo();
		
		
		// Testing out my code so that we can get the transitions
		ITypeBinding binding = WorkspaceUtilities.getDeclNodeFromType(type);
		for( IMethodBinding method : binding.getDeclaredMethods() ) {
			IInvocationSignature sig = ssr.getSignature(method);
			
			// Results...
			sig.getRequiredReceiverStateOptions();
			sig.getEnsuredReceiverStateOptions();
		}
		
		// That's good, but we also need the methods from all supertypes
		for( ITypeBinding face_bind : binding.getInterfaces() ) {
			
		}
		ITypeBinding super_type = binding.getSuperclass();
		// And we need to do this recursively...
		// Just so we don't miss any methods...
		
		
		// END NEB
		
		StateSpace space = ssr.getStateSpace(binding);
		
		GModel m = new GModel();
		GStateMachine machine = new GStateMachine(m);
		m.addStateMachine(machine);
		addChildNodes( machine.getTop(), machine, space.getRootState(), space);
		
		return machine;
	}
	
	private void addChildNodes(State state, StateMachine machine, String str, StateSpace space) {
		State newState = new GNormalState((GStateMachine) machine);
		newState.setName(str);
		state.addSubstate(newState);
		for(String s: space.getChildNodes(str)) {
			addChildNodes(newState, machine, s, space);
		}
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
		        		GStateMachine machine = getStateMachineFromIType(type);
		        		setStateMachine(machine);
		        		getGraphicalViewer().setContents(getStateMachine().getTop());
		        		getGraphicalViewer().getControl().setBackground(
		        				ColorConstants.listBackground);
		        		getViewSite().getPage().addPostSelectionListener(this);
		        		lact.layoutStatechartPage();
		        	}
		        }
		      }
		    }
	}
	
}
