package edu.cmu.cs.anek.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import edu.cmu.cs.anek.graph.CalledRcvr;
import edu.cmu.cs.anek.graph.CalledReturn;
import edu.cmu.cs.anek.graph.FieldLoad;
import edu.cmu.cs.anek.graph.Graph;
import edu.cmu.cs.anek.graph.MergeNode;
import edu.cmu.cs.anek.graph.MethodGraph;
import edu.cmu.cs.anek.graph.Node;
import edu.cmu.cs.anek.graph.NodeSpecifics;
import edu.cmu.cs.anek.graph.ParameterDirection;
import edu.cmu.cs.anek.graph.Receiver;
import edu.cmu.cs.anek.graph.Return;
import edu.cmu.cs.anek.graph.SplitNode;
import edu.cmu.cs.anek.graph.StandardArg;
import edu.cmu.cs.anek.graph.StandardParameter;
import edu.cmu.cs.anek.graph.permissions.ConcretePermission;
import edu.cmu.cs.anek.graph.permissions.ConcretePermissionElement;
import edu.cmu.cs.anek.graph.permissions.Fraction;
import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.graph.permissions.PermissionKind;
import edu.cmu.cs.anek.graph.permissions.PermissionUse;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy;
import edu.cmu.cs.anek.graph.permissions.StateHierarchy.StateHierarchyNode;
import edu.cmu.cs.crystal.util.Option;


/**
 * This class allows us to load the graphml into a Graph.
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 * @see {@link Graph}
 */
public final class XMLToGraph {

    public static Graph loadGraph(InputStream i_stream) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(i_stream);

