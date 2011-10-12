package edu.cmu.cs.anek.eclipse;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.xml.sax.SAXException;


import edu.cmu.cs.anek.extractor.ExtractCommand;
import edu.cmu.cs.anek.extractor.GraphExtractor;
import edu.cmu.cs.anek.graph.Graph;
import edu.cmu.cs.anek.output.GraphMLDoc;
import edu.cmu.cs.anek.output.GraphToXML;
import edu.cmu.cs.anek.util.Utilities;

/**
 * The Eclipse action that is run when a user selects "Extract Graph."
 * @author Nels E. Beckman
 *
 */
public class ExtractGraphAction implements IObjectActionDelegate {

	private Shell shell;
	
	private ISelection lastSelection;
	
	/**
	 * Constructor for Action1.
	 */
	public ExtractGraphAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		assert(this.lastSelection != null);
		if( this.lastSelection instanceof IStructuredSelection ) {
			List<IJavaElement> elements = EclipseUtils.structuredSelection(this.lastSelection);
			try {

				// 1 - ask user where to save file (if they cancel, we do no work)
				IPath filename = Path.fromOSString(launchSaveDialog());
				if( filename == null ) return;

                extractGraphWriteToFile(elements, filename);

			} catch(JavaModelException jme) {
				MessageDialog.openInformation(shell,
						"Graph Extractor",
						"Something went wrong with your selection...");
			} catch (IOException e) {
			    MessageDialog.openInformation(shell,
                        "Graph Extractor",
                        "File does not exist or cannot be written to.");
            } catch (SAXException e) {
                MessageDialog.openInformation(shell,
                        "Graph Extractor",
                        "Generated XML was not valid.");
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (CoreException e) {
                MessageDialog.openInformation(shell,
                        "Graph Extractor",
                        "File does not exist or cannot be written to.");
            } 
		}
	}

	
    public static void extractGraphWriteToFile(List<IJavaElement> elements,
            IPath path) throws IOException,
            SAXException, FileNotFoundException, CoreException {
        // 2 - Get a command from their selection
        ExtractCommand command = ExtractCommand.commandFomSelection(elements);
                        
        // 3 - Generate the graph
        Graph graph = GraphExtractor.extractGraph(command);
        
        // 4 - convert graph to output
        GraphMLDoc xml = GraphToXML.graphToXML(graph);
        
        // 5 - write to file
        IFileSystem fs = EFS.getLocalFileSystem();
        IFileStore file = fs.getStore(path);
        
        OutputStream out = file.openOutputStream(EFS.OVERWRITE, null);
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)));
        xml.output(pw);
        pw.close();
        
        // 6 - Validate xml
        if( Activator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.VALIDATE_PREF) ) {
            Utilities.validate(path.toOSString());
        }
    }

    private String launchSaveDialog() {
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setFilterExtensions(new String[]{"*.graphml"});
		String filename = dialog.open();
		return filename;
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection_) {
		this.lastSelection = selection_;
	}
}
