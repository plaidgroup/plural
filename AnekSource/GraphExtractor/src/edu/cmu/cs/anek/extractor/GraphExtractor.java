package edu.cmu.cs.anek.extractor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;


import edu.cmu.cs.anek.extractor.ExtractCommand.CommandEntry;
import edu.cmu.cs.anek.graph.Graph;
import edu.cmu.cs.anek.graph.MethodGraph;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.ITACTransferFunction;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.eclipse.CompilationUnitTACs;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.plural.alias.AliasingLE;
import edu.cmu.cs.plural.alias.LocalAliasTransfer;

/**
 * Class for creating a {@link Graph} from an
 * {@link ExtractCommand}. The code in this class will
 * have to first parse the Java elements before it can
 * create graphs from them. This class cannot be instantiated
 * by clients. They should only call public static methods. 
 * 
 * @author Nels E. Beckman
 *
 */
public class GraphExtractor {

    // Create a method graph from a parsed method.
    private static MethodGraph methodGraph(MethodDeclaration method, Map<ITypeBinding, StateHierarchy> hierarchiesMap) {
        CompilationUnitTACs cats = new CompilationUnitTACs();
        // Create an alias analysis
        ITACFlowAnalysis<AliasingLE> aliasAnalysis = new TACFlowAnalysis<AliasingLE>( 
                new LocalAliasTransfer(new AnnotationDatabase(), 
                        new HashMap<IVariableBinding,Variable>()),cats);
        // All this gnarly code is simply to instantiate the flow analysis.
        ITACTransferFunction<TupleLatticeElement<Aliasing, VariableFlowAnalysis.PredLattice>> tf = 
    	    new VariableFlowAnalysis.Transfer(cats.getMethodTAC(method),aliasAnalysis); 
    	TACFlowAnalysis<TupleLatticeElement<Aliasing, VariableFlowAnalysis.PredLattice>> fa = 
    	    new TACFlowAnalysis<TupleLatticeElement<Aliasing, VariableFlowAnalysis.PredLattice>>(tf,cats); 
        GraphExtractorVisitor method_extractor = new GraphExtractorVisitor(fa,aliasAnalysis, 
                hierarchiesMap);
        // now, visit important nodes, using flow analysis to find predecessor
        // nodes.
    	return method_extractor.visitAndExtract(method);
    }

    /**
     * Extract a graph based on the given command.
     */
    public static Graph extractGraph(ExtractCommand command) {
        // This implementation of this method is somewhat complex
        // because I want to extract each file in parallel.

        final List<MethodGraph> method_graphs = Collections.synchronizedList(new LinkedList<MethodGraph>());

        // A map that is populated lazily, containing the needed state hierarchies.
        final Map<ITypeBinding, StateHierarchy> hierarchies_map = 
            new HashMap<ITypeBinding,StateHierarchy>();

        // Just pretend that this is a parallel loop.
        final Collection<Callable<Object>> tasks = new LinkedList<Callable<Object>>();
        for( final CommandEntry entry : command ) {
            tasks.add(Executors.callable((new Runnable() { public void run() {
                try { 
                    Iterable<MethodDeclaration> parsed_methods = methodsToVisit(entry);
                    for( MethodDeclaration parsed_method : parsed_methods ) {
                        // get method graphs
                        method_graphs.add(methodGraph(parsed_method, hierarchies_map)); }}
                catch(Exception e) { e.printStackTrace(); throw new RuntimeException(e); }
            } })));
        }

        // run each command in parallel if appropriate
        ExecutorService executor = command.size() <= 1 ? 
                Executors.newSingleThreadExecutor() :
                    Executors.newCachedThreadPool();
        // join
        try {
            executor.<Object>invokeAll(tasks);
        } catch (InterruptedException e1) {
            throw new RuntimeException(e1);
        }
        Iterable<StateHierarchy> state_hierarchies = 
            hierarchies_map.values();
        return new Graph(method_graphs, state_hierarchies);
    }

    // Parse compilation unit and return the parsed methods if they should
    // be returned according to the entry.
    public static Iterable<MethodDeclaration> methodsToVisit(final CommandEntry entry) {
    	 ASTParser parser = ASTParser.newParser(AST.JLS3); 
         parser.setResolveBindings(true); 
         parser.setSource(entry.getCompilationUnit()); 
         ASTNode root_node = parser.createAST(/* passing in monitor messes up previous monitor state */ null); 
    
         final List<MethodDeclaration> result = new LinkedList<MethodDeclaration>();
         // Visit every method, add it to the resulting list if we are
         // supposed to.
         root_node.accept(new ASTVisitor(){
    		@Override public void endVisit(MethodDeclaration node) {
    			IMethod method_elem = (IMethod)node.resolveBinding().getJavaElement();
    			if( entry.extractMethod(method_elem) ) {
    				result.add(node);
    			}
    		}
         });
         return result;
    }

}
