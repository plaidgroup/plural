package edu.cmu.cs.anek.graph.permissions;

import edu.cmu.cs.anek.util.Utilities;

/**
 * Does this permission need Frame permission, virtual permission
 * or both?
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 *
 */
public enum PermissionUse {
    Frame, Virtual, Both;

    public static PermissionUse fromPluralUse(
            edu.cmu.cs.plural.perm.parser.PermissionUse pu) {
        switch(pu) {
        case DISP_FIELDS:
            return Both;
        case DISPATCH:
            return Virtual;
        case FIELDS:
             return Frame;
        }
        return Utilities.impossible();
    }
}
