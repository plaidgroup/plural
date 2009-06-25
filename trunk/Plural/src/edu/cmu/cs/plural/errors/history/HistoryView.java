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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.cs.crystal.IAnalysisInput;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.internal.AbstractCrystalPlugin;
import edu.cmu.cs.crystal.internal.Crystal;
import edu.cmu.cs.crystal.internal.WorkspaceUtilities;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.eclipse.CompilationUnitTACs;
import edu.cmu.cs.crystal.util.Box;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.concurrent.nimby.NIMBYTransferFunction;
import edu.cmu.cs.plural.concurrent.syncorswim.SyncOrSwimTransferFunction;
import edu.cmu.cs.plural.contexts.PluralContext;
import edu.cmu.cs.plural.states.IInvocationCase;
import edu.cmu.cs.plural.states.IInvocationCaseInstance;
import edu.cmu.cs.plural.states.IInvocationSignature;
import edu.cmu.cs.plural.states.MethodCheckingKind;
import edu.cmu.cs.plural.states.StateSpaceRepository;
import edu.cmu.cs.plural.track.FractionAnalysisContext;
import edu.cmu.cs.plural.track.FractionalAnalysis;
import edu.cmu.cs.plural.track.FractionalTransfer;

/**
 * A TreeView plugin which allows us to visualize the evolution of choice contexts.
 * 
 * @author Nels E. Beckman
 * @since Jun 1, 2009
 *
 */
public class HistoryView extends ViewPart implements ISelectionListener, ISelectionProvider {

	private TreeViewer treeViewer;
	
	@Override
	public void createPartControl(Composite parent) {
		this.treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.VIRTUAL);
		this.treeViewer.setUseHashlookup(true);
		
		IContentProvider content = new ContextTreeContentProvider();
		this.treeViewer.setContentProvider(content);
		this.treeViewer.addDoubleClickListener(new HistoryViewDoubleClickListener());
		
		this.treeViewer.setInput(new Object());
		
		// Register as listener
		getViewSite().getPage().addPostSelectionListener(this);
		
		// We provide tree selection events
		getSite().setSelectionProvider(this);
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	private static StateSpaceRepository getStateSpaceRepository(IAnalysisInput input) {
		return StateSpaceRepository.getInstance(input.getAnnoDB());
	}
	
	enum PluralAnalyses {
		PLURAL, NIMBY, SYNC_OR_SWIM;
	}
	
	class MethodHolder {
		IMethod method;
		MethodHolder(IMethod method) {
			this.method = method;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((method == null) ? 0 : method.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MethodHolder other = (MethodHolder) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (method == null) {
				if (other.method != null)
					return false;
			} else if (!method.equals(other.method))
				return false;
			return true;
		}
		private HistoryView getOuterType() {
			return HistoryView.this;
		}
	}
	
	class ContextTreeContentProvider implements ILazyTreeContentProvider {

		private Map<PluralAnalyses, ResultingDisplayTree> trees =
			new HashMap<PluralAnalyses, ResultingDisplayTree>(3);
		
		@Override public void dispose() { }

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.trees.clear();
		}

		@Override
		public Object getParent(Object element) {
			if( element instanceof MethodHolder )
				return null;
			else if( element instanceof IMethod )
				return new MethodHolder((IMethod) element);
			else if( element instanceof PluralAnalyses )
				return treeViewer.getInput();
			else if( element instanceof ResultingDisplayTree ) {
				ResultingDisplayTree display_tree = (ResultingDisplayTree)element;
				if( display_tree.getParent().isNone() ) // same as element instanceof PluralAnalyses
					return treeViewer.getInput();
				else
					return display_tree.getParent().unwrap();
			}
			else
				return null;
		}

