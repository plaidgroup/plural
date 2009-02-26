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

package fiddle.parts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;

import com.evelopers.unimod.core.stateworks.Transition;
import com.evelopers.unimod.plugin.eclipse.editpart.TransitionEditPart;
import com.evelopers.unimod.plugin.eclipse.figures.IEdgeFigure;
import com.evelopers.unimod.plugin.eclipse.figures.LabelFigure;
import com.evelopers.unimod.plugin.eclipse.figures.LabeledConnectionFigure;
import com.evelopers.unimod.plugin.eclipse.model.GTransition;

public class FTransitionEditPart extends TransitionEditPart {
    protected void sendProblems() {}
    
    String hover = null;
    String label = "Yay!";
    
    protected void setHover(String h) {
    	hover = h;
    }
    
    protected void setLabel(String l) {
    	label = l;
    }
    
    private void refreshHover() {
    	if (hover == null) return;
    	IFigure fig = this.figure;
    	
    	if (fig instanceof IEdgeFigure) {
    		IFigure toolTip = ((IEdgeFigure) fig).getLabelFigure().getToolTip();
    		if (toolTip instanceof Label) {
    			((Label) toolTip).setText(hover);
    		}
    	}
    }
    
    private void refreshLabel() {
    	if (label == null) return;
    	IFigure fig = this.figure;
    	
    	if(fig instanceof LabeledConnectionFigure) {
    		LabelFigure lf = ((LabeledConnectionFigure)fig).getLabelFigure();
    		lf.setText(label);
    	}
    }
    
    @Override
    public void setModel(Object model) {
    	super.setModel(model);
    	if(model instanceof Transition) {
    		setHover(((Transition) model).getGuard().getExpr());
    		setLabel(((Transition) model).getEvent().getName());
    	}
    }
    
    @Override
    protected IFigure createFigure() {
    	IFigure fig = super.createFigure();
    	refreshHover();
    	refreshLabel();
    	return fig;
    }
    
    @Override
    protected void refreshVisuals() {
    	super.refreshVisuals();
    	refreshHover();
    	refreshLabel();
    }
    
}
