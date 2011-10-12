package edu.cmu.cs.anek.eclipse;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.xml.sax.SAXException;

import edu.cmu.cs.anek.applier.eclipse.ApplyGraphAction;


/**
 * An action for running inference from start to finish. This means
 * generating a graph, writing it and the preferences to a file and
 * then applying the graph.
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 *
 */
public final class CompleteInferenceAction implements IObjectActionDelegate {

    private Shell shell;

    // The last selected element...
    private ISelection lastSelection;

    private Date date;


    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
        date = new Date();
    }

    @Override
    public void run(IAction action) {
        if( lastSelection instanceof IStructuredSelection ) {
            finishInferenceInNewThread();
        }
    }

    private void finishInferenceInNewThread() {
        final List<IJavaElement> elems = EclipseUtils.structuredSelection(lastSelection);
        // run the entire process inside a new thread
        (new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    // generate random file names, and params file
                    IPath pref_file = prefsFilesFromPreferences();
                    IPath input_file = randomXmlFileName("in");
                    IPath output_file = randomXmlFileName("out");

                    // generate graph
                    ExtractGraphAction.extractGraphWriteToFile(elems, input_file);

                    // execute algorithm
                    String program_exe = 
                        Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.ENG_PATH);
                    Boolean is_verbose =
                        Boolean.parseBoolean(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.VERBOSE_PREF));
                        
                    String[] command = new String[] {
                            program_exe, // program 
                            input_file.toOSString(), // args
                            output_file.toOSString(), 
                            pref_file.toOSString(),
                            is_verbose ? "-v" : ""
                    };
                    Process proc = Runtime.getRuntime().exec(command);
                    final InputStream proc_input = new BufferedInputStream(proc.getInputStream());
                    final InputStream proc_error = new BufferedInputStream(proc.getErrorStream());
                    final OutputStream console = AnekConsole.outputStream();

                    int c;
                    // write every character from the process to the console
                    while( ((c = proc_input.read()) > 0) || ((c = proc_error.read()) > 0) ) {
                        console.write(c);
                    }
                    // process finished... did it finish correctly?
                    if( proc.exitValue() != 0 )
                        return;
                    
                    // apply graph
                    ApplyGraphAction.applyGraphFromFileToElements(elems, output_file);

                    // delete temporary files
                    IFileSystem fs = EFS.getLocalFileSystem();
                    fs.getStore(pref_file).delete(EFS.NONE, null);
                    fs.getStore(input_file).delete(EFS.NONE,null);
                    fs.getStore(output_file).delete(EFS.NONE, null);
                } catch (IOException e) {
                    // Failed to write to or read any of the files...
                    //couldntWriteHelper();
                    e.printStackTrace();
                } catch (CoreException e) {
                    // Couldn't delete file...
                    // Could be file permissions...
                    //couldntWriteHelper();
                    e.printStackTrace();
                } catch (SAXException e) {
                    // bug... generating invalid XML
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    // bug... I think
                    e.printStackTrace();
                }
            }})).start();
    }

        private IPath randomXmlFileName(String string) {
            String file_name = "anek_graph" + string + date.getTime() + ".graphml";
            IPath path = Activator.getDefault().getStateLocation();
            return path.append(file_name);
        }

        private static String newline = System.getProperty("line.separator");

        /**
         * From the Anek preferences, generates a random file name, puts
         * the contents there, 
         * @return
         * @throws IOException 
         * @throws CoreException 
         */
        private IPath prefsFilesFromPreferences() throws IOException {
            IPreferenceStore pref_store = Activator.getDefault().getPreferenceStore();
            String file_name = "anek_parameters_" + date.getTime() + ".txt";

            IPath path = Activator.getDefault().getStateLocation();
            path = path.append(file_name);

            IFileSystem fs = EFS.getLocalFileSystem();
            IFileStore file = fs.getStore(path);

            try {
                OutputStream out = file.openOutputStream(EFS.OVERWRITE, null);
                Writer writer = new BufferedWriter(new OutputStreamWriter(out));

                // write every parameter
                for( String pref : PreferenceConstants.ANEK_PREFS ) {
                    String p_val = pref_store.getString(pref);
                    writer.write(pref);
                    writer.write('=');
                    writer.write(p_val);
                    writer.write(newline);
                }

                // close the file
                writer.close();
            } catch (CoreException e) {
                // if any of these things happen, we have a bug...
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            return path;
        }

        @Override
        public void selectionChanged(IAction action, ISelection selection) {
            this.lastSelection = selection;
        }

    }
