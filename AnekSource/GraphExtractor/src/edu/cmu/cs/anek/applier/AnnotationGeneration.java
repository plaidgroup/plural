package edu.cmu.cs.anek.applier;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.cmu.cs.anek.graph.permissions.ConcretePermissionElement;
import edu.cmu.cs.anek.graph.permissions.Permission;
import edu.cmu.cs.anek.graph.permissions.PermissionKind;
import edu.cmu.cs.anek.graph.permissions.PermissionUse;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.Utilities;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Fulls;
import edu.cmu.cs.plural.annot.Imm;
import edu.cmu.cs.plural.annot.Imms;
import edu.cmu.cs.plural.annot.Perm;
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
import edu.cmu.cs.plural.annot.Use;

/**
 * Code for generating text annotations from 
 * <br>
 * अनेक<br>
 * Anek<br>
 * 
 * @author Nels E. Beckman
 *
 */
final class AnnotationGeneration {
   
    /**
     * Inserts a standard permission annotation (@Kind or @Perm) 
     * for receivers and method parameters. Will not work for
     * fields or return values.
     *  
     * @param pre Pre- permission
     * @param post Post- permission
     * @param name_in_at_perm The name by which the element should be
     * @param offset
     * @return
     */
    static Pair<AnnotationDiff,AtPermCreator> 
        insertPermAnnotation(Option<Permission> pre, Option<Permission> post,
            String name_in_at_perm, int offset) {
        // All this code is written with our initial assumption
        // in mind: that only one permission perm method will be
        // inferred. (No dimensions)
        ConcretePermissionElement pre_elem = null;
        if( pre.isSome() ) {
            Permission pre_ = pre.unwrap();
            // TODO Assumes only one permission!
            if( !pre_.getGround().getPermissions().isEmpty() ) {
                pre_elem = pre_.getGround().getPermissions().iterator().next();
            }
        }

        ConcretePermissionElement post_elem = null;
        if( post.isSome() ) {
            Permission post_ = post.unwrap();
            // TODO Assumes only one permission
            if( !post_.getGround().getPermissions().isEmpty() ) {
                post_elem = post_.getGround().getPermissions().iterator().next();
            }
        }
        
        // When do we generated @Kind?
        // If pre is some, post is none OR if they are both some
        // and have the same fraction.
        boolean gen_at_kind =
            (pre_elem != null && 
                    (post_elem == null || pre_elem.sameFraction(post_elem)));
        
        Option<ConcretePermissionElement> post_elem_ = 
            post_elem == null ?
                    Option.<ConcretePermissionElement>none() : 
                    Option.some(post_elem);
        if( gen_at_kind ) {
            AnnotationDiff d = genAtKindAnno(offset, pre_elem, post_elem_);
            return Pair.create(d, AtPermCreator.EMPTY);
        }
        else if( pre_elem == null && post_elem == null ) {
            // do nothing
            return Pair.create(NullDiff.INSTANCE, AtPermCreator.EMPTY);
        }
        else {
            AtPermCreator apc = genPermAnno(pre_elem, post_elem, name_in_at_perm);
            return Pair.create(NullDiff.INSTANCE, apc);
        }
    }

    // Generate a diff corresponding to @Perm(requires="", ensures="")
    // NOTE permissions could be null
    private static AtPermCreator genPermAnno(ConcretePermissionElement p1,
            ConcretePermissionElement p2, String nameForAnno) {
        
        String r = "";
        if( p1 != null ) {
            r = concreteElementToPermString(nameForAnno, p1);
        }
        
        String e = "";
        if( p2 != null ) {
            e = concreteElementToPermString(nameForAnno, p2);
        }
        
        return new AtPermCreator(r, e);
    }

    private static String concreteElementToPermString(
            String nameForAnno, ConcretePermissionElement p) {
        // TODO This just generates a simple 'alive' annotation
        // it only keeps the permission kind.
        StringBuilder result = new StringBuilder(p.getKind().toString().toLowerCase());
        result.append('(');
        result.append(nameForAnno);
        
        // Permission use?
        switch(p.getUsage()) {
        case Frame:
            result.append("!fr");
            break;
        case Virtual:
            break;
        case Both:
            return Utilities.nyi();
        default:
            throw new RuntimeException("Impossible");
        }
        
        result.append(')');
        return result.toString();
    }

