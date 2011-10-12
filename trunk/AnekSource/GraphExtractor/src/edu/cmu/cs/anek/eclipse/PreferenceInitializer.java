package edu.cmu.cs.anek.eclipse;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


/**
 * Class used to initialize default preference values.
 * <br>
 * अनेक<br>
 * Anek<br>
 * <br>
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(PreferenceConstants.ENG_PATH, "GraphLoader.exe");
        store.setDefault(PreferenceConstants.VALIDATE_PREF, true);
        store.setDefault(PreferenceConstants.VERBOSE_PREF, false);
        store.setDefault(PreferenceConstants.COMMIT_WC_PREF, "nocommit");
        store.setDefault(PreferenceConstants.EL_PREF,
                "0.8");
        store.setDefault(PreferenceConstants.HEL_PREF,
                "0.7");
        store.setDefault(PreferenceConstants.CUL_PREF,
                "0.8");
        store.setDefault(PreferenceConstants.GETTER_L_PREF,
                "0.7");
        store.setDefault(PreferenceConstants.SYNC_L_PREF,
                "0.8");
        store.setDefault(PreferenceConstants.CTR_L_PREF,
                "0.8");
        store.setDefault(PreferenceConstants.EDGL_PREF,
                "0.8");
        store.setDefault(PreferenceConstants.STR_PREF,
                "0.8");
        store.setDefault(PreferenceConstants.MR_PREF,
                "0.9");
        store.setDefault(PreferenceConstants.AAP_PREF,
                "0.9999");
        store.setDefault(PreferenceConstants.MW_PREF,
                "0.9");
        store.setDefault(PreferenceConstants.SNL_PREF,
                "0.95");
        store.setDefault(PreferenceConstants.IPT_PREF,
                "0.5"); 
        store.setDefault(PreferenceConstants.BT_PREF,
                "0.8");
    }

}
