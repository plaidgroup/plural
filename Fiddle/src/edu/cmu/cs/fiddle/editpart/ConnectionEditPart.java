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

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.graph.CompoundDirectedGraph;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.NodeList;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;

import edu.cmu.cs.fiddle.figure.ConnectionFigure;
import edu.cmu.cs.fiddle.model.Connection;
import edu.cmu.cs.fiddle.model.IConnection;
import edu.cmu.cs.fiddle.model.IHasProperties;
import edu.cmu.cs.fiddle.model.IHasProperties.PropertyType;

/**
 * Edit part for our unidirectional connector.
 * 
 * @author Nels E. Beckman
 */
public class ConnectionEditPart extends AbstractConnectionEditPart
	implements PropertyChangeListener, IEdgeToGraphContributor {

	@Override
	protected void createEditPolicies() {

	}

	/**
	 * Does this method connect this state/dimension to its child
	 * or parent?
	 */
	public boolean isChildToParent() {
		Connection model = modelAsConnection();
		return model.getSource().isParentOf(model.getTarget()) ||
		       model.getTarget().isParentOf(model.getSource());
	}
	
	/**
	 * Does this method connect this state/dimension to itself?
	 */
	public boolean isSelfConnection() {
		return modelAsConnection().getSource().equals(modelAsConnection().getTarget());
	}
	
	@Override
	protected IFigure createFigure() {
		PolylineConnection connection = new PolylineConnection();
		connection.setTargetDecoration(new PolygonDecoration());
		
		
		MidpointLocator mpl = new MidpointLocator(connection, 0);
		mpl.setRelativePosition(PositionConstants.EAST);
		mpl.setGap(15);
		Label lbl = new Label(((IConnection) this.getModel()).getName());
		connection.add(lbl, mpl);
		return connection;
	}

	

	private Connection modelAsConnection() {
		return (Connection)this.getModel();
	}

	@Override
	public void activate() {
		super.activate();
		((IHasProperties)getModel()).addPropertyChangeListener(this);
	}

	@Override
	public void deactivate() {
		super.activate();
		((IHasProperties)getModel()).removePropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if( evt.getPropertyName().equals(PropertyType.CONNECTIONS.toString()) ) {
			refreshVisuals();
		}
	}

	@Override
	public void contributeToGraph(CompoundDirectedGraph graph,
			Map<EditPart, Object> editPartToEdge) {
		/*
		 * Adds this connection as an edge to the graph, and
		 * says that its connected to the two nodes it's
		 * actually connected to.
		 */
		Node source = (Node)editPartToEdge.get(getSource());
		Node target = (Node)editPartToEdge.get(getTarget());
		Edge e = new Edge(this, source, target);
		e.weight = 2;
		
		@SuppressWarnings({"unchecked", "unused"})
		boolean DONTUSE = graph.edges.add(e);
		
		editPartToEdge.put(this, e);
	}

	/**
	 * Using the results from the layout algorithm, actually bend the
	 * edge around anything that might be in the way.
	 */
	@Override
	public void applyGraphResults(CompoundDirectedGraph graph,
			Map<EditPart, Object> partsToNodes) {
		Edge e = (Edge)partsToNodes.get(this);
		NodeList nodes = e.vNodes;
		PolylineConnection conn = (PolylineConnection)getConnectionFigure();
		conn.setTargetDecoration(new PolygonDecoration());
		if (nodes != null) {
			List<Bendpoint> bends = new ArrayList<Bendpoint>();
			for (int i = 0; i < nodes.size(); i++) {
				Node vn = nodes.getNode(i);
				int x = vn.x;
				int y = vn.y;
				if (e.isFeedback()) {
					bends.add(new AbsoluteBendpoint(x, y + vn.height));
					bends.add(new AbsoluteBendpoint(x, y));
				} else {
					bends.add(new AbsoluteBendpoint(x, y));
					bends.add(new AbsoluteBendpoint(x, y + vn.height));
				}
			}
			conn.setRoutingConstraint(bends);
		} else {
			conn.setRoutingConstraint(Collections.emptyList());
		}
	}
}
