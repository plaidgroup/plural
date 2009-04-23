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
package edu.cmu.cs.fiddle.editpart;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ManhattanConnectionRouter;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.CompoundDirectedGraph;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.Subgraph;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import edu.cmu.cs.fiddle.figure.BendpointSelfConnectionRouter;
import edu.cmu.cs.fiddle.model.IState;
import edu.cmu.cs.fiddle.model.StateMachine;
import edu.cmu.cs.fiddle.model.IHasProperties.PropertyType;

/**
 * The top-level edit part, which corresponds with the StateMachine model
 * element and which exists just to show all of the children.
 * 
 * @author Nels E. Beckman
 */
public class TopLevelEditPart extends AbstractGraphicalEditPart
	implements PropertyChangeListener, INodeToGraphContributor {



	@Override
	protected IFigure createFigure() {
		// This code is directly from the tutorial
		Figure f = new Figure();
        f.setOpaque(true);

        f.setLayoutManager(new AutomaticLayoutManager(this));
        
        ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
		cLayer.setConnectionRouter(new ManhattanConnectionRouter());
		
        return f;
	}

	@Override
	protected void createEditPolicies() {
	}

	@Override
	protected List<?> getModelChildren() {
		return new ArrayList<IState>(modelAsStateMachine().getStates());
	}

	private StateMachine modelAsStateMachine() {
		return (StateMachine)getModel();
	}

	@Override
	public void activate() {
		super.activate();
		modelAsStateMachine().addPropertyChangeListener(this);
	}

	@Override
	public void deactivate() {
		super.deactivate();
		modelAsStateMachine().removePropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if( evt.getPropertyName().equals(PropertyType.CHILDREN.toString()) ) {
			this.refreshChildren();
		}
	}

	/**
	 * Make the model associated with this edit part and (recursively) its
	 * children into a model by updating the given graph and map. Do this
	 * by adding Dimensions and States as nodes. 
	 */
	@Override
	public void contributeNodesToGraph(CompoundDirectedGraph graph,
			Subgraph s, Map<EditPart, Object> partsToNodesOrEdges) {
		/*
		 * Copied from StructuredActivityPart, this method
		 * adds this thing as a graph node and then recurses on
		 * the children.
		 */
		Subgraph me = new Subgraph(this, s);
		me.outgoingOffset = 5;
		me.incomingOffset = 5;
		
		me.innerPadding = INNER_PADDING;
		me.setPadding(PADDING);
		partsToNodesOrEdges.put(this, me);
		
		@SuppressWarnings({"unused", "unchecked"})
		boolean DONTUSE = graph.nodes.add(me);
		
		for( Object o : this.getChildren() ) {
			INodeToGraphContributor child = (INodeToGraphContributor)o;
			child.contributeNodesToGraph(graph, me, partsToNodesOrEdges);
		}
	}

	/**
	 * Make the model associated with this edit part and (recursively) its
	 * children into a model by updating the given graph and map. Do this
	 * by adding IConnections as edges.
	 */
	@Override
	public void contributeEdgesToGraph(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodesOrEdges) {
		// We have no edges. Make our children contribute
		// their edges.
		for( Object child_ : this.getChildren() ) {
			INodeToGraphContributor child = (INodeToGraphContributor)child_;
			child.contributeEdgesToGraph(graph, partsToNodesOrEdges);
		}
	}

	/**
	 * Takes the results of performing a graph layout and
	 * applies them back to the model elements of this edit
	 * part and its children.
	 */
	public void applyGraphResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		// Straight from StructuredActivityPart
		applyOwnResults(graph, partsToNodes);
		applyChildrenResults(graph, partsToNodes);
	}

	/**
	 * Apply the graph to the children of this edit part.
	 */
	private void applyChildrenResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		
		for( Object child_ : this.getChildren() ) {
			INodeToGraphContributor child = (INodeToGraphContributor)child_;
			child.applyGraphResults(graph, partsToNodes);
		}
	}

	/**
	 * Apply the results of graph layout to this figure.
	 */
	private void applyOwnResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		Node n = (Node)partsToNodes.get(this);
		getFigure().setBounds(new Rectangle(n.x, n.y, n.width, n.height));
	}	
}