		@Override
		public void updateChildCount(Object element, int currentChildCount) {
			if( element instanceof MethodHolder ) {
				if( currentChildCount != 1 )
					treeViewer.setChildCount(element, 1);
			}
			if( element instanceof IMethod ) {
				int num_analyses = PluralAnalyses.values().length;
				if( currentChildCount != num_analyses )
					treeViewer.setChildCount(element, num_analyses);
			}
			else if( element instanceof PluralAnalyses ) {
//				if( currentChildCount == 0 )
//					treeViewer.setHasChildren(element, true);
			}
			else if( element instanceof ResultingDisplayTree ) {
				int children = ((ResultingDisplayTree)element).getChildren().size();
				if( currentChildCount != children )
					treeViewer.setChildCount(element, children);
			}
		}

		@Override
		public void updateElement(Object parent, int index) {
			if( parent instanceof MethodHolder && index == 0) {
				IMethod method = ((MethodHolder)parent).method;
				treeViewer.replace(parent, index, method);
				treeViewer.setChildCount(method, PluralAnalyses.values().length);
			}
			else if( parent instanceof IMethod ) {
				PluralAnalyses[] values = PluralAnalyses.values();
				PluralAnalyses analysis = values[index];
				treeViewer.replace(parent, index, analysis);
				treeViewer.setHasChildren(analysis, true);
			}
			else if( parent instanceof PluralAnalyses ) {
				PluralAnalyses analysis_type = (PluralAnalyses)parent;
				
				if( !this.trees.containsKey(parent) ) {
					// Okay, the user actually wants answers. Let's
					// go compute them for the given 
					MethodHolder holder = (MethodHolder)treeViewer.getInput();
					MultiCaseHistoryTree tree = 
						runAnalysis(analysis_type, holder.method);
					
					ResultingDisplayTree analysis_tree = 
						tree.createTreeForDisplay(parent);
					
					this.trees.put(analysis_type, analysis_tree);
				}
				
				ResultingDisplayTree analysis_tree = this.trees.get(analysis_type);
				List<ResultingDisplayTree> children = analysis_tree.getChildren();
				
				treeViewer.setChildCount(parent, children.size());
				ResultingDisplayTree child = children.get(index);
				treeViewer.replace(parent, index, child);
				
				
				int child_children = child.getChildren().size();
				treeViewer.setChildCount(child, child_children);
			}
			else if( parent instanceof ResultingDisplayTree ) {
				ResultingDisplayTree parent_tree = (ResultingDisplayTree)parent;
				List<ResultingDisplayTree> children = parent_tree.getChildren();
				ResultingDisplayTree child = children.get(index);
				treeViewer.replace(parent, index, child);
				treeViewer.setChildCount(child, child.getChildren().size());
			}
			
			
		}

		
		
		/**
		 * This code is very similar to 
		 * {@link FractionalAnalysis#analyzeMethod(MethodDeclaration)} and
		 * the methods called by that method.
		 */
		private MultiCaseHistoryTree runAnalysis(PluralAnalyses analysis_type, IMethod method) {
			// Get AST node declaration from 
			MethodDeclaration method_decl = methodDeclarationFromMethod(method);
			
			// Create input, one for all cases
			IAnalysisInput input = createAnalysisInput();
			
			// Now we go 'per case.'
			// only analyze methods with code in them; skip abstract methods
			IMethodBinding binding = method_decl.resolveBinding();
			StateSpaceRepository stateSpaceRepository = getStateSpaceRepository(input);
			IInvocationSignature sig = stateSpaceRepository.getSignature(binding);
			
			MultiCaseHistoryTree graph = new MultiCaseHistoryTree();
			int classFlags = sig.getSpecifiedMethodBinding().getDeclaringClass().getModifiers();
			for( IInvocationCase case_ : sig.cases() ) {
				final boolean isFinalClass = Modifier.isFinal(classFlags);
				final boolean isAbstractClass = Modifier.isAbstract(classFlags);
				final boolean isStaticMethod = Modifier.isStatic(method_decl.getModifiers());
				
				if( isStaticMethod || (!isFinalClass && !isAbstractClass && !case_.isVirtualFrameSpecial()) ) {
					Pair<HistoryNode, SingleCaseHistoryTree> pair =
						analyzeCase(method_decl, sig, case_, null, input, analysis_type, stateSpaceRepository);
					HistoryRoot root = HistoryRoot.noSeparateCaseRoot(pair.fst(), case_.toString());
					graph.addRoot( root, pair.snd());
					
				}
				else {
					int modifiers = binding.getDeclaringClass().getModifiers();
					if( !Modifier.isFinal(modifiers) ) {
						Pair<HistoryNode, SingleCaseHistoryTree> pair =
							analyzeCase(method_decl, sig, case_, false, input, analysis_type, stateSpaceRepository);
						HistoryRoot root = HistoryRoot.virtualNotCurrent(pair.fst(), case_.toString());
						graph.addRoot( root, pair.snd());
					}
					if( !Modifier.isAbstract(modifiers) ) {
						Pair<HistoryNode, SingleCaseHistoryTree> pair =
							analyzeCase(method_decl, sig, case_, true, input, analysis_type, stateSpaceRepository);
						HistoryRoot root = HistoryRoot.virtualIsCurrent(pair.fst(), case_.toString());
						graph.addRoot( root, pair.snd());
					}
				}
			}
			
			return graph;
		}

