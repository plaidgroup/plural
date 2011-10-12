
//
// Utilities.fs
// Nels E. Beckman
// Utilities types and functions
//
// अनेक
// Anek
//

namespace Anek
    module Utilities =
        exception NYI
        exception Impossible
        exception NoKey of string

        /// Given a list of lists, each of which may be a different length,
        /// returns a list of lists where now all the lists are the same length,
        /// which is equal to the length of the previous longest list, and where
        /// the remaining elements of each shorter list (the tail) is filled in
        /// with the given filler element.
        val makeRectangleConst : 'a list list -> 'a -> 'a list list

        /// The given function is passed the index of the current list.
        /// The returned element will be used to populate the rest of the list.
        val makeRectangle : 'a list list -> (int -> 'a) -> 'a list list

        module Multimap =
            type multimap<'a,'b> when 'a : comparison = Map<'a,'b list>

            val empty : multimap<'a,'b>
            val add : 'a -> 'b -> multimap<'a,'b> -> multimap<'a,'b> when 'a : comparison
            val find : 'a -> multimap<'a,'b> -> 'b list when 'a : comparison
            val tryFind : 'a -> multimap<'a,'b> -> 'b list option when 'a : comparison
