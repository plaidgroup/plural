
//
// Program.fs
// Nels E. Beckman
// Main program. Load a file from the command-line...
// output results..
//
// अनेक
// Anek
//

exception BadInput of string

// Generate a map of parameter/value pairs
let constrParametersFromFile filename =
    let lines = List.ofSeq(System.IO.File.ReadAllLines(filename)) in
    let rec helper (ls:string list) = 
        match ls with
            | [] -> Map.empty
            | line::ls ->
                (match List.ofSeq(line.Split('=')) with
                    | prop::value::_ -> 
                        try 
                            (helper ls).Add(prop, System.Double.Parse(value)) 
                        with
                            | :? System.FormatException -> raise (BadInput ("All values in parameter file must be floats: " + line))
                    | _ -> raise (BadInput("Constraint parameters file did not have the format KEY=VALUE on this line: " + line)))
    in  
    helper lines

let isVerbose = Array.exists (fun a -> a = "-v")

let isDeterministic = Array.exists (fun a -> a = "-d")

let usage () =
    System.Console.WriteLine "Usage: GraphLoader.exe <ProgramGraph.GRAPHML> <OutputGraph.GRAPHML> <ConstraintParameters.TXT> [-v] [-d]"

[<EntryPoint>]
let Main arg = 
    if arg.Length < 3 
    then usage(); 0
    else 
       // try
            // load the graph from XML
            let _ = System.Console.WriteLine("Loading XML...") in
            let graph = Anek.XMLLoader.loadGraph (arg.[0]) in
            let _ = System.Console.WriteLine("Done.") in
            let _ = System.Console.WriteLine("Loading Parameters...") in
            let cnstr_params = constrParametersFromFile (arg.[2]) in
            let _ = System.Console.WriteLine("Done.") in
            let verbose = isVerbose arg in
            let deterministic = isDeterministic arg in
            // infer graph permissions
            let stopwatch = new System.Diagnostics.Stopwatch() in
            let () = stopwatch.Start() in
            let _ = System.Console.WriteLine("Generating constraints...") in
            let constraints = Anek.NodeConstraintGenerator.generateConstraints graph verbose deterministic in
            let _ = System.Console.WriteLine("Done.") in
            let new_graph = 
                try
                    let _ = System.Console.WriteLine("Starting prob. inference...") in
                    Anek.ProbConstraints.inferGlobal graph constraints cnstr_params verbose deterministic 
                with
                    Anek.Utilities.NoKey(s) -> raise (BadInput("Expected parameter was not found in parameters file: " + s))
            in 
            // now output the result
            stopwatch.Stop();
            System.Console.WriteLine("Elapsed inference time: {0}", stopwatch.Elapsed.ToString());
            Anek.XMLWriter.writeGraphToFile new_graph (arg.[1]);
            0
       // with
         //   BadInput(s) -> System.Console.WriteLine(s); 1
           // | e -> System.Console.WriteLine(e);1