		/**
		 * This method will analyze just one case of a method specification.
		 * It returns the root (in the form of the root linear context) and 
		 * the entire context graph created. 
		 * @param stateSpaceRepository 
		 */
		private Pair<HistoryNode, SingleCaseHistoryTree> analyzeCase(
				MethodDeclaration method_decl, IInvocationSignature sig,
				IInvocationCase case_, Boolean assumeVirtual, 
				IAnalysisInput input, PluralAnalyses analysis_type, 
				StateSpaceRepository stateSpaceRepository) {

			boolean assumeVirtual_ = assumeVirtual != null && assumeVirtual;
			
			// How are we checking this method?
			MethodCheckingKind checkingKind = 
				MethodCheckingKind.methodCheckingKindImpl(method_decl.isConstructor(), 
						assumeVirtual_);

			// Create case instance
			IInvocationCaseInstance case_instance =
				case_.createPermissions(checkingKind, true, assumeVirtual_);
			
			// Then analysis context
			FractionAnalysisContext context = 
				createFractionalAnalysisContext(input, stateSpaceRepository, 
						assumeVirtual_, case_instance);
			
			// Transfer function
			FractionalTransfer xfer_function = createTransferFunction(analysis_type, input, context);
			
			// and analysis
			TACFlowAnalysis<PluralContext> analysis = createFractionalAnalysis(xfer_function, input);
						
			return HistoryVisitor.visitAndBuildTree(method_decl, analysis, input.getComUnitTACs().unwrap());
		}

		/**
		 * Returns a fractional analysis given a transfer function and an analysis
		 * input.
		 */
		private TACFlowAnalysis<PluralContext> createFractionalAnalysis(FractionalTransfer xfer_function, IAnalysisInput input) {
			return new TACFlowAnalysis<PluralContext>(xfer_function, 
					input.getComUnitTACs().unwrap());
		}

		/**
		 * Create a transfer function based on the type of analysis we are trying
		 * to perform.
		 */
		private FractionalTransfer createTransferFunction(PluralAnalyses analysis_type,
				IAnalysisInput input, 
				FractionAnalysisContext context) {
			switch(analysis_type) {
			case PLURAL:
				return new FractionalTransfer(input, context);
			case SYNC_OR_SWIM:
				return new SyncOrSwimTransferFunction(input, context); 
			case NIMBY: 
				return new NIMBYTransferFunction(input, context);
			default:
				return Utilities.nyi("Impossible");
			}
		}

