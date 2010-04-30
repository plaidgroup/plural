/**
 * Copyright (C) 2007, 2008 Carnegie Mellon University and others.
 *
 * This file is part of Plural.
 *
 * Plural is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * Plural is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Plural; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking Plural statically or dynamically with other modules is
 * making a combined work based on Plural. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of Plural
 * give you permission to combine Plural with free software programs or
 * libraries that are released under the GNU LGPL and with code
 * included in the standard release of Eclipse under the Eclipse Public
 * License (or modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the
 * GNU GPL for Plural and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of Plural are not
 * obligated to grant this special exception for their modified
 * versions; it is their choice whether to do so. The GNU General
 * Public License gives permission to release a modified version
 * without this exception; this exception also makes it possible to
 * release a modified version which carries forward this exception.
 */
package edu.cmu.cs.plural.track;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.annotations.CrystalAnnotation;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.util.Option;
import edu.cmu.cs.plural.annot.ResultPolyVar;
import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.perm.PermissionAnnotation;
import edu.cmu.cs.plural.perm.ResultPermissionAnnotation;
import edu.cmu.cs.plural.perm.parser.PermissionUse;
import edu.cmu.cs.plural.polymorphic.instantiation.GroundInstantiation;
import edu.cmu.cs.plural.polymorphic.instantiation.GroundParser;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedParameterPermissionAnnotation;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedReturnPermissionAnnotation;
import edu.cmu.cs.plural.polymorphic.instantiation.InstantiatedTypeAnalysis;
import edu.cmu.cs.plural.polymorphic.instantiation.RcvrInstantiationPackage;
import edu.cmu.cs.plural.polymorphic.internal.PolyVarReturnedAnnotation;
import edu.cmu.cs.plural.polymorphic.internal.PolyVarUseAnnotation;
import edu.cmu.cs.plural.track.Permission.PermissionKind;

/**
 * @author Kevin Bierhoff
 *
 */
public class CrystalPermissionAnnotation extends CrystalAnnotation implements ParameterPermissionAnnotation {
	
	/**
	 * Returns a (possibly empty) list of permission annotations for the receiver of the given method.
	 * @param db
	 * @param method
	 * @return a (possibly empty) list of permission annotations for the receiver of the given method.
	 */
	public static List<ParameterPermissionAnnotation> receiverAnnotations(
			AnnotationDatabase db,
			IMethodBinding method) {
		return annotationsOnMethod(ParameterPermissionAnnotation.class, db, method);
	}

	/**
	 * Returns a (possibly empty) list of permission annotations for the result of the given method.
	 * @param db
	 * @param method
	 * @return a (possibly empty) list of permission annotations for the result of the given method.
	 */
	public static List<ResultPermissionAnnotation> resultAnnotations(
			AnnotationDatabase db, Option<RcvrInstantiationPackage> ip,
			IMethodBinding method) {
		List<ResultPermissionAnnotation> result = new LinkedList<ResultPermissionAnnotation>();
		result.addAll(annotationsOnMethod(ResultPermissionAnnotation.class, db, method));
		
		ICrystalAnnotation poly_result_ = db.getSummaryForMethod(method).getReturn(ResultPolyVar.class.getName());
		if( poly_result_ != null ) {
			PolyVarReturnedAnnotation poly_result = (PolyVarReturnedAnnotation)poly_result_;
			Collection<InstantiatedReturnPermissionAnnotation> result_anno = 
				instantiatePolyVar(poly_result, ip, db);
			result.addAll(result_anno);
		}
		
		return result;
	}

