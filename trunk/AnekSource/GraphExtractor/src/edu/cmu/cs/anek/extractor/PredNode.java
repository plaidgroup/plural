package edu.cmu.cs.anek.extractor;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import edu.cmu.cs.anek.eclipse.EclipseUtils;
import edu.cmu.cs.anek.graph.CalledRcvr;
import edu.cmu.cs.anek.graph.CalledReturn;
import edu.cmu.cs.anek.graph.MergeNode;
import edu.cmu.cs.anek.graph.Node;
import edu.cmu.cs.anek.graph.NodeSpecifics;
import edu.cmu.cs.anek.graph.ParameterDirection;
import edu.cmu.cs.anek.graph.Receiver;
import edu.cmu.cs.anek.graph.SplitNode;
import edu.cmu.cs.anek.graph.StandardArg;
import edu.cmu.cs.anek.graph.StandardParameter;
import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.graph.permissions.PermissionUse;
import edu.cmu.cs.anek.util.Utilities;
import edu.cmu.cs.crystal.util.Option;

/**
 * An intermediate representation which will be used to
 * get a CFG using TAC nodes (Three Address Code, from the
 * Crystal framework) to our actual nodes which are
 * subclasses of {@link Node). Equality is very important
 * for all of these nodes! 
 *  
 * @author Nels E. Beckman
 *
 */
interface PredNode {
    Node createOrReturnNode(Map<PredNode, Node> map, PermissionExtractor permExtractor);
}

final class MethodArg implements PredNode {
    private final int argNum;
    
    private final IMethodBinding method;
    private final ASTNode callNode;
    
    public MethodArg(int argNum, IMethodBinding method, ASTNode callNode) {
        super();
        this.argNum = argNum;
        this.method = method;
        this.callNode = callNode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + argNum;
        result = prime * result
                + ((callNode == null) ? 0 : callNode.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
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
        MethodArg other = (MethodArg) obj;
        if (argNum != other.argNum)
            return false;
        if (callNode == null) {
            if (other.callNode != null)
                return false;
        } else if (!callNode.equals(other.callNode))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        return true;
    }

    @Override
    public Node createOrReturnNode(Map<PredNode, Node> map, PermissionExtractor permExtractor) {
        // For now we are using the call ast node's hashcode as
        // a numerical identifier. Hope this is okay...
        if( !map.containsKey(this) ) {
            // type name
            ITypeBinding p_type = 
                method.getParameterTypes()[this.argNum];
            
            // on a method arg return, the permission should flow into a merge node.
            NodeSpecifics spec = new MergeNode();
            Node n = new Node(EclipseUtils.bestNameableType(p_type), spec);
            map.put(this, n);
        }
        return map.get(this);
    }
}

final class MethodReturn implements PredNode {
    private final IMethodBinding method;
    private final ASTNode callNode;
    
    public MethodReturn(IMethodBinding method, ASTNode callNode) {
        super();
        this.method = method;
        this.callNode = callNode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((callNode == null) ? 0 : callNode.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
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
        MethodReturn other = (MethodReturn) obj;
        if (callNode == null) {
            if (other.callNode != null)
                return false;
        } else if (!callNode.equals(other.callNode))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        return true;
    }

    @Override
    public Node createOrReturnNode(Map<PredNode, Node> map, PermissionExtractor permExtractor) {
        if( !map.containsKey(this) ) {
            IMethodBinding binding = method;
            Permission perm = permExtractor.returnedPermission(binding);
            NodeSpecifics specs = new CalledReturn(StandardArg.siteID(callNode), 
                    perm, binding.getKey(), EclipseUtils.fullyQualifiedName(binding));
            // return type
            ITypeBinding return_type = binding.getReturnType();
            
            Node result = 
                new Node(EclipseUtils.bestNameableType(return_type), specs);
            map.put(this, result);
        }
        return map.get(this);
    }
    
    
}

final class MethodReceiver implements PredNode {
    private final IMethodBinding method;
    private final ASTNode callNode;

    public MethodReceiver(IMethodBinding method, ASTNode callNode) {
        super();
        this.method = method;
        this.callNode = callNode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((callNode == null) ? 0 : callNode.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
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
        MethodReceiver other = (MethodReceiver) obj;
        if (callNode == null) {
            if (other.callNode != null)
                return false;
        } else if (!callNode.equals(other.callNode))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        return true;
    }



    @Override
    public Node createOrReturnNode(Map<PredNode, Node> map, PermissionExtractor permExtractor) {
        if( !map.containsKey(this) ) {
            // rcvr name
            ITypeBinding rcrv_type = this.method.getDeclaringClass();
            String bestNameableType = EclipseUtils.bestNameableType(rcrv_type);
            // for normal nodes, create a merge node, but for
            // constructor nodes (!) create a rcvr post
            NodeSpecifics specs;
            if( this.method.isConstructor() ) {
                Permission perm = permExtractor.rcvrPostPermissions(method);
                perm = perm.copyWithNewUsage(PermissionUse.Virtual);
                Option<String> fullyQualifiedName = EclipseUtils.fullyQualifiedName(method);
                long siteID = StandardArg.siteID(callNode);
                boolean isPrivate = Modifier.isPrivate(method.getModifiers());
                specs = new CalledRcvr(siteID, ParameterDirection.POST, perm, 
                        Utilities.erasedMethod(method).getKey(),
                        fullyQualifiedName, isPrivate);
            }
            else {
                specs = new MergeNode();
            }
            Node n = new Node(bestNameableType,specs);
            map.put(this, n);
        }
        return map.get(this);
    }
}

final class Parameter implements PredNode {
    private final IVariableBinding var;
    private final int pos;
    private final IMethodBinding method;
    
