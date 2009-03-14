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

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.CompoundDirectedGraph;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.Subgraph;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import edu.cmu.cs.fiddle.figure.DimensionFigure;
import edu.cmu.cs.fiddle.model.Dimension;
import edu.cmu.cs.fiddle.model.IConnection;
import edu.cmu.cs.fiddle.model.IState;
import edu.cmu.cs.fiddle.model.IHasProperties.PropertyType;

/**
 * The edit part for a dimension. 
 * 
 * @author Nels E. Beckman
 * @see {@link Dimension}
 */
public class DimensionEditPart extends AbstractGraphicalEditPart implements
		NodeEditPart, PropertyChangeListener, INodeToGraphContributor {

	@Override
	protected IFigure createFigure() {
		// This is the last dimension if this is the last
		// edit part in our parent's list of edit parts.
		// This is a hack... is there a better way to do this?
		int last_index_of = ((IState)this.getParent().getModel()).getDimensions().lastIndexOf(this.getModel());
		boolean isLastDim = last_index_of == ((IState)this.getParent().getModel()).getDimensions().size() - 1;
		
		return new DimensionFigure(modelAsDimension().getName(), isLastDim);
	}

	@Override protected void createEditPolicies() {}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(
			ConnectionEditPart connection) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		return new ChopboxAnchor(getFigure());
	}

	private Dimension modelAsDimension() {
		return (Dimension)getModel();
	}
	
	@Override
	public void activate() {
		super.activate();
		modelAsDimension().addPropertyChangeListener(this);
	}

	@Override
	public void deactivate() {
		super.deactivate();
		modelAsDimension().removePropertyChangeListener(this);
	}

	@Override
	public IFigure getContentPane() {
		return ((DimensionFigure)getFigure()).getContents();
	}

	@Override
	protected List<?> getModelSourceConnections() {
		return new ArrayList<IConnection>(modelAsDimension().getOutgoingConnections());
	}

	@Override
	protected List<?> getModelTargetConnections() {
		return new ArrayList<IConnection>(modelAsDimension().getIncomingConnections());
	}

	@Override
	protected List<?> getModelChildren() {
		return new ArrayList<IState>(modelAsDimension().getStates());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if( evt.equals(PropertyType.CHILDREN.toString()) ) {
			this.refreshChildren();
		}
		else if( evt.equals(PropertyType.CONNECTIONS.toString()) ) {
			this.refreshSourceConnections();
			this.refreshTargetConnections();
		}
	}

	@Override
	public void contributeEdgesToGraph(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodesOrEdges) {
		// Make edges contribute themselves...
		for( Object connection_ : getSourceConnections() ) {
			IEdgeToGraphContributor connection = (IEdgeToGraphContributor)connection_;
			connection.contributeToGraph(graph, partsToNodesOrEdges);
		}
		// Then recurse on children.
		for( Object child_ : this.getChildren() ) {
			INodeToGraphContributor child = (INodeToGraphContributor)child_;
			child.contributeEdgesToGraph(graph, partsToNodesOrEdges);
		}
		
	}

	@Override
	public void contributeNodesToGraph(CompoundDirectedGraph graph, Subgraph s,
			Map<EditPart, Object> partsToNodesOrEdges) {
		Subgraph me = new Subgraph(this, s);
		me.outgoingOffset = 5;
		me.incomingOffset = 5;
		
		// Set minimum sizes but a state can be bigger
		me.width = DimensionFigure.MIN_WIDTH;
		me.insets.top = DimensionFigure.HEADER_SIZE;
		me.insets.left = 0;
		
			
		me.innerPadding = INNER_PADDING;
		me.setPadding(PADDING);
		partsToNodesOrEdges.put(this, me);
		
		@SuppressWarnings({"unused", "unchecked"})
		boolean DONTUSE = graph.nodes.add(me);
		
		for( Object child_ : this.getChildren() ) {
			INodeToGraphContributor child = (INodeToGraphContributor)child_;
			child.contributeNodesToGraph(graph, me, partsToNodesOrEdges);
		}
	}

	@Override
	public void applyGraphResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		applyOwnResults(graph, partsToNodes);
		applyChildrenResults(graph, partsToNodes);
	}

	/**
	 * Recurse on children, applying the results of graph
	 * layout.
	 */
	private void applyChildrenResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		for( Object child_ : this.getChildren() ) {
			INodeToGraphContributor child = (INodeToGraphContributor)child_;
			child.applyGraphResults(graph, partsToNodes);
		}
	}

	/**
	 * Apply the results of graph layout to yourself, adjusting
	 * the size of your figure.
	 */
	private void applyOwnResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		// Change your bounds...
		Node n = (Node)partsToNodes.get(this);
		getFigure().setBounds(new Rectangle(n.x, n.y, n.width, n.height));
		// Layout connections.
		for( Object connection_ : getSourceConnections() ) {
			IEdgeToGraphContributor connection = (IEdgeToGraphContributor)connection_;
			connection.applyGraphResults(graph, partsToNodes);
		}
	}
}