	private static <A extends PermissionAnnotation> List<A> annotationsOnMethod(
			Class<A> clazz,
			AnnotationDatabase db,
			IMethodBinding method) {
		AnnotationSummary s = db.getSummaryForMethod(method);
		if(s == null)
			return Collections.emptyList();
		List<ICrystalAnnotation> allAnnos = s.getReturn();
		if(allAnnos == null || allAnnos.isEmpty())
			return Collections.emptyList();
		List<A> result = new LinkedList<A>();
		for(ICrystalAnnotation anno : s.getReturn()) {
			if(clazz.isAssignableFrom(anno.getClass())) {
				A pa = clazz.cast(anno);
				result.add(pa);
			}
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns a (possibly empty) list of permission annotations for the given formal parameter 
	 * of the given method.
	 * @param db
	 * @param method
	 * @param paramIndex 0-based method parameter index.
	 * @return a (possibly empty) list of permission annotations for the given formal parameter 
	 * of the given method.
	 */
	public static List<ParameterPermissionAnnotation> parameterAnnotations(AnnotationDatabase db,
			Option<RcvrInstantiationPackage> ip, IMethodBinding method, int paramIndex) {
		AnnotationSummary s = db.getSummaryForMethod(method);
		if(s == null)
			return Collections.emptyList();
		List<ICrystalAnnotation> allAnnos = s.getParameter(paramIndex);
		if(allAnnos == null || allAnnos.isEmpty())
			return Collections.emptyList();
		List<ParameterPermissionAnnotation> result = new LinkedList<ParameterPermissionAnnotation>();
		result.addAll(AnnotationDatabase.filter(allAnnos, ParameterPermissionAnnotation.class));

		Collection<InstantiatedParameterPermissionAnnotation> wrappers =
			instantiatePolyVar(AnnotationDatabase.filter(allAnnos, PolyVarUseAnnotation.class), ip, db);
		result.addAll(wrappers);

		return Collections.unmodifiableList(result);
	}

	/**
	 * Takes the PolyVarUseAnnotations that are passed to it and returns 
	 * InstantiatedParameterPermissionAnnotation wrappers for the ones where its appropriate.
	 * Unfortunately, a lot of data is needed to create these wrapper annotations,
	 * hence the large number of parameters.
	 */
	private static Collection<InstantiatedParameterPermissionAnnotation> instantiatePolyVar(
			List<PolyVarUseAnnotation> filter, Option<RcvrInstantiationPackage> ip,
			AnnotationDatabase annoDB) {
		if( ip.isNone() )
			return Collections.emptyList();
		
		List<InstantiatedParameterPermissionAnnotation> result = new LinkedList<InstantiatedParameterPermissionAnnotation>();
		
		List<String> rcvr_type = ip.unwrap().getVarType();
		ITypeBinding rcvr_jtype = ip.unwrap().getVarJType();
		
		for( PolyVarUseAnnotation anno : filter ) {
			List<String> originals = Collections.singletonList(anno.getVariableName());
			List<String> subst_type = InstantiatedTypeAnalysis.substitute(rcvr_type, rcvr_jtype, originals, annoDB);
			assert(subst_type.size() == 1);
			// Now we need to parse the result and figure out what it is!
			String perm = subst_type.get(0);
			// This annotation should only be CONSTRUCTED if we know its an instantiation.
			Option<GroundInstantiation> ground_perm_ = GroundParser.parse(perm);
			if( ground_perm_.isSome() ) {
				// YES an instantiation
				InstantiatedParameterPermissionAnnotation anno_wrapper = 
					new InstantiatedParameterPermissionAnnotation(anno, ground_perm_.unwrap());
				result.add(anno_wrapper);
			}
			
		}
		
		return result;
	}

	/**
	 * @param polyResult
	 * @param ip
	 * @param db
	 * @return
	 */
	private static Collection<InstantiatedReturnPermissionAnnotation> instantiatePolyVar(
			PolyVarReturnedAnnotation anno,
			Option<RcvrInstantiationPackage> ip, AnnotationDatabase db) {
		if( ip.isNone() ) {
			return Collections.emptySet();
		}
		
		List<String> rcvr_type = ip.unwrap().getVarType();
		ITypeBinding rcvr_jtype = ip.unwrap().getVarJType();
		
		List<String> originals = Collections.singletonList(anno.getVariableName());
		List<String> subst_type = InstantiatedTypeAnalysis.substitute(rcvr_type, rcvr_jtype, originals, db);
		assert(subst_type.size() == 1);
		// Now we need to parse the result and figure out what it is!
		String perm = subst_type.get(0);
		// This annotation should only be CONSTRUCTED if we know its an instantiation.
		Option<GroundInstantiation> ground_perm_ = GroundParser.parse(perm);
		if( ground_perm_.isSome() ) {
			// YES an instantiation
			InstantiatedReturnPermissionAnnotation anno_wrapper = 
				new InstantiatedReturnPermissionAnnotation(anno, ground_perm_.unwrap());
			return Collections.singleton(anno_wrapper);
		}
		
		return Collections.emptySet();
	}

	/**
	 * Parse the instantiation of this permission, so that we will be ready when the pre & post-condition methods
	 * are called.
	 */
//	private GroundInstantiation parseInstantiation() {
//		List<String> rcvr_application = this.typeAnalysis.findType(rcvrVar);
//		List<String> poly_vars = Collections.singletonList(paramPermAnnotation.getVariableName());
//		// TODO Method decl is totally inappropriate to pass as an error node, maybe we should remove
//		// this parameter from the substitute method and just cover it with the spec checker. 
//		List<String> perms = InstantiatedTypeAnalysis.substitute(rcvr_application, rcvrVar.resolveType(), poly_vars, annoDB);
//		assert(perms.size() == 1);
//		// Now we need to parse the result and figure out what it is!
//		String perm = perms.get(0);
//		// This annotation should only be CONSTRUCTED if we know its an instantiation.
//		assert(PolyInternalChecker.isPermLitteral(perm));
//		GroundInstantiation result = GroundParser.parse(perm).unwrap();
//		return result;
//		
//	}




	
	/**
	 * @param db
	 * @param field
	 * @return
	 */
	public static List<ParameterPermissionAnnotation> fieldAnnotations(
			AnnotationDatabase db, IVariableBinding field) {
		List<ICrystalAnnotation> allAnnos = db.getAnnosForVariable(field);
		if(allAnnos == null || allAnnos.isEmpty())
			return Collections.emptyList();
		return Collections.unmodifiableList(AnnotationDatabase.filter(allAnnos, ParameterPermissionAnnotation.class));
	}

	/**
	 * @param name
	 */
	public CrystalPermissionAnnotation(String name) {
		super(name);
	}

	/**
	 * Default constructor to allow {@link java.lang.Class#newInstance()}.
	 */
	public CrystalPermissionAnnotation() {
	}
	
	public boolean isReturned() {
		return (Boolean) getObject("returned");
	}
	
	public PermissionKind getKind() {
		return deriveKindFromName(getName());
	}
	
	public String getRootNode() {
		String result = (String) getObject("guarantee");
		// give "guarantee" attribute precedence over "value"
		// backwards compatibility: ignore if null (unknown attribute)
		return result == null || result.isEmpty() ? (String) getObject("value") : result;
	}
	
	public String[] getRequires() {
		Object[] requires = (Object[]) getObject("requires");
		if(requires.length == 0)
			return new String[] { getRootNode() };
		String[] result = new String[requires.length];
		System.arraycopy(requires, 0, result, 0, requires.length);
		return result;
//		return Arrays.copyOf(result, result.length, String[].class);
	}
	
	public String[] getEnsures() {
		Object[] ensures = (Object[]) getObject("ensures");
		if(ensures.length == 0)
			return new String[] { getRootNode() };
		String[] result = new String[ensures.length];
		System.arraycopy(ensures, 0, result, 0, ensures.length);
		return result;
//		return Arrays.copyOf(ensures, ensures.length, String[].class);
	}
	
	private static PermissionKind deriveKindFromName(String name) {
		PermissionKind k;
		if(name.equals("edu.cmu.cs.plural.annot.Unique")) {
			k = PermissionKind.UNIQUE;
		}
		else if(name.equals("edu.cmu.cs.plural.annot.Full")) {
			k = PermissionKind.FULL;
		}
		else if(name.equals("edu.cmu.cs.plural.annot.Share")) {
			k = PermissionKind.SHARE;
		}
		else if(name.equals("edu.cmu.cs.plural.annot.Imm")) {
			k = PermissionKind.IMMUTABLE;
		}
		else if(name.equals("edu.cmu.cs.plural.annot.Pure")) {
			k = PermissionKind.PURE;
		}
		else
			// not a permission anno
			throw new IllegalArgumentException("Unknown permission annotation: " + name);
		return k;
	}

	@Override
	public boolean isFramePermission() {
		IVariableBinding result = (IVariableBinding) getObject("use");
		if(result == null)
			// backwards-compatibility: use fieldAccess flag
			return isFieldAccess();
	
		PermissionUse use = PermissionUse.valueOf(result.getName());
		if(use == PermissionUse.DISPATCH)
			// use legacy fieldAccess flag if "use" has default value
			return isFieldAccess();
		// new "use" attribute takes precedence over legacy fieldAccess flag
		return use.isFrame();
	}
	
	@Override
	public boolean isVirtualPermission() {
		IVariableBinding result = (IVariableBinding) getObject("use");
		if(result == null)
			// backwards-compatibility: use fieldAccess flag
			return ! isFieldAccess();
		
		PermissionUse use = PermissionUse.valueOf(result.getName());
		if(use == PermissionUse.DISPATCH)
			// use legacy fieldAccess flag if "use" has default value
			return ! isFieldAccess();
		// new "use" attribute takes precedence over legacy fieldAccess flag
		return use.isVirtual();
	}
	
	/**
	 * Kickin' it old school: this method queries the legacy fieldAcces
	 * flag and returns its value.
	 * TODO retire fieldAccess flag
	 * @return the value of the fieldAccess flag or <code>false</code>
	 * if that flag is unknown (would have to be a *very* old annotatio Jar).
	 */
	private boolean isFieldAccess() {
		Boolean result = (Boolean) getObject("fieldAccess");
		if(result == null)
			// backwards-compatibility: not a frame permission if attribute unknown
			return false;
		return result;
	}

	public static boolean isReceiverNotBorrowed(AnnotationDatabase annoDB, IMethodBinding binding) {
		for(ParameterPermissionAnnotation a : receiverAnnotations(annoDB, binding)) {
			if(! a.isFramePermission() && ! a.isReturned())
				return true;
		}
		return false;
	}

	public static boolean isReceiverFrameNotBorrowed(AnnotationDatabase annoDB, IMethodBinding binding) {
		for(ParameterPermissionAnnotation a : receiverAnnotations(annoDB, binding)) {
			if(a.isFramePermission() && ! a.isReturned())
				return true;
		}
		return false;
	}

	public static boolean isParameterNotBorrowed(AnnotationDatabase annoDB, 
			IMethodBinding binding, int paramIndex) {		
		for(ParameterPermissionAnnotation a : parameterAnnotations(annoDB, Option.<RcvrInstantiationPackage>none(), binding, paramIndex)) {
			if(! a.isReturned())
				return true;
		}
		return false;
	}

}
