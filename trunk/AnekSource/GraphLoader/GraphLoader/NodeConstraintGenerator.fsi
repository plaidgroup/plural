//
// NodeConstraintGenerator.fsi
// Nels E. Beckman
// Signature for constraint generation module. We'll 
// generate a series of Infer.NET constraints from the
// program graph.
//
// अनेक
// Anek
//
namespace Anek
    module NodeConstraintGenerator = 

        val generateConstraints : ProgramGraph.graph -> bool -> bool -> NodeConstraints.nodal_Constraint list 
        
        val generateMethodConstraints : bool -> ProgramGraph.method_Graph -> NodeConstraints.nodal_Constraint list
