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

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A corner anchor is an anchor that will be placed at one of the
 * four corners depending which is it nearest to. 
 * 
 * @author Sapna George
 * @author Nels E. Beckman
 */
public class CornerAnchor extends ChopboxAnchor {
	boolean isStart = true;
	Point refCorner = null;
	
	public CornerAnchor(IFigure source, boolean isStart) {
		super(source);
		this.isStart = isStart;
	}

	@Override
	public Point getLocation(Point reference) {
		Point loc = super.getLocation(reference);
		Point corner = getCorner(loc);
		Rectangle r = getOwner().getBounds().getCopy();
		getOwner().translateToAbsolute(corner);
		refCorner = corner.getCopy();
		
		int yOffset = r.height / 3;
		int xOffset = r.width / 3;
		if (isStart) {	
			if (corner.y == r.y)
				corner.translate(0, yOffset);
			else corner.translate(0, -yOffset);					
		}
		else {
			if (corner.x == r.x)
				corner.translate(xOffset, 0);
			else corner.translate(-xOffset, 0);
		}
		return corner;
	}	
	
	private Point getCorner(Point loc) {
		Rectangle r = getOwner().getBounds().getCopy();
		getOwner().translateToAbsolute(r);
		float centerX = r.x + 0.5f * r.width;
		float centerY = r.y + 0.5f * r.height;
		
		int quadrant = 1;
		if (loc.x <= centerX && loc.y <= centerY) quadrant = 1;
		if (loc.x > centerX && loc.y < centerY) quadrant = 2;
		if (loc.x < centerX && loc.y > centerY) quadrant = 3;
		if (loc.x > centerX && loc.y > centerY) quadrant = 4;
		
		switch(quadrant) {
			case 1: 
				return r.getTopLeft();
			case 2: 
				return r.getTopRight();
			case 3: 
				return r.getBottomLeft();
			case 4: 
				return r.getBottomRight();
		}
		return r.getTopLeft();
	}
	
	public Point getRefCorner() {
		return refCorner;
	}
	
}