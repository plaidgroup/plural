// 
// ProbConstraints.fs
// Nels E. Beckman
// Probabilistic constraint module. Generate probabilistic constraints
// from program constraints and solve them.
//
// अनेक
// Anek
//
namespace Anek
    module ProbConstraints =
        open Utilities
        open ProgramGraph
        open NodeConstraints

        open System.Collections.Generic
        open MicrosoftResearch.Infer
        open MicrosoftResearch.Infer.Models
        open MicrosoftResearch.Infer.Distributions
        open MicrosoftResearch.Infer.Factors
        open MicrosoftResearch.Infer.FSharp

        type cnstr_Params = {mrl:Bernoulli;
                             mwh:Bernoulli;mwl:Bernoulli;
                             sl:Variable<Bernoulli>;el:Bernoulli;
                             hel:Bernoulli;aap:Bernoulli;
                             cul:Bernoulli;cul_low:Bernoulli;
                             edge_l:Bernoulli;edge_l_low:Bernoulli;
                             splt_l:Bernoulli;
                             sync_l:Bernoulli;sync_l_low:Bernoulli;
                             ctr_l:Bernoulli;ctr_l_low:Bernoulli;
                             getter_l:Bernoulli;getter_l_low:Bernoulli;
                             ipt:float;bth:float;}


        // a map from nodes to infer.NET variables for each permission kind
        type node_Var_Maps = {node_Ndxs:Map<node_ID,int>;
                              edge_Ndxs:Map<node_ID*node_ID,int>;
                              borrow_Ndxs:Map<node_ID*node_ID,int>;
                              state_Ndxs:Map<node_ID,int list>;
                              state_e_Ndxs:Map<node_ID*node_ID,int list>
                              
                              unique_perms:VariableArray<bool>;
                              full_perms:VariableArray<bool>;
                              imm_perms:VariableArray<bool>;
                              share_perms:VariableArray<bool>;
                              pure_perms:VariableArray<bool>;
                              // permissions for edges
                              unique_e_perms:VariableArray<bool>;
                              full_e_perms:VariableArray<bool>;
                              imm_e_perms:VariableArray<bool>;
                              share_e_perms:VariableArray<bool>;
                              pure_e_perms:VariableArray<bool>;
                              // frame permission?
                              frame:VariableArray<bool>;
                              frame_e:VariableArray<bool>;
                              // borrowing constraint, independent of the above
                              borrow:VariableArray<bool>;
                              // state/dimension information
                              guarantee:VariableArray<bool>;
                              state:VariableArray<bool>;
                              // and for the edges
                              guarantee_e:VariableArray<bool>;
                              state_e:VariableArray<bool>;}

        // a data structure for finding state hierarchy information.
        // In addition to the state hierarchy, we can find the index into the state/guarantee
        // arrays (above) for a given hierarchy node.
        type hierarchy_Info = {hierarchy:state; array_ndxs:Map<state_Hierarchy_Node,int>}
        // from a type name, we can get that type's hierarchy info
        type hierarchy_Maps = Map<type_Name,hierarchy_Info>

        /// given the type_Name/state list that comes from a graph, returns the same information
        /// in hierarchy_Maps form.
        let mapsFromHierarchies (hierarchies:(type_Name*state) list) : hierarchy_Maps =
            let rec ndxHelperS (s:state) (result : (state_Hierarchy_Node*int) list) : (state_Hierarchy_Node*int) list =
                match s with
                    | SLeaf(s_) -> (s_,(List.length result))::result
                    | SNode(s_,dims) -> 
                        let result = List.fold (fun r d -> ndxHelperD d r) result dims in
                        (s_,(List.length result))::result
            and ndxHelperD (dim_name,fst_state,states) (result : (state_Hierarchy_Node*int) list) : (state_Hierarchy_Node*int) list =
               let result = ndxHelperS fst_state result in
               let result = List.fold (fun r s -> ndxHelperS s r) result states in
               (dim_name,(List.length result))::result
            in
            let infoHelper (s:state) : hierarchy_Info = 
                { hierarchy=s; array_ndxs=Map.ofList (ndxHelperS s List.empty) } 
            in
            // for each type name and state, create an info, add to map
            List.fold 
                (fun m (ty_name,st) -> 
                    let info = infoHelper st in
                    Map.add ty_name info m) 
                Map.empty hierarchies
        
        // TODO
        // All this code assumes that permissions will only be in one dimension, which is 
        // not going to be true eventually.
        // TODO

        // Given a kind return that kind's map, and the map of all the other kinds
        let kindToArray k (maps:node_Var_Maps) =
            match k with 
                | Unique -> (maps.unique_perms)
                | Full -> (maps.full_perms)
                | Immutable -> (maps.imm_perms)
                | Share -> (maps.share_perms)
                | Pure -> (maps.pure_perms)
        
        /// Given a kind, return the kind's edge map, and the edge maps of all other kinds.
        let kindToEdgeArray k (maps:node_Var_Maps) =
            match k with
                | Unique -> (maps.unique_e_perms)
                | Full -> (maps.full_e_perms)
                | Immutable -> (maps.imm_e_perms)
                | Share -> (maps.share_e_perms)
                | Pure -> (maps.pure_e_perms)

        // set guarantee priors for a node based on a concrete perm
        let setGuaranteeConcrete (cp:concrete_Perm_Element) node (hierarchies:hierarchy_Maps) probs =
            let hi = hierarchies.Item node.tyname in
            let guar_ndx = hi.array_ndxs.Item(cp.guarantee) in
            let node_count = hi.array_ndxs.Count in
            // an array where every observed value is false except the index that corresponds to the guarantee
            let high = Map.find "ConcreteGuarTrue" probs in
            let low  = Map.find "ConcreteGuarFalse" probs in
            [ for i in 0 .. node_count - 1 -> if i = guar_ndx then high else low ]
            

        // set state priors for a node based on a concrete perm
        let setStatesConcrete cp node (hierarchies:hierarchy_Maps) probs =
            let fst_state = List.head cp.states in
            let hi = hierarchies.Item node.tyname in
            let state_ndx = hi.array_ndxs.Item(fst_state) in
            let node_count = hi.array_ndxs.Count in
            // an array where every observed value is false except the index that corresponds to the state
            let high = Map.find "ConcreteStateTrue" probs in
            let low  = Map.find "ConcreteStateFalse" probs in
            [ for i in 0 .. node_count - 1 -> if i = state_ndx then high else low ] 
            

        // This node is in NO states, set priors accordingly...
        let setGuaranteeNone node (hierarchies:hierarchy_Maps) probs =
            let hi = hierarchies.Item node.tyname in
            let node_count = hi.array_ndxs.Count in
            // an array where every observed value is false except the index that corresponds to the state
            let prob = Map.find "NoState" probs in
            [ for i in 1 .. node_count  -> prob ]

        // This node is in NO states, set priors accordingly...
        let setStatesNone node (hierarchies:hierarchy_Maps) probs =
            let hi = hierarchies.Item node.tyname in
            let node_count = hi.array_ndxs.Count in
            // an array where every observed value is false except the index that corresponds to the state
            let prob = Map.find "NoState" probs in
            [ for i in 1 .. node_count  -> prob ]

        let setGuaranteeUnknown node (hierarchies:hierarchy_Maps) probs =
            let hi = hierarchies.Item node.tyname in
            let node_count = hi.array_ndxs.Count in
            let prob = Map.find "UnknownState" probs in
            [ for i in 1 .. node_count  -> prob ]

        let setStatesUnknown node (hierarchies:hierarchy_Maps) probs =
            let hi = hierarchies.Item node.tyname in
            let node_count = hi.array_ndxs.Count in
            let prob = Map.find "UnknownState" probs in
            [ for i in 1 .. node_count  -> prob ]

        /// Given a list of nodes, create variables for each of the five permission
        /// kinds for each node, for each edge, and bor the 'borrowing' variables.
        /// This sets the priors for each variable.
        // TODO: This method is WAY too long & complicated
        let createVariables nodes (constraints:nodal_Constraint list) (hierarchies:hierarchy_Maps) (probs) =
            let specifiedAsBorrowing (n1,n2) =
                let p1 = nodePermission n1 in
                let p2 = nodePermission n2 in
                match (p1,p2) with
                    | (ConcretePerm(cp1::[]),ConcretePerm(cp2::[])) -> Some(cp1.fract = cp2.fract)
                    | (ConcretePerm([]),ConcretePerm([])) ->   Some(false)
                    | (ConcretePerm(cp::[]),_) -> Some(false)
                    | (_,ConcretePerm(cp::[])) -> Some(false)
                    | (ConcretePerm(_),ConcretePerm(_)) -> raise NYI // not ready to deal with multi perms yet
                    | _ -> None
            in
            let borrowingPriors ns cs : Map<node_ID*node_ID,int> * VariableArray<bool> =
              // so we simultaneously calculate the size that the array should be
              // AND the priors for each array index and then assign them and return
              let (i,ndxs,priors) =
                List.fold
                    (fun (i,m,ps) c ->
                        let high = Map.find "BorrowTrue" probs in
                        let low = Map.find "BorrowFalse" probs in
                        let uk = Map.find "ContingentBorrowUK" probs in
                        match c with
                            | CannotBorrow(n1,n2) ->
                                let m = Map.add (n1.id,n2.id) i m in
                                let i = i+1 in
                                match specifiedAsBorrowing(n1,n2) with
                                    | None -> (i,m,low::ps)
                                    | Some(true) -> (i,m,high::ps)
                                    | Some(false) -> (i,m,low::ps)
                            | CanBorrow(n1,n2) ->
                                let m = Map.add (n1.id,n2.id) i m in
                                let i = i + 1 in
                                match specifiedAsBorrowing(n1,n2) with
                                    | None -> (i,m,high::ps)
                                    | Some(true) -> (i,m,high::ps)
                                    | Some(false) -> (i,m,low::ps)
                            | ContingentBorrow(n1,n2,_) ->
                                let m = Map.add (n1.id,n2.id) i m in
                                let i = i + 1 in
                                match specifiedAsBorrowing(n1,n2) with
                                    | None -> (i,m,uk::ps)
                                    | Some(true) -> (i,m,high::ps)
                                    | Some(false) -> (i,m,low::ps)
                            | _ -> (i,m,ps)) (0,Map.empty,[]) cs
              in
              let priors = List.rev priors in // gotta reverse b/c the foldl puts the priors out of order
              // and now to our same structures, we need to add the ARGUMENTS
              let (size,ndxs,priors') =
                List.fold
                    (fun (i,m,ps) n ->
                        let high = Map.find "BorrowTrue" probs in
                        let low = Map.find "BorrowFalse" probs in
                        let uk = Map.find "BorrowingUK" probs in
                        match n.specifics with
                            | StandardArg(mkey,_,Pre,site,pos,_) ->
                                let post = findArg Post mkey site pos nodes in
                                let m = Map.add (n.id,post.id) i m in
                                let i = i + 1 in
                                match specifiedAsBorrowing(n,post) with
                                    | None -> (i,m,uk::ps)
                                    | Some(true) -> (i,m,high::ps)
                                    | Some(false) -> (i,m,low::ps)
                            | CalledRcvr(mkey,_,Pre,site,_,_) ->
                                let post = findCalledRcvr Post mkey site nodes in
                                let m = Map.add (n.id,post.id) i m in
                                let i = i + 1 in
                                match specifiedAsBorrowing(n,post) with
                                    | None -> (i,m,uk::ps)
                                    | Some(true) -> (i,m,high::ps)
                                    | Some(false) -> (i,m,low::ps)
                            | _ -> (i,m,ps)) (i,ndxs,[]) ns
              in
              // create var array & assign priors
              let priors = priors @ (List.rev priors') in
              let range = new Range(size) in
              let b_priors = Variable.Array<Bernoulli>(range) in
              let result = Variable.Array<bool>(range) in
              let () = result.[range] <- Variable.Random<bool,Bernoulli>(b_priors.[range]) in
              let b_priors_ar = List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) priors) in
              let () = b_priors.ObservedValue <- b_priors_ar in
              (ndxs,result)
            in
            let setOfNodesWithConcreteSuccessors =
                List.fold (fun s c -> match c with ConcreteSuccessor(n) -> Set.add n.id s | _ -> s) Set.empty constraints 
            in
            let statePriors ns (hierarchies:hierarchy_Maps) =
                // this function is hard
                // go through all of the nodes, build up the priors and the index,
                // each one may contribute several array elements, one for each state defined
                let helper (i,m,s_priors,g_priors) n =
                    let hier = hierarchies.Item n.tyname in
                    let no_states = hier.array_ndxs.Count in
                    let final_ndx = i + no_states - 1 in
                    let range = [ for ndx in i..final_ndx -> ndx ] in
                    let i = final_ndx + 1 in
                    let m = Map.add n.id range m in
                    let (s_prep,g_prep) = 
                        match (nodePermission n) with
                            | ConcretePerm(cp::[]) -> 
                                // states
                                (setStatesConcrete cp n hierarchies probs,
                                 setGuaranteeConcrete cp n hierarchies probs)
                            | ConcretePerm([]) ->
                                // states
                                (setStatesNone n hierarchies probs,
                                 setGuaranteeNone n hierarchies probs)
                            | UnGroundPerm ->    
                                // states
                                (setStatesUnknown n hierarchies probs,
                                 setGuaranteeUnknown n hierarchies probs)
                            | _ -> raise NYI
                    in
                    // prep must be BACKWARDS since we're going to reverse the whole thing...
                    let (s_prep,g_prep) = (List.rev s_prep, List.rev g_prep) in
                    (i,m,s_prep @ s_priors,g_prep @ g_priors)
                in
                let (size,ndxs,s_priors,g_priors) = List.fold helper (0,Map.empty,[],[]) ns
                let (s_priors,g_priors) = (List.rev s_priors, List.rev g_priors) in
                // create arrays and assign priors
                let range = new Range(size) in
                let s_priors' = Variable.Array<Bernoulli>(range) in
                let g_priors' = Variable.Array<Bernoulli>(range) in
                let s_result = Variable.Array<bool>(range) in
                let g_result = Variable.Array<bool>(range) in
                let () = s_result.[range] <- Variable.Random<bool,Bernoulli>(s_priors'.[range]) in
                let () = g_result.[range] <- Variable.Random<bool,Bernoulli>(g_priors'.[range]) in
                let s_priors_ar = List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) s_priors) in
                let g_priors_ar = List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) g_priors) in
                let () = s_priors'.ObservedValue <- s_priors_ar in
                let () = g_priors'.ObservedValue <- g_priors_ar in
                // tmp - try to assign observed values to concrete specs, just like I did with full below
                let (_,true_is) = List.foldBack (fun s_pp (i,is) -> if s_pp > 0.9 then (i-1,i::is) else (i-1,is)) s_priors ((List.length s_priors)-1,[]) in
                let true_size = List.length true_is in
                let true_range = new Range(true_size) in
                let true_is = Array.ofList true_is in
                let true_is_ar = Variable.Constant<int>(true_is,true_range) in
                let states = Variable.GetItems(s_result,true_is_ar) in
                let () = states.ObservedValue <- Array.init true_size (fun _ -> true) in
                // tmp - do the same for guaranteed states...
                let (_,true_is) = List.foldBack (fun g_pp (i,is) -> if g_pp > 0.9 then (i-1,i::is) else (i-1,is)) g_priors ((List.length g_priors)-1,[]) in
                let true_size = List.length true_is in
                let true_range = new Range(true_size) in
                let true_is = Array.ofList true_is in
                let true_is_ar = Variable.Constant<int>(true_is,true_range) in
                let guarantees = Variable.GetItems(g_result,true_is_ar) in
                let () = guarantees.ObservedValue <- Array.init true_size (fun _ -> true) in
                // tmp - done
                (ndxs,s_result,g_result)
            in
            let stateEdgePriors ns (hierarchies:hierarchy_Maps) =
              // find number of edges
              // each edge gets a slot per state
              // build up array as well
              let edges = List.map (fun n1 -> List.map (fun n2 -> (n1,n2)) (!n1.succs)) ns in
              let edges = List.concat edges in
              let helper (i,m,priors) e =
                let (n1,n2) = e in
                let hier = hierarchies.Item n1.tyname in
                let no_states = hier.array_ndxs.Count in
                let final_ndx = i + no_states - 1 in
                let uk = Map.find "UnknownState" probs in
                let prep = [ for _ in 1..no_states -> uk ] in
                let range = [ for ndx in i..final_ndx -> ndx ] in
                let i = final_ndx + 1 in
                let m = Map.add (n1.id,n2.id) range m in
                (i,m, prep @ priors)
              in
              let (size,ndxs,priors) = List.fold helper (0,Map.empty,[]) edges in
              // create var array & assign priors
              let range = new Range(size) in
              let priors' = Variable.Array<Bernoulli>(range) in
              let s_result = Variable.Array<bool>(range) in
              let g_result = Variable.Array<bool>(range) in
              let () = s_result.[range] <- Variable.Random<bool,Bernoulli>(priors'.[range]) in
              let () = g_result.[range] <- Variable.Random<bool,Bernoulli>(priors'.[range]) in
              let priors_ar = List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) priors) in
              let () = priors'.ObservedValue <- priors_ar in
              (ndxs,s_result,g_result)
            in
            let framePriorProbs =
              let isArg n = match n.specifics with | StandardArg(_,_,_,_,_,_) -> true | _ -> false in
              List.map
                  (fun n ->
                      let high = Map.find "FrameTrue" probs in
                      let low = Map.find "FrameNone" probs in
                      let uk = Map.find "FrameUK" probs in
                      let virt = Map.find "VirtualTrue" probs in
                      let isarg = Map.find "FrameArg" probs in
                      match (nodePermission n) with
                          | ConcretePerm(cp::[]) ->
                              match cp.usage with
                                  | Frame -> if isArg n then isarg else high
                                  | Virtual -> virt
                                  | Both -> raise NYI
                          | ConcretePerm([]) -> low
                          | UnGroundPerm -> uk
                          | _ -> raise NYI)
            in
            // generate prior probabilities for each node given
            // what we're doing is creating a five tuple, each a list of doubles representing
            // the probability of each permission. The fold is to make sure we have tuples of
            // lists rather than lists of tuples
            let nodePriorProbs ns =
              let h (us,fs,is,ss,ps) n =
                match (nodePermission n) with
                    | ConcretePerm(cp::[]) -> 
                        // If the permission was annotated, set the prior for that kind to high
                        // since it is likely true, and set the rest to a very low value.
                        let high = Map.find "PermTrue" probs in
                        let low = Map.find "PermFalse" probs in
                        match cp.kind with // basically, just put high in the selected kind & low in the others
                            | Unique -> (high::us,low::fs,low::is,low::ss,low::ps)
                            | Full -> (low::us,high::fs,low::is,low::ss,low::ps)
                            | Immutable -> (low::us,low::fs,high::is,low::ss,low::ps)
                            | Share -> (low::us,low::fs,low::is,high::ss,low::ps)
                            | Pure -> (low::us,low::fs,low::is,low::ss,high::ps)
                    | ConcretePerm([]) ->
                        // give all perms a low value: This is the no-permission annotation (uncommon)
                        let low = Map.find "PermFalse" probs in
                        (low::us,low::fs,low::is,low::ss,low::ps)
                    | UnGroundPerm ->    
                        // Maybe it's on the PATH of a concrete perm. We added this recently because we were getting 
                        // low values for everything 5/25/10
                        if Set.contains n.id setOfNodesWithConcreteSuccessors 
                        then
                            // okay, we are trying something out, making pure less likely:
                            let full = Map.find "PermUGCSFull" probs in
                            let sunique = Map.find "PermUGCSUniqueShare" probs in
                            let impure = Map.find "PermUGCSImmPure" probs in
                            (sunique::us,full::fs,impure::is,sunique::ss,impure::ps)
                        else
                            // Everything has a 50/50 probability in this approach...
                            // Why not bump it down if it can't?
                            let half = Map.find "PermUGHigh" probs in
                            let low = Map.find "PermUGLow" probs in
                            (half::us,half::fs,low::is,half::ss,low::ps)
                    | _ -> raise NYI
              in
              // reverse so order matches original
              let (us,fs,is,ss,ps) = List.fold h ([],[],[],[],[]) ns in
              (List.rev us,List.rev fs,List.rev is,List.rev ss, List.rev ps)
            in
            // create arrays
            let node_count = List.length nodes in
            let all_node_range = new Range(node_count) in
            // for each node, create the mapping from node id to index in array
            let (_,node_ndxs) = List.fold (fun (i,m) n -> (i+1,Map.add n.id i m)) (0,Map.empty) nodes in
            let edges = List.collect (fun n_1 -> List.map (fun n_2 -> (n_1.id,n_2.id)) (!n_1.succs)) nodes in
            let edge_count = List.length edges in 
            let edge_range = new Range(edge_count) in
            let (_,edge_ndxs) = List.fold (fun (i,m) e -> (i+1, Map.add e i m)) (0,Map.empty) edges in
            let (borrow_ndxs,borrow_array) = borrowingPriors nodes constraints in
            let (state_ndxs,state_array,guarantee_array) = statePriors nodes hierarchies in
            let (state_e_ndxs,state_array_e,guarantee_array_e) = stateEdgePriors nodes hierarchies in
            let maps =  {node_Ndxs=node_ndxs;
                         edge_Ndxs=edge_ndxs;
                         borrow_Ndxs=borrow_ndxs;
                         state_Ndxs=state_ndxs;
                         state_e_Ndxs=state_e_ndxs;
                         // now, create each new array                         
                         unique_perms=Variable.Array<bool>(all_node_range);
                         full_perms=Variable.Array<bool>(all_node_range);
                         imm_perms=Variable.Array<bool>(all_node_range);
                         share_perms=Variable.Array<bool>(all_node_range);
                         pure_perms=Variable.Array<bool>(all_node_range);
                         // edges...
                         unique_e_perms=Variable.Array<bool>(edge_range);
                         full_e_perms=Variable.Array<bool>(edge_range);
                         imm_e_perms=Variable.Array<bool>(edge_range);
                         share_e_perms=Variable.Array<bool>(edge_range);
                         pure_e_perms=Variable.Array<bool>(edge_range);
                         // frame
                         frame=Variable.Array<bool>(all_node_range);
                         frame_e=Variable.Array<bool>(edge_range);
                         // Others...
                         borrow=borrow_array;
                         guarantee=guarantee_array;
                         guarantee_e=guarantee_array_e;
                         state=state_array;
                         state_e=state_array_e}
            in
            // apply prior probabilities for each node/perm kind
            let (u_pps,f_pps,i_pps,s_pps,p_pps) = nodePriorProbs nodes in
            // UNIQUE
            let u_priors = Variable.Array<Bernoulli>(all_node_range) in
            let () = maps.unique_perms.[all_node_range] <- Variable.Random<bool,Bernoulli>(u_priors.[all_node_range]) in
            let u_priors_ar = (List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) u_pps)) in
            let () = u_priors.ObservedValue <- u_priors_ar in
            // FULL
            let f_priors = Variable.Array<Bernoulli>(all_node_range) in
            let () = maps.full_perms.[all_node_range] <- Variable.Random<bool,Bernoulli>(f_priors.[all_node_range]) in
            let f_priors_ar = (List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) f_pps)) in
            let () = f_priors.ObservedValue <- f_priors_ar in
            // Tmp - try to assign observed values to the concrete specs?
            // First, find indicies of priors greater than 8
            let (_,true_is) = List.foldBack (fun f_pp (i,is) -> if f_pp > 0.8 then (i-1,i::is) else (i-1,is)) f_pps ((List.length f_pps)-1,[]) in
            let true_size = List.length true_is in
            let true_range = new Range(true_size) in
            let true_is = Array.ofList true_is in
            let true_is_ar = Variable.Constant<int>(true_is,true_range) in
            let fulls = Variable.GetItems(maps.full_perms,true_is_ar) in
            let () = fulls.ObservedValue <- Array.init true_size (fun _ -> true) in
            // tmp - done            
            // IMMUTABLE
            let i_priors = Variable.Array<Bernoulli>(all_node_range) in
            let () = maps.imm_perms.[all_node_range] <- Variable.Random<bool,Bernoulli>(i_priors.[all_node_range]) in
            let i_priors_ar = (List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) i_pps)) in
            let () = i_priors.ObservedValue <- i_priors_ar in
            // SHARE
            let s_priors = Variable.Array<Bernoulli>(all_node_range) in
            let () = maps.share_perms.[all_node_range] <- Variable.Random<bool,Bernoulli>(s_priors.[all_node_range]) in
            let s_priors_ar = (List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) s_pps)) in
            let () = s_priors.ObservedValue <- s_priors_ar in
            // PURE
            let p_priors = Variable.Array<Bernoulli>(all_node_range) in
            let () = maps.pure_perms.[all_node_range] <- Variable.Random<bool,Bernoulli>(p_priors.[all_node_range]) in
            let p_priors_ar = (List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) p_pps)) in
            let () = p_priors.ObservedValue <- p_priors_ar in
            // edges all have the same prior, 50/50
            let e_priors = Variable.Array<Bernoulli>(edge_range) in
            // except the low ones!
            let e_priors_low = Variable.Array<Bernoulli>(edge_range) in
            let () = maps.unique_e_perms.[edge_range] <- Variable.Random<bool,Bernoulli>(e_priors.[edge_range]) in
            let () = maps.full_e_perms.[edge_range] <- Variable.Random<bool,Bernoulli>(e_priors.[edge_range]) in
            let () = maps.imm_e_perms.[edge_range] <- Variable.Random<bool,Bernoulli>(e_priors_low.[edge_range]) in
            let () = maps.share_e_perms.[edge_range] <- Variable.Random<bool,Bernoulli>(e_priors.[edge_range]) in
            let () = maps.pure_e_perms.[edge_range] <- Variable.Random<bool,Bernoulli>(e_priors_low.[edge_range]) in
            let bern = new Bernoulli(Map.find "PermUGHigh" probs) in
            let () = e_priors.ObservedValue <- Array.init edge_count (fun _ -> bern) in
            let bern = new Bernoulli(Map.find "PermUGLow" probs) in
            let () = e_priors_low.ObservedValue <- Array.init edge_count (fun _ -> bern) in
            // frames
            let fr_pps = framePriorProbs nodes in
            let fr_priors = Variable.Array<Bernoulli>(all_node_range) in
            let () = maps.frame.[all_node_range] <- Variable.Random<bool,Bernoulli>(fr_priors.[all_node_range]) in
            let fr_priors_ar = List.toArray (List.map (fun (d:float) -> new Bernoulli(d)) fr_pps) in
            let () = fr_priors.ObservedValue <- fr_priors_ar in
            // frame edges all have the same prior, 4/10
            let fr_e_priors = Variable.Array<Bernoulli>(edge_range) in
            let () = maps.frame_e.[edge_range] <- Variable.Random<bool,Bernoulli>(fr_e_priors.[edge_range]) in
            let bern = new Bernoulli(Map.find "FrameEdge" probs) in
            let () = fr_e_priors.ObservedValue <- Array.init edge_count (fun _ -> bern) in
            // apply all other priors (states, borrowing, etc.)
            maps


             
        // Returns a variable whose truth indicates that each of the components for
        // the two given nodes are equal.
 //       let varNodesEqual n1 n2 maps : Variable<bool> list =
 //           let (v1u,v1f,v1i,v1s,v1p) = mapsForNode n1.id maps in
 //           let (v2u,v2f,v2i,v2s,v2p) = mapsForNode n2.id maps in
 //           [(Variable.op_Equality (v1u,v2u));
 //            (Variable.op_Equality (v1f,v2f));
 //            (Variable.op_Equality (v1i,v2i));
 //            (Variable.op_Equality (v1s,v2s));
 //            (Variable.op_Equality (v1p,v2p))]
 //
 //
 //       /// Return a variable whose truth indicates that the given node perm
 //       /// components should be equal to the given edge perm components.
 //       let varNodeEqualsEdge node_id (edge:node_ID*node_ID) maps : Variable<bool> list =
 //           let (v1u,v1f,v1i,v1s,v1p) = mapsForNode node_id maps in
 //           let (veu,vef,vei,ves,vep) = mapsForEdge edge maps in
 //           [(Variable.op_Equality (v1u,veu));
 //            (Variable.op_Equality (v1f,vef));
 //            (Variable.op_Equality (v1i,vei));
 //            (Variable.op_Equality (v1s,ves));
 //            (Variable.op_Equality (v1p,vep))]
 //
 //       /// modifies the probabilistic constraints to indicate that
 //       /// n1 and n2 should have equal permission.
 //       let equalNodes n1 n2 maps (likelihood:Variable<Bernoulli>) : unit =
 //           /// Force the two variables to be equal
 //           let vs = (varNodesEqual n1 n2 maps) in
 //           List.iter (fun (v:Variable<bool>) -> Variable.ConstrainEqualRandom(v, likelihood)) vs

        /// Synchronized nodes probably do not use unique or immutable permission
        let synchronizedConstraints ns maps (_:Bernoulli) (likelihood_low:Bernoulli) : unit =
            let range = new Range(List.length ns) in
            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in
            let n_ndx_array = Variable.Constant<int>(List.toArray n_ndxs,range) in
            // big OR of all the other permission kinds
            let uniques = Variable.GetItems(maps.unique_perms, n_ndx_array) in
            let imms = Variable.GetItems(maps.imm_perms, n_ndx_array) in
            // constrain
            let c_array = Variable.Array<bool>(range) in
            let () = c_array.[range] <- uniques.[range] ||| imms.[range] in
            let () = Variable.ConstrainEqualRandom( c_array.[range], likelihood_low ) in
            ()

        /// Getters are likely to need reading permissions for their receivers
        let getterConstraints ns maps (_:Bernoulli) (likelihood_low:Bernoulli) : unit =
            let range = new Range(List.length ns) in
            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in
            let n_ndx_array = Variable.Constant<int>(List.toArray n_ndxs,range) in
            // big OR of all the other permission kinds
            let uniques = Variable.GetItems(maps.unique_perms, n_ndx_array) in
            let fulls = Variable.GetItems(maps.full_perms, n_ndx_array) in
            let shares = Variable.GetItems(maps.share_perms, n_ndx_array) in
            // constrain
            let c_array = Variable.Array<bool>(range) in
            let () = c_array.[range] <- fulls.[range] ||| uniques.[range] ||| shares.[range] in
            let () = Variable.ConstrainEqualRandom( c_array.[range], likelihood_low ) in
            ()

        /// Setters are likely to need reading permissions for their receivers
        let setterConstraints ns maps (_:Bernoulli) (likelihood_low:Bernoulli) : unit =
            let range = new Range(List.length ns) in
            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in
            let n_ndx_array = Variable.Constant<int>(List.toArray n_ndxs,range) in
            // big OR of all the other permission kinds
            let pures = Variable.GetItems(maps.pure_perms, n_ndx_array) in
            let imms = Variable.GetItems(maps.imm_perms, n_ndx_array) in
            // constrain
            let c_array = Variable.Array<bool>(range) in
            let () = c_array.[range] <- pures.[range] ||| imms.[range] in
            let () = Variable.ConstrainEqualRandom( c_array.[range], likelihood_low ) in
            ()

        /// Take the list of nodes, and make all permissions other than unique of lower likelihood
        let createMethodUniqueConstraints (ns) (maps) (_:Bernoulli) (likelihood_low:Bernoulli) : unit =
            let range = new Range(List.length ns) in
            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in
            let n_ndx_array = Variable.Constant<int>(List.toArray n_ndxs,range) in
            // big OR of all the other permission kinds
            let fulls = Variable.GetItems(maps.full_perms, n_ndx_array) in
            let imms = Variable.GetItems(maps.imm_perms, n_ndx_array) in
            let shares = Variable.GetItems(maps.share_perms, n_ndx_array) in
            let pures = Variable.GetItems(maps.pure_perms, n_ndx_array) in
            // constrain
            let c_array = Variable.Array<bool>(range) in
            let () = c_array.[range] <- fulls.[range] ||| imms.[range] ||| shares.[range] ||| pures.[range] in
            let () = Variable.ConstrainEqualRandom( c_array.[range], likelihood_low ) in
            ()

        /// Constructors generally return unique permission.
        let constructorConstraints ns maps (l:Bernoulli) (likelihood_low:Bernoulli) : unit =
            createMethodUniqueConstraints ns maps l likelihood_low 

        /// Take the given list of node pairs that have equal permission, generate an array
        /// for the new constraint, and set the arrays to be equal
        let equalNodesConstraints (n_1s,n_2s) maps (likelihood:Bernoulli) : unit =
            let range = new Range(List.length n_1s) in
            let n_1ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) n_1s in
            let n_2ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) n_2s in
            let n_1ndx_array = Variable.Constant<int>(List.toArray n_1ndxs,range) in
            let n_2ndx_array = Variable.Constant<int>(List.toArray n_2ndxs,range) in
