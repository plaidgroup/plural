package edu.cmu.cs.anek.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.xml.sax.SAXException;

import edu.cmu.cs.crystal.util.Option;


/**
 * A collection of static utility methods.
 * @author Nels E. Beckman
 *
 */
public final class Utilities {
	
    /**
     * Get the erased version of this method, with respect to its
     */
    public static IMethodBinding erasedMethod(IMethodBinding method) {
        
        if( method.isGenericMethod() ) {
            throw new RuntimeException("Generic types are okay but not methods.");
        }
        
        ITypeBinding type = method.getDeclaringClass().getErasure().getErasure();
        for( IMethodBinding erased_method : type.getDeclaredMethods() ) {
            if( erased_method.isSubsignature(method) )
                return erased_method;
        }
        
        return impossible();
    }
    
	/**
	 * Form a new set from the union of two given sets.
	 */
	public static <A> Set<A> union(Collection<? extends A> c1,
	        Collection<? extends A> c2) {
	    Set<A> result = new HashSet<A>(c1);
	    result.addAll(c2);
	    return result;
	}

    public static boolean isReferenceType(IVariableBinding binding) {
        return Utilities.isReferenceType(binding.getType());
    }

    public static boolean isReferenceType(ITypeBinding binding) {
        return !binding.isPrimitive() &&
            !binding.isArray() && !binding.isEnum();
    }

    /**
     * Returns all of the annotations of type <code>annotation</code>
     * in <code>annos</code>.
     * @param annos The collection of annotations to search through.
     * @param annotation The type of the annotation you would like
     * returned. 
     * @param anno_aggregator The type of the aggregator of annotation,
     * if there is one. It is assumed that the aggregator stores annotations
     * of the desired type in its value parameter.
     * @return All of the annotations of type annotation that were found.
     */
    public static Collection<IAnnotationBinding> findAnnotations(IAnnotationBinding[] annos,
            Class<?> annotation, Class<?> anno_aggregator) {
        return faHelper(annos, annotation, 
                Option.<Class<?>>some(anno_aggregator));
    }
    
    /**
     * @see {@link #findAnnotations(IAnnotationBinding[], Class, Option)}.
     * To be called if there is no aggregator.
     */
    public static Option<IAnnotationBinding> findAnnotation(IAnnotationBinding[] annos,
            Class<?> annotation) {
        Collection<IAnnotationBinding> result_ = 
            faHelper(annos, annotation, Option.<Class<?>>none());
        return 
            result_.isEmpty() ? 
                    Option.<IAnnotationBinding>none() : 
                    Option.some(result_.iterator().next());
    }
    
    private static Collection<IAnnotationBinding> faHelper(IAnnotationBinding[] annos,
            Class<?> annotation, Option<Class<?>> anno_aggregator) {
        List<IAnnotationBinding> result = new LinkedList<IAnnotationBinding>();
        for( IAnnotationBinding anno : annos ) {
            // regular annotation
            if( anno.getAnnotationType().getQualifiedName().equals(annotation.getName()) )
                result.add(anno);
            // aggregator
            else if( anno_aggregator.isSome() &&  
              anno.getAnnotationType().getQualifiedName().equals(anno_aggregator.unwrap().getName()) ) {
                // add all from 'value'
                for( IMemberValuePairBinding member : anno.getAllMemberValuePairs() ) {
                    if( "value".equals(member.getName()) ) {
                        Object[] abs_ = (Object[])member.getValue();
                        IAnnotationBinding[] abs = Arrays.copyOf(abs_,abs_.length, IAnnotationBinding[].class);
                        result.addAll(Arrays.asList(abs));
                    }
                }
            }
        }
        return result;
    }
    
    public static <T> T impossible() {
        throw new RuntimeException("Impossible");
    }

    /**
     * Given a possibly-invalid NMTOKEN and return a valid NMTOKEN.
     */
    public static String legalNMToken(String nodeID) {
        // Don't remove the '-' sign! We need it.
        // This is ALL punctuation from \p{Punct} except '-'
        return nodeID.replaceAll("[!\"#\\$%&'\\(\\)\\*\\+,\\./:;<>\\?@\\[\\]\\\\^_`{}|\\~]", "");
//        return nodeID.replaceAll("[/;\\(\\)#~<>!{}\\+\\*\"]", "");
        //return nodeID.replaceAll("\\p{Punct}", "");
    }   
    
    /**
     * Validates the contents of the given file against the plural+graphml
     * schema.
     * @param filename
     * @throws SAXException
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void validate(String filename) throws SAXException, IOException,
            FileNotFoundException {
        File xmlFile = new File(filename);
        Source[] schemas = new Source[] {
                new StreamSource(new File("../xml/graphml+plural.xsd")),
                new StreamSource(new File("../xml/plural.xsd")),
                new StreamSource(new File("../xml/graphml.xsd")),
                new StreamSource(new File("../xml/graphml-attributes.xsd")),
                new StreamSource(new File("../xml/graphml-parseinfo.xsd")),
                new StreamSource(new File("../xml/graphml-structure.xsd")),
        };
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(schemas);
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new FileInputStream(xmlFile)));
    }
}
