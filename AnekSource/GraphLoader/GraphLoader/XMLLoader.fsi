// 
// XMLLoader.fs
// Nels E. Beckman
// Interface for loading an xml file meeting the graphml+plural.xsd schema and generating a
// graph type.
//
// अनेक
// Anek
//
namespace Anek
    module XMLLoader =
        exception BadXML // Thrown when the XML does not match the schema

        // given an file name for the xml file that contains the graph representation
        // returns the graph of the program
        val loadGraph : string -> ProgramGraph.graph