package edu.cmu.cs.anek.applier;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;


import edu.cmu.cs.anek.extractor.ExtractCommand;
import edu.cmu.cs.anek.extractor.GraphExtractor;
import edu.cmu.cs.anek.graph.Graph;
import edu.cmu.cs.anek.graph.MethodGraph;
import edu.cmu.cs.anek.util.Utilities;

/**
 * This class contains code for applying a graph (which contains
 * inferred permissions) to some selected Java code. Clients will
 * generally interact with this module through its static methods. 
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 * 
 */
public final class GraphApplier {
    
    
    // Not to be instantiated...
    private GraphApplier() {}

    /**
     * Applies the permissions of the given graph to the JavaElements
     * selected in command.
     * @throws CoreException 
     */
    public static void applyGraph(Graph g, ExtractCommand command, 
            boolean commitWorkingCopies) throws CoreException {
        // first, we need to make a map of IDs to method graphs so
        // that we know which nodes to use for each method we visit
        Map<String,MethodGraph> methods_from_ids = createIDMethodMap(g);
        goThroughCommand(command,methods_from_ids,commitWorkingCopies);
    }

    private static Map<String, MethodGraph> createIDMethodMap(Graph g) {
        Map<String,MethodGraph> result = new HashMap<String,MethodGraph>();
        for( MethodGraph m : g.getMethods() ) {
            result.put(m.id(), m);
        }
        return result;
    }
    
    private static void goThroughCommand(ExtractCommand command, Map<String, MethodGraph> methodsFromIds, boolean commitWorkingCopies) throws CoreException {
        for( ExtractCommand.CommandEntry entry : command ) {
            // get the list of methods to visit
            Iterable<MethodDeclaration> methods =
                GraphExtractor.methodsToVisit(entry);
            
            ICompilationUnit cu = entry.getCompilationUnit();
            Collection<AnnotationDiff> diffs = new LinkedList<AnnotationDiff>();
            
            // make compilation unit the working copy, so changes will
            // happen even if file is closed.
            try {
                cu.becomeWorkingCopy(null);
                
                for( MethodDeclaration method : methods ) {
                    String id = method.resolveBinding().getKey();
                    id = Utilities.legalNMToken(id);

                    // Maybe we did no inference for this method...
                    if( methodsFromIds.containsKey(id) ) {
                        MethodGraph m_graph = methodsFromIds.get(id);
                        // collect diffs
                        diffs.addAll(MethodApplier.apply(m_graph,method));
                    }
                }
                
                // apply diffs
                Set<String> types_added = new HashSet<String>();
                MultiTextEdit mte = new MultiTextEdit();
                for( AnnotationDiff diff : diffs ) {
                    types_added.addAll(diff.typesAdded());
                    diff.addDiffToEdit(mte);
                }
                // Do them all at once so offsets are correct.
                cu.applyTextEdit(mte, null);
                
                if( cu.hasUnsavedChanges() ) {
                    // Reorganize imports
                    ImportRewrite ir = ImportRewrite.create(cu, true);
                    for( String added_type : types_added )  
                        ir.addImport(added_type);
                    TextEdit import_edit = ir.rewriteImports(null);
                    cu.applyTextEdit(import_edit, null);
                    
                    // Reformat code...
                    CodeFormatter formatter = ToolFactory.createCodeFormatter(null);
                    ISourceRange range = cu.getSourceRange();
                    TextEdit indent_edit =
                        formatter.format(CodeFormatter.K_COMPILATION_UNIT, 
                            cu.getSource(),
                            0, cu.getSource().length(),
                            //range.getOffset(), range.getLength(), 
                            0, null);
                    // When text cannot be formatted, what is the problem? Syntax errors?
                    if( indent_edit != null )
                        cu.applyTextEdit(indent_edit, null);
                    
                    // reconcile cu
                    cu.reconcile(ICompilationUnit.NO_AST, 
                            false, null, null);
                    
                    // This can cause changes that cannot be undone, so we
                    // only want to do it if we're supposed to.
                    if( commitWorkingCopies )
                        cu.commitWorkingCopy(false, null);
                }
            } finally {
                cu.discardWorkingCopy();
            }
        }
    }
}
