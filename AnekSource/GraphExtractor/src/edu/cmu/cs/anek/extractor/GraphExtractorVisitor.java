package edu.cmu.cs.anek.extractor;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.cmu.cs.anek.eclipse.EclipseUtils;
import edu.cmu.cs.anek.extractor.VariableFlowAnalysis.PredLattice;
import edu.cmu.cs.anek.graph.CalledRcvr;
import edu.cmu.cs.anek.graph.FieldStore;
import edu.cmu.cs.anek.graph.MethodGraph;
import edu.cmu.cs.anek.graph.Node;
import edu.cmu.cs.anek.graph.NodeSpecifics;
import edu.cmu.cs.anek.graph.ParameterDirection;
import edu.cmu.cs.anek.graph.Receiver;
import edu.cmu.cs.anek.graph.Return;
import edu.cmu.cs.anek.graph.SplitNode;
import edu.cmu.cs.anek.graph.StandardArg;
import edu.cmu.cs.anek.graph.StandardParameter;
import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.graph.permissions.PermissionUse;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy;
import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.tac.ITACFlowAnalysis;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Box;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.plural.alias.AliasingLE;


/**
 * In order to build a graph we need to visit various AST nodes in the
 * graph, and make calls to the dataflow analysis. This work does the
 * visiting.
 * 
 * @author Nels E. Beckman
 *
 */
final class GraphExtractorVisitor extends ASTVisitor {

    private Map<PredNode, Node> variableUsedNodes = new HashMap<PredNode, Node>();
	// this map from type to state hierarchy is updated lazily when 
	// calls to the ExtractStateHierarchy class are made.
	private final PermissionExtractor permExtractor;
	
	private final TACFlowAnalysis<TupleLatticeElement<Aliasing, PredLattice>> analysis;
	private final ITACFlowAnalysis<AliasingLE> aliasAnalysis;
    private Node returnNode;
    private Set<Node> allGraphNodes;
		
	GraphExtractorVisitor(TACFlowAnalysis<TupleLatticeElement<Aliasing, PredLattice>> analysis,
	        ITACFlowAnalysis<AliasingLE> aliasAnalysis,
	        Map<ITypeBinding, StateHierarchy> hierarchiesMap) {
	    this.analysis = analysis;
	    this.aliasAnalysis = aliasAnalysis;
	    // One per method so that any permission caching is cleared.
	    this.permExtractor = new PermissionExtractor(hierarchiesMap);
	}
	
	private static boolean rcvrSynchronized(MethodDeclaration m) {
	    return 
	        !Modifier.isStatic(m.getModifiers()) &&
	        Modifier.isSynchronized(m.getModifiers());
	}
	
