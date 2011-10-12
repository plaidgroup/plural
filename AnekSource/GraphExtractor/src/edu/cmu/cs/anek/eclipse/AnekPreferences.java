package edu.cmu.cs.anek.eclipse;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The Anek preferences page. This preferences page was
 * created from a wizard, so most of the commends are
 * auto-generated.
 * <br>
 * अनेक<br>
 * Anek<br>
 * <br>
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class AnekPreferences
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

    public AnekPreferences() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Preferences for running permission inference.");
    }
    
    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
    public void createFieldEditors() {
        addField(new BooleanFieldEditor(PreferenceConstants.VALIDATE_PREF, 
                "&Validate XML (slower)", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceConstants.VERBOSE_PREF,
                "Verbose Mode", getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(
                PreferenceConstants.COMMIT_WC_PREF,
            "Inference on un-opened files",
            1,
            new String[][] { 
                        { "Apply results to open files only", "nocommit" }, 
                        { "Apply results to all selected files (prohibits 'undo')", "commit" }
        }, getFieldEditorParent()));
        addField(new FileFieldEditor(PreferenceConstants.ENG_PATH,
                "Engine &path:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.EL_PREF,
                "Equal likelihood:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.HEL_PREF,
                "(Heuristic) Equal likelihood:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.CUL_PREF,
                "(Heuristic) Create Methods Return Unique:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.GETTER_L_PREF,
                "(Heuristic) Getters and Setters:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.SYNC_L_PREF,
                "(Heuristic) Synchronized:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.CTR_L_PREF,
                "(Heuristic) Constructors:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.AAP_PREF,
                "Args=Params likelihood:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.EDGL_PREF,
                "Edge likelihood:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.STR_PREF,
                "Stronger likelihood:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.MR_PREF,
                "Must read likelihood:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.MW_PREF,
                "Must write likelihood:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.SNL_PREF,
                "Split likelihood:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.IPT_PREF,
                "Is perm. threshold:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.BT_PREF,
                "Borrowing threshold:", getFieldEditorParent()));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }
    
    
}