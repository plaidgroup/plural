//
// NodeConstraintGenerator.fs
// Nels E. Beckman
// Constraint generation module.
//
// अनेक
// Anek
//
namespace Anek
    module NodeConstraintGenerator = 
        open ProgramGraph
        open NodeConstraints
        open Utilities
        
        // Get the unground perm or None if it's not.
        let unground perm =
            match perm with
            | UnGroundPerm(_) -> Some perm
            | _ -> None

        // Given a node, returns SOME(dir) if the node is an arg or a rcvr,
        // and otherwise returns NONE
        let rcvrOrArgToDir node =
            match node.specifics with
            | StandardArg(_,_,dir,_,_,_) -> Some(dir)
            | CalledRcvr(_,_,dir,_,_,_) -> Some(dir)
            | _ -> None

                    
        /// Generates StatesSame constraints from nodes to the edges that
        /// touch them.
        let sameStatesGenerator nodes =
            let rec helper ns =
                match ns with 
                    | [] -> []
                    | n::tl ->
                        let out_constrs = List.map (fun succ -> StatesSame(n, (n.id,succ.id))) (!n.succs) in
                        let in_constrs  = List.map (fun pred -> StatesSame(n, (pred.id,n.id))) (!n.preds) in
                        out_constrs @ in_constrs @ (helper tl)
            in
            helper nodes

        /// Generates constraints saying that the frame permissions are the same
        let sameFrameGenerator nodes =
            let rec helper ns =
                match ns with 
                    | [] -> []
                    | n::tl ->
                        let out_constrs = List.map (fun succ -> FramesSame(n, (n.id,succ.id))) (!n.succs) in
                        let in_constrs  = List.map (fun pred -> FramesSame(n, (pred.id,n.id))) (!n.preds) in
                        out_constrs @ in_constrs @ (helper tl)
            in
            helper nodes

        /// Generate constraints for incoming edges, ie
        /// that the node itself is equal to one of them.
        /// Does NOT apply to merge nodes, which could be
        /// subject to borrowing and therefore magic!
        let incomingEdgesGenerator nodes =
            let helper n =
                match n.specifics with
                    | Merge(_) -> None
                    | _ -> 
                        if List.length (!n.preds) > 0
                        then Some(IncomingEdges(!n.preds,n))
                        else None
            in
            List.choose helper nodes

        
        /// Generate constraints for outgoing edges, ie
        /// that the outgoing edges are consistent and
        /// less than the current node. Should apply
        /// to all nodes...
        let outgoingEdgesGenerator nodes =
            let helper n =
                match (n.specifics,List.length (!n.succs)) with
                    | (Split(_),_) -> None
                    | (_,l) when l > 0 -> Some(OutgoingEdges(n,!n.succs))
                    | _ -> None
            in
            List.choose helper nodes

        /// Generate constraints for split edges, the edges
        /// that come out of a permission split
        let splitEdgesGenerator nodes =
            // put merge nodes at the beginning of the list...
            // This is a hack for our new implementation of
            // the splitEdges function.
            let put_merge_first ns =
                List.sortBy
                    (fun n -> match n.specifics with | Merge(_) -> 0 | _ -> 1)
                    ns
            in
            let helper n =
                match (n.specifics,List.length (!n.succs)) with
