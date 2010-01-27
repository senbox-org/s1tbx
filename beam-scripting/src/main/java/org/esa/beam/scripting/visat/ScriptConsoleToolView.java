package org.esa.beam.scripting.visat;

import com.bc.ceres.core.Assert;
import com.jidesoft.status.StatusBar;
import com.jidesoft.swing.JideScrollPane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

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

    @Override
    public JComponent createControl() {
        return new ScriptConsoleForm(getPaneWindow()).getContentPanel();
    }

}
 
