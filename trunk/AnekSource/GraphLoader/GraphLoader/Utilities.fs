//
// Utilities.fs
// Nels E. Beckman
// Utilities implementation
//
// अनेक
// Anek
//
namespace Anek
    module Utilities =
        exception NYI
        exception Impossible
        exception NoKey of string


        let makeRectangle lol filler_fn =
            let max_len =
                List.fold (fun max l -> let l_len = List.length l in if l_len > max then l_len else max) 0 lol
            in
            let nCopies start end_ = [ for i in start..end_ -> filler_fn i ] in
            List.map
                (fun l ->
                     let l_len = List.length l in
                     if l_len < max_len
                     then l @ (nCopies l_len (max_len-1)) // add filler to the end of this list
                     else l)
                lol


        let makeRectangleConst lol filler = makeRectangle lol (fun _ -> filler)

        
        module Multimap =
            type multimap<'a,'b> when 'a : comparison = Map<'a,'b list>

            let empty : multimap<'a,'b> when 'a : comparison = Map.empty
            
            let add key v (m:multimap<'a,'b>) = 
              match Map.tryFind key m with
                  | None -> Map.add key [v] m
                  | Some(vs) -> Map.add key (v::vs) m
                
            let find k (m:multimap<'a,'b>) = Map.find k m
            
            let tryFind k (m:multimap<'a,'b>) = Map.tryFind k m