//            let const_array = Array.init (List.length n_1s) (fun _ -> likelihood) in
//            let const_array:VariableArray<Bernoulli> = Variable.Constant<Bernoulli>(const_array,range) in
            // for each permission kind, get the subarrays for the nodes, and create a
            // output constraint array
            for k in allPerms do
                let k_perms = kindToArray k maps in
                let n_1s_k = Variable.GetItems(k_perms, n_1ndx_array) in
                let n_2s_k = Variable.GetItems(k_perms, n_2ndx_array) in
                let n_eq_k = Variable.Array<bool>(range) in
                let () = n_eq_k.[range] <- Variable.op_Equality( n_1s_k.[range], n_2s_k.[range] ) in
                let () = Variable.ConstrainEqualRandom( n_eq_k.[range], likelihood ) in
                ()
            
//            let n_1s_u = Variable.GetItems(maps.unique_perms, n_1ndx_array) in
//            let n_2s_u = Variable.GetItems(maps.unique_perms, n_2ndx_array) in
//            let n_eq_u = Variable.Array<bool>(equal_range) in
//            let () = n_eq_u.set_Item(equal_range, Variable.op_Equality(n_1s_u.get_Item(equal_range),n_2s_u.get_Item(equal_range))) in
//            let () = Variable.ConstrainEqualRandom(n_eq_u.get_Item(equal_range), const_array.get_Item(equal_range)) in
//            // full...
//            let n_1s_f = Variable.GetItems(maps.full_perms, n_1ndx_array) in
//            let n_2s_f = Variable.GetItems(maps.full_perms, n_2ndx_array) in
//            let n_eq_f = Variable.Array<bool>(equal_range) in
//            let () = n_eq_f.set_Item(equal_range, Variable.op_Equality(n_1s_f.get_Item(equal_range),n_2s_f.get_Item(equal_range))) in
//            let () = Variable.ConstrainEqualRandom(n_eq_f.get_Item(equal_range), const_array.get_Item(equal_range)) in
//            // imm
//            let n_1s_i = Variable.GetItems(maps.imm_perms, n_1ndx_array) in
//            let n_2s_i = Variable.GetItems(maps.imm_perms, n_2ndx_array) in
//            let n_eq_i = Variable.Array<bool>(equal_range) in
//            let () = n_eq_i.set_Item(equal_range, Variable.op_Equality(n_1s_i.get_Item(equal_range),n_2s_i.get_Item(equal_range))) in
//            let () = Variable.ConstrainEqualRandom(n_eq_i.get_Item(equal_range), const_array.get_Item(equal_range)) in
//            // share
//            let n_1s_s = Variable.GetItems(maps.share_perms, n_1ndx_array) in
//            let n_2s_s = Variable.GetItems(maps.share_perms, n_2ndx_array) in
//            let n_eq_s = Variable.Array<bool>(equal_range) in
//            let () = n_eq_s.set_Item(equal_range, Variable.op_Equality(n_1s_s.get_Item(equal_range),n_2s_s.get_Item(equal_range))) in
//            let () = Variable.ConstrainEqualRandom(n_eq_s.get_Item(equal_range), const_array.get_Item(equal_range)) in
//            // pure
//            let n_1s_p = Variable.GetItems(maps.pure_perms, n_1ndx_array) in
//            let n_2s_p = Variable.GetItems(maps.pure_perms, n_2ndx_array) in
//            let n_eq_p = Variable.Array<bool>(equal_range) in
//            let () = n_eq_p.set_Item(equal_range, Variable.op_Equality(n_1s_p.get_Item(equal_range),n_2s_p.get_Item(equal_range))) in
//            let () = Variable.ConstrainEqualRandom(n_eq_p.get_Item(equal_range), const_array.get_Item(equal_range)) in
//            ()


        /// return a variable that represents the formula,
        /// v1 -> v2,
        /// using classical logic that gives us
        /// not(v1) or v2
        let implication v1 v2 = 
            //(v1 &&& v2) ||| (Variable.op_LogicalNot v1)
            (Variable.op_LogicalNot v1) ||| v2

        
        
        /// Modifies the constraint for the given node, making it likely that
        /// it's a writer. This can be done with some amout of certainty.
        let mustWrite ns maps (likely_high:Bernoulli) (likely_low:Bernoulli) : unit =
            // for all of the ns, increase their u/f/s likelihood
            // decrease their i/p likelihood
            let range = new Range(List.length ns) in
            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in
            let ndx_array = Variable.Constant<int>(List.toArray n_ndxs,range) in
            // u/f/s
            let uniques = Variable.GetItems(maps.unique_perms, ndx_array) in
            let fulls = Variable.GetItems(maps.full_perms, ndx_array) in
            let shares = Variable.GetItems(maps.share_perms, ndx_array) in
            // p/i
            let imms = Variable.GetItems(maps.imm_perms, ndx_array) in
            let pures = Variable.GetItems(maps.pure_perms, ndx_array) in

            let writes_likely = Variable.Array<bool>(range) in
            let reads_unlikely = Variable.Array<bool>(range) in 
            let () = writes_likely.[range] <- uniques.[range] ||| fulls.[range] ||| shares.[range] in
            let () = reads_unlikely.[range] <- pures.[range] ||| imms.[range] in
            // set likely & unlikely
