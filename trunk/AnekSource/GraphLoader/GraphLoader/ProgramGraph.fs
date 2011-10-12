//
// ProgramGraph.fs
// Nels E. Beckman
// The types needed to construct an in-memory graph of our program abstraction.
//
// अनेक
// Anek
//
namespace Anek
    module ProgramGraph =
        // methods & fields
        type site_ID = int64
        type method_Name = string
        type method_Key = string
        type field_Name = string
        type param_Name = string
        type type_Name = string
        type pos = int
        // 
        type perm_Use = Frame | Virtual | Both
        // state hierarchies
        type state_Hierarchy_Node = string // For now... maybe a reference to something in the hierarchy?
        type state = SLeaf of state_Hierarchy_Node | SNode of state_Hierarchy_Node * dim list
        and dim = state_Hierarchy_Node * state * state list
        // Permissions
        type fraction = string // we just need identity here... Using string ID
        type permission_Kind = Unique | Full | Immutable | Share | Pure
        type concrete_Perm_Element = { kind:permission_Kind; guarantee:state_Hierarchy_Node; 
                                       states:state_Hierarchy_Node list; fract:fraction; usage:perm_Use }
        // unground permission has the ID of the node it is associated with, so we can reassociate
        type permission = UnGroundPerm | ConcretePerm of concrete_Perm_Element list
        // nodes
        and node_ID = string
        type parameter_Direction = Pre | Post
        type static_Field = Static | Instance
        type node_Specifics =
            CalledRcvr of method_Key * method_Name option * parameter_Direction * site_ID * permission * bool // bool = isPrivate
            | CalledReturn of method_Key * method_Name option * site_ID * permission
            | FieldLoad of field_Name * site_ID * static_Field * permission * field_Node_Ref ref
            | FieldStore of field_Name * site_ID * static_Field * permission * field_Node_Ref ref
            | Receiver of parameter_Direction * permission
            | Return of permission
            | StandardArg of method_Key * method_Name option * parameter_Direction * site_ID * pos * permission
            | StandardParameter of param_Name * parameter_Direction * pos * permission
            | Split of permission
            | Merge of permission
        and node = { id:node_ID; succs:node list ref;preds:node list ref; specifics:node_Specifics; tyname:string; synced:bool; }
        and field_Node_Ref = Uninit of string list | Init of node list
        // graphs
        type method_Graph = { methodKey:method_Key; methodName:method_Name; isConstructor:bool; nodes:node list; overridden:method_Key list }
        type graph = { methods:method_Graph list; hierarchies:(type_Name * state) list }


        //
        // UTILITY FUNCTIONS
        //

        /// Given a graph node, returns its permission.
        let nodePermission node =
            match node.specifics with
            | CalledRcvr(_,_,_,_,p,_) -> p
            | CalledReturn(_,_,_,p) -> p
            | FieldLoad(_,_,_,p,_) -> p
            | FieldStore(_,_,_,p,_) -> p
            | Receiver(_,p) -> p
            | Return(p) -> p
            | StandardArg(_,_,_,_,_,p) -> p
            | StandardParameter(_,_,_,p) -> p
            | Split(p) -> p
            | Merge(p) -> p

        /// For the given permission kind, returns all of the permission kinds
        /// that can be split into that permission kind. The returned list
        /// will NOT include the permission itself, for users who don't need
        /// that.
        let stronger k =
            match k with 
                | Unique -> []
                | Full -> [Unique]
                | Immutable -> [Unique;Full]
                | Pure -> [Share;Full;Immutable;Unique]
                | Share -> [Full;Unique]

        /// Returns all of the permission kinds that the given permission kind
        /// can be used to satisfy. It does NOT include the given k itself
        /// for clients who do not need that behavior.
        let weaker k =
            match k with
                | Unique -> [Full;Immutable;Share;Pure]
                | Full -> [Immutable;Share;Pure]
                | Immutable -> [Pure]
                | Share -> [Pure]
                | Pure -> []
                

        /// The permission kinds that have the right to read
        let allPerms = [Full;Immutable;Share;Pure;Unique]

        /// The permission kinds that have the right to write
        let writingPerms = [Full;Share;Unique]
        
        let tryFindArg dir mkey site pos nodes =
            List.tryFind
                (fun n ->
                    match n.specifics with
                        | StandardArg(mkey_,_,dir_,site_,pos_,_) when mkey=mkey_ && dir=dir_ && site=site_ && pos=pos_ -> true
                        | _ -> false)
                nodes            

        /// Find a standard arg in the list of nodes
        let findArg dir mkey site pos nodes =
            let result = tryFindArg dir mkey site pos nodes in
            result.Value

        let tryFindCalledRcvr dir mkey site nodes =
            List.tryFind
                (fun n ->
                    match n.specifics with
                        | CalledRcvr(mkey_,_,dir_,site_,_,_) when mkey=mkey_ && dir=dir_ && site=site_ -> true
                        | _ -> false)
                nodes            

        /// Find a CalledRcvr in the list of nodes
        let findCalledRcvr dir mkey site nodes =
            let result = tryFindCalledRcvr dir mkey site nodes in
            result.Value

        /// Find a StandardParam in the given list of nodes. 
        let findParam dir pos nodes =
            List.find
                (fun n ->
                    match n.specifics with
                        | StandardParameter(_,dir_,pos_,_) when dir=dir_ && pos=pos_ -> true
                        | _ -> false)
                nodes

        /// Find a Receiver in the given list of nodes
        let findRcvr dir nodes =
            List.find
                (fun n ->
                    match n.specifics with
                        | Receiver(dir_,_) when dir=dir_ -> true
                        | _ -> false)
                nodes

        
        /// Find a Return in the given list of nodes
        let findReturn nodes =
            List.find
                (fun n->
                    match n.specifics with
                        | Return(_) -> true
                        | _ -> false)
                nodes
