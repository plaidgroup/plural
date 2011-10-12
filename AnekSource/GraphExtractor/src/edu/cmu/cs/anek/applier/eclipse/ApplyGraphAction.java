package edu.cmu.cs.anek.applier.eclipse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

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


import edu.cmu.cs.anek.applier.GraphApplier;
import edu.cmu.cs.anek.eclipse.Activator;
import edu.cmu.cs.anek.eclipse.EclipseUtils;
import edu.cmu.cs.anek.eclipse.PreferenceConstants;
import edu.cmu.cs.anek.extractor.ExtractCommand;
import edu.cmu.cs.anek.graph.Graph;
import edu.cmu.cs.anek.input.XMLToGraph;
import edu.cmu.cs.anek.util.Utilities;

/**
 * This action is used to apply the results of specification inference
 * to the selected Java elements. It needs to load the chosen graph from
 * the user, check that it's well-formed, and then call the appropriate
 * application code.
 * <br>
 * अनेक<br>
 * Anek<br>
 * 
 * @author Nels E. Beckman
 *
 */
public class ApplyGraphAction implements IObjectActionDelegate {

    private Shell shell;
    
    // The last selected element...
    private ISelection lastSelection;
    
    /**
     * Constructor for Action1.
     */
    public ApplyGraphAction() {
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
        assert( this.lastSelection != null );
        if( this.lastSelection instanceof IStructuredSelection ) {
            List<IJavaElement> elems =
                EclipseUtils.structuredSelection(this.lastSelection);
            try {                
                // 1 - ask user where to load the file from (cancel returns)
                IPath filename = Path.fromOSString(launchOpenDialog());
                if( filename == null ) return;
                
                applyGraphFromFileToElements(elems, filename);

            } catch(JavaModelException jme) {
                MessageDialog.openInformation(shell,
                        "अनेक (Anek)",
                        "Something went wrong with your selection...");
                jme.printStackTrace();
                throw new RuntimeException(jme);
            } catch (IOException e) {
                MessageDialog.openInformation(shell,
                        "अनेक (Anek)",
                        "File does not exist or cannot be read from.");
            } catch (SAXException e) {
                MessageDialog.openInformation(shell,
                        "अनेक (Anek)",
                        "Given XML file was not valid.");
                e.printStackTrace();
                //throw new RuntimeException(e);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Given a file name/path and a list of selected java elements, this
     * method will open the graph, validate it, and apply it to the
     * elements.
     * @param elems
     * @param outputFile
     * @throws SAXException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws ParserConfigurationException
     * @throws JavaModelException
     * @throws CoreException
     * @throws ParserConfigurationException 
     */
    public static void applyGraphFromFileToElements(List<IJavaElement> elems,
            IPath path) throws SAXException, IOException,
            FileNotFoundException,
            JavaModelException, CoreException, ParserConfigurationException {

        // 2 - Validate xml file (if user has it selected)
        if( Activator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.VALIDATE_PREF) ) {
            Utilities.validate(path.toOSString());
        }
        
        // 3 - Generate graph from XML
        IFileSystem fs = EFS.getLocalFileSystem();
        IFileStore file = fs.getStore(path);
        Graph g = XMLToGraph.loadGraph(file.openInputStream(EFS.NONE, null));
        
        // 4 - get command from selection, which shows
        //     which methods we might apply graph nodes to
        ExtractCommand command = ExtractCommand.commandFomSelection(elems);
        
        // 5 - Apply structure to graph
        String commit_wc_ =
            Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.COMMIT_WC_PREF);
        boolean commit_wc = "commit".equals(commit_wc_);
        GraphApplier.applyGraph(g, command, commit_wc);
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        this.lastSelection = selection;
    }
    
    /**
     * Opens a file open dialog 
     */
    private String launchOpenDialog() {
        FileDialog dialog = new FileDialog(shell, SWT.OPEN);
        dialog.setFilterExtensions(new String[]{"*.graphml"});
        String filename = dialog.open();
        return filename;
    }
}
