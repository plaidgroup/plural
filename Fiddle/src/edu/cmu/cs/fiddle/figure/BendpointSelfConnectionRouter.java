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
package edu.cmu.cs.fiddle.figure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.AutomaticRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.FanRouter;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A router that should only be used when CornerAnchors are
 * being used as the connection's anchor. It was designed to
 * better display "self-connections."
 * 
 * Currently, this routing looks horrible. Can we do something
 * about this?
 * 
 * @author Sapna George
 * @author Nels E. Beckman
 */
public class BendpointSelfConnectionRouter extends AutomaticRouter {
	
	private final FanRouter fanRouter = new FanRouter();
	
	private int separation = 30;
	private Map<HashKey,List<Connection>> connections = new HashMap<HashKey,List<Connection>>();
	
	private class HashKey {
		private Point corner;
		
		HashKey(Connection conn) {
			ConnectionAnchor anchor = conn.getSourceAnchor();
			//Point ref = anchor.getReferencePoint();
			//Point loc = anchor.getLocation(ref);
			if (anchor instanceof CornerAnchor) {
				corner = ((CornerAnchor)anchor).getRefCorner();
				if( corner == null ) 
					throw new NullPointerException();
			}
			else
				throw new RuntimeException("MATCH");
		}
		
		public boolean equals(Object object) {
			boolean isEqual = false;
			HashKey hashKey;
			
			if (object instanceof HashKey) {
				hashKey = (HashKey)object;
				Point p = hashKey.getCorner();
				
				isEqual = p.equals(corner);
			}
			return isEqual;
		}

		public Point getCorner() {
			return corner;
		}
		
		public int hashCode() {
			return corner.hashCode();
		}
	}
	
	public BendpointSelfConnectionRouter() {
		super();
	}
	
	/**
	 * Returns the separation in pixels between routed connections.
	 * @return  the separation
	 */
	public int getSeparation() {
		return this.separation;  
	}//getSeparation()
	  
	protected void handleCollision(Connection conn, int index) {
		PointList points = conn.getPoints();
		Point start = points.getFirstPoint();
		Point end = points.getLastPoint();
		
		if (start.equals(end))
			return;
		
		CornerAnchor ca = (CornerAnchor)conn.getSourceAnchor();
		Point corner = ca.getRefCorner();
		Rectangle r = ca.getOwner().getBounds();
		int dw = index * separation;
		int dh = index * separation;
		Point bendPoint = null;
		
		if (corner.equals(r.getTopLeft())) {
			int bx = corner.x - dw;
			int by = corner.y - dh;
			bendPoint = new Point(bx, by);
		}
		else if (corner.equals(r.getTopRight())) {
			int bx = corner.x + dw;
			int by = corner.y - dh;
			bendPoint = new Point(bx, by);
		}
		else if (corner.equals(r.getBottomLeft())) {
			int bx = corner.x - dw;
			int by = corner.y + dh;
			bendPoint = new Point(bx, by);
		}
		else if (corner.equals(r.getBottomRight())) {
			int bx = corner.x + dw;
			int by = corner.y + dh;
			bendPoint = new Point(bx, by);
		}
		if (!bendPoint.equals(corner))
			points.insertPoint(bendPoint, 1);
		
	    conn.setPoints( points );
	}//handleCollision()
	
	private void put(HashKey key, Connection value) {
		List<Connection> existingValues = connections.get(key);
		if (existingValues == null) {
			List<Connection> val = new ArrayList<Connection>();
			val.add(value);
			connections.put(key, val);
			return;
		}
		if (!existingValues.contains(value))
			existingValues.add(value);
		return;
	}
	
	@Override
	public void route(Connection conn) {
		if( !(conn.getSourceAnchor() instanceof CornerAnchor) ) {
			// This is NOT a self connection, delegate to the
			// FanRouter.
			this.fanRouter.route(conn);
			return;
		}
		
		super.route(conn);
		if (conn.getPoints().size() == 2) {
			//PointList points = conn.getPoints();
			HashKey connectionKey = new HashKey(conn);
			List<Connection> connectionList = 
				connections.get(connectionKey);
			if (connectionList == null) {
				put(connectionKey, conn);
			}
			connectionList = connections.get(connectionKey);
			if (connectionList != null) {
				
				int index;
				
				if (connectionList.contains(conn)) {
					index = connectionList.indexOf(conn) + 1;	
				} 
				else {
					index = connectionList.size() + 1;
					put(connectionKey, conn);
				}
				handleCollision(conn, index);
			} 
		}
		else {
			System.err.println("Here");
		}
	}

	@Override
	protected void handleCollision(PointList list, int index) {
		// Do nothing!
	}
}