//            let likely_constants = Array.init (List.length ns) (fun _ -> likely_high) in
//            let likely_constants = Variable.Constant<Bernoulli>(likely_constants,range) in
//            let unlikely_constants = Array.init (List.length ns) (fun _ -> likely_low) in
//            let unlikely_constants = Variable.Constant<Bernoulli>(unlikely_constants,range) in
            let () = Variable.ConstrainEqualRandom(writes_likely.[range], likely_high ) in
            let () = Variable.ConstrainEqualRandom(reads_unlikely.[range], likely_low ) in
            ()
                    
        /// Modifies the constraint for the given node, making it likely that it's
        /// a reader (although not too likely, since writers can also be readers)
        let mustRead ns maps (likelihood:Bernoulli) : unit =
            let range = new Range(List.length ns) in
            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in
            let ndx_array = Variable.Constant<int>(List.toArray n_ndxs,range) in
            let uniques = Variable.GetItems(maps.unique_perms, ndx_array) in
            let fulls = Variable.GetItems(maps.full_perms, ndx_array) in
            let imms = Variable.GetItems(maps.imm_perms, ndx_array) in
            let shares = Variable.GetItems(maps.share_perms, ndx_array) in
            let pures = Variable.GetItems(maps.pure_perms, ndx_array) in

            let reads_likely = Variable.Array<bool>(range) in
            let () =
                reads_likely.[range] <- uniques.[range] ||| fulls.[range] ||| imms.[range] ||| shares.[range] ||| pures.[range]
            in
            let () = Variable.ConstrainEqualRandom( reads_likely.[range], likelihood ) in
            ()

        /// The given nodes all need frame permission with high prob.
        let needsFrame ns maps (likelihood:Bernoulli) : unit =
            let range = new Range(List.length ns) in
            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in
            let ndx_array = Variable.Constant<int>(List.toArray n_ndxs,range) in
            let frames = Variable.GetItems(maps.frame, ndx_array) in
            let () = Variable.ConstrainEqualRandom( frames.[range], likelihood ) in
            ()

        /// The given nodes & edges should have the same likelihood of needing a
        /// frame perm with high prob.
        let framesSameEdge nodes_and_edges maps (likelihood:Bernoulli) =
            let range = new Range(List.length nodes_and_edges) in
            let (ns,edges) = List.unzip nodes_and_edges in
            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in
            let e_ndxs = List.map (fun (n1,n2) -> Map.find (n1,n2) maps.edge_Ndxs) edges in
            let n_ndx_array = Variable.Constant<int>(List.toArray n_ndxs,range) in
            let e_ndx_array = Variable.Constant<int>(List.toArray e_ndxs,range) in
            let nodes = Variable.GetItems(maps.frame, n_ndx_array) in
            let edges = Variable.GetItems(maps.frame_e, e_ndx_array) in
            let frames_same_edge = Variable.Array<bool>(range) in
            let () = frames_same_edge.[range] <- Variable.op_Equality(nodes.[range], edges.[range]) in
            let () = Variable.ConstrainEqualRandom( frames_same_edge.[range], likelihood ) in
            ()            


                
        /// links the likelihood of borrowing of the pre/post to the likelihood of
        /// borrowing of the args in the list.
        let contingentBorrow cs maps (likelihood:Bernoulli) =
            // For now, we just do each one on a variable, b/c we
            // can't figure out how to do it otherwise.
            for (n1,n2,pairs) in cs do
                // get indices
                let pair_ndxs = List.map (fun (p1,p2) -> Map.find (p1.id,p2.id) maps.borrow_Ndxs) pairs in
                let length = List.length pairs in
                let range = new Range(length) in
                let pair_ndxs = Variable.Constant<int>(List.toArray pair_ndxs, range) in
                let pairs = Variable.GetItems(maps.borrow, pair_ndxs) in
                let v_all = Variable.AllTrue(pairs) in
                // we know that v_all is equal to borrow[n] with high prob.
                let single_range = new Range(1) in
                let cur_borrow_ndx = Variable.Constant<int>([| (Map.find (n1.id,n2.id) maps.borrow_Ndxs) |], single_range) in
                let n_ar = Variable.GetItems(maps.borrow, cur_borrow_ndx) in // an array of length 1!
                let constr = Variable.Array<bool>(single_range) in
                let () = constr.[single_range] <- Variable.op_Equality(n_ar.[single_range], v_all) in
                let () = Variable.ConstrainEqualRandom( constr.[single_range], likelihood ) in
                ()
                
            
