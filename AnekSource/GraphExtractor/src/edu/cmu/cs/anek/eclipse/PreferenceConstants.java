package edu.cmu.cs.anek.eclipse;

/**
 * <br>
 * अनेक<br>
 * Anek<br>
 * <br>
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {

    public static final String VALIDATE_PREF = "validateXMLPreference";
    
    public static final String VERBOSE_PREF = "verboseModePreference";
    
    public static final String COMMIT_WC_PREF = "commitWorkingCopiesPreference";
    
    public static final String ENG_PATH = "enginePathPreference";
    
    public static final String EL_PREF = "EqualLikelihood";
    
    public static final String AAP_PREF = "ArgsAreParams";
    
    public static final String HEL_PREF = "HEqualLikelihood";
    
    public static final String CUL_PREF = "HCreateUniqueLikelihood";

    public static final String GETTER_L_PREF = "HGetterLikelihood";
    
    public static final String SYNC_L_PREF = "HSyncLikelihood";
    
    public static final String CTR_L_PREF = "HConstructorLikelihood";
    
    public static final String EDGL_PREF = "EdgeLikelihood";
    
    public static final String STR_PREF = "StrongerLikelihood";
    
    public static final String MR_PREF = "MustReadLikelihood";
    
    public static final String MW_PREF = "MustWriteLikelihood";

    public static final String SNL_PREF = "SplitLikelihood";
    
    public static final String IPT_PREF = "IsPermissionThreshold";
    
    public static final String BT_PREF = "BorrowingThreshold";
    
    /**
     * Array of all the constants that will be written to the 
     * Anek inference preference file.
     */
    public static final String[] ANEK_PREFS = 
        new String[] { EL_PREF, STR_PREF, MR_PREF, MW_PREF, 
            SNL_PREF, IPT_PREF, BT_PREF, EDGL_PREF , HEL_PREF, 
            CUL_PREF, GETTER_L_PREF, SYNC_L_PREF, CTR_L_PREF, AAP_PREF} ;
}