//                    | (Split(_),_) -> Some(OutgoingEdges(n,!n.succs)) // what is everything's equal?
                    | (Split(_),_) ->
                        let succs = put_merge_first (!n.succs) in
                        Some(SplitEdges(n,succs))
                    | (_,_) -> None
            in
            List.choose helper nodes
       
        /// If a path from a parameter PRE to a POST does not end in any other
        /// nodes, a borrowing path constraint is generated, meaning the pre/post
        /// is likely borrowed.
        /// SEE: methodCallGenerator, whose function is also related to borrowing.
        let borrowingGenerator(m:method_Graph) : nodal_Constraint list =
            // Data structures that pre-cache information we are going to want
            let (paramPosts,rcvrPost,argPosts,_) =
              List.fold // Add each type to its appropriate map, only going through list once.
                (fun (pp,rp,ap,crp) n ->
                   match n.specifics with
                     | StandardParameter(_,Post,pos,_) -> (Map.add pos n pp, rp,ap,crp)
                     | Receiver(Post,_) -> (pp, Some n, ap,crp)
                     | StandardArg(mkey,_,Post,site,pos,_) -> (pp,rp, Map.add (mkey,site,pos) n ap, crp)
                     | CalledRcvr(mkey,_,Post,site,_,_) -> (pp,rp,ap, Map.add (mkey,site) n crp)
                     | _ -> (pp,rp,ap,crp))
                (Map.empty,None,Map.empty,Map.empty) m.nodes
            in
            // Find leaf nodes: starting at the given node, find all of the nodes
            // reachable from this node that have no successors.
            let findLeafNodes n =
                // mutable!! 
                let visited = new System.Collections.Generic.HashSet<node_ID>() in
                let rec helper n_i =
                  let _ = visited.Add n_i.id in
                  // get successors to check if this is a final node, but
                  // only visit successors if they are not already visited,
                  // OR bridge gap over args
                  let neighbors =
                      match (!n.succs,n.specifics) with
                          | ([],StandardArg(mkey,_,Pre,site,pos,_)) -> [Map.find (mkey,site,pos) argPosts]
                          | (l,_) -> l
                  in
                  if List.isEmpty neighbors
                  then [n] // Base case. No neighbors, return yourself
                  else // only visit unvisited neighbors
                       List.concat (List.map (fun neighbor ->
                                                if visited.Contains neighbor.id then [] else helper neighbor)
                                              neighbors)
                in
                helper n
            in
            // you will notice this code looks suspciously like above, but it returns the argument pairs
            // encountered between the parameter and its post-condition
            // TODO Fix this duplication. :-{
            let findContingentArgs n =
                // mutable!! 
                let visited = new System.Collections.Generic.HashSet<node_ID>() in
                let rec helper n_i =
                  let _ = visited.Add n_i.id in
                  // get successors to check if this is a final node, but
                  // only visit successors if they are not already visited,
                  // OR bridge gap over args
                  let (neighbors,arg_pairs) =
                      match (!n.succs,n.specifics) with
                          | ([],StandardArg(mkey,_,Pre,site,pos,_)) ->
                              let arg_post = Map.find (mkey,site,pos) argPosts in
                              ([arg_post],[(n,arg_post)])
                          | (l,_) -> (l,[])
                  in
                  if List.isEmpty neighbors
                  then arg_pairs // Base case. No neighbors, return yourself
                  else // only visit unvisited neighbors
                       arg_pairs @
                       List.concat (List.map (fun neighbor ->
                                                if visited.Contains neighbor.id then [] else helper neighbor)
                                              neighbors)
                in
                helper n
            in
            // Main loop, go through each param or receiver pre
            let result =
              seq {
                // search begins at Parameter pre or RCVR pre
                for n in m.nodes do
                  match n.specifics with
                    | StandardParameter(_,Pre,pos,_) ->
                      let post = Map.find pos paramPosts in
                      let leaf_nodes = findLeafNodes n in
                      let maybe_borrow =
                        List.forall (fun n_i -> n_i.id = post.id) leaf_nodes
                      in
                      let r =
                        if maybe_borrow
                        then
                          let cont_args = findContingentArgs n in
                          if List.isEmpty cont_args then CanBorrow(n,post) else ContingentBorrow(n,post,cont_args)
                        else CannotBorrow(n,post)
                      in yield r
                    | Receiver(Pre,_) ->
                      let post = rcvrPost.Value in
                      let leaf_nodes = findLeafNodes n in
                      let maybe_borrow =
                        List.forall (fun n_i -> n_i.id = post.id) leaf_nodes
                      in
                      let r =
                        if maybe_borrow
                        then
                          let cont_args = findContingentArgs n in
                          if List.isEmpty cont_args then CanBorrow(n,post) else ContingentBorrow(n,post,cont_args)
                        else CannotBorrow(n,post)
                      in yield r
                    | _ -> () 
              }
            in
            List.ofSeq result
          




        // Generate access permissions for field reads
        // a field store requires a modifying permission, field read req.
        // a reading permission.
        let fieldAccessGenerator (nodes:node list) : nodal_Constraint list = 
            // returns a constraint setting the permission of node equal to one of
            // the writing or reading permission kinds.
            let fieldPerms writes (node:node) : nodal_Constraint list =
                let rcvr_perm = if writes then MustWrite(node) else MustRead(node) in
                NeedsFrame(node)::rcvr_perm::[]
            in
            // bool in return value is whether or not to call writers or readers
            let fieldRcvrs node : bool * (node list) =
                match node.specifics with
                    | FieldStore(_,_,_,_,{contents=Init(ns)}) -> (true,ns)
                    | FieldLoad(_,_,_,_,{contents=Init(ns)}) -> (false,ns)
                    | _ -> (false,[])
            in
            let helper (nodes:node list) : nodal_Constraint seq =
                // make a sequence, for every FieldLoad or FieldStore...
                seq {
                    for n1 in nodes do
                        let (writes,n2s) = fieldRcvrs n1 in
                            for n2 in n2s do
                                yield! fieldPerms writes n2                       
                }  
            in
            List.ofSeq(helper nodes)

        /// For every method call site, generates a MethodSplit constraint.
        /// The effect of this constraint (eventually) will be that, IF
        /// the permission is borrowed, the permission at the merge point
        /// is likely the same as at the split point. OTHERWISE, the perm
        /// at the merge point is either the split edge or the post edge.
        let methodCallGenerator (nodes:node list) : nodal_Constraint list =
            let helper_merge ns visited =
                seq {
                    for n4 in ns do
                        if Set.contains n4.id visited
                        then ()
                        else match n4.specifics with | Merge(_) -> yield n4 | _ -> ()
                }
            in
            let helper_post_rcvr site mkey ns visited =
                seq {
                    for n3 in ns do
                        if Set.contains n3.id visited
                        then ()
                        else 
                            match n3.specifics with
                                | CalledRcvr(mkey_,_,Post,site_,_,_) when mkey=mkey_ && site=site_ ->
                                    let visited = Set.add n3.id visited in
                                    for n4 in helper_merge (!n3.succs) visited do
                                        yield (n3,n4)
                                | _ -> ()
                }
            in
            let helper_post site mkey pos ns visited =
                seq {
                    for n3 in ns do
                        if Set.contains n3.id visited
                        then ()
                        else 
                            match n3.specifics with
                                | StandardArg(mkey_,_,Post,site_,pos_,_) when mkey=mkey_ && site=site_ && pos=pos_ ->
                                    let visited = Set.add n3.id visited in
                                    for n4 in helper_merge (!n3.succs) visited do
                                        yield (n3,n4)
                                | _ -> ()
                }
            in
            let helper_pre ns visited =
                seq {
                    for n2 in ns do
                        if Set.contains n2.id visited
                        then ()
                        else 
                            match n2.specifics with
                                | StandardArg(mkey,_,Pre,site,pos,_) -> 
                                    let visited = Set.add n2.id visited in
                                    for (n3,n4) in helper_post site mkey pos (nodes) visited do // Note, all nodes
                                        yield (n2,n3,n4)
                                | CalledRcvr(mkey,_,Pre,site,_,_) ->
                                    let visited = Set.add n2.id visited in
                                    for (n3,n4) in helper_post_rcvr site mkey (nodes) visited do
                                        yield (n2,n3,n4)
                                | _ -> ()
                }  
            in
            let helper_split ns =
                seq {
                    for n1 in ns do
                        match n1.specifics with
                            | Split(_) -> 
                                for (n2,n3,n4) in helper_pre (!n1.succs) (Set.singleton n1.id) do
                                    yield (n1,n2,n3,n4)
                            | _ -> ()
                }
            in
            let quads = helper_split nodes in
            Seq.fold 
                // for each 'quad' we create an EorS for the split to the pre, and a MethodSplit
                // 5-27-2010, remove equal or stronger? Why am I directly playing with the nodes?
                // Won't the edge rules handle this?
                //(fun result (n1,n2,n3,n4) -> (MethodSplit(n1,n2,n3,n4))::(EqualOrStronger(n1,n2))::result)
                (fun result (n1,n2,n3,n4) -> (MethodSplit(n1,n2,n3,n4))::result)
                List.empty quads


        // Finds all the loads and reads to the same field name, and asserts that 
        // their permissions are equal.
        let sameFieldSamePermGenerator (nodes:node list) : nodal_Constraint list =
            let nameAndPerm (nodes:node list) : seq<node * field_Name> =
                seq {
                    for n in nodes do
                        match n.specifics with
                            | FieldLoad(f,_,_,_,_) -> yield (n,f)
                            | FieldStore(f,_,_,_,_) -> yield (n,f)
                            | _ -> ()
                }
            in
            let helper (nodes:node list) : nodal_Constraint seq =
                seq {
                    // go through all nodes. if they are fields with the
                    // same name, but not the same node, and one of the
                    // perms is unground, make the perms equal.
                    for (n1,n1_f) in nameAndPerm nodes do
                        for (n2,n2_f) in nameAndPerm nodes do
                            if n1.id <> n2.id && n1_f = n2_f 
                            then yield Equal(n1,n2) 
                            else ()
                }
            in
            List.ofSeq (helper nodes)


        /// Generates the HUERISTIC constraint that method pres
        /// and posts generally have the same permission.
        let preAndPostSameH is_ctr (nodes) : nodal_Constraint list =
            List.choose 
                (fun n -> 
                    match n.specifics with
                        | StandardParameter(_,Pre,pos,_) ->
                            let post = findParam Post pos nodes in
                            Some(HEqual(n,post))
                        | Receiver(Pre,_) ->
                            // ONLY if this is not a constructor...
                            if is_ctr
                            then None
                            else
                                let post = findRcvr Post nodes in
                                Some(HEqual(n,post))
                        | _ -> None) 
                nodes

        /// Generates the HEURISTIC constraint that 'create' methods
        /// usually return unique permissions.
        let createUniqueH (m:method_Graph) : nodal_Constraint list = 
            if m.methodName.StartsWith("create") 
            then
                // might not have a return at all...
                let find_fun n =
                    match n.specifics with
                        | Return(_) -> true
                        | _ -> false
                in
                match List.tryFind find_fun m.nodes with
                    | Some(ret) -> [HCreateUnique(ret)]
                    | None -> []
            else []

        /// Generates the HEURISTIC constraint that 'getter' methods
        /// usually require reading permissions to their receivers.
        let gettersReadoH (m:method_Graph) : nodal_Constraint list =
            if m.methodName.StartsWith("get")
            then
                /// could be a static method...
                let find_fun dir n =
                    match n.specifics with
                        | Receiver(d,_) when d=dir -> true
                        | _ -> false
                in
                let rcvr_pre_ = List.tryFind (find_fun Pre) m.nodes in
                let rcvr_post_ = List.tryFind (find_fun Post) m.nodes in
                match (rcvr_pre_,rcvr_post_) with
                    | ( Some(n1), Some(n2) ) -> [HGetter(n1);HGetter(n2)]
                    | _ -> []
            else []


        /// Generates the HEURISTIC constraint that 'setter' methods
        /// usually require writing permissions to their receivers.
        let settersWriteH (m:method_Graph) : nodal_Constraint list =
            if m.methodName.StartsWith("set")
            then
                /// could be a static method...
                let find_fun dir n =
                    match n.specifics with
                        | Receiver(d,_) when d=dir -> true
                        | _ -> false
                in
                let rcvr_pre_ = List.tryFind (find_fun Pre) m.nodes in
                let rcvr_post_ = List.tryFind (find_fun Post) m.nodes in
                match (rcvr_pre_,rcvr_post_) with
                    | ( Some(n1), Some(n2) ) -> [HSetter(n1);HSetter(n2)]
                    | _ -> []
            else []

        /// finds Synchronized nodes
        let synchronizedNodeH ns : nodal_Constraint list =
            List.choose
              (fun n ->
                   if n.synced
                   then Some(HSynchronized(n))
                    else None)
              ns

        /// Generates the heuristic constraint that constructor returns
        /// are usually unique
        let constructorH (m:method_Graph) : nodal_Constraint list =
            if m.isConstructor
            then
                let rcvr = ProgramGraph.findRcvr Post m.nodes in
                [NeedsFrame(rcvr);HConstructor(rcvr)]
            else []

        /// Generate constraints saying that params, THIS and returns
        /// are equal to the specifications on the methods they
        /// override
        let overridesEqual (g:ProgramGraph.graph) =
            // map from keys to methods
            let method_map =
                List.fold (fun m meth -> Map.add meth.methodKey meth m) Map.empty g.methods 
            in
            // helper can return lists of constraints, just in
            // case we need to add more later...
            let helper n o =
                let overridden_n_ =
                    match n.specifics with
                        | StandardParameter(_,dir,pos,_) -> Some(findParam dir pos o.nodes)
                        | Return(_) -> Some(findReturn o.nodes)
                        | Receiver(dir,_) -> Some(findRcvr dir o.nodes)
                        | _ -> None
                in
                match overridden_n_ with
                    | Some(overridden_n) -> [Equal(n,overridden_n)]
                    | None -> []
            in
            let seq_ =
                seq {
                    for m in g.methods do
                        for o in m.overridden do
                            match Map.tryFind o method_map with
                                | None -> ()
                                | Some(o) ->
                                    for n in m.nodes do
                                        yield! helper n o
                }
            in
            List.ofSeq seq_

        /// Generate constraints that arguments to a function
        /// and the equivalent parameters have the same permission.
        let argsAndParamsEqual (g:ProgramGraph.graph) =
            // look for args... finding them, find the methods they call
            // then, find the corresponding parameters, if they exist
            // and finally, create an EqualOrStronger constraint from
            // arg to parameter.
            // go through all nodes, create a string->method map
            // this will be helpful when we need to find a given param for a 
            // given method from an argument
            let method_map =
                List.fold (fun m meth -> Map.add meth.methodKey meth m) Map.empty g.methods 
            in
            let helper n (this_method_nodes:node list) =
                match n.specifics with
                    | StandardArg(mkey,_,Pre,site,pos,_) -> 
                        let arg_post = findArg Post mkey site pos this_method_nodes in
                        let meth_ = Map.tryFind mkey method_map in
                        match meth_ with
                            | None -> None // maybe method was not analyzed...
                            | Some(meth) -> 
                                // ASSERTS that param exists...
                                let param_pre = findParam Pre pos meth.nodes in
                                let param_post = findParam Post pos meth.nodes in
                                // VERY important that the pres & posts are kept together so
                                // we can link their borrowing probabilities.
                                Some(ArgIsParam(n,arg_post,param_pre,param_post,false))
                    | StandardArg(mkey,_,Post,site,pos,_) ->
                        // Because of constructors, it is possible to have an arg post without
                        // a pre, so in those cases we still need to make arg & param be equal.
                        // Equal(...) is okay because we don't need the borrowing abilities to
                        // be linked...
                        let pre = tryFindArg Pre mkey site pos this_method_nodes in // Adding more linear ops... :-( PERF
                        match pre with
                            | Some(_) -> None
                            | None ->
                                let meth_ = Map.tryFind mkey method_map in
                                match meth_ with
                                    | None -> None
                                    | Some(meth) ->
                                        let param_post = findParam Post pos meth.nodes in
                                        Some(Equal(n,param_post))
                    | CalledRcvr(mkey,_,Pre,site,_,isPrivate) ->
                        let cr_post = findCalledRcvr Post mkey site this_method_nodes in
                        let meth_ = Map.tryFind mkey method_map in
                        match meth_ with
                            | None ->
                                None
                            | Some(meth) ->
                                let rcvr_pre = findRcvr Pre meth.nodes in
                                let rcvr_post = findRcvr Post meth.nodes in
                                Some(ArgIsParam(n,cr_post,rcvr_pre,rcvr_post,isPrivate))
                    | CalledRcvr(mkey,_,Post,site,_,_) ->
                        // Because of constructors, it is possible to have an arg post without
                        // a pre, so in those cases we still need to make arg & param be equal.
                        // Equal(...) is okay because we don't need the borrowing abilities to
                        // be linked...
                        let pre = tryFindCalledRcvr Pre mkey site this_method_nodes in // Adding more linear ops... :-( PERF
                        match pre with
                            | Some(_) -> None
                            | None ->
                                let meth_ = Map.tryFind mkey method_map in
                                match meth_ with
                                    | None -> None
                                    | Some(meth) ->
                                        let rcvr_post = findRcvr Post meth.nodes in
                                        Some(Equal(n,rcvr_post))
                    | CalledReturn(mkey,_,_,_) ->
                        let meth_ = Map.tryFind mkey method_map in
                        match meth_ with
                            | None -> None
                            | Some(meth) -> 
                                let ret = findReturn meth.nodes in
                                Some(Equal(n,ret))
                    | _ -> None // no other node types matter
            in
            let seq_ = 
                seq { 
                    for m in g.methods do
                        for n in m.nodes do
                            let constr_ = helper n m.nodes in
                            if constr_.IsSome then yield constr_.Value else ()
                }
            in
            List.ofSeq seq_
            
        /// Finds nodes that are predecessors of concrete permissions. Those nodes
        /// likely have SOME kind of permission, so generate a constraint saying so.
        let onPathOfConcretePermission (nodes:node list) =
            // remove duplicate nodes from the given list
            let removeDups nodes =
                let r =
                    List.fold
                        (fun (seen,result) node -> 
                            if Set.contains node.id seen
                            then (seen,result)
                            else (Set.add node.id seen,node::result))
                        (Set.empty,List.Empty) nodes
                in snd r
            in
            // Recursively finds all predecessor nodes.
            let rec findAllPreds (n:node) visited : node list =
                if Set.contains n.id visited 
                then List.empty
                else
                    let visited = Set.add n.id visited in
                    let preds = (!n.preds) in
                    preds @
                    (List.fold (fun s p -> s @ (findAllPreds p visited)) List.empty preds)
            in
            let concrete_nodes = 
                List.filter (fun n -> 
                                match nodePermission n with
                                    | ConcretePerm(_::_) -> true // must have at least one permission
                                    | _ -> false) nodes
            in
            let seq_ =
                seq {
                    for cn in concrete_nodes do
                        let preds = findAllPreds cn Set.empty in
                        let preds = removeDups preds in
                        for pred in preds do
                            yield ConcreteSuccessor(pred)
                }
            in
            List.ofSeq seq_
                                    
                

        // Some constraints are interprocedural in nature, and this
        // function will generate them.
        let generateInterproceduralConstraints (g:ProgramGraph.graph) =
            let _ = System.Console.WriteLine("  Generating interprocedural constraints...") in
            let aape = argsAndParamsEqual g in
            let over = overridesEqual g in
            let _ = System.Console.WriteLine("  Done.") in
            over @ aape 

        // removes all of the hueristic constraints
        let filterHeuristics (cs) =
            let filter =
                function | HEqual(_,_) -> false | HCreateUnique(_) -> false | HGetter(_) -> false | HSetter(_) -> false
                         | HSynchronized(_) -> false | HConstructor(_) -> false | ConcreteSuccessor(_) | _ -> true
            in
            List.filter filter cs

        let generateMethodConstraints (verbose:bool) (m:method_Graph) =
            let _ =
                if verbose then System.Console.WriteLine ("  Creating method constraints for method " + m.methodKey) else ()
            in
            let iec = incomingEdgesGenerator m.nodes in
            let oec = outgoingEdgesGenerator m.nodes in
            let sec = splitEdgesGenerator m.nodes in
            let ssc = sameStatesGenerator m.nodes in
            let sframec = sameFrameGenerator m.nodes in
            let opcc = onPathOfConcretePermission m.nodes in
            let sfc = sameFieldSamePermGenerator m.nodes in
            let fac = fieldAccessGenerator m.nodes in
            let bc = borrowingGenerator m in
            let mcc = methodCallGenerator m.nodes in
            let papc = preAndPostSameH m.isConstructor m.nodes in
            let createu = createUniqueH m in
            let getters = gettersReadoH m in
            let setters = settersWriteH m in
            let synced = synchronizedNodeH m.nodes in
            let ctrs = constructorH m in
            let _ = System.Console.WriteLine("  Done.")
            in  
            ctrs @ getters @ setters @ createu @ synced @ iec @ oec @ sec @ ssc @ sframec @ opcc @ sfc @ fac @ bc @ mcc @ papc

        // Run all of the constraint-generating visitors on
        // all of these method graphs. (I don't actually have
        // the visitors yet...)
        let generateConstraints (g:ProgramGraph.graph) (verbose:bool) (deterministic:bool) =
            // get all the constraints from all the 
            let method_constraints = List.reduce (List.append) (List.map (generateMethodConstraints verbose) g.methods) in
            let inter_proc_constraints = generateInterproceduralConstraints g in
            let result = inter_proc_constraints @ method_constraints in
            if deterministic
            then filterHeuristics result
            else result
