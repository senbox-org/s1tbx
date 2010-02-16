package org.esa.beam.scripting.visat;

import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.*;

// todo - find out how to:
// (1) ... gracefully cancel a running script
// (2) ... remove bindings (references) in JavaScript to products, views, etc. in order to avoid memory leaks
// (3) ... debug a script
// (4) ... trace & undo changes to BEAM made by a script

/**
 * A tool window for the scripting console.
 */
public class ScriptConsoleToolView extends AbstractToolView {

    public static final String ID = ScriptConsoleToolView.class.getName();
    private String titleBase;

    @Override
    public JComponent createControl() {
        titleBase = getDescriptor().getTitle();
        return new ScriptConsoleForm(this).getContentPanel();
    }

    public String getTitleBase() {
        return titleBase;
    }
}
 