//            // AND of all args, IMPLIES pre/post borrow
//            let borrow_lookup n1 n2 = maps.borrow.Item(n1.id,n2.id) in
//            let and_of_args = 
//                let (fst_pre,fst_post) = List.head args in
//                let fst = borrow_lookup fst_pre fst_post in
//                List.fold (fun v (apre,apost) -> v &&& (borrow_lookup apre apost)) fst (List.tail args)
//            in
//            let impl = implication and_of_args (borrow_lookup pre post) in
//            Variable.ConstrainEqualRandom(impl, likelihood)


        let argIsParamGuarantee args_and_params (range:Range) maps (likelihood:Bernoulli) =
            // PRE guarantees
            let ndxs =
                (List.map
                     (fun (apre,_,ppre,_,_) ->
                          (Map.find apre.id maps.state_Ndxs,Map.find ppre.id maps.state_Ndxs)) args_and_params)
            in
            let (apre_g_ndxs,ppre_g_ndxs) = List.unzip ndxs in
            let (apre_g_ndxs,ppre_g_ndxs) = (List.concat apre_g_ndxs,List.concat ppre_g_ndxs) in
            let apre_ndxs_array = Variable.Constant<int>(List.toArray apre_g_ndxs, range) in
            let ppre_ndxs_array = Variable.Constant<int>(List.toArray ppre_g_ndxs, range) in
            let apres = Variable.GetItems(maps.guarantee, apre_ndxs_array) in
            let ppres = Variable.GetItems(maps.guarantee, ppre_ndxs_array) in
            let pres_eq = Variable.Array<bool>(range) in
            let () = pres_eq.[range] <- Variable.op_Equality(apres.[range], ppres.[range]) in
            let () = Variable.ConstrainEqualRandom( pres_eq.[range], likelihood ) in
            // POST guarantees
            let ndxs =
                (List.map
                     (fun (_,apost,_,ppost,_) ->
                          (Map.find apost.id maps.state_Ndxs,Map.find ppost.id maps.state_Ndxs)) args_and_params)
            in
            let (apost_g_ndxs,ppost_g_ndxs) = List.unzip ndxs in
            let (apost_g_ndxs,ppost_g_ndxs) = (List.concat apost_g_ndxs,List.concat ppost_g_ndxs) in
            let apost_ndxs_array = Variable.Constant<int>(List.toArray apost_g_ndxs, range) in
            let ppost_ndxs_array = Variable.Constant<int>(List.toArray ppost_g_ndxs, range) in
            let aposts = Variable.GetItems(maps.guarantee, apost_ndxs_array) in
            let pposts = Variable.GetItems(maps.guarantee, ppost_ndxs_array) in
            let posts_eq = Variable.Array<bool>(range) in
            let () = posts_eq.[range] <- Variable.op_Equality(aposts.[range], pposts.[range]) in
            let () = Variable.ConstrainEqualRandom( posts_eq.[range], likelihood ) in 
            ()

        let unzipX5 l = List.foldBack (fun (a,b,c,d,e) (a_s,bs,cs,ds,es) -> (a::a_s,b::bs,c::cs,d::ds,e::es) ) l ([],[],[],[],[])

        // For all private methods, arg frame = arg param 
        let privateFramesSame args_and_params maps (likelihood:Bernoulli) =
            // forget about the not private methods
            let args_and_params =
                List.filter (fun (_,_,_,_,ip) -> ip) args_and_params
            in
            // now the rest of the method is pretty similar to argIsParam
            let range = new Range(List.length args_and_params) in
            // PRE frames
            let pre_ndxs =
                List.map
                    (fun (apre,_,ppre,_,_) ->
                         (Map.find apre.id maps.node_Ndxs,
                          Map.find ppre.id maps.node_Ndxs)) args_and_params
            in
            // POST frames
            let post_ndxs =
                List.map
                    (fun (_,apost,_,ppost,_) ->
                         (Map.find apost.id maps.node_Ndxs,
                          Map.find ppost.id maps.node_Ndxs)) args_and_params
            in
            for ndxs in [pre_ndxs;post_ndxs] do
                let (a_ndxs,p_ndxs) = List.unzip ndxs in
                let a_ndxs_array = Variable.Constant<int>(List.toArray a_ndxs,range) in
                let p_ndxs_array = Variable.Constant<int>(List.toArray p_ndxs,range) in
                let args = Variable.GetItems(maps.frame, a_ndxs_array) in
                let params_ = Variable.GetItems(maps.frame, p_ndxs_array) in
                let frame_eq = Variable.Array<bool>(range) in
                let () = frame_eq.[range] <- Variable.op_Equality(args.[range], params_.[range]) in
                let () = Variable.ConstrainEqualRandom( frame_eq.[range], likelihood ) in
                ()

        /// Links the permissions at arg pre/post with the param pre/post AND sets
        /// their borrowing as likely equal.
        let argIsParam args_and_params maps (likelihood:Bernoulli) =
            let (apres,aposts,ppres,pposts,_) = unzipX5 args_and_params in
            // argpre & param pre are likely to be equal
            let () = equalNodesConstraints (apres,ppres) maps likelihood in
            // argpost & param post are likely to be equal
            let () = equalNodesConstraints (aposts,pposts) maps likelihood in
            // frames for private method calls
            let () = privateFramesSame args_and_params maps (likelihood:Bernoulli) in
            // borrowing
            let range = new Range(List.length args_and_params) in
            let ndxs =
                List.map
                    (fun (apre,apost,ppre,ppost,_) ->
                         (Map.find (apre.id,apost.id) maps.borrow_Ndxs,
                          Map.find (ppre.id,ppost.id) maps.borrow_Ndxs)) args_and_params
            in
            let (a_ndxs,p_ndxs) = List.unzip ndxs in
            let a_ndxs_array = Variable.Constant<int>(List.toArray a_ndxs,range) in
            let p_ndxs_array = Variable.Constant<int>(List.toArray p_ndxs,range) in
            let args = Variable.GetItems(maps.borrow, a_ndxs_array) in
            let params_ = Variable.GetItems(maps.borrow, p_ndxs_array) in
            let borrowing_eq = Variable.Array<bool>(range) in
            let () = borrowing_eq.[range] <- Variable.op_Equality(args.[range], params_.[range]) in
            let () = Variable.ConstrainEqualRandom( borrowing_eq.[range], likelihood ) in
            // End borrowing
            // now states & guaranteeeees!
            // PRE states
            let ndxs =
                (List.map
                     (fun (apre,_,ppre,_,_) ->
                          (Map.find apre.id maps.state_Ndxs,Map.find ppre.id maps.state_Ndxs)) args_and_params)
            in
            let (apre_s_ndxs,ppre_s_ndxs) = List.unzip ndxs in
            let (apre_s_ndxs,ppre_s_ndxs) = (List.concat apre_s_ndxs,List.concat ppre_s_ndxs) in
            let range = new Range(List.length apre_s_ndxs) in
            let apre_ndxs_array = Variable.Constant<int>(List.toArray apre_s_ndxs, range) in
            let ppre_ndxs_array = Variable.Constant<int>(List.toArray ppre_s_ndxs, range) in
            let apres = Variable.GetItems(maps.state, apre_ndxs_array) in
            let ppres = Variable.GetItems(maps.state, ppre_ndxs_array) in
            let pres_eq = Variable.Array<bool>(range) in
            let () = pres_eq.[range] <- Variable.op_Equality(apres.[range], ppres.[range]) in
            let () = Variable.ConstrainEqualRandom( pres_eq.[range], likelihood ) in
            // POST states
            // no new range, b/c we think all the remaining arrays will have to be of the same type.
            let ndxs =
                (List.map
                     (fun (_,apost,_,ppost,_) ->
                          (Map.find apost.id maps.state_Ndxs,Map.find ppost.id maps.state_Ndxs)) args_and_params)
            in
            let (apost_s_ndxs,ppost_s_ndxs) = List.unzip ndxs in
            let (apost_s_ndxs,ppost_s_ndxs) = (List.concat apost_s_ndxs,List.concat ppost_s_ndxs) in
            let apost_ndxs_array = Variable.Constant<int>(List.toArray apost_s_ndxs, range) in
            let ppost_ndxs_array = Variable.Constant<int>(List.toArray ppost_s_ndxs, range) in
            let aposts = Variable.GetItems(maps.state, apost_ndxs_array) in
            let pposts = Variable.GetItems(maps.state, ppost_ndxs_array) in
            let posts_eq = Variable.Array<bool>(range) in
            let () = posts_eq.[range] <- Variable.op_Equality(aposts.[range], pposts.[range]) in
            let () = Variable.ConstrainEqualRandom( posts_eq.[range], likelihood ) in
            // guarantees
            let () = argIsParamGuarantee args_and_params range maps likelihood in
            // end states & guarantees
            ()



        /// A method split generates the probabilistic constraints for method call sites.
        /// The constraint is, IF the arg is borrowed, the merge=split. IF the arg is not
        /// borrowed, then the merge=(split|post)
        let methodSplits method_splits maps (likelihood:Bernoulli) : unit =
//        n_split n_pre n_post n_merge maps 
            ();
            // this will reverse the order, don't think it matters
            let (n_splits,n_merges,split_edges,post_edges,borrows) =
                List.foldBack
                    (fun (ns,npr,npo,nm) (nss,nms,ses,pes,bs) ->
                       (ns::nss,nm::nms,(ns.id,nm.id)::ses,(npo.id,nm.id)::pes,(npr.id,npo.id)::bs))
                    method_splits ([],[],[],[],[])
            in
            // get indexs
            let splits_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) n_splits in
            let merges_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) n_merges in
            let split_e_ndxs = List.map (fun e -> Map.find e maps.edge_Ndxs) split_edges in
            let post_e_ndxs = List.map (fun e -> Map.find e maps.edge_Ndxs) post_edges in
            let borrow_ndxs = List.map (fun b -> Map.find b maps.borrow_Ndxs) borrows in
            // turn them into index arrays
            let range = new Range(List.length method_splits) in
            let splits_ndxs = Variable.Constant<int>(List.toArray splits_ndxs,range) in
            let merges_ndxs = Variable.Constant<int>(List.toArray merges_ndxs,range) in
            let split_e_ndxs = Variable.Constant<int>(List.toArray split_e_ndxs,range) in
            let post_e_ndxs = Variable.Constant<int>(List.toArray post_e_ndxs,range) in
            let borrow_ndxs = Variable.Constant<int>(List.toArray borrow_ndxs,range) in
            let borrows = Variable.GetItems(maps.borrow,borrow_ndxs) in
            // likelihood arrays
//            let likely_constants = Array.init (List.length method_splits) (fun _ -> likelihood) in
//            let likely_constants = Variable.Constant<Bernoulli>(likely_constants, range) in
            for k in allPerms do
                // for each perm kind,
                let kind_array = kindToArray k maps in
                let kind_edge_array = kindToEdgeArray k maps in
                // get the split & merge arrays of this kind
                let k_split_nodes = Variable.GetItems(kind_array,splits_ndxs) in
                let k_merge_nodes = Variable.GetItems(kind_array,merges_ndxs) in
                let k_split_edges = Variable.GetItems(kind_edge_array,split_e_ndxs) in
                let k_post_edges = Variable.GetItems(kind_edge_array,post_e_ndxs) in
            
                // borrow & merge = split OR
                // !borrow & merge = split or merge = post
                let form = Variable.Array<bool>(range) in
                let () = form.[range] <- (borrows.[range] &&& (Variable.op_Equality(k_merge_nodes.[range], k_split_nodes.[range]))) |||
                                         (Variable.op_LogicalNot(borrows.[range]) &&&
                                              (Variable.op_Equality(k_merge_nodes.[range],k_split_edges.[range])) |||
                                              (Variable.op_Equality(k_merge_nodes.[range],k_post_edges.[range])))
                in
                Variable.ConstrainEqualRandom( form.[range], likelihood )


        let groupedByNumNeighbors (input:((node list)*node) list) = //: (int*(((node list)*node) list)) list =
            let (map,max) =
                List.fold
                    (fun (m,max) (neighbors,node) ->
                         let num = List.length neighbors in
                         let max = if num > max then num else max in
                         (Multimap.add num (neighbors,node) m,max)) (Multimap.empty,0) input
            in
            map
            // TODO switch 2 to max if you ever want to do all incoming edges
  //          let result' = [ for i in 1..2 -> (i,Multimap.tryFind i map) ] in
            // now remove all the Nones...
//            List.choose (fun (i,v) -> match v with | Some(v) -> Some(i,v) | None -> None) result'

        /// nth list of lists
        let nthLol n lol = List.map (fun l -> List.nth l n) lol                

        /// Generates constraints that relate the permission at a node to the
        /// permissions on the edges coming into that node.
        /// Specifically: The permission on node is likely to be one of the
        /// permissions on the incoming edges.
        let incomingEdges (incoming_edges:(node list * node) list) maps (likelihood:Bernoulli) =
            let neighbor_map = groupedByNumNeighbors incoming_edges in
            let one_neighbors = Map.tryFind 1 neighbor_map in
            let two_neighbors = Map.tryFind 2 neighbor_map in
            if one_neighbors.IsSome then
                let one_neighbors = one_neighbors.Value in
                let length = List.length one_neighbors in
                let range = new Range(length) in
                let (neighbors,nodes) = List.unzip one_neighbors in
                // DEBUG
                //            let () = printfn "Nodes with one neighbor: %A" (List.map (fun n -> n.id) nodes) in
                // END DEBUG
                let neighbors = List.concat neighbors in // should be the same size now...
                let () = if (List.length neighbors) <> (List.length nodes) then raise Impossible else () in
                let node_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) nodes in
                let edge_ndxs = List.map (fun (n1,n2) -> Map.find (n1.id,n2.id) maps.edge_Ndxs) (List.zip neighbors nodes) in
                let node_ndxs = Variable.Constant<int>(List.toArray node_ndxs, range) in
                let edge_ndxs = Variable.Constant<int>(List.toArray edge_ndxs, range) in
                for k in allPerms do
                    let k_perms = kindToArray k maps in
                    let k_perms_e = kindToEdgeArray k maps in
                    let n_perms_k = Variable.Subarray(k_perms, node_ndxs) in
                    let e_perms_k = Variable.GetItems(k_perms_e, edge_ndxs) in
                    let cnstr = Variable.Array<bool>(range) in
                    let () = cnstr.[range] <- Variable.op_Equality( n_perms_k.[range], e_perms_k.[range] ) in
                    Variable.ConstrainEqualRandom( cnstr.[range], likelihood )
            if two_neighbors.IsSome then
                let two_neighbors = two_neighbors.Value in
                // now, do the two neighbors case, just like the one neighbors case...
                let length = List.length two_neighbors in
                let range = new Range(length) in
                let (neighbors,nodes) = List.unzip two_neighbors in
                let node_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) nodes in
                let edge1_ndxs = List.map (fun (n1,n2) -> Map.find (n1.id,n2.id) maps.edge_Ndxs) (List.zip (nthLol 0 neighbors) nodes) in
                let edge2_ndxs = List.map (fun (n1,n2) -> Map.find (n1.id,n2.id) maps.edge_Ndxs) (List.zip (nthLol 1 neighbors) nodes) in
                let node_ndxs  = Variable.Constant<int>(List.toArray node_ndxs,range) in
                let edge1_ndxs = Variable.Constant<int>(List.toArray edge1_ndxs,range) in
                let edge2_ndxs = Variable.Constant<int>(List.toArray edge2_ndxs,range) in
                for k in allPerms do
                    let k_perms = kindToArray k maps in
                    let k_perms_e = kindToEdgeArray k maps in
                    let n_perms_k = Variable.Subarray(k_perms, node_ndxs) in
                    let e1_perms_k = Variable.GetItems(k_perms_e, edge1_ndxs) in
                    let e2_perms_k = Variable.GetItems(k_perms_e, edge2_ndxs) in
                    let cnstr = Variable.Array<bool>(range) in
                    let () =
                        cnstr.[range] <- Variable.op_Equality( n_perms_k.[range], e1_perms_k.[range] ) |||
                                         Variable.op_Equality( n_perms_k.[range], e2_perms_k.[range] )
                    in
                    Variable.ConstrainEqualRandom( cnstr.[range], likelihood )

//            // sort nodes by the number of neighbors they each have
//            // and do all the groups w/ same numbers of neighbors at
//            // the same time, so that the arrays will be the same
//            // sizes.
//            let (one_neighbors,two_neighbors) =  groupedByNumNeighbors incoming_edges in
//            //                let () = printfn "group %i, |incoming_edges|=%i" num_neighbors (List.length incoming_edges) in
//            // get indices for each of
//            let (neighbors_s,ns) = List.unzip incoming_edges in
//            let length = List.length incoming_edges in
//            let range = new Range(length) in
//            let n_ndxs = List.map (fun n -> Map.find n.id maps.node_Ndxs) ns in 
//            let n_ndxs = Variable.Constant<int>(List.toArray n_ndxs, range) in
//            // now we have to do this for each permission kind!!            
//            for k in allPerms do
//                let k_perms = kindToArray k maps in
//                let n_perms_k = Variable.Subarray(k_perms, n_ndxs) in
//                    // case 1



            
//            let edges = List.map (fun n-> (n.id,node.id)) neighbors in
//            // for each edge, node is equal to one of the edges, OR one of the others.
//            let forEachKind k =
//                let node_var = (fst (kindToMap k maps)).Item(node.id) in // get the map for this edge for this perm kind
//                let base_case = (fst (kindToEdgeMap k maps)).Item(List.head edges) in //node equals edge
//                let base_case = Variable.op_Equality(base_case,node_var) in
//                let or_of_eq = // OR... folded
//                    List.fold (fun v edge -> 
//                                let edge_var = (fst (kindToEdgeMap k maps)).Item(edge) in
//                                Variable.op_Equality(base_case,node_var) ||| v)
//                            base_case
//                            (List.tail edges)
//                in
//                Variable.ConstrainEqualRandom(or_of_eq,likelihood)
//            in
//            List.iter forEachKind (allPerms)




        /// Generate simple outgoing edge constraints, for nodes where all of the
        /// outgoing edges can be exactly the same. 
        let outgoingEdges outgoing_edges maps (likelihood:Bernoulli) =
            // ndx construction is complicated
            // for each neighbor, we want to repeat the node index
            // so that in the end both arrays will be the same size
            let ndxs =
                List.map (fun (n,neighbors) ->
                          let n_ndx = Map.find n.id maps.node_Ndxs in
                          let no_neighbors = List.length neighbors in
                          let n_ndxs = List.replicate no_neighbors n_ndx in
                          let neigh_ndxs = List.map (fun neigh -> Map.find (n.id,neigh.id) maps.edge_Ndxs) neighbors in
                          (n_ndxs,neigh_ndxs)) outgoing_edges
            in
            let (node_ndxs,edge_ndxs) = List.unzip ndxs in
            let node_ndxs = List.concat node_ndxs in
            let edge_ndxs = List.concat edge_ndxs in
            let length = List.length node_ndxs in
            let range = new Range(length) in
            let node_ndxs = Variable.Constant<int>(List.toArray node_ndxs, range) in
            let edge_ndxs = Variable.Constant<int>(List.toArray edge_ndxs, range) in
            // likelihoods