    // create a diff corresponding to an @Kind annotation
    private static AnnotationDiff genAtKindAnno(int offset, 
            ConcretePermissionElement preElem, 
            Option<ConcretePermissionElement> postElem_) {
        StringBuilder result = new StringBuilder("@");
        PermissionKind kind = preElem.getKind();
        Class<?> annotation = PermissionKind.kindAnnotation(kind);
        String anno = annotation.getSimpleName();
        result.append(anno);
        result.append('(');
        
        Set<String> types_added = new HashSet<String>();
        types_added.add(annotation.getName());
        
        // use...
        if( preElem.getUsage().equals(PermissionUse.Frame) ) {
            // default is
            result.append("use=Use.FIELDS, ");
            types_added.add(Use.class.getName());
        }
        else if( preElem.getUsage().equals(PermissionUse.Both) ) {
            Utilities.nyi();
        }
        
        String guar = preElem.getGuarantee().name();
        if( !"alive".equals(guar) ) {
            result.append("guarantee=\"");
            result.append(guar);
            result.append("\", ");
        }
        
        // TODO Only one state...
        String req_state = preElem.getStates().iterator().next().name();
        if( !"alive".equals(req_state) ) {
            result.append("requires=\"");
            result.append(req_state);
            result.append("\", ");
        }
        
        if( postElem_.isNone() )
            result.append("returned=false");
        else {
            ConcretePermissionElement postElem = postElem_.unwrap();
            // TODO Only one state...
            String ens_state = postElem.getStates().iterator().next().name();
            if( !"alive".equals(ens_state) ) {
                result.append("ensures=\"");
                result.append(ens_state);
                result.append("\"");
            }
            else if( result.charAt(result.length() - 2) == ',' ) {
                // gross! Removes space and comma
                result.deleteCharAt(result.length()-1);
                result.deleteCharAt(result.length()-1);
            }
        }
        
        result.append(") ");
        return new InsertDiff(offset, result.toString(),
                types_added);
    }

    private final static Set<String> PLURAL_ANNOTATION_NAMES =
        new HashSet<String>();
    static {
        
        // permission annotations
        PLURAL_ANNOTATION_NAMES.add(Unique.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Uniques.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Full.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Fulls.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Imm.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Imms.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Share.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Shares.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Pure.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Pures.class.getName());
        PLURAL_ANNOTATION_NAMES.add(ResultUnique.class.getName());
        PLURAL_ANNOTATION_NAMES.add(ResultFull.class.getName());
        PLURAL_ANNOTATION_NAMES.add(ResultImm.class.getName());
        PLURAL_ANNOTATION_NAMES.add(ResultShare.class.getName());
        PLURAL_ANNOTATION_NAMES.add(ResultPure.class.getName());
        PLURAL_ANNOTATION_NAMES.add(Perm.class.getName());
    }
    
    /**
     * Generate a diff that will remove any and all plural
     * annotations from the given ast element.
     */
    public static AnnotationDiff removeAllPluralAnnotations(
            ASTNode param) {
        final List<AnnotationDiff> result_ =
            new LinkedList<AnnotationDiff>();
        //
        // We have to visit the AST element looking for annotations!
        // we must do this because simple annotation bindings do not
        // contain file location information.
        //
        param.accept(new ASTVisitor() {
            // this helper generates diffs to remove the given annotations
            // and does not descend if they are plural annotations so
            // we can just delete the whole thing.
            private boolean helper(IAnnotationBinding b, ASTNode n) {
                String qualifiedName = b.getAnnotationType().getQualifiedName();
                if( PLURAL_ANNOTATION_NAMES.contains(qualifiedName) ) {
                    int offset = n.getStartPosition();
                    int length = n.getLength();
                    RemoveDiff d = new RemoveDiff(offset, length);
                    result_.add(d);
                    return false;
                }
                return true;
            }
            
            @Override
            public boolean visit(MarkerAnnotation node) {
                return helper(node.resolveAnnotationBinding(), node);
            }

            @Override
            public boolean visit(NormalAnnotation node) {
                return helper(node.resolveAnnotationBinding(), node);
            }

            @Override
            public boolean visit(SingleMemberAnnotation node) {
                return helper(node.resolveAnnotationBinding(), node);
            }

            @Override
            public boolean visit(TypeDeclaration node) {
                // don't descend into anonymous classes.
                return false;
            }

            @Override
            public boolean visit(AnonymousClassDeclaration node) {
                // don't descend into anonymous classes.
                return false;
            } 
        });
        return new MultipleDiff(result_);
    }

    private static final String newline = System.getProperty("line.separator");
    
    /**
     * Given a permission, generates an @Result permission.
     */
    public static AnnotationDiff insertResultAnnotation(Permission permission, int offset) {
        if( !permission.getGround().getPermissions().isEmpty() ) {
            // TODO Assumes only one permission!
            ConcretePermissionElement p_elem =
                permission.getGround().getPermissions().iterator().next();
            PermissionKind k = p_elem.getKind();
            
            Class<?> anno_class = PermissionKind.kindResultAnnotation(k);
            String anno_type = anno_class.getName();
            
            StringBuilder sb = new StringBuilder("@");
            sb.append(anno_class.getSimpleName());
            sb.append(newline);
            return new InsertDiff(offset, sb.toString(), anno_type);
        }
        return NullDiff.INSTANCE;
    }    
}
