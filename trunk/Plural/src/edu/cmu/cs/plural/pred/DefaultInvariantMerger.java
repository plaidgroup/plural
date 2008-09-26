package edu.cmu.cs.plural.pred;

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.cs.crystal.analysis.alias.Aliasing;
import edu.cmu.cs.plural.concrete.Implication;
import edu.cmu.cs.plural.fractions.PermissionSetFromAnnotations;
import edu.cmu.cs.plural.linear.DefaultPredicateMerger;
import edu.cmu.cs.plural.linear.TensorPluralTupleLE;

/**
 * An extension of {@link DefaultPredicateMerger} that works for unpacking
 * invariants. The primary difference is that it purifies permissions and
 * will not override a currently assigned field, if there is one.
 * 
 * @author Nels E. Beckman
 * @since Sep 26, 2008
 */
public class DefaultInvariantMerger extends DefaultPredicateMerger {

	private final String assignedField;
	private final boolean purify;

	/**
	 * @param astNode The ast node where this merge is occurring.
	 * @param value The current lattice.
	 * @param assignedField The field that is being assigned, or <code>null</code>
	 * @param purify Should the permissions that are unpacked be purified?
	 */
	public DefaultInvariantMerger(ASTNode astNode, 
			TensorPluralTupleLE value, String assignedField, boolean purify) {
		super(astNode, value);
		this.assignedField = assignedField;
		this.purify = purify;
	}

	@Override public void addImplication(Aliasing var,
			Implication implication) {
		// TODO Originally we weren't doing anything here, but
		// that seems incorrect.
		super.addImplication(var, implication);
	}
	
	@Override public void mergeInPermission(Aliasing a, String var_name,
			PermissionSetFromAnnotations perms) {
		if( !isAssigned(var_name) )
			// Purify the permission if necessary
			super.mergeInPermission(a, var_name, purify ? perms.purify() : perms);
	}

	// The rest of the methods do nothing but call their super, if
	// the given field is not being assigned.
	
	@Override
	public void addNull(Aliasing var, String var_name) {
		if( !isAssigned(var_name) )
			super.addNull(var, var_name);
	}

	@Override
	public void addFalse(Aliasing var, String var_name) {
		if( !isAssigned(var_name) )
			super.addFalse(var, var_name);
	}

	@Override
	public void addNonNull(Aliasing var, String var_name) {
		if( !isAssigned(var_name) )
			super.addNonNull(var, var_name);
	}

	@Override
	public void addStateInfo(Aliasing var, String var_name,
			Set<String> stateInfo, boolean inFrame) {
		if( !isAssigned(var_name) )
			super.addStateInfo(var, var_name, stateInfo, inFrame);
	}

	@Override
	public void addTrue(Aliasing var, String var_name) {
		if( !isAssigned(var_name) )
			super.addTrue(var, var_name);
	}
	
	private boolean isAssigned(String var_name) {
		return
		assignedField != null && var_name.endsWith(assignedField);
	}
	
}