//            let likely_constants = Array.init length (fun _ -> likelihood) in
//            let likely_constants = Variable.Constant<Bernoulli>(likely_constants,range) in
            // for each perm...
            for k in allPerms do
                // build sub arrays
                let k_node_ar = kindToArray k maps in
                let k_edge_ar = kindToEdgeArray k maps in
                let k_node_ar = Variable.Subarray(k_node_ar, node_ndxs) in
                let k_edge_ar = Variable.GetItems(k_edge_ar, edge_ndxs) in
                // build constraint array
                let cnstr_ar = Variable.Array<bool>(range) in
                // edge === node
                let () = cnstr_ar.[range] <- Variable.op_Equality( k_node_ar.[range], k_edge_ar.[range] ) in
                Variable.ConstrainEqualRandom( cnstr_ar.[range], likelihood ) 
      
            
//            let edges = List.map (fun n-> (node.id,n.id)) neighbors in
//            for edge in edges do
//                for (eq_var:Variable<bool>) in varNodeEqualsEdge node.id edge maps do
//                    Variable.ConstrainEqualRandom(eq_var,likelihood)
               


        /// Generates constraints that relate the permission at a node to the
        /// permissions on the edges that leave the node.
        /// Specifically: The permission on node is stronger than each of
        /// the edges leaving the node AND those edges are consistent with
        /// one another; for each pair of edges e1,e2, 
        /// imm(e1) -> !(unique(e2)|full(e2)|share(e2) and
        /// (unique(e1)|full(e1)) -> !(unique(e2)|full(e2)|share(e2)) and
        /// (unique(e1)) -> !(pure(e2))
//        let splitEdges split_edges maps (likely_high:Bernoulli) (likely_low:Bernoulli) =
//            // first, strip out all entries from split edges where
//            // the number of neighbors is either 1 or more than two.
//            let (two_neighbors, rest) =
//                List.partition (fun (_,neighbors) -> (List.length neighbors) = 2) split_edges
//            in
//            // just defer to outgoingEdges for the easy ones
//            let () = outgoingEdges rest maps likely_high in
//            // two_neighbors is the hard part...
//            // 5-28-10
//            // Based on today's discussion with Aditya, we're going
//            // to get rid of the equals every edge AND
//            // the edgeConsistency call, and assume that generally
//            // there are two edges. Then we will kind of
//            // enumerate all the possible splits, and OR them together.
//            let length = List.length two_neighbors in
//            let range = new Range(length) in
//            let n_ndxs = List.map (fun (n,_) -> Map.find n.id maps.node_Ndxs) two_neighbors in
//            let e1_ndxs =
//                List.map (fun (n,neighbors) -> let e_1 = (n.id,(List.head neighbors).id) in Map.find e_1 maps.edge_Ndxs) two_neighbors
//            in
//            let e2_ndxs =
//                List.map (fun (n,neighbors) -> let e_2 = (n.id,(List.nth neighbors 1).id) in Map.find e_2 maps.edge_Ndxs) two_neighbors
//            in
//            let n_ndxs = Variable.Constant<int>(List.toArray n_ndxs,range) in
//            let e1_ndxs = Variable.Constant<int>(List.toArray e1_ndxs,range) in
//            let e2_ndxs = Variable.Constant<int>(List.toArray e2_ndxs,range) in
//            // get all of the permission arrays for n,e1,e2
//            let n_u = Variable.Subarray(maps.unique_perms, n_ndxs) in
//            let n_f = Variable.Subarray(maps.full_perms, n_ndxs) in
//            let n_i = Variable.Subarray(maps.imm_perms, n_ndxs) in
//            let n_s = Variable.Subarray(maps.share_perms, n_ndxs) in
//            let n_p = Variable.Subarray(maps.pure_perms, n_ndxs) in
//            let e1_u = Variable.GetItems(maps.unique_e_perms, e1_ndxs) in
//            let e1_f = Variable.GetItems(maps.full_e_perms, e1_ndxs) in
//            let e1_i = Variable.GetItems(maps.imm_e_perms, e1_ndxs) in
//            let e1_s = Variable.GetItems(maps.share_e_perms, e1_ndxs) in
//            let e1_p = Variable.GetItems(maps.pure_e_perms, e1_ndxs) in
//            let e2_u = Variable.GetItems(maps.unique_e_perms, e2_ndxs) in
//            let e2_f = Variable.GetItems(maps.full_e_perms, e2_ndxs) in
//            let e2_i = Variable.GetItems(maps.imm_e_perms, e2_ndxs) in
//            let e2_s = Variable.GetItems(maps.share_e_perms, e2_ndxs) in
//            let e2_p = Variable.GetItems(maps.pure_e_perms, e2_ndxs) in
//            //
//            // LONG enumeration of all possible splits
//            //
//            // unique...
//            let unique_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range)
//                let () =
//                    use c = Variable.If(n_u.[range])
//                    // unique
//                    let tmp = (e1_u.[range]) in
//                    let tmp = (e2_u.[range]) ||| tmp in 
//                    // unique split
//                    let tmp = (e1_f.[range] &&& e2_p.[range]) ||| tmp in
//                    let tmp = (e1_p.[range] &&& e2_f.[range]) ||| tmp in
//                    let tmp = (e1_i.[range] &&& e2_i.[range]) ||| tmp in
//                    let tmp = (e1_i.[range] &&& e2_p.[range]) ||| tmp in
//                    let tmp = (e1_p.[range] &&& e2_i.[range]) ||| tmp in
//                    let tmp = (e1_s.[range] &&& e2_s.[range]) ||| tmp in
//                    let tmp = (e1_p.[range] &&& e2_s.[range]) ||| tmp in
//                    let tmp = (e1_s.[range] &&& e2_p.[range]) ||| tmp in
//                    let () = unique_cnstr.[range] <- tmp in
//                    let () = Variable.ConstrainEqualRandom( unique_cnstr.[range], likely_high ) in
//                    ()
//                in
//                let () =
//                    use c = Variable.IfNot(n_u.[range])
//                    let () = unique_cnstr.[range] <- Variable.op_LogicalNot(e1_u.[range] ||| e2_u.[range]) in
//                    //   let () = Variable.ConstrainEqualRandom( unique_cnstr.[range], likely_low ) in
//                    ()
//                in
//                ()
//            in
//            // full
//            let full_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range) in
//                let () =
//                    use c = Variable.If(n_f.[range]) in
//                    // full split
//                    let tmp = (e1_f.[range] &&& e2_p.[range]) in
//                    let tmp = (e2_p.[range] &&& e2_f.[range]) ||| tmp in
//                    let tmp = (e1_i.[range] &&& e2_i.[range]) ||| tmp in
//                    let tmp = (e1_i.[range] &&& e2_p.[range]) ||| tmp in
//                    let tmp = (e1_p.[range] &&& e2_i.[range]) ||| tmp in
//                    let tmp = (e1_s.[range] &&& e2_s.[range]) ||| tmp in
//                    let tmp = (e1_p.[range] &&& e2_s.[range]) ||| tmp in
//                    let tmp = (e1_s.[range] &&& e2_p.[range]) ||| tmp in
//                    let () = full_cnstr.[range] <- tmp in 
//                    let () = Variable.ConstrainEqualRandom( full_cnstr.[range], likely_high ) in ()
//                in
//                let () =
//                    use c = Variable.IfNot(n_f.[range])
//                    let () = full_cnstr.[range] <- Variable.Bernoulli(1.0) in ()
//                in
//                ()
//            in
//
//            // imm
//            let imm_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range) in
//                let () =
//                    use c = Variable.If(n_i.[range]) in
//                    let tmp = (e1_i.[range] &&& e2_i.[range]) in
//                    let tmp = (e1_p.[range] &&& e2_i.[range]) ||| tmp in
//                    let tmp = (e1_i.[range] &&& e2_p.[range]) ||| tmp in
//                    let () = imm_cnstr.[range] <- tmp in
//                    let () = Variable.ConstrainEqualRandom( imm_cnstr.[range], likely_high ) in
//                    ()
//                in
//                let () =
//                    use c = Variable.IfNot(n_i.[range]) in
//                    let () = imm_cnstr.[range] <- Variable.Bernoulli(1.0) in ()
//                in
//                ()
//            in
//
//            // share
//            let share_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range) in
//                let () =
//                    use c = Variable.If(n_s.[range]) in
//                    // share
//                    let tmp = (e1_s.[range] &&& e2_s.[range]) in
//                    let tmp = (e1_p.[range] &&& e2_s.[range]) ||| tmp in
//                    let tmp = (e1_s.[range] &&& e2_p.[range]) ||| tmp in
//                    let () = share_cnstr.[range] <- tmp in
//                    let () = Variable.ConstrainEqualRandom( share_cnstr.[range], likely_high ) in
//                    ()
//                in
//                let () =
//                    use c = Variable.IfNot(n_s.[range]) in
//                    let () = share_cnstr.[range] <- Variable.Bernoulli(1.0) in ()
//                in
//                ()
//            in
//
//            // pure
//            let pure_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range) in
//                let () =
//                    use c = Variable.If(n_p.[range]) in
//                    // pure
//                    let () = pure_cnstr.[range] <- e1_p.[range] &&& e2_p.[range] in
//                    let () = Variable.ConstrainEqualRandom( pure_cnstr.[range], likely_high ) in
//                    ()
//                in
//                let () =
//                    use c = Variable.IfNot(n_p.[range]) in
//                    let () = pure_cnstr.[range] <- Variable.Bernoulli(1.0) in
//                    ()
//                in
//                ()
//            in
//
//
//
//
//                    
//            // create final array
////            let v_or = Variable.Array<bool>(range) in
////            // unique
////            let tmp = (n_u.[range] &&& e1_u.[range]) in
////            let tmp = (n_u.[range] &&& e2_u.[range]) ||| tmp in
////            // unique split
////            let tmp = (n_u.[range] &&& e1_f.[range] &&& e2_p.[range]) ||| tmp in
////            let tmp = (n_u.[range] &&& e1_p.[range] &&& e2_f.[range]) ||| tmp in
////            let tmp = (n_u.[range] &&& e1_i.[range] &&& e2_i.[range]) ||| tmp in
////            let tmp = (n_u.[range] &&& e1_i.[range] &&& e2_p.[range]) ||| tmp in
////            let tmp = (n_u.[range] &&& e1_p.[range] &&& e2_i.[range]) ||| tmp in
////            let tmp = (n_u.[range] &&& e1_s.[range] &&& e2_s.[range]) ||| tmp in
////            let tmp = (n_u.[range] &&& e1_p.[range] &&& e2_s.[range]) ||| tmp in
////            let tmp = (n_u.[range] &&& e1_s.[range] &&& e2_p.[range]) ||| tmp in
////            // full split
////            let tmp = (n_f.[range] &&& e1_f.[range] &&& e2_p.[range]) ||| tmp in
////            let tmp = (n_f.[range] &&& e2_p.[range] &&& e2_f.[range]) ||| tmp in
////            let tmp = (n_f.[range] &&& e1_i.[range] &&& e2_i.[range]) ||| tmp in
////            let tmp = (n_f.[range] &&& e1_i.[range] &&& e2_p.[range]) ||| tmp in
////            let tmp = (n_f.[range] &&& e1_p.[range] &&& e2_i.[range]) ||| tmp in
////            let tmp = (n_f.[range] &&& e1_s.[range] &&& e2_s.[range]) ||| tmp in
////            let tmp = (n_f.[range] &&& e1_p.[range] &&& e2_s.[range]) ||| tmp in
////            let tmp = (n_f.[range] &&& e1_s.[range] &&& e2_p.[range]) ||| tmp in
////            // imm
////            let tmp = (n_i.[range] &&& e1_i.[range] &&& e2_i.[range]) ||| tmp in
////            let tmp = (n_i.[range] &&& e1_p.[range] &&& e2_i.[range]) ||| tmp in
////            let tmp = (n_i.[range] &&& e1_i.[range] &&& e2_p.[range]) ||| tmp in
////            // share
////            let tmp = (n_s.[range] &&& e1_s.[range] &&& e2_s.[range]) ||| tmp in
////            let tmp = (n_s.[range] &&& e1_p.[range] &&& e2_s.[range]) ||| tmp in
////            let tmp = (n_s.[range] &&& e1_s.[range] &&& e2_p.[range]) ||| tmp in
////            // pure
////            let tmp = (n_p.[range] &&& e1_p.[range] &&& e2_p.[range]) ||| tmp in
////            // make it likely
////            let () = v_or.[range] <- tmp in
////            let () = Variable.ConstrainEqualRandom(v_or.[range], likely_high) in
//            // Bad stuff
//            let bad = Variable.Array<bool>(range) in
//            let () = bad.[range] <- (e1_u.[range] &&& e2_u.[range]) ||| (e1_f.[range] &&& e2_f.[range]) in
//            let () = Variable.ConstrainEqualRandom(bad.[range], likely_low) in
////            let bad = Variable.Array<bool>(range) in
////            let () = bad.[range] <- Variable.op_LogicalNot(n_u.[range] ||| n_f.[range] ||| n_i.[range] ||| n_s.[range] ||| n_p.[range]) &&& (e1_u.[range] ||| e1_f.[range] ||| e1_i.[range] ||| e1_s.[range] ||| e1_p.[range] ||| e2_u.[range] ||| e2_f.[range] ||| e2_i.[range] ||| e2_s.[range] ||| e2_p.[range])
////            let () = Variable.ConstrainEqualRandom(bad.[range], likely_low) in
//            ()

            
//            let edge_1 = List.head edges in
//            let edge_2 = List.head (List.tail edges) in
//            let (n_u, n_f, n_i, n_s, n_p) = mapsForNode node.id maps in
//            let (e1_u,e1_f,e1_i,e1_s,e1_p) = mapsForEdge edge_1 maps in
//            let (e2_u,e2_f,e2_i,e2_s,e2_p) = mapsForEdge edge_2 maps in
//            //
//            // LONG enumeration of all possible splits
//            //
//            // unique
//            let v_or =  n_u &&& e1_u  in
//            let v_or = (n_u &&& e2_u) ||| v_or in
//            // unique split
//            let v_or = (n_u &&& e1_f &&& e2_p) ||| v_or in
//            let v_or = (n_u &&& e1_p &&& e2_f) ||| v_or in
//            let v_or = (n_u &&& e1_i &&& e2_i) ||| v_or in
//            let v_or = (n_u &&& e1_i &&& e2_p) ||| v_or in
//            let v_or = (n_u &&& e1_p &&& e2_i) ||| v_or in
//            let v_or = (n_u &&& e1_s &&& e2_s) ||| v_or in
//            let v_or = (n_u &&& e1_p &&& e2_s) ||| v_or in
//            let v_or = (n_u &&& e1_s &&& e2_p) ||| v_or in
//            // full split
//            let v_or = (n_f &&& e1_f &&& e2_p) ||| v_or in
//            let v_or = (n_f &&& e2_p &&& e2_f) ||| v_or in
//            let v_or = (n_f &&& e1_i &&& e2_i) ||| v_or in
//            let v_or = (n_f &&& e1_i &&& e2_p) ||| v_or in
//            let v_or = (n_f &&& e1_p &&& e2_i) ||| v_or in
//            let v_or = (n_f &&& e1_s &&& e2_s) ||| v_or in
//            let v_or = (n_f &&& e1_p &&& e2_s) ||| v_or in
//            let v_or = (n_f &&& e1_s &&& e2_p) ||| v_or in
//            // imm
//            let v_or = (n_i &&& e1_i &&& e2_i) ||| v_or in
//            let v_or = (n_i &&& e1_p &&& e2_i) ||| v_or in
//            let v_or = (n_i &&& e1_i &&& e2_p) ||| v_or in
//            // share
//            let v_or = (n_s &&& e1_s &&& e2_s) ||| v_or in
//            let v_or = (n_s &&& e1_p &&& e2_s) ||| v_or in
//            let v_or = (n_s &&& e1_s &&& e2_p) ||| v_or in
//            // pure
//            let v_or = (n_p &&& e1_p &&& e2_p) ||| v_or in
//            Variable.ConstrainEqualRandom(v_or, likelihood);
//            //
//            // Other BAD things
//            // 
//            Variable.ConstrainEqualRandom(e1_u &&& e2_u, low)






            
        /// Generates constraints that relate the permission at a node to the
        /// permissions on the edges that leave the node.
        let splitEdges split_edges maps (likely_high:Bernoulli) (likely_low:Bernoulli) =
            // first, strip out all entries from split edges where
            // the number of neighbors is either 1 or more than two.
            let (two_neighbors, rest) =
                List.partition (fun (_,neighbors) -> (List.length neighbors) = 2) split_edges
            in
            // just defer to outgoingEdges for the easy ones
            let () = outgoingEdges rest maps likely_high in
            // two_neighbors is the hard part...
            // 5-28-10
            // Based on today's discussion with Aditya, we're going
            // to get rid of the equals every edge AND
            // the edgeConsistency call, and assume that generally
            // there are two edges.
            let length = List.length two_neighbors in
            let range = new Range(length) in
            let n_ndxs = List.map (fun (n,_) -> Map.find n.id maps.node_Ndxs) two_neighbors in
            let e1_ndxs =
                List.map (fun (n,neighbors) -> let e_1 = (n.id,(List.head neighbors).id) in Map.find e_1 maps.edge_Ndxs) two_neighbors
            in
            let e2_ndxs =
                List.map (fun (n,neighbors) -> let e_2 = (n.id,(List.nth neighbors 1).id) in Map.find e_2 maps.edge_Ndxs) two_neighbors
            in
            let n_ndxs = Variable.Constant<int>(List.toArray n_ndxs,range) in
            let e1_ndxs = Variable.Constant<int>(List.toArray e1_ndxs,range) in
            let e2_ndxs = Variable.Constant<int>(List.toArray e2_ndxs,range) in
            // get all of the permission arrays for n,e1,e2
            let n_u = Variable.Subarray(maps.unique_perms, n_ndxs) in
            let n_f = Variable.Subarray(maps.full_perms, n_ndxs) in
            let n_i = Variable.Subarray(maps.imm_perms, n_ndxs) in
            let n_s = Variable.Subarray(maps.share_perms, n_ndxs) in
            let n_p = Variable.Subarray(maps.pure_perms, n_ndxs) in
            let e1_u = Variable.GetItems(maps.unique_e_perms, e1_ndxs) in
            let e1_f = Variable.GetItems(maps.full_e_perms, e1_ndxs) in
            let e1_i = Variable.GetItems(maps.imm_e_perms, e1_ndxs) in
            let e1_s = Variable.GetItems(maps.share_e_perms, e1_ndxs) in
            let e1_p = Variable.GetItems(maps.pure_e_perms, e1_ndxs) in
            let e2_u = Variable.GetItems(maps.unique_e_perms, e2_ndxs) in
            let e2_f = Variable.GetItems(maps.full_e_perms, e2_ndxs) in
            let e2_i = Variable.GetItems(maps.imm_e_perms, e2_ndxs) in
            let e2_s = Variable.GetItems(maps.share_e_perms, e2_ndxs) in
            let e2_p = Variable.GetItems(maps.pure_e_perms, e2_ndxs) in

            // Okay, new attempt.
            // Now we'll talk about equality of the variables rather
            // than truthiness:
            // Shares & Immutables are the same
            // shares
            let () = Variable.ConstrainEqualRandom( Variable.op_Equality(n_s.[range], e1_s.[range]), likely_high ) in
            let () = Variable.ConstrainEqualRandom( Variable.op_Equality(n_s.[range], e2_s.[range]), likely_high ) in
            let () = Variable.ConstrainEqualRandom( Variable.op_Equality(e1_s.[range], e2_s.[range]), likely_high ) in
            // imms
            let () = Variable.ConstrainEqualRandom( Variable.op_Equality(n_i.[range], e1_i.[range]), likely_high ) in
            let () = Variable.ConstrainEqualRandom( Variable.op_Equality(n_i.[range], e2_i.[range]), likely_high ) in
            let () = Variable.ConstrainEqualRandom( Variable.op_Equality(e1_i.[range], e2_i.[range]), likely_high ) in
            // uniques
            let tmp = Variable.op_Equality(n_u.[range], e1_u.[range]) in
            let tmp = tmp ||| Variable.op_Equality(n_u.[range], e2_u.[range]) in
            let () = Variable.ConstrainEqualRandom( tmp, likely_high ) in
            let () = Variable.ConstrainEqualRandom( e1_u.[range] &&& e2_u.[range], likely_low ) in
            // full/pure
            // the most complicated one...
            // n_f = e1_f = e2_p OR n_f = e2_f = e1_p
            let tmp = Variable.op_Equality(n_f.[range],e1_f.[range]) in
            let tmp = Variable.op_Equality(n_f.[range],e2_p.[range]) &&& tmp in
            let tmp2 = Variable.op_Equality(n_f.[range],e2_f.[range]) in
            let tmp2 = Variable.op_Equality(n_f.[range],e1_p.[range]) &&& tmp2 in
            let tmp = tmp ||| tmp2
            let () = Variable.ConstrainEqualRandom( tmp, likely_high ) in
            let () = Variable.ConstrainEqualRandom( e1_f.[range] &&& e2_f.[range], likely_low ) in
            ()
            
