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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.CompoundDirectedGraph;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.Subgraph;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import edu.cmu.cs.fiddle.model.FinalState;
import edu.cmu.cs.fiddle.model.IConnection;
import edu.cmu.cs.fiddle.model.IHasProperties.PropertyType;

/**
 * The edit part for a final state. Frankly, this will be extremely
 * similar to InitialStateEditPart, and the only difference will probably
 * be the figure that this edit part creates.
 * 
 * @author Nels E. Beckman
 *
 */
public class FinalStateEditPart extends AbstractGraphicalEditPart implements
		NodeEditPart, PropertyChangeListener, INodeToGraphContributor {

	private static final Dimension FINAL_STATE_SIZE = new Dimension(28, 28);
	
	private static final class FinalStateFigure extends Shape {
		@Override protected void fillShape(Graphics graphics) {
			// This code mostly comes from Ellipse
			Rectangle inner_r = new Rectangle(getBounds());
			inner_r.x += 4;
			inner_r.y += 4;
			inner_r.width -= 8;
			inner_r.height -= 8;
			inner_r.shrink((lineWidth - 1) / 2, (lineWidth - 1) / 2);
			graphics.fillOval(inner_r);
		}

		@Override protected void outlineShape(Graphics graphics) {
			// This code mostly comes from Ellipse
			Rectangle outer_r = new Rectangle(getBounds());
			outer_r.width--;
			outer_r.height--;
			outer_r.shrink((lineWidth - 1) / 2, (lineWidth - 1) / 2);
			graphics.drawOval(outer_r);
		}
	}

	@Override
	protected IFigure createFigure() {
		IFigure f = new FinalStateFigure();
		f.setBackgroundColor(ColorConstants.black);
		f.setForegroundColor(ColorConstants.black);
		return f;
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

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if( evt.equals(PropertyType.CONNECTIONS.toString()) ) {
			this.refreshSourceConnections();
			this.refreshTargetConnections();
		}
	}

	@Override
	protected List<?> getModelSourceConnections() {
		return new ArrayList<IConnection>(modelAsFinalState().getOutgoingConnections());
	}

	@Override
	protected List<?> getModelTargetConnections() {
		return new ArrayList<IConnection>(modelAsFinalState().getIncomingConnections());
	}

	@Override
	protected List<?> getModelChildren() {
		return Collections.emptyList();
	}

	private FinalState modelAsFinalState() {
		return (FinalState)getModel();
	}
	
	@Override
	public void activate() {
		super.activate();
		modelAsFinalState().addPropertyChangeListener(this);
	}

	@Override
	public void deactivate() {
		super.deactivate();
		modelAsFinalState().removePropertyChangeListener(this);
	}

	@Override
	public void applyGraphResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		// Straight from StructuredActivityPart
		applyOwnResults(graph, partsToNodes);
	}

	private void applyOwnResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		// Set your bound again...
		Node n = (Node)partsToNodes.get(this);
		getFigure().setBounds(new Rectangle(n.x, n.y, n.width, n.height));
		// Then apply results to your connections.
		for( Object connection_ : getSourceConnections() ) {
			IEdgeToGraphContributor connection = (IEdgeToGraphContributor)connection_;
			connection.applyGraphResults(graph, partsToNodes);
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
		// Has no children...
	}

	@Override
	public void contributeNodesToGraph(CompoundDirectedGraph graph, Subgraph s,
			Map<EditPart, Object> partsToNodesOrEdges) {
		Node me = new Node(this, s);
		me.outgoingOffset = 5;
		me.incomingOffset = 5;
			
		me.height = FINAL_STATE_SIZE.height;
		me.width = FINAL_STATE_SIZE.width;
		
		me.setPadding(PADDING);
		partsToNodesOrEdges.put(this, me);

		@SuppressWarnings({"unchecked", "unused"})
		boolean DONTUSE = graph.nodes.add(me);
		
		// Has no children...
	}
}
