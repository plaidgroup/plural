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

import org.eclipse.draw2d.Graphics;

import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;

/**
 * @author psrichar
 *
 */
public class ConnectionFigure extends PolylineConnection {
	public static double VIEW_ANGLE = Math.PI * 0.5;
	
    public double[] getCenterAndRadius() {    	
    	// get first and last points
		int[] points = getPoints().toIntArray();    	
    	double x1 = points[0];
    	double y1 = points[1];
    	double x2 = points[points.length - 2];
    	double y2 = points[points.length - 1];
    	
		double cx = ((x1 + x2) + Math.tan(VIEW_ANGLE / 2) * (y2 - y1)) / 2;
		double cy = ((y1 + y2) - Math.tan(VIEW_ANGLE / 2) * (x2 - x1)) / 2;
		double r = Math.sqrt((cx - x1)*(cx - x1) + (cy - y1)*(cy - y1));
		double startAngle = Math.PI - Math.atan2(cy - y1, cx - x1);				
		double widthAngle = 2.0*Math.PI - VIEW_ANGLE;

    	double[] d = new double[5];
    	d[0] = cx;
    	d[1] = cy;
    	d[2] = r;
    	d[3] = startAngle;
    	d[4] = widthAngle;
    	return d;
    }
    
	@Override
	protected void outlineShape(Graphics g) {
		PointList pointList = getPoints();
		if (pointList.size() <= 2) {
			super.outlineShape(g);
			return;
		}
    	double[] d = getCenterAndRadius();
    	double cx = d[0];
    	double cy = d[1];
    	double r = d[2];
    	double startAngle = d[3]; 
    	double widthAngle = d[4];

    	g.drawArc(
				(int)(cx - r), (int)(cy - r), 
				(int)(2.0*r) + 1, (int)(2.0*r) + 1, 
				(int)Math.toDegrees(startAngle), - (int)Math.toDegrees(widthAngle)
		);
	}
	
}