//            //
//            // LONG enumeration of all possible splits
//            //
//            //
//            // WE ARE NOW RELYING ON THE FACT THAT THE
//            // THE SECOND EDGE IS COMING FROM THE PRE OF
//            // THE CALLED ARGUMENT.
//            //
//            // unique...
//            let unique_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range)
//                let () =
//                    use c = Variable.If(e2_u.[range])
//                    let () = unique_cnstr.[range] <- n_u.[range] in
//                    let () = Variable.ConstrainEqualRandom( unique_cnstr.[range], likely_high ) in
//                    ()
//                in
//                let () =
//                    use c = Variable.IfNot(e2_u.[range])
//                    let () = unique_cnstr.[range] <- Variable.Bernoulli(1.0) in
//                    //   let () = Variable.ConstrainEqualRandom( unique_cnstr.[range], likely_low ) in
//                    ()
//                in
//                ()
//            in
//            // full
//            let full_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range) in
//                let () =
//                    use c = Variable.If(e2_f.[range]) in
//                    // full split
//                    let tmp = ((n_u.[range] ||| n_f.[range]) &&& e1_p.[range]) in
////                    let tmp = (e2_p.[range] &&& e2_f.[range]) ||| tmp in
//                    let () = full_cnstr.[range] <- tmp in 
//                    let () = Variable.ConstrainEqualRandom( full_cnstr.[range], likely_high ) in ()
//                in
//                let () =
//                    use c = Variable.IfNot(e2_f.[range])
//                    let () = full_cnstr.[range] <- Variable.Bernoulli(1.0) in ()
//                in
//                ()
//            in
//
//            // imm
//            let imm_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range) in
//                let () =
//                    use c = Variable.If(e2_i.[range]) in
//                    let tmp = (n_i.[range] &&& e1_i.[range]) in
//                    let () = imm_cnstr.[range] <- tmp in
//                    let () = Variable.ConstrainEqualRandom( imm_cnstr.[range], likely_high ) in
//                    ()
//                in
//                let () =
//                    use c = Variable.IfNot(e2_i.[range]) in
//                    let () = imm_cnstr.[range] <- Variable.Bernoulli(1.0) in ()
//                in
//                ()
//            in
//
//            // share
//            let share_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range) in
//                let () =
//                    use c = Variable.If(e2_s.[range]) in
//                    // share
//                    let tmp = (n_s.[range] &&& e1_s.[range])
//                    let () = share_cnstr.[range] <- tmp in
//                    let () = Variable.ConstrainEqualRandom( share_cnstr.[range], likely_high ) in
//                    ()
//                in
//                let () =
//                    use c = Variable.IfNot(e2_s.[range]) in
//                    let () = share_cnstr.[range] <- Variable.Bernoulli(1.0) in ()
//                in
//                ()
//            in
//
//            // pure
//            let pure_cnstr = Variable.Array<bool>(range) in
//            let () =
//                use a = Variable.ForEach(range) in
//                let () =
//                    use c = Variable.If(e2_p.[range]) in
//                    // pure
//                    let () = pure_cnstr.[range] <- n_p.[range] &&& e1_p.[range] in
//                    let () = Variable.ConstrainEqualRandom( pure_cnstr.[range], likely_high ) in
//                    ()
//                in
//                let () =
//                    use c = Variable.IfNot(e2_p.[range]) in
//                    let () = pure_cnstr.[range] <- Variable.Bernoulli(1.0) in
//                    ()
//                in
//                ()
//            in
//            // Bad stuff
////            let bad = Variable.Array<bool>(range) in
////            let () = bad.[range] <- (e1_u.[range] &&& e2_u.[range]) ||| (e1_f.[range] &&& e2_f.[range]) in
////            let () = Variable.ConstrainEqualRandom(bad.[range], likely_low) in
////            let bad = Variable.Array<bool>(range) in
////            let () = bad.[range] <- Variable.op_LogicalNot(n_u.[range] ||| n_f.[range] ||| n_i.[range] ||| n_s.[range] ||| n_p.[range]) &&& (e1_u.[range] ||| e1_f.[range] ||| e1_i.[range] ||| e1_s.[range] ||| e1_p.[range] ||| e2_u.[range] ||| e2_f.[range] ||| e2_i.[range] ||| e2_s.[range] ||| e2_p.[range])
////            let () = Variable.ConstrainEqualRandom(bad.[range], likely_low) in
//            ()

              
        
        /// The permission at node has the same state and guarantee as the
        /// permission at edge
        let sameStatesEdge nodes_and_edges maps (likelihood:Bernoulli) =
            let length = List.length nodes_and_edges in
            let (nodes,edges) = List.unzip nodes_and_edges in
            // get indeces for nodes & edge states
            let node_ndxs = List.map (fun n -> Map.find n.id maps.state_Ndxs) nodes in
            let edge_ndxs = List.map (fun e -> Map.find e maps.state_e_Ndxs) edges in
            let node_ndxs = List.concat node_ndxs in
            let edge_ndxs = List.concat edge_ndxs in
            let range = new Range(List.length node_ndxs) in
            let node_ndxs = Variable.Constant<int>(List.toArray node_ndxs,range) in
            let edge_ndxs = Variable.Constant<int>(List.toArray edge_ndxs,range) in
            // get subarrays
            let n_state_ar = Variable.GetItems(maps.state, node_ndxs) in
            let e_state_ar = Variable.GetItems(maps.state_e, edge_ndxs) in
            let n_guar_ar = Variable.GetItems(maps.guarantee, node_ndxs) in
            let e_guar_ar = Variable.GetItems(maps.guarantee_e, edge_ndxs) in
            // constraint arrays
            let state_cnstr_ar = Variable.Array<bool>(range) in
            let guar_cnstr_ar = Variable.Array<bool>(range) in
            let () = state_cnstr_ar.[range] <- Variable.op_Equality( n_state_ar.[range], e_state_ar.[range] ) in
            let () = guar_cnstr_ar.[range] <- Variable.op_Equality( n_guar_ar.[range], e_guar_ar.[range] ) in
            // constrain to be likely true
            let () = Variable.ConstrainEqualRandom( state_cnstr_ar.[range], likelihood ) in
            let () = Variable.ConstrainEqualRandom( guar_cnstr_ar.[range],  likelihood ) in
            ()

        /// Given program constraints, modifies map in place with the new prob. constraints
        let applyProgramConstraints constraints maps cnstr_params : unit =
            // for each constraint type, build up a list of them, send off to the helper
            let equals_cs =
                List.fold (fun (n1s,n2s) c -> match c with | Equal(n1,n2) -> (n1::n1s,n2::n2s) | _ -> (n1s,n2s)) ([],[]) constraints
            in
            let () = equalNodesConstraints equals_cs maps cnstr_params.el in
            let hequals_cs =
                List.fold (fun (n1s,n2s) c -> match c with | HEqual(n1,n2) -> (n1::n1s,n2::n2s) | _ -> (n1s,n2s)) ([],[]) constraints
            in  
            let () = equalNodesConstraints hequals_cs maps cnstr_params.hel in
            let create_unique_cs =
                List.choose (fun c -> match c with | HCreateUnique(n) -> Some(n) | _ -> None) constraints
            in
            let () = createMethodUniqueConstraints create_unique_cs maps cnstr_params.cul cnstr_params.cul_low in
            let getter_cs =
                List.choose (fun c -> match c with | HGetter(n) -> Some(n) | _ -> None) constraints
            in
            let () = getterConstraints getter_cs maps cnstr_params.getter_l cnstr_params.getter_l_low in
            let setter_cs =
                List.choose (fun c -> match c with | HSetter(n) -> Some(n) | _ -> None) constraints
            in
            let () = setterConstraints setter_cs maps cnstr_params.getter_l cnstr_params.getter_l_low in
            let synced_cs =
                List.choose (fun c -> match c with | HSynchronized(n) -> Some(n) | _ -> None) constraints
            in
            let () = synchronizedConstraints synced_cs maps cnstr_params.sync_l cnstr_params.sync_l_low in
            let ctr_cs =
                List.choose (fun c -> match c with | HConstructor(n) -> Some(n) | _ -> None) constraints
            in
            let () = constructorConstraints ctr_cs maps cnstr_params.ctr_l cnstr_params.ctr_l_low in
            let must_writes = List.choose (fun c -> match c with | MustWrite(n) -> Some(n) | _ -> None) constraints in