		/**
		 * Given a method from the model class hierarchy, get a method declaration
		 * from the ASTNode class hierarchy.
		 */
		private MethodDeclaration methodDeclarationFromMethod(final IMethod method) {
			// First thing we do is to actually parse this bad-boy.
			ICompilationUnit compilationUnit = method.getCompilationUnit();
			List<ICompilationUnit> compUnitList = Collections.singletonList(compilationUnit);
			Map<ICompilationUnit, ASTNode> map =
				WorkspaceUtilities.parseCompilationUnits(compUnitList);
			
			assert( map.containsKey(compilationUnit) );
			
			ASTNode root = map.get(compilationUnit);

			// now we have to recur down into the node until we find this method.
			final Box<Option<MethodDeclaration>> result_ = 
				Box.box(Option.<MethodDeclaration>none());
			
			root.accept(new ASTVisitor() {

				@Override
				public boolean visit(MethodDeclaration node) {
					// How do we know if they are the same?
					// Name, return type, signature?
					IJavaElement element = node.resolveBinding().getJavaElement();
					if( element.equals(method) ) {
						result_.setValue(Option.some(node));
						return false;
					}
					return super.visit(node);
				}
			});
			
			if( result_.getValue().isNone() ) 
				assert(false) : "Should be impossible";
			
			return result_.getValue().unwrap();
		}

		/**
		 * Create a 'fractional analysis context,' an input to the analysis
		 * that is more or less just a wrapper around IAnalaysisInput.
		 */
		private FractionAnalysisContext createFractionalAnalysisContext(
				final IAnalysisInput input, 
				final StateSpaceRepository stateSpaceRepository, 
				final Boolean assumeVirtualFrame, 
				final IInvocationCaseInstance analyzedCase) {
			return new FractionAnalysisContext(){

				@Override
				public boolean assumeVirtualFrame() {
					return assumeVirtualFrame;
				}

				@Override
				public IInvocationCaseInstance getAnalyzedCase() {
					return analyzedCase;
				}

				@Override
				public StateSpaceRepository getRepository() {
					return stateSpaceRepository;
				}

				@Override
				public AnnotationDatabase getAnnoDB() {
					return input.getAnnoDB();
				}

				@Override
				public Option<CompilationUnitTACs> getComUnitTACs() {
					return input.getComUnitTACs();
				}

				@Override
				public Option<IProgressMonitor> getProgressMonitor() {
					return input.getProgressMonitor();
				}};
		}

		/**
		 * Creates an input for a new analysis. This basically consists
		 * of creating a bunch of new input classes.
		 */
		private IAnalysisInput createAnalysisInput() {			
			final AnnotationDatabase annodb = new AnnotationDatabase();
			Crystal crystal = AbstractCrystalPlugin.getCrystalInstance();
			crystal.registerAnnotationsWithDatabase(annodb);
			
			return new IAnalysisInput() {
				private CompilationUnitTACs cutac = new CompilationUnitTACs();
				
				@Override
				public AnnotationDatabase getAnnoDB() {
					return annodb;
				}

				@Override
				public Option<CompilationUnitTACs> getComUnitTACs() {
					return Option.some(cutac);
				}

				@Override
				public Option<IProgressMonitor> getProgressMonitor() {
					return Option.none();
				}
			};
		}


		
	}
	
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// Only respond to selection of Method!
		if( selection != null && selection instanceof IStructuredSelection ) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (!ss.isEmpty()) {
				Object fe = ss.getFirstElement();
				if (fe instanceof IJavaElement) {
					IJavaElement ije = (IJavaElement) fe;
					if (ije instanceof IMethod) {
						IMethod im = (IMethod) ije;
						
						// If this is not already the input...
						MethodHolder new_input = new MethodHolder(im);
						if( !this.treeViewer.getInput().equals(new_input) )
							this.treeViewer.setInput(new_input);
					}
				}
			}
		}
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		this.treeViewer.addSelectionChangedListener(listener);
	}

	@Override
	public ISelection getSelection() {
		return this.treeViewer.getSelection();
	}

	@Override
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		this.treeViewer.removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		this.treeViewer.setSelection(selection);
	}
}