        Element root = document.getDocumentElement();
        return new XMLToGraph().loadGraph(root);
    }

    private Graph loadGraph(Element root) {        
        // Hierarchies
        // map from typename to state hierarchies
        Map<String,StateHierarchy> hierarchies =
            hierarchies(root);

        
        // Graphs
        NodeList method_elems = root.getElementsByTagName("graph");
        Iterable<MethodGraph> methods = methods(method_elems);

        return new Graph(methods, hierarchies.values());        
    }
    
    private final Map<String,Fraction> 
        fractions = new HashMap<String,Fraction>();
    
    private static Map<String, StateHierarchy> hierarchies(Element root) {
        // TODO this code does not actually load the
        // state hierarchy.
        return Collections.emptyMap();
    }

    // Given elements known to be <graph>s, 
    // return a list of MethodGraphs.
    private Iterable<MethodGraph> methods(NodeList methodElems) {
        List<MethodGraph> result = new LinkedList<MethodGraph>();
        for( int i=0;i<methodElems.getLength();i++ ) {
            Element method_elem = (Element) methodElems.item(i);
            result.add(method(method_elem));
        }
        return result;
    }

    private MethodGraph method(Element methodElem) {
        String method_key = methodElem.getAttribute("id");
        NodeList node_elems = methodElem.getElementsByTagName("node");
        NodeList data_elems1 = methodElem.getElementsByTagName("data");
        NodeList data_elems2 = methodElem.getElementsByTagName("data");
        NodeList data_elems3 = methodElem.getElementsByTagName("data");
        return new MethodGraph(nodes(node_elems), method_key, 
                methodName(data_elems1), isConstructor(data_elems2),
                overriddenMethods(data_elems3));
    }

    private boolean isConstructor(NodeList dataElems) {
        for( int i=0;i<dataElems.getLength();i++ ) {
            Element node_elem = (Element) dataElems.item(i);
            // is this the data element we are looking for?
            if( node_elem.getAttribute("key").equals("isctr") ) {
                return 
                Boolean.parseBoolean(node_elem.getFirstChild().getNodeValue());
            }
        }
        throw new RuntimeException("BAD XML");
    }

    private Set<String> overriddenMethods(NodeList dataElems) {
        Set<String> result = new HashSet<String>();
        
        for( int i=0;i<dataElems.getLength();i++ ) {
            Element node_elem = (Element) dataElems.item(i);
            // is this the data element we are looking for?
            if( node_elem.getAttribute("key").equals("overrides") ) {
                // Make sure to get the FIRST CHILD, which is a text node...
                result.add(node_elem.getFirstChild().getNodeValue());
            }
        }
        
        return result;
    }
    
    private String methodName(NodeList dataElems) {
        for( int i=0;i<dataElems.getLength();i++ ) {
            Element node_elem = (Element) dataElems.item(i);
            // is this the data element we are looking for?
            if( node_elem.getAttribute("key").equals("mname") ) {
                return node_elem.getFirstChild().getNodeValue();
            }
        }
        throw new RuntimeException("BAD XML");
    }

    private Set<Node> nodes(NodeList nodeElems) {
        Set<Node> result = new HashSet<Node>();
        for( int i=0;i<nodeElems.getLength();i++ ) {
            Element node_elem = (Element) nodeElems.item(i);
            result.add(node(node_elem));
        }
        return result;
    }

    private Node node(Element nodeElem) {
        Element spec_data = null;
        Element type_data = null;
        Element sync_data = null;
        
        String key = nodeElem.getAttribute("id");
        
        NodeList data_elems = nodeElem.getElementsByTagName("data");
        for( int i=0; i<data_elems.getLength();i++) {
            Element cur_data_elem = (Element) data_elems.item(i);
            if( "nodespecskey".equals(cur_data_elem.getAttribute("key")) )
                spec_data = cur_data_elem;
            if( "tname".equals(cur_data_elem.getAttribute("key")) )
                type_data = cur_data_elem;
            if( "synced".equals(cur_data_elem.getAttribute("key")) ) 
                sync_data = cur_data_elem;
        }
        if( spec_data == null || type_data == null || sync_data == null )
            throw new RuntimeException("BAD XML");
        
        // Must be present, or BAD XML
        Element specs_elem = 
            (Element) spec_data.getElementsByTagName("plural:node-specifics").item(0);
        
        String typeName = type_data.getNodeValue();
        
        boolean synced = Boolean.parseBoolean(sync_data.getNodeValue());
        
        NodeSpecifics specs = nodeSpecifics(specs_elem,key);
        return new Node(typeName,specs,synced);
    }

    private NodeSpecifics nodeSpecifics(Element specsElem, String key) {
        // Get the child, & see what kind of node it is
        {
            NodeList l = specsElem.getElementsByTagName("plural:standard-parameter");
            if( l.getLength() > 0 ) {
                return standardParameter((Element) l.item(0), key);
            }
        }
        {
            NodeList l = specsElem.getElementsByTagName("plural:standard-argument");
            if( l.getLength() > 0 ) {
                return standardArgument((Element) l.item(0));
            }
        }
        {
            NodeList l = specsElem.getElementsByTagName("plural:this");
            if( l.getLength() > 0 ) {
                return thisSpecs((Element) l.item(0), key);
            }
        }
        {
            NodeList l = specsElem.getElementsByTagName("plural:called-return");
            if( l.getLength() > 0 ) {
                return calledReturn((Element) l.item(0));
            }
        }
        {
            NodeList l = specsElem.getElementsByTagName("plural:called-receiver");
            if( l.getLength() > 0 ) {
                return calledReceiver((Element) l.item(0));
            }
        }
        {
            NodeList l = specsElem.getElementsByTagName("plural:return");
            if( l.getLength() > 0 ) {
                return returnSpec((Element) l.item(0), key);
            }
        }
        {
            NodeList l = specsElem.getElementsByTagName("plural:split");
            if( l.getLength() > 0 ) {
                return split((Element)l.item(0), key);
            }
        }
        {
            NodeList l = specsElem.getElementsByTagName("plural:merge");
            if( l.getLength() > 0 ) {
                return merge((Element)l.item(0), key);
            }
        }
        {
            NodeList l = specsElem.getElementsByTagName("plural:field-load");
            if( l.getLength() > 0 ) {
                return fieldLoad((Element)l.item(0), key);
            }
        }
        throw new RuntimeException("NYI");
    }

    private NodeSpecifics fieldLoad(Element item, String id) {
        long siteID = Long.parseLong(item.getAttribute("siteID"));
        Element perm_elem = getPermElement(item);
        Permission perm = permission(perm_elem);
        boolean isstatic = Boolean.parseBoolean(item.getAttribute("static"));
        String field_name = item.getAttribute("field-name");
        String key = FieldLoad.keyFromID(id);
        return new FieldLoad(siteID, perm, key, field_name, isstatic);
    }

    private NodeSpecifics merge(Element item, String id) {
        Element perm_elem = getPermElement(item);
        Permission perm = permission(perm_elem);
        
        String key = MergeNode.keyFromID(id);
        return new MergeNode(perm,key);
    }

    private NodeSpecifics split(Element item, String id) {
        Element perm_elem = getPermElement(item);
        Permission perm = permission(perm_elem);
        
        String key = SplitNode.keyFromID(id);
        return new SplitNode(perm,key);
    }

    private NodeSpecifics calledReturn(Element item) {
        long siteID = Long.parseLong(item.getAttribute("siteID"));
        Element perm_elem = getPermElement(item);
        Permission perm = permission(perm_elem);
        String meth_attr = item.getAttribute("method");
        Option<String> m_name = 
            meth_attr.equals("") ? Option.<String>none() : Option.some(meth_attr);
        String key = item.getAttribute("methodKey");
        return new CalledReturn(siteID, perm, key, m_name);
    }

    private NodeSpecifics thisSpecs(Element item, String id) {
        ParameterDirection dir =
            ParameterDirection.valueOf(item.getAttribute("direction"));
        Element perm_elem = getPermElement(item);
        Permission perm = permission(perm_elem);
        String methodKey = Receiver.keyFromID(id);
        return new Receiver(dir, perm, methodKey);
    }

    private NodeSpecifics calledReceiver(Element item) {
        long siteID = Long.parseLong(item.getAttribute("siteID"));
        ParameterDirection dir =
            ParameterDirection.valueOf(item.getAttribute("direction"));
        Element perm_elem = getPermElement(item);
        Permission perm = permission(perm_elem);
        String meth_attr = item.getAttribute("method");
        Option<String> m_name = 
            meth_attr.equals("") ? Option.<String>none() : Option.some(meth_attr);
        String key = item.getAttribute("methodKey");
        boolean isPrivate = Boolean.parseBoolean(item.getAttribute("isPrivate"));
        return new CalledRcvr(siteID, dir, perm, key, m_name, isPrivate);
    }

    private NodeSpecifics standardArgument(Element item) {
        long site_id = Long.parseLong(item.getAttribute("siteID"));
        int arg_pos = Integer.parseInt(item.getAttribute("argPos"));
        String meth_attr = item.getAttribute("method");
        Option<String> m_name = 
            meth_attr.equals("") ? Option.<String>none() : Option.some(meth_attr);
        ParameterDirection dir =
            ParameterDirection.valueOf(item.getAttribute("direction"));
        Element perm_elem = getPermElement(item);
        Permission perm = permission(perm_elem);
        String method_key = item.getAttribute("methodKey");
        return new StandardArg(site_id, arg_pos, dir, perm, m_name, method_key);
    }

    private NodeSpecifics returnSpec(Element elem, String id) {
        Element perm_elem = getPermElement(elem);
        Permission perm = permission(perm_elem);
        String methodKey = Return.methodKeyFromID(id);
        return new Return(perm, methodKey);
    }

    private NodeSpecifics standardParameter(Element elem, String key) {
        String name = elem.getAttribute("name");
        ParameterDirection dir =
            ParameterDirection.valueOf(elem.getAttribute("direction"));
        int pos = Integer.parseInt(elem.getAttribute("pos"));
        
        Element perm_elem = getPermElement(elem);
        Permission perm = permission(perm_elem);
     
        key = StandardParameter.methodKeyFromID(key);
        return new StandardParameter(dir, perm, key, name, pos);
    }

    private Element getPermElement(Element elem) {
        // There MUST be exactly one
        Element perm_elem_ = 
            (Element) elem.getElementsByTagName("plural:permission").item(0);
        return (Element)
            perm_elem_.getElementsByTagName("plural:concrete-perm").item(0);
    }

    private Permission permission(Element permElem) {
        NodeList c_perm_elems = permElem.getElementsByTagName("plural:concrete-perm-element");
        return new ConcretePermission(concretePermissionElems(c_perm_elems));
    }

    private Set<ConcretePermissionElement> concretePermissionElems(
            NodeList cPermElems) {
        Set<ConcretePermissionElement> result =
            new HashSet<ConcretePermissionElement>();
        for(int i=0;i<cPermElems.getLength();i++) {
            Element c_perm_elem =
                (Element) cPermElems.item(i);
            result.add(concretePermElem(c_perm_elem));
        }
        return result;
    }

    private ConcretePermissionElement concretePermElem(Element cPermElem) {
        PermissionKind kind = 
            PermissionKind.valueOf(cPermElem.getAttribute("kind"));
        PermissionUse usage =
            PermissionUse.valueOf(cPermElem.getAttribute("usage"));
        // TODO JUST MAKES UP A NODE!! Should get it from a hierarchy, perhaps
        String guarantee =
            cPermElem.getAttribute("guarantee");
        StateHierarchyNode g_node = new StateHierarchy.State(guarantee);
        Fraction fract = 
            createOrLoadFraction(cPermElem.getAttribute("fraction-id"));
        NodeList state_elems = cPermElem.getElementsByTagName("plural:state");
        
        return new ConcretePermissionElement(kind, fract, usage,
                g_node, permissionStates(state_elems));
    }

    private Set<StateHierarchyNode> permissionStates(NodeList stateElems) {
        Set<StateHierarchyNode> result = new HashSet<StateHierarchyNode>();
        for(int i=0; i<stateElems.getLength(); i++) {
            Element elem = (Element) stateElems.item(i);
            String state_name = elem.getAttribute("name");
            // TODO JUST MAKES UP A NODE!! Should get it from a hierarchy, perhaps
            result.add(new StateHierarchy.State(state_name));
        }
        return result;
    }

    private Fraction createOrLoadFraction(String id) {
        if( !this.fractions.containsKey(id) ) {
            this.fractions.put(id, new Fraction());
        }
        return this.fractions.get(id);
    }
    
}
