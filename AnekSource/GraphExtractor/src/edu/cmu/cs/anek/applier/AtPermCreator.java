package edu.cmu.cs.anek.applier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.plural.annot.Perm;

/**
 * Instances of this interfaces can be used to create
 * the @Perm annotation for a method. The whole reason
 * that this type is necessary is that one often cannot
 * tell what kind of permission they are going to have
 * to write (@Kind or @Perm) until they look at individual
 * parameters and the receiver. Instances of this class
 * take potentially several parameters that need an @Perm
 * annotations and gracefully merges them together into
 * one @Perm, since there can only be one per method!
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 *
 */
public final class AtPermCreator {

    public static final AtPermCreator EMPTY = new AtPermCreator();
    
    
    private final List<String> preStrings;
    private final List<String> postStrings;
    
    public AtPermCreator(String pre, String post) {
        this(Collections.singletonList(pre),
                Collections.singletonList(post));
    }
    
    private AtPermCreator() {
        this(Collections.<String>emptyList(),Collections.<String>emptyList());
    }
    
    private AtPermCreator(List<String> preStrings, List<String> postStrings) {
        this.preStrings = preStrings;
        this.postStrings = postStrings;
    }

    /**
     * Generate this @Perm annotation, via diff, 
     * given the file offset where it should begin
     * (typically the method declaration's starting
     * offset). 
     */
    public AnnotationDiff toDiff(int offset) {
        if( preStrings.isEmpty() && postStrings.isEmpty() )
            return NullDiff.INSTANCE;
        
        StringBuilder result = new StringBuilder("@Perm(requires=\"");
                
        // REQUIRES
        for( String pre : preStrings ) {
            result.append(pre);
            result.append(" * ");
        }
        if( !preStrings.isEmpty() ) {
            // remove last ' * '
            result.delete(result.length()-3, result.length());
        }
        result.append("\", ensures=\"");
        
        // ENSURES
        for( String post : postStrings ) {
            result.append(post);
            result.append(" * ");
        }
        if( !postStrings.isEmpty() ) {
            // remove last ' * '
            result.delete(result.length()-3, result.length());
        }
        
        result.append("\")");
        
        String newline = System.getProperty("line.separator");
        result.append(newline);
        return new InsertDiff(offset, result.toString(),
                Perm.class.getName());
    }
    
    /**
     * Combine two AtPermCreators, so that the diff when
     * created will include text for both.
     */
    public AtPermCreator combine(AtPermCreator other) {
        final List<String> preStrings = new LinkedList<String>(this.preStrings);
        preStrings.addAll(other.preStrings);
        final List<String> postStrings = new LinkedList<String>(this.postStrings);
        postStrings.addAll(other.postStrings);
        return new AtPermCreator(preStrings, postStrings);
    }
}
