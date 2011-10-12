package edu.cmu.cs.anek.graph.permissions;

import static edu.cmu.cs.anek.util.Utilities.impossible;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Fulls;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Imms;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.Pures;
import edu.cmu.cs.plural.annot.ResultFull;
import edu.cmu.cs.plural.annot.ResultImm;
import edu.cmu.cs.plural.annot.ResultPure;
import edu.cmu.cs.plural.annot.ResultShare;
import edu.cmu.cs.plural.annot.ResultUnique;
import edu.cmu.cs.plural.annot.Share;
import edu.cmu.cs.plural.annot.Shares;
import edu.cmu.cs.plural.annot.Unique;
import edu.cmu.cs.plural.annot.Uniques;

/**
 * The five permission kinds.
 * @author Nels E. Beckman
 *
 */
public enum PermissionKind {
    UNIQUE, FULL, SHARE, PURE, IMMUTABLE;
    
    /**
     * Given a permission kind, this will return the class of
     * the permission annotation is it associated with.
     * e.g., UNIQUE -> @Unique.class
     */
    public static Class<?> kindAnnotation(PermissionKind kind) {
        switch(kind) {
        case UNIQUE: return Unique.class;
        case FULL: return Full.class;
        case SHARE: return Share.class;
        case PURE: return Pure.class;
        case IMMUTABLE: return Imm.class;
        }
        return impossible();
    }
    
    /**
     * Given a permission kind, this will return the class of
     * the permission aggregator is it associated with.
     * e.g., UNIQUE -> @Uniques.class
     */
    public static Class<?> kindsAnnotation(PermissionKind kind) {
        switch(kind) {
        case UNIQUE: return Uniques.class;
        case FULL: return Fulls.class;
        case SHARE: return Shares.class;
        case PURE: return Pures.class;
        case IMMUTABLE: return Imms.class;
        }
        return impossible();
    }
    
    /**
     * Given a permission kind, this will return the class of
     * the permission result is it associated with.
     * e.g., UNIQUE -> @ResultUnique.class
     */
    public static Class<?> kindResultAnnotation(PermissionKind kind) {
        switch(kind) {
        case UNIQUE: return ResultUnique.class;
        case FULL: return ResultFull.class;
        case SHARE: return ResultShare.class;
        case PURE: return ResultPure.class;
        case IMMUTABLE: return ResultImm.class;
        }
        return impossible();
    }
}
