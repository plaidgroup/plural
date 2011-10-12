// 
// XMLWriter.fs
// Nels E. Beckman
// Code for writing a graph to XML format in a file.
//
// अनेक
// Anek
//
namespace Anek
    module XMLWriter =
        open Utilities
        open ProgramGraph

        let graph_ml_header = @"<?xml version=""1.0"" encoding=""UTF-8""?>
                              <graphml xmlns=""http://graphml.graphdrawing.org/xmlns""
                              xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
                              xmlns:plural=""http://www.nelsbeckman.com""
                              xsi:schemaLocation=""http://graphml.graphdrawing.org/xmlns 
                              xml/graphml+plural.xsd"">
                              <key for=""graphml"" id=""hierarchykey""/>
                              <key for=""graph"" id=""mname"" attr.name=""methodname"" attr.type=""string""/>
                              <key for=""node"" id=""nodespecskey""/>
                              <key for=""node"" id=""synced"" attr.name=""synchronized"" attr.type=""boolean""/>
                              <key for=""graph"" id=""overrides"" attr.name=""overriddenmethods"" attr.type=""string""/>
                              <key id=""tname"" for=""node"" attr.name=""typename"" attr.type=""string""/>"

        let rec pState (state:state) =
            seq {
                match state with
                    | SLeaf(name) -> yield "<plural:state name=\"" + name + "\"/>"
                    | SNode(name,dims) ->
                        yield "<plural:state name=\"" + name + "\">";
                        for dim in dims do
                            yield! pDim dim
                        yield "</plural:state>"
            }
        and pDim (name,fst_state,states) =
            seq {
                yield "<plural:dim name=\"" + name + "\">";
                yield! pState fst_state;
                for s in states do
                    yield! pState s
                yield "</plural:dim>";
            }

        // Given a list of static hierarchies, output string
        let hierarchyToString (hierarchies:(type_Name * state) list) = 
            seq {
                yield "<data key=\"hierarchykey\">";
                for (ty,state) in hierarchies do
                    yield "<plural:state-hierarchy type=\"" + ty + "\">";
                    yield! pState state;
                    yield "</plural:state-hierarchy>";
                yield "</data>";
            }

        let kindToString k =
            match k with
                | Unique -> "UNIQUE"
                | Full -> "FULL"
                | Immutable -> "IMMUTABLE"
                | Share -> "SHARE"
                | Pure -> "PURE"

        let usageToString u =
            match u with
                | Frame -> "Frame"
                | Virtual -> "Virtual"
                | Both -> "Both"

        let concreteElementToString cp = 
            let kind = kindToString cp.kind in
            let usage = usageToString cp.usage in
            let fid = cp.fract in
            seq {
                yield "<plural:concrete-perm-element kind=\"" + kind + "\" guarantee=\"" + 
                        cp.guarantee + "\" usage=\"" + usage + "\" fraction-id=\"" + fid + "\">";
                yield! List.map (fun s -> "<plural:state name=\"" + s + "\"/>") cp.states;
                yield "</plural:concrete-perm-element>";
            }

        let dirToString dir =
            match dir with
                | Pre -> "PRE"
                | Post -> "POST"

        let permToString perm = 
            seq {
                yield "<plural:permission>";
                match perm with
                    | UnGroundPerm -> yield "<plural:unground-perm/>"
                    | ConcretePerm(cps) -> 
                        yield "<plural:concrete-perm>";
                        for cp in cps do
                            yield! concreteElementToString cp;
                        yield "</plural:concrete-perm>";
                yield "</plural:permission>";
            }

        /// Take the node_Specifics and convert it into XML
        let specsToString (specs:node_Specifics) = 
            //
            // ONE HELPER FUNCTION PER SPECS TYPE
            //
            let crcvr key (name:method_Name option) dir site perm isPrivate =
                let site = site.ToString() in
                let dir = dirToString dir in
                let isPrivate = isPrivate.ToString() in
                let mname = if name.IsSome then "method=\"" + name.Value + "\"" else "" in
                seq {
                    yield "<plural:called-receiver " + mname + " methodKey=\"" + key + "\" siteID=\"" + site + "\" direction=\"" + dir + "\" isPrivate=\"" + isPrivate + "\">";
                    yield! permToString perm;
                    yield "</plural:called-receiver>";
                }
            in
            let cret key (name:method_Name option) site perm =
                let site = site.ToString() in
                let mname = if name.IsSome then "method=\"" + name.Value + "\"" else "" in
                seq {
                    yield "<plural:called-return " + mname + " methodKey=\"" + key + "\" siteID=\"" + site + "\">";
                    yield! permToString perm;
                    yield "</plural:called-return>"
                }
            in
            let fl name site istatic perm ns =
                let site = site.ToString() in
                let istatic = match istatic with | Static -> "true" | Instance -> "false" in
                seq {
                    yield "<plural:field-load siteID=\"" + site + "\" static=\"" + istatic + "\" field-name=\"" + name + "\">";
                    yield! permToString perm;
                    yield! List.map (fun n -> "<plural:receiver id=\"" + n.id + "\"/>") ns;
                    yield "</plural:field-load>"
                }
            in
            let fs name site istatic perm ns =
                let site = site.ToString() in
                let istatic = match istatic with | Static -> "true" | Instance -> "false" in
                seq {
                    yield "<plural:field-store siteID=\"" + site + "\" static=\"" + istatic + "\" field-name=\"" + name + "\">";
                    yield! permToString perm;
                    yield! List.map (fun n -> "<plural:receiver id=\"" + n.id + "\"/>") ns;
                    yield "</plural:field-store>"
                }
            in
            let rcvr dir perm =
                let dir = dirToString dir in
                seq {
                    yield "<plural:this direction=\"" + dir + "\">";
                    yield! permToString perm;
                    yield "</plural:this>";
                }
            in
            let ret perm =
                seq {
                    yield "<plural:return>";
                    yield! permToString perm;
                    yield "</plural:return>"
                }
            in
            let sa key (name:method_Name option) dir site pos perm =
                let site = site.ToString() in
                let dir = dirToString dir in
                let pos = pos.ToString() in
                let mname = if name.IsSome then "method=\"" + name.Value + "\"" else "" in
                seq {
                    yield "<plural:standard-argument " + mname + " methodKey=\"" + key + "\" siteID=\"" + site + "\" direction=\"" + dir + "\" argPos=\"" + pos + "\">";
                    yield! permToString perm;
                    yield "</plural:standard-argument>";
                }
            in
            let sp name dir pos perm =
                let dir = dirToString dir in
                let pos = pos.ToString() in
                seq {
                    yield "<plural:standard-parameter name=\"" + name + "\" direction=\"" + dir + "\" pos=\"" + pos + "\">";
                    yield! permToString perm;
                    yield "</plural:standard-parameter>";
                }
            in  
            let split perm =
                seq {
                    yield "<plural:split>";
                    yield! permToString perm;
                    yield "</plural:split>"
                }
            in
            let merge perm = 
                seq {
                    yield "<plural:merge>";
                    yield! permToString perm;
                    yield "</plural:merge>"
                }
            in
            seq {
                yield @"<data key=""nodespecskey"">";
                yield "<plural:node-specifics>";
                match specs with
                    | CalledRcvr(key,name,dir,site,perm,isPrivate) -> yield! crcvr key name dir site perm isPrivate
                    | CalledReturn(key,name,site,perm) -> yield! cret key name site perm
                    | FieldLoad(name,site,istatic,perm,{contents=Init(ns)}) -> yield! fl name site istatic perm ns
                    | FieldLoad(_) -> raise Impossible
                    | FieldStore(name,site,istatic,perm,{contents=Init(ns)}) -> yield! fs name site istatic perm ns
                    | FieldStore(_) -> raise Impossible
                    | Receiver(dir,perm) -> yield! rcvr dir perm
                    | Return(p) -> yield! ret p
                    | StandardArg(key,name,dir,site,pos,perm) -> yield! sa key name dir site pos perm
                    | StandardParameter(name,dir,pos,perm) -> yield! sp name dir pos perm
                    | Split(perm) -> yield! split perm
                    | Merge(perm) -> yield! merge perm
                yield "</plural:node-specifics>";
                yield "</data>"
            }

        let nodeToString (n:node) =
            let synced = n.synced.ToString() in
            seq {
                yield "<node id=\"" + n.id + "\">";
                yield "<data key=\"tname\">" + n.tyname + "</data>";
                yield "<data key=\"synced\">" + synced + "</data>";
                yield! specsToString n.specifics;
                yield "</node>"
            }

        /// Return the edges of the given node as XML
        let edges n =
            seq {
                for n2 in (!n.succs) do
                    yield "<edge source=\"" + n.id + "\" target=\"" + n2.id + "\"/>"
            }

        /// Given a method graph, output the method as xml
        let methodToString (m:method_Graph) =
            let is_ctr = m.isConstructor.ToString() in
            seq { 
                yield "<graph edgedefault=\"directed\" id=\"" + m.methodKey + "\">";
                yield "<data key=\"mname\">" + m.methodName + "</data>";
                yield "<data key=\"isctr\">" + is_ctr + "</data>";
                for o in m.overridden do
                    yield "<data key=\"overrides\">" + o + "</data>"
                for n in m.nodes do
                    yield! edges n
                for n in m.nodes do
                    yield! nodeToString n;
                yield "</graph>"
            }

        /// Creates a sequence of strings (lines really)
        /// that represent the given graph in graphml.
        let graphToString (g:graph) : string seq =
            seq {
                yield graph_ml_header;
                yield! hierarchyToString g.hierarchies
                for n in g.methods do
                    yield! methodToString n;
                yield "</graphml>"
            }

        /// write the given graph to the file given by the path 
        /// argument
        let writeGraphToFile (g:graph) (filename:string) : unit =
            let content = graphToString(g) in
            System.IO.File.WriteAllLines (filename, Seq.toArray content)
    
