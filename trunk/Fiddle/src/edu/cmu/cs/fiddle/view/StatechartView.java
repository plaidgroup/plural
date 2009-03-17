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


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
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

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.internal.AbstractCrystalPlugin;
import edu.cmu.cs.crystal.internal.WorkspaceUtilities;
import edu.cmu.cs.fiddle.editpart.StateEditPartFactory;
import edu.cmu.cs.fiddle.model.Connection;
import edu.cmu.cs.fiddle.model.Dimension;
import edu.cmu.cs.fiddle.model.FinalState;
import edu.cmu.cs.fiddle.model.IConnectable;
import edu.cmu.cs.fiddle.model.IDimension;
import edu.cmu.cs.fiddle.model.IState;
import edu.cmu.cs.fiddle.model.InitialState;
import edu.cmu.cs.fiddle.model.State;
import edu.cmu.cs.fiddle.model.StateMachine;
import edu.cmu.cs.plural.states.IInvocationSignature;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.states.StateSpaceRepository;


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

	//private LayoutAction lact;

	public StateMachine getStateMachine() {
		return stateMachine;
	}

	public void setStateMachine(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}

	protected void setPin(boolean b){
		this.pin = b;
	}

	// Just create some simple model, like we always do.
//	private GStateMachine createModelForTestPurposes() {
//		GModel m = new GModel();
//
//		GStateMachine new_state_machine = new GStateMachine(m);
//		new_state_machine.setName("Nels state machine #" +  
//				System.currentTimeMillis());
//		m.addStateMachine(new_state_machine);
//
//		GNormalState my_first_state = new GNormalState(new_state_machine);
//		new_state_machine.getTop().addSubstate(my_first_state);
//
//		GNormalState my_second_state = new GNormalState(new_state_machine);
//		new_state_machine.getTop().addSubstate(my_second_state);
//
//		/*GTransition my_transition = (GTransition) */new_state_machine.createTransition(
//				my_first_state, my_second_state, new_state_machine.createGuard("o1.x1"), new_state_machine.createEvent("e1"));
//
//		return new_state_machine;
//	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		//setEditDomain(new MyEditDomain(null)); // NEB: Normally takes an editor
		setGraphicalViewer(new ScrollingGraphicalViewer());
		getGraphicalViewer().createControl(parent);
//		getGraphicalViewer().setRootEditPart(
//				new FreeformGraphicalRootEditPart());
		getGraphicalViewer().setEditPartFactory(new StateEditPartFactory());

		setStateMachine(new StateMachine());
		getGraphicalViewer().setContents(getStateMachine());

		getGraphicalViewer().getControl().setBackground(
				ColorConstants.listBackground);

		// Add the menu that performs graph layout
		// addGraphLayoutAction();
		addGraphPinAction();

		getViewSite().getPage().addPostSelectionListener(this);
	}

//	private void addGraphLayoutAction() {
//		Action action = new LayoutAction(this);
//		IActionBars actionBars = getViewSite().getActionBars();
//		IMenuManager dropDownMenu = actionBars.getMenuManager();
//		IToolBarManager toolBar = actionBars.getToolBarManager();
//		dropDownMenu.add(action);
//		toolBar.add(action);
//		lact = (LayoutAction)action;
//	}

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
		//getEditDomain().addViewer(viewer);
		this.graphicalViewer = scrollingGraphicalViewer;
	}
	
	/**
	 * Sets the EditDomain for this ViewPart.
	 * 
	 * @param anEditDomain
	 *            the EditDomain for this ViewPart.
	 */
