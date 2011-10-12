package edu.cmu.cs.anek.applier;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * A change to apply to the code buffer.
 * <br>
 * अनेक<br>
 * Anek<br>
 * 
 * @author Nels E. Beckman
 *
 */
public interface AnnotationDiff {
    /**
     * Returns the types this diff has added to the document,
     * if any.
     */
    public Set<String> typesAdded();
    /**
     * When called, if the receiver diff has text edits to apply, it
     * will add them to the given MultiTextEdit. 
     */
    public void addDiffToEdit(MultiTextEdit mte);
}

final class MultipleDiff implements AnnotationDiff {

    private final Iterable<AnnotationDiff> diffs;
    
    public MultipleDiff(Iterable<AnnotationDiff> diffs) {
        this.diffs = diffs;
    }

    @Override
    public void addDiffToEdit(MultiTextEdit mte) {
        for( AnnotationDiff d : diffs ) {
            d.addDiffToEdit(mte);
        }
    }

    @Override
    public Set<String> typesAdded() {
        Set<String> result = new HashSet<String>();
        for( AnnotationDiff d : diffs ) {
            result.addAll(d.typesAdded());
        }
        return result;
    }
}

final class NullDiff implements AnnotationDiff {

    public static final AnnotationDiff INSTANCE = new NullDiff();

    @Override
    public void addDiffToEdit(MultiTextEdit mte) {
        // DOES NOTHING
    }

    @Override
    public Set<String> typesAdded() {
        return Collections.emptySet();
    }
    
}

final class InsertDiff implements AnnotationDiff {
    private final TextEdit edit;
    
    private final Set<String> typeUsed;
    
    public InsertDiff(int replaceStart, 
            String text, String type_used) {
        this(replaceStart, text, Collections.singleton(type_used));
    }

    public InsertDiff(int replaceStart, 
            String text, Set<String> type_used) {
        this.edit = new InsertEdit(replaceStart,text);
        this.typeUsed = type_used;
    }
    
    @Override
    public void addDiffToEdit(MultiTextEdit mte) {
        mte.addChild(edit);
    }

    @Override
    public Set<String> typesAdded() {
        return Collections.unmodifiableSet(typeUsed);
    }
}

final class RemoveDiff implements AnnotationDiff {

    private final TextEdit edit;
    
    public RemoveDiff(int offset, int length) {
        this.edit = new DeleteEdit(offset,length);
    }

    @Override
    public void addDiffToEdit(MultiTextEdit mte) {
        mte.addChild(edit);
    }

    @Override
    public Set<String> typesAdded() {
        // TODO Why not have this modify the import fixer directly
        // then we can remove types as well... maybe.
        return Collections.emptySet();
    }
    
}