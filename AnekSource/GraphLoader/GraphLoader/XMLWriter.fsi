// 
// XMLWriter.fsi
// Nels E. Beckman
// Interface for writing a graph to XML format in a file.
//
// अनेक
// Anek
//
namespace Anek
    module XMLWriter =

        /// write the given graph to the file given by the path 
        /// argument
        val writeGraphToFile : Anek.ProgramGraph.graph -> string -> unit
