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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.CompoundDirectedGraph;
import org.eclipse.draw2d.graph.CompoundDirectedGraphLayout;
import org.eclipse.gef.EditPart;

/**
 * This layout manager will automatically arrange our graphs and edges
 * so that they are not all overlapping. It will call back into the top-level
 * edit part in order to create a node out of all of its states and
 * connections.
 * 
 * @author Nels E. Beckman
 * @see {@link CompoundDirectedGraph}
 * @see {@link CompoundDirectedGraphLayout}
 */
public class AutomaticLayoutManager extends AbstractLayout {

	private final TopLevelEditPart topEditPart;
	
	public AutomaticLayoutManager(TopLevelEditPart topEditPart) {
		super();
		this.topEditPart = topEditPart;
	}

	
	@Override
	protected Dimension calculatePreferredSize(IFigure container, int hint,
			int hint2) {
		/*
		 * This is literally the code from the flow example but
		 * I think it makes the size of this thing the union of the
		 * sizes of its children.
		 */
		container.validate();
		
		@SuppressWarnings("unchecked")
		List<IFigure> children = container.getChildren();
		
		Rectangle result =
			new Rectangle().setLocation(container.getClientArea().getLocation());
		for (int i = 0; i < children.size(); i++)
			result.union(children.get(i).getBounds());
		result.resize(container.getInsets().getWidth(), container.getInsets().getHeight());
		return result.getSize();
	}

	@Override
	public void layout(IFigure container) {
		/*
		 * This code generates a graph from the model by
		 * calling into the edit part. Then it uses the
		 * graph layout and tells the edit part to make it
		 * so.
		 */
		CompoundDirectedGraph graph = new CompoundDirectedGraph();
		Map<EditPart, Object> partsToNodes = new HashMap<EditPart, Object>();
		topEditPart.contributeNodesToGraph(graph, null, partsToNodes);
		topEditPart.contributeEdgesToGraph(graph, partsToNodes);
		new CompoundDirectedGraphLayout().visit(graph);
		topEditPart.applyGraphResults(graph, partsToNodes);
	}

	

}
