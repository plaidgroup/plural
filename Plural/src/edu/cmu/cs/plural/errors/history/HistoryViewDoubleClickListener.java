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

package edu.cmu.cs.plural.errors.history;


import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * A listener for double-clicks on the history view. When a user
 * double-clicks on a context, for example, we want to take them
 * to the location where that context was obtained.
 * 
 * @author Nels E. Beckman
 * @since Jun 8, 2009
 *
 */
public class HistoryViewDoubleClickListener implements IDoubleClickListener {

	@Override
	public void doubleClick(DoubleClickEvent event) {
		ISelection selection = event.getSelection();
		
		if( selection instanceof TreeSelection ) {
			TreeSelection tree_selection = (TreeSelection)selection;
			Object actual_selection = tree_selection.getFirstElement();
			
			if( actual_selection instanceof IMethod ) {
				IMethod method = (IMethod)actual_selection;
				try {
					JavaUI.openInEditor(method, true, true);
				} catch (PartInitException e) {} 
				  catch (JavaModelException e) {}
			}
			else if( actual_selection instanceof ResultingDisplayTree && 
					  ((ResultingDisplayTree)actual_selection).getContents() instanceof DisplayLinearContext ) {
				// Just switch to the Context Viewer view so that we can see what
				// the context looks like.
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView( "Plural.contextViewer" );
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		}
	}
}