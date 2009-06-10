/**
 * Copyright (C) 2007-2009 Carnegie Mellon University and others.
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

package edu.cmu.cs.plural.errors;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.cs.plural.contexts.LinearContext;
import edu.cmu.cs.plural.errors.history.DisplayLinearContext;
import edu.cmu.cs.plural.errors.history.HistoryView;
import edu.cmu.cs.plural.errors.history.ResultingDisplayTree;

/**
 * A view that allows programmers to see the full description of some
 * context.
 * 
 * @author Nels E. Beckman
 * @since Jun 9, 2009
 *
 */
public class ContextView extends ViewPart implements ISelectionListener {

	private Browser browser;
	
	@Override
	public void createPartControl(Composite parent) {
		this.browser = new Browser(parent, SWT.NONE);
		this.browser.setText("<b>Double-click a context in the Plural History view to see its contents.</b>");
		
		// Register as listener
		getViewSite().getPage().addPostSelectionListener(this);
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	private static String scrub(String str) {
		return str;
		//return str.replace("<", "&lt;").replace(">", "&gt").replace("&", "&amp;");
	}
	
	private static String createDisplayString(DisplayLinearContext dctx) {
		LinearContext ctx = dctx.getContext();
		String result = "<b>Context:</b> " + System.identityHashCode(ctx) + "<br>" +
		    "<b>Location:</b> " + scrub(dctx.getLocation().toString()) + "<br>" +
			"<b>Type:</b> " + scrub(ctx.getClass().toString()) + "<br>" +
			"<b>Choice ID: </b>" + ctx.getChoiceID() + "<br>" +
			"<b>Permissions: </b>" + scrub(ctx.getHumanReadablePerms()) +"<br>" + 
			"<b>Concrete State Information: </b> ...<br>";
		
		return result;
	}
	
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection_) {
		if( part instanceof HistoryView ) {
			if( selection_ instanceof ITreeSelection ) {
				ITreeSelection selection = (ITreeSelection)selection_;
				
				Object selected_object = selection.getFirstElement();
				
				if( selected_object instanceof ResultingDisplayTree ) {
					ResultingDisplayTree display_tree = (ResultingDisplayTree)selected_object;
					Object tree_contents = display_tree.getContents();
					
					if( tree_contents instanceof DisplayLinearContext ) {
						DisplayLinearContext dctx = (DisplayLinearContext)tree_contents;
						this.browser.setText(createDisplayString(dctx));
					}
				}
			}
		}
		
	}


}