//            let () = mustWrite must_writes maps cnstr_params.mwh cnstr_params.mwl in
            let must_reads = List.choose (fun c -> match c with | MustRead(n) -> Some(n) | _ -> None) constraints in
//            let () = mustRead must_reads maps cnstr_params.mrl in
            let arg_is_params =
                List.choose
                    (fun c ->
                         match c with
                             | ArgIsParam(apre,apost,ppre,ppost,isPrivate) -> Some(apre,apost,ppre,ppost,isPrivate)
                             | _ -> None) constraints
            in
            let () = argIsParam arg_is_params maps cnstr_params.aap in
            let needs_frames = List.choose (fun c -> match c with | NeedsFrame(n) -> Some(n) | _ -> None) constraints in
            let () = needsFrame needs_frames maps cnstr_params.mrl // pretty likely. Do we need our own likelihood?
            let frames_sames = List.choose (fun c -> match c with | FramesSame(n,(n1,n2)) -> Some(n,(n1,n2)) | _ -> None) constraints in
            let () = framesSameEdge frames_sames maps cnstr_params.edge_l in
            let method_splits = List.choose (fun c -> match c with | MethodSplit(n1,n2,n3,n4) -> Some(n1,n2,n3,n4) | _ -> None) constraints in
            let () = methodSplits method_splits maps cnstr_params.splt_l in
            // I am continuing on while I wait for a response from INFER.NET folks but
            // if you do get a response you can comment out the remaining lines to get back to where you were.
            let states_same = List.choose (fun c -> match c with | StatesSame(n1,edge) -> Some(n1,edge) | _ -> None) constraints in
            let () = sameStatesEdge states_same maps cnstr_params.edge_l in
            let outgoing_edges =
                List.choose (fun c -> match c with | OutgoingEdges(node,neighbors) -> Some(node,neighbors) | _ -> None) constraints
            in
            let () = outgoingEdges outgoing_edges maps cnstr_params.edge_l in
            let split_edges =
                List.choose (fun c -> match c with | SplitEdges(node,neighbors) -> Some(node,neighbors) | _ -> None) constraints
            in
            let () = splitEdges split_edges maps cnstr_params.edge_l cnstr_params.edge_l_low in
            let incoming_edges =
                List.choose (fun c -> match c with IncomingEdges(neighbors,node) -> Some(neighbors,node) | _ -> None) constraints
            in
            let () = incomingEdges incoming_edges maps cnstr_params.edge_l in
            let contingent_borrows =
                List.choose (fun c -> match c with ContingentBorrow(n1,n2,args) -> Some(n1,n2,args) | _ -> None) constraints
            in
            let () = contingentBorrow contingent_borrows maps cnstr_params.el in
            ()

        // Make all parameters deterministic (1.0)
        let deterministicParameters (cnstr_params:Map<string,float>) =
            let is_perm_thresh = 
                try cnstr_params.Item "IsPermissionThreshold" with _ -> raise (NoKey("IsPermissionThreshold")) in
            let borrowing_threshold = 
                try cnstr_params.Item "BorrowingThreshold" with _ -> raise (NoKey("BorrowingThreshold")) in
            let high = new MicrosoftResearch.Infer.Distributions.Bernoulli(1.0) in
            let high_const = Variable.Constant(high) in
            let low  = new MicrosoftResearch.Infer.Distributions.Bernoulli(0.0) in
            {mrl=high;mwh=high;mwl=low; sl=high_const;el=high;hel=high;
             edge_l=high;edge_l_low=low; sync_l=high;sync_l_low=low; ctr_l=high;ctr_l_low=low;
             aap=high;cul=high;cul_low=low; getter_l=high;getter_l_low=low;
             splt_l=high;ipt=is_perm_thresh;bth=borrowing_threshold} 
            
        // Load all parameters from the file. Do it once so that we don't
        // make multiple constants.
        let loadParameters (cnstr_params:Map<string,float>) =
            let must_read_likelihood = 
                try cnstr_params.Item "MustReadLikelihood" with _ -> raise (NoKey("MustReadLikelihood")) in
            let must_read_likelihood = new MicrosoftResearch.Infer.Distributions.Bernoulli(must_read_likelihood) in

            let must_write_likelihood = 
                try cnstr_params.Item "MustWriteLikelihood" with _ -> raise (NoKey("MustWriteLikelihood")) in
            let must_write_high = new MicrosoftResearch.Infer.Distributions.Bernoulli(must_write_likelihood) in
            let must_write_low = new MicrosoftResearch.Infer.Distributions.Bernoulli(1.0 - must_write_likelihood) in
            
            let stronger_likelihood = 
                try cnstr_params.Item "StrongerLikelihood" with _ -> raise (NoKey("StrongerLikelihood")) in
            let stronger_likelihood = Variable.Constant(new MicrosoftResearch.Infer.Distributions.Bernoulli(stronger_likelihood)) in

            let equal_likelihood = 
                try cnstr_params.Item "EqualLikelihood" with _ -> raise (NoKey("EqualLikelihood")) in
            let equal_likelihood = new MicrosoftResearch.Infer.Distributions.Bernoulli(equal_likelihood) in

            let split_likelihood = 
                try cnstr_params.Item "SplitLikelihood" with _ -> raise (NoKey("SplitLikelihood")) in
            let split_likelihood = new MicrosoftResearch.Infer.Distributions.Bernoulli(split_likelihood) in

            let edge_likelihood = 
                try cnstr_params.Item "EdgeLikelihood" with _ -> raise (NoKey("EdgeLikelihood")) in
            let edge_likelihood_high = new MicrosoftResearch.Infer.Distributions.Bernoulli(edge_likelihood) in
            let edge_likelihood_low = new MicrosoftResearch.Infer.Distributions.Bernoulli(1.0 - edge_likelihood) in
            
            let aap_likelihood = 
                try cnstr_params.Item "ArgsAreParams" with _ -> raise (NoKey("ArgsAreParams")) in
            let aap_likelihood = new MicrosoftResearch.Infer.Distributions.Bernoulli(aap_likelihood) in

            let hequal_likelihood = 
                try cnstr_params.Item "HEqualLikelihood" with _ -> raise (NoKey("HEqualLikelihood")) in
            let hequal_likelihood = new MicrosoftResearch.Infer.Distributions.Bernoulli(hequal_likelihood) in
  
            let create_unique_likelihood =
                try cnstr_params.Item "HCreateUniqueLikelihood" with _ -> raise (NoKey("HCreateUniqueLikelihood")) in
            let create_unique_likelihood_high = new MicrosoftResearch.Infer.Distributions.Bernoulli(create_unique_likelihood) in
            let create_unique_likelihood_low = new MicrosoftResearch.Infer.Distributions.Bernoulli(1.0 - create_unique_likelihood) in

            let getter_likelihood =
                try cnstr_params.Item "HGetterLikelihood" with _ -> raise (NoKey("HGetterLikelihood")) in
            let getter_likelihood_high = new MicrosoftResearch.Infer.Distributions.Bernoulli(getter_likelihood) in
            let getter_likelihood_low = new MicrosoftResearch.Infer.Distributions.Bernoulli(1.0 - getter_likelihood) in

            let sync_likelihood =
                try cnstr_params.Item "HSyncLikelihood" with _ -> raise (NoKey("HSyncLikelihood")) in
            let sync_likelihood_high = new MicrosoftResearch.Infer.Distributions.Bernoulli(sync_likelihood) in
            let sync_likelihood_low = new MicrosoftResearch.Infer.Distributions.Bernoulli(1.0 - sync_likelihood) in

            let ctr_likelihood =
                try cnstr_params.Item "HConstructorLikelihood" with _ -> raise (NoKey("HConstructorLikelihood")) in
            let ctr_likelihood_high = new MicrosoftResearch.Infer.Distributions.Bernoulli(ctr_likelihood) in
            let ctr_likelihood_low = new MicrosoftResearch.Infer.Distributions.Bernoulli(1.0 - ctr_likelihood) in
       
            let is_perm_thresh = 
                try cnstr_params.Item "IsPermissionThreshold" with _ -> raise (NoKey("IsPermissionThreshold")) in
            let borrowing_threshold = 
                try cnstr_params.Item "BorrowingThreshold" with _ -> raise (NoKey("BorrowingThreshold")) in

            {mrl=must_read_likelihood;mwh=must_write_high;mwl=must_write_low;
             sl=stronger_likelihood;el=equal_likelihood;hel=hequal_likelihood;
             edge_l=edge_likelihood_high;edge_l_low=edge_likelihood_low;
             sync_l=sync_likelihood_high;sync_l_low=sync_likelihood_low;
             ctr_l=ctr_likelihood_high;ctr_l_low=ctr_likelihood_low;
             aap=aap_likelihood;cul=create_unique_likelihood_high;cul_low=create_unique_likelihood_low;
             getter_l=getter_likelihood_high;getter_l_low=getter_likelihood_low;
             splt_l=split_likelihood;ipt=is_perm_thresh;bth=borrowing_threshold} 


        let printConstraints cs =
            let printer c =
                match c with
                    | Equal(n1,n2) -> printfn "Equal(%s,%s)" n1.id n2.id
                    | ArgIsParam(apre,apost,ppre,ppost,isPrivate) -> printfn "ArgIsParam(%s,%s,%s,%s,%b)" apre.id apost.id ppre.id ppost.id isPrivate
                    | MethodSplit(split,pre,post,merge) -> printfn "MethodSplit(%s,%s,%s,%s)" split.id pre.id post.id merge.id
                    | MustWrite(n) -> printfn "MustWrite(%s)" n.id
                    | MustRead(n) -> printfn "MustRead(%s)" n.id
                    | NeedsFrame(n) -> printfn "NeedsFrame(%s)" n.id
                    | FramesSame(n,(nid1,nid2)) -> printfn "FramesSame(%s,(%s,%s))" n.id nid1 nid2
                    | CannotBorrow(n1,n2) -> printfn "CannotBorrow(%s,%s)" n1.id n2.id
                    | CanBorrow(n1,n2)-> printfn "CanBorrow(%s,%s)" n1.id n2.id
                    | ContingentBorrow(n1,n2,args) ->
                        printfn "ContingentBorrow(%s,%s,%A)" n1.id n2.id (List.map (fun (n1,n2) -> (n1.id,n2.id)) args)
                    | IncomingEdges(ns,n) -> printfn "IncomingEdges(%A,%s)" (List.map (fun n -> n.id) ns) n.id
                    | OutgoingEdges(n,ns) -> printfn "OutgoingEdges(%s,%A)" n.id (List.map (fun n -> n.id) ns)
                    | SplitEdges(n,ns) -> printfn "SplitEdges(%s,%A)" n.id (List.map (fun n -> n.id) ns)
                    | StatesSame(n,(nid1,nid2)) -> printfn "StatesSame(%s,(%s,%s))" n.id nid1 nid2
                    | HEqual(n1,n2) -> printfn "HEqual(%s,%s)" n1.id n2.id
                    | HCreateUnique(n) -> printfn "HCreateUnique(%s)" n.id
                    | HGetter(n) -> printfn "HGetter(%s)" n.id
                    | HSetter(n) -> printfn "HSetter(%s)" n.id
                    | HSynchronized(n) -> printfn "HSynchronized(%s)" n.id
                    | HConstructor(n) -> printfn "HConstructor(%s)" n.id
                    | ConcreteSuccessor(n) -> printfn "ConcreteSuccessor(%s)" n.id
            in
            List.iter printer cs

             
        let printNodesResult ns maps (ie:InferenceEngine) =
            let uniques = ie.Infer<DistributionArray<Bernoulli>>(maps.unique_perms) in
            let fulls = ie.Infer<DistributionArray<Bernoulli>>(maps.full_perms) in
            let imms = ie.Infer<DistributionArray<Bernoulli>>(maps.imm_perms) in
            let shares = ie.Infer<DistributionArray<Bernoulli>>(maps.share_perms) in
            let pures = ie.Infer<DistributionArray<Bernoulli>>(maps.pure_perms) in
            let frames = ie.Infer<DistributionArray<Bernoulli>>(maps.frame) in
            for n in ns do
                let () = printfn "Node %s" n.id in
                let ndx = Map.find n.id maps.node_Ndxs in
                let () = printfn "<%f,%f,%f,%f,%f, frame: %f>" 
                             (uniques.[ndx].GetProbTrue())
                             (fulls.[ndx].GetProbTrue())
                             (imms.[ndx].GetProbTrue())
                             (shares.[ndx].GetProbTrue())
                             (pures.[ndx].GetProbTrue())
                             (frames.[ndx].GetProbTrue())
                in
                ()

        let printEdgesResult nodes maps (ie:InferenceEngine) : unit =
            let uniques = ie.Infer<DistributionArray<Bernoulli>>(maps.unique_e_perms) in
            let fulls = ie.Infer<DistributionArray<Bernoulli>>(maps.full_e_perms) in
            let imms = ie.Infer<DistributionArray<Bernoulli>>(maps.imm_e_perms) in
            let shares = ie.Infer<DistributionArray<Bernoulli>>(maps.share_e_perms) in
            let pures = ie.Infer<DistributionArray<Bernoulli>>(maps.pure_e_perms) in
            let frames = ie.Infer<DistributionArray<Bernoulli>>(maps.frame_e) in
            for n1 in nodes do
                for n2 in (!n1.succs) do
                    if n1 = n2 then ()
                    else
                        let ndx = Map.find (n1.id,n2.id) maps.edge_Ndxs in
                        printfn "Edge (%s,%s)" n1.id n2.id;
                        printfn "<%f,%f,%f,%f,%f, frame: %f>"
                             (uniques.[ndx].GetProbTrue())
                             (fulls.[ndx].GetProbTrue())
                             (imms.[ndx].GetProbTrue())
                             (shares.[ndx].GetProbTrue())
                             (pures.[ndx].GetProbTrue())
                             (frames.[ndx].GetProbTrue())


