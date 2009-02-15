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
package edu.cmu.cs.plural.states;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Modifier;

import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;
import edu.cmu.cs.crystal.util.Pair;
import edu.cmu.cs.crystal.util.SimpleMap;
import edu.cmu.cs.plural.fractions.PermissionFactory;
import edu.cmu.cs.plural.fractions.PermissionFromAnnotation;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.perm.ParameterPermissionAnnotation;
import edu.cmu.cs.plural.perm.ResultPermissionAnnotation;
import edu.cmu.cs.plural.perm.parser.PermAnnotation;
import edu.cmu.cs.plural.perm.parser.PermParser;
import edu.cmu.cs.plural.perm.parser.AbstractParamVisitor.FractionCreation;
import edu.cmu.cs.plural.pred.MethodPostcondition;
import edu.cmu.cs.plural.pred.MethodPrecondition;
import edu.cmu.cs.plural.track.CrystalPermissionAnnotation;

/**
 * @author Kevin Bierhoff
 *
 */
abstract class AbstractBindingSignature extends AbstractBinding 
		implements IInvocationSignature {
	
	private Map<String, String> capturedParams;
	private Map<String, String> releasedParams;
	private Set<String> notReturned;
	
	/**
	 * @param crystal
	 * @param binding The binding that contains specs to be used
	 * @param staticallyInvokedBinding The invoked binding according to the type checker, 
	 * which can be different from <code>specBinding</code> if specifications are inherited.
	 */
	protected AbstractBindingSignature(AnnotationDatabase annoDB, IMethodBinding binding, IMethodBinding staticallyInvokedBinding) {
		super(annoDB, binding, staticallyInvokedBinding);
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
	
	protected Pair<MethodPrecondition, MethodPostcondition> preAndPost(
			boolean forAnalyzingBody, Pair<String, String> preAndPostString,
			boolean frameAsVirtual, boolean noReceiverPre) {
		final Map<String, StateSpace> spaces = new HashMap<String, StateSpace>();
		Map<String, PermissionSetFromAnnotations> pre = 
			new HashMap<String, PermissionSetFromAnnotations>();
		Map<String, PermissionSetFromAnnotations> post = 
			new HashMap<String, PermissionSetFromAnnotations>();
		
		FractionCreation namedFractions = 
			forAnalyzingBody ? FractionCreation.NAMED_UNIVERSAL : FractionCreation.VARIABLE_UNIVERSAL;
		
		/*
		 * 1. receiver
		 */
		if(!isStaticMethod(binding)) {
			StateSpace rcvr_space = getStateSpace(binding.getDeclaringClass());
			spaces.put("this", rcvr_space);
			if(!frameAsVirtual)
				spaces.put("this!fr", rcvr_space);
			
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> rcvr_borrowed = 
				prePostFromAnnotations(rcvr_space, 
						CrystalPermissionAnnotation.receiverAnnotations(getAnnoDB(), binding), 
						namedFractions, 
						frameAsVirtual,
						noReceiverPre);
			
			assert !frameAsVirtual || rcvr_borrowed.fst().getFramePermissions().isEmpty();
			
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> rcvr_pre =
				PermissionSetFromAnnotations.splitPermissionSets(rcvr_borrowed.fst());
			if(!noReceiverPre) {
				pre.put("this", rcvr_pre.fst());
				if(!frameAsVirtual)
					pre.put("this!fr", rcvr_pre.snd());
			}
			
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> rcvr_post =
				PermissionSetFromAnnotations.splitPermissionSets(rcvr_borrowed.snd());
			post.put("this", rcvr_post.fst());
			if(!frameAsVirtual)
				post.put("this!fr", rcvr_post.snd());
			
		}
		
		/*
		 * 2. arguments
		 */
		for(int paramIndex = 0; paramIndex < binding.getParameterTypes().length; paramIndex++) {
			String paramName = "#" + paramIndex;
			// use possibly more precise parameter type from typechecking
			StateSpace space = getStateSpace(staticallyInvokedBinding.getParameterTypes()[paramIndex]);
			spaces.put(paramName, space);
			
			Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> param_borrowed = 
				prePostFromAnnotations(space, 
					CrystalPermissionAnnotation.parameterAnnotations(getAnnoDB(), binding, paramIndex), 
					namedFractions,
					false, false);
			pre.put(paramName, param_borrowed.fst());
			post.put(paramName, param_borrowed.snd());
		}
		
		/*
		 * 3. result
		 */
		String capturing;  // symbolic name for capturing object
		// unfortunately this varies because it's "this" in constructors.
		if(binding.isConstructor()) {
			capturing = frameAsVirtual ? 
					"this" /* for "new" */ : 
						"this!fr" /* for super(...) or this(...) */;
		}
		else { // regular method
			capturing = "result";
			
			// use possibly more precise return type from typechecking
			StateSpace space = getStateSpace(staticallyInvokedBinding.getReturnType());
			spaces.put("result", space);
			
			PermissionSetFromAnnotations result = PermissionSetFromAnnotations.createEmpty(space);
			for(ResultPermissionAnnotation a : CrystalPermissionAnnotation.resultAnnotations(getAnnoDB(), binding)) {
				PermissionFromAnnotation p = PermissionFactory.INSTANCE.createOrphan(space, a.getRootNode(), a.getKind(), a.isFramePermission(), a.getEnsures(), ! forAnalyzingBody);
				result = result.combine(p, forAnalyzingBody /* named means universal */);
			}
			post.put("result", result);
		}
		
		return PermParser.parseSignature(
				preAndPostString, forAnalyzingBody, frameAsVirtual, 
				new SimpleMap<String, StateSpace>() {
					@Override
					public StateSpace get(String key) {
						StateSpace result = spaces.get(key);
						assert result != null : "Can't find state space for " + key;
						return result;
					}
				},
				getCapturedParams(), capturing, getReleasedParams(), pre, post, 
				getNotReturned());
	}

	private Set<String> getNotReturned() {
		if(notReturned == null)
			initParamInfo();
		return notReturned;
	}

	private Map<String, String> getReleasedParams() {
		if(releasedParams == null) {
			initParamInfo();
		}
		return releasedParams;
	}

	private Map<String, String> getCapturedParams() {
		if(capturedParams == null) {
			initParamInfo();
		}
		return capturedParams;
	}

	/**
	 * Initializes the maps for captured and released parameters from the respective annotations.
	 * @tag todo.general -id="1969913" : read capturing states from @Param annotations on classes
	 */
	private void initParamInfo() {
		capturedParams = new LinkedHashMap<String, String>();
		releasedParams = new LinkedHashMap<String, String>();
		notReturned = new LinkedHashSet<String>();
//		if(binding.isConstructor())
//			notReturned.add("this!fr");
		
		// receiver
		ICrystalAnnotation this_c = getAnnoDB().getSummaryForMethod(binding).getReturn("edu.cmu.cs.plural.annot.Capture");
		if(this_c != null) {
			// doesn't matter here whether @Capture or @Lend
			capturedParams.put("this!fr", StateSpace.STATE_ALIVE);
		}
		ICrystalAnnotation this_l = getAnnoDB().getSummaryForMethod(binding).getReturn("edu.cmu.cs.plural.annot.Lend");
		if(this_l != null) {
			// doesn't matter here whether @Capture or @Lend
			capturedParams.put("this!fr", StateSpace.STATE_ALIVE);
		}
		ICrystalAnnotation this_r = getAnnoDB().getSummaryForMethod(binding).getReturn("edu.cmu.cs.plural.annot.Release");
		if(this_r != null) {
			releasedParams.put("this!fr", (String) this_r.getObject("value"));
		}
		if(CrystalPermissionAnnotation.isReceiverNotBorrowed(getAnnoDB(), binding))
			notReturned.add("this");
		if(CrystalPermissionAnnotation.isReceiverFrameNotBorrowed(getAnnoDB(), binding))
			notReturned.add("this!fr");
		
		// parameters
		for(int i = 0; i < binding.getParameterTypes().length; i++) {
			ICrystalAnnotation param_c = getAnnoDB().getSummaryForMethod(binding).getParameter(i, "edu.cmu.cs.plural.annot.Capture");
			if(param_c != null) {
				// doesn't matter here whether @Capture or @Lend
				capturedParams.put("#" + i, StateSpace.STATE_ALIVE);
			}
			ICrystalAnnotation param_l = getAnnoDB().getSummaryForMethod(binding).getParameter(i, "edu.cmu.cs.plural.annot.Lend");
			if(param_l != null) {
				// doesn't matter here whether @Capture or @Lend
				capturedParams.put("#" + i, StateSpace.STATE_ALIVE);
			}
			ICrystalAnnotation param_r = getAnnoDB().getSummaryForMethod(binding).getParameter(i, "edu.cmu.cs.plural.annot.Release");
			if(param_r != null) {
				releasedParams.put("#" + i, (String) param_r.getObject("value"));
			}
			if(CrystalPermissionAnnotation.isParameterNotBorrowed(getAnnoDB(), binding, i))
				notReturned.add("#" + i);
		}
		capturedParams = Collections.unmodifiableMap(capturedParams);
		releasedParams = Collections.unmodifiableMap(releasedParams);
		notReturned = Collections.unmodifiableSet(notReturned);
	}

	private boolean isStaticMethod(IMethodBinding binding) {
		return Modifier.isStatic(binding.getModifiers());
	}

	/**
	 * @param space
	 * @param annos
	 * @param namedFractions
	 * @param frameAsVirtual This should only be used for receiver permissions 
	 * at virtual method call sites.
	 * @param ignoreVirtual Ignores virtual permissions, useful for constructor signatures.
	 * @return
	 */
	private Pair<PermissionSetFromAnnotations, PermissionSetFromAnnotations> prePostFromAnnotations(
			StateSpace space, List<ParameterPermissionAnnotation> annos, 
			FractionCreation namedFractions, 
			boolean frameAsVirtual, boolean ignoreVirtual) {
		PermissionSetFromAnnotations pre = PermissionSetFromAnnotations.createEmpty(space);
		PermissionSetFromAnnotations post = PermissionSetFromAnnotations.createEmpty(space);
		for(ParameterPermissionAnnotation a : annos) {
			if(!a.isFramePermission() && ignoreVirtual)
				continue;
			PermissionFromAnnotation p = PermissionFactory.INSTANCE.createOrphan(
					space, a.getRootNode(), a.getKind(), 
					a.isFramePermission() && !frameAsVirtual, 
					a.getRequires(), namedFractions.createNamed());
			pre = pre.combine(p, namedFractions.isNamedUniversal());
			if(a.isReturned())
				post = post.combine(p.copyNewState(a.getEnsures()), namedFractions.isNamedUniversal());
		}
		return Pair.create(pre, post);
	}

}
