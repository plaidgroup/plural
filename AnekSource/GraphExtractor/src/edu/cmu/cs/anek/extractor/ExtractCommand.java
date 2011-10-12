package edu.cmu.cs.anek.extractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;

import edu.cmu.cs.crystal.util.Lambda;

/**
 * An instruction to extract the graph for a group of
 * java elements, along with which methods from those
 * java elements should actually be extracted.
 * 
 * 'What methods to extract' is going to be something
 * along the lines of 'everything' or specific method
 * names.
 * 
 * @author Nels E. Beckman
 *
 */
public final class ExtractCommand implements Iterable<ExtractCommand.CommandEntry> {
	
	private Collection<CommandEntry> entries;
	
	private ExtractCommand(Collection<CommandEntry> entries) {
		this.entries = entries;
	}
	
	/**
	 * Given a list of selected Java elements, returns an extract
	 * command that corresponds to the selection.
	 * @throws JavaModelException 
	 */
	public static ExtractCommand commandFomSelection(Iterable<IJavaElement> selection) throws JavaModelException {
		Set<CommandEntry> entries = new HashSet<CommandEntry>();
		for( IJavaElement elem : selection ) {
			extractHelper(elem, entries);
		}
		return new ExtractCommand(entries);
	}
	
	// Recursively walk the tree, when you see compilation units
	// add them, when you see methods, find their compilation unit
	// add them...
	private static void extractHelper(IJavaElement node, Set<CommandEntry> result) throws JavaModelException {
		
		if( node instanceof ICompilationUnit ) {
			result.add(new CommandEntry((ICompilationUnit)node,
					CommandEntry.INCLUDE_ALL_METHODS));
		}
		else if( node instanceof IMethod ) {
			IMethod method = (IMethod)node;
			result.add(new CommandEntry(method.getCompilationUnit(),
					CommandEntry.thisMethodOnly(method)));
		}
		else if( node instanceof IJavaProject ||
				  node instanceof IPackageFragment ||
				  node instanceof IPackageDeclaration ||
				  node instanceof IPackageFragmentRoot /* a folder */) {
			IParent parent = (IParent)node;
			for( IJavaElement child : parent.getChildren() )
				extractHelper(child, result);
		}
	}

	@Override
	public Iterator<CommandEntry> iterator() {
		return entries.iterator();
	}

    public int size() {
        return entries.size();
    }
    
    public static class CommandEntry {
        private ICompilationUnit compUnit;
        private Lambda<IMethod,Boolean> extractMethod;
        
        public CommandEntry(ICompilationUnit compUnit, Lambda<IMethod, Boolean> extractMethod) {
            this.compUnit = compUnit;
            this.extractMethod = extractMethod;
        }

        final static Lambda<IMethod,Boolean> INCLUDE_ALL_METHODS = 
            new Lambda<IMethod,Boolean>(){
                @Override public Boolean call(IMethod i) { return true; }};
                
        static Lambda<IMethod,Boolean> thisMethodOnly(final IMethod m) {
            return new Lambda<IMethod,Boolean>(){
                @Override public Boolean call(IMethod i) {
                    return i.equals(m);
                }};
        }

        public ICompilationUnit getCompilationUnit() {
            return compUnit;
        }
        
        public boolean extractMethod(IMethod method) {
            return this.extractMethod.call(method);
        }
    }
}