/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
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
package edu.cmu.cs.plural.states;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.perm.ResultPermissionAnnotation;
import edu.cmu.cs.plural.perm.parser.PermAnnotation;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.track.CrystalPermissionAnnotation;
import edu.cmu.cs.plural.util.Pair;

/**
 * @author Kevin Bierhoff
 *
 */
abstract class AbstractBindingSignature extends AbstractBinding 
		implements IInvocationSignature {
	
	private static final Logger log = Logger.getLogger(AbstractBindingSignature.class.getName());
	
	/**
	 * @param crystal
	 * @param binding The binding that contains specs to be used
	 * @param staticallyInvokedType The statically invoked type of this binding, which can 
	 * be different from <code>binding</code>'s declaring class if this is an inherited binding
	 */
	protected AbstractBindingSignature(AnnotationDatabase annoDB, IMethodBinding binding, ITypeBinding staticallyInvokedType) {
		super(annoDB, binding, staticallyInvokedType);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#getConstructorSignature()
	 */
	@Override
	public IConstructorSignature getConstructorSignature() {
		throw new IllegalStateException("This is not a constructor signature: " + this);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.plural.states.IInvocationSignature#getMethodSignature()
	 */
	@Override
	public IMethodSignature getMethodSignature() {
		throw new IllegalStateException("This is not a method signature: " + this);
	}
	
	/**
	 * Returns the pre and post condition permissions for the receiver of the
	 * method call, given the method binding.
	 * @param namedFractions
	 * @param preAndPostString Pre- and post-condition from {@link PermAnnotation}.
	 * @param frameAsVirtual If <code>true</code>, frame permissions are coerced into
	 * virtual permissions.  This can only be done for receiver permissions and should
	 * only be used at virtual method call sites.
	 */
	protected Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
	receiverPermissions(boolean namedFractions, Pair<String, String> preAndPostString, 
			boolean frameAsVirtual) {
		if(isStaticMethod(binding))
			return null;
		StateSpace space = getStateSpace(binding.getDeclaringClass());
		Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> result = 
			prePostFromAnnotations(space, CrystalPermissionAnnotation.receiverAnnotations(getAnnoDB(), binding), namedFractions, frameAsVirtual);
		
		result = mergeWithParsedRcvrPermissions(result, preAndPostString, space, namedFractions, frameAsVirtual);
//		if(binding.isConstructor())
//			result = Pair.create(null, result.snd());
		return result;
	}

	private boolean isStaticMethod(IMethodBinding binding) {
		return (binding.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
	}

//	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
//	parameterPermissions(IMethodBinding binding, int paramIndex) {
//		return this.parameterPermissions(binding, paramIndex, false);
//	}
	
	/**
	 * Returns the pre and post condition permissions for the paramIndex-th 
	 * parameter of the binding method.
	 */
	protected Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> 
	parameterPermissions(int paramIndex, boolean namedFractions, 
			Pair<String, String> preAndPostString) {
		
		StateSpace space = getStateSpace(binding.getParameterTypes()[paramIndex]);
		Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> result = 
			prePostFromAnnotations(space, 
				CrystalPermissionAnnotation.parameterAnnotations(getAnnoDB(), binding, paramIndex), 
				namedFractions,
				false);
		
		result = mergeWithParsedParamPermissions(result, preAndPostString, space, paramIndex, namedFractions);
		return result;
	}

	/**
	 * @param space
	 * @param annos
	 * @param namedFractions
	 * @param frameAsVirtual This should only be used for receiver permissions 
	 * at virtual method call sites.
	 * @return
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> prePostFromAnnotations(
			StateSpace space, List<ParameterPermissionAnnotation> annos, boolean namedFractions, 
			boolean frameAsVirtual) {
		PermissionSetFromAnnotations pre = PermissionSetFromAnnotations.createEmpty(space);
		PermissionSetFromAnnotations post = PermissionSetFromAnnotations.createEmpty(space);
		for(ParameterPermissionAnnotation a : annos) {
			PermissionFromAnnotation p = PermissionFactory.INSTANCE.createOrphan(
					space, a.getRootNode(), a.getKind(), 
					a.isFramePermission() && !frameAsVirtual, 
					a.getRequires(), namedFractions);
			pre = pre.combine(p);
			if(a.isReturned())
				post = post.combine(p.copyNewState(a.getEnsures()));
		}
		return Pair.create(pre, post);
	}

	/**
	 * This method takes receiver permissions that came from the old-style
	 * permission annotations on a method and combines them with permissions from
	 * the new-style annotations.
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> mergeWithParsedRcvrPermissions(
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> preAndPost,
			Pair<String, String> preAndPostString, StateSpace space, boolean namedFractions,
			boolean frameAsVirtual) {
//		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		
		if( preAndPostString == null ) {
			return preAndPost;
		}
			
		PermissionSetFromAnnotations prePerm = preAndPost.fst();
		PermissionSetFromAnnotations postPerm = preAndPost.snd();
		try {
			Pair<List<PermissionFromAnnotation>,
			List<PermissionFromAnnotation>> prePostPerms = 
				PermParser.parseReceiverPermissions(preAndPostString.fst(), preAndPostString.snd(),
						space, namedFractions, frameAsVirtual);
			for( PermissionFromAnnotation pre_p : prePostPerms.fst() ) {
				prePerm = prePerm.combine(pre_p);
			}
			for( PermissionFromAnnotation post_p : prePostPerms.snd() ) {
				postPerm = postPerm.combine(post_p);
			}
		} catch(RecognitionException re) {
			if(log.isLoggable(Level.WARNING))
				log.warning("Permission parameter parse error! " + re.toString());
		}
		return Pair.create(prePerm, postPerm);
	}
	/**
	 * This method merges takes the pre and post permission for a parameter from the old style annotations and
	 * merges in the permissions from the new-style annotations.
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> mergeWithParsedParamPermissions(
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> preAndPost,
			Pair<String, String> preAndPostString,
			StateSpace space, int paramIndex, boolean namedFractions) {
//		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		
		if( preAndPostString == null ) {
			return preAndPost;
		}
			
		PermissionSetFromAnnotations prePerm = preAndPost.fst();
		PermissionSetFromAnnotations postPerm = preAndPost.snd();
		try {
			Pair<List<PermissionFromAnnotation>,
			List<PermissionFromAnnotation>> prePostPerms = 
				PermParser.parseParameterPermissions(preAndPostString.fst(), preAndPostString.snd(),
						space, paramIndex, namedFractions);
			for( PermissionFromAnnotation pre_p : prePostPerms.fst() ) {
				prePerm = prePerm.combine(pre_p);
			}
			for( PermissionFromAnnotation post_p : prePostPerms.snd() ) {
				postPerm = postPerm.combine(post_p);
			}
		} catch(RecognitionException re) {
			if(log.isLoggable(Level.WARNING))
				log.warning("Permission parameter parse error! " + re.toString());
		}
		return Pair.create(prePerm, postPerm);
	}
	/**
	 * This method takes the result permission for a parameter from the old style
	 * annotations and merges in the permissions from the new-style annotations.
	 */
	private PermissionSetFromAnnotations mergeWithParsedResultPermissions(
			PermissionSetFromAnnotations result, String postString,
			StateSpace space, boolean namedFractions) {
//		Pair<String, String> preAndPostString = PermParser.getPermAnnotationStrings(getAnnoDB().getSummaryForMethod(binding));
		
		if( postString == null ) {
			return result;
		}
			
		try {
			List<PermissionFromAnnotation> postPerms = 
				PermParser.parseResultPermissions(postString,
						space, namedFractions);
			for( PermissionFromAnnotation pre_p : postPerms ) {
				result = result.combine(pre_p);
			}
		} catch(RecognitionException re) {
			if(log.isLoggable(Level.WARNING))
				log.warning("Permission parameter parse error! " + re.toString());
		}
		return result;
	}
	
//	private PermissionSetFromAnnotations 
//	resultPermissions(IMethodBinding binding) {
//		PermissionSetFromAnnotations result = resultPermissions(binding, true);
//		return result;
//	}
	
	protected PermissionSetFromAnnotations 
	resultPermissions(boolean namedFractions, String postString) {
		StateSpace space = getStateSpace(binding.getReturnType());
		PermissionSetFromAnnotations result = PermissionSetFromAnnotations.createEmpty(space);
		for(ResultPermissionAnnotation a : CrystalPermissionAnnotation.resultAnnotations(getAnnoDB(), binding)) {
			PermissionFromAnnotation p = PermissionFactory.INSTANCE.createOrphan(space, a.getRootNode(), a.getKind(), a.isFramePermission(), a.getEnsures(), namedFractions);
			result = result.combine(p);
		}
		result = mergeWithParsedResultPermissions(result, postString, space, namedFractions);
		return result;
	}

}
