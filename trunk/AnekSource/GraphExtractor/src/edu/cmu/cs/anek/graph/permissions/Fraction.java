package edu.cmu.cs.anek.graph.permissions;

/**
 * A Fraction is an abstract representation of a permission fraction.
 * It never holds a literal fraction value, but its identity serves
 * to denote whether or not two fractions must be equal. This is a
 * pretty dumb object. It's really just an ID.  
 * 
 * @author Nels E. Beckman
 *
 */
public final class Fraction {

    public String fractionID() {
        return Integer.toString(System.identityHashCode(this));
    }
    
}
