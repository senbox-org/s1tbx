/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
 