	MethodGraph visitAndExtract(MethodDeclaration method) {
	    IMethodBinding method_binding = method.resolveBinding();
	    
	    // Create nodes for incoming parameters
	    this.allGraphNodes = new HashSet<Node>();
	    // keep track of which aliasing locations to add POST nodes for
	    Map<IVariableBinding, Aliasing> param_locations = new HashMap<IVariableBinding,Aliasing>();
	    int param_pos = 0;
	    for( Object param_ : method.parameters() ) {
            SingleVariableDeclaration param = (SingleVariableDeclaration) param_;
            
            IVariableBinding param_bind = param.resolveBinding();
            if( Utilities.isReferenceType(param_bind) ) {
                // Get variable and location, to check post-condition.
                AliasingLE a_lattice = aliasAnalysis.getResultsAfter(param);
                Variable param_var = aliasAnalysis.getSourceVariable(param_bind);
                param_locations.put(param_bind, a_lattice.get(param_var));
                
                Parameter parameter_pred_node = 
                    new Parameter(param_bind, param_pos,method_binding);
                // modifies variableUsedNodes
                parameter_pred_node.createOrReturnNode(this.variableUsedNodes, this.permExtractor);
            }
            param_pos++;
	    }
	    
	    // this
	    if( !Modifier.isStatic(method_binding.getModifiers()) ) {
	        PredNode pn = new This(method_binding);
            // modifies variableUsedNodes
	        Node n = pn.createOrReturnNode(variableUsedNodes, permExtractor);
	        if( rcvrSynchronized(method) )
	            n.setSynchronized(true);
	    }
	    
	    // return
        ITypeBinding return_type = method_binding.getReturnType();
	    if( Utilities.isReferenceType(return_type) ) {
	        Permission return_perm = permExtractor.returnedPermission(method_binding);
	        NodeSpecifics specs = 
	            new Return(return_perm, method.resolveBinding().getKey());
	        this.returnNode = new Node(EclipseUtils.bestNameableType(return_type), specs);
	        allGraphNodes.add(returnNode);
	    }
	    
	    // Visit method body
	    method.accept(this);
	    
	    // Create nodes for outgoing parameters & connect them
	    TupleLatticeElement<Aliasing, PredLattice>  end_value = this.analysis.getEndResults(method);
	    int param_num = 0;
	    for( Object param_ : method.parameters() ) {
	        SingleVariableDeclaration param = (SingleVariableDeclaration) param_;
	        IVariableBinding param_binding = param.resolveBinding();
	        
	        if( Utilities.isReferenceType(param_binding) ) {

	            // Create outgoing parameter node	        
	            ITypeBinding param_type = param_binding.getType();
	            Permission param_perm = permExtractor.parameterPostPermissions(method_binding, 
	                    param_num, param_type);
	            NodeSpecifics spec = new StandardParameter(ParameterDirection.POST, param_perm, 
	                    param_binding.getKey(), param_binding.getName(), param_num);
	            String param_type_name = EclipseUtils.bestNameableType(param_type);
	            Node param_graph_node = new Node(param_type_name, spec);
	            allGraphNodes.add(param_graph_node);

	            // get lattice results
	            Aliasing param_loc = param_locations.get(param_binding);
	            PredLattice preds_ = end_value.get(param_loc);
	            Set<? extends PredNode> preds = preds_.predecessors();

	            // for each predecessor, add this as a child
	            for( PredNode pred_node : preds ) {
	                Node pred = pred_node.createOrReturnNode(variableUsedNodes, this.permExtractor);
	                
	                // only if the method is an actual method...
	                if( method.getBody() != null )
	                    pred.addAdjacentNode(param_graph_node);
	            }
	        }
	        param_num++;
	    }
	    
	    // outgoing rcvr
	    if( !Modifier.isStatic(method_binding.getModifiers()) ) {
	        Permission this_perm = permExtractor.rcvrPostPermissions(method_binding);
	        NodeSpecifics this_node = 
	            new Receiver(ParameterDirection.POST,this_perm, method_binding.getKey());
	        ITypeBinding rcvr_type = method_binding.getDeclaringClass();
	        String rcvr_type_name = EclipseUtils.bestNameableType(rcvr_type);
	        Node this_post = new Node(rcvr_type_name,this_node);
	        if( rcvrSynchronized(method) )
	            this_post.setSynchronized(true);
	        
	        allGraphNodes.add(this_post);
	        
	        // get lattice results
	        Variable this_var = this.analysis.getThisVariable(method);
            Aliasing this_loc = aliasAnalysis.getResultsBefore(method).get(this_var);
            PredLattice preds_ = end_value.get(this_loc);
            Set<? extends PredNode> preds = preds_.predecessors();
            
            // for each predecessor, add this as a child
            for( PredNode pred_node : preds ) {
                Node pred = pred_node.createOrReturnNode(variableUsedNodes, this.permExtractor);
                
                // only if the method is an actual method...
                if( method.getBody() != null )
                    pred.addAdjacentNode(this_post);
            }
	    }
	    
	    // Create as method graph
	    allGraphNodes.addAll(variableUsedNodes.values());
	    MethodGraph result_ = new MethodGraph(allGraphNodes, 
	            Utilities.erasedMethod(method_binding).getKey(), 
	            method_binding.getName(), method_binding.isConstructor(),
	            erasedOverriddenMethodKeys(method_binding));
	    
	    // Any Optimization...
	    result_ = GraphOptimization.removeMergeDeadends(result_);
	    
	    return result_;
	}
	
	private static Set<String> erasedOverriddenMethodKeys(IMethodBinding method) {
	    final Set<String> result = new HashSet<String>();
	    class MCallback extends EclipseUtils.MethodHierarchyCallback {
            @Override
            public boolean nextMethod(IMethodBinding method) {
                result.add(Utilities.erasedMethod(method).getKey());
                // TODO For the moment, we are only returning the first
                // overridden method. I think this may be a better idea
                // since making everything equal to 50/50 could have the
                // reverse effect from what we want.
                return false;
            }
	        
	    }
	    EclipseUtils.visitMethodHierarchy(new MCallback(), method, false);
	    return result;
	}
	
