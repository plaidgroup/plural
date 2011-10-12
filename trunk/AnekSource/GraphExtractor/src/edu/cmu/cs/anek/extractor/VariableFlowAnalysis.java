package edu.cmu.cs.anek.extractor;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.analysis.alias.AliasLE;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.simple.AbstractingTransferFunction;
import edu.cmu.cs.crystal.simple.SimpleLatticeOperations;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.eclipse.EclipseTAC;
import edu.cmu.cs.crystal.tac.model.LoadFieldInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.ThisVariable;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.plural.alias.AliasingLE;

/**
 * Flow-based analysis for determining how variables
 * flow around a method body. The code in this class is
 * rather complex and convoluted because I am using a
 * dataflow analysis framework (Crystal) essentially to
 * avoid having to generate control-flow graphs myself.
 * The resulting analysis is therefore somewhat strange.
 * 
 * @author Nels E. Beckman
 *
 */
final class VariableFlowAnalysis {

    static class PredLattice {
        private Set<? extends PredNode> predecessors;
        
        PredLattice(Set<? extends PredNode> predecessors) {
            this.predecessors = predecessors;
        }

        PredLattice(PredNode predecessor) {
            this.predecessors = Collections.singleton(predecessor);
        }
        
        Set<? extends PredNode> predecessors() {
            return this.predecessors;
        }
    }
    
    static class Ops extends SimpleLatticeOperations<PredLattice> {
        // Notion of relationship in this lattice is a little unusual.
        // We basically want to keep going until the set reaches a 
        // fixedpoint. For our purposes, more elements means less precise,
        // which is kind of the same as a normal lattice in a way.
        
        @Override
        public boolean atLeastAsPrecise(PredLattice left, PredLattice right) {
            return right.predecessors().containsAll(left.predecessors());
        }

        private static final PredLattice BOTTOM = new PredLattice(Collections.<PredNode>emptySet());
        
        @Override
        public PredLattice bottom() {
            return BOTTOM;
        }

        @Override
        public PredLattice copy(PredLattice original) {
            return new PredLattice(original.predecessors());
        }

        @Override
        public PredLattice join(PredLattice left, PredLattice right) {
            return new PredLattice(Utilities.union(left.predecessors(), 
                    right.predecessors()));
        }
    }
    
    static class Transfer extends AbstractingTransferFunction<TupleLatticeElement<Aliasing,PredLattice>> {

        // local Alias analysis gives us abstract object locations.
        private final ITACFlowAnalysis<AliasingLE> aliasAnalysis;
        
        private final Ops ops = new Ops();
        private final TupleLatticeOperations<Aliasing, PredLattice> tuple_ops =  
            new TupleLatticeOperations<Aliasing, PredLattice>(ops, ops.bottom()); 
        
        private final EclipseTAC tac;
        
        public Transfer(EclipseTAC tac,
                ITACFlowAnalysis<AliasingLE> aliasAnalysis) {
            this.tac = tac;
            this.aliasAnalysis = aliasAnalysis;
        }

        @Override
        public TupleLatticeElement<Aliasing, PredLattice> createEntryValue(
                MethodDeclaration method) {
            IMethodBinding method_binding = method.resolveBinding();
            // For every method parameter, put that the last place it was read
            // was the declaration (ie., the pre-condtion)
            TupleLatticeElement<Aliasing, PredLattice> value = tuple_ops.getDefault();
            int param_pos = 0;
            for( Object svd_ : method.parameters() ) {
                SingleVariableDeclaration svd = (SingleVariableDeclaration) svd_;
                AliasingLE a_lattice = this.aliasAnalysis.getResultsAfter(svd);
                Variable var = this.tac.sourceVariable(svd.resolveBinding());
                PredNode pred_node = 
                    new Parameter(svd.resolveBinding(),param_pos,method_binding);
                PredLattice pred_lattice = new PredLattice(pred_node);
                value.put(a_lattice.get(var), pred_lattice);
                param_pos++;
            }
            
            AliasingLE a_lattice =  this.aliasAnalysis.getStartResults(method);
            if( !Modifier.isStatic(method_binding.getModifiers()) ) {
                // This
                ThisVariable this_var = this.tac.thisVariable();
                PredNode pred_node = new This(method_binding);
                AliasLE this_loc = a_lattice.get(this_var);
                assert(!this_loc.getLabels().isEmpty());
                value.put(this_loc, new PredLattice(pred_node));
            }
            return value;
        }

        @Override
        public ILatticeOperations<TupleLatticeElement<Aliasing, PredLattice>> getLatticeOperations() {
            return tuple_ops;
        }