//	protected void setEditDomain(MyEditDomain anEditDomain) {
//		this.editDomain = anEditDomain;
//	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		getGraphicalViewer().getControl().setFocus();
	}
	
	private StateSpaceRepository getSpaceRepo(AnnotationDatabase annoDB) {
		Crystal crystal = AbstractCrystalPlugin.getCrystalInstance();
		crystal.registerAnnotationsWithDatabase(annoDB);
		StateSpaceRepository ssr = StateSpaceRepository.getInstance(annoDB);
		
		return ssr;
	}
	
	private void addTransitions(ITypeBinding binding, StateMachine machine, StateSpaceRepository ssr, Map<String, IConnectable> stringToNode){
		List<IMethodBinding> methods = getClassMethods(binding, ssr);
		
		for( IMethodBinding method : methods ) {
			IInvocationSignature sig = ssr.getSignature(method);
			String start = "";
			String finish = "";
			
			// Results...
			for(Set<String> set : sig.getRequiredReceiverStateOptions()){
				for(String s : set){
					start = s;
				}
			}
			
			if(method.isConstructor()) start = "Initial";
			
			for(Set<String> set : sig.getEnsuredReceiverStateOptions()){
				for(String s : set){
					finish = s;
				}
			}

			IConnectable s1 = stringToNode.get(start);
			IConnectable s2 = stringToNode.get(finish);
			if(null != s1 && null != s2) {
				createTransition(s1, s2, machine, method);
			}
		}
		
		for( ITypeBinding face_bind : binding.getInterfaces() ) {
			addTransitions(face_bind, machine, ssr, stringToNode);
		}
		
		ITypeBinding super_type = binding.getSuperclass();
		if (super_type!=null) addTransitions(super_type, machine, ssr, stringToNode);
	}
	
	private void createTransition(IConnectable s1, IConnectable s2, StateMachine machine, IMethodBinding method) {
		String sig = extractSignature(method);
		Connection.connectTwoIConnectables(s1, s2);
		//machine.createTransition(s1, s2, machine.createGuard(sig), ((GStateMachine) machine).createEvent(method.getName()));
	}
	
	private String extractSignature(IMethodBinding method){
		if(method == null) return "";
		StringBuffer sb = new StringBuffer();
		sb.append(method.getName() + "(");
		
		ITypeBinding[] pt = method.getParameterTypes();
		if(Array.getLength(pt) != 0) {
			for (ITypeBinding itb: pt) {
				sb.append(itb.getName() + ", ");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append(")");
		sb.append(" : ");
		sb.append(method.getReturnType().getName());
		
		return sb.toString();
	}
	
	private List <IMethodBinding> getClassMethods( ITypeBinding binding, StateSpaceRepository ssr ) {
		List <IMethodBinding> methods = new ArrayList <IMethodBinding> ();
		List <String> names = new ArrayList <String> ();
		IMethodBinding[] arr = binding.getDeclaredMethods();	
		
		for( IMethodBinding met : arr ) {
			methods.add(met);
			names.add(met.getName());
		}
		
		if( binding.getSuperclass() != null ) {
			for( IMethodBinding met : getClassMethods(binding.getSuperclass(), ssr) ){
				if ( (! names.contains(met.getName())) && (! methods.contains(met)) ) {
					methods.add(met);
					names.add(met.getName());
				}
			}
		}
		
		if( Array.getLength(binding.getInterfaces()) != 0 ) {
			for(ITypeBinding it : binding.getInterfaces() ) {
				for( IMethodBinding met : getClassMethods(it, ssr) ){
					if ( (! names.contains(met.getName())) && (! methods.contains(met)) ) {
						methods.add(met);
						names.add(met.getName());
					}
				}
			}
		}
		
		return methods;
	}
	
	private static <T> List<T> list(T... ts) {
		ArrayList<T> result = new ArrayList<T>(ts.length);
		for( T t : ts ) {
			result.add(t);
		}
		return result;
	}
	
	private StateMachine getPracticeModel() {
		StateMachine machine = new StateMachine();
		IState initialState = new InitialState();
		machine.addState(initialState);
		
		// two dimensions
		// two states per dim
		IState fast_state = new State("Fast", Collections.<IDimension>emptyList());
		IState slow_state = new State("Slow", Collections.<IDimension>emptyList());
		Connection.connectTwoIConnectables(slow_state, fast_state);
		Dimension speed_dim = new Dimension("Speed", list(fast_state, slow_state));
		
		IState open_state = new State("Open", Collections.<IDimension>emptyList());
		IState closed_state = new State("Closed", Collections.<IDimension>emptyList());
		Connection.connectTwoIConnectables(open_state, closed_state);
		Dimension protocol_dim =  new Dimension("Protocol", list(open_state,closed_state));
		
		IState aliveState = new State("Alive", list(speed_dim, protocol_dim));
		machine.addState(aliveState);
		Connection.connectTwoIConnectables(initialState, aliveState);
		
		IState finalState = new FinalState();
		machine.addState(finalState);
		Connection.connectTwoIConnectables(aliveState, finalState);
		
		
		return machine;
	}
	
	/**
	 * @param rootState
	 * @param space
	 * @param stringToNode 
	 * @return
	 */
	private Collection<IDimension> makeDimensionsInState(String parentState,
			StateSpace space, Map<String, IConnectable> stringToNode) {
		List<IDimension> result = new ArrayList<IDimension>();
		for( String childDim : space.getChildNodes(parentState) ) {
			Dimension childDim_ = new Dimension(childDim, 
					makeStatesInDimension(childDim, space, stringToNode));
			
			// Add to map so that we can later find this dim
			stringToNode.put(childDim, childDim_);
			
			result.add(childDim_);
		}
		return result;
	}
	
	/**
	 * @param childDim
	 * @param space
	 * @param stringToNode 
	 * @return
	 */
	private Collection<? extends IState> makeStatesInDimension(String parentDim,
			StateSpace space, Map<String, IConnectable> stringToNode) {
		List<IState> result = new ArrayList<IState>();
		for( String childState : space.getChildNodes(parentDim) ) {
			IState childState_ = new State(childState, 
					makeDimensionsInState(childState, space, stringToNode));
			
			// Add to map so that we can later find this dim
			stringToNode.put(childState, childState_);
			
			result.add(childState_);
		}
		return result;
	}

	private StateMachine getStateMachineFromIType(IType type) {
		final AnnotationDatabase annoDB = new AnnotationDatabase();
		StateSpaceRepository ssr = getSpaceRepo(annoDB);
		ITypeBinding binding = WorkspaceUtilities.getDeclNodeFromType(type);
		StateSpace space = ssr.getStateSpace(binding);
		
		StateMachine machine = new StateMachine();
		IState initialState = new InitialState();
		machine.addState(initialState);
		
		// Here's a map from node names to nodes
		// We will need this so we can later find the nodes to
		// connect them together.
		Map<String, IConnectable> stringToNode = new HashMap<String, IConnectable>();
		
		// After adding the initial state, add the root
		// state to the machine.
		Collection<IDimension> root_dims = makeDimensionsInState(space.getRootState(), space, stringToNode);
		IState rootState = new State(space.getRootState(), root_dims);
		machine.addState(rootState);
		stringToNode.put(space.getRootState(), rootState);
		
		Connection.connectTwoIConnectables(initialState, rootState);
		
		addTransitions(binding, machine, ssr, stringToNode);
		
		return machine;
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
						StateMachine machine = getStateMachineFromIType(type);
						setStateMachine(machine);
						getGraphicalViewer().setContents(getStateMachine());
//						getGraphicalViewer().getControl().setBackground(
//								ColorConstants.listBackground);
						getViewSite().getPage().addPostSelectionListener(this);
						//lact.layoutStatechartPage();
					}
				}
			}
		}
	}

}