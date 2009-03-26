package edu.cmu.cs.plural.perm.parser;

/**
 * This enumerations defines constants with the same names
 * as {@link edu.cmu.cs.plural.annot.Use}, which allows
 * using {@link PermissionUse#valueOf(String)} to get the right
 * instance from a variable binding in an annotation.
 * @author Kevin Bierhoff
 * @since Mar 25, 2009
 */
public enum PermissionUse {
	DISPATCH {
		@Override public boolean isVirtual() { return true; }
		@Override public boolean isFrame() { return false; }
	}, 
	FIELDS {
		@Override public boolean isVirtual() { return false; }
		@Override public boolean isFrame() { return true; }
	},
	DISP_FIELDS {
		@Override public boolean isVirtual() { return true; }
		@Override public boolean isFrame() { return true; }
	};
	public abstract boolean isVirtual();
	public abstract boolean isFrame();
}