	//
	// VISIT METHODS
	//
	@Override
    public boolean visit(TypeDeclaration node) {
	    // Do not visit any nested classes.
	    return false;
    }
	
	
	
    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        // Do not visit any nested classes.
        return false;
    }

    @Override
    public void endVisit(MethodInvocation node) {
        int arg_num = 0;
        
        TupleLatticeElement<Aliasing,PredLattice> tuple = analysis.getResultsBefore(node);
        AliasingLE a_lattice = aliasAnalysis.getResultsBefore(node);
        IMethodBinding method_binding = node.resolveMethodBinding();
        
        // Args
        for( Object arg_ : node.arguments() ) {
            Expression arg  = (Expression)arg_;
            if( Utilities.isReferenceType(arg.resolveTypeBinding()) ) {
                // create arg pre, split & merge
                hookUpArgNodes(node, arg, arg_num, a_lattice, tuple);
            }
            
            arg_num++;
        }
        
        // Receiver:
        if( !Modifier.isStatic(method_binding.getModifiers()) ) {
            // create rcvr pre, split & merge
            hookUpReceiverNodes(node, a_lattice, tuple);
        }
        
        
        // Return value: Do nothing. If it is
        // needed, subsequent nodes will create it.
    }

    @Override
    public void endVisit(SynchronizedStatement stmt) {
        Expression sync_expr = stmt.getExpression();
        TupleLatticeElement<Aliasing,PredLattice> tuple = analysis.getResultsBefore(sync_expr);
        AliasingLE a_lattice = aliasAnalysis.getResultsBefore(sync_expr);
        
        Variable sync_var = analysis.getVariable(sync_expr);
        Aliasing sync_loc = a_lattice.get(sync_var);
        Set<? extends PredNode> preds = tuple.get(sync_loc).predecessors();
        // for each predecessor, set sync'ed to TRUE.
        for( PredNode pred : preds ) {
            Node node = pred.createOrReturnNode(variableUsedNodes, permExtractor);
            node.setSynchronized(true);
        }
    }

    /**
     * For the given receiver, create a split, pre and post, and hood them all
     * up together.
     */
    private void hookUpReceiverNodes(MethodInvocation method, AliasingLE a_lattice,
            TupleLatticeElement<Aliasing,PredLattice> tuple) {
        IMethodBinding method_binding = method.resolveMethodBinding();
        
        // First create the split node, and hook it up to the preds of this expression
        ITypeBinding rcvr_type = method_binding.getDeclaringClass();
        Node split_node = new Node(EclipseUtils.bestNameableType(rcvr_type),
                new SplitNode());
        allGraphNodes.add(split_node);
        
        // lattice value 
        Variable rcvr_var = method.getExpression() == null ? 
                analysis.getImplicitThisVariable(method_binding) : 
                analysis.getVariable(method.getExpression());
        Aliasing rcvr_loc = a_lattice.get(rcvr_var);
        Set<? extends PredNode> preds = tuple.get(rcvr_loc).predecessors();
        
        // set as a child
        for( PredNode pred_node : preds ) {
            Node pred = pred_node.createOrReturnNode(variableUsedNodes, permExtractor);
            pred.addAdjacentNode(split_node);
        }
        
        // Now, create the pre, post, and hook them up with the existing merge
        long siteID = StandardArg.siteID(method);
        String method_key = Utilities.erasedMethod(method_binding).getKey();
        Option<String> fullyQualifiedName = EclipseUtils.fullyQualifiedName(method_binding);
        
        boolean isPrivate = Modifier.isPrivate(method_binding.getModifiers());
        
        // PRE
        Permission pre_perm = this.permExtractor.rcvrPrePermissions(method_binding);
        pre_perm = adjustUseFromAccess(pre_perm,method_binding);
        NodeSpecifics pre_specs = new CalledRcvr(siteID, ParameterDirection.PRE,
                pre_perm,method_key, fullyQualifiedName, isPrivate);
        Node rcvr_pre = new Node(EclipseUtils.bestNameableType(rcvr_type),pre_specs);
        split_node.addAdjacentNode(rcvr_pre);
        allGraphNodes.add(rcvr_pre);
        // POST
        Permission post_perm = this.permExtractor.rcvrPostPermissions(method_binding);
        post_perm = adjustUseFromAccess(post_perm,method_binding);
        NodeSpecifics post_specs = new CalledRcvr(siteID, ParameterDirection.POST,
                post_perm, method_key, fullyQualifiedName, isPrivate);
        Node rcvr_post = new Node(EclipseUtils.bestNameableType(rcvr_type),post_specs);
        allGraphNodes.add(rcvr_post);
        
        // Now get the merge node
        PredNode merge_node_ = new MethodReceiver(method.resolveMethodBinding(),method);
        Node merge_node = merge_node_.createOrReturnNode(variableUsedNodes, permExtractor);
        // Two nodes go into the merge node b/c it's a MERGE node.
        rcvr_post.addAdjacentNode(merge_node);
        split_node.addAdjacentNode(merge_node);
    }
    
    /**
     * Adjusts the 'use' of the given permission based on the given called
     * method.
     */
    private static Permission adjustUseFromAccess(Permission p,
            IMethodBinding called_method) {
        if( !Modifier.isPrivate(called_method.getModifiers()) )
            return p.copyWithNewUsage(PermissionUse.Virtual);
        else
            return p;
                
    }
    
    /**
     * For the given argument, create a split node ahead of the arg, create a 
     * pre node and a post node. Hook them all up.
     */
    private void hookUpArgNodes(MethodInvocation method, Expression arg, int arg_num,
            AliasingLE a_lattice, TupleLatticeElement<Aliasing,PredLattice> tuple) {
        IMethodBinding method_binding = method.resolveMethodBinding();
        ITypeBinding param_type = method_binding.getParameterTypes()[arg_num];
        
        // First thing to do is to create a split node, and hook up all the preds
        // to the split.
        Node split_node = new Node(EclipseUtils.bestNameableType(param_type),
                new SplitNode());
        allGraphNodes.add(split_node);
        
        // Get lattice value
        Variable arg_var = analysis.getVariable(arg);
        Aliasing arg_loc = a_lattice.get(arg_var);
        Set<? extends PredNode> preds = tuple.get(arg_loc).predecessors();
        
        // Set split as a child of all predecessors
        for( PredNode pred_node : preds ) {
            Node pred = pred_node.createOrReturnNode(this.variableUsedNodes, this.permExtractor);
            pred.addAdjacentNode(split_node);
        }
        
        // BUT, there are additional nodes to create. Create an arg pre and
        // an arg post.
        long siteID = StandardArg.siteID(method);
        Option<String> fullyQualifiedName = EclipseUtils.fullyQualifiedName(method_binding);
        String method_key = method_binding.getKey();
        
        // PRE
        Permission pre_perm = 
            this.permExtractor.parameterPrePermissions(method_binding, arg_num, param_type);
//        pre_perm = adjustUseFromAccess(pre_perm, method_binding);
        NodeSpecifics arg_pre_spec = new StandardArg(siteID,arg_num,
                ParameterDirection.PRE, pre_perm, fullyQualifiedName, method_key);
        Node arg_pre = new Node(EclipseUtils.bestNameableType(param_type), arg_pre_spec);
        split_node.addAdjacentNode(arg_pre);
        allGraphNodes.add(arg_pre);
        // POST
        Permission post_perm = this.permExtractor.parameterPostPermissions(method_binding, arg_num, param_type);
//        post_perm = adjustUseFromAccess(post_perm, method_binding);
        NodeSpecifics arg_post_spec = new StandardArg(siteID,arg_num,
                ParameterDirection.POST, post_perm, fullyQualifiedName, method_key);
        Node arg_post = new Node(EclipseUtils.bestNameableType(param_type), arg_post_spec);
        allGraphNodes.add(arg_post);
        
        PredNode merge_node_ = new MethodArg(arg_num,method.resolveMethodBinding(),method);
        Node merge_node = merge_node_.createOrReturnNode(this.variableUsedNodes, this.permExtractor);
        
        // TWO edges come into the merge node, hence the MERGE! :-)
        arg_post.addAdjacentNode(merge_node);
        split_node.addAdjacentNode(merge_node);
    }
    
    @Override
    public void endVisit(ReturnStatement node) {
        Expression expr = node.getExpression();
        if( expr != null ) { // false if method is void.
            ITypeBinding expr_type = expr.resolveTypeBinding();
            if( Utilities.isReferenceType(expr_type) ) {
                // It's a matter of construction that this does not equal null.
                Node return_node = this.returnNode;
                // Get lattice for expr
                TupleLatticeElement<Aliasing,PredLattice> tuple = analysis.getResultsAfter(node);
                // and aliasing locations
                AliasingLE a_lattice = this.aliasAnalysis.getResultsAfter(node);
                
                Variable expr_var = analysis.getVariable(expr);
                PredLattice lattice = tuple.get(a_lattice.get(expr_var));
                for( PredNode pred_ : lattice.predecessors() ) {
                    Node pred = pred_.createOrReturnNode(variableUsedNodes, permExtractor);
                    pred.addAdjacentNode(return_node);
                }
            }
        }
    }


    @Override
    public boolean visit(final Assignment node) {
        // We only really care if this is an assignment to a field.
        Expression lhs = node.getLeftHandSide();
        final Expression rhs = node.getRightHandSide();
        
        Option<Pair<IVariableBinding,Option<Variable>>> field_ =
            fieldReadHelper(lhs);
        if( field_.isSome() ) {
            IVariableBinding field = field_.unwrap().fst();
            Option<Variable> rcvr = field_.unwrap().snd();
            fieldAssignHelper(field, node, rcvr);
        }

        
        // Still go into the right-hand side
        rhs.accept(this);
        return false;
    }

    private void fieldAssignHelper(IVariableBinding field, 
            Assignment node, Option<Variable> rcvr) {
        Expression rhs = node.getRightHandSide();
        // Get results from flow analysis
        TupleLatticeElement<Aliasing,PredLattice> tuple = analysis.getResultsAfter(rhs);
        // get aliasing locations
        AliasingLE a_lattice = aliasAnalysis.getResultsAfter(rhs);

        Set<Node> rcvr_nodes;
        if( rcvr.isSome() ) {
            // Field store nodes hold reference to the predecessors
            // of the receiver node.
            Variable rcvr_var = rcvr.unwrap();
            PredLattice lattice = tuple.get(a_lattice.get(rcvr_var));
            rcvr_nodes = new HashSet<Node>();
            for( PredNode rcvr_pred_ : lattice.predecessors() ) {
                Node rcvr_pred = rcvr_pred_.createOrReturnNode(variableUsedNodes, permExtractor);
                rcvr_nodes.add(rcvr_pred);
            }
        }
        else 
            rcvr_nodes = Collections.emptySet();

        // create new graph node
        long siteID = edu.cmu.cs.anek.graph.FieldLoad.nodeToSiteID(node);
        Permission field_perm = this.permExtractor.extractField(field);
        NodeSpecifics store = new FieldStore(siteID,field_perm, rcvr_nodes,
                Modifier.isStatic(field.getModifiers()), 
                field.getKey(), EclipseUtils.fullyQualifiedName(field).unwrap());
        ITypeBinding field_type = field.getType();
        Node store_node = 
            new Node(EclipseUtils.bestNameableType(field_type),store);
        allGraphNodes.add(store_node);

        // For RHS get pred after the entire assignment. This will be split node
        // which we want to lead into the FIELD node
        Variable rhs_var = analysis.getVariable(rhs);
        PredLattice lattice = analysis.getResultsAfter(node).get(a_lattice.get(rhs_var));
        for( PredNode pred_ : lattice.predecessors() ) {
            Node pred = pred_.createOrReturnNode(variableUsedNodes, permExtractor);
            pred.addAdjacentNode(store_node);
        }
        
        // ADDITIONALLY, get split node, hook up to Preds of RHS, before assignment
        edu.cmu.cs.anek.extractor.FieldStore f_pred = 
            new edu.cmu.cs.anek.extractor.FieldStore(field,node);
        Node split_node = f_pred.createOrReturnNode(variableUsedNodes, permExtractor);
        PredLattice b4_lattice = tuple.get(a_lattice.get(rhs_var));
        for( PredNode pred_ : b4_lattice.predecessors() ) {
            Node pred = pred_.createOrReturnNode(variableUsedNodes, permExtractor);
            pred.addAdjacentNode(split_node);
        }
    }

    
    // Given a possible field read node, returns SOME if it is a read
    // and returns a pair of the biding and a variable for the
    // receiver if there is a non-static one.
    private Option<Pair<IVariableBinding,Option<Variable>>> fieldReadHelper(ASTNode node) {
        final Box<Pair<IVariableBinding,Option<Variable>>> result_ = Box.box(null);
        node.accept(new ASTVisitor() {
            @Override
            public boolean visit(FieldAccess node) {
                IVariableBinding field = node.resolveFieldBinding();
                if( Utilities.isReferenceType(field.getType()) ) {
                    Option<Variable> rcvr = Option.none();
                    if( !Modifier.isStatic(field.getModifiers()) ) { 
                        Variable rcvr_expr = analysis.getVariable(node.getExpression());
                        rcvr = Option.some(rcvr_expr);
                    }

                    result_.setValue(Pair.create(field, rcvr));

                    node.getExpression().accept(GraphExtractorVisitor.this);
                }
                return false;
            }

            @Override
            public boolean visit(QualifiedName node) {
                // A qualified name basically has to be a field access.
                IBinding binding = node.getName().resolveBinding();
                if( binding instanceof IVariableBinding ) {
                    IVariableBinding field_binding = (IVariableBinding) binding;
                    if( Utilities.isReferenceType(field_binding.getType()) ) {
                        IBinding rcvr_binding = node.getQualifier().resolveBinding();
                        Option<Variable> rcvr_var = Option.none();
                        if( rcvr_binding instanceof IVariableBinding ) {
                            Variable rcvr_var_ = analysis.getVariable(node.getQualifier());
                            rcvr_var = Option.some(rcvr_var_);
                        }

                        result_.setValue(Pair.create(field_binding, rcvr_var));

                        node.getQualifier().accept(GraphExtractorVisitor.this);
                    }
                }
                return false;
            } 

            @Override
            public boolean visit(SimpleName node) {
                // A simple name might be a field access...
                IBinding binding = node.resolveBinding();
                if( binding instanceof IVariableBinding ) {
                    IVariableBinding var_binding = (IVariableBinding) binding;
                    if( var_binding.isField() ) {
                        if( Utilities.isReferenceType(var_binding.getType()) ) {
                            Option<Variable> rcvr_var = Option.none();
                            if( !Modifier.isStatic(var_binding.getModifiers()) ) {
                                Variable var = analysis.getImplicitThisVariable(var_binding);
                                rcvr_var = Option.some(var);
                            }
                            result_.setValue(Pair.create(var_binding, rcvr_var));
                        }
                    }
                }
                return false;
            }});
        
        
        if( result_.getValue() == null )
            return Option.none();
        else
            return Option.some(result_.getValue());
    }
    
    /*
     * ALL POTENTIAL FIELD READS GO DOWN HERE.
     * As far as I know only these three types can possible
     * be a field read.
     */
    
    @Override
    public boolean visit(FieldAccess node) {
        return allFieldReads(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        return allFieldReads(node);
    }

    @Override
    public boolean visit(SimpleName node) {
        return allFieldReads(node);
    }
    
    private boolean allFieldReads(ASTNode node) {
        // Have to do analysis first...
        TupleLatticeElement<Aliasing, PredLattice> analysis_results = analysis.getResultsAfter(node);
        
        Option<Pair<IVariableBinding,Option<Variable>>> field_ =
            fieldReadHelper(node);
        if( field_.isSome() ) {
            IVariableBinding field = field_.unwrap().fst();
            Option<Variable> rcvr_var_ = field_.unwrap().snd();
            
            if( rcvr_var_.isSome() ) {
                // We only have to do anything at all if there is a
                // receiver that we want to link to 
                PredNode pn = new FieldLoad(field, node);
                // HACK HACK HACK!
                Node n = pn.createOrReturnNode(variableUsedNodes, permExtractor);
                
                edu.cmu.cs.anek.graph.FieldLoad load = 
                    (edu.cmu.cs.anek.graph.FieldLoad)n.getSpecifics();

                // get receiver preds
                Variable rcvr_var = rcvr_var_.unwrap();
                Aliasing rcvr_loc = aliasAnalysis.getResultsAfter(node).get(rcvr_var);
                PredLattice lattice = analysis_results.get(rcvr_loc);
                for( PredNode rcvr_pred : lattice.predecessors() ) {
                    load.addRcvrNode(rcvr_pred.createOrReturnNode(variableUsedNodes, permExtractor));
                }
            }
        }
        // helper visits children...
        return false;
    }
}
