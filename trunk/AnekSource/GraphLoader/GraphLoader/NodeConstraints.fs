//
// NodeConstraints.fsi
// Nels E. Beckman
// Constraint types. This module defines a bunch of constraints
// between concrete and unground permissions. 
//
// अनेक
// Anek
//
namespace Anek
    module NodeConstraints =
        open Anek.ProgramGraph
        // The node constraints are meant to be as simple as possible and to preserve
        // all possible information until the next step.

        type nodal_Constraint =
            // nodes 1 and 2 have the same permission
            Equal of Anek.ProgramGraph.node * Anek.ProgramGraph.node
            
            // An argument is the same as a parameter. The four components are
            // ARG_PRE, ARG_POST, PARAM_PRE, PARAM_POST, is method private?
            | ArgIsParam of node * node * node * node * bool
                        
            // node 1 has the same or a stronger perm than node 2
            //| EqualOrStronger of Anek.ProgramGraph.node * Anek.ProgramGraph.node
            
            // a possible split. passes along the SPLIT, the PRE, the POST and the MERGE
            // if (PRE,POST) is borrowed, SPLIT=MERGE
            | MethodSplit of node * node * node * node

            // the node must have some ability to write
            | MustWrite of Anek.ProgramGraph.node
            // the node must have some ability to read
            | MustRead of Anek.ProgramGraph.node
            // needs a frame permission
            | NeedsFrame of node
            // Frame of a node should equal an edge
            | FramesSame of node * (node_ID*node_ID)
            
            // Very unlikely that it borrows b/c there is a path to a field store
            | CannotBorrow of node * node
            // Very likely that it borrows since there are no splits between the PRE & POST
            | CanBorrow of node * node
            // Borrowing is contingent upon the borrowing of these other arg pre/posts
            | ContingentBorrow of node * node * (node * node) list

            // Constraints that exist now that edges can be modeled, also known as modeling the
            // splitting of fractions. These constraints are hopefully the simplest and most
            // general.

            // The node's permission is equal to one of its incoming edges
            // TODO, given our new terminology this should be called merge or something
            | IncomingEdges of node list * node
            // The edges each may be equal to the node
            | OutgoingEdges of node * node list
            // The node's permission is stronger than all of its outgoing edges,
            // AND they are consistent with one another.
            | SplitEdges of node * node list

            // States
            // The state on node 1 is equal to the state on the EDGE between node 1 and node 2
            | StatesSame of node * (node_ID*node_ID)

            // Heuristic constraints
            // All heuristic constraints are proceeded by H (obviously not...)

            // These nodes are 'likely' to be equal, although not necessarily...
            | HEqual of node * node
            // This node is likely to be unique (or unlikely to be anything else)
            | HCreateUnique of node
            // Getters and setter methods usally need reading or writing rcvr perms
            | HGetter of node
            | HSetter of node
            // This node is synchronized
            | HSynchronized of node
            // This node is a constructor return...
            | HConstructor of node
            // This node has a successor that is a concrete permission.
            | ConcreteSuccessor of node
