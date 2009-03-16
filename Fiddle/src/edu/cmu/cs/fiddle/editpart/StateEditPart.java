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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

import edu.cmu.cs.fiddle.figure.StateFigure;
import edu.cmu.cs.fiddle.model.State;
import edu.cmu.cs.fiddle.model.IHasProperties.PropertyType;

/**
 * The edit part for the State class.
 * 
 * @author Nels E. Beckman
 * @see {@link State}
 */
public class StateEditPart extends AbstractGraphicalEditPart 
	implements NodeEditPart, PropertyChangeListener, INodeToGraphContributor {

	// This is created on activate and
	// destroyed on dispose
	private org.eclipse.swt.graphics.Font labelFont = new Font(Display.getDefault(), "Times New Roman", 10, SWT.NORMAL);
	
	@Override
	protected IFigure createFigure() {
		return new StateFigure(getModelAsState().getName(), labelFont);
	}

	@Override protected void createEditPolicies() {	}

	@Override
	protected List<?> getModelChildren() {
		return getModelAsState().getDimensions();
	}
	
	@Override
	protected List<?> getModelSourceConnections() {
		return new ArrayList<Object>(getModelAsState().getOutgoingConnections());
	}

	@Override
	protected List<?> getModelTargetConnections() {
		return new ArrayList<Object>(getModelAsState().getIncomingConnections());
	}

	@Override
	public IFigure getContentPane() {
		return ((StateFigure)getFigure()).getContents();
	}
	
	private State getModelAsState() {
		return (State)getModel();
	}

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

	@Override
	public void activate() {
		super.activate();
		getModelAsState().addPropertyChangeListener(this);
	}

	@Override
	public void deactivate() {
		super.deactivate();
		getModelAsState().removePropertyChangeListener(this);
		// I dispose in deactivate, but install in the constructor
		// because the createFigure is called before activate!!
		labelFont.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if( evt.getPropertyName().equals(PropertyType.CHILDREN.toString()) ) {
			this.refreshChildren();
		} else if( evt.getPropertyName().equals(PropertyType.CONNECTIONS) ) {
			this.refreshSourceConnections();
			this.refreshTargetConnections();
		} else if( evt.getPropertyName().equals(PropertyType.LOCATION) ||
				   evt.getPropertyName().equals(PropertyType.SHAPE) ) {
			this.refreshVisuals();
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
		me.width = StateFigure.MIN_WIDTH;
		me.insets.top = StateFigure.HEADER_SIZE;
		me.insets.bottom = StateFigure.FOOTER_SIZE;
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