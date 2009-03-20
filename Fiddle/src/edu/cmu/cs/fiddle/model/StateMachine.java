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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.internal.AbstractCrystalPlugin;
import edu.cmu.cs.crystal.internal.WorkspaceUtilities;
import edu.cmu.cs.plural.states.IInvocationSignature;
import edu.cmu.cs.plural.states.StateSpace;
import edu.cmu.cs.plural.states.StateSpaceRepository;

/**
 * There is only one state machine in the model. It is the top
 * level element.
 * 
 * @author Nels E. Beckman
 */
public class StateMachine implements IHasProperties {

	// Mutable
	private final Set<IState> topLevelStates;
	
	private PropertyChangeSupport listeners;
	
	public StateMachine() {
		this.topLevelStates = new HashSet<IState>(); 
		this.listeners = new PropertyChangeSupport(this);
	}
	
	public Set<IState> getStates() {
		return Collections.unmodifiableSet(topLevelStates);
	}
	
	public void addState(IState state) {
		this.topLevelStates.add(state);
		firePropertyChange(PropertyType.CHILDREN, null, state);
	}

	private void firePropertyChange(PropertyType prop, Object oldValue, Object newValue) {
		this.listeners.firePropertyChange(prop.toString(), oldValue, newValue);
	}
	
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.listeners.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.listeners.removePropertyChangeListener(listener);
	}
	
	public static StateMachine getStateMachineFromIType(IType type) {
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
	
	private static StateSpaceRepository getSpaceRepo(AnnotationDatabase annoDB) {
		Crystal crystal = AbstractCrystalPlugin.getCrystalInstance();
		crystal.registerAnnotationsWithDatabase(annoDB);
		StateSpaceRepository ssr = StateSpaceRepository.getInstance(annoDB);
		
		return ssr;
	}
	
	/**
	 * @param rootState
	 * @param space
	 * @param stringToNode 
	 * @return
	 */
	private static Collection<IDimension> makeDimensionsInState(String parentState,
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
	private static Collection<? extends IState> makeStatesInDimension(String parentDim,
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
	
	private static void addTransitions(ITypeBinding binding, StateMachine machine, StateSpaceRepository ssr, Map<String, IConnectable> stringToNode){
		List<IMethodBinding> methods = getClassMethods(binding, ssr);
		StateSpace space = ssr.getStateSpace(binding);
		
		for( IMethodBinding method : methods ) {
			IInvocationSignature sig = ssr.getSignature(method);
			// Results...
			for(Set<String> set : sig.getRequiredReceiverStateOptions()){
				for(String start : set){
					for(Set<String> fset : sig.getEnsuredReceiverStateOptions()){
						for(String finish : fset){
							IConnectable s1 = stringToNode.get(start);
							IConnectable s2 = stringToNode.get(finish);
							
							if(null != s1 && null != s2) {
								if(!space.areOrthogonal(start, finish))
									createTransition(s1, s2, machine, method);
							}
						}						
					}

				}
			}

		}
		
		for( ITypeBinding face_bind : binding.getInterfaces() ) {
			addTransitions(face_bind, machine, ssr, stringToNode);
		}
		
		ITypeBinding super_type = binding.getSuperclass();
		if (super_type!=null) addTransitions(super_type, machine, ssr, stringToNode);
	}
	
	private static void createTransition(IConnectable s1, IConnectable s2, StateMachine machine, IMethodBinding method) {
		IConnection con = Connection.connectTwoIConnectables(s1, s2);
		con.setName(method.getName());
		con.setHover(extractSignature(method));
	}
	
	private static String extractSignature(IMethodBinding method){
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
	
	private static List <IMethodBinding> getClassMethods( ITypeBinding binding, StateSpaceRepository ssr ) {
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
	
}
