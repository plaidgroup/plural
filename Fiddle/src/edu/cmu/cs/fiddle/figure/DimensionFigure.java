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
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

/**
 * The figure for a dimension, it basically has a header with the
 * dimension name and then a contents figure. The whole thing has a 
 * dashed line on the bottom.
 * 
 * @author Nels E. Beckman
 */
public class DimensionFigure extends Shape {

	private Label name;
	private IFigure contents;
	
	public static final int MIN_WIDTH = 80;
	public static final int HEADER_SIZE = 15;
	
	private boolean isFirstDim;
	
	public DimensionFigure(String name, boolean isLastDim) {
		super();
	
		this.setLayoutManager(new BorderLayout());
		this.setOpaque(true);
	
		this.isFirstDim = isLastDim;
		this.name = new Label(" " + name);
		this.name.setLabelAlignment(Label.LEFT);
		this.add(this.name, BorderLayout.TOP);
		
		this.contents = new Figure();
		
		this.add(contents, BorderLayout.CENTER);
	}

	public IFigure getContents() {
		return contents;
	}
	
	@Override
	public Dimension getPreferredSize(int wHint, int hHint) {		
		int size_id_like = this.getParent().getBounds().height / this.getParent().getChildren().size();
		return new Dimension(wHint, size_id_like);
	}

	@Override
	protected void fillShape(Graphics graphics) {
		graphics.setLineStyle(SWT.LINE_DASH);
		graphics.setForegroundColor(ColorConstants.black);
		graphics.setBackgroundColor(ColorConstants.black);
		graphics.drawLine(this.getBounds().getBottomLeft(), 
				this.getBounds().getBottomRight());
		
		// Draw color under the label
		graphics.setBackgroundColor(new Color(null, 255, 223, 208));
		graphics.fillRectangle(getLabelRectangle());
	}

	private Rectangle getLabelRectangle() {
		Rectangle r = this.getBounds();
		int text_right_x = this.name.getTextBounds().getBottomRight().x + 3;
		Point bot_right_label = new Point(text_right_x, r.y + HEADER_SIZE );
		
		Rectangle result = new Rectangle(r.getTopLeft().getTranslated(1, 1),
				                         bot_right_label);
		return result;
	}
	
	@Override
	protected void outlineShape(Graphics graphics) {
		Rectangle label_rect = getLabelRectangle();
		graphics.drawLine(label_rect.getBottomLeft(), label_rect.getBottomRight());
		graphics.drawLine(label_rect.getBottomRight(), label_rect.getTopRight());
		
		// Draw a dotted line to our right as long as this
		// is not the last dimension.
		if( !isFirstDim ) {
			IFigure state_fig = this.getParent().getParent();
			
			assert(state_fig instanceof StateFigure);
			
			Point parent_bot = state_fig.getBounds().getBottom();
			Point parent_top = state_fig.getBounds().getTop();
			
			// We are using the StateFigure size minus the headers because
			// we had trouble getting the line to extend all the way from header
			// to footer, when we just used the size of this figure or the contents
			// pane.
			int my_x = this.getBounds().getLeft().x;
			Point line_top_left = new Point(my_x,
					                        parent_top.y);
			Point line_bot_left = new Point(my_x,
					                        parent_bot.y);

			graphics.setLineStyle(SWT.LINE_DASH);
			graphics.setLineWidth(4);
			graphics.setForegroundColor(ColorConstants.black);
			graphics.setBackgroundColor(ColorConstants.black);
			graphics.drawLine(line_top_left, line_bot_left);
		}
	}
}