//        let printBorrowing n1 n2 maps (ie:InferenceEngine) : unit =
//            printfn "Borrowing (%s,%s)" n1.id n2.id;
//            let b_var = maps.borrow.Item (n1.id,n2.id) in
//            printfn "Result: %f" ((ie.Infer<Bernoulli>(b_var)).GetProbTrue())
//

                        
        let recreateGuarantee (results:float list) (thresh:float) maps (hierarchy:hierarchy_Info) =
            let (max_ndx,max_val,_) =
              List.fold
                  (fun (max_ndx,max_val,cur_ndx) r ->
                       if r > max_val then (cur_ndx,r,cur_ndx+1) else (max_ndx,max_val,cur_ndx+1)) (0,0.0,0) results
            in
            let g_ = Map.tryFindKey (fun k v -> v = max_ndx) (hierarchy.array_ndxs) in
            if g_.IsSome && max_val > thresh then g_.Value else "alive"

        let recreateStates (results:float list) (thresh:float) maps hierarchy =
            let (max_ndx,max_val,_) =
              List.fold
                  (fun (max_ndx,max_val,cur_ndx) r ->
                       if r > max_val then (cur_ndx,r,cur_ndx+1) else (max_ndx,max_val,cur_ndx+1)) (0,0.0,0) results
            in
            let g_ = Map.tryFindKey (fun k v -> v = max_ndx) (hierarchy.array_ndxs) in
            if g_.IsSome
            then
//                let _ = printfn "State %s found for node, at value %f" g_.Value max_val in
                if max_val > thresh then [g_.Value] else ["alive"]
            else
                ["alive"]

//            && max_val > thresh then [g_.Value] else ["alive"]

//        let recreateUsage node (ie:InferenceEngine) threshold node_maps : perm_Use =
//            let var = node_maps.frame.Item(node.id) in
//            let b = (ie.Infer<Bernoulli>(var)).GetProbTrue() > threshold in
//            if b then Frame else Virtual

        let recreatePermission (ie:InferenceEngine) (thresh:float) (vu,vf,vi,vs,vp) fraction guarantee states usage = 
            // Find the max of all inferred values
            let max = (Unique,vu) in
            let max = if vf > snd max then (Full,vf) else max in
            let max = if vi > snd max then (Immutable,vi) else max in
            let max = if vs > snd max then (Share,vs) else max in
            let max = if vp > snd max then (Pure,vp) else max in
            // if max > thresh, we have our permission kind
            if snd max > thresh 
            then
                ConcretePerm([{kind=fst max; guarantee=guarantee; states=states;fract=fraction;usage=usage}])
            else
                // otherwise empty permission
                ConcretePerm([])




        /// Update the permission for the node specifics
        let recreateSpecifics ie thresh specifics perm_vars fraction guarantee states usage =
            // recreate the perm
            // for now, there's not much recreation going on here
            // we're just outputting the permission we want to!
            let rPerm p = 
                recreatePermission ie thresh perm_vars fraction guarantee states usage
            in
            match specifics with
                | CalledRcvr(k,m,d,s,perm,isPrivate) -> CalledRcvr(k,m,d,s, rPerm perm, isPrivate)
                | CalledReturn(k,m,s,perm) -> CalledReturn(k,m,s, rPerm perm)
                // field load and store creates new nodes that point to strings
                | FieldLoad(f,s,is,perm,{contents=Init(ns)}) -> 
                    FieldLoad(f,s,is, rPerm perm, ref (Uninit(List.map (fun n -> n.id) ns)))
                | FieldStore(f,s,is,perm,{contents=Init(ns)}) ->
                    FieldStore(f,s,is, rPerm perm, ref (Uninit(List.map (fun n -> n.id) ns)))
                | Receiver(d,perm) -> Receiver(d, rPerm perm)
                | Return(perm) -> Return(rPerm perm)
                | StandardArg(k,m,d,s,pos,perm) -> StandardArg(k,m,d,s,pos, rPerm perm)
                | StandardParameter(n,d,pos,perm) -> StandardParameter(n,d,pos,rPerm perm)
                | Merge(perm) -> Merge(rPerm perm)
                | Split(perm) -> Split(rPerm perm)
                | _ -> raise Impossible // Will happen if fields are initialized: BAD.

        /// Given the lists of new and old nodes (zipped together)
        /// mutate all of the neighbors lists in the new nodes, finding
        /// the new neighbors using the id_node_map.
        let connectEdges new_and_old_nodes (id_node_map:IDictionary<string,node>) = 
            let rec helper nodes =
                match nodes with
                    | [] -> ()
                    | (new_node,old_node)::tl ->
                        // for each neighbor of the old node, look up the new node
                        new_node.succs := (List.map (fun succ -> id_node_map.Item (succ.id)) (!old_node.succs))
                        new_node.preds := (List.map (fun pred -> id_node_map.Item (pred.id)) (!old_node.preds))
                        helper tl
            in
            helper new_and_old_nodes

        /// Connect fields to their receivers
        let connectField node (id_node_map:IDictionary<string,node>) : unit = 
            let updateFn id = id_node_map.Item id in
            match node.specifics with 
                | FieldLoad(_,_,_,_,({contents=Uninit(ns)} as rcvrs)) -> 
                    rcvrs := Init(List.map updateFn ns)
                | FieldStore(_,_,_,_,({contents=Uninit(ns)} as rcvrs)) ->
                    rcvrs := Init(List.map updateFn ns)
                | FieldLoad(_) -> raise Impossible
                | FieldStore(_) -> raise Impossible
                | _ -> ()

        // Generate a map from node ids to fractions
        // this function solves the borrowing constraints for PRE/POST params, and if 
        // borrowing is likely, it creates a new fraction that is identical for both
        // pre and post nodes.
        let genFractionFunction (ie:InferenceEngine) (old_graph:method_Graph) (node_maps) f_threshold : Map<node_ID,fraction> =
            let original_nodes = old_graph.nodes in
            let borrowing_results = ie.Infer<DistributionArray<Bernoulli>>(node_maps.borrow) in
            let rec helper ns result =
                match ns with
                    | [] -> result
                    | hd::tl -> 
                        // new fraction, for if we need one
                        let f = (new System.Object()).GetHashCode().ToString() in
                        match hd.specifics with
                            | StandardParameter(name,Pre,pos,_) -> 
                                // solve borrowing variable
                                let post = findParam Post pos original_nodes in
                                let ndx = Map.find (hd.id,post.id) node_maps.borrow_Ndxs in
                                let r = borrowing_results.[ndx].GetProbTrue() in
                                // this is definitely f, but what is post?
                                let result = Map.add hd.id f result in
                                if r > f_threshold 
                                then
                                    // add post w/ same fraction
                                    let result = Map.add post.id f result in
                                    helper tl result
                                else helper tl result
                            | Receiver(Pre,_) ->
                                // solve for borrowing
                                let post = findRcvr Post original_nodes in
                                let ndx = Map.find (hd.id,post.id) node_maps.borrow_Ndxs in
                                let r = borrowing_results.[ndx].GetProbTrue() in
                                // this is definitely f, but what is post?
                                let result = Map.add hd.id f result in
                                if r > f_threshold 
                                then
                                    // add post w/ same fraction
                                    let result = Map.add post.id f result in
                                    helper tl result
                                else helper tl result
                            | _ -> 
                                let result = if result.ContainsKey hd.id then result else Map.add hd.id f result in
                                helper tl result
            in
            helper original_nodes Map.empty

        /// Go through the previous method graph and replace all of the old permissions with the
        /// new, inferred permissions.
        let recreateGraphWithInferredPerms ie old_graph node_maps threshold f_threshold (hierarchies:hierarchy_Maps) =
            // map will be mutated as the new nodes are created
            let id_node_map = new Dictionary<string,node>() in
            let fraction_function = genFractionFunction ie old_graph node_maps f_threshold in
            // do inference...
            let (u_results,f_results,i_results,s_results,p_results) =
              (ie.Infer<DistributionArray<Bernoulli>>(node_maps.unique_perms),
               ie.Infer<DistributionArray<Bernoulli>>(node_maps.full_perms),
               ie.Infer<DistributionArray<Bernoulli>>(node_maps.imm_perms),
               ie.Infer<DistributionArray<Bernoulli>>(node_maps.share_perms),
               ie.Infer<DistributionArray<Bernoulli>>(node_maps.pure_perms))
            in
            let guar_results = ie.Infer<DistributionArray<Bernoulli>>(node_maps.guarantee) in
            let state_results = ie.Infer<DistributionArray<Bernoulli>>(node_maps.state) in
            let fr_results = ie.Infer<DistributionArray<Bernoulli>>(node_maps.frame) in
            // then, edges must be reconnected.
            let nodeHelper old_node = 
                let id = old_node.id in
                let tyname = old_node.tyname in
                let fract = fraction_function.Item id in
                let s_ndxs = Map.find id node_maps.state_Ndxs in
                let g_results_range = List.map (fun ndx -> guar_results.[ndx].GetProbTrue()) s_ndxs in
//                let _ = printfn "Node is %s" id in
                let guarantee = recreateGuarantee g_results_range threshold node_maps (hierarchies.Item(tyname)) in
                let s_results_range = List.map (fun ndx -> state_results.[ndx].GetProbTrue()) s_ndxs in
                let states = recreateStates s_results_range threshold node_maps  (hierarchies.Item(tyname)) in
                let ndx = Map.find old_node.id node_maps.node_Ndxs in
                let usage = if fr_results.[ndx].GetProbTrue() > threshold then Frame else Virtual in
                // get probs true for this node
                let probs_true =
                    (u_results.get_Item(ndx).GetProbTrue(),
                     f_results.get_Item(ndx).GetProbTrue(),
                     i_results.get_Item(ndx).GetProbTrue(),
                     s_results.get_Item(ndx).GetProbTrue(),
                     p_results.get_Item(ndx).GetProbTrue())
                in
                let new_specs = 
                    recreateSpecifics ie threshold old_node.specifics probs_true fract guarantee states usage
                in
                let new_node = {id=id;succs=ref []; preds=ref []; specifics=new_specs; tyname=tyname; synced=old_node.synced} in
                let _ = id_node_map.Add(id, new_node) in
                new_node
            in
            let new_nodes = List.map nodeHelper old_graph.nodes in
            let () = connectEdges (List.zip new_nodes old_graph.nodes) id_node_map in
            let () = List.iter (fun n -> connectField n id_node_map) new_nodes in
            { old_graph with nodes=new_nodes }

        let doInferAll (ie:InferenceEngine) method_maps : unit =

            let s:seq<IVariable> =
                seq {
                    // build an enumerable by casting each permission array
                    yield (method_maps.unique_perms:>IVariable)
                    yield (method_maps.full_perms:>IVariable)
                    yield (method_maps.imm_perms:>IVariable)
                    yield (method_maps.share_perms:>IVariable)
                    yield (method_maps.pure_perms:>IVariable)
                    // also arrays for edges
                    // This won't be necessary (pre-infering edges)
                    // once we are no longer printing out the edge
                    // values.
                    yield (method_maps.unique_e_perms:>IVariable)
                    yield (method_maps.full_e_perms:>IVariable)
                    yield (method_maps.imm_e_perms:>IVariable)
                    yield (method_maps.share_e_perms:>IVariable)
                    yield (method_maps.pure_e_perms:>IVariable)

                    yield (method_maps.borrow:>IVariable)
                    yield (method_maps.state:>IVariable)
                    yield (method_maps.guarantee:>IVariable)
                    yield (method_maps.frame:>IVariable)
                }
            in
            ie.set_NumberOfIterations(-1); // -1 is default
            ie.InferAll(s)

        let normalProbsMap =
            let result =
                [| ("UnknownState",0.5);
                   ("NoState",0.01);
                   ("ConcreteStateTrue",0.9999);
                   ("ConcreteStateFalse",0.0001);
                   ("ConcreteGuarTrue",0.99);
                   ("ConcreteGuarFalse",0.01);
                   ("BorrowTrue",0.99);
                   ("BorrowFalse",0.1);
                   ("ContingentBorrowUK",0.7);
                   ("BorrowingUK",0.5);
                   ("FrameArg",0.4);
                   ("FrameTrue",0.99);
                   ("VirtualTrue",0.3);
                   ("FrameNone",0.01);
                   ("FrameUK",0.5);
                   ("PermTrue",0.99);
                   ("PermFalse",0.01);
                   ("PermUGCSFull",0.7);
                   ("PermUGCSUniqueShare",0.5);
                   ("PermUGCSImmPure",0.5);
                   ("PermUGLow",0.4);
                   ("PermUGHigh",0.5);
                   ("FrameEdge",0.4);
                 |]
            in
            Map.ofArray result
            
        let detProbsMap =
            let result =
                [| ("UnknownState",0.5);
                   ("NoState",0.0);
                   ("ConcreteStateTrue",1.0);
                   ("ConcreteStateFalse",0.0);
                   ("ConcreteGuarTrue",1.0);
                   ("ConcreteGuarFalse",0.0);
                   ("BorrowTrue",1.0);
                   ("BorrowFalse",0.0);
                   ("ContingentBorrowUK",0.5);
                   ("BorrowingUK",0.5);
                   ("FrameArg",0.5);
                   ("FrameTrue",1.0);
                   ("VirtualTrue",0.0);
                   ("FrameNone",0.0);
                   ("FrameUK",0.5);
                   ("PermTrue",1.0);
                   ("PermFalse",0.0);
                   ("PermUGCSFull",0.5);
                   ("PermUGCSUniqueShare",0.5);
                   ("PermUGCSImmPure",0.5);
                   ("PermUGLow",0.5);
                   ("PermUGHigh",0.5);
                   ("FrameEdge",0.5);
                 |]
            in
            Map.ofArray result

        // perform global inference...
        // I am making assumptions that all the called methods will work just the same as they did in
        // local inference, so any bugs may be due to this being an invalid assumption...
        let inferGlobal (g:graph) (constraints: nodal_Constraint list) (cnstr_params:Map<string,float>) verbose deterministic : graph =
            let cnstr_params =
                if deterministic
                then loadParameters cnstr_params
                else deterministicParameters cnstr_params
            in
            let all_nodes = 
                List.reduce (List.append) (List.map (fun m -> m.nodes) g.methods)
            in
            let () = printfn "Loading state hierarchies..." in
            let hierarchies = mapsFromHierarchies g.hierarchies in
            let () = printfn "Done." in
            let () = printfn "Building variable data structures..." in
            let priors = if deterministic then detProbsMap else normalProbsMap in
            let method_maps = createVariables all_nodes constraints hierarchies priors in
            let () = printfn "Done." in
            let () = printfn "Applying constraints to variables..." in
            let () = applyProgramConstraints constraints method_maps cnstr_params in
            let () = printfn "Done." in
            // now go through the nodes and return the probability for each kind
            let ie = InferenceEngine() in
            let _  = ie.ShowProgress <- true in
            // call inferall on the engine
            let () = printfn "Running Infer.NET..." in
            let _ = doInferAll ie method_maps in
            let () = printfn "Done." in
            let hierarchies = mapsFromHierarchies g.hierarchies in
            let () = printfn "Rebuilding graph..." in
            let new_methods =
                List.map( fun m -> recreateGraphWithInferredPerms ie m method_maps cnstr_params.ipt cnstr_params.bth hierarchies) g.methods
            in
            let () = printfn "Done."
            // DEBUG CODE
            let () =
                if verbose then
                    let _ = printConstraints constraints in
                    let _ = printNodesResult all_nodes method_maps ie in 
                    let _ = printEdgesResult all_nodes method_maps ie in
                    ()
                else ()
            in
            {methods=new_methods; hierarchies=g.hierarchies}