        @Override
        public TupleLatticeElement<Aliasing, PredLattice> transfer(
                NewObjectInstruction instr,
                TupleLatticeElement<Aliasing, PredLattice> value) {
            // TODO Watch out for duplication errors. Copied from
            // below...
            ASTNode node = instr.getNode();
            ClassInstanceCreation method_call_node = (ClassInstanceCreation) node;
            //  results before SEEMS appropriate, except for the return
            // value...
            AliasingLE a_lattice = this.aliasAnalysis.getResultsBefore(instr);
            // args
            int i = 0;
            for( Variable arg : instr.getArgOperands() ) {
                if( Utilities.isReferenceType(arg.resolveType()) ) {
                    PredNode pred_node = 
                        new MethodArg(i, 
                                method_call_node.resolveConstructorBinding(),method_call_node);
                    value.put(a_lattice.get(arg), new PredLattice(pred_node));
                }
                i++;
            }
            
            // receiver (in a constructor, the receiver value is the
            // newly created thing)
            // Important difference: here we put the receiver value into
            // the target variable...
//            IMethodBinding method_binding = instr.resolveBinding();
            
            AliasingLE a_after_lattice = aliasAnalysis.getResultsAfter(instr);
            Variable target = instr.getTarget();
            value.put(a_after_lattice.get(target), 
                    new PredLattice(new MethodReceiver(method_call_node.resolveConstructorBinding(),
                            method_call_node)));
            
            // here's what we would have done had this been a normal rcvr
//            if( !Modifier.isStatic(method_binding.getModifiers() ) ) {
//                Variable rcvr = instr.getReceiverOperand();
//                value.put(a_lattice.get(rcvr), 
//                        new PredLattice(new MethodReceiver(method_call_node)));
//            }
            
            return value;
        }

        @Override
        public TupleLatticeElement<Aliasing, PredLattice> transfer(
                MethodCallInstruction instr,
                TupleLatticeElement<Aliasing, PredLattice> value) {
            ASTNode node = instr.getNode();
            
            if( node instanceof SuperMethodInvocation ) {
                System.err.println("Super method invocation... could still have params.");
                return value;
            }
            
            MethodInvocation method_call_node = (MethodInvocation) node;
            
            //  results before SEEMS appropriate, except for the return
            // value...
            AliasingLE a_lattice = this.aliasAnalysis.getResultsBefore(instr);
            // args
            int i = 0;
            for( Variable arg : instr.getArgOperands() ) {
                if( Utilities.isReferenceType(arg.resolveType()) ) {
                    PredNode pred_node = new MethodArg(i,method_call_node.resolveMethodBinding(), method_call_node);
                    value.put(a_lattice.get(arg), new PredLattice(pred_node));
                }
                i++;
            }
            // assign to
            // Method call site means 'return value'
            IMethodBinding method_binding = instr.resolveBinding();
            ITypeBinding return_type = method_binding.getReturnType();
            if( return_type != null && Utilities.isReferenceType(return_type) ) {
                AliasingLE a_after_lattice = aliasAnalysis.getResultsAfter(instr);
                Variable target = instr.getTarget();
                value.put(a_after_lattice.get(target), 
                        new PredLattice(new MethodReturn(method_call_node.resolveMethodBinding(),method_call_node)));
            }
            
            // receiver
            if( !Modifier.isStatic(method_binding.getModifiers() ) ) {
                Variable rcvr = instr.getReceiverOperand();
                value.put(a_lattice.get(rcvr), 
                        new PredLattice(new MethodReceiver(method_call_node.resolveMethodBinding(),method_call_node)));
            }
            return value;
        }

        @Override
        public TupleLatticeElement<Aliasing, PredLattice> transfer(
                LoadFieldInstruction instr,
                TupleLatticeElement<Aliasing, PredLattice> value) {

            IVariableBinding field_binding = instr.resolveFieldBinding();
            if( Utilities.isReferenceType(field_binding) ) {
                // After, b/c we want the target's location after the
                // assignment (which presumably is the field)
                AliasingLE a_lattice = aliasAnalysis.getResultsAfter(instr);
                // Set field load as predecessor to target.
                value.put(a_lattice.get(instr.getTarget()), 
                        new PredLattice(new FieldLoad(field_binding,instr.getNode())));
            }
            return value;
        }

        @Override
        public TupleLatticeElement<Aliasing, PredLattice> transfer(
                StoreFieldInstruction instr,
                TupleLatticeElement<Aliasing, PredLattice> value) {
            // The field store case is a  little unusual. Before the
            // field store node, we need a split node for the permission
            // coming into the expression that is being assigned to the
            // field. This will show how we remove the permission, and
            // is necessary to fully differentiate splits of permissions
            // from mere nodes with multiple successors.
            
            IVariableBinding field_binding = instr.resolveFieldBinding();
            if( Utilities.isReferenceType(field_binding) ) {
                // After, b/c we want the target's location after the
                // assignment (which presumably is the field)
                AliasingLE a_lattice = aliasAnalysis.getResultsAfter(instr);
                // Set field load as predecessor to target.
                value.put(a_lattice.get(instr.getSourceOperand()),
                        new PredLattice(new FieldStore(field_binding,instr.getNode())));
            }
            return value;
        }       
    }    
}