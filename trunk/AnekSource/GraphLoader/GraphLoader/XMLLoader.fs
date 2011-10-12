// 
// XMLLoader.fs
// Nels E. Beckman
// Implements the XMLLoader signature.
// Loads xml into the graph type.
//
// अनेक
// Anek
//

namespace Anek
    module XMLLoader =
        open ProgramGraph        
        open Utilities

        open System.Xml
        open System.Xml.Linq
        open System.Linq
        open System.Collections.Generic

        exception BadXML // done, and should never happen

        let graphml_ns = "http://graphml.graphdrawing.org/xmlns"

        let plural_ns = "http://www.nelsbeckman.com"
        
        // Doesn't coerce automatically...
        // with graphml namespace
        let gxn s : XName = XName.Get (s,graphml_ns)
        // with plural namespace
        let pxn s: XName = XName.Get (s,plural_ns)
        // with no namespace (for attributes)
        let an = XName.op_Implicit

        //
        // PERMISSION PARSING
        //
        let kind =
            function 
            |  "PURE" -> Pure
            |  "SHARE" -> Share
            |  "UNIQUE" -> Unique
            |  "FULL" -> Full
            |  "IMMUTABLE" -> Immutable
            | _ -> raise BadXML

        let usage =
            function
            | "Frame" -> Frame
            | "Virtual" -> Virtual
            | "Both" -> Both
            | _ -> raise BadXML

        let state(elem:XElement) =
            elem.Attribute(an "name").Value

        let concretePermElement(perm_elem:XElement) =
            let kind = kind (perm_elem.Attribute(an "kind").Value) in
            let usage = usage (perm_elem.Attribute(an "usage").Value) in
            let guarantee = perm_elem.Attribute(an "guarantee").Value in
            let states = perm_elem.Elements(pxn "state").Select( fun s -> state s ) in
            let states = List.ofSeq states in
            let fraction = perm_elem.Attribute(an "fraction-id").Value in 
            { kind = kind; guarantee = guarantee;
              states = states; fract = fraction; usage=usage }

        // <plural:concrete-permission>
        let concretePermission(perm_elem:XElement) (_:node_ID) =
            let perm_elems = perm_elem.Elements( pxn "concrete-perm-element" ).Select( fun e -> concretePermElement e ) in
            ConcretePerm(List.ofSeq perm_elems)

        // <plural:permission>
        let permission (perm_elem:XElement) (node_id:node_ID) : permission = 
            let specific_perm_elem = perm_elem.Elements().First() in
            let perm_name = specific_perm_elem.Name.LocalName in
            match perm_name with
            | "unground-perm" -> UnGroundPerm
            | "concrete-perm" -> concretePermission specific_perm_elem node_id
            | _ -> raise BadXML

        //
        // NODE SPECIFICS FUNCTIONS
        //

        // <standard-parameter>
        let standardParameter (elem:XElement) (node_id:node_ID) =
            let dir = 
                match elem.Attribute(an "direction").Value with
                | "PRE" -> Pre
                | "POST" -> Post
                | _ -> raise BadXML
            in 
            let name = elem.Attribute(an "name").Value in
            let pos = System.Int32.Parse(elem.Attribute(an "pos").Value) in
            let perm = permission (elem.Element(pxn "permission")) node_id in
            StandardParameter(name, dir, pos, perm)

        // <standard-argument>
        let standardArgument (elem:XElement) (node_id:node_ID) =
            let method_name = try Some(elem.Attribute(an "method").Value) with :? System.NullReferenceException -> None in
            let method_key = elem.Attribute(an "methodKey").Value in
            let dir = 
                match elem.Attribute(an "direction").Value with
                | "PRE" -> Pre
                | "POST" -> Post
                | _ -> raise BadXML
            in
            let site = System.Int64.Parse( elem.Attribute(an "siteID").Value ) in
            let arg_pos = System.Int32.Parse( elem.Attribute(an "argPos").Value ) in
            let perm = permission (elem.Element(pxn "permission")) node_id in
            StandardArg (method_key, method_name, dir, site, arg_pos, perm)

        // <return>
        let returnElement (elem:XElement) (node_id:node_ID) =
            let perm = permission (elem.Element(pxn "permission")) node_id in
            Return perm

        // <called-receiver>
        let calledReceiver (elem:XElement) (node_id:node_ID) =
            let method_ = try Some(elem.Attribute(an "method").Value) with :? System.NullReferenceException -> None in
            let method_key = elem.Attribute(an "methodKey").Value in
            let dir = 
                match elem.Attribute(an "direction").Value with
                | "PRE" -> Pre
                | "POST" -> Post
                | _ -> raise BadXML
            let site = System.Int64.Parse( elem.Attribute(an "siteID").Value ) in
            let perm = permission ( elem.Element(pxn "permission")) node_id in
            let isPrivate = bool.Parse( elem.Attribute(an "isPrivate").Value ) in
            CalledRcvr (method_key,method_,dir,site,perm,isPrivate)

        // <called-return>
        let calledReturn (elem:XElement) (node_id:node_ID) =
            let method_name = try Some(elem.Attribute(an "method").Value) with :? System.NullReferenceException -> None in
            let method_key = elem.Attribute(an "methodKey").Value in
            let site_id = System.Int64.Parse( elem.Attribute(an "siteID").Value ) in
            let perm = permission (elem.Element(pxn "permission")) node_id in
            CalledReturn(method_key,method_name, site_id, perm)

        // <this>
        let thisElement (elem:XElement) (node_id:node_ID) =
            let dir = 
                match elem.Attribute(an "direction").Value with
                | "PRE" -> Pre
                | "POST" -> Post
                | _ -> raise BadXML
            in
            let perm = permission (elem.Element(pxn "permission")) node_id in
            Receiver(dir,perm)

        // <field-load>
        let fieldLoad (elem:XElement) (node_id:node_ID) =
            let field = elem.Attribute(an "field-name").Value in
            let site = System.Int64.Parse( elem.Attribute(an "siteID").Value ) in
            let istatic = System.Boolean.Parse( elem.Attribute(an "static").Value ) in
            let istatic = if istatic then Static else Instance in
            let perm = permission (elem.Element(pxn "permission")) node_id in
            let rcvr_ids = elem.Elements(pxn "receiver").Select(fun (e:XElement) -> e.Attribute(an "id").Value) in
            FieldLoad(field,site,istatic,perm, ref (Uninit(List.ofSeq  rcvr_ids)))

        // <field-store>
        let fieldStore (elem:XElement) (node_id:node_ID) =
            let field = elem.Attribute(an "field-name").Value in
            let site = System.Int64.Parse( elem.Attribute(an "siteID").Value ) in
            let istatic = System.Boolean.Parse( elem.Attribute(an "static").Value ) in
            let istatic = if istatic then Static else Instance in
            let perm = permission (elem.Element(pxn "permission")) node_id in
            let rcvr_ids = elem.Elements(pxn "receiver").Select(fun (e:XElement) -> e.Attribute(an "id").Value) in
            FieldLoad(field,site,istatic,perm, ref (Uninit(List.ofSeq  rcvr_ids)))

        // <merge>
        let merge (elem:XElement) node_id =
            let perm = permission (elem.Element(pxn "permission")) node_id in
            Merge(perm)

        // <split>
        let split (elem:XElement) node_id =
            let perm = permission (elem.Element(pxn "permission")) node_id in
            Split(perm)

        // <node-specifics> 
        let nodeSpecifics (elem:XElement) (node_id:node_ID) : node_Specifics = 
            let specific_elem = elem.Elements().First() in
            let specs_tag = specific_elem.Name.LocalName in 
            match specs_tag with
                | "standard-argument" -> standardArgument specific_elem node_id
                | "standard-parameter" -> standardParameter specific_elem node_id
                | "called-return" -> calledReturn specific_elem node_id
                | "called-receiver" -> calledReceiver specific_elem node_id
                | "this" -> thisElement specific_elem node_id
                | "return" -> returnElement specific_elem node_id
                | "field-load" -> fieldLoad specific_elem node_id
                | "field-store" -> fieldStore specific_elem node_id
                | "merge" -> merge specific_elem node_id
                | "split" -> split specific_elem node_id
                | _ -> raise BadXML
        
        // create nodes but don't fill in the neighbors
        let  incompleteNode (node:XElement) (id_node_map:IDictionary<string,node>) : node =
            let id = (node.Attribute (an "id")).Value in
            // pick out node specifics
            let node_specs_filter = fun (data_elem:XElement) -> (data_elem.Attribute (an "key")).Value = "nodespecskey" in
            let sdata = (node.Elements (gxn "data")).Where( node_specs_filter ).First() in
            let specs_elem = sdata.Element(pxn "node-specifics") in
            let specs = nodeSpecifics specs_elem id in

            let node_type_filter = fun (data_elem:XElement) -> (data_elem.Attribute (an "key")).Value = "tname" in
            let tdata = (node.Elements (gxn "data")).Where( node_type_filter ).First() in
            let type_name = tdata.Value in

            let synced_filter = fun (data_elem:XElement) -> (data_elem.Attribute (an "key")).Value = "synced" in
            let syncdata = (node.Elements (gxn "data")).Where( synced_filter ).First() in
            let synced = bool.Parse(syncdata.Value) in
            
            let succs = ref [] in
            let preds = ref [] in
            let node:node = // new node with empty neighbors
                { id=id; succs = succs; preds = preds; specifics = specs; tyname=type_name; synced=synced } 
            in
            let _ = id_node_map.Add(id, node) in // add id to map so we can build edges
            node

        // takes <edge>, finds the nodes in the
        let linkEdge (edge_elem:XElement) (id_node_map:IDictionary<string,node>) =
            let source_id = edge_elem.Attribute(an "source").Value in
            let target_id = edge_elem.Attribute(an "target").Value in
            let source_node = id_node_map.Item source_id in
            let target_node = id_node_map.Item target_id in
            let source_prev_succs = !source_node.succs in
            let target_prev_preds = !target_node.preds in
            source_node.succs := (target_node::source_prev_succs);
            target_node.preds := (source_node::target_prev_preds)

        // go back to all the field load and field store nodes and replace
        // the IDs with nodes.
        let rec setFieldReceivers nodes (id_node_map:IDictionary<string,node>) =
            match nodes with
                | [] -> ()
                | hd::tl ->   
                    // if its a field load or a field store, find all the ids for nodes, look up the nodes, assign them
                    // to the receiver field.
                    match hd.specifics with
                        | FieldStore(_,_,_,_,({contents=Uninit(ids)} as c)) -> 
                            c := Init((List.map (fun id -> id_node_map.Item id)) ids);
                            setFieldReceivers tl id_node_map
                        | FieldLoad(_,_,_,_,({contents=Uninit(ids)} as c)) -> 
                            c := Init((List.map (fun id -> id_node_map.Item id)) ids);
                            setFieldReceivers tl id_node_map
                        | _ -> setFieldReceivers tl id_node_map

        // given a single method graph element, creates a method_Graph
        let methodGraph(graph_elem:XElement):method_Graph = 
            let method_key = (graph_elem.Attribute (an "id")).Value in
            // get the method name, which is in one of the data elements
            let method_name_filter = fun (data_elem:XElement) -> (data_elem.Attribute (an "key")).Value = "mname" in
            let mdata = (graph_elem.Elements (gxn "data")).Where( method_name_filter ).First() in
            let method_name = mdata.Value in
            // get is_constructor, which is one of the data elements
            let is_ctr_filter = fun (data_elem:XElement) -> (data_elem.Attribute (an "key")).Value = "isctr" in
            let ctrdata = (graph_elem.Elements (gxn "data")).Where( is_ctr_filter ).First() in
            let is_ctr = bool.Parse(ctrdata.Value) in
            // get overridden, which could be several of the data elements
            let overridden_filter = fun (data_elem:XElement) -> (data_elem.Attribute (an "key")).Value = "overrides" in
            let odata = (graph_elem.Elements (gxn "data")).Where( overridden_filter ) in
            let odata = Seq.fold (fun l (de:XElement) -> de.Value::l) [] odata in
            
            let id_node_map = new Dictionary<string,node>() in
            let nodes = (graph_elem.Elements (gxn "node")).Select( fun node -> incompleteNode node id_node_map ) in
            let nodes = List.ofSeq nodes in // IMPORTANT! Also forces nodes to be build, so next step can work.
            let _ = for edge in graph_elem.Elements (gxn "edge") do
                        linkEdge edge id_node_map
            let _ = setFieldReceivers nodes id_node_map in
            { methodKey = method_key; methodName = method_name; isConstructor=is_ctr; nodes = nodes; overridden=odata }

        // <state>
        let rec stateTree (elem:XElement):state =
            let name = elem.Attribute(an "name").Value in
            let dims = elem.Elements( pxn "dim" ) in
            let dims = List.ofSeq dims 
            in
                if dims.Length > 0
                then SNode(name, List.map dimTree dims)
                else SLeaf(name)
        // <dim>
        and dimTree (elem:XElement):dim =
            let name = elem.Attribute(an "name").Value in
            let states = elem.Elements(pxn "state") in
            let states = List.ofSeq states in
            let f_state = stateTree (List.head states) 
            in
                (name, f_state, List.map stateTree (List.tail states))
            

        let stateHierarchies(elem:XElement):(type_Name * state) = 
            let type_name = elem.Attribute(an "type").Value in
            let s_elem = elem.Element(pxn "state") in
            (type_name, stateTree s_elem)


        let graph(doc:XDocument) : graph =
            let graph_elems = doc.Root.Elements (gxn "graph") in
            let method_graphs = graph_elems.Select( fun g_elem -> methodGraph g_elem )
            in
            let hierarchy_data_node = 
                let h_key_filter = fun (d_elem:XElement) -> (d_elem.Attribute (an "key")).Value = "hierarchykey" 
                in (doc.Root.Elements (gxn "data")).Where(h_key_filter).First()
            in
            let hierarchies = hierarchy_data_node.Elements( pxn "state-hierarchy" ).Select( fun e -> stateHierarchies e )
            in
            { methods = List.ofSeq method_graphs;
              hierarchies = List.ofSeq hierarchies }
            
        // load XML reader, 
        let loadGraph (filename:string) : graph = let doc = XDocument.Load filename in graph(doc)
