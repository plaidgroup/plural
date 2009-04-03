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

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;

/**
 * This figure represents a state. It's pretty simple, although it
 * has a header where the name of the state goes and a contents pane
 * that will hold the dimensions of the state.
 * 
 * @author Nels E. Beckman
 */
public class StateFigure extends Shape {

	public static final int MIN_WIDTH = 80;
	public static final int HEADER_SIZE = 15;
	public static final int FOOTER_SIZE = 20;
	
	private Label nameLabel;
	
	private IFigure contentsFigure;
	
	/**
	 * @param name
	 */
	public StateFigure(String name, org.eclipse.swt.graphics.Font labelFont) {
		this.setLayoutManager(new BorderLayout());
		this.setOpaque(true);		
		
		this.nameLabel = new Label();
		this.nameLabel.setText(name);
		this.nameLabel.setFont(labelFont);
		
		this.add(nameLabel, BorderLayout.TOP);
		
		this.contentsFigure = new Figure();
		
		this.add(contentsFigure, BorderLayout.CENTER);
	}

	public IFigure getContents() {
		return this.contentsFigure;
	}
	
	@Override protected void fillShape(Graphics graphics) {
		// Make the header blue
		// We must do this here because we have to make it
		// a rounded rectangle, which is not so easy.
		Color color = new Color(null, 195, 199, 255);
		
		Rectangle label_rect = this.nameLabel.getBounds();
		graphics.setBackgroundColor(color);
		graphics.fillRoundRectangle(label_rect, 15, 15);
		
		// Now b/c the first rectangle was rounded, but we
		// want the bottom of the filled-in area to be flat,
		// we draw ANOTHER rectangle, this one half the size
		// but complete at the bottom.
		Dimension new_dim = new Dimension(label_rect.width, label_rect.height / 2 + 1);
		Point new_loc = label_rect.getLeft();
		Rectangle new_rect = new Rectangle(new_loc, new_dim);
		graphics.fillRectangle(new_rect);		
	}

	@Override
	protected void outlineShape(Graphics graphics) {
		Rectangle r = getBounds();
		int x = r.x + lineWidth / 2;
		int y = r.y + lineWidth / 2;
		int w = r.width - Math.max(1, lineWidth);
		int h = r.height - Math.max(1, lineWidth);
		
		Rectangle new_r = new Rectangle(x, y, w, h);
		graphics.drawRoundRectangle(new_r, 15, 15);
		
		// Also draw line under the label
		Point line_left = new Point(r.x, r.y + HEADER_SIZE);
		Point line_right = new Point(r.x + r.width, r.y + HEADER_SIZE);
		graphics.drawLine(line_left, line_right);
		
		// Also draw a line at the footer
		Point footer_line_left = new Point(r.x, r.y + r.height - FOOTER_SIZE);
		Point footer_line_right = new Point(r.x + r.width, r.y + r.height -  FOOTER_SIZE);
		graphics.drawLine(footer_line_left, footer_line_right);
	}
}