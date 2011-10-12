
// 
// ProbConstraints.fsi
// Nels E. Beckman
// Interface for probabilistic constraint module. Probabilistic constraints
// tighly coupled to the infer.NET API, and they are created from
// program constraints.
//
// अनेक
// Anek
//
namespace Anek
    module ProbConstraints =

        /// Given a program graph and a list of constraints over that
        /// graph, return a new graph that has no unground permissions.
//        val infer : Anek.ProgramGraph.method_Graph -> 
//                    Anek.NodeConstraints.nodal_Constraint list -> 
//                    Map<string,float> -> Anek.ProgramGraph.method_Graph 

        /// Perform global permission inference. Given a graph, a list of
        /// constraints and a map containing inference parameters, 
        /// returns a brand new graph, with inferred permissions inserted
        /// for unground permissions.
        val inferGlobal : Anek.ProgramGraph.graph ->
                          Anek.NodeConstraints.nodal_Constraint list ->
                          Map<string,float> -> bool -> bool -> Anek.ProgramGraph.graph