    public Parameter(IVariableBinding var, int pos, IMethodBinding method) {
        this.var = var;
        this.pos = pos;
        this.method = method;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + pos;
        result = prime * result + ((var == null) ? 0 : var.hashCode());
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
        Parameter other = (Parameter) obj;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        if (pos != other.pos)
            return false;
        if (var == null) {
            if (other.var != null)
                return false;
        } else if (!var.equals(other.var))
            return false;
        return true;
    }

    @Override
    public Node createOrReturnNode(Map<PredNode, Node> map, 
            PermissionExtractor perm_extractor) {
        if( map.containsKey(this) )
            return map.get(this);
        else {
            // I think the node HAS to be a PRE-condition
            // node, and there is no way that a POST-condition
            // node will be created until the GraphExtractor runs.
            ITypeBinding type = var.getType();
            Permission perm =
                perm_extractor.parameterPrePermissions(method, pos, type);
            NodeSpecifics spec = 
                new StandardParameter(ParameterDirection.PRE, 
                        perm, var.getKey(), var.getName(), pos);
            Node node = new Node(EclipseUtils.bestNameableType(type), spec);
            map.put(this,node);
            return node;
        }
    }
    
    
}

final class FieldStore implements PredNode {

    private final IVariableBinding field;
    private final ASTNode loadNode;
    
    public FieldStore(IVariableBinding field, ASTNode loadNode) {
        this.field = field;
        this.loadNode = loadNode;
    }

    @Override
    public Node createOrReturnNode(Map<PredNode, Node> map,
            PermissionExtractor permExtractor) {
        if( !map.containsKey(this) ) {
            SplitNode specs = new SplitNode();
            Node n = new Node(EclipseUtils.bestNameableType(field.getType()),
                    specs);
            map.put(this, n);
        }
        return map.get(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result
                + ((loadNode == null) ? 0 : loadNode.hashCode());
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
        FieldStore other = (FieldStore) obj;
        if (field == null) {
            if (other.field != null)
                return false;
        } else if (!field.equals(other.field))
            return false;
        if (loadNode == null) {
            if (other.loadNode != null)
                return false;
        } else if (!loadNode.equals(other.loadNode))
            return false;
        return true;
    }
}

final class FieldLoad implements PredNode {

    private final IVariableBinding field;
    private final ASTNode loadNode;
    
    public FieldLoad(IVariableBinding field, ASTNode loadNode) {
        this.field = field;
        this.loadNode = loadNode;
    }

    @Override
    public Node createOrReturnNode(Map<PredNode, Node> map,
            PermissionExtractor permExtractor) {
        if( !map.containsKey(this) ) {
            Permission perm = permExtractor.extractField(field);
            long siteID = edu.cmu.cs.anek.graph.FieldLoad.nodeToSiteID(loadNode);
            boolean is_static = Modifier.isStatic(field.getModifiers());
            NodeSpecifics spec = new edu.cmu.cs.anek.graph.FieldLoad(siteID, perm, field.getKey(), 
                    EclipseUtils.fullyQualifiedName(field).unwrap(), is_static);
            Node node = new Node(EclipseUtils.bestNameableType(field.getType()), spec);
            map.put(this, node);
        }
        return map.get(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result
                + ((loadNode == null) ? 0 : loadNode.hashCode());
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
        FieldLoad other = (FieldLoad) obj;
        if (field == null) {
            if (other.field != null)
                return false;
        } else if (!field.equals(other.field))
            return false;
        if (loadNode == null) {
            if (other.loadNode != null)
                return false;
        } else if (!loadNode.equals(other.loadNode))
            return false;
        return true;
    }
}

final class This implements PredNode {
    private final IMethodBinding thisMethod;

    public This(IMethodBinding thisMethod) {
        this.thisMethod = thisMethod;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((thisMethod == null) ? 0 : thisMethod.hashCode());
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
        This other = (This) obj;
        if (thisMethod == null) {
            if (other.thisMethod != null)
                return false;
        } else if (!thisMethod.equals(other.thisMethod))
            return false;
        return true;
    }



    @Override
    public Node createOrReturnNode(Map<PredNode, Node> map, PermissionExtractor permExtractor) {
        if( !map.containsKey(this) ) {
            Permission perm = permExtractor.rcvrPrePermissions(this.thisMethod);
            NodeSpecifics specs = 
                new Receiver(ParameterDirection.PRE, perm, thisMethod.getKey());
            
            ITypeBinding this_type = thisMethod.getDeclaringClass();
            String nameable = EclipseUtils.bestNameableType(this_type);
            Node node = new Node(nameable,specs);
            map.put(this, node);
        }
        return map.get(this);
    }
}